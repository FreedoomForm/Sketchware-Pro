package pro.sketchware.creator.runtime;

/** Machine-readable outcome of operation validation. */
public final class CreatorValidationResult {
    public enum Code {
        OK, PROJECT_MISMATCH, STALE_REVISION, INVALID_PAYLOAD, DUPLICATE_ID,
        MISSING_REFERENCE, INVALID_ROUTE, SAFETY_VIOLATION, UNKNOWN_OPERATION
    }

    private final Code code;
    private final String message;

    private CreatorValidationResult(Code code, String message) {
        this.code = code;
        this.message = message;
    }

    public static CreatorValidationResult ok() { return new CreatorValidationResult(Code.OK, "ok"); }
    public static CreatorValidationResult error(Code code, String message) {
        return new CreatorValidationResult(code, message);
    }

    public boolean isOk() { return code == Code.OK; }
    public Code getCode() { return code; }
    public String getMessage() { return message; }
}
