package pro.sketchware.creator.runtime;

import static com.google.common.truth.Truth.assertThat;

import java.io.File;
import java.util.Arrays;
import org.junit.Test;

public class CreatorFileServiceTest {
    @Test public void performsLegacyFileUtilityOperationsWithinApprovedRoot() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "creator-file-service-" + System.nanoTime());
        assertThat(root.mkdirs()).isTrue();
        try {
            CreatorFileService.FileOperations files = new CreatorFileService.FileOperations(null, Arrays.asList(root));
            String original = new File(root, "draft/original.txt").getPath();
            String copied = new File(root, "draft/copied.txt").getPath();
            String moved = new File(root, "archive/moved.txt").getPath();
            files.write(original, "Creator Runtime");
            assertThat(files.read(original)).isEqualTo("Creator Runtime");
            files.copy(original, copied);
            files.move(copied, moved);
            assertThat(files.exists(moved)).isTrue();
            assertThat(files.isFile(moved)).isTrue();
            assertThat(files.list(new File(root, "archive").getPath())).containsExactly("moved.txt");
            files.delete(new File(root, "draft").getPath());
            assertThat(files.exists(new File(root, "draft").getPath())).isFalse();
        } finally {
            delete(root);
        }
    }

    @Test public void blocksPathsOutsideApprovedRuntimeRoots() throws Exception {
        File root = new File(System.getProperty("java.io.tmpdir"), "creator-file-service-" + System.nanoTime());
        assertThat(root.mkdirs()).isTrue();
        try {
            CreatorFileService.FileOperations files = new CreatorFileService.FileOperations(null, Arrays.asList(root));
            boolean blocked = false;
            try { files.write(new File(root.getParentFile(), "outside.txt").getPath(), "blocked"); }
            catch (SecurityException expected) { blocked = true; }
            assertThat(blocked).isTrue();
        } finally {
            delete(root);
        }
    }

    private static void delete(File file) {
        if (file == null || !file.exists()) return;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) delete(child);
        file.delete();
    }
}
