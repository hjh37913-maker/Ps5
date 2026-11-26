package com.kaizer.ps5simulator.ui.components

import android.graphics.Canvas
import android.graphics.Paint
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Glyph
import com.kaizer.ps5simulator.core.engine.Hits
import com.kaizer.ps5simulator.core.engine.Icons
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.core.design.Metrics
import com.kaizer.ps5simulator.core.design.Ref
import com.kaizer.ps5simulator.core.design.Tok
import kotlin.math.min

/**
 * Shared console chrome: top navigation (tabs + system glyphs), focus ring, and small
 * typographic helpers used by every screen.
 *
 * Measurements come from the reference plane: tabs sit on the top bar baseline with the
 * active tab at full white and the inactive one at 55% white; the glyph cluster is
 * right-aligned to the gutter; the clock closes the row.
 */
object Chrome {

    const val TAB_GAMES = 0
    const val TAB_MEDIA = 1

    /** Draws the top bar. Returns the y of the switcher row top in device px. */
    fun topBar(
        f: Frame,
        activeTab: Int,
        titles: List<String>,
        onTab: (Int) -> Unit,
        showClock: Boolean,
        rightInset: Float = 0f,
    ): Float {
        val m = f.engine.metrics
        val ui = f.ui
        val c = f.canvas
        val left = m.px(Ref.TAB_INSET_LEFT)
        val top = m.px(38f)
        val size = m.textPx(Ref.TAB_TEXT_SIZE)

        // tab row baseline
        val baseline = top + size * 0.82f
        var x = left
        titles.forEachIndexed { i, t ->
            val active = i == activeTab
            val w = ui.measure(t, size, Ui.Weight.Bold, letterSpacing = 0.01f)
            ui.line(
                c, t, x, baseline, size,
                if (active) 0xFFFFFFFF.toInt() else Ui.a(Tok.TextSecondary, 0.74f),
                Ui.Weight.Bold, letterSpacing = 0.012f,
            )
            val pad = m.px(14f)
            val id = 9100 + i
            f.engine.hits.add(x - pad, top - m.px(10f), x + w + pad, baseline + m.px(14f), id) {
                onTab(i)
            }
            if (active) {
                // subtle underline used by the console when a section is focused
                ui.rect(c, x, baseline + m.px(12f), x + w, baseline + m.px(14f),
                    Ui.a(Tok.TextPrimary, 0.0f), 0f)
            }
            x += m.px(Ref.TAB_SPACING)
        }

        // right cluster: search, settings, avatar, clock
        val glyph = m.px(32f)
        var rx = m.safeRight - m.px(28f) - rightInset
        if (showClock) {
            val clock = f.engine.clockText()
            val cs = m.textPx(26f)
            val cw = ui.measure(clock, cs, Ui.Weight.Medium)
            ui.line(c, clock, rx - cw, baseline, cs, Ui.a(Tok.TextPrimary, 0.92f), Ui.Weight.Medium)
            rx -= cw + m.px(46f)
        }
        // avatar
        val avR = m.px(18f)
        val avCx = rx - avR
        avatar(c, ui, avCx, baseline - size * 0.30f, avR)
        rx = avCx - avR - m.px(36f)
        // settings
        Icons.draw(c, Glyph.Settings, rx - glyph / 2f, baseline - size * 0.30f, glyph,
            Ui.a(Tok.TextPrimary, 0.9f), m.px(2.4f))
        f.engine.hits.add(rx - glyph, baseline - size * 0.8f, rx, baseline + m.px(6f), 9201) {
            f.engine.openSettings()
        }
        rx -= glyph + m.px(34f)
        Icons.draw(c, Glyph.Search, rx - glyph / 2f, baseline - size * 0.30f, glyph,
            Ui.a(Tok.TextPrimary, 0.9f), m.px(2.4f))
        f.engine.hits.add(rx - glyph, baseline - size * 0.8f, rx, baseline + m.px(6f), 9202) {
            f.engine.toast("Search is available in the Game Library")
        }
        return m.px(Ref.SWITCHER_TOP)
    }

