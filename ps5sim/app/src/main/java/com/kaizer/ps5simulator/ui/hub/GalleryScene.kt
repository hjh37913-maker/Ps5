package com.kaizer.ps5simulator.ui.hub

import android.graphics.Color
import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Ease
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.engine.Engine
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Glyph
import com.kaizer.ps5simulator.core.engine.Icons
import com.kaizer.ps5simulator.core.engine.Scene
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.core.gfx.Art
import com.kaizer.ps5simulator.core.gfx.Draw
import com.kaizer.ps5simulator.data.model.Game

/**
 * GAME HUB > MEDIA — screenshot viewer.
 *
 * Full-screen pager over the game's gallery (three 16:9 captures) plus a "video" entry
 * that opens the simulated launch sequence. Swipe or use the arrows; the counter and
 * filmstrip match the console's media viewer treatment.
 */
class GalleryScene(private val engine: Engine, private val gameId: String) : Scene {

    override val id: Int = 3120

    private val game: Game? get() = engine.game(gameId)
    private var page = 0f
    private var target = 0f
    private var reveal = 0f
    private var dragAccum = 0f

    override fun onShow() { reveal = 0f }

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val g = game ?: return
        if (engine.animationsOn) reveal = (reveal + f.dtMs / 240f).coerceAtMost(1f) else reveal = 1f
        val a = Ease.soft(reveal)
        page += (target - page) * (1f - kotlin.math.exp(-14f * f.dtMs / 1000f))
        val pages = 3
        if (kotlin.math.abs(target - page) < 0.002f) page = target

        c.drawColor(Color.BLACK)
        // pager
        val idx = page
        val base = idx.toInt().coerceIn(0, pages - 1)
        ui.withAlpha(a) {
            for (k in listOf(base - 1, base, base + 1)) {
                if (k < 0 || k >= pages) continue
                val offset = (k - idx) * m.widthPx
                if (kotlin.math.abs(offset) > m.widthPx) continue
                val bmp = Art.get(g.shot(k + 1), m.widthPx.toInt(), (m.widthPx * 9 / 16).toInt())
                val top = (m.heightPx - m.widthPx * 9f / 16f) / 2f
                if (bmp != null) {
                    Draw.inside(c, bmp, offset, top, offset + m.widthPx, top + m.widthPx * 9f / 16f)
                } else {
                    ui.rect(c, offset, top, offset + m.widthPx, top + m.widthPx * 9f / 16f,
                        g.deep)
                }
            }
        }
        // chrome
        ui.scrim(c, 0f, 0f, m.widthPx, m.px(160f), 0xCC000000.toInt(), fromTop = true)
        ui.scrim(c, 0f, m.heightPx - m.px(220f), m.widthPx, m.heightPx, 0xE6000000.toInt(),
            fromTop = false)
        val left = engine.gutter
        val cy = m.px(52f)
        Icons.draw(c, Glyph.ArrowBack, left + m.px(15f), cy, m.px(30f), 0xFFFFFFFF.toInt(), m.px(2.2f))
        engine.hits.add(left - m.px(20f), cy - m.px(28f), left + m.px(200f), cy + m.px(28f), 21001) {
            engine.pop()
        }
        ui.lineFit(c, g.title, left + m.px(46f), cy + m.textPx(11f), m.widthPx * 0.5f,
            m.textPx(30f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
        ui.line(c, "${idx.toInt() + 1} / $pages  ·  MEDIA", m.safeRight - engine.gutter,
            cy + m.textPx(11f), m.textPx(22f), Ui.a(Tok.TextSecondary, 0.9f), Ui.Weight.Medium,
            align = Paint.Align.RIGHT)
        // art notice (original content)
        ui.line(c, engine.s("art_notice"), left, m.heightPx - m.px(120f), m.textPx(15f),
            Ui.a(Tok.TextTertiary, 0.55f), Ui.Weight.Medium)
        // filmstrip
        val tw = m.px(150f)
        val th = tw * 9f / 16f
        val gap = m.px(12f)
        var x = left
        val ty = m.heightPx - m.px(88f)
        for (k in 0 until pages) {
            val bmp = Art.get(g.shot(k + 1), tw.toInt(), th.toInt())
            ui.rect(c, x, ty, x + tw, ty + th, 0xFF111114.toInt(), m.px(6f))
            if (bmp != null) ui.clip(c, x, ty, x + tw, ty + th) {
                Draw.cover(c, bmp, x, ty, x + tw, ty + th)
            }
            if (k == idx.toInt()) ui.rectOutline(c, x, ty, x + tw, ty + th, 0xFFFFFFFF.toInt(),
                m.px(6f), 2f)
            engine.hits.add(x, ty, x + tw, ty + th, 21010 + k) { target = k.toFloat() }
            x += tw + gap
        }
        // video entry (opens the simulated launch)
        val vx = x + m.px(10f)
        if (vx + tw < m.safeRight) {
            ui.rect(c, vx, ty, vx + tw, ty + th, 0xFF16171C.toInt(), m.px(6f))
            ui.circle(c, vx + tw / 2f, ty + th / 2f, m.px(16f), 0x33FFFFFF)
            Icons.draw(c, Glyph.Play, vx + tw / 2f, ty + th / 2f, m.px(18f), 0xFFFFFFFF.toInt(), 0f)
            engine.hits.add(vx, ty, vx + tw, ty + th, 21020) {
                engine.sim.recordLaunch(g.id)
                engine.openLaunch(g)
            }
        }
    }

    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (kotlin.math.abs(dx) < 1f) return false
        dragAccum += dx
        if (kotlin.math.abs(dragAccum) > engine.metrics.px(90f)) {
            target = (target + if (dragAccum < 0) 1f else -1f).coerceIn(0f, 2f)
            dragAccum = 0f
            engine.sfx.play(com.kaizer.ps5simulator.core.engine.Sfx.Cue.NavAlt)
        }
        return true
    }

    override fun isAnimating(): Boolean = reveal < 1f || kotlin.math.abs(target - page) > 0.002f
}
