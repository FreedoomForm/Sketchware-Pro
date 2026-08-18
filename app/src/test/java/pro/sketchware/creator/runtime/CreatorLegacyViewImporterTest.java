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
        assertThat(document.getWidgets().get("root_main").getChildren()).containsExactly("title", "input", "web").inOrder();
        assertThat(document.getWidgets().get("title").getType()).isEqualTo("text");
        assertThat(document.getWidgets().get("title").getProperties().get("text")).isEqualTo("Hello");
        assertThat(document.getWidgets().get("input").getProperties().get("hint")).isEqualTo("Your name");
        assertThat(document.getWidgets().get("web").getType()).isEqualTo("web");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(3);
        assertThat(result.getReport().canPreviewImmediately()).isTrue();
    }

    @Test public void importsLegacyListWidgetIntoDirectRuntimeNativeRenderer() {
        ViewBean list = new ViewBean("list", ViewBean.VIEW_TYPE_WIDGET_LISTVIEW);

        CreatorLegacyViewImporter.Result result = new CreatorLegacyViewImporter().importLayout(
                "project", "Imported", "main", "/", Arrays.asList(list));

        assertThat(result.getDocument().getWidgets().containsKey("list")).isTrue();
        assertThat(result.getDocument().getWidgets().get("list").getType()).isEqualTo("list");
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(1);
        assertThat(result.getReport().canPreviewImmediately()).isTrue();
    }

    @Test public void importsEveryInventoriedLegacyViewTypeAsTypedRuntimeWidget() {
        java.util.List<ViewBean> views = new java.util.ArrayList<>();
        for (int type = 0; type < 49; type++) {
            ViewBean view = new ViewBean("legacy_" + type, type);
            view.parent = "root";
            view.index = type;
            views.add(view);
        }

        CreatorLegacyViewImporter.Result result = new CreatorLegacyViewImporter().importLayout(
                "project", "Imported", "main", "/", views);

        assertThat(result.getDocument().getWidgets()).hasSize(50);
        assertThat(result.getDocument().getWidgets().get("root_main").getChildren()).hasSize(49);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R1_RUNTIME_NATIVE)).isEqualTo(49);
        assertThat(result.getReport().count(CreatorCompatibilityTier.R0_UNSUPPORTED)).isEqualTo(0);
    }
}
