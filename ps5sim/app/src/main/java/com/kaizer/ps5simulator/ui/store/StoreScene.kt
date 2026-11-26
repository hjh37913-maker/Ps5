package com.kaizer.ps5simulator.ui.store

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
import com.kaizer.ps5simulator.ui.components.Chrome

/**
 * PLAYSTATION STORE — visual reconstruction of the store home. Everything is demo data;
 * purchases are intentionally disabled (no real transactions, no server).
 */
class StoreScene(private val engine: Engine) : Scene {
    override val id: Int = 3107

    private var reveal = 0f

    override fun isAnimating(): Boolean = reveal < 1f

    override fun onShow() { reveal = 0f }

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (engine.animationsOn) reveal = (reveal + f.dtMs / 280f).coerceAtMost(1f) else reveal = 1f
        val a = Ease.soft(reveal)
        ui.gradientRect(c, 0f, 0f, m.widthPx, m.heightPx, 0xFF0B1430.toInt(), 0xFF05070E.toInt())
        val hero = engine.games.getOrNull(1)
        if (hero != null) {
            val bmp = Art.get(hero.artwork, m.widthPx.toInt(), m.heightPx.toInt())
            if (bmp != null) ui.withAlpha(0.55f * a) {
                Draw.coverBias(c, bmp, 0f, 0f, m.widthPx, m.heightPx * 0.70f, 0.5f, 0.35f)
            }
        }
        ui.scrim(c, 0f, 0f, m.widthPx, m.heightPx, 0x99000000.toInt(), fromTop = true)
        Chrome.backRow(f, "PlayStation Store")

        ui.withAlpha(a) {
            val left = engine.gutter
            ui.line(c, engine.s("featured"), left, m.px(150f), m.textPx(44f), 0xFFFFFFFF.toInt(),
                Ui.Weight.Display)
            hero?.let { g ->
                ui.lineFit(c, g.title, left, m.px(210f), m.widthPx * 0.5f, m.textPx(30f),
                    0xFFFFFFFF.toInt(), Ui.Weight.Bold)
                ui.line(c, "${g.genre} · ${g.publisher} · ${g.year}", left, m.px(246f),
                    m.textPx(20f), Ui.a(Tok.TextSecondary, 0.85f), Ui.Weight.Medium)
                val price = "£69.99"
                val bw = m.px(420f)
                val bh = m.px(78f)
                val by = m.px(290f)
                ui.rect(c, left, by, left + bw, by + bh, 0xE6FFFFFF.toInt(), bh / 2f)
                ui.line(c, engine.s("add_library"), left + m.px(30f), by + bh / 2f + m.textPx(9f),
                    m.textPx(23f), 0xFF0B0B10.toInt(), Ui.Weight.Bold)
                ui.line(c, price, left + bw - m.px(30f), by + bh / 2f + m.textPx(9f),
                    m.textPx(23f), 0xFF0B0B10.toInt(), Ui.Weight.Bold, align = Paint.Align.RIGHT)
                engine.hits.add(left, by, left + bw, by + bh, 19001) {
                    engine.sim.toggleInstalled(g.id)
                    engine.sfx.play(com.kaizer.ps5simulator.core.engine.Sfx.Cue.Select)
                    engine.toast("Demo only — no purchase was made")
                }
            }
            // deals row
            val rowY = m.heightPx - m.px(300f)
            ui.line(c, engine.s("deals"), left, rowY - m.px(20f), m.textPx(24f),
                Ui.a(Tok.TextPrimary, 0.9f), Ui.Weight.Medium)
            val tw = m.px(180f)
            val gap = m.px(18f)
            engine.games.drop(2).take(6).forEachIndexed { i, g ->
                val x = left + i * (tw + gap)
                if (x + tw > m.safeRight) return@forEachIndexed
                ui.rect(c, x, rowY, x + tw, rowY + tw, 0xFF0B0B0F.toInt(), m.px(10f))
                val bmp = Art.get(g.cardArt, tw.toInt(), tw.toInt())
                if (bmp != null) ui.clip(c, x, rowY, x + tw, rowY + tw) {
                    Draw.cover(c, bmp, x, rowY, x + tw, rowY + tw)
                } else {
                    ui.gradientRect(c, x, rowY, x + tw, rowY + tw, g.accent, g.deep, m.px(10f))
                }
                // discount tag
                ui.rect(c, x, rowY, x + m.px(74f), rowY + m.px(30f), 0xFF4D8DF6.toInt(), 0f)
                ui.line(c, "-${20 + (i * 15)}%", x + m.px(8f), rowY + m.px(21f), m.textPx(16f),
                    0xFFFFFFFF.toInt(), Ui.Weight.Bold)
                ui.lineFit(c, g.title, x, rowY + tw + m.px(24f), tw, m.textPx(19f),
                    0xFFFFFFFF.toInt(), Ui.Weight.Medium)
                ui.line(c, "£${29 + i * 10}.99 · demo", x, rowY + tw + m.px(46f), m.textPx(16f),
                    Ui.a(Tok.TextSecondary, 0.65f), Ui.Weight.Regular)
                engine.hits.add(x, rowY, x + tw, rowY + tw, 19100 + i) { engine.openHub(g.id) }
            }
        }
        ui.line(c, engine.s("store_demo"), m.widthPx / 2f,
            m.heightPx - m.px(18f), m.textPx(16f), Ui.a(Tok.TextTertiary, 0.5f),
            Ui.Weight.Regular, align = Paint.Align.CENTER)
    }
}
