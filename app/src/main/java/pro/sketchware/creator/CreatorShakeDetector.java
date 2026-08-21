package pro.sketchware.creator;

/** Pure, deterministic shake detector used by the host-owned recovery route. */
public final class CreatorShakeDetector {
    public static final float DEFAULT_THRESHOLD = 15f;
    public static final long DEFAULT_DEBOUNCE_MS = 900L;

    private final float threshold;
    private final long debounceMs;
    private long lastShakeAt = Long.MIN_VALUE;

    public CreatorShakeDetector() {
        this(DEFAULT_THRESHOLD, DEFAULT_DEBOUNCE_MS);
    }

    public CreatorShakeDetector(float threshold, long debounceMs) {
        if (!(threshold > 0f) || debounceMs < 0L) throw new IllegalArgumentException("invalid detector configuration");
        this.threshold = threshold;
        this.debounceMs = debounceMs;
    }

    public boolean onSample(float x, float y, float z, long nowMs) {
        if (!Float.isFinite(x) || !Float.isFinite(y) || !Float.isFinite(z)) return false;
        float force = (float) Math.sqrt(x * x + y * y + z * z);
        if (force < threshold) return false;
        if (lastShakeAt != Long.MIN_VALUE && nowMs - lastShakeAt < debounceMs) return false;
        lastShakeAt = nowMs;
        return true;
    }

    public void reset() {
        lastShakeAt = Long.MIN_VALUE;
    }
}
