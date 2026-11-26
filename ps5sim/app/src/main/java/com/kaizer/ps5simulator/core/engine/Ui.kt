package com.kaizer.ps5simulator.core.engine

import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface

/**
 * Deterministic immediate-mode UI layer drawn on a single hardware-accelerated Canvas.
 *
 * Why not a widget tree: the console screens are anchored to a fixed design plane
 * (rows measured off official stills). Immediate mode lets us keep every measurement
 * in reference units and guarantees identical placement on any aspect ratio, at the
 * cost of a paint pass — measured well under one frame on a 720x1600 device because
 * all art is decoded async and cached.
 *
 * Painting contract:
 *  - every draw goes through helpers that degrade gracefully when fonts/bitmaps are
 *    still loading, so no frame can ever throw;
 *  - clipping is explicit;
 *  - opacity is applied through [alpha] and restored by the caller through [withAlpha].
 */
class Ui {

    val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.create("sans-serif", Typeface.NORMAL)
        isSubpixelText = true
        isLinearText = true
    }
    val image = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    val rect = RectF()

    var alpha: Float = 1f
        private set

    fun <T> withAlpha(a: Float, block: () -> T): T {
        val prev = alpha
        alpha = (prev * a).coerceIn(0f, 1f)
        try {
            return block()
        } finally {
            alpha = prev
        }
    }

    // ------------------------------------------------------------------ primitives
    fun rect(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, radius: Float = 0f) {
        if (r <= l || b <= t || alpha <= 0.004f) return
        fill.color = withAlpha(color)
        if (radius > 0f) {
            rect.set(l, t, r, b)
            c.drawRoundRect(rect, radius, radius, fill)
        } else {
            c.drawRect(l, t, r, b, fill)
        }
    }

    fun rectOutline(
        c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int,
        radius: Float = 0f, width: Float = 1f,
    ) {
        if (r <= l || b <= t || alpha <= 0.004f) return
        stroke.color = withAlpha(color)
        stroke.strokeWidth = width
        if (radius > 0f) {
            rect.set(l, t, r, b)
            c.drawRoundRect(rect, radius, radius, stroke)
        } else {
            c.drawRect(l, t, r, b, stroke)
        }
    }

    fun circle(c: Canvas, cx: Float, cy: Float, rad: Float, color: Int) {
        if (alpha <= 0.004f || rad <= 0f) return
        fill.color = withAlpha(color)
        c.drawCircle(cx, cy, rad, fill)
    }

    fun gradientRect(
        c: Canvas, l: Float, t: Float, r: Float, b: Float,
        top: Int, bottom: Int, radius: Float = 0f,
    ) {
        if (r <= l || b <= t || alpha <= 0.004f) return
        fill.shader = LinearGradient(l, t, l, b, withAlpha(top), withAlpha(bottom),
            Shader.TileMode.CLAMP)
        if (radius > 0f) {
            rect.set(l, t, r, b)
            c.drawRoundRect(rect, radius, radius, fill)
        } else {
            c.drawRect(l, t, r, b, fill)
        }
        fill.shader = null
    }

    /** Soft radial halo, used for focus glows and backdrop light pools. */
    fun glow(c: Canvas, cx: Float, cy: Float, radius: Float, color: Int, steps: Int = 10) {
        if (alpha <= 0.004f || radius <= 0f) return
        val a = android.graphics.Color.alpha(color)
        val rgb = color and 0x00FFFFFF.toInt()
        for (i in steps downTo 1) {
            val t = i.toFloat() / steps
            val r = radius * t
            val sa = (a * (1f - t) * (1f - t) * 0.9f).toInt()
            if (sa <= 2) continue
            fill.color = withAlpha((sa shl 24) or rgb)
            c.drawCircle(cx, cy, r, fill)
        }
    }

    /** Vertical scrim used under text blocks so typography stays legible. */
    fun scrim(c: Canvas, l: Float, t: Float, r: Float, b: Float, color: Int, fromTop: Boolean) {
        if (r <= l || b <= t || alpha <= 0.004f) return
        val a = android.graphics.Color.alpha(color)
        val rgb = color and 0x00FFFFFF.toInt()
        fill.shader = LinearGradient(
            l, t, l, b,
            if (fromTop) withAlpha((a shl 24) or rgb) else withAlpha(rgb),
            if (fromTop) withAlpha(rgb) else withAlpha((a shl 24) or rgb),
            Shader.TileMode.CLAMP,
        )
        c.drawRect(l, t, r, b, fill)
        fill.shader = null
    }

    // ------------------------------------------------------------------ text
    var fontRegular: Typeface = Typeface.create("sans-serif", Typeface.NORMAL)
    var fontMedium: Typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
    var fontBold: Typeface = Typeface.create("sans-serif", Typeface.BOLD)
    var fontDisplay: Typeface = Typeface.create("sans-serif", Typeface.BOLD)

    private var spacingPx: Float = 0f

    fun textPaint(size: Float, color: Int, weight: Weight = Weight.Regular, letterSpacing: Float = 0f): Paint {
        text.textSize = size
        text.color = withAlpha(color)
        text.typeface = when (weight) {
            Weight.Regular -> fontRegular
            Weight.Medium -> fontMedium
            Weight.Bold -> fontBold
            Weight.Display -> fontDisplay
        }
        text.letterSpacing = letterSpacing
        return text
    }

    enum class Weight { Regular, Medium, Bold, Display }

    /** Draws a single line; returns the advance width. */
    fun line(
        c: Canvas, s: String, x: Float, y: Float, size: Float, color: Int,
        weight: Weight = Weight.Regular, letterSpacing: Float = 0f,
        align: Paint.Align = Paint.Align.LEFT,
    ): Float {
        if (s.isEmpty() || alpha <= 0.004f) return 0f
        val p = textPaint(size, color, weight, letterSpacing)
        p.textAlign = align
        c.drawText(s, x, y, p)
        p.textAlign = Paint.Align.LEFT
        return p.measureText(s)
    }

    fun measure(s: String, size: Float, weight: Weight = Weight.Regular, letterSpacing: Float = 0f): Float {
        val p = textPaint(size, 0xFFFFFFFF.toInt(), weight, letterSpacing)
        return p.measureText(s)
    }

    /** Ellipsized single line that never overflows [maxW]. */
    fun lineFit(
        c: Canvas, s: String, x: Float, y: Float, maxW: Float, size: Float, color: Int,
        weight: Weight = Weight.Regular, letterSpacing: Float = 0f,
        align: Paint.Align = Paint.Align.LEFT,
    ): Float {
        if (s.isEmpty()) return 0f
        val p = textPaint(size, color, weight, letterSpacing)
        var str = s
        if (p.measureText(str) > maxW) {
            var lo = 0
            var hi = str.length
            while (lo < hi) {
                val mid = (lo + hi + 1) / 2
                if (p.measureText(str.substring(0, mid) + "…") <= maxW) lo = mid else hi = mid - 1
            }
            str = str.substring(0, lo).trimEnd() + "…"
        }
        p.textAlign = align
        c.drawText(str, x, y, p)
        p.textAlign = Paint.Align.LEFT
        return p.measureText(str)
    }

    /**
     * Wrapped paragraph with a max line count. Returns the baseline of the last line
     * (or [y] when nothing was drawn).
     */
    fun paragraph(
        c: Canvas, s: String, x: Float, y: Float, maxW: Float, size: Float, color: Int,
        lineHeight: Float, maxLines: Int = 3, weight: Weight = Weight.Regular,
    ): Float {
        if (s.isEmpty()) return y
        val p = textPaint(size, color, weight)
        var line = StringBuilder()
        var count = 0
        var cy = y
        val words = s.split(' ')
        var i = 0
        while (i < words.size) {
            val w = words[i]
            val trial = if (line.isEmpty()) w else "$line $w"
            if (p.measureText(trial) <= maxW) {
                line = StringBuilder(trial)
                i++
            } else {
                if (line.isNotEmpty()) {
                    if (count == maxLines - 1 && i < words.size) {
                        lineFit(c, "$line ${words.drop(i).joinToString(" ")}", x, cy, maxW, size,
                            color, weight)
                        return cy
                    }
                    c.drawText(line.toString(), x, cy, p)
                    count++
                    cy += lineHeight
                    if (count >= maxLines) return cy - lineHeight
                }
                line = StringBuilder()
                if (line.isEmpty() && p.measureText(w) > maxW) {
                    i++
                }
            }
        }
        if (line.isNotEmpty() && count < maxLines) {
            c.drawText(line.toString(), x, cy, p)
            return cy
        }
        return cy - lineHeight
    }

    // ------------------------------------------------------------------ clipping
    inline fun clip(c: Canvas, l: Float, t: Float, r: Float, b: Float, block: () -> Unit) {
        if (r <= l || b <= t) return
        val count = c.save()
        c.clipRect(l, t, r, b)
        try {
            block()
        } finally {
            c.restoreToCount(count)
        }
    }

    inline fun save(c: Canvas, block: () -> Unit) {
        val count = c.save()
        try {
            block()
        } finally {
            c.restoreToCount(count)
        }
    }

    // ------------------------------------------------------------------ color utils
    fun withAlpha(color: Int): Int {
        val a = (android.graphics.Color.alpha(color) * alpha).toInt().coerceIn(0, 255)
        return (a shl 24) or (color and 0x00FFFFFF.toInt())
    }

    companion object {
        fun a(color: Int, factor: Float): Int {
            val alpha = (android.graphics.Color.alpha(color) * factor).toInt().coerceIn(0, 255)
            return (alpha shl 24) or (color and 0x00FFFFFF.toInt())
        }

        fun mix(c1: Int, c2: Int, t: Float): Int {
            val r = ((c1 shr 16 and 0xFF) * (1 - t) + (c2 shr 16 and 0xFF) * t).toInt()
            val g = ((c1 shr 8 and 0xFF) * (1 - t) + (c2 shr 8 and 0xFF) * t).toInt()
            val b = ((c1 and 0xFF) * (1 - t) + (c2 and 0xFF) * t).toInt()
            val a = ((c1 ushr 24) * (1 - t) + (c2 ushr 24) * t).toInt()
            return (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
}
