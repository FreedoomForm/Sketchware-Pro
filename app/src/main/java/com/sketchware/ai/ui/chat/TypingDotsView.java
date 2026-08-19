package com.sketchware.ai.ui.chat;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * Three bouncing dots indicator used by the chat UI while the AI is
 * "typing" / thinking. Ported from {@code KelivoTypingDotsView} in the
 * FabioSilva11/Sketchware-IA reference.
 *
 * <p>The view is self-contained: it allocates a single {@link ValueAnimator}
 * on first {@link #onDraw} and cycles 0..1 forever, drawing three dots
 * whose vertical offset follows a sine wave staggered by 1/3 phase each.
 *
 * <p>Visibility is the canonical on/off switch — when {@link #setVisibility}
 * is set to {@code GONE} the animator is paused to save battery.
 */
public final class TypingDotsView extends View {

    private static final int DOT_COUNT = 3;
    private static final float DOT_RADIUS_DP = 2.5f;
    private static final float DOT_SPACING_DP = 5f;
    private static final float BOUNCE_HEIGHT_DP = 3f;
    private static final long CYCLE_MS = 900L;

    private final Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
    private float phase;

    public TypingDotsView(Context context) {
        this(context, null);
    }

    public TypingDotsView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public TypingDotsView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        dotPaint.setColor(0xFF8E8E93);
        animator.setDuration(CYCLE_MS);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationCancel(Animator a) { phase = 0f; }
        });
    }

    @Override protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (getVisibility() == VISIBLE) startIfStopped();
    }

    @Override protected void onDetachedFromWindow() {
        stop();
        super.onDetachedFromWindow();
    }

    @Override public void setVisibility(int visibility) {
        super.setVisibility(visibility);
        if (visibility == VISIBLE) startIfStopped();
        else stop();
    }

    private void startIfStopped() {
        if (!animator.isStarted()) animator.start();
    }

    private void stop() {
        if (animator.isStarted()) animator.cancel();
    }

    @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        float density = getResources().getDisplayMetrics().density;
        int width = (int) ((DOT_COUNT * DOT_RADIUS_DP * 2) + (DOT_COUNT - 1) * DOT_SPACING_DP + 2) * (int) density;
        int height = (int) ((DOT_RADIUS_DP * 2 + BOUNCE_HEIGHT_DP * 2) * density + 2);
        setMeasuredDimension(width, height);
    }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float density = getResources().getDisplayMetrics().density;
        float r = DOT_RADIUS_DP * density;
        float spacing = DOT_SPACING_DP * density;
        float bounce = BOUNCE_HEIGHT_DP * density;
        float cy = getHeight() / 2f + bounce / 2f;

        for (int i = 0; i < DOT_COUNT; i++) {
            float x = r + i * (r * 2 + spacing) + 1f;
            // Stagger phase per dot, sine bounce.
            float p = (phase + i * (1f / DOT_COUNT)) % 1f;
            float y = cy - (float) Math.sin(p * Math.PI * 2) * bounce;
            // Dim dots that are at the bottom of their arc.
            int alpha = (int) (120 + 135 * (0.5f + 0.5f * Math.sin(p * Math.PI * 2)));
            dotPaint.setAlpha(Math.min(255, alpha));
            canvas.drawCircle(x, y, r, dotPaint);
        }
    }
}
