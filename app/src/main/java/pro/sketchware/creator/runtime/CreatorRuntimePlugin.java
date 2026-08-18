package pro.sketchware.creator.runtime;

import java.util.Map;

/**
 * Reviewed, host-owned implementation boundary for a legacy Sketchware
 * capability. Runtime projects invoke these plugins instead of compiling an APK.
 */
public interface CreatorRuntimePlugin {
    enum Status { SUCCEEDED, PERMISSION_REQUIRED, DENIED, UNSUPPORTED_ARGUMENT, FAILED }

    final class Result {
        private final Status status;
        private final Map<String, Object> output;
        private final String detail;

        public Result(Status status, Map<String, Object> output, String detail) {
            this.status = status;
            this.output = output;
            this.detail = detail;
        }
        public Status getStatus() { return status; }
        public Map<String, Object> getOutput() { return output; }
        public String getDetail() { return detail; }
    }

    String getId();
    Result execute(Map<String, Object> arguments);
}
