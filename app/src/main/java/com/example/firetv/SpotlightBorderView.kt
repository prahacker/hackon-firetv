package com.example.firetv

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator

class SpotlightBorderView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val borderPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }

    private val cornerPaint = Paint().apply {
        isAntiAlias = true
        color = 0xFFFFFFFF.toInt()
        style = Paint.Style.FILL
    }

    private val borderRect = RectF()
    private var animator: ValueAnimator? = null
    private var progress = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        borderRect.set(
            borderPaint.strokeWidth / 2,
            borderPaint.strokeWidth / 2,
            w - borderPaint.strokeWidth / 2,
            h - borderPaint.strokeWidth / 2
        )
        startAnimation()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val perimeter = (borderRect.width() + borderRect.height()) * 2
        val distance = perimeter * progress
        drawBorder(canvas, distance)
    }

    private fun drawBorder(canvas: Canvas, distance: Float) {
        val w = borderRect.width()
        val h = borderRect.height()
        when {
            distance < w -> canvas.drawLine(borderRect.left, borderRect.top, borderRect.left + distance, borderRect.top, borderPaint)
            distance < w + h -> {
                canvas.drawLine(borderRect.left, borderRect.top, borderRect.right, borderRect.top, borderPaint)
                canvas.drawLine(borderRect.right, borderRect.top, borderRect.right, borderRect.top + (distance - w), borderPaint)
            }
            distance < w * 2 + h -> {
                canvas.drawLine(borderRect.left, borderRect.top, borderRect.right, borderRect.top, borderPaint)
                canvas.drawLine(borderRect.right, borderRect.top, borderRect.right, borderRect.bottom, borderPaint)
                canvas.drawLine(borderRect.right, borderRect.bottom, borderRect.right - (distance - w - h), borderRect.bottom, borderPaint)
            }
            else -> {
                canvas.drawRect(borderRect, borderPaint)
            }
        }
    }

    private fun startAnimation() {
        animator?.cancel()
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 4000
            interpolator = LinearInterpolator()
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        animator?.cancel()
    }
}