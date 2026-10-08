package com.example.drivelog;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import androidx.annotation.NonNull;

public class TutorialSpotlightView extends View {

    private final Paint dimPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clearPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulsePaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private RectF targetRect;
    private float cornerRadius = 0f;

    private RectF animStartRect;
    private RectF animEndRect;
    private float animStartRadius;
    private float animEndRadius;
    private ValueAnimator moveAnimator;

    private ValueAnimator pulseAnimator;
    private float pulseOffset = 0f;
    private int pulseAlpha = 255;

    public TutorialSpotlightView(Context context) {
        super(context);
        init();
    }

    public TutorialSpotlightView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        dimPaint.setColor(Color.parseColor("#B3000000")); // 70% dark overlay
        dimPaint.setStyle(Paint.Style.FILL);

        clearPaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR));
        clearPaint.setStyle(Paint.Style.FILL);

        float density = getResources().getDisplayMetrics().density;

        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(3.5f * density);
        borderPaint.setColor(Color.parseColor("#2196F3")); // Primary Blue

        pulsePaint.setStyle(Paint.Style.STROKE);
        pulsePaint.setStrokeWidth(2.0f * density);
        pulsePaint.setColor(Color.parseColor("#2196F3"));

        startPulseAnimation();
    }

    private void startPulseAnimation() {
        if (pulseAnimator != null) pulseAnimator.cancel();
        pulseAnimator = ValueAnimator.ofFloat(0f, 1f);
        pulseAnimator.setDuration(1200);
        pulseAnimator.setRepeatCount(ValueAnimator.INFINITE);
        pulseAnimator.setRepeatMode(ValueAnimator.RESTART);
        float density = getResources().getDisplayMetrics().density;
        pulseAnimator.addUpdateListener(animation -> {
            float val = (float) animation.getAnimatedValue();
            pulseOffset = val * 12f * density;
            pulseAlpha = (int) ((1f - val) * 200);
            invalidate();
        });
        pulseAnimator.start();
    }

    public void setTargetView(View view, float cornerRadiusDp) {
        if (view == null || view.getWidth() == 0 || view.getHeight() == 0) {
            this.targetRect = null;
            invalidate();
            return;
        }

        int[] loc = new int[2];
        view.getLocationOnScreen(loc);

        int[] myLoc = new int[2];
        getLocationOnScreen(myLoc);

        float left = loc[0] - myLoc[0];
        float top = loc[1] - myLoc[1];

        float density = getResources().getDisplayMetrics().density;
        float paddingPx = 6f * density;

        RectF newRect = new RectF(
            left - paddingPx,
            top - paddingPx,
            left + view.getWidth() + paddingPx,
            top + view.getHeight() + paddingPx
        );

        float newRadius = cornerRadiusDp * density;

        if (this.targetRect == null) {
            this.targetRect = newRect;
            this.cornerRadius = newRadius;
            invalidate();
        } else {
            animateToRect(newRect, newRadius);
        }
    }

    private void animateToRect(RectF newRect, float newRadius) {
        if (moveAnimator != null && moveAnimator.isRunning()) {
            moveAnimator.cancel();
        }

        animStartRect = new RectF(this.targetRect);
        animEndRect = newRect;
        animStartRadius = this.cornerRadius;
        animEndRadius = newRadius;

        moveAnimator = ValueAnimator.ofFloat(0f, 1f);
        moveAnimator.setDuration(350);
        moveAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        moveAnimator.addUpdateListener(animation -> {
            float frac = (float) animation.getAnimatedValue();
            targetRect = new RectF(
                animStartRect.left + (animEndRect.left - animStartRect.left) * frac,
                animStartRect.top + (animEndRect.top - animStartRect.top) * frac,
                animStartRect.right + (animEndRect.right - animStartRect.right) * frac,
                animStartRect.bottom + (animEndRect.bottom - animStartRect.bottom) * frac
            );
            cornerRadius = animStartRadius + (animEndRadius - animStartRadius) * frac;
            invalidate();
        });
        moveAnimator.start();
    }

    private final RectF pulseRect = new RectF();

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (targetRect == null || targetRect.isEmpty()) {
            canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);
            return;
        }

        int saveCount = canvas.saveLayer(0, 0, getWidth(), getHeight(), null);

        // 1. Fundo escuro translúcido
        canvas.drawRect(0, 0, getWidth(), getHeight(), dimPaint);

        // 2. Recorte transparente sobre o ícone/view em destaque
        canvas.drawRoundRect(targetRect, cornerRadius, cornerRadius, clearPaint);

        canvas.restoreToCount(saveCount);

        // 3. Borda azul brilhante
        canvas.drawRoundRect(targetRect, cornerRadius, cornerRadius, borderPaint);

        // 4. Anel externo com efeito pulsante de destaque
        if (pulseAlpha > 0) {
            pulseRect.set(
                targetRect.left - pulseOffset,
                targetRect.top - pulseOffset,
                targetRect.right + pulseOffset,
                targetRect.bottom + pulseOffset
            );
            pulsePaint.setAlpha(pulseAlpha);
            canvas.drawRoundRect(pulseRect, cornerRadius + pulseOffset, cornerRadius + pulseOffset, pulsePaint);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (moveAnimator != null) moveAnimator.cancel();
        if (pulseAnimator != null) pulseAnimator.cancel();
    }
}
