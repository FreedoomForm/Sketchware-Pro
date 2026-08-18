package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.util.Collections;
import org.junit.Test;

public class CreatorRuntimeServiceDispatcherTest {
    @Test public void dispatchesOnlyRegisteredRuntimeServices() {
        CreatorRuntimeServiceDispatcher dispatcher = new CreatorRuntimeServiceDispatcher()
                .register(new CreatorRuntimeService() {
                    @Override public String getId() { return "reviewed_service"; }
                    @Override public Result execute(java.util.Map<String, Object> arguments) {
                        return new Result(Status.SUCCEEDED, Collections.singletonMap("executed", true), null);
                    }
                });
        assertThat(dispatcher.dispatch("reviewed_service", Collections.emptyMap()).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
        assertThat(dispatcher.dispatch("unregistered", Collections.emptyMap()).getStatus())
                .isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
        assertThat(dispatcher.registered()).containsKey("reviewed_service");
    }
}
