package com.kaizer.ps5simulator.ui.components

import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Ref
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.core.gfx.Art
import com.kaizer.ps5simulator.core.gfx.Draw
import com.kaizer.ps5simulator.data.model.ActivityCard
import com.kaizer.ps5simulator.data.model.Game
import kotlin.math.min

/** Reusable console card surfaces: activity cards, info cards, thumbnails. */
object Cards {

    /**
     * Control Center activity card (portrait media card, progress ring badge).
     * Geometry from the reference: 348 wide, 5:7 art window on top, caption block below.
     */
    fun activityCard(
        f: Frame, card: ActivityCard, game: Game?, l: Float, t: Float, w: Float, h: Float,
        focused: Boolean, onTap: () -> Unit,
    ) {
        val ui = f.ui
        val c = f.canvas
        val m = f.engine.metrics
        val radius = m.px(Ref.CC_CARD_RADIUS)
        val g = game
        ui.rect(c, l, t, l + w, t + h, 0xFF0C0C10.toInt(), radius)
        val artH = h * 0.60f
        val bmp = g?.let { Art.get(it.artwork, w.toInt().coerceAtLeast(64), artH.toInt().coerceAtLeast(64)) }
        if (g != null && bmp != null) {
            ui.clip(c, l, t, l + w, t + artH) {
                Draw.coverBias(c, bmp, l, t, l + w, t + artH, 0.5f, 0.35f)
            }
        } else if (g != null) {
            ui.gradientRect(c, l, t, l + w, t + artH, g.accent, g.deep, radius)
        }
        // darken art so the caption stays legible (console keeps art bright but plates text)
        ui.scrim(c, l, t + artH * 0.45f, l + w, t + h, 0xE6000000.toInt(), fromTop = false)
        ui.rect(c, l, t + artH, l + w, t + h, 0x00000000.toInt(), 0f)

        // progress badge (big % with underline) — console style
        card.progress?.let { p ->
            val pText = "$p%"
            val ps = m.textPx(40f)
            val px = l + m.px(20f)
            val py = t + m.px(56f)
            ui.line(c, pText, px, py, ps, 0xFFFFFFFF.toInt(), Ui.Weight.Display)
            val barW = ui.measure(pText, ps, Ui.Weight.Display) * 0.86f
            ui.rect(c, px, py + m.px(12f), px + barW, py + m.px(15f), 0x44FFFFFF.toInt(), 2f)
            ui.rect(c, px, py + m.px(12f), px + barW * (p / 100f), py + m.px(15f),
                0xFFFFFFFF.toInt(), 2f)
            card.progressLabel?.let { lbl ->
                val s = m.textPx(18f)
                val bw = ui.measure(lbl, s, Ui.Weight.Medium) + m.px(18f)
                val by = py + m.px(24f)
                ui.rect(c, px, by, px + bw, by + m.px(28f), 0xCC000000.toInt(), m.px(4f))
                ui.rectOutline(c, px, by, px + bw, by + m.px(28f), 0x66FFFFFF.toInt(), m.px(4f), 1f)
                ui.line(c, lbl, px + m.px(9f), by + m.px(19.5f), s, 0xFFFFFFFF.toInt(),
                    Ui.Weight.Medium)
            }
        }
        // caption block
        val capTop = t + artH + m.px(16f)
        ui.lineFit(c, card.category, l + m.px(20f), capTop + m.textPx(10f), w - m.px(40f),
            m.textPx(18f), Ui.a(Tok.TextSecondary, 0.85f), Ui.Weight.Medium)
        ui.lineFit(c, card.title, l + m.px(20f), capTop + m.px(22f) + m.textPx(24f), w - m.px(40f),
            m.textPx(26f), 0xFFFFFFFF.toInt(), Ui.Weight.Bold)
        ui.lineFit(c, card.subtitle, l + m.px(20f), capTop + m.px(52f) + m.textPx(24f),
            w - m.px(40f), m.textPx(20f), Ui.a(Tok.TextSecondary, 0.7f), Ui.Weight.Regular)

        if (focused) {
            Chrome.focusRing(f, l, t, l + w, t + h, radius)
        }
        f.engine.hits.add(l, t, l + w, t + h, 9800 + card.id.hashCode()) { onTap() }
    }

