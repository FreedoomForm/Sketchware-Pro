package pro.sketchware.creator.runtime;

import android.Manifest;
import android.content.Context;
import android.os.Environment;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Runtime-native implementation of the non-visual legacy FileUtil operations.
 *
 * <p>Only the owning app's private directories and Android shared external-storage root are reachable.
 * A legacy project never gains a path outside those roots, and external access remains subject to runtime
 * storage permission instead of a generated APK manifest.</p>
 */
public final class CreatorFileService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final FileOperations files;

    public CreatorFileService(CreatorRuntimeEnvironment environment) {
        this(environment, FileOperations.forContext(environment.getContext()));
    }

    CreatorFileService(CreatorRuntimeEnvironment environment, FileOperations files) {
        if (environment == null || files == null) throw new IllegalArgumentException("environment/files");
        this.environment = environment;
        this.files = files;
    }

    @Override public String getId() { return "file"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        if (action == null) return CreatorRuntimeServiceArguments.invalid("file requires an action.");
        String path = CreatorRuntimeServiceArguments.string(arguments, "path");
        if (path == null || path.trim().isEmpty()) return CreatorRuntimeServiceArguments.invalid("file requires a path.");
        boolean mutates = "write".equals(action) || "copy".equals(action) || "copy_dir".equals(action)
                || "move".equals(action) || "delete".equals(action) || "make_dir".equals(action);
        if (files.requiresExternalPermission(path)
                && !environment.hasPermission(mutates ? Manifest.permission.WRITE_EXTERNAL_STORAGE : Manifest.permission.READ_EXTERNAL_STORAGE)) {
            environment.requestPermission(getId(), mutates ? Manifest.permission.WRITE_EXTERNAL_STORAGE : Manifest.permission.READ_EXTERNAL_STORAGE);
            return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                    "Storage permission was requested for the selected legacy path.");
        }
        try {
            if ("read".equals(action)) return CreatorRuntimeServiceArguments.succeeded("content", files.read(path));
            if ("write".equals(action)) {
                String content = CreatorRuntimeServiceArguments.string(arguments, "content");
                files.write(path, content == null ? "" : content);
                return CreatorRuntimeServiceArguments.succeeded("written", true, "path", path);
            }
            if ("copy".equals(action) || "copy_dir".equals(action) || "move".equals(action)) {
                String destination = CreatorRuntimeServiceArguments.string(arguments, "destination");
                if (destination == null || destination.trim().isEmpty()) {
                    return CreatorRuntimeServiceArguments.invalid(action + " requires destination.");
                }
                if (files.requiresExternalPermission(destination)
                        && !environment.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    environment.requestPermission(getId(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
                    return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                            "Storage permission was requested for the destination legacy path.");
                }
                if ("copy".equals(action)) files.copy(path, destination);
                else if ("copy_dir".equals(action)) files.copyDirectory(path, destination);
                else files.move(path, destination);
                return CreatorRuntimeServiceArguments.succeeded("completed", true, "action", action);
            }
            if ("delete".equals(action)) {
                files.delete(path);
                return CreatorRuntimeServiceArguments.succeeded("deleted", true, "path", path);
            }
            if ("make_dir".equals(action)) {
                files.makeDirectory(path);
                return CreatorRuntimeServiceArguments.succeeded("created", true, "path", path);
            }
            if ("exists".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", files.exists(path));
            if ("is_dir".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", files.isDirectory(path));
            if ("is_file".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", files.isFile(path));
            if ("length".equals(action)) return CreatorRuntimeServiceArguments.succeeded("value", files.length(path));
            if ("list_dir".equals(action)) return CreatorRuntimeServiceArguments.succeeded("entries", files.list(path));
            return CreatorRuntimeServiceArguments.invalid("Unsupported file action: " + action);
        } catch (IOException | SecurityException error) {
            return CreatorRuntimeServiceArguments.failed(error.getMessage() == null ? "File operation failed." : error.getMessage());
        }
    }

    static final class FileOperations {
        private final List<File> permittedRoots;
        private final File externalRoot;

        static FileOperations forContext(Context context) {
            File external = Environment.getExternalStorageDirectory();
            List<File> roots = new ArrayList<>();
            if (external != null) roots.add(external);
            roots.add(context.getFilesDir());
            roots.add(context.getCacheDir());
            File appExternal = context.getExternalFilesDir(null);
            if (appExternal != null) roots.add(appExternal);
            return new FileOperations(external, roots);
        }

        FileOperations(File externalRoot, List<File> permittedRoots) {
            if (permittedRoots == null || permittedRoots.isEmpty()) throw new IllegalArgumentException("permittedRoots");
            this.externalRoot = externalRoot;
            this.permittedRoots = new ArrayList<>(permittedRoots);
        }

        boolean requiresExternalPermission(String rawPath) {
            if (externalRoot == null) return false;
            try { return within(resolveCanonical(externalRoot), resolveCanonical(rawPath)); }
            catch (IOException | SecurityException ignored) { return false; }
        }

        String read(String path) throws IOException { return new String(readBytes(resolve(path)), StandardCharsets.UTF_8); }

        void write(String path, String content) throws IOException {
            File target = resolve(path);
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create directory: " + parent);
            try (FileOutputStream output = new FileOutputStream(target, false)) {
                output.write(content.getBytes(StandardCharsets.UTF_8));
            }
        }

        void copy(String source, String destination) throws IOException {
            File from = resolve(source);
            File to = resolve(destination);
            if (!from.isFile()) throw new IOException("Source is not a file: " + source);
            File parent = to.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create directory: " + parent);
            try (FileInputStream input = new FileInputStream(from); FileOutputStream output = new FileOutputStream(to, false)) {
                byte[] buffer = new byte[8192];
                for (int read; (read = input.read(buffer)) >= 0; ) output.write(buffer, 0, read);
            }
        }

        void copyDirectory(String source, String destination) throws IOException {
            File from = resolve(source);
            File to = resolve(destination);
            if (!from.isDirectory()) throw new IOException("Source is not a directory: " + source);
            copyDirectory(from, to);
        }

        void move(String source, String destination) throws IOException {
            File from = resolve(source);
            File to = resolve(destination);
            if (!from.exists()) throw new IOException("Source does not exist: " + source);
            File parent = to.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create directory: " + parent);
            if (from.renameTo(to)) return;
            if (from.isDirectory()) copyDirectory(source, destination); else copy(source, destination);
            delete(source);
        }

        void delete(String path) throws IOException { deleteRecursively(resolve(path)); }

        void makeDirectory(String path) throws IOException {
            File target = resolve(path);
            if (!target.exists() && !target.mkdirs()) throw new IOException("Could not create directory: " + path);
        }

        boolean exists(String path) throws IOException { return resolve(path).exists(); }
        boolean isDirectory(String path) throws IOException { return resolve(path).isDirectory(); }
        boolean isFile(String path) throws IOException { return resolve(path).isFile(); }
        long length(String path) throws IOException { return resolve(path).length(); }

        List<String> list(String path) throws IOException {
            File target = resolve(path);
            if (!target.isDirectory()) throw new IOException("Path is not a directory: " + path);
            String[] names = target.list();
            if (names == null) return Collections.emptyList();
            Arrays.sort(names);
            return Collections.unmodifiableList(Arrays.asList(names));
        }

        private void copyDirectory(File source, File destination) throws IOException {
            if (!destination.exists() && !destination.mkdirs()) throw new IOException("Could not create directory: " + destination);
            File[] children = source.listFiles();
            if (children == null) return;
            for (File child : children) {
                File next = new File(destination, child.getName());
                if (child.isDirectory()) copyDirectory(child, next);
                else copy(child.getPath(), next.getPath());
            }
        }

        private void deleteRecursively(File target) throws IOException {
            if (!target.exists()) return;
            if (target.isDirectory()) {
                File[] children = target.listFiles();
                if (children != null) for (File child : children) deleteRecursively(child);
            }
            if (!target.delete()) throw new IOException("Could not delete path: " + target);
        }

        private byte[] readBytes(File target) throws IOException {
            if (!target.isFile()) throw new IOException("Path is not a file: " + target);
            try (FileInputStream input = new FileInputStream(target); java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                for (int read; (read = input.read(buffer)) >= 0; ) output.write(buffer, 0, read);
                return output.toByteArray();
            }
        }

        private File resolve(String rawPath) throws IOException {
            File target = resolveCanonical(rawPath);
            for (File root : permittedRoots) if (within(resolveCanonical(root), target)) return target;
            throw new SecurityException("Legacy file path is outside Creator Runtime storage roots: " + rawPath);
        }

        private static File resolveCanonical(String rawPath) throws IOException { return new File(rawPath).getCanonicalFile(); }
        private static File resolveCanonical(File rawPath) throws IOException { return rawPath.getCanonicalFile(); }
        private static boolean within(File root, File target) {
            String rootPath = root.getPath();
            String targetPath = target.getPath();
            return targetPath.equals(rootPath) || targetPath.startsWith(rootPath + File.separator);
        }
    }
}
