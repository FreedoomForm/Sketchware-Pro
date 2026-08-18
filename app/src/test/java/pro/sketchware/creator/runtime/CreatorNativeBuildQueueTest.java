package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.besome.sketch.beans.ComponentBean;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CreatorNativeBuildQueueTest {

    @Test public void buildRunnerReceivesPinnedRevisionAndEmitsLifecycleEvents() {
        CreatorProjectDocument document = CreatorProjectDocument.empty("project", "Demo").withRevision(7);
        CreatorRuntimeEventLog log = new CreatorRuntimeEventLog(10);
        List<String> states = new ArrayList<>();
        CreatorNativeBuildQueue queue = new CreatorNativeBuildQueue(Runnable::run, log,
                pinned -> assertThat(pinned.getRevision()).isEqualTo(7L));

        String buildId = queue.enqueue(document, (id, status, detail) -> states.add(status.name()));

        assertThat(buildId).startsWith("build-");
        assertThat(states).containsExactly("QUEUED", "RUNNING", "SUCCEEDED").inOrder();
        assertThat(log.snapshot()).hasSize(3);
        assertThat(log.snapshot().get(2).getName()).isEqualTo("build.completed");
        assertThat(log.snapshot().get(2).getAttributes().get("sourceRevision")).isEqualTo(7L);
    }

    @Test public void componentIsAssignedToRuntimePluginInsteadOfFallback() {
        assertThat(CreatorLegacyComponentCapabilityMatrix.tierFor(
                ComponentBean.COMPONENT_TYPE_FIREBASE_CLOUD_MESSAGE))
                .isEqualTo(CreatorCompatibilityTier.R2_RUNTIME_PLUGIN);
    }
}
