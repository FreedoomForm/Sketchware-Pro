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