    /** Game Hub info card ("Time to beat", "Your time", …) with hairline separators. */
    fun infoCard(
        f: Frame, l: Float, t: Float, w: Float, h: Float, title: String, icon: Int,
        lines: List<Pair<String, String>>, footer: String,
    ) {
        val ui = f.ui
        val c = f.canvas
        val m = f.engine.metrics
        val radius = m.px(18f)
        ui.rect(c, l, t, l + w, t + h, 0x8C0E1014.toInt(), radius)
        ui.rectOutline(c, l, t, l + w, t + h, 0x1AFFFFFF.toInt(), radius, 1.2f)
        // header
        com.kaizer.ps5simulator.core.engine.Icons.draw(c, iconToGlyph(icon), l + m.px(26f),
            t + m.px(26f), m.px(20f), Ui.a(Tok.TextSecondary, 0.9f), m.px(1.8f))
        ui.line(c, title, l + m.px(44f), t + m.px(33f), m.textPx(20f),
            Ui.a(Tok.TextSecondary, 0.9f), Ui.Weight.Medium)
        // big values row
        var x = l + m.px(24f)
        val vy = t + m.px(80f)
        lines.forEachIndexed { i, (value, label) ->
            val vs = m.textPx(34f)
            val vw = ui.line(c, value, x, vy, vs, 0xFFFFFFFF.toInt(), Ui.Weight.Display)
            ui.line(c, label, x, vy + m.px(24f), m.textPx(18f), Ui.a(Tok.TextSecondary, 0.7f),
                Ui.Weight.Regular)
            x += vw + m.px(22f)
            if (i < lines.size - 1) {
                ui.rectOutline(c, x - m.px(11f), vy - m.px(26f), x - m.px(11f) + 1.2f,
                    vy + m.px(20f), 0x1FFFFFFF.toInt(), 0f, 1.2f)
            }
        }
        // footer with thin progress line
        ui.rect(c, l + m.px(24f), t + h - m.px(34f), l + w - m.px(24f), t + h - m.px(31f),
            0x22FFFFFF.toInt(), 1.5f)
        ui.rect(c, l + m.px(24f), t + h - m.px(34f),
            l + m.px(24f) + (w - m.px(48f)) * 0.54f, t + h - m.px(31f), 0xFF4D8DF6.toInt(), 1.5f)
        ui.lineFit(c, footer, l + m.px(24f), t + h - m.px(12f), w - m.px(48f), m.textPx(17f),
            Ui.a(Tok.TextSecondary, 0.75f), Ui.Weight.Regular)
    }

    private fun iconToGlyph(icon: Int): com.kaizer.ps5simulator.core.engine.Glyph =
        com.kaizer.ps5simulator.core.engine.Glyph.entries[icon.coerceIn(
            0, com.kaizer.ps5simulator.core.engine.Glyph.entries.size - 1)]

    /** Library grid tile: square art with platform tag, optional badge. */
    fun libraryTile(
        f: Frame, g: Game, l: Float, t: Float, size: Float, focused: Boolean,
        installed: Boolean, favourite: Boolean, onTap: () -> Unit,
    ) {
        val ui = f.ui
        val c = f.canvas
        val m = f.engine.metrics
        val radius = m.px(Ref.LIB_TILE_RADIUS)
        ui.rect(c, l, t, l + size, t + size, 0xFF0B0B0F.toInt(), radius)
        val bmp = Art.get(g.coverArt, size.toInt().coerceAtLeast(64), size.toInt().coerceAtLeast(64))
        if (bmp != null) {
            ui.clip(c, l, t, l + size, t + size) {
                Draw.cover(c, bmp, l, t, l + size, t + size)
            }
        } else {
            ui.gradientRect(c, l, t, l + size, t + size, g.accent, g.deep, radius)
            ui.line(c, g.titleInitials, l + size / 2f, t + size * 0.58f, size * 0.20f,
                0x66FFFFFF.toInt(), Ui.Weight.Display, align = Paint.Align.CENTER)
        }
        ui.rectOutline(c, l, t, l + size, t + size, 0x14FFFFFF.toInt(), radius, 1f)
        // badges
        if (favourite) {
            com.kaizer.ps5simulator.core.engine.Icons.draw(c,
                com.kaizer.ps5simulator.core.engine.Glyph.Star, l + size - m.px(20f),
                t + m.px(20f), m.px(18f), 0xFFFFD166.toInt(), 0f)
        }
        if (!installed) {
            val s = m.textPx(15f)
            val tw = ui.measure("NOT INSTALLED", s, Ui.Weight.Bold) + m.px(12f)
            ui.rect(c, l + m.px(10f), t + size - m.px(14f) - m.px(20f), l + m.px(10f) + tw,
                t + size - m.px(14f), 0xCC000000.toInt(), m.px(3f))
            ui.line(c, "NOT INSTALLED", l + m.px(16f), t + size - m.px(20f), s,
                0xFFFFFFFF.toInt(), Ui.Weight.Bold)
        }
        if (focused) Chrome.focusRing(f, l, t, l + size, t + size, radius)
        f.engine.hits.add(l, t, l + size, t + size, 9900 + g.id.hashCode()) { onTap() }
    }

