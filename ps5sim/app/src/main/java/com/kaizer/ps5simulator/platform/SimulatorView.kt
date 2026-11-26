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
        tag = TAG_SIMULATOR
        engine.invalidationCallback = { postInvalidateOnAnimation() }
        engine.init()
    }

    /**
     * Layout plane. The console interface is authored on a 16:9 artboard; in portrait we
     * letterbox that artboard (full width, vertically centred) instead of reflowing it,
     * so nothing is ever cropped or stretched — the same rule the console's own output
     * scaler follows. In landscape it fills the screen.
     */
    private var artboardTop = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        computeMetrics(w, h)
    }

    fun computeMetrics(w: Int, h: Int) {
        val dm = resources.displayMetrics
        val portrait = h > w
        val artH = if (portrait) (w * 9f / 16f) else h.toFloat()
        artboardTop = if (portrait) (h - artH) / 2f else 0f
        engine.updateMetrics(
            Metrics(
                widthPx = w.toFloat(),
                heightPx = artH,
                density = dm.density,
                userScale = engine.settings.uiScale,
                insetLeftPx = 0f,
                insetRightPx = 0f,
                insetTopPx = 0f,
                insetBottomPx = 0f,
            )
        )
    }

    private fun toArtboardY(y: Float): Float = y - artboardTop

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        canvas.translate(0f, artboardTop)
        engine.draw(canvas, System.nanoTime() / 1_000_000L)
        canvas.restore()
        firstFrameDrawn = true
        if (engine.needsFrame()) postInvalidateOnAnimation()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y - artboardTop
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = x; lastY = y
                engine.onDown(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                engine.onDrag(x, y, x - lastX, y - lastY)
                lastX = x; lastY = y
            }
            MotionEvent.ACTION_UP -> engine.onUp(x, y)
            MotionEvent.ACTION_CANCEL -> engine.onCancel()
        }
        postInvalidateOnAnimation()
        return true
    }

    fun handleBack(): Boolean = engine.back()

    companion object {
        const val TAG_SIMULATOR = "ps5.simulator.view"
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        engine.release()
    }

    override fun onWindowFocusChanged(hasWindowFocus: Boolean) {
        super.onWindowFocusChanged(hasWindowFocus)
        if (hasWindowFocus) postInvalidateOnAnimation()
    }
}
