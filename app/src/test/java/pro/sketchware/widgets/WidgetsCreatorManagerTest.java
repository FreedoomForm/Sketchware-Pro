package pro.sketchware.widgets;

import static com.google.common.truth.Truth.assertThat;

import java.util.ArrayList;
import java.util.HashMap;

import org.junit.Test;

public class WidgetsCreatorManagerTest {
    @Test
    public void nullCustomWidgetJsonBecomesEmptyListForBuiltInPalette() {
        ArrayList<HashMap<String, Object>> normalized =
                WidgetsCreatorManager.normalizeWidgetConfigurations(null);

        assertThat(normalized).isNotNull();
        assertThat(normalized).isEmpty();
    }

    @Test
    public void validCustomWidgetListIsPreserved() {
        ArrayList<HashMap<String, Object>> loaded = new ArrayList<>();
        HashMap<String, Object> widget = new HashMap<>();
        widget.put("Class", "Widgets");
        loaded.add(widget);

        assertThat(WidgetsCreatorManager.normalizeWidgetConfigurations(loaded))
                .containsExactly(widget);
    }
}
