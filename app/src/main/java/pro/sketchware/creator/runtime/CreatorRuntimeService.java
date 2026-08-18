package pro.sketchware.creator.runtime;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A reviewed capability implemented by the Creator Runtime itself.
 *
 * <p>Project documents can invoke only registered services. They never load Java
 * classes, plugins, or generated application code supplied by a project.
 */
public interface CreatorRuntimeService {
    enum Status { SUCCEEDED, PERMISSION_REQUIRED, DENIED, UNSUPPORTED_ARGUMENT, FAILED }

    final class Result {
        private final Status status;
        private final Map<String, Object> output;
        private final String detail;

        public Result(Status status, Map<String, Object> output, String detail) {
            this.status = status == null ? Status.FAILED : status;
            this.output = Collections.unmodifiableMap(new LinkedHashMap<>(output == null
                    ? Collections.<String, Object>emptyMap() : output));
            this.detail = detail;
        }

        public Status getStatus() { return status; }
        public Map<String, Object> getOutput() { return output; }
        public String getDetail() { return detail; }
    }

    String getId();

    Result execute(Map<String, Object> arguments);
}
