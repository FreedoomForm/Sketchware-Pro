package pro.sketchware.creator;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CreatorShakeDetectorTest {
    @Test public void lowForceDoesNotTrigger() {
        CreatorShakeDetector detector = new CreatorShakeDetector(10f, 100L);
        assertThat(detector.onSample(1f, 2f, 3f, 0L)).isFalse();
    }

    @Test public void strongForceTriggersOnceAndDebounces() {
        CreatorShakeDetector detector = new CreatorShakeDetector(10f, 100L);
        assertThat(detector.onSample(10f, 0f, 0f, 1000L)).isTrue();
        assertThat(detector.onSample(10f, 0f, 0f, 1050L)).isFalse();
        assertThat(detector.onSample(10f, 0f, 0f, 1100L)).isTrue();
    }

    @Test public void invalidSamplesDoNotTrigger() {
        CreatorShakeDetector detector = new CreatorShakeDetector();
        assertThat(detector.onSample(Float.NaN, 0f, 0f, 1000L)).isFalse();
        assertThat(detector.onSample(Float.POSITIVE_INFINITY, 0f, 0f, 2000L)).isFalse();
    }

    @Test public void resetAllowsImmediateTrigger() {
        CreatorShakeDetector detector = new CreatorShakeDetector(10f, 1000L);
        assertThat(detector.onSample(10f, 0f, 0f, 1000L)).isTrue();
        detector.reset();
        assertThat(detector.onSample(10f, 0f, 0f, 1001L)).isTrue();
    }
}
