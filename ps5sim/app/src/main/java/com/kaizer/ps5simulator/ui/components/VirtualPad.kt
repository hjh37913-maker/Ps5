package com.kaizer.ps5simulator.ui.components

import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.data.model.Game

/**
 * Optional on-screen DualSense control overlay.
 *
 * Deliberately opt-in and drawn *above* the console chrome as a translucent HUD: the
 * reference layout underneath is never modified. Controls map to real simulator actions:
 *   ✕ select / open · ○ back · D-pad left/right switch game · Options = Control Center.
 */
object VirtualPad {

    private const val PAD_ALPHA = 0.30f

    fun draw(f: Frame) {
        val m = f.engine.metrics
        val ui = f.ui
        val c = f.canvas
        val padW = m.widthPx * 0.34f
        val padH = padW * 0.52f
        val l = m.safeRight - padW - m.px(34f)
        val t = m.heightPx - padH - m.px(24f)
        val bx = l + padW * 0.80f
        val by = t + padH * 0.48f
        val br = padH * 0.075f

        ui.withAlpha(PAD_ALPHA) {
            // body: two grips + centre bar, the DualSense silhouette
            ui.rect(c, l, t + padH * 0.10f, l + padW, t + padH * 0.62f, 0xFFEDEFF3.toInt(),
                padH * 0.22f)
            ui.circle(c, l + padW * 0.12f, t + padH * 0.72f, padH * 0.26f, 0xFFEDEFF3.toInt())
            ui.circle(c, l + padW * 0.88f, t + padH * 0.72f, padH * 0.26f,
                0xFFEDEFF3.toInt())
            // touch bar
            ui.rect(c, l + padW * 0.30f, t + padH * 0.18f, l + padW * 0.70f,
                t + padH * 0.28f, 0xFFB9BEC7.toInt(), padH * 0.05f)
            // sticks
            ui.circle(c, l + padW * 0.36f, t + padH * 0.52f, padH * 0.13f,
                0xFF3A3F49.toInt())
            ui.circle(c, l + padW * 0.64f, t + padH * 0.52f, padH * 0.13f,
                0xFF3A3F49.toInt())
            // d-pad
            ui.rect(c, l + padW * 0.175f, t + padH * 0.46f, l + padW * 0.235f,
                t + padH * 0.58f, 0xFF3A3F49.toInt(), padH * 0.02f)
            ui.rect(c, l + padW * 0.145f, t + padH * 0.49f, l + padW * 0.265f,
                t + padH * 0.55f, 0xFF3A3F49.toInt(), padH * 0.02f)
            // face buttons
            ui.circle(c, bx, by - br * 1.8f, br, 0xFF5BC48A.toInt())
            ui.circle(c, bx + br * 1.8f, by, br, 0xFFE06B7B.toInt())
            ui.circle(c, bx - br * 1.8f, by, br, 0xFF7EA6E8.toInt())
            ui.circle(c, bx, by + br * 1.8f, br, 0xFFD9A0D8.toInt())
        }

        // hit targets: D-pad left/right, ✕ select, ○ back, Options
        f.engine.hits.add(l + padW * 0.145f, t + padH * 0.46f, l + padW * 0.185f,
            t + padH * 0.58f, 22001) { f.engine.home?.stepSelection(-1) }
        f.engine.hits.add(l + padW * 0.235f, t + padH * 0.46f, l + padW * 0.275f,
            t + padH * 0.58f, 22002) { f.engine.home?.stepSelection(1) }
        f.engine.hits.add(bx - br * 2f, by - br * 2f, bx + br * 2f, by + br * 2f, 22003) {
            f.engine.openHub(f.engine.selectedGame()?.id ?: "gtavi")
        }
        f.engine.hits.add(l, t, l + padW * 0.05f, t + padH, 22004) { f.engine.back() }
        f.engine.hits.add(l + padW * 0.42f, t + padH * 0.02f, l + padW * 0.58f,
            t + padH * 0.16f, 22005) { f.engine.openControlCenter() }

        // label
        ui.line(c, f.engine.s("virtual_pad"), l + padW / 2f, t - m.px(10f), m.textPx(15f),
            Ui.a(Tok.TextSecondary, 0.55f), Ui.Weight.Medium, align = Paint.Align.CENTER)
    }
}
