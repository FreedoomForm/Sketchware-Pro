package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
import com.besome.sketch.beans.MoreBlockCollectionBean;
import com.besome.sketch.beans.ProjectFileBean;
import com.besome.sketch.beans.ProjectLibraryBean;
import com.besome.sketch.beans.ProjectResourceBean;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.Test;

public class CreatorLegacyArtifactImporterTest {
    @Test public void importsComponentServiceAndSupportedViewBlockChain() {
        CreatorProjectDocument document = documentWithButton();
        ComponentBean camera = new ComponentBean(ComponentBean.COMPONENT_TYPE_CAMERA, "camera1");
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean message = new BlockBean("1", "", "", "showMessage");
        message.parameters.add("Created live");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(message));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                document, Collections.singletonList(camera), Collections.singletonList(click), blocks);

        @SuppressWarnings("unchecked") Map<String, Object> components =
                (Map<String, Object>) result.getDocument().getState().get("legacy.components");
        @SuppressWarnings("unchecked") Map<String, Object> descriptor = (Map<String, Object>) components.get("camera1");
        assertThat(descriptor.get("serviceId")).isEqualTo("camera");
        CreatorEventBinding binding = result.getDocument().getEvents().get("legacy_button_onClick");
        assertThat(binding.getEventName()).isEqualTo("click");
        assertThat(binding.getBlocks()).hasSize(1);
        assertThat(binding.getBlocks().get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.SHOW_MESSAGE);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void blocksUnknownLegacyOpcodeWithoutFallbackExecution() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean unsupported = new BlockBean("1", "", "", "executeJava");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(unsupported));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        assertThat(result.getDocument().getEvents()).isEmpty();
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(1);
        assertThat(result.getReport().canPreviewImmediately()).isFalse();
    }

    @Test public void importsNativeDrawerOpenCloseActionsAndOpenStateReporterWithoutFallback() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean open = new BlockBean("1", "2", "", "openDrawer");
        BlockBean close = new BlockBean("2", "", "", "closeDrawer");
        Map<String, java.util.List<BlockBean>> actions = new LinkedHashMap<>();
        actions.put(click.getEventKey(), Arrays.asList(open, close));

        CreatorLegacyArtifactImporter.Result actionResult = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), actions);
        java.util.List<CreatorRuntimeBlock> imported = actionResult.getDocument().getEvents()
                .get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(2);
        assertThat(imported.get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL);
        assertThat(imported.get(0).getPayload()).containsEntry("serviceId", "drawer");
        @SuppressWarnings("unchecked") Map<String, Object> openArguments =
                (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> closeArguments =
                (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(openArguments).containsEntry("action", "open");
        assertThat(closeArguments).containsEntry("action", "close");

        BlockBean condition = new BlockBean("3", "", "", "if");
        condition.parameters.add("@4");
        condition.subStack1 = 5;
        BlockBean reporter = new BlockBean("4", "", "", "isDrawerOpen");
        BlockBean message = new BlockBean("5", "", "", "showMessage");
        message.parameters.add("Open");
        Map<String, java.util.List<BlockBean>> reporterBlocks = new LinkedHashMap<>();
        reporterBlocks.put(click.getEventKey(), Arrays.asList(condition, reporter, message));
        CreatorLegacyArtifactImporter.Result reporterResult = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), reporterBlocks);
        CreatorRuntimeBlock importedCondition = reporterResult.getDocument().getEvents()
                .get("legacy_button_onClick").getBlocks().get(0);
        assertThat(importedCondition.getType()).isEqualTo(CreatorRuntimeBlock.Type.IF_BOOLEAN);
        @SuppressWarnings("unchecked") Map<String, Object> expression =
                (Map<String, Object>) importedCondition.getPayload().get("expression");
        assertThat(expression).containsEntry("opCode", "isdraweropen");
        assertThat(reporterResult.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsAndExecutesMoreBlockCallWithScopedGetArgWithoutJavaExecution() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean call = new BlockBean("1", "echo %s.name", " ", "definedFunc");
        call.parameters.add("Ada");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(call));

        BlockBean assign = new BlockBean("1", "", "", "setVar");
        assign.parameters.add("received");
        assign.parameters.add("@2");
        BlockBean argument = new BlockBean("2", "name", "s", "getArg");
        MoreBlockCollectionBean definition = new MoreBlockCollectionBean("echo", "echo %s.name",
                new java.util.ArrayList<>(Arrays.asList(assign, argument)));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks,
                Collections.singletonList(definition));
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
        @SuppressWarnings("unchecked") Map<String, Object> definitions =
                (Map<String, Object>) result.getDocument().getState().get("legacy.moreBlocks");
        assertThat(definitions).containsKey("echo");
        assertThat(result.getDocument().getEvents()).containsKey("legacy_moreblock_echo");
        assertThat(result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0).getType())
                .isEqualTo(CreatorRuntimeBlock.Type.CUSTOM_FUNCTION_CALL);

        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(result.getDocument(), 20, new CreatorRuntimeEventLog(20));
        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");
        assertThat(engine.getCurrent().getState().get("received")).isEqualTo("Ada");
    }

    @Test public void importsAndEvaluatesTypedMoreBlockReturnValue() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean assign = new BlockBean("1", "", "", "setVar");
        assign.parameters.add("received");
        assign.parameters.add("@2");
        BlockBean call = new BlockBean("2", "echo[s]", "s", "definedFunc");
        call.parameters.add("Ada");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(assign, call));

        BlockBean returnBlock = new BlockBean("1", "", "", "return");
        returnBlock.parameters.add("@2");
        BlockBean argument = new BlockBean("2", "name", "s", "getArg");
        MoreBlockCollectionBean definition = new MoreBlockCollectionBean("echo[s]", "echo %s.name",
                new java.util.ArrayList<>(Arrays.asList(returnBlock, argument)));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks,
                Collections.singletonList(definition));
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
        CreatorRuntimeBlock returnRuntimeBlock = result.getDocument().getEvents()
                .get("legacy_moreblock_echo").getBlocks().get(0);
        assertThat(returnRuntimeBlock.getType()).isEqualTo(CreatorRuntimeBlock.Type.RETURN);

        CreatorRuntimeEngine engine = new CreatorRuntimeEngine(result.getDocument(), 20, new CreatorRuntimeEventLog(20));
        new CreatorRuntimeExecutor().dispatch(engine, "button", "click");
        assertThat(engine.getCurrent().getState().get("received")).isEqualTo("Ada");
    }

    @Test public void keepsArbitraryAddSourceDirectlyVisibleAndBlockedWithoutFallbackExecution() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean source = new BlockBean("1", "", "", "addSourceDirectly");
        source.parameters.add("throw new IllegalStateException(\"must not execute\");");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(source));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        assertThat(result.getDocument().getEvents()).isEmpty();
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(1);
    }

    @Test public void importsLegacyValueResourceFamiliesAsTypedVariantMetadata() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("values/strings.xml", "<resources><string name=\"title\">Creator</string></resources>");
        values.put("values-night/colors.xml", "<resources><color name=\"ink\">#efefef</color></resources>");
        values.put("values/styles.xml", "<resources><style name=\"AppStyle\" parent=\"Base\"><item name=\"android:textColor\">#111111</item></style></resources>");
        values.put("values/themes.xml", "<resources><style name=\"Theme.Creator\"><item name=\"android:windowLightStatusBar\">true</item></style></resources>");
        values.put("values/arrays.xml", "<resources><string-array name=\"labels\"><item>one</item><item>two</item></string-array></resources>");

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importValueResources(documentWithButton(), values);

        @SuppressWarnings("unchecked") Map<String, Object> strings =
                (Map<String, Object>) result.getDocument().getState().get("legacy.stringResources");
        @SuppressWarnings("unchecked") Map<String, Object> defaultStrings = (Map<String, Object>) strings.get("");
        assertThat(defaultStrings).containsEntry("title", "Creator");
        @SuppressWarnings("unchecked") Map<String, Object> colors =
                (Map<String, Object>) result.getDocument().getState().get("legacy.colorResources");
        @SuppressWarnings("unchecked") Map<String, Object> nightColors = (Map<String, Object>) colors.get("-night");
        assertThat(nightColors).containsEntry("ink", "#efefef");
        @SuppressWarnings("unchecked") Map<String, Object> styles =
                (Map<String, Object>) result.getDocument().getState().get("legacy.styleResources");
        @SuppressWarnings("unchecked") Map<String, Object> defaultStyles = (Map<String, Object>) styles.get("");
        @SuppressWarnings("unchecked") Map<String, Object> appStyle = (Map<String, Object>) defaultStyles.get("AppStyle");
        assertThat(appStyle).containsEntry("parent", "Base");
        @SuppressWarnings("unchecked") Map<String, Object> styleItems = (Map<String, Object>) appStyle.get("items");
        assertThat(styleItems).containsEntry("android:textColor", "#111111");
        @SuppressWarnings("unchecked") Map<String, Object> arrays =
                (Map<String, Object>) result.getDocument().getState().get("legacy.arrayResources");
        @SuppressWarnings("unchecked") Map<String, Object> defaultArrays = (Map<String, Object>) arrays.get("");
        @SuppressWarnings("unchecked") Map<String, Object> labels = (Map<String, Object>) defaultArrays.get("labels");
        assertThat(labels).containsEntry("type", "string-array");
        assertThat((java.util.List<?>) labels.get("items")).containsExactly("one", "two").inOrder();
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(5);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void followsLegacyNextBlockChainAndRejectsUntypedControlFlow() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean second = new BlockBean("2", "", "", "showMessage");
        second.parameters.add("Second");
        BlockBean first = new BlockBean("1", "", "", "setVar");
        first.parameters.add("answer");
        first.parameters.add("42");
        first.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(second, first));

        CreatorLegacyArtifactImporter.Result ordered = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        assertThat(ordered.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0).getType())
                .isEqualTo(CreatorRuntimeBlock.Type.SET_STATE);

        first.subStack1 = 2;
        CreatorLegacyArtifactImporter.Result controlFlow = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);
        assertThat(controlFlow.getDocument().getEvents()).isEmpty();
        assertThat(controlFlow.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(1);
    }

    @Test public void importsCanonicalForeverAndBreakAsTypedBoundedControlFlow() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean forever = new BlockBean("1", "", "", "forever");
        forever.subStack1 = 2;
        BlockBean increment = new BlockBean("2", "", "", "increaseInt");
        increment.parameters.add("count");
        increment.nextBlock = 3;
        BlockBean breakBlock = new BlockBean("3", "", "", "break");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(forever, increment, breakBlock));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.FOREVER);
        assertThat(imported.getThenBlocks()).hasSize(2);
        assertThat(imported.getThenBlocks().get(1).getType()).isEqualTo(CreatorRuntimeBlock.Type.BREAK);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsSetListMapUsingItsLegacyKeyValueIndexListArgumentOrder() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean setListMap = new BlockBean("1", "", "", "setListMap");
        setListMap.parameters.add("title");
        setListMap.parameters.add("Creator");
        setListMap.parameters.add("0");
        setListMap.parameters.add("rows");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(setListMap));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.LIST_MUTATE);
        assertThat(imported.getPayload()).containsEntry("stateId", "rows");
        assertThat(imported.getPayload()).containsEntry("action", "map_put_at");
        assertThat(imported.getPayload()).containsEntry("key", "title");
        assertThat(imported.getPayload()).containsEntry("value", "Creator");
        assertThat(imported.getPayload()).containsEntry("index", "0");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalSetAtListFamilyWithValueIndexListArgumentOrder() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean setAt = new BlockBean("1", "", "", "setAtPosListstr");
        setAt.parameters.add("Creator");
        setAt.parameters.add("1");
        setAt.parameters.add("names");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(setAt));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.LIST_MUTATE);
        assertThat(imported.getPayload()).containsEntry("stateId", "names");
        assertThat(imported.getPayload()).containsEntry("action", "set_at");
        assertThat(imported.getPayload()).containsEntry("value", "Creator");
        assertThat(imported.getPayload()).containsEntry("index", "1");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsSetTypefaceAsTypedFontAndStyleWidgetProperty() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean typeface = new BlockBean("1", "", "", "setTypeface");
        typeface.parameters.add("button");
        typeface.parameters.add("display");
        typeface.parameters.add("BOLD");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(typeface));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY);
        assertThat(imported.getPayload()).containsEntry("property", "typeface");
        @SuppressWarnings("unchecked") Map<String, Object> typefaceValue = (Map<String, Object>) imported.getPayload().get("value");
        assertThat(typefaceValue).containsExactly("font", "display", "style", "BOLD");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsMapGetAllKeysAsTypedMapToListMutation() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean keys = new BlockBean("1", "", "", "mapGetAllKeys");
        keys.parameters.add("profile");
        keys.parameters.add("keys");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(keys));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.LIST_MUTATE);
        assertThat(imported.getPayload()).containsEntry("stateId", "keys");
        assertThat(imported.getPayload()).containsEntry("action", "replace_map_keys");
        assertThat(imported.getPayload()).containsEntry("sourceMapStateId", "profile");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalJsonToMapAndListMapMutations() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean map = new BlockBean("1", "2", "", "strToMap");
        map.parameters.add("{\"name\":\"Ada\"}");
        map.parameters.add("profile");
        BlockBean list = new BlockBean("2", "", "", "strToListMap");
        list.parameters.add("[{\"name\":\"Ada\"}]");
        list.parameters.add("rows");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(map, list));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported.get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.MAP_MUTATE);
        assertThat(imported.get(0).getPayload()).containsEntry("action", "replace_json");
        assertThat(imported.get(0).getPayload()).containsEntry("stateId", "profile");
        assertThat(imported.get(1).getType()).isEqualTo(CreatorRuntimeBlock.Type.LIST_MUTATE);
        assertThat(imported.get(1).getPayload()).containsEntry("action", "replace_json_maps");
        assertThat(imported.get(1).getPayload()).containsEntry("stateId", "rows");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsSupportedStateEqualityConditionalSubstackGraph() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean branch = new BlockBean("1", "", "", "if_state_equals");
        branch.parameters.add("status");
        branch.parameters.add("approved");
        branch.subStack1 = 2;
        branch.subStack2 = 3;
        BlockBean approved = new BlockBean("2", "", "", "showMessage");
        approved.parameters.add("Approved");
        BlockBean pending = new BlockBean("3", "", "", "showMessage");
        pending.parameters.add("Pending");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(branch, approved, pending));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.IF_STATE_EQUALS);
        assertThat(imported.getThenBlocks().get(0).getPayload().get("message")).isEqualTo("Approved");
        assertThat(imported.getElseBlocks().get(0).getPayload().get("message")).isEqualTo("Pending");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalWidgetAndStateOpcodesAsTypedRuntimeBlocks() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean enabled = new BlockBean("1", "", "", "setEnable");
        enabled.parameters.add("button");
        enabled.parameters.add("false");
        BlockBean color = new BlockBean("2", "", "", "setTextColor");
        color.parameters.add("button");
        color.parameters.add("#123456");
        BlockBean state = new BlockBean("3", "", "", "setVarString");
        state.parameters.add("status");
        state.parameters.add("ready");
        BlockBean toast = new BlockBean("4", "", "", "doToast");
        toast.parameters.add("Runtime native");
        enabled.nextBlock = 2;
        color.nextBlock = 3;
        state.nextBlock = 4;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(enabled, color, state, toast));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(4);
        assertThat(imported.get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY);
        assertThat(imported.get(0).getPayload()).containsEntry("property", "enabled");
        assertThat(imported.get(1).getPayload()).containsEntry("property", "textColor");
        assertThat(imported.get(2).getType()).isEqualTo(CreatorRuntimeBlock.Type.SET_STATE);
        assertThat(imported.get(3).getType()).isEqualTo(CreatorRuntimeBlock.Type.SHOW_MESSAGE);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsActivityAndComponentEventsAsRuntimeBindingsWithDeferredDescriptors() {
        ComponentBean timer = new ComponentBean(ComponentBean.COMPONENT_TYPE_TIMERTASK, "timer1");
        EventBean activity = new EventBean(EventBean.EVENT_TYPE_ACTIVITY, 0, "onResume", "onResume");
        EventBean component = new EventBean(EventBean.EVENT_TYPE_COMPONENT, 0, "timer1", "onTimer");
        BlockBean activityMessage = new BlockBean("1", "", "", "showMessage");
        activityMessage.parameters.add("Resumed");
        BlockBean componentMessage = new BlockBean("2", "", "", "showMessage");
        componentMessage.parameters.add("Tick");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(activity.getEventKey(), Collections.singletonList(activityMessage));
        blocks.put(component.getEventKey(), Collections.singletonList(componentMessage));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.singletonList(timer), Arrays.asList(activity, component), blocks);

        CreatorEventBinding activityBinding = result.getDocument().getEvents().get("legacy_activity_resume");
        assertThat(activityBinding.getTargetWidgetId()).isEqualTo(CreatorLegacyArtifactImporter.ACTIVITY_EVENT_TARGET);
        assertThat(activityBinding.getEventName()).isEqualTo("resume");
        CreatorEventBinding componentBinding = result.getDocument().getEvents().get("legacy_component_timer1_onTimer");
        assertThat(componentBinding.getTargetWidgetId()).isEqualTo("timer1");
        assertThat(componentBinding.getEventName()).isEqualTo("tick");
        @SuppressWarnings("unchecked") Map<String, Object> deferred =
                (Map<String, Object>) result.getDocument().getState().get("legacy.deferredEvents");
        assertThat(deferred).containsKey(activity.getEventKey());
        assertThat(deferred).containsKey(component.getEventKey());
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void normalizesCanonicalComponentEventsToDirectRuntimePublications() {
        EventBean http = new EventBean(EventBean.EVENT_TYPE_COMPONENT, 0, "network1", "onResponse");
        EventBean picker = new EventBean(EventBean.EVENT_TYPE_COMPONENT, 0, "picker1", "onDateSet");
        EventBean ads = new EventBean(EventBean.EVENT_TYPE_COMPONENT, 0, "ads1", "onUserEarnedReward");
        BlockBean message = new BlockBean("1", "", "", "showMessage");
        message.parameters.add("received");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(http.getEventKey(), Collections.singletonList(message));
        blocks.put(picker.getEventKey(), Collections.singletonList(message));
        blocks.put(ads.getEventKey(), Collections.singletonList(message));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Arrays.asList(http, picker, ads), blocks);

        assertThat(result.getDocument().getEvents().get("legacy_component_network1_onResponse").getEventName())
                .isEqualTo("response");
        assertThat(result.getDocument().getEvents().get("legacy_component_picker1_onDateSet").getEventName())
                .isEqualTo("selected");
        assertThat(result.getDocument().getEvents().get("legacy_component_ads1_onUserEarnedReward").getEventName())
                .isEqualTo("reward");
    }

    @Test public void importsLegacyTimerSubstackAsDirectRuntimeTickBinding() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean timer = new BlockBean("1", "", "", "timerAfter");
        timer.parameters.add("timerTask1");
        timer.parameters.add("250");
        timer.subStack1 = 2;
        BlockBean callback = new BlockBean("2", "", "", "showMessage");
        callback.parameters.add("Timer completed");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(timer, callback));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock call = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(call.getType()).isEqualTo(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL);
        assertThat(call.getPayload().get("serviceId")).isEqualTo("timer");
        @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) call.getPayload().get("arguments");
        assertThat(arguments).containsEntry("action", "after");
        CreatorEventBinding tick = result.getDocument().getEvents().get("legacy_timer_callback_timerTask1");
        assertThat(tick.getTargetWidgetId()).isEqualTo("timerTask1");
        assertThat(tick.getEventName()).isEqualTo("tick");
        assertThat(tick.getBlocks().get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.SHOW_MESSAGE);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyDialogButtonSubstackAsDirectRuntimeButtonBinding() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean positive = new BlockBean("1", "", "", "dialogOkButton");
        positive.parameters.add("dialog1");
        positive.parameters.add("Continue");
        positive.subStack1 = 2;
        BlockBean callback = new BlockBean("2", "", "", "showMessage");
        callback.parameters.add("Accepted");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(positive, callback));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock call = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertServiceCall(call, "dialog", "set_positive_button");
        @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) call.getPayload().get("arguments");
        assertThat(arguments).containsEntry("label", "Continue");
        assertThat(arguments).containsEntry("callbackTargetId", "legacy_dialog_button_callback_1_positive");
        CreatorEventBinding binding = result.getDocument().getEvents().get("legacy_dialog_button_callback_1_positive");
        assertThat(binding.getTargetWidgetId()).isEqualTo("legacy_dialog_button_callback_1_positive");
        assertThat(binding.getEventName()).isEqualTo("button");
        assertThat(binding.getBlocks().get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.SHOW_MESSAGE);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyViewOnClickSubstackAsTypedDynamicClickBinding() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean attach = new BlockBean("1", "", "", "viewOnClick");
        attach.parameters.add("button");
        attach.subStack1 = 2;
        BlockBean callback = new BlockBean("2", "", "", "showMessage");
        callback.parameters.add("Tapped again");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(attach, callback));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock binding = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(binding.getType()).isEqualTo(CreatorRuntimeBlock.Type.ATTACH_EVENT);
        assertThat(binding.getPayload()).containsEntry("bindingId", "legacy_button_onClick");
        assertThat(binding.getPayload()).containsEntry("targetWidgetId", "button");
        assertThat(binding.getPayload()).containsEntry("eventName", "click");
        assertThat(binding.getThenBlocks()).hasSize(1);
        assertThat(binding.getThenBlocks().get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.SHOW_MESSAGE);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalListMutationOpcodesAsTypedRuntimeBlocks() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean add = new BlockBean("1", "", "", "addListStr");
        add.parameters.add("items");
        add.parameters.add("one");
        BlockBean clear = new BlockBean("2", "", "", "clearList");
        clear.parameters.add("items");
        add.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(add, clear));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported.get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.LIST_MUTATE);
        assertThat(imported.get(0).getPayload()).containsEntry("action", "add");
        assertThat(imported.get(1).getPayload()).containsEntry("action", "clear");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyWidgetDataFocusAndNavigationActionsAsTypedRuntimeCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean listData = new BlockBean("1", "", "", "listSetData");
        listData.parameters.add("list1");
        listData.parameters.add("items");
        BlockBean checked = new BlockBean("2", "", "", "listSetItemChecked");
        checked.parameters.add("list1");
        checked.parameters.add("2");
        checked.parameters.add("true");
        BlockBean scroll = new BlockBean("3", "", "", "listSmoothScrollTo");
        scroll.parameters.add("list1");
        scroll.parameters.add("4");
        BlockBean spinnerData = new BlockBean("4", "", "", "spnSetData");
        spinnerData.parameters.add("spinner1");
        spinnerData.parameters.add("items");
        BlockBean focus = new BlockBean("5", "", "", "requestFocus");
        focus.parameters.add("input1");
        BlockBean back = new BlockBean("6", "", "", "webViewGoBack");
        back.parameters.add("web1");
        BlockBean forward = new BlockBean("7", "", "", "webViewGoForward");
        forward.parameters.add("web1");
        listData.nextBlock = 2;
        checked.nextBlock = 3;
        scroll.nextBlock = 4;
        spinnerData.nextBlock = 5;
        focus.nextBlock = 6;
        back.nextBlock = 7;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(listData, checked, scroll, spinnerData, focus, back, forward));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(7);
        for (CreatorRuntimeBlock block : imported) assertThat(block.getPayload().get("serviceId")).isEqualTo("widget");
        assertWidgetAction(imported.get(0), "list_set_data");
        assertWidgetAction(imported.get(1), "list_set_item_checked");
        assertWidgetAction(imported.get(2), "list_smooth_scroll_to");
        assertWidgetAction(imported.get(3), "spinner_set_data");
        assertWidgetAction(imported.get(4), "request_focus");
        assertWidgetAction(imported.get(5), "web_go_back");
        assertWidgetAction(imported.get(6), "web_go_forward");
        @SuppressWarnings("unchecked") Map<String, Object> dataArguments = (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        assertThat(dataArguments).containsEntry("itemsStateId", "items");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyWidgetMaintenanceActionsAsTypedRuntimeCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        String[] opcodes = {"spnSetSelection", "webViewSetCacheMode", "webViewClearCache", "webViewClearHistory",
                "webViewStopLoading", "webViewZoomIn", "webViewZoomOut", "calendarViewSetDate",
                "calendarViewSetMinDate", "calnedarViewSetMaxDate"};
        String[] actions = {"spinner_set_selection", "web_set_cache_mode", "web_clear_cache", "web_clear_history",
                "web_stop_loading", "web_zoom_in", "web_zoom_out", "calendar_set_date",
                "calendar_set_min_date", "calendar_set_max_date"};
        java.util.List<BlockBean> chain = new java.util.ArrayList<>();
        for (int index = 0; index < opcodes.length; index++) {
            BlockBean block = new BlockBean(String.valueOf(index + 1), "", "", opcodes[index]);
            boolean calendar = opcodes[index].startsWith("calendar") || opcodes[index].startsWith("calnedar");
            block.parameters.add(calendar ? "calendar1" : opcodes[index].startsWith("spn") ? "spinner1" : "web1");
            if (opcodes[index].equals("spnSetSelection")) block.parameters.add("2");
            else if (opcodes[index].equals("webViewSetCacheMode")) block.parameters.add("LOAD_NO_CACHE");
            else if (calendar) block.parameters.add("1700000000000");
            if (index + 1 < opcodes.length) block.nextBlock = index + 2;
            chain.add(block);
        }
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), chain);

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(actions.length);
        for (int index = 0; index < actions.length; index++) assertWidgetAction(imported.get(index), actions[index]);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyListCheckedPositionsAsTypedRuntimeStateOutput() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean positions = new BlockBean("1", "", "", "listGetCheckedPositions");
        positions.parameters.add("list1");
        positions.parameters.add("checkedIndexes");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(positions));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock call = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertWidgetAction(call, "list_checked_positions");
        @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) call.getPayload().get("arguments");
        assertThat(arguments).containsEntry("resultStateId", "checkedIndexes");
        assertThat(arguments).containsEntry("resultKey", "positions");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyProgressAndImageStyleActionsAsTypedRuntimeCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean indeterminate = new BlockBean("1", "", "", "progressbarSetIndeterminate");
        indeterminate.parameters.add("progress1");
        indeterminate.parameters.add("true");
        BlockBean filter = new BlockBean("2", "", "", "setColorFilter");
        filter.parameters.add("image1");
        filter.parameters.add("#ff102030");
        indeterminate.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(indeterminate, filter));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertWidgetAction(imported.get(0), "progress_set_indeterminate");
        assertWidgetAction(imported.get(1), "image_set_color_filter");
        @SuppressWarnings("unchecked") Map<String, Object> filterArguments = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(filterArguments).containsEntry("color", "#ff102030");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacySeekBarThumbAndTrackResourcesAsTypedWidgetProperties() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean thumb = new BlockBean("1", "", "", "setThumbResource");
        thumb.parameters.add("slider");
        thumb.parameters.add("Handle.9");
        BlockBean track = new BlockBean("2", "", "", "setTrackResource");
        track.parameters.add("slider");
        track.parameters.add("Track.9");
        thumb.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(thumb, track));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported.get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY);
        assertThat(imported.get(0).getPayload()).containsEntry("property", "thumbResource");
        assertThat(imported.get(0).getPayload()).containsEntry("value", "handle");
        assertThat(imported.get(1).getPayload()).containsEntry("property", "trackResource");
        assertThat(imported.get(1).getPayload()).containsEntry("value", "track");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyCustomAdapterDataBlocksAsTypedListMapStateReferences() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        String[] opcodes = {"listSetCustomViewData", "recyclerSetCustomViewData", "spnSetCustomViewData",
                "pagerSetCustomViewData", "gridSetCustomViewData"};
        String[] widgets = {"list", "recycler", "spinner", "pager", "grid"};
        java.util.List<BlockBean> chain = new java.util.ArrayList<>();
        for (int index = 0; index < opcodes.length; index++) {
            BlockBean block = new BlockBean(String.valueOf(index + 1), "", "", opcodes[index]);
            block.parameters.add(widgets[index]);
            block.parameters.add("rows");
            if (index + 1 < opcodes.length) block.nextBlock = index + 2;
            chain.add(block);
        }
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), chain);

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(opcodes.length);
        for (int index = 0; index < imported.size(); index++) {
            assertThat(imported.get(index).getType()).isEqualTo(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY);
            assertThat(imported.get(index).getPayload()).containsEntry("widgetId", widgets[index]);
            assertThat(imported.get(index).getPayload()).containsEntry("property", "customDataStateId");
            assertThat(imported.get(index).getPayload()).containsEntry("value", "rows");
        }
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyBannerAndInterstitialActionsAsTypedRuntimeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean banner = new BlockBean("1", "", "", "adViewLoadAd");
        banner.parameters.add("banner");
        BlockBean create = new BlockBean("2", "", "", "interstitialadCreate");
        create.parameters.add("interstitial1");
        BlockBean load = new BlockBean("3", "", "", "interstitialadLoadAd");
        load.parameters.add("interstitial1");
        BlockBean show = new BlockBean("4", "", "", "interstitialadShow");
        show.parameters.add("interstitial1");
        banner.nextBlock = 2;
        create.nextBlock = 3;
        load.nextBlock = 4;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(banner, create, load, show));
        ComponentBean interstitial = new ComponentBean(ComponentBean.COMPONENT_TYPE_INTERSTITIAL_AD,
                "interstitial1", "ca-app-pub-runtime-test");

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.singletonList(interstitial), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "widget", "ad_load");
        assertServiceCall(imported.get(1), "ads_interstitial", "create");
        assertServiceCall(imported.get(2), "ads_interstitial", "load");
        assertServiceCall(imported.get(3), "ads_interstitial", "show");
        @SuppressWarnings("unchecked") Map<String, Object> loadArguments = (Map<String, Object>) imported.get(2).getPayload().get("arguments");
        assertThat(loadArguments).containsEntry("adUnitId", "ca-app-pub-runtime-test");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyMapViewActionsAsTypedRuntimeNativeCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        String[] opcodes = {"mapViewSetMapType", "mapViewMoveCamera", "mapViewZoomTo", "mapViewZoomIn",
                "mapViewZoomOut", "mapViewAddMarker", "mapViewSetMarkerInfo", "mapViewSetMarkerPosition",
                "mapViewSetMarkerColor", "mapViewSetMarkerIcon", "mapViewSetMarkerVisible"};
        String[] actions = {"set_map_type", "move_camera", "zoom_to", "zoom_in", "zoom_out", "add_marker",
                "set_marker_info", "set_marker_position", "set_marker_color", "set_marker_icon", "set_marker_visible"};
        java.util.List<BlockBean> chain = new java.util.ArrayList<>();
        for (int index = 0; index < opcodes.length; index++) {
            BlockBean block = new BlockBean(String.valueOf(index + 1), "", "", opcodes[index]);
            block.parameters.add("map");
            if (index == 0) block.parameters.add("MAP_TYPE_SATELLITE");
            else if (index == 1) { block.parameters.add("41.2"); block.parameters.add("69.3"); }
            else if (index == 2) block.parameters.add("11");
            else if (index >= 5 && index <= 10) {
                block.parameters.add("marker");
                if (index == 5 || index == 7) { block.parameters.add("41.2"); block.parameters.add("69.3"); }
                else if (index == 6) { block.parameters.add("Title"); block.parameters.add("Snippet"); }
                else if (index == 8) { block.parameters.add("HUE_RED"); block.parameters.add("0.7"); }
                else if (index == 9) block.parameters.add("Pin.9");
                else block.parameters.add("true");
            }
            if (index + 1 < opcodes.length) block.nextBlock = index + 2;
            chain.add(block);
        }
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), chain);

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents()
                .get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(actions.length);
        for (int index = 0; index < actions.length; index++) assertServiceCall(imported.get(index), "map", actions[index]);
        @SuppressWarnings("unchecked") Map<String, Object> move = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> icon = (Map<String, Object>) imported.get(9).getPayload().get("arguments");
        assertThat(move).containsEntry("latitude", "41.2");
        assertThat(move).containsEntry("longitude", "69.3");
        assertThat(icon).containsEntry("resourceName", "pin");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyMapListInsertAndGetWithCanonicalArgumentOrder() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean insert = new BlockBean("1", "", "", "insertMapToList");
        insert.parameters.add("row");
        insert.parameters.add("1");
        insert.parameters.add("rows");
        BlockBean get = new BlockBean("2", "", "", "getMapInList");
        get.parameters.add("1");
        get.parameters.add("rows");
        get.parameters.add("selectedRow");
        insert.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(insert, get));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported.get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.LIST_MUTATE);
        assertThat(imported.get(0).getPayload()).containsEntry("stateId", "rows");
        assertThat(imported.get(0).getPayload()).containsEntry("action", "insert");
        assertThat(imported.get(1).getType()).isEqualTo(CreatorRuntimeBlock.Type.SET_STATE);
        assertThat(imported.get(1).getPayload()).containsEntry("stateId", "selectedRow");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalMapMutationOpcodesAsTypedRuntimeBlocks() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean put = new BlockBean("1", "", "", "mapPut");
        put.parameters.add("profile");
        put.parameters.add("name");
        put.parameters.add("Ada");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(put));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.MAP_MUTATE);
        assertThat(imported.getPayload()).containsEntry("action", "put");
        assertThat(imported.getPayload()).containsEntry("key", "name");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalIntentBlocksAsDirectRuntimeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean screen = new BlockBean("1", "", "", "intentSetScreen");
        screen.parameters.add("intent1");
        screen.parameters.add("details");
        BlockBean start = new BlockBean("2", "", "", "startActivity");
        start.parameters.add("intent1");
        screen.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(screen, start));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported.get(0).getPayload().get("serviceId")).isEqualTo("intent");
        @SuppressWarnings("unchecked") Map<String, Object> configure = (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> launch = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(configure).containsEntry("action", "configure_screen");
        assertThat(configure).containsEntry("screenId", "details");
        assertThat(launch).containsEntry("action", "start");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalDialogBlocksAsDirectRuntimeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean title = new BlockBean("1", "", "", "dialogSetTitle");
        title.parameters.add("dialog1");
        title.parameters.add("Runtime dialog");
        BlockBean show = new BlockBean("2", "", "", "dialogShow");
        show.parameters.add("dialog1");
        title.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(title, show));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        @SuppressWarnings("unchecked") Map<String, Object> configure = (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> launch = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(imported.get(0).getPayload().get("serviceId")).isEqualTo("dialog");
        assertThat(configure).containsEntry("action", "set_title");
        assertThat(launch).containsEntry("action", "show");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalMediaBlocksAsDirectRuntimeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean create = new BlockBean("1", "", "", "mediaplayerCreate");
        create.parameters.add("player1");
        create.parameters.add("intro");
        BlockBean play = new BlockBean("2", "", "", "mediaplayerStart");
        play.parameters.add("player1");
        create.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(create, play));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        @SuppressWarnings("unchecked") Map<String, Object> load = (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> start = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(imported.get(0).getPayload().get("serviceId")).isEqualTo("media");
        assertThat(load).containsEntry("action", "load_resource");
        assertThat(start).containsEntry("action", "play");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalUiBlocksAsDirectRuntimeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean title = new BlockBean("1", "", "", "setTitle");
        title.parameters.add("Creator Runtime");
        BlockBean copy = new BlockBean("2", "", "", "copyToClipboard");
        copy.parameters.add("copied");
        title.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(title, copy));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        @SuppressWarnings("unchecked") Map<String, Object> setTitle = (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> copyText = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(imported.get(0).getPayload().get("serviceId")).isEqualTo("ui");
        assertThat(setTitle).containsEntry("action", "set_title");
        assertThat(copyText).containsEntry("action", "copy_text");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsDeviceAndSpeechBlocksAsExistingRuntimeNativeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean gyro = new BlockBean("1", "", "", "gyroscopeStartListen");
        gyro.parameters.add("gyro1");
        BlockBean location = new BlockBean("2", "", "", "locationManagerRequestLocationUpdates");
        location.parameters.add("location1");
        location.parameters.add("LocationManager.NETWORK_PROVIDER");
        location.parameters.add("2500");
        location.parameters.add("5");
        BlockBean camera = new BlockBean("3", "", "", "camerastarttakepicture");
        camera.parameters.add("camera1");
        BlockBean picker = new BlockBean("4", "", "", "filepickerstartpickfiles");
        picker.parameters.add("picker1");
        BlockBean pitch = new BlockBean("5", "", "", "textToSpeechSetPitch");
        pitch.parameters.add("tts1");
        pitch.parameters.add("1.2");
        BlockBean speak = new BlockBean("6", "", "", "textToSpeechSpeak");
        speak.parameters.add("tts1");
        speak.parameters.add("Ready");
        BlockBean listen = new BlockBean("7", "", "", "speechToTextStartListening");
        listen.parameters.add("stt1");
        BlockBean stop = new BlockBean("8", "", "", "speechToTextStopListening");
        stop.parameters.add("stt1");
        BlockBean shutdown = new BlockBean("9", "", "", "speechToTextShutdown");
        shutdown.parameters.add("stt1");
        gyro.nextBlock = 2;
        location.nextBlock = 3;
        camera.nextBlock = 4;
        picker.nextBlock = 5;
        pitch.nextBlock = 6;
        speak.nextBlock = 7;
        listen.nextBlock = 8;
        stop.nextBlock = 9;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(gyro, location, camera, picker, pitch, speak, listen, stop, shutdown));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(9);
        assertServiceCall(imported.get(0), "gyroscope", "start");
        assertServiceCall(imported.get(1), "location", "start");
        @SuppressWarnings("unchecked") Map<String, Object> locationArguments =
                (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(locationArguments).containsEntry("provider", "network");
        assertServiceCall(imported.get(2), "camera", "capture");
        assertServiceCall(imported.get(3), "file_picker", "pick");
        assertServiceCall(imported.get(4), "text_to_speech", "set_pitch");
        assertServiceCall(imported.get(5), "text_to_speech", "speak");
        assertServiceCall(imported.get(6), "speech_to_text", "listen");
        assertServiceCall(imported.get(7), "speech_to_text", "stop");
        assertServiceCall(imported.get(8), "speech_to_text", "shutdown");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsMutatingLegacyFileUtilityBlocksAsDirectRuntimeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean write = new BlockBean("1", "", "", "fileutilwrite");
        write.parameters.add("/storage/emulated/0/creator/note.txt");
        write.parameters.add("runtime native");
        BlockBean copy = new BlockBean("2", "", "", "fileutilcopy");
        copy.parameters.add("/storage/emulated/0/creator/note.txt");
        copy.parameters.add("/storage/emulated/0/creator/copy.txt");
        BlockBean directory = new BlockBean("3", "", "", "fileutilmakedir");
        directory.parameters.add("/storage/emulated/0/creator/archive");
        write.nextBlock = 2;
        copy.nextBlock = 3;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(write, copy, directory));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "file", "write");
        assertServiceCall(imported.get(1), "file", "copy");
        assertServiceCall(imported.get(2), "file", "make_dir");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyBitmapOperationsAsTypedRuntimeNativeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        String[] opcodes = {"resizeBitmapFileRetainRatio", "resizeBitmapFileToSquare", "resizeBitmapFileToCircle",
                "resizeBitmapFileWithRoundedBorder", "cropBitmapFileFromCenter", "rotateBitmapFile",
                "scaleBitmapFile", "skewBitmapFile", "setBitmapFileColorFilter", "setBitmapFileBrightness",
                "setBitmapFileContrast"};
        String[] actions = {"resize_retain_ratio", "resize_square", "resize_circle", "rounded_border",
                "crop_center", "rotate", "scale", "skew", "color_filter", "brightness", "contrast"};
        java.util.List<BlockBean> chain = new java.util.ArrayList<>();
        for (int index = 0; index < opcodes.length; index++) {
            BlockBean block = new BlockBean(String.valueOf(index + 1), "", "", opcodes[index]);
            block.parameters.add("/storage/emulated/0/source.png");
            block.parameters.add("/storage/emulated/0/output.png");
            if (!"resizeBitmapFileToCircle".equals(opcodes[index])) block.parameters.add("12");
            if ("cropBitmapFileFromCenter".equals(opcodes[index]) || "scaleBitmapFile".equals(opcodes[index])
                    || "skewBitmapFile".equals(opcodes[index])) block.parameters.add("24");
            if (index + 1 < opcodes.length) block.nextBlock = index + 2;
            chain.add(block);
        }
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), chain);

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(actions.length);
        for (int index = 0; index < actions.length; index++) assertServiceCall(imported.get(index), "bitmap", actions[index]);
        @SuppressWarnings("unchecked") Map<String, Object> cropArguments = (Map<String, Object>) imported.get(4).getPayload().get("arguments");
        assertThat(cropArguments).containsEntry("height", "12");
        assertThat(cropArguments).containsEntry("width", "24");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyObjectAnimatorConfigurationAsRuntimeNativeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean target = new BlockBean("1", "", "", "objectanimatorSetTarget");
        target.parameters.add("animator1");
        target.parameters.add("button");
        BlockBean property = new BlockBean("2", "", "", "objectanimatorSetProperty");
        property.parameters.add("animator1");
        property.parameters.add("alpha");
        BlockBean range = new BlockBean("3", "", "", "objectanimatorSetFromTo");
        range.parameters.add("animator1");
        range.parameters.add("0");
        range.parameters.add("1");
        BlockBean duration = new BlockBean("4", "", "", "objectanimatorSetDuration");
        duration.parameters.add("animator1");
        duration.parameters.add("450");
        BlockBean start = new BlockBean("5", "", "", "objectanimatorStart");
        start.parameters.add("animator1");
        target.nextBlock = 2;
        property.nextBlock = 3;
        range.nextBlock = 4;
        duration.nextBlock = 5;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(target, property, range, duration, start));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "animator", "set_target");
        assertServiceCall(imported.get(1), "animator", "set_property");
        assertServiceCall(imported.get(2), "animator", "set_from_to");
        assertServiceCall(imported.get(3), "animator", "set_duration");
        assertServiceCall(imported.get(4), "animator", "start");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsSupportedLegacyFirebaseAuthOperationsAsRuntimeNativeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean register = new BlockBean("1", "", "", "firebaseauthCreateUser");
        register.parameters.add("auth1");
        register.parameters.add("ada@example.com");
        register.parameters.add("safe-pass");
        BlockBean reset = new BlockBean("2", "", "", "firebaseauthResetPassword");
        reset.parameters.add("auth1");
        reset.parameters.add("ada@example.com");
        BlockBean signOut = new BlockBean("3", "", "", "firebaseauthSignOutUser");
        signOut.parameters.add("auth1");
        register.nextBlock = 2;
        reset.nextBlock = 3;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(register, reset, signOut));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "firebase_auth", "register");
        assertServiceCall(imported.get(1), "firebase_auth", "reset_password");
        assertServiceCall(imported.get(2), "firebase_auth", "sign_out");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsSupportedLegacyFirebaseStorageOperationsAsRuntimeNativeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean upload = new BlockBean("1", "", "", "firebasestorageUploadFile");
        upload.parameters.add("storage1");
        upload.parameters.add("/storage/emulated/0/creator/photo.jpg");
        upload.parameters.add("uploads/photo.jpg");
        BlockBean delete = new BlockBean("2", "", "", "firebasestorageDelete");
        delete.parameters.add("storage1");
        delete.parameters.add("gs://creator.appspot.com/uploads/photo.jpg");
        upload.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(upload, delete));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "firebase_storage", "upload_file");
        assertServiceCall(imported.get(1), "firebase_storage", "delete_url");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyFirebaseDeleteAndListenersUsingComponentBasePath() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean delete = new BlockBean("1", "", "", "firebaseDelete");
        delete.parameters.add("firebase1");
        delete.parameters.add("users/ada");
        BlockBean listen = new BlockBean("2", "", "", "firebaseStartListen");
        listen.parameters.add("firebase1");
        BlockBean stop = new BlockBean("3", "", "", "firebaseStopListen");
        stop.parameters.add("firebase1");
        delete.nextBlock = 2;
        listen.nextBlock = 3;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(delete, listen, stop));
        ComponentBean firebase = new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE, "firebase1", "profiles");

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.singletonList(firebase), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "firebase", "remove");
        assertServiceCall(imported.get(1), "firebase", "listen");
        assertServiceCall(imported.get(2), "firebase", "stop_listen");
        @SuppressWarnings("unchecked") Map<String, Object> deleteArguments = (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> listenArguments = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(deleteArguments).containsEntry("path", "profiles/users/ada");
        assertThat(listenArguments).containsEntry("path", "profiles");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyFirebaseAddAndPushWithTypedMapStateAndBasePaths() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean add = new BlockBean("1", "", "", "firebaseAdd");
        add.parameters.add("firebase1");
        add.parameters.add("users/ada");
        add.parameters.add("profile");
        BlockBean push = new BlockBean("2", "", "", "firebasePush");
        push.parameters.add("firebase1");
        push.parameters.add("profile");
        add.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(add, push));
        ComponentBean firebase = new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE, "firebase1", "profiles");

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.singletonList(firebase), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "firebase", "update");
        assertServiceCall(imported.get(1), "firebase", "push_update");
        @SuppressWarnings("unchecked") Map<String, Object> addArguments = (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        @SuppressWarnings("unchecked") Map<String, Object> pushArguments = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(addArguments).containsEntry("path", "profiles/users/ada");
        assertThat(addArguments).containsEntry("valueStateId", "profile");
        assertThat(pushArguments).containsEntry("path", "profiles");
        assertThat(pushArguments).containsEntry("valueStateId", "profile");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyFirebaseGetChildrenWithTypedStateAndCallbackBinding() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean getChildren = new BlockBean("1", "", "", "firebaseGetChildren");
        getChildren.parameters.add("firebase1");
        getChildren.parameters.add("profilesRows");
        getChildren.subStack1 = 2;
        BlockBean callback = new BlockBean("2", "", "", "showMessage");
        callback.parameters.add("Profiles loaded");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(getChildren, callback));
        ComponentBean firebase = new ComponentBean(ComponentBean.COMPONENT_TYPE_FIREBASE, "firebase1", "profiles");

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.singletonList(firebase), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock call = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertServiceCall(call, "firebase", "get_children");
        @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) call.getPayload().get("arguments");
        assertThat(arguments).containsEntry("path", "profiles");
        assertThat(arguments).containsEntry("resultStateId", "profilesRows");
        assertThat(arguments).containsEntry("callbackTargetId", "legacy_firebase_children_callback_1");
        CreatorEventBinding callbackBinding = result.getDocument().getEvents()
                .get("legacy_firebase_children_callback_1");
        assertThat(callbackBinding.getTargetWidgetId()).isEqualTo("legacy_firebase_children_callback_1");
        assertThat(callbackBinding.getEventName()).isEqualTo("children");
        assertThat(callbackBinding.getBlocks().get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.SHOW_MESSAGE);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyPickerDialogShowBlocksAsRuntimeNativeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean date = new BlockBean("1", "", "", "datePickerDialogShow");
        BlockBean time = new BlockBean("2", "", "", "timePickerDialogShow");
        time.parameters.add("timePicker1");
        date.nextBlock = 2;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(date, time));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "date_picker", "show");
        assertServiceCall(imported.get(1), "time_picker", "show");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsMutatingLegacyCalendarBlocksAsRuntimeNativeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean now = new BlockBean("1", "", "", "calendarGetNow");
        now.parameters.add("calendar1");
        BlockBean add = new BlockBean("2", "", "", "calendarAdd");
        add.parameters.add("calendar1");
        add.parameters.add("Calendar.DAY_OF_MONTH");
        add.parameters.add("2");
        BlockBean setTime = new BlockBean("3", "", "", "calendarSetTime");
        setTime.parameters.add("calendar1");
        setTime.parameters.add("1735689600000");
        now.nextBlock = 2;
        add.nextBlock = 3;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(now, add, setTime));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "calendar", "reset");
        assertServiceCall(imported.get(1), "calendar", "add");
        assertServiceCall(imported.get(2), "calendar", "set_time");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyFileComponentMutationsWithNamedStorageMetadata() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean setFile = new BlockBean("1", "", "", "fileSetFileName");
        setFile.parameters.add("settings1");
        setFile.parameters.add("alternate_settings");
        BlockBean setData = new BlockBean("2", "", "", "fileSetData");
        setData.parameters.add("settings1");
        setData.parameters.add("theme");
        setData.parameters.add("dark");
        BlockBean remove = new BlockBean("3", "", "", "fileRemoveData");
        remove.parameters.add("settings1");
        remove.parameters.add("theme");
        setFile.nextBlock = 2;
        setData.nextBlock = 3;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(setFile, setData, remove));
        ComponentBean storage = new ComponentBean(ComponentBean.COMPONENT_TYPE_SHAREDPREF, "settings1", "default_settings");

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.singletonList(storage), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "local_storage", "configure");
        assertServiceCall(imported.get(1), "local_storage", "set");
        assertServiceCall(imported.get(2), "local_storage", "remove");
        @SuppressWarnings("unchecked") Map<String, Object> setArguments = (Map<String, Object>) imported.get(1).getPayload().get("arguments");
        assertThat(setArguments).containsEntry("storeName", "default_settings");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyRequestNetworkConfigurationAsRuntimeNativeServiceCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean params = new BlockBean("1", "", "", "requestnetworkSetParams");
        params.parameters.add("network1");
        params.parameters.add("requestParams");
        params.parameters.add("REQUEST_PARAM");
        BlockBean headers = new BlockBean("2", "", "", "requestnetworkSetHeaders");
        headers.parameters.add("network1");
        headers.parameters.add("requestHeaders");
        BlockBean start = new BlockBean("3", "", "", "requestnetworkStartRequestNetwork");
        start.parameters.add("network1");
        start.parameters.add("GET");
        start.parameters.add("https://example.test/probe");
        start.parameters.add("profile");
        params.nextBlock = 2;
        headers.nextBlock = 3;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(params, headers, start));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported =
                result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "http", "set_params");
        assertServiceCall(imported.get(1), "http", "set_headers");
        assertServiceCall(imported.get(2), "http", "start");
        @SuppressWarnings("unchecked") Map<String, Object> paramsArguments = (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        assertThat(paramsArguments).containsEntry("paramsStateId", "requestParams");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalIfElseAndRepeatSubstacksAsTypedControlFlow() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean conditional = new BlockBean("1", "", "", "ifElse");
        conditional.parameters.add("enabled");
        conditional.subStack1 = 2;
        conditional.subStack2 = 3;
        BlockBean thenIncrement = new BlockBean("2", "", "", "increaseInt");
        thenIncrement.parameters.add("counter");
        BlockBean elseIncrement = new BlockBean("3", "", "", "decreaseInt");
        elseIncrement.parameters.add("counter");
        BlockBean repeat = new BlockBean("4", "", "", "repeat");
        repeat.parameters.add("3");
        repeat.subStack1 = 5;
        BlockBean repeatedIncrement = new BlockBean("5", "", "", "increaseInt");
        repeatedIncrement.parameters.add("counter");
        conditional.nextBlock = 4;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(conditional, thenIncrement, elseIncrement, repeat, repeatedIncrement));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(2);
        assertThat(imported.get(0).getType()).isEqualTo(CreatorRuntimeBlock.Type.IF_BOOLEAN);
        assertThat(imported.get(0).getThenBlocks()).hasSize(1);
        assertThat(imported.get(0).getElseBlocks()).hasSize(1);
        assertThat(imported.get(1).getType()).isEqualTo(CreatorRuntimeBlock.Type.REPEAT);
        assertThat(imported.get(1).getThenBlocks()).hasSize(1);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyProgressDialogConfigurationAndVisibilityAsRuntimeNativeCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean title = new BlockBean("1", "", "", "progressdialogSetTitle");
        title.parameters.add("progress1");
        title.parameters.add("Loading");
        BlockBean max = new BlockBean("2", "", "", "progressdialogSetMax");
        max.parameters.add("progress1");
        max.parameters.add("100");
        BlockBean show = new BlockBean("3", "", "", "progressdialogShow");
        show.parameters.add("progress1");
        BlockBean dismiss = new BlockBean("4", "", "", "progressdialogDismiss");
        dismiss.parameters.add("progress1");
        title.nextBlock = 2;
        max.nextBlock = 3;
        show.nextBlock = 4;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(title, max, show, dismiss));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "dialog", "progress_set_title");
        assertServiceCall(imported.get(1), "dialog", "progress_set_max");
        assertServiceCall(imported.get(2), "dialog", "show_progress");
        assertServiceCall(imported.get(3), "dialog", "dismiss_progress");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCanonicalSoundPoolBlocksAsRuntimeNativeMediaCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean create = new BlockBean("1", "", "", "soundpoolCreate");
        create.parameters.add("soundPool1");
        create.parameters.add("4");
        BlockBean load = new BlockBean("2", "", "", "soundpoolLoad");
        load.parameters.add("soundPool1");
        load.parameters.add("click_sound");
        BlockBean play = new BlockBean("3", "", "", "soundpoolStreamPlay");
        play.parameters.add("soundPool1");
        play.parameters.add("7");
        play.parameters.add("0");
        BlockBean stop = new BlockBean("4", "", "", "soundpoolStreamStop");
        stop.parameters.add("soundPool1");
        stop.parameters.add("11");
        create.nextBlock = 2;
        load.nextBlock = 3;
        play.nextBlock = 4;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(create, load, play, stop));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "media", "sound_create");
        assertServiceCall(imported.get(1), "media", "sound_load_name");
        assertServiceCall(imported.get(2), "media", "sound_play_stream");
        assertServiceCall(imported.get(3), "media", "sound_stop_stream");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsNestedReporterExpressionsForIfAndRepeatWithoutJavaFallback() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean conditional = new BlockBean("1", "", "", "if");
        conditional.parameters.add("@2");
        conditional.subStack1 = 3;
        BlockBean equals = new BlockBean("2", "", "", "stringEquals");
        equals.parameters.add("name");
        equals.parameters.add("Ada");
        BlockBean increment = new BlockBean("3", "", "", "increaseInt");
        increment.parameters.add("counter");
        BlockBean repeat = new BlockBean("4", "", "", "repeat");
        repeat.parameters.add("@5");
        repeat.subStack1 = 6;
        BlockBean sum = new BlockBean("5", "", "", "+");
        sum.parameters.add("1");
        sum.parameters.add("2");
        BlockBean repeated = new BlockBean("6", "", "", "increaseInt");
        repeated.parameters.add("counter");
        conditional.nextBlock = 4;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(conditional, equals, increment, repeat, sum, repeated));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported.get(0).getPayload()).containsKey("expression");
        assertThat(imported.get(1).getPayload()).containsKey("countExpression");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsNestedReporterExpressionsAsTypedSetVarStateValues() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean setVar = new BlockBean("1", "", "", "setVarInt");
        setVar.parameters.add("total");
        setVar.parameters.add("@2");
        BlockBean sum = new BlockBean("2", "", "", "+");
        sum.parameters.add("4");
        sum.parameters.add("5");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(setVar, sum));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.SET_STATE);
        assertThat(imported.getPayload()).containsKey("expression");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsNestedReporterExpressionForCommonWidgetSetterValue() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean setText = new BlockBean("1", "", "", "setText");
        setText.parameters.add("label");
        setText.parameters.add("@2");
        BlockBean join = new BlockBean("2", "", "", "stringJoin");
        join.parameters.add("Hello ");
        join.parameters.add("name");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(setText, join));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.SET_WIDGET_PROPERTY);
        assertThat(imported.getPayload()).containsKey("expression");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsNestedReporterExpressionsForListAndMapMutationValues() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean listAdd = new BlockBean("1", "", "", "addListInt");
        listAdd.parameters.add("items");
        listAdd.parameters.add("@2");
        BlockBean sum = new BlockBean("2", "", "", "+");
        sum.parameters.add("2"); sum.parameters.add("3");
        BlockBean mapPut = new BlockBean("3", "", "", "mapPut");
        mapPut.parameters.add("profile"); mapPut.parameters.add("score"); mapPut.parameters.add("@4");
        BlockBean length = new BlockBean("4", "", "", "stringLength");
        length.parameters.add("Ada");
        listAdd.nextBlock = 3;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(listAdd, sum, mapPut, length));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported.get(0).getPayload()).containsKey("valueExpression");
        assertThat(imported.get(1).getPayload()).containsKey("valueExpression");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyBluetoothConnectionAndPairedDeviceBlocksAsRuntimeNativeCalls() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean ready = new BlockBean("1", "", "", "bluetoothConnectReadyConnectionToUuid");
        ready.parameters.add("bluetooth1"); ready.parameters.add("00001101-0000-1000-8000-00805F9B34FB"); ready.parameters.add("server");
        BlockBean start = new BlockBean("2", "", "", "bluetoothConnectStartConnection");
        start.parameters.add("bluetooth1"); start.parameters.add("00:11:22:33:44:55"); start.parameters.add("client");
        BlockBean send = new BlockBean("3", "", "", "bluetoothConnectSendData");
        send.parameters.add("bluetooth1"); send.parameters.add("hello"); send.parameters.add("client");
        BlockBean paired = new BlockBean("4", "", "", "bluetoothConnectGetPairedDevices");
        paired.parameters.add("bluetooth1"); paired.parameters.add("paired");
        ready.nextBlock = 2; start.nextBlock = 3; send.nextBlock = 4;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(ready, start, send, paired));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertServiceCall(imported.get(0), "bluetooth", "ready_connection");
        assertServiceCall(imported.get(1), "bluetooth", "start_connection");
        assertServiceCall(imported.get(2), "bluetooth", "send_data");
        assertServiceCall(imported.get(3), "bluetooth", "paired_devices");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyFirebaseStorageDownloadAsRuntimeNativeCall() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean download = new BlockBean("1", "", "", "firebaseStorageDownloadFile");
        download.parameters.add("storage1");
        download.parameters.add("gs://example.appspot.com/photos/ada.jpg");
        download.parameters.add("/storage/emulated/0/Download/ada.jpg");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(download));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertServiceCall(imported, "firebase_storage", "download_file");
        @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) imported.getPayload().get("arguments");
        assertThat(arguments).containsEntry("url", "gs://example.appspot.com/photos/ada.jpg");
        assertThat(arguments).containsEntry("filePath", "/storage/emulated/0/Download/ada.jpg");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyFileUtilListDirAsTypedRuntimeStateOutput() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean listDir = new BlockBean("1", "", "", "fileutilListDir");
        listDir.parameters.add("/storage/emulated/0/Download");
        listDir.parameters.add("entries");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(listDir));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertServiceCall(imported, "file", "list_dir");
        @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) imported.getPayload().get("arguments");
        assertThat(arguments).containsEntry("resultStateId", "entries");
        assertThat(arguments).containsEntry("resultKey", "entries");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void rejectsConditionalWithMissingSubstackReference() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean branch = new BlockBean("1", "", "", "if_state_equals");
        branch.parameters.add("status");
        branch.parameters.add("approved");
        branch.subStack1 = 99;
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Collections.singletonList(branch));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        assertThat(result.getDocument().getEvents()).isEmpty();
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(1);
    }

    @Test public void importsProjectMetadataAndBlocksArbitraryNativeLibraries() {
        ProjectFileBean activity = new ProjectFileBean(ProjectFileBean.PROJECT_FILE_TYPE_ACTIVITY, "main");
        activity.options |= ProjectFileBean.OPTION_ACTIVITY_DRAWER | ProjectFileBean.OPTION_ACTIVITY_FAB;
        ProjectLibraryBean firebase = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE);
        firebase.useYn = ProjectLibraryBean.LIB_USE_Y;
        ProjectLibraryBean nativeLibrary = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB);

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importProjectMetadata(
                documentWithButton(), Collections.singletonList(activity), Arrays.asList(firebase, nativeLibrary));

        assertThat(result.getDocument().getState()).containsKey("legacy.projectFiles");
        @SuppressWarnings("unchecked") Map<String, Object> projectFileIndex =
                (Map<String, Object>) result.getDocument().getState().get("legacy.projectFileIndex");
        @SuppressWarnings("unchecked") Map<String, Object> main = (Map<String, Object>) projectFileIndex.get("main");
        assertThat(main).containsEntry("runtimeKind", "activity");
        assertThat(main).containsEntry("activityName", "MainActivity");
        assertThat(main).containsEntry("javaName", "MainActivity.java");
        assertThat(main).containsEntry("xmlName", "main.xml");
        assertThat(main).containsEntry("hasFab", true);
        assertThat(main).containsEntry("drawerName", "_drawer_main");
        assertThat(main).containsEntry("drawerXmlName", "_drawer_main.xml");
        @SuppressWarnings("unchecked") Map<String, Object> relationships = (Map<String, Object>) main.get("relationships");
        assertThat(relationships).containsEntry("drawerId", "_drawer_main");
        assertThat(relationships).containsEntry("drawerLayoutResource", "_drawer_main.xml");
        assertThat(result.getDocument().getState()).containsKey("legacy.libraries");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(2);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(1);
    }

    @Test public void preservesResourceMetadataForLiveRuntimeConsumption() {
        ProjectResourceBean image = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                "hero", "images/hero.svg");
        image.rotate = 90;
        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importResources(
                documentWithButton(), Collections.singletonList(image));

        @SuppressWarnings("unchecked") java.util.List<Object> resources =
                (java.util.List<Object>) result.getDocument().getState().get("legacy.resources");
        @SuppressWarnings("unchecked") Map<String, Object> descriptor = (Map<String, Object>) resources.get(0);
        assertThat(descriptor.get("name")).isEqualTo("hero");
        assertThat(descriptor.get("source")).isEqualTo("images/hero.svg");
        assertThat(descriptor.get("svg")).isEqualTo(true);
        assertThat(descriptor.get("kind")).isEqualTo("image");
        @SuppressWarnings("unchecked") Map<String, Object> images =
                (Map<String, Object>) result.getDocument().getState().get("legacy.imageResources");
        assertThat(images).containsKey("hero");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(1);
    }

    @Test public void preservesVideoResourceMetadataForRuntimeNativePlaybackConsumption() {
        ProjectResourceBean video = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                "intro", "videos/intro.webm");

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importResources(
                documentWithButton(), Collections.singletonList(video));

        @SuppressWarnings("unchecked") Map<String, Object> videos =
                (Map<String, Object>) result.getDocument().getState().get("legacy.videoResources");
        @SuppressWarnings("unchecked") Map<String, Object> descriptor = (Map<String, Object>) videos.get("intro");
        assertThat(descriptor).containsEntry("kind", "video");
        assertThat(descriptor).containsEntry("source", "videos/intro.webm");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(1);
    }

    @Test public void preservesSoundResourceMetadataForRuntimeNativeMediaConsumption() {
        ProjectResourceBean sound = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                "intro", "sounds/intro.mp3");
        sound.curSoundPosition = 120;
        sound.totalSoundDuration = 2400;

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importResources(
                documentWithButton(), Collections.singletonList(sound));

        @SuppressWarnings("unchecked") Map<String, Object> sounds =
                (Map<String, Object>) result.getDocument().getState().get("legacy.soundResources");
        @SuppressWarnings("unchecked") Map<String, Object> descriptor = (Map<String, Object>) sounds.get("intro");
        assertThat(descriptor).containsEntry("kind", "sound");
        assertThat(descriptor).containsEntry("source", "sounds/intro.mp3");
        assertThat(descriptor).containsEntry("currentSoundPosition", 120);
        assertThat(descriptor).containsEntry("totalSoundDuration", 2400);
    }

    @Test public void preservesFontResourcesForLiveRuntimeConsumption() {
        ProjectResourceBean font = new ProjectResourceBean(ProjectResourceBean.PROJECT_RES_TYPE_FILE,
                "headline", "fonts/headline.otf");

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importResources(
                documentWithButton(), Collections.singletonList(font));

        @SuppressWarnings("unchecked") Map<String, Object> fonts =
                (Map<String, Object>) result.getDocument().getState().get("legacy.fontResources");
        @SuppressWarnings("unchecked") Map<String, Object> descriptor = (Map<String, Object>) fonts.get("headline");
        assertThat(descriptor).containsEntry("kind", "font");
        assertThat(descriptor).containsEntry("source", "fonts/headline.otf");
    }

    private static void assertServiceCall(CreatorRuntimeBlock block, String serviceId, String action) {
        assertThat(block.getType()).isEqualTo(CreatorRuntimeBlock.Type.RUNTIME_SERVICE_CALL);
        assertThat(block.getPayload().get("serviceId")).isEqualTo(serviceId);
        @SuppressWarnings("unchecked") Map<String, Object> arguments = (Map<String, Object>) block.getPayload().get("arguments");
        assertThat(arguments).containsEntry("action", action);
    }

    private static void assertWidgetAction(CreatorRuntimeBlock block, String action) {
        assertServiceCall(block, "widget", action);
    }

    private static CreatorProjectDocument documentWithButton() {
        Map<String, CreatorWidget> widgets = new LinkedHashMap<>();
        widgets.put("root", new CreatorWidget("root", "column", null, Arrays.asList("button"), null));
        widgets.put("button", new CreatorWidget("button", "button", "root", null, null));
        Map<String, CreatorScreen> screens = new LinkedHashMap<>();
        screens.put("home", new CreatorScreen("home", "/", "root"));
        return new CreatorProjectDocument(CreatorProjectDocument.SCHEMA_VERSION, "project", 0, "Demo", "home",
                screens, widgets, CreatorEntryControl.defaultControl());
    }
}
