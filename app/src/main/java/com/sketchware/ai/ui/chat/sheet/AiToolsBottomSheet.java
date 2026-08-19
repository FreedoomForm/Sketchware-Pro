package com.sketchware.ai.ui.chat.sheet;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import pro.sketchware.R;

/**
 * Attach-tools bottom sheet — ported from the reference repo's
 * {@code KelivoToolsBottomSheet}. Opens when the user taps the paperclip
 * button in the chat input bar.
 *
 * <p>Shows three tiles (Camera · Photos · Upload) and invokes the supplied
 * {@link Callback} for whichever tile the user picks. The host fragment is
 * responsible for launching the right intent and consuming the result.
 *
 * <p>The "Instruction" and "Context" rows from the reference sheet are
 * omitted here because the underlying features aren't part of this fork's
 * chat surface yet — adding them later is a one-line layout tweak.
 */
public final class AiToolsBottomSheet {

    /** Called when the user picks a tile. */
    public interface Callback {
        void onCamera();
        void onPhotos();
        void onUpload();
    }

    private AiToolsBottomSheet() {
        // No instances — static helper only.
    }

    /** Show the attach-tools bottom sheet. */
    public static void show(Context ctx, Callback callback) {
        BottomSheetDialog dialog = new BottomSheetDialog(ctx);
        View view = LayoutInflater.from(ctx).inflate(R.layout.bottom_sheet_ai_tools, null);
        view.findViewById(R.id.tool_camera).setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onCamera();
        });
        view.findViewById(R.id.tool_photos).setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onPhotos();
        });
        view.findViewById(R.id.tool_upload).setOnClickListener(v -> {
            dialog.dismiss();
            if (callback != null) callback.onUpload();
        });
        dialog.setContentView(view);
        dialog.show();
    }
}
