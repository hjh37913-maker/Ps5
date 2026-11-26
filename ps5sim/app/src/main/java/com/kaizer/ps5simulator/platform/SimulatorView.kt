package com.kaizer.ps5simulator.platform

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import com.kaizer.ps5simulator.core.design.Metrics
import com.kaizer.ps5simulator.core.engine.Engine

/** Hosts the engine and drives it with a vsync-aligned invalidate loop. */
class SimulatorView(context: Context, attrs: android.util.AttributeSet? = null) : View(context, attrs) {

    val engine = Engine(context)
    /** True once the first frame has been composed (used by tests to wait for a stable UI). */
    var firstFrameDrawn = false
        private set
    private var lastX = 0f
    private var lastY = 0f

    init {
        isFocusable = true
        isFocusableInTouchMode = true
        keepScreenOn = true
        engine.invalidationCallback = { postInvalidateOnAnimation() }
        engine.init()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val dm = resources.displayMetrics
        engine.updateMetrics(
            Metrics(
                widthPx = w.toFloat(),
                heightPx = h.toFloat(),
                density = dm.density,
                userScale = engine.settings.uiScale,
                insetLeftPx = 0f,
                insetRightPx = 0f,
                insetTopPx = 0f,
                insetBottomPx = 0f,
            )
        )
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        engine.draw(canvas, System.nanoTime() / 1_000_000L)
        firstFrameDrawn = true
        if (engine.isAnimating()) postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x; lastY = event.y
                engine.onDown(event.x, event.y)
            }
            MotionEvent.ACTION_MOVE -> {
                engine.onDrag(event.x, event.y, event.x - lastX, event.y - lastY)
                lastX = event.x; lastY = event.y
            }
            MotionEvent.ACTION_UP -> engine.onUp(event.x, event.y)
            MotionEvent.ACTION_CANCEL -> engine.onCancel()
        }
        postInvalidateOnAnimation()
        return true
    }

    fun handleBack(): Boolean = engine.back()

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        engine.release()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) postInvalidateOnAnimation()
    }
}
