package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.besome.sketch.beans.BlockBean;
import com.besome.sketch.beans.ComponentBean;
import com.besome.sketch.beans.EventBean;
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
        ProjectLibraryBean firebase = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_FIREBASE);
        firebase.useYn = ProjectLibraryBean.LIB_USE_Y;
        ProjectLibraryBean nativeLibrary = new ProjectLibraryBean(ProjectLibraryBean.PROJECT_LIB_TYPE_NATIVE_LIB);

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importProjectMetadata(
                documentWithButton(), Collections.singletonList(activity), Arrays.asList(firebase, nativeLibrary));

        assertThat(result.getDocument().getState()).containsKey("legacy.projectFiles");
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
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(1);
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
