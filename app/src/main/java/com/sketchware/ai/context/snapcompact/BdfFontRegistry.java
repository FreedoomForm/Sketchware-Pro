package com.sketchware.ai.context.snapcompact;

import android.content.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Lazy-loading cache for the three bundled BDF fonts. Each font is loaded
 * once per process from {@code assets/snapcompact/} and cached for the
 * lifetime of the process. Fonts are pure-Java parsed; no native code.
 *
 * <p>Mirrors the bundled-font registry in oh-my-pi's
 * {@code crates/pi-natives/src/snapcompact.rs} (which embeds the same
 * BDF files at compile time and parses them with a Rust BDF reader).
 */
public final class BdfFontRegistry {

    public enum FontName {
        FONT_5x8("5x8.bdf"),
        FONT_6x12("6x12.bdf"),
        FONT_8x13("8x13.bdf");

        public final String assetName;
        FontName(String assetName) { this.assetName = assetName; }
    }

    private static final Object LOCK = new Object();
    private static final Map<FontName, BdfFont> CACHE = new HashMap<>();
    private static Context appContext;

    private BdfFontRegistry() {}

    /** Bind the registry to an application context (call once at startup). */
    public static void init(Context context) {
        synchronized (LOCK) {
            appContext = context.getApplicationContext();
        }
    }

    /** Get a font, loading it from assets on first access. */
    public static BdfFont get(FontName name) throws IOException {
        synchronized (LOCK) {
            BdfFont cached = CACHE.get(name);
            if (cached != null) return cached;
            if (appContext == null) {
                throw new IllegalStateException(
                    "BdfFontRegistry not initialized — call init(context) first");
            }
            try (InputStream in = appContext.getAssets().open("snapcompact/" + name.assetName)) {
                BdfFont font = BdfFont.load(name.name(), in);
                CACHE.put(name, font);
                return font;
            }
        }
    }
}
