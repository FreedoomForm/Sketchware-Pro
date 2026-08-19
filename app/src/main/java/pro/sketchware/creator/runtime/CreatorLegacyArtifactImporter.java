package pro.sketchware.creator.runtime;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.beans.ProjectResourceBean;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Imports legacy components, events, and block chains into the versioned Creator
 * Runtime document. Unsupported executable blocks stay visible in the report
 * and are never compiled or delegated to an APK fallback.
 */
public final class CreatorLegacyArtifactImporter {
    public static final String ACTIVITY_EVENT_TARGET = "__creator_runtime_activity__";
    public static final class Result {
        private final CreatorProjectDocument document;
        private final CreatorCompatibilityReport report;
        Result(CreatorProjectDocument document, CreatorCompatibilityReport report) {
            this.document = document;
            this.report = report;
        }
        public CreatorProjectDocument getDocument() { return document; }
        public CreatorCompatibilityReport getReport() { return report; }
    }

    public Result importArtifacts(CreatorProjectDocument base, List<ComponentBean> components,
                                  List<EventBean> events, Map<String, List<BlockBean>> blocksByEvent) {
        if (base == null) throw new IllegalArgumentException("base");
        CreatorCompatibilityReport report = new CreatorCompatibilityReport();
        Map<String, Object> state = new LinkedHashMap<>(base.getState());
        Map<String, Object> componentState = new LinkedHashMap<>();
        for (ComponentBean component : components == null ? Collections.<ComponentBean>emptyList() : components) {
            if (component == null || blank(component.componentId)) {
                report.add("unknown", "ComponentBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Component has no stable ID and cannot be imported safely.");
                continue;
            }
            String serviceId = CreatorRuntimeComponentServiceMatrix.serviceFor(component.type);
            if (serviceId == null) {
                report.add(component.componentId, "ComponentBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "No Creator Runtime service is registered for component type " + component.type + ".");
                continue;
            }
            Map<String, Object> descriptor = new LinkedHashMap<>();
            descriptor.put("serviceId", serviceId);
            descriptor.put("type", component.type);
            descriptor.put("param1", component.param1);
            descriptor.put("param2", component.param2);
            descriptor.put("param3", component.param3);
            componentState.put(component.componentId, descriptor);
            report.add(component.componentId, "ComponentBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                    "Mapped to Creator Runtime service " + serviceId + ".");
        }
        state.put("legacy.components", componentState);

        Map<String, CreatorEventBinding> bindings = new LinkedHashMap<>(base.getEvents());
        Map<String, Object> deferredEvents = new LinkedHashMap<>();
        for (EventBean event : events == null ? Collections.<EventBean>emptyList() : events) {
            if (event == null || blank(event.targetId) || blank(event.eventName)) {
                report.add("unknown", "EventBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Event has no stable target or name and cannot be imported safely.");
                continue;
            }
            String eventKey = event.getEventKey();
            List<BlockBean> legacyBlocks = blocksByEvent == null ? null : blocksByEvent.get(eventKey);
            BlockConversion blocks = convertBlocks(legacyBlocks, componentState);
            if (!blocks.unsupported.isEmpty()) {
                deferredEvents.put(eventKey, blocks.unsupported);
                report.add(eventKey, "BlockBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Unsupported legacy block opcodes: " + String.join(", ", blocks.unsupported) + ".");
                continue;
            }
            for (Map.Entry<String, List<CreatorRuntimeBlock>> callback : blocks.timerCallbacks.entrySet()) {
                if (callback.getKey().startsWith("firebase_children:")) {
                    String callbackId = callback.getKey().substring("firebase_children:".length());
                    String bindingId = "legacy_firebase_children_callback_" + callbackId;
                    bindings.put(bindingId, new CreatorEventBinding(bindingId, bindingId, "children",
                            callback.getValue()));
                    report.add("firebaseGetChildren:" + callbackId, "BlockBean",
                            CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                            "Imported Firebase children callback substack as a direct runtime children binding.");
                    continue;
                }
                if (callback.getKey().startsWith("dialog_button:")) {
                    String callbackId = callback.getKey().substring("dialog_button:".length());
                    String bindingId = "legacy_dialog_button_callback_" + callbackId;
                    bindings.put(bindingId, new CreatorEventBinding(bindingId, bindingId, "button",
                            callback.getValue()));
                    report.add("dialogButton:" + callbackId, "BlockBean",
                            CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                            "Imported dialog button callback substack as a direct runtime button binding.");
                    continue;
                }
                String bindingId = "legacy_timer_callback_" + callback.getKey();
                bindings.put(bindingId, new CreatorEventBinding(bindingId, callback.getKey(), "tick",
                        callback.getValue()));
                report.add("timer:" + callback.getKey(), "BlockBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                        "Imported timer callback substack as a direct runtime tick binding.");
            }
            if (!base.getWidgets().containsKey(event.targetId)) {
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("eventType", event.eventType);
                descriptor.put("targetId", event.targetId);
                descriptor.put("eventName", normalizeEventName(event.eventName));
                descriptor.put("blockCount", blocks.converted.size());
                deferredEvents.put(eventKey, descriptor);
                if (event.eventType == EventBean.EVENT_TYPE_ACTIVITY) {
                    String bindingId = "legacy_activity_" + normalizeEventName(event.eventName);
                    bindings.put(bindingId, new CreatorEventBinding(bindingId, ACTIVITY_EVENT_TARGET,
                            normalizeEventName(event.eventName), blocks.converted));
                } else if (event.eventType == EventBean.EVENT_TYPE_COMPONENT) {
                    String bindingId = "legacy_component_" + eventKey;
                    bindings.put(bindingId, new CreatorEventBinding(bindingId, event.targetId,
                            normalizeEventName(event.eventName), blocks.converted));
                }
                report.add(eventKey, "EventBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                        event.eventType == EventBean.EVENT_TYPE_ACTIVITY || event.eventType == EventBean.EVENT_TYPE_COMPONENT
                                ? "Imported as a typed runtime event binding with a compatibility descriptor."
                                : "Imported as a runtime event descriptor.");
                continue;
            }
            String bindingId = "legacy_" + eventKey;
            bindings.put(bindingId, new CreatorEventBinding(bindingId, event.targetId,
                    normalizeEventName(event.eventName), blocks.converted));
            report.add(eventKey, "EventBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                    "Imported as a typed Creator Runtime event binding.");
        }
        state.put("legacy.deferredEvents", deferredEvents);
        return new Result(base.withRuntimeState(base.getRevision(), state, bindings), report);
    }

    public Result importProjectMetadata(CreatorProjectDocument base, List<ProjectFileBean> files,
                                        List<ProjectLibraryBean> libraries) {
        if (base == null) throw new IllegalArgumentException("base");
        CreatorCompatibilityReport report = new CreatorCompatibilityReport();
        Map<String, Object> state = new LinkedHashMap<>(base.getState());
        List<Object> projectFiles = new ArrayList<>();
        Map<String, Object> projectFileIndex = new LinkedHashMap<>();
        for (ProjectFileBean file : files == null ? Collections.<ProjectFileBean>emptyList() : files) {
            if (file == null || blank(file.fileName)) {
                report.add("unknown", "ProjectFileBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Project file has no stable name and cannot be imported safely.");
                continue;
            }
            Map<String, Object> descriptor = new LinkedHashMap<>();
            descriptor.put("fileType", file.fileType);
            descriptor.put("fileName", file.fileName);
            descriptor.put("orientation", file.orientation);
            descriptor.put("keyboardSetting", file.keyboardSetting);
            descriptor.put("options", file.options);
            descriptor.put("presetName", file.presetName);
            descriptor.put("runtimeKind", projectFileRuntimeKind(file.fileType));
            descriptor.put("xmlName", file.getXmlName());
            Map<String, Object> relationships = new LinkedHashMap<>();
            relationships.put("screenId", file.fileName);
            relationships.put("layoutResource", file.getXmlName());
            if (file.fileType == ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY) {
                descriptor.put("activityName", file.getActivityName());
                descriptor.put("javaName", file.getJavaName());
                descriptor.put("hasToolbar", file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_TOOLBAR));
                descriptor.put("isFullscreen", file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FULLSCREEN));
                descriptor.put("hasFab", file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_FAB));
                descriptor.put("hasDrawer", file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER));
                relationships.put("activityClass", file.getActivityName());
                relationships.put("activitySource", file.getJavaName());
                if (file.hasActivityOption(ProjectFileBean.OPTION_ACTIVITY_DRAWER)) {
                    descriptor.put("drawerName", file.getDrawerName());
                    descriptor.put("drawerXmlName", file.getDrawerXmlName());
                    relationships.put("drawerId", file.getDrawerName());
                    relationships.put("drawerLayoutResource", file.getDrawerXmlName());
                }
            }
            descriptor.put("relationships", relationships);
            projectFiles.add(descriptor);
            projectFileIndex.put(file.fileName, new LinkedHashMap<>(descriptor));
            report.add(file.fileName, "ProjectFileBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                    "Imported as typed Creator Runtime " + projectFileRuntimeKind(file.fileType)
                            + " metadata with its stable layout and screen relationships.");
        }
        state.put("legacy.projectFiles", projectFiles);
        state.put("legacy.projectFileIndex", projectFileIndex);

        List<Object> projectLibraries = new ArrayList<>();
        for (ProjectLibraryBean library : libraries == null ? Collections.<ProjectLibraryBean>emptyList() : libraries) {
            if (library == null) continue;
            if (library.libType == ProjectLibraryBean.PROJECT_LIB_TYPE_LOCAL_LIB
                    || library.libType == ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB) {
                report.add("library:" + library.libType, "ProjectLibraryBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Arbitrary local or native libraries are blocked; they cannot execute in Creator Runtime.");
                continue;
            }
            Map<String, Object> descriptor = new LinkedHashMap<>();
            descriptor.put("libType", library.libType);
            descriptor.put("enabled", library.isEnabled());
            descriptor.put("appId", library.appId);
            descriptor.put("data", library.data);
            descriptor.put("configurations", library.configurations == null
                    ? Collections.<String, Object>emptyMap() : new LinkedHashMap<>(library.configurations));
            projectLibraries.add(descriptor);
            report.add("library:" + library.libType, "ProjectLibraryBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                    "Imported as Creator Runtime integration configuration.");
        }
        state.put("legacy.libraries", projectLibraries);
        return new Result(base.withRuntimeState(base.getRevision(), state, base.getEvents()), report);
    }

    private static String projectFileRuntimeKind(int fileType) {
        switch (fileType) {
            case ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY: return "activity";
            case ProjectFileBean.PROJECT_FILE_TYPE_CUSTOM_VIEW: return "custom_view";
            case ProjectFileBean.PROJECT_FILE_TYPE_DRAWER: return "drawer";
            case ProjectFileBean.PROJECT_FILE_TYPE_FRAGMENT: return "fragment";
            case ProjectFileBean.PROJECT_FILE_TYPE_SHEET: return "sheet";
            case ProjectFileBean.PROJECT_FILE_TYPE_DIALOG_FRAGMENT: return "dialog_fragment";
            default: return "unknown";
        }
    }

    public Result importResources(CreatorProjectDocument base, List<ProjectResourceBean> resources) {
        if (base == null) throw new IllegalArgumentException("base");
        CreatorCompatibilityReport report = new CreatorCompatibilityReport();
        Map<String, Object> state = new LinkedHashMap<>(base.getState());
        List<Object> imported = new ArrayList<>();
        Map<String, Object> images = new LinkedHashMap<>();
        Map<String, Object> videos = new LinkedHashMap<>();
        Map<String, Object> sounds = new LinkedHashMap<>();
        Map<String, Object> fonts = new LinkedHashMap<>();
        for (ProjectResourceBean resource : resources == null ? Collections.<ProjectResourceBean>emptyList() : resources) {
            if (resource == null || blank(resource.resName) || blank(resource.resFullName)) {
                report.add("unknown", "ProjectResourceBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Resource has no stable name or source reference and cannot be imported safely.");
                continue;
            }
            Map<String, Object> descriptor = new LinkedHashMap<>();
            descriptor.put("name", resource.resName);
            descriptor.put("source", resource.resFullName);
            descriptor.put("type", resource.resType);
            descriptor.put("rotate", resource.rotate);
            descriptor.put("flipHorizontal", resource.flipHorizontal);
            descriptor.put("flipVertical", resource.flipVertical);
            descriptor.put("ninePatch", resource.isNinePatch());
            descriptor.put("svg", resource.isSvg());
            descriptor.put("currentSoundPosition", resource.curSoundPosition);
            descriptor.put("totalSoundDuration", resource.totalSoundDuration);
            if (isImage(resource.resFullName)) {
                descriptor.put("kind", "image");
                images.put(resource.resName, new LinkedHashMap<>(descriptor));
            } else if (isVideo(resource.resFullName)) {
                descriptor.put("kind", "video");
                videos.put(resource.resName, new LinkedHashMap<>(descriptor));
            } else if (isSound(resource.resFullName)) {
                descriptor.put("kind", "sound");
                sounds.put(resource.resName, new LinkedHashMap<>(descriptor));
            } else if (isFont(resource.resFullName)) {
                descriptor.put("kind", "font");
                fonts.put(resource.resName, new LinkedHashMap<>(descriptor));
            }
            imported.add(descriptor);
            report.add(resource.resName, "ProjectResourceBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                    "Preserved as runtime resource metadata for live widget and media services.");
        }
        state.put("legacy.resources", imported);
        state.put("legacy.imageResources", images);
        state.put("legacy.videoResources", videos);
        state.put("legacy.soundResources", sounds);
        state.put("legacy.fontResources", fonts);
        return new Result(base.withRuntimeState(base.getRevision(), state, base.getEvents()), report);
    }

    /**
     * Preserves the editable value-resource families stored by legacy Sketchware in
     * {@code files/resource/values{variant}/}. Values remain structured runtime data;
     * they are neither compiled into an APK nor converted into generated Java.
     *
     * <p>Keys may be bare file names such as {@code strings.xml}, or paths such as
     * {@code values-night/colors.xml}. The portion after {@code values} is retained as
     * the Android resource variant key.</p>
     */
    public Result importValueResources(CreatorProjectDocument base, Map<String, String> xmlByPath) {
        if (base == null) throw new IllegalArgumentException("base");
        CreatorCompatibilityReport report = new CreatorCompatibilityReport();
        Map<String, Object> state = new LinkedHashMap<>(base.getState());
        Map<String, Object> strings = valueResourceFamily(state, "legacy.stringResources");
        Map<String, Object> colors = valueResourceFamily(state, "legacy.colorResources");
        Map<String, Object> styles = valueResourceFamily(state, "legacy.styleResources");
        Map<String, Object> themes = valueResourceFamily(state, "legacy.themeResources");
        Map<String, Object> arrays = valueResourceFamily(state, "legacy.arrayResources");
        Map<String, Object> sources = valueResourceFamily(state, "legacy.valueResourceSources");

        for (Map.Entry<String, String> entry : xmlByPath == null
                ? Collections.<String, String>emptyMap().entrySet() : xmlByPath.entrySet()) {
            String sourcePath = entry.getKey() == null ? "" : entry.getKey();
            String fileName = fileName(sourcePath);
            String family = valueResourceFamilyName(fileName);
            if (family == null) {
                report.add(sourcePath.isEmpty() ? "unknown" : sourcePath, "ValueResourceXml",
                        CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Unsupported value-resource file; only strings, colors, styles, themes, and arrays are allowed.");
                continue;
            }
            String xml = entry.getValue() == null ? "" : entry.getValue();
            try {
                Map<String, Object> parsed = parseValueResourceFamily(family, xml);
                String variant = resourceVariant(sourcePath);
                Map<String, Object> target = "strings".equals(family) ? strings
                        : "colors".equals(family) ? colors
                        : "styles".equals(family) ? styles
                        : "themes".equals(family) ? themes : arrays;
                target.put(variant, parsed);
                Map<String, Object> sourceDescriptor = new LinkedHashMap<>();
                sourceDescriptor.put("path", sourcePath);
                sourceDescriptor.put("fileName", fileName);
                sourceDescriptor.put("variant", variant);
                sourceDescriptor.put("family", family);
                sourceDescriptor.put("xml", xml);
                sources.put(family + "@" + variant, sourceDescriptor);
                report.add(fileName + "@" + variant, "ValueResourceXml",
                        CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                        "Imported " + parsed.size() + " " + family + " entries as typed live runtime metadata.");
            } catch (Exception error) {
                report.add(sourcePath.isEmpty() ? fileName : sourcePath, "ValueResourceXml",
                        CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Malformed value-resource XML: " + error.getMessage());
            }
        }
        state.put("legacy.stringResources", strings);
        state.put("legacy.colorResources", colors);
        state.put("legacy.styleResources", styles);
        state.put("legacy.themeResources", themes);
        state.put("legacy.arrayResources", arrays);
        state.put("legacy.valueResourceSources", sources);
        return new Result(base.withRuntimeState(base.getRevision(), state, base.getEvents()), report);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> valueResourceFamily(Map<String, Object> state, String key) {
        Object current = state.get(key);
        return current instanceof Map ? new LinkedHashMap<>((Map<String, Object>) current) : new LinkedHashMap<>();
    }

    private static String fileName(String sourcePath) {
        int separator = Math.max(sourcePath.lastIndexOf('/'), sourcePath.lastIndexOf('\\'));
        return separator < 0 ? sourcePath : sourcePath.substring(separator + 1);
    }

    private static String resourceVariant(String sourcePath) {
        String normalized = sourcePath.replace('\\', '/');
        for (String segment : normalized.split("/")) {
            if ("values".equals(segment)) return "";
            if (segment.startsWith("values-")) return segment.substring("values".length());
        }
        return "";
    }

    private static String valueResourceFamilyName(String fileName) {
        if ("strings.xml".equals(fileName)) return "strings";
        if ("colors.xml".equals(fileName)) return "colors";
        if ("styles.xml".equals(fileName)) return "styles";
        if ("themes.xml".equals(fileName)) return "themes";
        if ("arrays.xml".equals(fileName)) return "arrays";
        return null;
    }

    private static Map<String, Object> parseValueResourceFamily(String family, String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
        factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);
        Document document = factory.newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        Element root = document.getDocumentElement();
        if (root == null || !"resources".equals(root.getTagName())) {
            throw new IllegalArgumentException("Root element must be <resources>.");
        }
        Map<String, Object> parsed = new LinkedHashMap<>();
        NodeList nodes = root.getChildNodes();
        for (int index = 0; index < nodes.getLength(); index++) {
            Node node = nodes.item(index);
            if (node.getNodeType() != Node.ELEMENT_NODE) continue;
            Element element = (Element) node;
            String name = element.getAttribute("name");
            if (blank(name)) continue;
            if ("strings".equals(family) && "string".equals(element.getTagName())) {
                parsed.put(name, element.getTextContent());
            } else if ("colors".equals(family) && "color".equals(element.getTagName())) {
                parsed.put(name, element.getTextContent().trim());
            } else if (("styles".equals(family) || "themes".equals(family)) && "style".equals(element.getTagName())) {
                Map<String, Object> style = new LinkedHashMap<>();
                style.put("parent", element.hasAttribute("parent") ? element.getAttribute("parent") : "");
                Map<String, Object> items = new LinkedHashMap<>();
                NodeList children = element.getChildNodes();
                for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                    Node child = children.item(childIndex);
                    if (child.getNodeType() != Node.ELEMENT_NODE || !"item".equals(child.getNodeName())) continue;
                    Element item = (Element) child;
                    String itemName = item.getAttribute("name");
                    if (!blank(itemName)) items.put(itemName, item.getTextContent().trim());
                }
                style.put("items", items);
                parsed.put(name, style);
            } else if ("arrays".equals(family) && isArrayElement(element.getTagName())) {
                Map<String, Object> array = new LinkedHashMap<>();
                array.put("type", element.getTagName());
                List<Object> values = new ArrayList<>();
                NodeList children = element.getChildNodes();
                for (int childIndex = 0; childIndex < children.getLength(); childIndex++) {
                    Node child = children.item(childIndex);
                    if (child.getNodeType() == Node.ELEMENT_NODE && "item".equals(child.getNodeName())) {
                        values.add(child.getTextContent());
                    }
                }
                array.put("items", values);
                parsed.put(name, array);
            }
        }
        return parsed;
    }

    private static boolean isArrayElement(String tagName) {
        return "array".equals(tagName) || "string-array".equals(tagName) || "integer-array".equals(tagName);
    }

    private static final class BlockConversion {
        final List<CreatorRuntimeBlock> converted = new ArrayList<>();
        final List<String> unsupported = new ArrayList<>();
        final Map<String, List<CreatorRuntimeBlock>> timerCallbacks = new LinkedHashMap<>();
    }

    private BlockConversion convertBlocks(List<BlockBean> blocks, Map<String, Object> componentDescriptors) {
        BlockConversion result = new BlockConversion();
        Map<Integer, BlockBean> byId = new LinkedHashMap<>();
        java.util.Set<Integer> referenced = new java.util.LinkedHashSet<>();
        java.util.Set<Integer> reporterReferences = new java.util.LinkedHashSet<>();
        for (BlockBean block : blocks == null ? Collections.<BlockBean>emptyList() : blocks) {
            if (block == null) continue;
            try { byId.put(Integer.parseInt(block.id), block); } catch (NumberFormatException ignored) {
                result.unsupported.add("invalid block id");
            }
            if (block.nextBlock >= 0) referenced.add(block.nextBlock);
            if (block.subStack1 >= 0) referenced.add(block.subStack1);
            if (block.subStack2 >= 0) referenced.add(block.subStack2);
            for (String parameter : block.parameters == null ? Collections.<String>emptyList() : block.parameters) {
                if (parameter == null || !parameter.trim().startsWith("@")) continue;
                try {
                    int reporterId = Integer.parseInt(parameter.trim().substring(1));
                    referenced.add(reporterId);
                    reporterReferences.add(reporterId);
                } catch (NumberFormatException ignored) {
                    result.unsupported.add("invalid reporter reference " + parameter);
                }
            }
        }
        java.util.Set<Integer> visited = new java.util.LinkedHashSet<>();
        for (Map.Entry<Integer, BlockBean> entry : byId.entrySet()) {
            if (!referenced.contains(entry.getKey())) convertChain(entry.getValue(), byId, visited, result.converted,
                    result.unsupported, result.timerCallbacks, componentDescriptors);
        }
        visited.addAll(reporterReferences);
        for (Map.Entry<Integer, BlockBean> entry : byId.entrySet()) {
            if (!visited.contains(entry.getKey())) result.unsupported.add("orphan block " + entry.getKey());
        }
        return result;
    }

    private void convertChain(BlockBean start, Map<Integer, BlockBean> byId, java.util.Set<Integer> visited,
                              List<CreatorRuntimeBlock> target, List<String> unsupported,
                              Map<String, List<CreatorRuntimeBlock>> timerCallbacks,
                              Map<String, Object> componentDescriptors) {
        BlockBean current = start;
        while (current != null) {
            int id;
            try { id = Integer.parseInt(current.id); } catch (NumberFormatException ignored) {
                unsupported.add("invalid block id"); return;
            }
            if (!visited.add(id)) { unsupported.add("cyclic block graph at " + id); return; }
            CreatorRuntimeBlock converted = convertBlock(current, byId, visited, unsupported, timerCallbacks, componentDescriptors);
            if (converted != null) target.add(converted);
            current = byId.get(current.nextBlock);
        }
    }

    private CreatorRuntimeBlock convertBlock(BlockBean block, Map<Integer, BlockBean> byId,
                                             java.util.Set<Integer> visited, List<String> unsupported,
                                             Map<String, List<CreatorRuntimeBlock>> timerCallbacks,
                                             Map<String, Object> componentDescriptors) {
        if (blank(block.opCode)) { unsupported.add("empty"); return null; }
        String op = block.opCode.trim().toLowerCase(Locale.ROOT);
        List<String> values = block.parameters == null ? Collections.<String>emptyList() : block.parameters;
        Map<String, Object> payload = new LinkedHashMap<>();
        if ("if_state_equals".equals(op) || "ifstateequals".equals(op)) {
            if (values.size() < 2 || block.subStack1 < 0) { unsupported.add(block.opCode); return null; }
            List<CreatorRuntimeBlock> thenBlocks = new ArrayList<>();
            List<CreatorRuntimeBlock> elseBlocks = new ArrayList<>();
            BlockBean thenStart = byId.get(block.subStack1);
            if (thenStart == null) { unsupported.add(block.opCode + " (missing then substack)"); return null; }
            convertChain(thenStart, byId, visited, thenBlocks, unsupported, timerCallbacks, componentDescriptors);
            if (block.subStack2 >= 0) {
                BlockBean elseStart = byId.get(block.subStack2);
                if (elseStart == null) { unsupported.add(block.opCode + " (missing else substack)"); return null; }
                convertChain(elseStart, byId, visited, elseBlocks, unsupported, timerCallbacks, componentDescriptors);
            }
            payload.put("stateId", values.get(0));
            payload.put("equals", values.get(1));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.IF_STATE_EQUALS, payload, thenBlocks, elseBlocks);
        }
        if ("if".equals(op) || "ifelse".equals(op)) {
            if (values.isEmpty() || block.subStack1 < 0) { unsupported.add(block.opCode); return null; }
            List<CreatorRuntimeBlock> thenBlocks = new ArrayList<>();
            List<CreatorRuntimeBlock> elseBlocks = new ArrayList<>();
            BlockBean thenStart = byId.get(block.subStack1);
            if (thenStart == null) { unsupported.add(block.opCode + " (missing then substack)"); return null; }
            convertChain(thenStart, byId, visited, thenBlocks, unsupported, timerCallbacks, componentDescriptors);
            if ("ifelse".equals(op) && block.subStack2 >= 0) {
                BlockBean elseStart = byId.get(block.subStack2);
                if (elseStart == null) { unsupported.add(block.opCode + " (missing else substack)"); return null; }
                convertChain(elseStart, byId, visited, elseBlocks, unsupported, timerCallbacks, componentDescriptors);
            }
            String condition = values.get(0).trim();
            if (condition.startsWith("@")) {
                Map<String, Object> expression = expression(condition, byId, new java.util.LinkedHashSet<Integer>());
                if (expression == null) { unsupported.add(block.opCode + " (invalid reporter expression)"); return null; }
                payload.put("expression", expression);
            } else if ("true".equalsIgnoreCase(condition) || "false".equalsIgnoreCase(condition)) payload.put("constant", Boolean.valueOf(condition));
            else payload.put("stateId", condition);
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.IF_BOOLEAN, payload, thenBlocks, elseBlocks);
        }
        if ("forever".equals(op)) {
            if (block.subStack1 < 0 || block.subStack2 >= 0) { unsupported.add(block.opCode); return null; }
            BlockBean bodyStart = byId.get(block.subStack1);
            if (bodyStart == null) { unsupported.add(block.opCode + " (missing forever substack)"); return null; }
            List<CreatorRuntimeBlock> body = new ArrayList<>();
            convertChain(bodyStart, byId, visited, body, unsupported, timerCallbacks, componentDescriptors);
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.FOREVER, payload, body,
                    Collections.<CreatorRuntimeBlock>emptyList());
        }
        if ("break".equals(op)) {
            if (!values.isEmpty() || block.subStack1 >= 0 || block.subStack2 >= 0) { unsupported.add(block.opCode); return null; }
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.BREAK, payload);
        }
        if ("repeat".equals(op)) {
            if (values.isEmpty() || block.subStack1 < 0) { unsupported.add(block.opCode); return null; }
            BlockBean bodyStart = byId.get(block.subStack1);
            if (bodyStart == null) { unsupported.add(block.opCode + " (missing repeat substack)"); return null; }
            List<CreatorRuntimeBlock> body = new ArrayList<>();
            convertChain(bodyStart, byId, visited, body, unsupported, timerCallbacks, componentDescriptors);
            String count = values.get(0).trim();
            if (count.startsWith("@")) {
                Map<String, Object> expression = expression(count, byId, new java.util.LinkedHashSet<Integer>());
                if (expression == null) { unsupported.add(block.opCode + " (invalid count expression)"); return null; }
                payload.put("countExpression", expression);
            } else if (count.matches("-?\\d+")) payload.put("count", count);
            else payload.put("countStateId", count);
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.REPEAT, payload, body, Collections.<CreatorRuntimeBlock>emptyList());
        }
        boolean timerWithCallback = "timerafter".equals(op) || "timerevery".equals(op);
        boolean firebaseChildrenWithCallback = "firebasegetchildren".equals(op);
        boolean dialogButtonWithCallback = "dialogokbutton".equals(op) || "dialogcancelbutton".equals(op)
                || "dialogneutralbutton".equals(op);
        if ((block.subStack1 >= 0 || block.subStack2 >= 0) && !timerWithCallback && !firebaseChildrenWithCallback
                && !dialogButtonWithCallback) {
            unsupported.add(block.opCode + " (control flow)"); return null;
        }
        if (timerWithCallback && block.subStack2 >= 0) { unsupported.add(block.opCode + " (unexpected else substack)"); return null; }
        if (firebaseChildrenWithCallback && block.subStack2 >= 0) { unsupported.add(block.opCode + " (unexpected else substack)"); return null; }
        if (dialogButtonWithCallback && block.subStack2 >= 0) { unsupported.add(block.opCode + " (unexpected else substack)"); return null; }
        if ("timerafter".equals(op) || "timerevery".equals(op)) {
            int required = "timerafter".equals(op) ? 2 : 3;
            if (values.size() < required) { unsupported.add(block.opCode); return null; }
            if (block.subStack1 >= 0) {
                BlockBean callbackStart = byId.get(block.subStack1);
                if (callbackStart == null) { unsupported.add(block.opCode + " (missing timer substack)"); return null; }
                List<CreatorRuntimeBlock> callback = new ArrayList<>();
                convertChain(callbackStart, byId, visited, callback, unsupported, timerCallbacks, componentDescriptors);
                timerCallbacks.put(values.get(0), callback);
            }
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("timerId", values.get(0));
            arguments.put("action", "timerafter".equals(op) ? "after" : "every");
            arguments.put("delayMs", values.get(1));
            if ("timerevery".equals(op)) arguments.put("periodMs", values.get(2));
            return serviceCall("timer", arguments);
        } else if ("timercancel".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("timer", CreatorRuntimeServiceArguments.output("timerId", values.get(0), "action", "cancel"));
        } else if ("vibratoraction".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("vibrator", CreatorRuntimeServiceArguments.output("durationMs", values.get(1)));
        } else if ("increaseint".equals(op) || "decreaseint".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            payload.put("stateId", values.get(0));
            payload.put("delta", "increaseint".equals(op) ? 1L : -1L);
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.INCREMENT_STATE, payload);
        } else if ("setlistmap".equals(op)) {
            return listMapPutAt(block, values, unsupported, byId);
        } else if ("insertmaptolist".equals(op)) {
            return listMapInsert(block, values, unsupported, byId);
        } else if ("getmapinlist".equals(op)) {
            return listMapGet(block, values, unsupported, byId);
        } else if ("setatposliststr".equals(op) || "setatposlistnum".equals(op) || "setmapatposlistmap".equals(op)) {
            return listSetAt(block, values, unsupported, byId);
        } else if ("addlistint".equals(op) || "addliststr".equals(op) || "addlistmap".equals(op)) {
            return listMutation(block, values, "add", unsupported, byId);
        } else if ("insertlistint".equals(op) || "insertliststr".equals(op) || "insertlistmap".equals(op)) {
            return listMutation(block, values, "insert", unsupported, byId);
        } else if ("deletelist".equals(op)) {
            return listMutation(block, values, "remove_at", unsupported, byId);
        } else if ("clearlist".equals(op)) {
            return listMutation(block, values, "clear", unsupported, byId);
        } else if ("listaddall".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            payload.put("stateId", values.get(0));
            payload.put("action", "add_all");
            payload.put("sourceStateId", values.get(1));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, payload);
        } else if ("mapgetallkeys".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            payload.put("stateId", values.get(1));
            payload.put("action", "replace_map_keys");
            payload.put("sourceMapStateId", values.get(0));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, payload);
        } else if ("strtomap".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            payload.put("stateId", values.get(1));
            payload.put("action", "replace_json");
            putExpressionOrValue(payload, "json", "jsonExpression", values.get(0), byId, unsupported, block.opCode);
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.MAP_MUTATE, payload);
        } else if ("strtolistmap".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            payload.put("stateId", values.get(1));
            payload.put("action", "replace_json_maps");
            putExpressionOrValue(payload, "json", "jsonExpression", values.get(0), byId, unsupported, block.opCode);
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, payload);
        } else if ("mapcreatenew".equals(op)) {
            return mapMutation(block, values, "create", unsupported, byId);
        } else if ("mapput".equals(op)) {
            return mapMutation(block, values, "put", unsupported, byId);
        } else if ("mapremovekey".equals(op)) {
            return mapMutation(block, values, "remove", unsupported, byId);
        } else if ("mapclear".equals(op)) {
            return mapMutation(block, values, "clear", unsupported, byId);
        } else if ("intentsetaction".equals(op)) {
            return intentCall(block, values, "configure_action", unsupported);
        } else if ("intentsetdata".equals(op)) {
            return intentCall(block, values, "configure_data", unsupported);
        } else if ("intentsetscreen".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0),
                    "action", "configure_screen", "screenId", values.get(1)));
        } else if ("intentputextra".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0),
                    "action", "put_extra", "key", values.get(1), "value", values.get(2)));
        } else if ("intentsetflags".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0),
                    "action", "set_flags", "flag", values.get(1)));
        } else if ("startactivity".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0), "action", "start"));
        } else if ("finishactivity".equals(op)) {
            return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", "runtime", "action", "finish"));
        } else if ("dialogsettitle".equals(op)) {
            return dialogCall(block, values, "set_title", unsupported);
        } else if ("dialogsetmessage".equals(op)) {
            return dialogCall(block, values, "set_message", unsupported);
        } else if ("dialogshow".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("dialog", CreatorRuntimeServiceArguments.output("dialogId", values.get(0), "action", "show"));
        } else if ("dialogdismiss".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("dialog", CreatorRuntimeServiceArguments.output("dialogId", values.get(0), "action", "dismiss"));
        } else if ("dialogokbutton".equals(op) || "dialogcancelbutton".equals(op)
                || "dialogneutralbutton".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            String button = "dialogokbutton".equals(op) ? "positive"
                    : "dialogcancelbutton".equals(op) ? "negative" : "neutral";
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("dialogId", values.get(0));
            arguments.put("action", "set_" + button + "_button");
            arguments.put("label", values.get(1));
            if (block.subStack1 >= 0) {
                BlockBean callbackStart = byId.get(block.subStack1);
                if (callbackStart == null) { unsupported.add(block.opCode + " (missing callback substack)"); return null; }
                String callbackId = block.id + "_" + button;
                List<CreatorRuntimeBlock> callback = new ArrayList<>();
                convertChain(callbackStart, byId, visited, callback, unsupported, timerCallbacks, componentDescriptors);
                timerCallbacks.put("dialog_button:" + callbackId, callback);
                arguments.put("callbackTargetId", "legacy_dialog_button_callback_" + callbackId);
            }
            return serviceCall("dialog", arguments);
        } else if ("mediaplayercreate".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("media", CreatorRuntimeServiceArguments.output("id", values.get(0),
                    "action", "load_resource", "resourceName", values.get(1)));
        } else if ("mediaplayerstart".equals(op) || "mediaplayerpause".equals(op)
                || "mediaplayerstop".equals(op) || "mediaplayerrelease".equals(op)
                || "mediaplayerreset".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            String action = "mediaplayerstart".equals(op) ? "play" : "mediaplayerpause".equals(op) ? "pause"
                    : "mediaplayerstop".equals(op) ? "stop" : "release";
            return serviceCall("media", CreatorRuntimeServiceArguments.output("id", values.get(0), "action", action));
        } else if ("mediaplayerseek".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("media", CreatorRuntimeServiceArguments.output("id", values.get(0),
                    "action", "seek", "positionMs", values.get(1)));
        } else if ("mediaplayersetlooping".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("media", CreatorRuntimeServiceArguments.output("id", values.get(0),
                    "action", "set_looping", "looping", values.get(1)));
        } else if ("settitle".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("ui", CreatorRuntimeServiceArguments.output("action", "set_title", "title", values.get(0)));
        } else if ("copytoclipboard".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("ui", CreatorRuntimeServiceArguments.output("action", "copy_text", "text", values.get(0)));
        } else if ("gyroscopestartlisten".equals(op) || "gyroscopestoplisten".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("gyroscope", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "gyroscopestartlisten".equals(op) ? "start" : "stop"));
        } else if ("locationmanagerrequestlocationupdates".equals(op)) {
            if (values.size() < 4) { unsupported.add(block.opCode); return null; }
            return serviceCall("location", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "start", "provider", normalizeLocationProvider(values.get(1)),
                    "intervalMs", values.get(2), "distanceMeters", values.get(3)));
        } else if ("locationmanagerremoveupdates".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("location", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "stop"));
        } else if ("camerastarttakepicture".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("camera", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "capture"));
        } else if ("filepickerstartpickfiles".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("file_picker", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "pick"));
        } else if ("texttospeechsetpitch".equals(op) || "texttospeechsetspeechrate".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("text_to_speech", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "texttospeechsetpitch".equals(op) ? "set_pitch" : "set_rate",
                    "value", values.get(1)));
        } else if ("texttospeechspeak".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("text_to_speech", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "speak", "text", values.get(1)));
        } else if ("texttospeechstop".equals(op) || "texttospeechshutdown".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("text_to_speech", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "texttospeechstop".equals(op) ? "stop" : "shutdown"));
        } else if ("speechtotextstartlistening".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("speech_to_text", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "listen"));
        } else if ("speechtotextstoplistening".equals(op) || "speechtotextshutdown".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("speech_to_text", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "speechtotextstoplistening".equals(op) ? "stop" : "shutdown"));
        } else if ("fileutilwrite".equals(op)) {
            return fileCall(block, values, "write", 2, unsupported);
        } else if ("fileutilcopy".equals(op)) {
            return fileCall(block, values, "copy", 2, unsupported);
        } else if ("fileutilcopydir".equals(op)) {
            return fileCall(block, values, "copy_dir", 2, unsupported);
        } else if ("fileutilmove".equals(op)) {
            return fileCall(block, values, "move", 2, unsupported);
        } else if ("fileutildelete".equals(op)) {
            return fileCall(block, values, "delete", 1, unsupported);
        } else if ("fileutilmakedir".equals(op)) {
            return fileCall(block, values, "make_dir", 1, unsupported);
        } else if ("fileutillistdir".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("file", CreatorRuntimeServiceArguments.output(
                    "action", "list_dir", "path", values.get(0), "resultStateId", values.get(1), "resultKey", "entries"));
        } else if ("objectanimatorsettarget".equals(op)) {
            return animatorCall(block, values, "set_target", 2, unsupported);
        } else if ("objectanimatorsetproperty".equals(op)) {
            return animatorCall(block, values, "set_property", 2, unsupported);
        } else if ("objectanimatorsetvalue".equals(op)) {
            return animatorCall(block, values, "set_value", 2, unsupported);
        } else if ("objectanimatorsetfromto".equals(op)) {
            return animatorCall(block, values, "set_from_to", 3, unsupported);
        } else if ("objectanimatorsetduration".equals(op)) {
            return animatorCall(block, values, "set_duration", 2, unsupported);
        } else if ("objectanimatorsetrepeatmode".equals(op)) {
            return animatorCall(block, values, "set_repeat_mode", 2, unsupported);
        } else if ("objectanimatorsetrepeatcount".equals(op)) {
            return animatorCall(block, values, "set_repeat_count", 2, unsupported);
        } else if ("objectanimatorsetinterpolator".equals(op)) {
            return animatorCall(block, values, "set_interpolator", 2, unsupported);
        } else if ("objectanimatorstart".equals(op) || "objectanimatorcancel".equals(op)) {
            return animatorCall(block, values, "objectanimatorstart".equals(op) ? "start" : "cancel", 1, unsupported);
        } else if ("firebaseauthcreateuser".equals(op) || "firebaseauthsigninuser".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase_auth", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "firebaseauthcreateuser".equals(op) ? "register" : "sign_in",
                    "email", values.get(1), "password", values.get(2)));
        } else if ("firebaseauthsigninanonymously".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase_auth", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "anonymous"));
        } else if ("firebaseauthresetpassword".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase_auth", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "reset_password", "email", values.get(1)));
        } else if ("firebaseauthsignoutuser".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase_auth", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "sign_out"));
        } else if ("firebasestorageuploadfile".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase_storage", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "upload_file", "filePath", values.get(1), "path", values.get(2)));
        } else if ("firebasestoragedownloadfile".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase_storage", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "download_file", "url", values.get(1), "filePath", values.get(2)));
        } else if ("firebasestoragedelete".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase_storage", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "delete_url", "url", values.get(1)));
        } else if ("firebasegetchildren".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("componentId", values.get(0));
            arguments.put("action", "get_children");
            arguments.put("path", firebasePath(componentDescriptors, values.get(0), null));
            arguments.put("resultStateId", values.get(1));
            if (block.subStack1 >= 0) {
                BlockBean callbackStart = byId.get(block.subStack1);
                if (callbackStart == null) { unsupported.add(block.opCode + " (missing callback substack)"); return null; }
                String callbackId = String.valueOf(block.id);
                List<CreatorRuntimeBlock> callback = new ArrayList<>();
                convertChain(callbackStart, byId, visited, callback, unsupported, timerCallbacks, componentDescriptors);
                timerCallbacks.put("firebase_children:" + callbackId, callback);
                arguments.put("callbackTargetId", "legacy_firebase_children_callback_" + callbackId);
            }
            return serviceCall("firebase", arguments);
        } else if ("firebasedelete".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return firebaseCall(values.get(0), "remove", firebasePath(componentDescriptors, values.get(0), values.get(1)));
        } else if ("firebaseadd".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "update",
                    "path", firebasePath(componentDescriptors, values.get(0), values.get(1)),
                    "valueStateId", values.get(2)));
        } else if ("firebasepush".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("firebase", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "push_update",
                    "path", firebasePath(componentDescriptors, values.get(0), null),
                    "valueStateId", values.get(1)));
        } else if ("firebasestartlisten".equals(op) || "firebasestoplisten".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return firebaseCall(values.get(0), "firebasestartlisten".equals(op) ? "listen" : "stop_listen",
                    firebasePath(componentDescriptors, values.get(0), null));
        } else if ("datepickerdialogshow".equals(op)) {
            return serviceCall("date_picker", CreatorRuntimeServiceArguments.output("action", "show"));
        } else if ("timepickerdialogshow".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("time_picker", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "show"));
        } else if ("calendargetnow".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return calendarCall(values.get(0), "reset", null, null);
        } else if ("calendaradd".equals(op) || "calendarset".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return calendarCall(values.get(0), "calendaradd".equals(op) ? "add" : "set", values.get(1), values.get(2));
        } else if ("calendarsettime".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return calendarCall(values.get(0), "set_time", "timestamp", values.get(1));
        } else if ("filesetfilename".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return storageCall(values.get(0), "configure", null, values.get(1), null, componentDescriptors);
        } else if ("filesetdata".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return storageCall(values.get(0), "set", values.get(1), values.get(2), null, componentDescriptors);
        } else if ("fileremovedata".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return storageCall(values.get(0), "remove", values.get(1), null, null, componentDescriptors);
        } else if ("requestnetworksetparams".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("http", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "set_params", "paramsStateId", values.get(1), "requestType", values.get(2)));
        } else if ("requestnetworksetheaders".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("http", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "set_headers", "headersStateId", values.get(1)));
        } else if ("requestnetworkstartrequestnetwork".equals(op)) {
            if (values.size() < 4) { unsupported.add(block.opCode); return null; }
            return serviceCall("http", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "start", "method", values.get(1), "url", values.get(2), "tag", values.get(3)));
        } else if ("progressdialogsettitle".equals(op) || "progressdialogsetmessage".equals(op)
                || "progressdialogsetmax".equals(op) || "progressdialogsetprogress".equals(op)
                || "progressdialogsetcancelable".equals(op) || "progressdialogsetcanceled".equals(op)
                || "progressdialogsetcanceledoutside".equals(op) || "progressdialogsetstyle".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            String action = "progressdialogsettitle".equals(op) ? "progress_set_title"
                    : "progressdialogsetmessage".equals(op) ? "progress_set_message"
                    : "progressdialogsetmax".equals(op) ? "progress_set_max"
                    : "progressdialogsetprogress".equals(op) ? "progress_set_value"
                    : "progressdialogsetcancelable".equals(op) ? "progress_set_cancelable"
                    : "progressdialogsetstyle".equals(op) ? "progress_set_style" : "progress_set_cancel_on_touch_outside";
            return serviceCall("dialog", CreatorRuntimeServiceArguments.output(
                    "dialogId", values.get(0), "action", action, "value", values.get(1)));
        } else if ("progressdialogshow".equals(op) || "progressdialogdismiss".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("dialog", CreatorRuntimeServiceArguments.output(
                    "dialogId", values.get(0), "action", "progressdialogshow".equals(op) ? "show_progress" : "dismiss_progress"));
        } else if ("soundpoolcreate".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("media", CreatorRuntimeServiceArguments.output(
                    "id", values.get(0), "action", "sound_create", "maxStreams", values.get(1)));
        } else if ("soundpoolload".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("media", CreatorRuntimeServiceArguments.output(
                    "id", values.get(0), "action", "sound_load_name", "resourceName", values.get(1)));
        } else if ("soundpoolstreamplay".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("media", CreatorRuntimeServiceArguments.output(
                    "id", values.get(0), "action", "sound_play_stream", "soundId", values.get(1), "loop", values.get(2)));
        } else if ("soundpoolstreamstop".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("media", CreatorRuntimeServiceArguments.output(
                    "id", values.get(0), "action", "sound_stop_stream", "streamId", values.get(1)));
        } else if ("bluetoothconnectreadyconnection".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return bluetoothCall(values.get(0), "ready_connection", null, null, values.get(1));
        } else if ("bluetoothconnectreadyconnectiontouuid".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return bluetoothCall(values.get(0), "ready_connection", values.get(1), null, values.get(2));
        } else if ("bluetoothconnectstartconnection".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return bluetoothCall(values.get(0), "start_connection", null, values.get(1), values.get(2));
        } else if ("bluetoothconnectstartconnectiontouuid".equals(op)) {
            if (values.size() < 4) { unsupported.add(block.opCode); return null; }
            return bluetoothCall(values.get(0), "start_connection", values.get(1), values.get(2), values.get(3));
        } else if ("bluetoothconnectstopconnection".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return bluetoothCall(values.get(0), "stop_connection", null, null, values.get(1));
        } else if ("bluetoothconnectsenddata".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("bluetooth", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "send_data", "data", values.get(1), "tag", values.get(2)));
        } else if ("bluetoothconnectactivatebluetooth".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("bluetooth", CreatorRuntimeServiceArguments.output("componentId", values.get(0), "action", "request_enable"));
        } else if ("bluetoothconnectgetpaireddevices".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("bluetooth", CreatorRuntimeServiceArguments.output(
                    "componentId", values.get(0), "action", "paired_devices", "resultStateId", values.get(1), "resultKey", "devices"));
        } else if ("listsetdata".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "list_set_data", "itemsStateId", values.get(1)));
        } else if ("listrefresh".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "list_refresh"));
        } else if ("listsetitemchecked".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "list_set_item_checked",
                    "position", values.get(1), "checked", values.get(2)));
        } else if ("listgetcheckedpositions".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "list_checked_positions",
                    "resultStateId", values.get(1), "resultKey", "positions"));
        } else if ("listsmoothscrollto".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "list_smooth_scroll_to", "position", values.get(1)));
        } else if ("spnsetdata".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "spinner_set_data", "itemsStateId", values.get(1)));
        } else if ("spnrefresh".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "spinner_refresh"));
        } else if ("requestfocus".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "request_focus"));
        } else if ("progressbarsetindeterminate".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "progress_set_indeterminate", "indeterminate", values.get(1)));
        } else if ("setcolorfilter".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "image_set_color_filter", "color", values.get(1)));
        } else if ("webviewgoback".equals(op) || "webviewgoforward".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "webviewgoback".equals(op) ? "web_go_back" : "web_go_forward"));
        } else if ("spnsetselection".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "spinner_set_selection", "position", values.get(1)));
        } else if ("webviewsetcachemode".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", "web_set_cache_mode", "cacheMode", values.get(1)));
        } else if ("webviewclearcache".equals(op) || "webviewclearhistory".equals(op)
                || "webviewstoploading".equals(op) || "webviewzoomin".equals(op) || "webviewzoomout".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            String action = "webviewclearcache".equals(op) ? "web_clear_cache"
                    : "webviewclearhistory".equals(op) ? "web_clear_history"
                    : "webviewstoploading".equals(op) ? "web_stop_loading"
                    : "webviewzoomin".equals(op) ? "web_zoom_in" : "web_zoom_out";
            return serviceCall("widget", CreatorRuntimeServiceArguments.output("widgetId", values.get(0), "action", action));
        } else if ("calendarviewsetdate".equals(op) || "calendarviewsetmindate".equals(op)
                || "calnedarviewsetmaxdate".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            String action = "calendarviewsetdate".equals(op) ? "calendar_set_date"
                    : "calendarviewsetmindate".equals(op) ? "calendar_set_min_date" : "calendar_set_max_date";
            return serviceCall("widget", CreatorRuntimeServiceArguments.output(
                    "widgetId", values.get(0), "action", action, "timestamp", values.get(1)));
        }
        if ("settext".equals(op) || "set_text".equals(op)) {
            return widgetProperty(block, values, "text", unsupported, byId);
        } else if ("settypeface".equals(op)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            payload.put("widgetId", values.get(0));
            payload.put("property", "typeface");
            payload.put("value", CreatorRuntimeServiceArguments.output("font", values.get(1), "style", values.get(2)));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY, payload);
        } else if ("setchecked".equals(op) || "set_checked".equals(op)) {
            return widgetProperty(block, values, "checked", unsupported, byId);
        } else if ("setenable".equals(op)) {
            return widgetProperty(block, values, "enabled", unsupported);
        } else if ("setvisible".equals(op)) {
            return widgetProperty(block, values, "visible", unsupported);
        } else if ("setclickable".equals(op)) {
            return widgetProperty(block, values, "clickable", unsupported);
        } else if ("sethint".equals(op)) {
            return widgetProperty(block, values, "hint", unsupported, byId);
        } else if ("settextcolor".equals(op)) {
            return widgetProperty(block, values, "textColor", unsupported, byId);
        } else if ("settextsize".equals(op)) {
            return widgetProperty(block, values, "textSize", unsupported, byId);
        } else if ("sethinttextcolor".equals(op)) {
            return widgetProperty(block, values, "hintTextColor", unsupported);
        } else if ("setbgcolor".equals(op)) {
            return widgetProperty(block, values, "backgroundColor", unsupported);
        } else if ("setbgresource".equals(op)) {
            return widgetProperty(block, values, "backgroundResource", unsupported);
        } else if ("setalpha".equals(op)) {
            return widgetProperty(block, values, "alpha", unsupported);
        } else if ("setrotate".equals(op)) {
            return widgetProperty(block, values, "rotation", unsupported);
        } else if ("settranslationx".equals(op)) {
            return widgetProperty(block, values, "translationX", unsupported);
        } else if ("settranslationy".equals(op)) {
            return widgetProperty(block, values, "translationY", unsupported);
        } else if ("setscalex".equals(op)) {
            return widgetProperty(block, values, "scaleX", unsupported);
        } else if ("setscaley".equals(op)) {
            return widgetProperty(block, values, "scaleY", unsupported);
        } else if ("setimage".equals(op)) {
            return widgetProperty(block, values, "resourceName", unsupported);
        } else if ("setimagefilepath".equals(op)) {
            return widgetProperty(block, values, "filePath", unsupported);
        } else if ("setimageurl".equals(op)) {
            return widgetProperty(block, values, "url", unsupported);
        } else if ("seekbarsetmax".equals(op)) {
            return widgetProperty(block, values, "max", unsupported);
        } else if ("seekbarsetprogress".equals(op)) {
            return widgetProperty(block, values, "progress", unsupported);
        } else if ("spnsetselection".equals(op)) {
            return widgetProperty(block, values, "selectedIndex", unsupported);
        } else if ("webviewloadurl".equals(op)) {
            return widgetProperty(block, values, "url", unsupported);
        } else if ("calendarviewsetdate".equals(op)) {
            return widgetProperty(block, values, "date", unsupported);
        } else if ("setvar".equals(op) || "set_var".equals(op)
                || "setvarboolean".equals(op) || "setvarint".equals(op) || "setvarstring".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            payload.put("stateId", values.get(0));
            if (values.get(1).trim().startsWith("@")) {
                Map<String, Object> expression = expression(values.get(1), byId, new java.util.LinkedHashSet<Integer>());
                if (expression == null) { unsupported.add(block.opCode + " (invalid value expression)"); return null; }
                payload.put("expression", expression);
            } else payload.put("value", values.get(1));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, payload);
        } else if ("showmessage".equals(op) || "show_message".equals(op) || "toast".equals(op)
                || "dotoast".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            payload.put("message", values.get(0));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SHOW_MESSAGE, payload);
        } else if ("navigate".equals(op) || "open_screen".equals(op)) {
            if (values.isEmpty()) { unsupported.add(block.opCode); return null; }
            payload.put("screenId", values.get(0));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.NAVIGATE, payload);
        } else if ("runtime_service".equals(op) || "service_call".equals(op)) {
            if (values.isEmpty() || !CreatorRuntimeServiceCatalog.defaults().supports(values.get(0))) {
                unsupported.add(block.opCode); return null;
            }
            payload.put("serviceId", values.get(0));
            payload.put("arguments", Collections.<String, Object>emptyMap());
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL, payload);
        }
        unsupported.add(block.opCode);
        return null;
    }

    private static CreatorRuntimeBlock serviceCall(String serviceId, Map<String, Object> arguments) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("serviceId", serviceId);
        payload.put("arguments", new LinkedHashMap<>(arguments));
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL, payload);
    }

    private static CreatorRuntimeBlock listMutation(BlockBean block, List<String> values, String action,
                                                    List<String> unsupported, Map<Integer, BlockBean> byId) {
        int required = "add".equals(action) ? 2 : "clear".equals(action) ? 1 : 2;
        if (values.size() < required) { unsupported.add(block.opCode); return null; }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stateId", values.get(0));
        payload.put("action", action);
        if ("add".equals(action)) putExpressionOrValue(payload, "value", "valueExpression", values.get(1), byId, unsupported, block.opCode);
        else if ("insert".equals(action)) {
            if (values.size() < 3) { unsupported.add(block.opCode); return null; }
            putExpressionOrValue(payload, "index", "indexExpression", values.get(1), byId, unsupported, block.opCode);
            putExpressionOrValue(payload, "value", "valueExpression", values.get(2), byId, unsupported, block.opCode);
        } else if ("remove_at".equals(action)) putExpressionOrValue(payload, "index", "indexExpression", values.get(1), byId, unsupported, block.opCode);
        if (!unsupported.isEmpty() && unsupported.get(unsupported.size() - 1).contains("invalid reporter expression")) return null;
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, payload);
    }

    /** Legacy setListMap order is key, value, index, list. */
    private static CreatorRuntimeBlock listMapPutAt(BlockBean block, List<String> values,
                                                    List<String> unsupported, Map<Integer, BlockBean> byId) {
        if (values.size() < 4) { unsupported.add(block.opCode); return null; }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stateId", values.get(3));
        payload.put("action", "map_put_at");
        putExpressionOrValue(payload, "key", "keyExpression", values.get(0), byId, unsupported, block.opCode);
        putExpressionOrValue(payload, "value", "valueExpression", values.get(1), byId, unsupported, block.opCode);
        putExpressionOrValue(payload, "index", "indexExpression", values.get(2), byId, unsupported, block.opCode);
        if (!unsupported.isEmpty() && unsupported.get(unsupported.size() - 1).contains("invalid reporter expression")) return null;
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, payload);
    }

    /** Legacy insertMapToList order is map, index, list. */
    private static CreatorRuntimeBlock listMapInsert(BlockBean block, List<String> values,
                                                     List<String> unsupported, Map<Integer, BlockBean> byId) {
        if (values.size() < 3) { unsupported.add(block.opCode); return null; }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stateId", values.get(2));
        payload.put("action", "insert");
        putExpressionOrValue(payload, "value", "valueExpression", values.get(0), byId, unsupported, block.opCode);
        putExpressionOrValue(payload, "index", "indexExpression", values.get(1), byId, unsupported, block.opCode);
        if (!unsupported.isEmpty() && unsupported.get(unsupported.size() - 1).contains("invalid reporter expression")) return null;
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, payload);
    }

    /** Legacy getMapInList order is index, list, destination map. */
    private static CreatorRuntimeBlock listMapGet(BlockBean block, List<String> values,
                                                  List<String> unsupported, Map<Integer, BlockBean> byId) {
        if (values.size() < 3) { unsupported.add(block.opCode); return null; }
        Map<String, Object> expression = new LinkedHashMap<>();
        expression.put("kind", "reporter");
        expression.put("opCode", "getmapatposlistmap");
        List<Object> arguments = new ArrayList<>();
        arguments.add(literalExpression(values.get(0), byId, unsupported, block.opCode));
        arguments.add(literalExpression(values.get(1), byId, unsupported, block.opCode));
        if (!unsupported.isEmpty() && unsupported.get(unsupported.size() - 1).contains("invalid reporter expression")) return null;
        expression.put("arguments", arguments);
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE,
                CreatorRuntimeServiceArguments.output("stateId", values.get(2), "expression", expression));
    }

    private static Map<String, Object> literalExpression(String rawValue, Map<Integer, BlockBean> byId,
                                                          List<String> unsupported, String opcode) {
        if (rawValue != null && rawValue.trim().startsWith("@")) {
            Map<String, Object> expression = expression(rawValue, byId, new java.util.LinkedHashSet<Integer>());
            if (expression == null) {
                unsupported.add(opcode + " (invalid reporter expression)");
                return Collections.emptyMap();
            }
            return expression;
        }
        return CreatorRuntimeServiceArguments.output("kind", "literal", "value", rawValue);
    }

    /** Legacy set-at family order is value, index, list for all supported list types. */
    private static CreatorRuntimeBlock listSetAt(BlockBean block, List<String> values,
                                                 List<String> unsupported, Map<Integer, BlockBean> byId) {
        if (values.size() < 3) { unsupported.add(block.opCode); return null; }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stateId", values.get(2));
        payload.put("action", "set_at");
        putExpressionOrValue(payload, "value", "valueExpression", values.get(0), byId, unsupported, block.opCode);
        putExpressionOrValue(payload, "index", "indexExpression", values.get(1), byId, unsupported, block.opCode);
        if (!unsupported.isEmpty() && unsupported.get(unsupported.size() - 1).contains("invalid reporter expression")) return null;
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.LIST_MUTATE, payload);
    }

    private static CreatorRuntimeBlock mapMutation(BlockBean block, List<String> values, String action,
                                                   List<String> unsupported, Map<Integer, BlockBean> byId) {
        int required = "put".equals(action) ? 3 : "remove".equals(action) ? 2 : 1;
        if (values.size() < required) { unsupported.add(block.opCode); return null; }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("stateId", values.get(0));
        payload.put("action", action);
        if ("put".equals(action) || "remove".equals(action))
            putExpressionOrValue(payload, "key", "keyExpression", values.get(1), byId, unsupported, block.opCode);
        if ("put".equals(action)) putExpressionOrValue(payload, "value", "valueExpression", values.get(2), byId, unsupported, block.opCode);
        if (!unsupported.isEmpty() && unsupported.get(unsupported.size() - 1).contains("invalid reporter expression")) return null;
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.MAP_MUTATE, payload);
    }

    private static void putExpressionOrValue(Map<String, Object> payload, String valueKey, String expressionKey,
                                             String rawValue, Map<Integer, BlockBean> byId, List<String> unsupported,
                                             String opcode) {
        if (rawValue != null && rawValue.trim().startsWith("@")) {
            Map<String, Object> expression = expression(rawValue, byId, new java.util.LinkedHashSet<Integer>());
            if (expression == null) unsupported.add(opcode + " (invalid reporter expression)");
            else payload.put(expressionKey, expression);
        } else payload.put(valueKey, rawValue);
    }

    private static CreatorRuntimeBlock intentCall(BlockBean block, List<String> values, String action,
                                                  List<String> unsupported) {
        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
        return serviceCall("intent", CreatorRuntimeServiceArguments.output("intentId", values.get(0),
                "action", action, "value", values.get(1)));
    }

    private static CreatorRuntimeBlock dialogCall(BlockBean block, List<String> values, String action,
                                                  List<String> unsupported) {
        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
        return serviceCall("dialog", CreatorRuntimeServiceArguments.output("dialogId", values.get(0),
                "action", action, "value", values.get(1)));
    }

    private static CreatorRuntimeBlock fileCall(BlockBean block, List<String> values, String action, int required,
                                                List<String> unsupported) {
        if (values.size() < required) { unsupported.add(block.opCode); return null; }
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("action", action);
        arguments.put("path", values.get(0));
        if ("write".equals(action)) arguments.put("content", values.get(1));
        else if (required > 1) arguments.put("destination", values.get(1));
        return serviceCall("file", arguments);
    }

    private static CreatorRuntimeBlock animatorCall(BlockBean block, List<String> values, String action, int required,
                                                    List<String> unsupported) {
        if (values.size() < required) { unsupported.add(block.opCode); return null; }
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("componentId", values.get(0));
        arguments.put("action", action);
        if ("set_target".equals(action)) arguments.put("widgetId", values.get(1));
        else if ("set_property".equals(action)) arguments.put("property", values.get(1));
        else if ("set_value".equals(action)) arguments.put("value", values.get(1));
        else if ("set_from_to".equals(action)) {
            arguments.put("from", values.get(1));
            arguments.put("to", values.get(2));
        } else if ("set_duration".equals(action)) arguments.put("durationMs", values.get(1));
        else if ("set_repeat_mode".equals(action)) arguments.put("repeatMode", values.get(1));
        else if ("set_repeat_count".equals(action)) arguments.put("repeatCount", values.get(1));
        else if ("set_interpolator".equals(action)) arguments.put("interpolator", values.get(1));
        return serviceCall("animator", arguments);
    }

    private static CreatorRuntimeBlock firebaseCall(String componentId, String action, String path) {
        return serviceCall("firebase", CreatorRuntimeServiceArguments.output(
                "componentId", componentId, "action", action, "path", path));
    }

    private static CreatorRuntimeBlock calendarCall(String componentId, String action, String key, String value) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("componentId", componentId);
        arguments.put("action", action);
        if ("set_time".equals(action)) arguments.put("timestamp", value);
        else if (key != null) {
            arguments.put("field", key);
            arguments.put("value", value);
        }
        return serviceCall("calendar", arguments);
    }

    private static CreatorRuntimeBlock bluetoothCall(String componentId, String action, String uuid, String address, String tag) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("componentId", componentId);
        arguments.put("action", action);
        arguments.put("tag", tag);
        if (uuid != null) arguments.put("uuid", uuid);
        if (address != null) arguments.put("address", address);
        return serviceCall("bluetooth", arguments);
    }

    private static Map<String, Object> expression(String parameter, Map<Integer, BlockBean> byId, java.util.Set<Integer> path) {
        if (parameter == null) return literalExpression("");
        String value = parameter.trim();
        if (!value.startsWith("@")) return literalExpression(value);
        int id;
        try { id = Integer.parseInt(value.substring(1)); }
        catch (NumberFormatException ignored) { return null; }
        if (!path.add(id)) return null;
        try {
            BlockBean block = byId.get(id);
            if (block == null || blank(block.opCode)) return null;
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("kind", "reporter");
            result.put("opCode", block.opCode.trim().toLowerCase(Locale.ROOT));
            List<Object> arguments = new ArrayList<>();
            for (String argument : block.parameters == null ? Collections.<String>emptyList() : block.parameters) {
                Map<String, Object> nested = expression(argument, byId, path);
                if (nested == null) return null;
                arguments.add(nested);
            }
            result.put("arguments", arguments);
            return result;
        } finally {
            path.remove(id);
        }
    }

    private static Map<String, Object> literalExpression(String value) {
        return CreatorRuntimeServiceArguments.output("kind", "literal", "value", value);
    }

    @SuppressWarnings("unchecked")
    private static CreatorRuntimeBlock storageCall(String componentId, String action, String key, String value,
                                                   String explicitStoreName, Map<String, Object> componentDescriptors) {
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("componentId", componentId);
        arguments.put("action", action);
        Object raw = componentDescriptors == null ? null : componentDescriptors.get(componentId);
        Map<String, Object> descriptor = raw instanceof Map ? (Map<String, Object>) raw : Collections.<String, Object>emptyMap();
        String storeName = explicitStoreName == null ? String.valueOf(descriptor.get("param1") == null ? "" : descriptor.get("param1")) : explicitStoreName;
        if ("configure".equals(action)) arguments.put("storeName", value);
        else {
            arguments.put("key", key);
            if (value != null) arguments.put("value", value);
            if (!blank(storeName)) arguments.put("storeName", storeName);
        }
        return serviceCall("local_storage", arguments);
    }

    @SuppressWarnings("unchecked")
    private static String firebasePath(Map<String, Object> componentDescriptors, String componentId, String childPath) {
        Object raw = componentDescriptors == null ? null : componentDescriptors.get(componentId);
        Map<String, Object> descriptor = raw instanceof Map ? (Map<String, Object>) raw : Collections.<String, Object>emptyMap();
        String base = String.valueOf(descriptor.get("param1") == null ? "" : descriptor.get("param1")).trim();
        String child = childPath == null ? "" : childPath.trim();
        while (base.startsWith("/")) base = base.substring(1);
        while (base.endsWith("/")) base = base.substring(0, base.length() - 1);
        while (child.startsWith("/")) child = child.substring(1);
        return base.isEmpty() ? child : child.isEmpty() ? base : base + "/" + child;
    }

    private static CreatorRuntimeBlock widgetProperty(BlockBean block, List<String> values, String property,
                                                      List<String> unsupported) {
        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("widgetId", values.get(0));
        payload.put("property", property);
        payload.put("value", values.get(1));
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY, payload);
    }

    private static CreatorRuntimeBlock widgetProperty(BlockBean block, List<String> values, String property,
                                                      List<String> unsupported, Map<Integer, BlockBean> byId) {
        if (values.size() < 2) { unsupported.add(block.opCode); return null; }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("widgetId", values.get(0));
        payload.put("property", property);
        if (values.get(1).trim().startsWith("@")) {
            Map<String, Object> expression = expression(values.get(1), byId, new java.util.LinkedHashSet<Integer>());
            if (expression == null) { unsupported.add(block.opCode + " (invalid value expression)"); return null; }
            payload.put("expression", expression);
        } else payload.put("value", values.get(1));
        return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY, payload);
    }

    private static String normalizeEventName(String eventName) {
        String normalized = eventName.trim().toLowerCase(Locale.ROOT);
        if ("initializelogic".equals(normalized) || "oncreate".equals(normalized)) return "create";
        if ("onresume".equals(normalized)) return "resume";
        if ("onpause".equals(normalized)) return "pause";
        if ("ondestroy".equals(normalized)) return "destroy";
        if ("onstart".equals(normalized)) return "start";
        if ("onstop".equals(normalized)) return "stop";
        if ("onbackpressed".equals(normalized)) return "back_pressed";
        if ("onpostcreate".equals(normalized)) return "post_create";
        if ("ontimer".equals(normalized)) return "tick";
        if ("onresponse".equals(normalized) || "onrequestnetworkresponse".equals(normalized)) return "response";
        if ("onerror".equals(normalized) || "onrequestnetworkerror".equals(normalized)) return "error";
        if ("ondateset".equals(normalized) || "ontimeset".equals(normalized)) return "selected";
        if ("onlocationchanged".equals(normalized) || "ongyroscopechanged".equals(normalized)) return "changed";
        if ("oncompletion".equals(normalized)) return "completed";
        if ("onadloaded".equals(normalized) || "onrewardadloaded".equals(normalized)) return "loaded";
        if ("onuserearnedreward".equals(normalized)) return "reward";
        if ("onaddismissedfullscreencontent".equals(normalized)) return "dismissed";
        if ("onadshowedfullscreencontent".equals(normalized)) return "shown";
        if ("onrewardadfailedtoload".equals(normalized) || "onadfailedtoshowfullscreencontent".equals(normalized)) return "error";
        if ("oncodesent".equals(normalized)) return "code_sent";
        if ("onclick".equals(normalized) || "click".equals(normalized)) return "click";
        if ("oncheckedchanged".equals(normalized) || "change".equals(normalized)) return "change";
        if ("onitemselected".equals(normalized)) return "item_selected";
        if ("ondateset".equals(normalized)) return "date_selected";
        if ("ontimeset".equals(normalized)) return "time_selected";
        return normalized;
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }

    private static boolean isSound(String source) {
        String value = source == null ? "" : source.toLowerCase(Locale.ROOT);
        return value.endsWith(".mp3") || value.endsWith(".wav") || value.endsWith(".ogg")
                || value.endsWith(".m4a") || value.endsWith(".aac") || value.endsWith(".flac");
    }

    private static boolean isImage(String source) {
        String value = source == null ? "" : source.toLowerCase(Locale.ROOT);
        return value.endsWith(".png") || value.endsWith(".jpg") || value.endsWith(".jpeg") || value.endsWith(".gif")
                || value.endsWith(".webp") || value.endsWith(".bmp") || value.endsWith(".svg");
    }

    private static boolean isVideo(String source) {
        String value = source == null ? "" : source.toLowerCase(Locale.ROOT);
        return value.endsWith(".mp4") || value.endsWith(".avi") || value.endsWith(".mkv") || value.endsWith(".webm")
                || value.endsWith(".3gp") || value.endsWith(".mov");
    }

    private static boolean isFont(String source) {
        String value = source == null ? "" : source.toLowerCase(Locale.ROOT);
        return value.endsWith(".ttf") || value.endsWith(".otf") || value.endsWith(".ttc") || value.endsWith(".woff")
                || value.endsWith(".woff2");
    }

    private static String normalizeLocationProvider(String legacyProvider) {
        if (legacyProvider == null) return "gps";
        String provider = legacyProvider.trim().toLowerCase(Locale.ROOT);
        if (provider.contains("network")) return "network";
        if (provider.contains("passive")) return "passive";
        return "gps";
    }
}
