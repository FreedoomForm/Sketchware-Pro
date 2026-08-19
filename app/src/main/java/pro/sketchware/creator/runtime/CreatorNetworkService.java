package pro.sketchware.creator.runtime;

import java.io.IOException;
import com.google.gson.Gson;
import java.util.LinkedHashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Runtime-native asynchronous HTTP service for the legacy RequestNetwork component. */
public final class CreatorNetworkService implements CreatorRuntimeService {
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final CreatorRuntimeEnvironment environment;
    private final OkHttpClient client;
    private final Map<String, Map<String, Object>> headersByRequest = new LinkedHashMap<>();
    private final Map<String, Map<String, Object>> paramsByRequest = new LinkedHashMap<>();

    public CreatorNetworkService(CreatorRuntimeEnvironment environment) {
        this(environment, new OkHttpClient());
    }

    CreatorNetworkService(CreatorRuntimeEnvironment environment, OkHttpClient client) {
        this.environment = environment;
        this.client = client;
    }

    @Override public String getId() { return "http"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        String requestId = CreatorRuntimeServiceArguments.string(arguments, "requestId");
        if ("set_headers".equals(action)) {
            if (requestId == null) return CreatorRuntimeServiceArguments.invalid("set_headers requires requestId.");
            headersByRequest.put(requestId, CreatorRuntimeServiceArguments.map(arguments, "headers"));
            return CreatorRuntimeServiceArguments.succeeded("requestId", requestId, "headersSet", true);
        }
        if ("set_params".equals(action)) {
            if (requestId == null) return CreatorRuntimeServiceArguments.invalid("set_params requires requestId.");
            paramsByRequest.put(requestId, CreatorRuntimeServiceArguments.map(arguments, "params"));
            return CreatorRuntimeServiceArguments.succeeded("requestId", requestId, "paramsSet", true);
        }
        String url = CreatorRuntimeServiceArguments.string(arguments, "url");
        String method = CreatorRuntimeServiceArguments.string(arguments, "method");
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            return CreatorRuntimeServiceArguments.invalid("http requires an http or https url.");
        }
        method = method == null ? "GET" : method.toUpperCase(java.util.Locale.US);
        try {
            Request.Builder request = new Request.Builder().url(url);
            Map<String, Object> headers = new LinkedHashMap<>(CreatorRuntimeServiceArguments.map(arguments, "headers"));
            if (requestId != null && headers.isEmpty()) headers.putAll(headersByRequest.getOrDefault(requestId, java.util.Collections.<String, Object>emptyMap()));
            for (Map.Entry<String, Object> header : headers.entrySet()) {
                request.header(header.getKey(), String.valueOf(header.getValue()));
            }
            String bodyText = CreatorRuntimeServiceArguments.string(arguments, "body");
            Map<String, Object> params = requestId == null ? java.util.Collections.<String, Object>emptyMap()
                    : paramsByRequest.getOrDefault(requestId, java.util.Collections.<String, Object>emptyMap());
            if (bodyText == null && !params.isEmpty()) bodyText = new Gson().toJson(params);
            RequestBody body = ("GET".equals(method) || "HEAD".equals(method)) ? null
                    : RequestBody.create(bodyText == null ? "" : bodyText, JSON);
            request.method(method, body);
            client.newCall(request.build()).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException error) {
                    environment.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                            "url", url, "message", error.getMessage() == null ? "Network request failed." : error.getMessage()));
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    try (Response closeable = response) {
                        String responseBody = closeable.body() == null ? "" : closeable.body().string();
                        Map<String, Object> output = new LinkedHashMap<>();
                        output.put("url", url);
                        output.put("statusCode", closeable.code());
                        output.put("body", responseBody);
                        output.put("successful", closeable.isSuccessful());
                        environment.publish(getId(), closeable.isSuccessful() ? "response" : "error", output);
                    }
                }
            });
            return CreatorRuntimeServiceArguments.succeeded("url", url, "method", method, "requestId", requestId, "started", true);
        } catch (IllegalArgumentException error) {
            return CreatorRuntimeServiceArguments.invalid(error.getMessage());
        }
    }
}
