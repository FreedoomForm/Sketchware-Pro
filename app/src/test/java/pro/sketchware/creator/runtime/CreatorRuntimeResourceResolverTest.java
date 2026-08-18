package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import org.junit.Test;

public class CreatorRuntimeResourceResolverTest {
    @Test public void resolvesExistingProjectImageWithinConfiguredDirectory() throws Exception {
        File root = temporaryDirectory("creator-resource-root");
        File image = new File(root, "hero.png");
        try (FileOutputStream stream = new FileOutputStream(image)) { stream.write(new byte[]{1}); }

        File resolved = CreatorRuntimeResourceResolver.resolve("project", "hero.png", id -> root);

        assertThat(resolved).isEqualTo(image.getCanonicalFile());
    }

    @Test public void rejectsTraversalAndAbsentProjectImages() throws Exception {
        File root = temporaryDirectory("creator-resource-safe");
        File outside = File.createTempFile("creator-resource-outside", ".png");
        try (FileOutputStream stream = new FileOutputStream(outside)) { stream.write(new byte[]{1}); }

        assertThat(CreatorRuntimeResourceResolver.resolve("project", "../" + outside.getName(), id -> root)).isNull();
        assertThat(CreatorRuntimeResourceResolver.resolve("project", "absent.png", id -> root)).isNull();
    }

    private static File temporaryDirectory(String prefix) throws Exception {
        File file = File.createTempFile(prefix, "");
        if (!file.delete() || !file.mkdirs()) throw new IllegalStateException("Unable to create temporary directory");
        file.deleteOnExit();
        return file;
    }
}
