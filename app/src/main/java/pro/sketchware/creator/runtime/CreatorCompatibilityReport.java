package pro.sketchware.creator.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Human-readable and machine-readable result of one legacy-project import attempt. */
public final class CreatorCompatibilityReport {
    public static final class Item {
        private final String sourceId;
        private final String sourceType;
        private final CreatorCompatibilityTier tier;
        private final String message;

        public Item(String sourceId, String sourceType, CreatorCompatibilityTier tier, String message) {
            this.sourceId = sourceId;
            this.sourceType = sourceType;
            this.tier = tier;
            this.message = message;
        }

        public String getSourceId() { return sourceId; }
        public String getSourceType() { return sourceType; }
        public CreatorCompatibilityTier getTier() { return tier; }
        public String getMessage() { return message; }
    }

    private final List<Item> items = new ArrayList<>();

    public void add(String sourceId, String sourceType, CreatorCompatibilityTier tier, String message) {
        items.add(new Item(sourceId, sourceType, tier, message));
    }

    public List<Item> getItems() { return Collections.unmodifiableList(items); }

    public int count(CreatorCompatibilityTier tier) {
        int count = 0;
        for (Item item : items) if (item.getTier() == tier) count++;
        return count;
    }

    public boolean canPreviewImmediately() {
        return count(CreatorCompatibilityTier.R3_NATIVE_FALLBACK) == 0
                && count(CreatorCompatibilityTier.R0_UNSUPPORTED) == 0;
    }
}
