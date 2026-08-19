package pro.sketchware.creator.runtime;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/** Runtime-native asynchronous HTTP service for the legacy RequestNetwork component. */
public final class CreatorNetworkService implements CreatorRuntimeService {
    interface EventPublisher { void publish(String serviceId, String eventName, Map<String, Object> payload); }
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private final EventPublisher publisher;
    private final OkHttpClient client;
    private final Map<String, Configuration> configurations = new LinkedHashMap<>();

    public CreatorNetworkService(CreatorRuntimeEnvironment environment) {
        this((serviceId, eventName, payload) -> environment.publish(serviceId, eventName, payload), new OkHttpClient());
    }

    CreatorNetworkService(CreatorRuntimeEnvironment environment, OkHttpClient client) {
        this((serviceId, eventName, payload) -> environment.publish(serviceId, eventName, payload), client);
    }

    CreatorNetworkService(EventPublisher publisher, OkHttpClient client) {
        if (publisher == null || client == null) throw new IllegalArgumentException("publisher/client");
        this.publisher = publisher;
        this.client = client;
    }

    @Override public String getId() { return "http"; }

    @Override public synchronized Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if ("set_params".equals(action) || "set_headers".equals(action) || "start".equals(action)) {
            String componentId = CreatorRuntimeServiceArguments.string(arguments, "componentId");
            if (componentId == null) return CreatorRuntimeServiceArguments.invalid("RequestNetwork action requires componentId.");
            Configuration configuration = configurations.get(componentId);
            if (configuration == null) {
                configuration = new Configuration();
                configurations.put(componentId, configuration);
            }
            if ("set_params".equals(action)) {
                configuration.params = CreatorRuntimeServiceArguments.map(arguments, "params");
                configuration.requestType = CreatorRuntimeServiceArguments.string(arguments, "requestType");
                return CreatorRuntimeServiceArguments.succeeded("configured", true, "componentId", componentId, "action", action);
            }
            if ("set_headers".equals(action)) {
                configuration.headers = CreatorRuntimeServiceArguments.map(arguments, "headers");
                return CreatorRuntimeServiceArguments.succeeded("configured", true, "componentId", componentId, "action", action);
            }
            Map<String, Object> merged = new LinkedHashMap<>(arguments);
            merged.put("params", configuration.params);
            merged.put("headers", configuration.headers);
            merged.put("requestType", configuration.requestType);
            return executeRequest(merged);
        }
        return executeRequest(arguments);
    }

    private Result executeRequest(Map<String, Object> arguments) {
        String url = CreatorRuntimeServiceArguments.string(arguments, "url");
        String method = CreatorRuntimeServiceArguments.string(arguments, "method");
        if (url == null || !(url.startsWith("https://") || url.startsWith("http://"))) {
            return CreatorRuntimeServiceArguments.invalid("http requires an http or https url.");
        }
        method = method == null ? "GET" : method.toUpperCase(java.util.Locale.US);
        try {
            Map<String, Object> params = CreatorRuntimeServiceArguments.map(arguments, "params");
            String requestType = CreatorRuntimeServiceArguments.string(arguments, "requestType");
            boolean requestBody = "REQUEST_BODY".equalsIgnoreCase(requestType) || "1".equals(requestType);
            if (!requestBody && "GET".equals(method) && !params.isEmpty()) {
                okhttp3.HttpUrl parsed = okhttp3.HttpUrl.parse(url);
                if (parsed == null) return CreatorRuntimeServiceArguments.invalid("http requires a valid url.");
                okhttp3.HttpUrl.Builder builder = parsed.newBuilder();
                for (Map.Entry<String, Object> parameter : params.entrySet()) {
                    builder.addQueryParameter(parameter.getKey(), String.valueOf(parameter.getValue()));
                }
                url = builder.build().toString();
            }
            Request.Builder request = new Request.Builder().url(url);
            for (Map.Entry<String, Object> header : CreatorRuntimeServiceArguments.map(arguments, "headers").entrySet()) {
                request.header(header.getKey(), String.valueOf(header.getValue()));
            }
            String bodyText = CreatorRuntimeServiceArguments.string(arguments, "body");
            RequestBody body = null;
            if (!("GET".equals(method) || "HEAD".equals(method))) {
                if (bodyText != null) body = RequestBody.create(bodyText, JSON);
                else if (requestBody) body = RequestBody.create(new com.google.gson.Gson().toJson(params), JSON);
                else {
                    FormBody.Builder form = new FormBody.Builder();
                    for (Map.Entry<String, Object> parameter : params.entrySet()) {
                        form.add(parameter.getKey(), String.valueOf(parameter.getValue()));
                    }
                    body = form.build();
                }
            }
            request.method(method, body);
            final String requestUrl = url;
            final String tag = CreatorRuntimeServiceArguments.string(arguments, "tag");
            client.newCall(request.build()).enqueue(new Callback() {
                @Override public void onFailure(Call call, IOException error) {
                    publisher.publish(getId(), "error", CreatorRuntimeServiceArguments.output(
                            "url", requestUrl, "tag", tag,
                            "message", error.getMessage() == null ? "Network request failed." : error.getMessage()));
                }

                @Override public void onResponse(Call call, Response response) throws IOException {
                    try (Response closeable = response) {
                        String responseBody = closeable.body() == null ? "" : closeable.body().string();
                        Map<String, Object> output = new LinkedHashMap<>();
                        output.put("url", requestUrl);
                        output.put("tag", tag);
                        output.put("statusCode", closeable.code());
                        output.put("body", responseBody);
                        output.put("successful", closeable.isSuccessful());
                        Map<String, Object> responseHeaders = new LinkedHashMap<>();
                        for (String name : closeable.headers().names()) responseHeaders.put(name, closeable.header(name));
                        output.put("headers", responseHeaders);
                        publisher.publish(getId(), closeable.isSuccessful() ? "response" : "error", output);
                    }
                }
            });
            return CreatorRuntimeServiceArguments.succeeded("url", url, "method", method, "tag", tag, "started", true);
        } catch (IllegalArgumentException error) {
            return CreatorRuntimeServiceArguments.invalid(error.getMessage());
        }
    }

    private static final class Configuration {
        Map<String, Object> params = new LinkedHashMap<>();
        Map<String, Object> headers = new LinkedHashMap<>();
        String requestType = "REQUEST_PARAM";
    }
}