    /** Small selectable chip used by library filters and sort menus. */
    fun chip(
        f: Frame, label: String, l: Float, t: Float, active: Boolean, onTap: () -> Unit,
    ): Float {
        val m = f.engine.metrics
        val ui = f.ui
        val s = m.textPx(22f)
        val w = ui.measure(label, s, Ui.Weight.Medium) + m.px(36f)
        val h = m.px(48f)
        ui.rect(f.canvas, l, t, l + w, t + h,
            if (active) 0xF2FFFFFF.toInt() else 0x2EFFFFFF.toInt(), h / 2f)
        if (!active) ui.rectOutline(f.canvas, l, t, l + w, t + h, 0x33FFFFFF.toInt(), h / 2f, 1.4f)
        ui.line(f.canvas, label, l + w / 2f, t + h / 2f + s * 0.36f, s,
            if (active) 0xFF101014.toInt() else 0xE6FFFFFF.toInt(), Ui.Weight.Medium,
            align = Paint.Align.CENTER)
        f.engine.hits.add(l, t, l + w, t + h, 9950 + label.hashCode()) { onTap() }
        return w
    }

    /** Thin focusable row used in Settings / Profile lists. */
    fun row(
        f: Frame, l: Float, r: Float, t: Float, h: Float, title: String, value: String?,
        chevron: Boolean = true, onTap: (() -> Unit)? = null, enabled: Boolean = true,
    ) {
        val m = f.engine.metrics
        val ui = f.ui
        val c = f.canvas
        val alpha = if (enabled) 1f else 0.4f
        ui.lineFit(c, title, l, t + h / 2f + m.textPx(11f), (r - l) * 0.62f, m.textPx(27f),
            Ui.a(Tok.TextPrimary, 0.95f * alpha), Ui.Weight.Medium)
        value?.let {
            ui.lineFit(c, it, l, t + h / 2f + m.textPx(10f), 0f, m.textPx(23f),
                Ui.a(Tok.TextSecondary, 0.9f * alpha), Ui.Weight.Regular,
                align = Paint.Align.RIGHT)
            // right-align manually against the row edge
            val vw = ui.measure(it, m.textPx(23f), Ui.Weight.Regular)
            ui.line(c, it, r - (if (chevron) m.px(34f) else 0f) - vw, t + h / 2f + m.textPx(10f),
                m.textPx(23f), Ui.a(Tok.TextSecondary, 0.9f * alpha), Ui.Weight.Regular)
        }
        if (chevron) {
            com.kaizer.ps5simulator.core.engine.Icons.draw(c,
                com.kaizer.ps5simulator.core.engine.Glyph.ChevronRight, r - m.px(12f),
                t + h / 2f, m.px(20f), Ui.a(Tok.TextTertiary, 0.8f * alpha), m.px(1.8f))
        }
        if (onTap != null && enabled) f.engine.hits.add(l, t, r, t + h, 9960 + title.hashCode(), onTap)
    }

    /** Console toggle switch (right-aligned in a row). */
    fun toggle(f: Frame, l: Float, t: Float, on: Boolean, w: Float, h: Float): Boolean {
        val ui = f.ui
        val c = f.canvas
        val m = f.engine.metrics
        ui.rect(c, l, t, l + w, t + h, if (on) 0xFF4D8DF6.toInt() else 0x3DFFFFFF.toInt(), h / 2f)
        val knobR = h / 2f - m.px(4f)
        val cx = if (on) l + w - h / 2f else l + h / 2f
        ui.circle(c, cx, t + h / 2f, knobR, 0xFFFFFFFF.toInt())
        return on
    }

    /** Horizontal slider with a fill and a knob, used by Control Center. */
    fun slider(
        f: Frame, l: Float, t: Float, w: Float, h: Float, value: Float, iconOnLeft: Int?,
        onChange: (Float) -> Unit,
    ) {
        val ui = f.ui
        val c = f.canvas
        val m = f.engine.metrics
        ui.rect(c, l, t, l + w, t + h, 0x33FFFFFF.toInt(), h / 2f)
        if (value > 0.01f) {
            ui.rect(c, l, t, l + w * value, t + h, 0xE6FFFFFF.toInt(), h / 2f)
        }
        iconOnLeft?.let {
            com.kaizer.ps5simulator.core.engine.Icons.draw(c,
                com.kaizer.ps5simulator.core.engine.Glyph.entries[it.coerceIn(0,
                    com.kaizer.ps5simulator.core.engine.Glyph.entries.size - 1)],
                l + h * 0.5f, t + h / 2f, h * 0.44f,
                if (value > 0.2f) 0xFF101014.toInt() else 0xCCFFFFFF.toInt(), m.px(1.6f))
        }
        val cx = (l + w * value).coerceIn(l + h / 2f, l + w - h / 2f)
        ui.circle(c, cx, t + h / 2f, h * 0.62f, 0xFFFFFFFF.toInt())
        f.engine.hits.add(l, t - h * 0.4f, l + w, t + h * 1.4f, 9980 + l.toInt()) {
            onChange(((f.engine.lastTouchX - l) / w).coerceIn(0f, 1f))
        }
    }
}
