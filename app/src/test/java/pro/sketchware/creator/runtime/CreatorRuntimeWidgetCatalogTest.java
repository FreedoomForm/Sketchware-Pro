package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CreatorRuntimeWidgetCatalogTest {
    @Test public void fullOriginalWidgetVocabularyIsRuntimeNative() {
        String[] expected = {
                "column", "row", "stack", "scroll", "hscroll", "pager", "text", "button", "input", "image",
                "checkbox", "switch", "progress", "spinner", "slider", "calendar_view", "fab", "radio", "rating",
                "search", "autocomplete", "list", "grid", "clock", "date_picker", "time_picker", "web", "video",
                "lottie", "ad_banner", "tabs", "bottom_navigation", "badge", "pattern", "sidebar", "card",
                "collapsing", "text_input", "swipe_refresh", "radio_group", "circle_image", "otp", "code", "sign_in"
        };
        assertThat(CreatorRuntimeWidgetCatalog.runtimeNativeTypes()).hasSize(expected.length);
        for (String type : expected) {
            assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative(type)).isTrue();
        }
    }

    @Test public void coreInteractiveWidgetTypesAreClassifiedAsRuntimeNative() {
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("column")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("input")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("image")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("checkbox")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("switch")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("webview")).isFalse();
    }
}
