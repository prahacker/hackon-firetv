package com.example.firetv

import android.animation.ValueAnimator
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator

class SpotlightDrawable : Drawable() {

    private val pathPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 6f // A thicker line for better visibility
        strokeCap = Paint.Cap.ROUND
    }

    private val path = Path()
    private val pathMeasure = PathMeasure()
    private val pathSegment = Path()

    private val animator = ValueAnimator.ofFloat(0f, 1f).apply {
        duration = 3000 // Animation duration in milliseconds
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            invalidateSelf()
        }
    }

    override fun onBoundsChange(bounds: Rect) {
        // Create a rounded rectangle path based on the view's bounds
        val cornerRadius = 12f
        path.reset()
        path.addRoundRect(RectF(bounds), cornerRadius, cornerRadius, Path.Direction.CW)
        pathMeasure.setPath(path, false)
    }

    override fun draw(canvas: Canvas) {
        // Create the "comet" or "trailing" effect using a gradient
        val progress = animator.animatedFraction
        val perimeter = pathMeasure.length
        val headPosition = perimeter * progress
        val tailLength = perimeter * 0.15f // The tail will be 15% of the total length

        val start = (headPosition - tailLength).coerceAtLeast(0f)
        val stop = headPosition

        // Gradient: Fades from white (head) to transparent (tail)
        val gradient = LinearGradient(
            0f, 0f, 100f, 0f, // Use dummy coordinates, matrix will position it
            intArrayOf(Color.TRANSPARENT, Color.WHITE),
            floatArrayOf(0f, 1f),
            Shader.TileMode.CLAMP
        )

        pathPaint.shader = gradient

        pathSegment.reset()
        pathMeasure.getSegment(start, stop, pathSegment, true)
        canvas.drawPath(pathSegment, pathPaint)
    }

    fun start() {
        if (!animator.isStarted) {
            animator.start()
        }
    }

    fun stop() {
        animator.cancel()
    }

    override fun setAlpha(alpha: Int) {
        pathPaint.alpha = alpha
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        pathPaint.colorFilter = colorFilter
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}