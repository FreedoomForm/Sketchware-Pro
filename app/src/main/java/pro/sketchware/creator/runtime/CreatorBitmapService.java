package pro.sketchware.creator.runtime;

import android.Manifest;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.ExifInterface;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;

/** Typed runtime-native implementation of the reviewed legacy FileUtil bitmap family. */
public final class CreatorBitmapService implements CreatorRuntimeService {
    private final CreatorRuntimeEnvironment environment;
    private final CreatorFileService.FileOperations files;

    public CreatorBitmapService(CreatorRuntimeEnvironment environment) {
        this(environment, CreatorFileService.FileOperations.forContext(environment.getContext()));
    }

    CreatorBitmapService(CreatorRuntimeEnvironment environment, CreatorFileService.FileOperations files) {
        if (environment == null || files == null) throw new IllegalArgumentException("environment/files");
        this.environment = environment;
        this.files = files;
    }

    @Override public String getId() { return "bitmap"; }

    @Override public Result execute(Map<String, Object> arguments) {
        String action = CreatorRuntimeServiceArguments.string(arguments, "action");
        String path = CreatorRuntimeServiceArguments.string(arguments, "path");
        if (action == null || path == null || path.trim().isEmpty()) return CreatorRuntimeServiceArguments.invalid("bitmap requires action and path.");
        String destination = CreatorRuntimeServiceArguments.string(arguments, "destination");
        boolean readOnly = "jpeg_rotate".equals(action);
        if (!readOnly && (destination == null || destination.trim().isEmpty())) {
            return CreatorRuntimeServiceArguments.invalid(action + " requires destination.");
        }
        if (files.requiresExternalPermission(path) && !environment.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
            environment.requestPermission(getId(), Manifest.permission.READ_EXTERNAL_STORAGE);
            return permission();
        }
        if (!readOnly && files.requiresExternalPermission(destination)
                && !environment.hasPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            environment.requestPermission(getId(), Manifest.permission.WRITE_EXTERNAL_STORAGE);
            return permission();
        }
        try {
            File source = files.resolveForRuntime(path);
            if (!source.isFile()) return CreatorRuntimeServiceArguments.failed("Bitmap source does not exist.");
            if (readOnly) return CreatorRuntimeServiceArguments.succeeded("value", jpegRotate(source));
            File target = files.resolveForRuntime(destination);
            Bitmap src = BitmapFactory.decodeFile(source.getPath());
            if (src == null) return CreatorRuntimeServiceArguments.failed("Bitmap source cannot be decoded.");
            Bitmap transformed = transform(action, src, arguments);
            if (transformed == null) return CreatorRuntimeServiceArguments.invalid("Unsupported bitmap action: " + action);
            save(transformed, target);
            return CreatorRuntimeServiceArguments.succeeded("completed", true, "path", target.getPath());
        } catch (IOException | IllegalArgumentException | SecurityException error) {
            return CreatorRuntimeServiceArguments.failed(error.getMessage() == null ? "Bitmap operation failed." : error.getMessage());
        }
    }

    private Result permission() {
        return new Result(Status.PERMISSION_REQUIRED, Collections.<String, Object>emptyMap(),
                "Storage permission was requested for the bitmap operation.");
    }

