package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;
import java.util.Collections;

public class CreatorRuntimePluginDispatcherTest {
    @Test public void dispatchesOnlyRegisteredReviewedPlugin() {
        CreatorRuntimePluginDispatcher dispatcher = new CreatorRuntimePluginDispatcher()
                .register(new CreatorRuntimePlugin() {
                    @Override public String getId() { return "storage"; }
                    @Override public Result execute(java.util.Map<String, Object> arguments) {
                        return new Result(Status.SUCCEEDED, Collections.singletonMap("saved", true), null);
                    }
                });
        assertThat(dispatcher.dispatch("storage", Collections.emptyMap()).getStatus())
                .isEqualTo(CreatorRuntimePlugin.Status.SUCCEEDED);
        assertThat(dispatcher.dispatch("unreviewed", Collections.emptyMap()).getStatus())
                .isEqualTo(CreatorRuntimePlugin.Status.UNSUPPORTED_ARGUMENT);
    }
}
