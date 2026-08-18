package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import com.besome.sketch.beans.LayoutBean;
import com.besome.sketch.beans.ViewBean;

import org.junit.Test;

import java.util.Arrays;

public class CreatorLegacyViewImporterTest {
    @Test public void importsSupportedLegacyWidgetTreeAndReportsUnsupportedItemsClearly() {
        ViewBean title = new ViewBean("title", ViewBean.VIEW_TYPE_WIDGET_TEXTVIEW);
        title.parent = "root";
        title.index = 0;
        title.text.text = "Hello";
        title.text.textSize = 22;
        ViewBean input = new ViewBean("input", ViewBean.VIEW_TYPE_WIDGET_EDITTEXT);
        input.parent = "root";
        input.index = 1;
        input.text.hint = "Your name";
        ViewBean web = new ViewBean("web", ViewBean.VIEW_TYPE_WIDGET_WEBVIEW);
        web.parent = "root";
        web.index = 2;

        CreatorLegacyViewImporter.Result result = new CreatorLegacyViewImporter().importLayout(
                "project", "Imported", "main", "/", Arrays.asList(title, input, web));

        CreatorProjectDocument document = result.getDocument();
        assertThat(document.getScreens().get("main").getRootWidgetId()).isEqualTo("root_main");
        assertThat(document.getWidgets().get("root_main").getChildren()).containsExactly("title", "input").inOrder();
        assertThat(document.getWidgets().get("title").getType()).isEqualTo("text");
        assertThat(document.getWidgets().get("title").getProperties().get("text")).isEqualTo("Hello");
        assertThat(document.getWidgets().get("input").getProperties().get("hint")).isEqualTo("Your name");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(2);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R2_RUNTIME_PLUGIN)).isEqualTo(1);
        assertThat(result.getReport().canPreviewImmediately()).isTrue();
    }

    @Test public void reportsLegacyNativeOnlyWidgetAsFallbackRatherThanSilentlyDroppingIt() {
        ViewBean list = new ViewBean("list", ViewBean.VIEW_TYPE_WIDGET_LISTVIEW);

        CreatorLegacyViewImporter.Result result = new CreatorLegacyViewImporter().importLayout(
                "project", "Imported", "main", "/", Arrays.asList(list));

        assertThat(result.getDocument().getWidgets().containsKey("list")).isFalse();
        assertThat(result.getReport().count(CreatorCompatibilityTier.R3_NATIVE_FALLBACK)).isEqualTo(1);
        assertThat(result.getReport().canPreviewImmediately()).isFalse();
    }
}
