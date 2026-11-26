package com.kaizer.ps5simulator.ui.trophies

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
import com.kaizer.ps5simulator.data.catalog.Catalog
import com.kaizer.ps5simulator.ui.components.Chrome

/**
 * TROPHIES — the console trophy list.
 *
 * Reference: huge "Trophies" title top-left, level star + total in the header, then a
 * dense list: cover thumb, title, platform tag, completion %, and gold/silver/bronze
 * counts, with the focused row framed by a thin white outline.
 */
class TrophiesScene(private val engine: Engine) : Scene {
    override val id: Int = 3108

    private var reveal = 0f
    private var focused = 0
    private var scroll = 0f
    private var target = 0f
    private var dragStart = 0f
    private var scrollStart = 0f

    override fun onShow() { reveal = 0f }

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (engine.animationsOn) reveal = (reveal + f.dtMs / 260f).coerceAtMost(1f) else reveal = 1f
        val a = Ease.soft(reveal)
        val list = Catalog.trophies

        // reference background: warm grey vignette (trophy screens use a light-diffused backdrop)
        ui.gradientRect(c, 0f, 0f, m.widthPx, m.heightPx, 0xFF3A3A3C.toInt(),
            0xFF141416.toInt())
        ui.glow(c, m.widthPx * 0.18f, m.px(60f), m.widthPx * 0.5f, 0x40FFFFFF.toInt())
        ui.scrim(c, 0f, 0f, m.widthPx, m.heightPx, 0x33000000.toInt(), fromTop = true)

        Chrome.backRow(f, "")

