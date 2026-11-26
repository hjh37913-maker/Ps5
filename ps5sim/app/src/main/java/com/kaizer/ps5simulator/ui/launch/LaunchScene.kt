package com.kaizer.ps5simulator.ui.launch

import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Ease
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.engine.Engine
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Scene
import com.kaizer.ps5simulator.core.engine.Sfx
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.core.gfx.Art
import com.kaizer.ps5simulator.core.gfx.Draw
import com.kaizer.ps5simulator.data.model.Game

/**
 * SIMULATED GAME LAUNCH.
 *
 * The console sequence, rebuilt in miniature: artwork fills, the title resolves, a
 * progress bar advances, then a short "simulation running" panel appears with live
 * stats and a clean exit. Clearly labelled as a simulation — it must never claim to
 * launch a real game.
 *
 * Timing is frame-driven from frame deltas so it behaves identically at any refresh rate.
 */
class LaunchScene(private val engine: Engine, private val gameId: String) : Scene {

    override val id: Int = 3110

    private var t: Float = 0f            // seconds since start
    private var phase = 0                // 0 intro, 1 loading, 2 running, 3 exiting
    private var exitFade = 1f
    private var tapCount = 0
    private var lastTap = 0L

    private val game: Game? get() = engine.game(gameId)

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val g = game ?: return
        t += f.dtMs / 1000f

