package pro.sketchware.creator.runtime;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.beans.ProjectResourceBean;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Imports legacy components, events, and block chains into the versioned Creator
 * Runtime document. Unsupported executable blocks stay visible in the report
 * and are never compiled or delegated to an APK fallback.
 */
public final class CreatorLegacyArtifactImporter {
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
            BlockConversion blocks = convertBlocks(legacyBlocks);
            if (!blocks.unsupported.isEmpty()) {
                deferredEvents.put(eventKey, blocks.unsupported);
                report.add(eventKey, "BlockBean", CreatorCompatibilityTier.R0_UNSUPPORTED,
                        "Unsupported legacy block opcodes: " + String.join(", ", blocks.unsupported) + ".");
                continue;
            }
            if (!base.getWidgets().containsKey(event.targetId)) {
                Map<String, Object> descriptor = new LinkedHashMap<>();
                descriptor.put("eventType", event.eventType);
                descriptor.put("targetId", event.targetId);
                descriptor.put("eventName", normalizeEventName(event.eventName));
                descriptor.put("blockCount", blocks.converted.size());
                deferredEvents.put(eventKey, descriptor);
                report.add(eventKey, "EventBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                        "Imported as a runtime service or activity event descriptor.");
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
            projectFiles.add(descriptor);
            report.add(file.fileName, "ProjectFileBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                    "Imported as visible Creator Runtime screen metadata.");
        }
        state.put("legacy.projectFiles", projectFiles);

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

    public Result importResources(CreatorProjectDocument base, List<ProjectResourceBean> resources) {
        if (base == null) throw new IllegalArgumentException("base");
        CreatorCompatibilityReport report = new CreatorCompatibilityReport();
        Map<String, Object> state = new LinkedHashMap<>(base.getState());
        List<Object> imported = new ArrayList<>();
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
            imported.add(descriptor);
            report.add(resource.resName, "ProjectResourceBean", CreatorCompatibilityTier.R1_RUNTIME_NATIVE,
                    "Preserved as runtime resource metadata for live widget and media services.");
        }
        state.put("legacy.resources", imported);
        return new Result(base.withRuntimeState(base.getRevision(), state, base.getEvents()), report);
    }

    private static final class BlockConversion {
        final List<CreatorRuntimeBlock> converted = new ArrayList<>();
        final List<String> unsupported = new ArrayList<>();
    }

    private BlockConversion convertBlocks(List<BlockBean> blocks) {
        BlockConversion result = new BlockConversion();
        Map<Integer, BlockBean> byId = new LinkedHashMap<>();
        java.util.Set<Integer> referenced = new java.util.LinkedHashSet<>();
        for (BlockBean block : blocks == null ? Collections.<BlockBean>emptyList() : blocks) {
            if (block == null) continue;
            try { byId.put(Integer.parseInt(block.id), block); } catch (NumberFormatException ignored) {
                result.unsupported.add("invalid block id");
            }
            if (block.nextBlock >= 0) referenced.add(block.nextBlock);
            if (block.subStack1 >= 0) referenced.add(block.subStack1);
            if (block.subStack2 >= 0) referenced.add(block.subStack2);
        }
        java.util.Set<Integer> visited = new java.util.LinkedHashSet<>();
        for (Map.Entry<Integer, BlockBean> entry : byId.entrySet()) {
            if (!referenced.contains(entry.getKey())) convertChain(entry.getValue(), byId, visited, result.converted, result.unsupported);
        }
        for (Map.Entry<Integer, BlockBean> entry : byId.entrySet()) {
            if (!visited.contains(entry.getKey())) result.unsupported.add("orphan block " + entry.getKey());
        }
        return result;
    }

    private void convertChain(BlockBean start, Map<Integer, BlockBean> byId, java.util.Set<Integer> visited,
                              List<CreatorRuntimeBlock> target, List<String> unsupported) {
        BlockBean current = start;
        while (current != null) {
            int id;
            try { id = Integer.parseInt(current.id); } catch (NumberFormatException ignored) {
                unsupported.add("invalid block id"); return;
            }
            if (!visited.add(id)) { unsupported.add("cyclic block graph at " + id); return; }
            CreatorRuntimeBlock converted = convertBlock(current, byId, visited, unsupported);
            if (converted != null) target.add(converted);
            current = byId.get(current.nextBlock);
        }
    }

    private CreatorRuntimeBlock convertBlock(BlockBean block, Map<Integer, BlockBean> byId,
                                             java.util.Set<Integer> visited, List<String> unsupported) {
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
            convertChain(thenStart, byId, visited, thenBlocks, unsupported);
            if (block.subStack2 >= 0) {
                BlockBean elseStart = byId.get(block.subStack2);
                if (elseStart == null) { unsupported.add(block.opCode + " (missing else substack)"); return null; }
                convertChain(elseStart, byId, visited, elseBlocks, unsupported);
            }
            payload.put("stateId", values.get(0));
            payload.put("equals", values.get(1));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.IF_STATE_EQUALS, payload, thenBlocks, elseBlocks);
        }
        if ("if".equals(op) || "ifelse".equals(op)) {
            if (values.size() < 1 || block.subStack1 < 0) { unsupported.add(block.opCode); return null; }
            List<CreatorRuntimeBlock> thenBlocks = new ArrayList<>();
            List<CreatorRuntimeBlock> elseBlocks = new ArrayList<>();
            BlockBean thenStart = byId.get(block.subStack1);
            if (thenStart == null) { unsupported.add(block.opCode + " (missing then substack)"); return null; }
            convertChain(thenStart, byId, visited, thenBlocks, unsupported);
            if ("ifelse".equals(op)) {
                if (block.subStack2 < 0 || byId.get(block.subStack2) == null) {
                    unsupported.add(block.opCode + " (missing else substack)"); return null;
                }
                convertChain(byId.get(block.subStack2), byId, visited, elseBlocks, unsupported);
            }
            Map<String, Object> condition = parseCondition(values.get(0));
            if (condition == null) { unsupported.add(block.opCode + " (untyped condition)"); return null; }
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.IF_CONDITION, condition, thenBlocks, elseBlocks);
        }
        if ("repeat".equals(op)) {
            if (values.size() < 1 || block.subStack1 < 0 || byId.get(block.subStack1) == null) {
                unsupported.add(block.opCode); return null;
            }
            List<CreatorRuntimeBlock> body = new ArrayList<>();
            convertChain(byId.get(block.subStack1), byId, visited, body, unsupported);
            payload.put("count", values.get(0));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.REPEAT, payload, body, Collections.<CreatorRuntimeBlock>emptyList());
        }
        if ("forever".equals(op)) {
            if (block.subStack1 < 0 || byId.get(block.subStack1) == null) {
                unsupported.add(block.opCode); return null;
            }
            List<CreatorRuntimeBlock> body = new ArrayList<>();
            convertChain(byId.get(block.subStack1), byId, visited, body, unsupported);
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.FOREVER, Collections.<String, Object>emptyMap(), body, Collections.<CreatorRuntimeBlock>emptyList());
        }
        if ("break".equals(op)) {
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.BREAK, Collections.<String, Object>emptyMap());
        }
        if (block.subStack1 >= 0 || block.subStack2 >= 0) { unsupported.add(block.opCode + " (control flow)"); return null; }
        if ("settext".equals(op) || "set_text".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            payload.put("widgetId", values.get(0)); payload.put("property", "text"); payload.put("value", values.get(1));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY, payload);
        } else if ("setchecked".equals(op) || "set_checked".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            payload.put("widgetId", values.get(0)); payload.put("property", "checked"); payload.put("value", values.get(1));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY, payload);
        } else if ("setvar".equals(op) || "set_var".equals(op)
                || "setvarboolean".equals(op) || "setvarint".equals(op) || "setvarstring".equals(op)) {
            if (values.size() < 2) { unsupported.add(block.opCode); return null; }
            payload.put("stateId", values.get(0)); payload.put("expression", values.get(1));
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.SET_STATE, payload);
        } else if ("increaseint".equals(op) || "increase_int".equals(op)
                || "decreaseint".equals(op) || "decrease_int".equals(op)) {
            if (values.size() < 1) { unsupported.add(block.opCode); return null; }
            payload.put("stateId", values.get(0));
            payload.put("delta", op.startsWith("decrease") ? -1 : 1);
            return new CreatorRuntimeBlock(CreatorRuntimeBlock.Type.STATE_INCREMENT, payload);
        } else if ("showmessage".equals(op) || "show_message".equals(op) || "toast".equals(op)) {
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

    private static Map<String, Object> parseCondition(String expression) {
        if (expression == null) return null;
        String value = expression.trim();
        Map<String, Object> condition = new LinkedHashMap<>();
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            condition.put("operator", value.toLowerCase(Locale.ROOT));
            return condition;
        }
        String[] operators = {"!=", ">=", "<=", "==", ">", "<"};
        for (String token : operators) {
            int index = value.indexOf(token);
            if (index > 0 && index + token.length() < value.length()) {
                String left = value.substring(0, index).trim();
                String right = value.substring(index + token.length()).trim();
                condition.put("operator", "==".equals(token) ? "equals"
                        : "!=".equals(token) ? "not_equals"
                        : ">".equals(token) ? "greater"
                        : ">=".equals(token) ? "greater_or_equal"
                        : "<".equals(token) ? "less" : "less_or_equal");
                condition.put("left", left);
                condition.put("right", unquote(right));
                return condition;
            }
        }
        if (value.startsWith("!")) {
            Map<String, Object> nested = parseCondition(value.substring(1));
            if (nested == null) return null;
            condition.put("operator", "not");
            condition.put("operand", nested);
            return condition;
        }
        return null;
    }

    private static String unquote(String value) {
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            return value.substring(1, value.length() - 1);
        }
        return value;
    }

    private static String normalizeEventName(String eventName) {
        String normalized = eventName.trim().toLowerCase(Locale.ROOT);
        if ("onclick".equals(normalized) || "click".equals(normalized)) return "click";
        if ("oncheckedchanged".equals(normalized) || "change".equals(normalized)) return "change";
        if ("onitemselected".equals(normalized)) return "item_selected";
        if ("ondateset".equals(normalized)) return "date_selected";
        if ("ontimeset".equals(normalized)) return "time_selected";
        return normalized;
    }

    private static boolean blank(String value) { return value == null || value.trim().isEmpty(); }
}
