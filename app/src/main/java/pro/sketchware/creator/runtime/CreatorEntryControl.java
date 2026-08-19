package pro.sketchware.creator.runtime;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * User-editable presentation of the visible route into the creator editor.
 *
 * <p>This object deliberately has no recovery toggle. The emergency recovery
 * path belongs to the host and cannot be disabled by a project operation.
 */
public final class CreatorEntryControl {
    private static final Set<String> PLACEMENTS = Collections.unmodifiableSet(new LinkedHashSet<>(Arrays.asList(
            "bottom_end", "bottom_start", "top_end", "top_start", "center")));

    private final boolean visible;
    private final String label;
    private final String placement;

    public CreatorEntryControl(boolean visible, String label, String placement) {
        if (label == null || label.trim().isEmpty()) throw new IllegalArgumentException("label");
        if (!PLACEMENTS.contains(placement)) throw new IllegalArgumentException("placement");
        this.visible = visible;
        this.label = label;
        this.placement = placement;
    }

    public static CreatorEntryControl defaultControl() {
        return new CreatorEntryControl(true, "Continue", "bottom_end");
    }

    public boolean isVisible() { return visible; }
    public String getLabel() { return label; }
    public String getPlacement() { return placement; }

    public CreatorEntryControl withValues(Boolean nextVisible, String nextLabel, String nextPlacement) {
        return new CreatorEntryControl(nextVisible == null ? visible : nextVisible,
                nextLabel == null ? label : nextLabel,
                nextPlacement == null ? placement : nextPlacement);
    }

    public static boolean isSupportedPlacement(String placement) {
        return PLACEMENTS.contains(placement);
    }
}
