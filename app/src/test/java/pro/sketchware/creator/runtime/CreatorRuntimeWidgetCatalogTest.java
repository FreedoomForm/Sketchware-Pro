package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class CreatorRuntimeWidgetCatalogTest {
    @Test public void coreInteractiveWidgetTypesAreClassifiedAsRuntimeNative() {
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("column")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("input")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("image")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("checkbox")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("switch")).isTrue();
        assertThat(CreatorRuntimeWidgetCatalog.isRuntimeNative("webview")).isFalse();
    }
}