        // phases
        when (phase) {
            0 -> if (t > 1.1f) { phase = 1; t = 0f }
            1 -> if (t > 2.4f) { phase = 2; t = 0f }
            3 -> {
                exitFade -= f.dtMs / 320f
                if (exitFade <= 0f) {
                    engine.back()
                    return
                }
            }
        }

        ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, 0xFF05060A.toInt())
        val bmp = Art.get(g.artwork, m.widthPx.toInt(), m.heightPx.toInt())
        val intro = if (phase == 0) Ease.out((t / 1.1f).coerceIn(0f, 1f)) else 1f
        if (bmp != null) {
            val scale = 1.08f - 0.08f * intro
            val w = m.widthPx * scale
            val h = m.heightPx * scale
            ui.withAlpha(intro * exitFade) {
                Draw.cover(c, bmp, (m.widthPx - w) / 2f, (m.heightPx - h) / 2f,
                    (m.widthPx + w) / 2f, (m.heightPx + h) / 2f)
            }
        }
        ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, 0x73000000.toInt())
        ui.scrim(c, 0f, 0f, m.widthPx, m.heightPx * 0.5f, 0x99000000.toInt(), fromTop = true)
        ui.scrim(c, 0f, m.heightPx * 0.55f, m.widthPx, m.heightPx, 0xCC000000.toInt(), fromTop = false)

        when (phase) {
            0, 1 -> drawLoading(f, g, intro)
            2 -> drawRunning(f, g)
            else -> drawRunning(f, g)
        }

        // simulation badge, always visible
        val bs = m.textPx(16f)
        val label = "SIMULATION — NOT A REAL GAME SESSION"
        val bw = ui.measure(label, bs, Ui.Weight.Medium) + m.px(28f)
        ui.rect(c, m.safeRight - engine.gutter - bw, m.px(28f), m.safeRight - engine.gutter,
            m.px(28f) + m.px(34f), 0xCC101014.toInt(), m.px(17f))
        ui.rectOutline(c, m.safeRight - engine.gutter - bw, m.px(28f), m.safeRight - engine.gutter,
            m.px(28f) + m.px(34f), 0x33FFFFFF.toInt(), m.px(17f), 1f)
        ui.line(c, label, m.safeRight - engine.gutter - bw + m.px(14f), m.px(28f) + m.px(22f),
            bs, Ui.a(Tok.TextSecondary, 0.9f), Ui.Weight.Medium)
    }

    private fun drawLoading(f: Frame, g: Game, intro: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val cx = m.widthPx / 2f
        val cy = m.heightPx / 2f
        ui.withAlpha(intro) {
            val titleSize = m.textPx(56f)
            ui.lineFit(c, g.title, cx, cy - m.px(30f), m.widthPx * 0.7f, titleSize,
                0xFFFFFFFF.toInt(), Ui.Weight.Display, align = Paint.Align.CENTER)
            // progress bar
            val bw = m.widthPx * 0.32f
            val bh = m.px(4f)
            val by = cy + m.px(26f)
            ui.rect(c, cx - bw / 2f, by, cx + bw / 2f, by + bh, 0x33FFFFFF.toInt(), bh / 2f)
            val p = if (phase == 0) 0.08f else Ease.soft((t / 2.4f).coerceIn(0f, 1f))
            ui.rect(c, cx - bw / 2f, by, cx - bw / 2f + bw * p, by + bh, 0xFFFFFFFF.toInt(), bh / 2f)
            ui.line(c, "Loading simulation…", cx, by + m.px(42f), m.textPx(20f),
                Ui.a(Tok.TextSecondary, 0.75f), Ui.Weight.Medium, align = Paint.Align.CENTER)
        }
    }

    private fun drawRunning(f: Frame, g: Game) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val left = engine.gutter
        val top = m.heightPx * 0.42f
        // session panel
        val pw = m.px(760f)
        val ph = m.px(300f)
        ui.rect(c, left, top, left + pw, top + ph, 0xCC0B0C10.toInt(), m.px(22f))
        ui.rectOutline(c, left, top, left + pw, top + ph, 0x1FFFFFFF.toInt(), m.px(22f), 1.2f)
        ui.line(c, "Simulation running", left + m.px(34f), top + m.px(56f), m.textPx(24f),
            Ui.a(Tok.TextSecondary, 0.9f), Ui.Weight.Medium)
        ui.lineFit(c, g.title, left + m.px(34f), top + m.px(116f), pw - m.px(68f),
            m.textPx(44f), 0xFFFFFFFF.toInt(), Ui.Weight.Display)
        ui.line(c, "${g.genre} · ${g.publisher}", left + m.px(34f), top + m.px(154f),
            m.textPx(21f), Ui.a(Tok.TextSecondary, 0.8f), Ui.Weight.Regular)

        // live stats
        val secs = (System.currentTimeMillis() / 1000).toInt()
        val stats = listOf(
            "Session" to "%02d:%02d".format(secs / 60 % 60, secs % 60),
            "FPS target" to "60",
            "Quality" to "Performance",
        )
        var x = left + m.px(34f)
        stats.forEach { (k, v) ->
            ui.line(c, v, x, top + m.px(220f), m.textPx(30f), 0xFFFFFFFF.toInt(), Ui.Weight.Bold)
            ui.line(c, k, x, top + m.px(246f), m.textPx(17f),
                Ui.a(Tok.TextSecondary, 0.65f), Ui.Weight.Regular)
            x += m.px(180f)
        }

        // exit button
        val bw = m.px(220f)
        val bh = m.px(64f)
        val bx = left
        val by = top + ph + m.px(28f)
        ui.rect(c, bx, by, bx + bw, by + bh, 0x2EFFFFFF.toInt(), bh / 2f)
        ui.rectOutline(c, bx, by, bx + bw, by + bh, 0x40FFFFFF.toInt(), bh / 2f, 2f)
        ui.line(c, "Close simulation", bx + bw / 2f, by + bh / 2f + m.textPx(9f),
            m.textPx(22f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium, align = Paint.Align.CENTER)
        engine.hits.add(bx, by, bx + bw, by + bh, 12001) {
            if (phase == 2) {
                phase = 3
                exitFade = 1f
                engine.sfx.play(Sfx.Cue.Back)
            }
        }
        // double-tap anywhere also exits
        engine.hits.add(0f, 0f, m.widthPx, m.heightPx, 12000) {
            val now = System.currentTimeMillis()
            if (now - lastTap < 380) {
                tapCount++
                if (phase == 2) {
                    phase = 3
                    exitFade = 1f
                    engine.sfx.play(Sfx.Cue.Back)
                }
            }
            lastTap = now
        }
    }

    override fun back(): Boolean {
        if (phase != 3) {
            phase = 3
            exitFade = 1f
            engine.sfx.play(Sfx.Cue.Back)
            return true
        }
        return false
    }

    override fun isAnimating(): Boolean = true
}