        ui.withAlpha(a) {
            val left = engine.gutter
            ui.line(c, "Trophies", left, m.px(140f), m.textPx(58f), 0xFFFFFFFF.toInt(),
                Ui.Weight.Regular)
            // header stats
            val right = m.safeRight - engine.gutter
            val trophyY = m.px(108f)
            val total = Catalog.trophies.sumOf { it.gold + it.silver + it.bronze } +
                Catalog.profile.totalTrophies
            var x = right - m.px(520f)
            Icons.draw(c, Glyph.Trophy, x, trophyY, m.px(34f), Tok.TrophyGold, m.px(2f))
            ui.line(c, total.toString(), x + m.px(30f), trophyY + m.textPx(14f), m.textPx(44f),
                0xFFFFFFFF.toInt(), Ui.Weight.Medium)
            ui.line(c, "Total", x + m.px(30f), trophyY + m.px(42f), m.textPx(17f),
                Ui.a(Tok.TextSecondary, 0.8f), Ui.Weight.Regular)
            x += m.px(190f)
            val counts = listOf(
                Tok.TrophyPlatinum to Catalog.profile.platinum,
                Tok.TrophyGold to Catalog.profile.gold,
                Tok.TrophySilver to Catalog.profile.silver,
                Tok.TrophyBronze to Catalog.profile.bronze,
            )
            counts.forEach { (col, n) ->
                Icons.draw(c, Glyph.Trophy, x, trophyY, m.px(30f), col, m.px(1.8f))
                ui.line(c, n.toString(), x + m.px(24f), trophyY + m.textPx(10f),
                    m.textPx(26f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
                x += m.px(130f)
            }

            // rows
            val rowTop = m.px(196f)
            val rowH = m.px(96f)
            val maxScroll = (list.size * rowH - (m.heightPx - rowTop - m.px(40f))).coerceAtLeast(0f)
            target = target.coerceIn(0f, maxScroll)
            scroll += (target - scroll) * (1f - kotlin.math.exp(-16f * f.dtMs / 1000f))

            ui.clip(c, left - m.px(20f), rowTop - m.px(8f), right + m.px(20f), m.heightPx) {
                list.forEachIndexed { i, t ->
                    val y = rowTop + i * rowH - scroll
                    if (y + rowH < rowTop - m.px(20f) || y > m.heightPx) return@forEachIndexed
                    val isFocused = i == focused
                    if (isFocused) {
                        ui.rect(c, left - m.px(18f), y, right + m.px(18f), y + rowH - m.px(6f),
                            0x1AFFFFFF.toInt(), m.px(6f))
                        ui.rectOutline(c, left - m.px(18f), y, right + m.px(18f),
                            y + rowH - m.px(6f), 0xCCFFFFFF.toInt(), m.px(6f), 1.6f)
                    }
                    // cover
                    val g = engine.game(t.gameId)
                    val thumb = m.px(64f)
                    val tx = left
                    val ty = y + m.px(12f)
                    ui.rect(c, tx, ty, tx + thumb, ty + thumb, 0xFF0C0C10.toInt(), m.px(3f))
                    if (g != null) {
                        val bmp = Art.get(g.coverArt, thumb.toInt(), thumb.toInt())
                        if (bmp != null) ui.clip(c, tx, ty, tx + thumb, ty + thumb) {
                            Draw.cover(c, bmp, tx, ty, tx + thumb, ty + thumb)
                        }
                    }
                    ui.lineFit(c, t.gameTitle, tx + thumb + m.px(20f), ty + m.px(24f),
                        m.widthPx * 0.28f, m.textPx(23f), 0xFFFFFFFF.toInt(), Ui.Weight.Regular)
                    // platform tag
                    val ps = m.textPx(14f)
                    val pw = ui.measure(t.platform, ps, Ui.Weight.Medium) + m.px(12f)
                    ui.rectOutline(c, tx + thumb + m.px(20f), ty + m.px(34f),
                        tx + thumb + m.px(20f) + pw, ty + m.px(54f), 0x88FFFFFF.toInt(), m.px(2f), 1f)
                    ui.line(c, t.platform, tx + thumb + m.px(26f), ty + m.px(48f), ps,
                        0xFFFFFFFF.toInt(), Ui.Weight.Medium)
                    // completion with the underline the console uses instead of a bar
                    val pctText = "${t.progressPercent}%"
                    val pctX = left + m.widthPx * 0.42f
                    ui.line(c, pctText, pctX, ty + m.px(40f), m.textPx(34f),
                        0xFFFFFFFF.toInt(), Ui.Weight.Regular)
                    val uw = m.px(56f)
                    ui.rect(c, pctX, ty + m.px(52f), pctX + uw, ty + m.px(55f), 0x44FFFFFF.toInt(), 1.5f)
                    ui.rect(c, pctX, ty + m.px(52f), pctX + uw * (t.progressPercent / 100f),
                        ty + m.px(55f), 0xFFFFFFFF.toInt(), 1.5f)
                    // trophy counts
                    var cx = left + m.widthPx * 0.58f
                    listOf(Tok.TrophyGold to t.gold, Tok.TrophySilver to t.silver,
                        Tok.TrophyBronze to t.bronze).forEach { (col, n) ->
                        Icons.draw(c, Glyph.Trophy, cx, ty + m.px(26f), m.px(24f), col, m.px(1.6f))
                        ui.line(c, n.toString(), cx + m.px(20f), ty + m.px(34f), m.textPx(21f),
                            0xFFFFFFFF.toInt(), Ui.Weight.Regular)
                        cx += m.px(96f)
                    }
                    Chrome.divider(f, left, right, y + rowH - m.px(8f), 0.08f)
                    engine.hits.add(left, y, right, y + rowH, 17000 + i) {
                        focused = i
                        engine.sfx.play(com.kaizer.ps5simulator.core.engine.Sfx.Cue.NavAlt)
                    }
                }
            }
        }
    }

    override fun onDown(x: Float, y: Float): Boolean {
        dragStart = y
        scrollStart = target
        return false
    }

    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (kotlin.math.abs(dy) > 2f) {
            target = (scrollStart - (y - dragStart)).coerceAtLeast(0f)
            return true
        }
        return false
    }
}
