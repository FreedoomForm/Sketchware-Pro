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
