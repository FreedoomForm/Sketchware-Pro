package pro.sketchware.creator.runtime;

import com.sketchware.ai.util.SketchwareApi;
import java.io.File;
import java.io.IOException;

/** Resolves project-owned image files for the live Creator Runtime only. */
public final class CreatorRuntimeResourceResolver {
    interface ProjectImageDirectory { File get(String projectId); }

    private CreatorRuntimeResourceResolver() { }

    public static File resolveProjectImage(String projectId, String resourceFileName) {
        return resolve(projectId, resourceFileName, new ProjectImageDirectory() {
            @Override public File get(String id) {
                try {
                    Object store = SketchwareApi.invokeStatic("a.a.a.jC", "d", id);
                    Object path = SketchwareApi.invoke(store, "l");
                    return path instanceof String ? new File((String) path) : null;
                } catch (Throwable ignored) {
                    return null;
                }
            }
        });
    }

    static File resolve(String projectId, String resourceFileName, ProjectImageDirectory directories) {
        if (projectId == null || projectId.trim().isEmpty() || resourceFileName == null
                || resourceFileName.trim().isEmpty() || directories == null) return null;
        File root = directories.get(projectId);
        if (root == null) return null;
        try {
            File canonicalRoot = root.getCanonicalFile();
            File target = new File(canonicalRoot, resourceFileName).getCanonicalFile();
            String rootPath = canonicalRoot.getPath();
            String targetPath = target.getPath();
            if (!targetPath.equals(rootPath) && !targetPath.startsWith(rootPath + File.separator)) return null;
            return target.isFile() ? target : null;
        } catch (IOException ignored) {
            return null;
        }
    }
}
