package com.gathin.tvhome;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.SystemClock;
import android.view.View;

import java.util.Random;

/**
 * A minimal, transparent full-screen overlay of gently falling snowflakes for the winter / Christmas skin.
 * Allocates nothing per frame and uses low particle count (30 flakes) to stay completely fluid on weak TV GPUs.
 * Pauses cleanly when Home is backgrounded or when reduced motion is enabled.
 */
public final class SnowFallView extends View {

    private static final int FLAKE_COUNT = 30;
    private static final double TAU = 2 * Math.PI;

    private final float density;
    private final Random rnd = new Random();
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final float[] x = new float[FLAKE_COUNT];
    private final float[] y = new float[FLAKE_COUNT];
    private final float[] radius = new float[FLAKE_COUNT];
    private final float[] speedY = new float[FLAKE_COUNT];
    private final float[] driftAmp = new float[FLAKE_COUNT];
    private final float[] driftFreq = new float[FLAKE_COUNT];
    private final float[] driftPhase = new float[FLAKE_COUNT];
    private final int[] alpha = new int[FLAKE_COUNT];

    private boolean running;
    private boolean reducedMotion;
    private long lastTime;

    public SnowFallView(Context context) {
        super(context);
        density = context.getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(Color.WHITE);
        setFocusable(false);
        setClickable(false);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
        setWillNotDraw(false);
    }

    @Override
    public boolean hasOverlappingRendering() {
        return false;
    }

    public void setReducedMotion(boolean reduced) {
        this.reducedMotion = reduced;
        if (reduced && running) {
            stop();
            invalidate();
        }
    }

    public void start() {
        if (running || reducedMotion) return;
        running = true;
        lastTime = SystemClock.uptimeMillis();
        postInvalidateOnAnimation();
    }

    public void stop() {
        running = false;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (w > 0 && h > 0) initFlakes(w, h);
    }

    private void initFlakes(int w, int h) {
        for (int i = 0; i < FLAKE_COUNT; i++) {
            x[i] = rnd.nextFloat() * w;
            y[i] = rnd.nextFloat() * h;
            radius[i] = (1.5f + rnd.nextFloat() * 2.5f) * density;
            speedY[i] = (18f + rnd.nextFloat() * 28f) * density; // dp per second
            driftAmp[i] = (10f + rnd.nextFloat() * 22f) * density;
            driftFreq[i] = 0.35f + rnd.nextFloat() * 0.45f;
            driftPhase[i] = rnd.nextFloat() * (float) TAU;
            alpha[i] = 90 + rnd.nextInt(140); // soft transparency
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int w = getWidth();
        int h = getHeight();
        if (w <= 0 || h <= 0 || reducedMotion) return;

        long now = SystemClock.uptimeMillis();
        float dt = lastTime > 0 ? (now - lastTime) / 1000f : 0.016f;
        if (dt > 0.1f) dt = 0.016f; // avoid huge jumps if paused
        lastTime = now;

        for (int i = 0; i < FLAKE_COUNT; i++) {
            if (running) {
                y[i] += speedY[i] * dt;
                if (y[i] > h + radius[i]) {
                    y[i] = -radius[i];
                    x[i] = rnd.nextFloat() * w;
                }
            }
            float curX = x[i] + (float) Math.sin(now * 0.001f * driftFreq[i] * TAU + driftPhase[i]) * driftAmp[i];
            paint.setAlpha(alpha[i]);
            canvas.drawCircle(curX, y[i], radius[i], paint);
        }

        if (running) {
            postInvalidateOnAnimation();
        }
    }
}
