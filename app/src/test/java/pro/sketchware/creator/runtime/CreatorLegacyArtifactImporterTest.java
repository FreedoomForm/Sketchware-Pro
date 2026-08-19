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

    @Test public void importsLegacyServiceOpcodesWithStructuredArguments() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean request = new BlockBean("1", "", "", "requestnetworkStartRequestNetwork");
        request.parameters.add("network");
        request.parameters.add("GET");
        request.parameters.add("https://example.test/api");
        request.parameters.add("");
        request.nextBlock = 2;
        BlockBean speak = new BlockBean("2", "", "", "textToSpeechSpeak");
        speak.parameters.add("tts");
        speak.parameters.add("Hello");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(speak, request));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(2);
        assertThat(imported.get(0).getPayload().get("serviceId")).isEqualTo("http");
        @SuppressWarnings("unchecked") Map<String, Object> httpArgs =
                (Map<String, Object>) imported.get(0).getPayload().get("arguments");
        assertThat(httpArgs.get("url")).isEqualTo("https://example.test/api");
        assertThat(imported.get(1).getPayload().get("serviceId")).isEqualTo("text_to_speech");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsCommonWidgetSettersAsTypedProperties() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean visible = new BlockBean("1", "", "", "setVisible");
        visible.parameters.add("button");
        visible.parameters.add("VISIBLE");
        visible.nextBlock = 2;
        BlockBean alpha = new BlockBean("2", "", "", "setAlpha");
        alpha.parameters.add("button");
        alpha.parameters.add("0.5");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(alpha, visible));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        java.util.List<CreatorRuntimeBlock> imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks();
        assertThat(imported).hasSize(2);
        assertThat(imported.get(0).getPayload().get("property")).isEqualTo("visibility");
        assertThat(imported.get(1).getPayload().get("property")).isEqualTo("alpha");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyIfElseAsTypedCondition() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean branch = new BlockBean("1", "", "", "ifElse");
        branch.parameters.add("status == \"approved\"");
        branch.subStack1 = 2;
        branch.subStack2 = 3;
        BlockBean yes = new BlockBean("2", "", "", "showMessage");
        yes.parameters.add("Approved");
        BlockBean no = new BlockBean("3", "", "", "showMessage");
        no.parameters.add("Pending");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(branch, yes, no));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.IF_CONDITION);
        assertThat(imported.getPayload().get("operator")).isEqualTo("equals");
        assertThat(imported.getThenBlocks()).hasSize(1);
        assertThat(imported.getElseBlocks()).hasSize(1);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }

    @Test public void importsLegacyRepeatWithTypedBody() {
        EventBean click = new EventBean(EventBean.EVENT_TYPE_VIEW, 3, "button", "onClick");
        BlockBean repeat = new BlockBean("1", "", "", "repeat");
        repeat.parameters.add("3");
        repeat.subStack1 = 2;
        BlockBean body = new BlockBean("2", "", "", "showMessage");
        body.parameters.add("Tick");
        Map<String, java.util.List<BlockBean>> blocks = new LinkedHashMap<>();
        blocks.put(click.getEventKey(), Arrays.asList(repeat, body));

        CreatorLegacyArtifactImporter.Result result = new CreatorLegacyArtifactImporter().importArtifacts(
                documentWithButton(), Collections.emptyList(), Collections.singletonList(click), blocks);

        CreatorRuntimeBlock imported = result.getDocument().getEvents().get("legacy_button_onClick").getBlocks().get(0);
        assertThat(imported.getType()).isEqualTo(CreatorRuntimeBlock.Type.REPEAT);
        assertThat(imported.getPayload().get("count")).isEqualTo("3");
        assertThat(imported.getThenBlocks()).hasSize(1);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
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
