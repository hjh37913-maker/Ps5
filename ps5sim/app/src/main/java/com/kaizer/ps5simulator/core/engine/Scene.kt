package com.kaizer.ps5simulator.core.engine

import android.graphics.Canvas

/** A full-screen surface inside the simulator. */
interface Scene {
    val id: Int
    fun draw(f: Frame)
    /** Returns true when the touch was consumed. */
    fun onDown(x: Float, y: Float): Boolean = false
    fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean = false
    fun onUp(x: Float, y: Float): Boolean = false
    /** Hardware back: true when handled, false to let the root close the app. */
    fun back(): Boolean = false
    fun onShow() {}
    /** True while the scene needs continuous frames (animation running). */
    fun isAnimating(): Boolean = false
    fun onHide() {}
}

/** Everything a scene needs for one frame. */
class Frame(
    val canvas: Canvas,
    val ui: Ui,
    val engine: Engine,
    val now: Long,
    val dtMs: Long,
)

/**
 * Hit-testing helpers shared by scenes. Scenes register rectangles once per draw pass
 * so touches always match exactly what the user sees (immediate-mode guarantee).
 */
class Hits {
    private data class Region(
        val l: Float, val t: Float, val r: Float, val b: Float,
        val order: Int, val id: Int, val action: () -> Unit,
    )

    private val regions = ArrayList<Region>(48)
    private var order = 0
    private var pressedId = -1

    fun clear() {
        regions.clear()
        order = 0
    }

    fun add(l: Float, t: Float, r: Float, b: Float, id: Int = order, action: () -> Unit) {
        regions += Region(l, t, r, b, order++, id, action)
    }

    /** Top-most region containing the point (later registrations win). */
    fun pick(x: Float, y: Float): Int {
        for (i in regions.indices.reversed()) {
            val g = regions[i]
            if (x >= g.l && x <= g.r && y >= g.t && y <= g.b) return i
        }
        return -1
    }

    fun down(x: Float, y: Float): Boolean {
        val i = pick(x, y)
        pressedId = i
        return i >= 0
    }

    fun up(x: Float, y: Float): Boolean {
        val i = pick(x, y)
        val was = pressedId
        pressedId = -1
        if (i < 0) return false
        if (was == i) regions[i].action()
        return true
    }

    fun cancel() {
        pressedId = -1
    }

    val pressed: Int get() = pressedId
}