    /**
     * Draws the console's round profile avatar. The avatar art is original (a soft
     * gradient bust silhouette on a dark disc) because no PSN imagery is bundled.
     */
    fun avatar(c: Canvas, ui: Ui, cx: Float, cy: Float, r: Float, seed: Int = 3,
               ring: Boolean = false) {
        ui.circle(c, cx, cy, r, 0xFF2A3350.toInt())
        // bust silhouette
        val bodyTop = cy + r * 0.18f
        ui.rect(c, cx - r * 0.66f, bodyTop, cx + r * 0.66f, cy + r, 0xFF1B2238.toInt(), r * 0.5f)
        ui.circle(c, cx, cy - r * 0.28f, r * 0.36f, 0xFF1B2238.toInt())
        // warm rim light so the disc reads in dark scenes
        ui.circle(c, cx, cy - r * 0.34f, r * 0.30f, 0x33FFD9A8.toInt())
        if (ring) {
            ui.rectOutline(c, cx - r, cy - r, cx + r, cy + r, 0x66FFFFFF.toInt(), r, r * 0.10f)
        }
    }

    /** Thin white focus ring with a soft outer glow, used on tiles and cards. */
    fun focusRing(f: Frame, l: Float, t: Float, r: Float, b: Float, radius: Float, alpha: Float = 1f) {
        val ui = f.ui
        val c = f.canvas
        ui.withAlpha(alpha) {
            ui.rectOutline(c, l - 3f, t - 3f, r + 3f, b + 3f, 0x40FFFFFF.toInt(), radius + 3f, 2.5f)
            ui.rectOutline(c, l, t, r, b, 0xF2FFFFFF.toInt(), radius, 3.5f)
        }
    }

    /** Small console-style pill label ("In progress", platform tags, etc.). */
    fun pill(f: Frame, text: String, l: Float, t: Float, textSize: Float, fill: Int, color: Int): Float {
        val ui = f.ui
        val w = ui.measure(text, textSize, Ui.Weight.Medium) + textSize * 1.1f
        val h = textSize * 1.75f
        ui.rect(f.canvas, l, t, l + w, t + h, fill, h / 2f)
        ui.line(f.canvas, text, l + textSize * 0.55f, t + h * 0.72f, textSize, color, Ui.Weight.Medium)
        return w
    }

    /** Section label ("Must see", "Console storage", …). */
    fun sectionLabel(f: Frame, text: String, l: Float, baseline: Float, alpha: Float = 0.92f) {
        val m = f.engine.metrics
        f.ui.line(f.canvas, text, l, baseline, m.textPx(28f), Ui.a(Tok.TextPrimary, alpha),
            Ui.Weight.Medium)
    }

    /** Thin horizontal separator used in lists. */
    fun divider(f: Frame, l: Float, r: Float, y: Float, alpha: Float = 0.10f) {
        f.ui.rect(f.canvas, l, y, r, y + 1.2f, Ui.a(0xFFFFFFFF.toInt(), alpha))
    }

    /** Concentric soft shadow under the switcher row (console tiles float over art). */
    fun floatShadow(f: Frame, l: Float, t: Float, r: Float, b: Float, radius: Float) {
        val ui = f.ui
        for (i in 3 downTo 1) {
            val g = i * 5f
            ui.rect(
                f.canvas, l - g * 0.4f, t + g * 0.6f, r + g * 0.4f, b + g,
                0x14000000.toInt(), radius,
            )
        }
    }

    /** Back chevron + label used by sub-screens. Registers a back hit target. */
    fun backRow(f: Frame, label: String): Boolean {
        val m = f.engine.metrics
        val left = f.engine.gutter
        val cy = m.px(52f)
        val size = m.px(30f)
        Icons.draw(f.canvas, Glyph.ArrowBack, left + size * 0.5f, cy, size,
            Ui.a(Tok.TextPrimary, 0.9f), m.px(2.2f))
        f.engine.hits.add(left - m.px(20f), cy - m.px(26f), left + m.px(120f), cy + m.px(26f), 9300) {
            f.engine.pop()
        }
        if (label.isNotEmpty()) {
            f.ui.line(f.canvas, label, left + size * 1.4f, cy + m.textPx(10f), m.textPx(26f),
                Ui.a(Tok.TextSecondary, 0.9f), Ui.Weight.Medium)
        }
        return true
    }

    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun clamp(v: Float, lo: Float, hi: Float): Float = min(hi, maxOf(lo, v))
}
