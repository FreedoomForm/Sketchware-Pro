package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.Test;

public class CreatorNetworkServiceTest {
    @Test public void rejectsNonHttpUrlsBeforeCreatingARequest() {
        CreatorNetworkService service = new CreatorNetworkService((id, event, payload) -> { }, new OkHttpClient());
        Map<String, Object> arguments = new LinkedHashMap<>();
        arguments.put("url", "file:///tmp/not-allowed");

        assertThat(service.execute(arguments).getStatus()).isEqualTo(CreatorRuntimeService.Status.UNSUPPORTED_ARGUMENT);
    }

    @Test public void startsValidRequestAndPublishesTypedResponseEvent() throws Exception {
        try (MockWebServer server = new MockWebServer()) {
            server.enqueue(new MockResponse().setResponseCode(200).setBody("runtime"));
            server.start();
            CountDownLatch received = new CountDownLatch(1);
            Map<String, Object> payload = new LinkedHashMap<>();
            CreatorNetworkService service = new CreatorNetworkService((id, event, result) -> {
                if ("http".equals(id) && "response".equals(event)) {
                    payload.putAll(result);
                    received.countDown();
                }
            }, new OkHttpClient());
            Map<String, Object> arguments = new LinkedHashMap<>();
            arguments.put("url", server.url("probe").toString());
            arguments.put("method", "GET");

            CreatorRuntimeService.Result result = service.execute(arguments);

            assertThat(result.getStatus()).isEqualTo(CreatorRuntimeService.Status.SUCCEEDED);
            assertThat(result.getOutput()).containsEntry("started", true);
            assertThat(received.await(2, TimeUnit.SECONDS)).isTrue();
            assertThat(payload).containsEntry("statusCode", 200);
            assertThat(payload).containsEntry("body", "runtime");
        }
    }
}