    private static Bitmap transform(String action, Bitmap src, Map<String, Object> arguments) {
        if ("resize_retain_ratio".equals(action)) return retainRatio(src, positiveInt(arguments.get("max")));
        if ("resize_square".equals(action)) {
            int max = positiveInt(arguments.get("max"));
            return Bitmap.createScaledBitmap(src, max, max, true);
        }
        if ("resize_circle".equals(action)) return circle(src);
        if ("rounded_border".equals(action)) return rounded(src, number(arguments.get("pixels")));
        if ("crop_center".equals(action)) return cropCenter(src, positiveInt(arguments.get("width")), positiveInt(arguments.get("height")));
        if ("rotate".equals(action) || "scale".equals(action) || "skew".equals(action)) {
            Matrix matrix = new Matrix();
            if ("rotate".equals(action)) matrix.postRotate(number(arguments.get("angle")));
            else if ("scale".equals(action)) matrix.postScale(number(arguments.get("x")), number(arguments.get("y")));
            else matrix.postSkew(number(arguments.get("x")), number(arguments.get("y")));
            return Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true);
        }
        if ("color_filter".equals(action)) {
            Bitmap target = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
            Paint paint = new Paint();
            paint.setColorFilter(new LightingColorFilter((int) number(arguments.get("color")), 1));
            new Canvas(target).drawBitmap(src, 0, 0, paint);
            return target;
        }
        if ("brightness".equals(action) || "contrast".equals(action)) {
            float value = number(arguments.get("value"));
            float[] matrix = "brightness".equals(action)
                    ? new float[]{1,0,0,0,value, 0,1,0,0,value, 0,0,1,0,value, 0,0,0,1,0}
                    : new float[]{value,0,0,0,0, 0,value,0,0,0, 0,0,value,0,0, 0,0,0,1,0};
            Bitmap target = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
            Paint paint = new Paint();
            paint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(matrix)));
            new Canvas(target).drawBitmap(src, 0, 0, paint);
            return target;
        }
        return null;
    }

    private static Bitmap retainRatio(Bitmap src, int max) {
        float scale = Math.min((float) max / src.getWidth(), (float) max / src.getHeight());
        if (scale >= 1f) return src;
        return Bitmap.createScaledBitmap(src, Math.max(1, Math.round(src.getWidth() * scale)),
                Math.max(1, Math.round(src.getHeight() * scale)), true);
    }

    private static Bitmap circle(Bitmap src) {
        Bitmap target = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Rect rect = new Rect(0, 0, src.getWidth(), src.getHeight());
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawCircle(src.getWidth() / 2f, src.getHeight() / 2f, src.getWidth() / 2f, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, rect, rect, paint);
        return target;
    }

    private static Bitmap rounded(Bitmap src, float pixels) {
        Bitmap target = Bitmap.createBitmap(src.getWidth(), src.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Rect rect = new Rect(0, 0, src.getWidth(), src.getHeight());
        canvas.drawARGB(0, 0, 0, 0);
        canvas.drawRoundRect(new RectF(rect), pixels, pixels, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(src, rect, rect, paint);
        return target;
    }

    private static Bitmap cropCenter(Bitmap src, int width, int height) {
        if (src.getWidth() < width && src.getHeight() < height) return src;
        int cropWidth = Math.min(width, src.getWidth());
        int cropHeight = Math.min(height, src.getHeight());
        int x = Math.max(0, (src.getWidth() - width) / 2);
        int y = Math.max(0, (src.getHeight() - height) / 2);
        return Bitmap.createBitmap(src, x, y, cropWidth, cropHeight);
    }

    private static void save(Bitmap bitmap, File target) throws IOException {
        File parent = target.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) throw new IOException("Could not create bitmap destination directory.");
        try (FileOutputStream output = new FileOutputStream(target, false)) {
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) throw new IOException("Could not encode bitmap.");
        }
    }

    private static int jpegRotate(File file) {
        try {
            int orientation = new ExifInterface(file.getPath()).getAttributeInt(ExifInterface.TAG_ORIENTATION, -1);
            if (orientation == ExifInterface.ORIENTATION_ROTATE_90) return 90;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_180) return 180;
            if (orientation == ExifInterface.ORIENTATION_ROTATE_270) return 270;
        } catch (IOException ignored) { }
        return 0;
    }

    private static int positiveInt(Object value) { return Math.max(1, Math.round(number(value))); }
    private static float number(Object value) {
        if (value instanceof Number) return ((Number) value).floatValue();
        try { return value == null ? 0f : Float.parseFloat(String.valueOf(value)); }
        catch (NumberFormatException ignored) { return 0f; }
    }
}
