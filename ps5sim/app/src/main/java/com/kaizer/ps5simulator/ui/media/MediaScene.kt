package com.kaizer.ps5simulator.ui.media

import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Ref
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.design.Ease
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
 * MEDIA — the console's media tab, rebuilt with the same chrome as Games but for video
 * and music. Content is local demo material (game key art + original audio).
 */
class MediaScene(private val engine: Engine) : Scene {
    override val id: Int = 3111

    private data class MediaItem(val title: String, val kind: String, val gameId: String, val dur: String)

    private val items by lazy {
        engine.games.take(8).mapIndexed { i, g ->
            MediaItem(
                when (i % 3) {
                    0 -> "${g.title} — Launch Trailer"
                    1 -> "${g.title} — Behind the Scenes"
                    else -> "${g.title} — Gameplay Reveal"
                },
                listOf("Video", "Music", "Video")[i % 3], g.id,
                listOf("2:14", "3:48", "1:56", "4:02", "2:37", "3:11", "5:04", "2:22")[i],
            )
        }
    }

    private var reveal = 0f
    private var scroll = 0f
    private var targetScroll = 0f
    private var dragging = false
    private var dragStartY = 0f
    private var scrollStart = 0f

    override fun isAnimating(): Boolean = reveal < 1f || dragging

    override fun onShow() { reveal = 0f }

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (engine.animationsOn) reveal = (reveal + f.dtMs / 260f).coerceAtMost(1f) else reveal = 1f
        val a = Ease.soft(reveal)

        ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, 0xFF07070B.toInt())
        // featured hero
        val hero = engine.games.firstOrNull()
        if (hero != null) {
            val bmp = Art.get(hero.artwork, m.widthPx.toInt(), m.heightPx.toInt())
            if (bmp != null) {
                ui.withAlpha(a * 0.9f) {
                    Draw.coverBias(c, bmp, 0f, 0f, m.widthPx, m.heightPx * 0.74f, 0.5f, 0.3f)
                }
            }
        }
        ui.scrim(c, 0f, 0f, m.widthPx, m.heightPx * 0.8f, 0xCC000000.toInt(), fromTop = true)
        ui.scrim(c, 0f, m.heightPx * 0.45f, m.widthPx, m.heightPx, 0xEB000000.toInt(), fromTop = false)

        Chrome.topBar(f, 1, listOf(engine.s("games"), engine.s("media")), onTab = { i -> if (i == 0) engine.popToRoot() },
            showClock = engine.settings.showClock)

        val left = engine.gutter
        ui.withAlpha(a) {
            ui.line(c, engine.s("media"), left, m.px(206f), m.textPx(58f), 0xFFFFFFFF.toInt(), Ui.Weight.Display)
            ui.line(c, engine.s("media_sub"), left, m.px(250f), m.textPx(24f),
                Ui.a(Tok.TextSecondary, 0.85f), Ui.Weight.Medium)
        }

        // rows
        val rowTop = m.px(300f)
        val cardW = m.px(330f)
        val cardH = m.px(330f)
        val gap = m.px(20f)
        val max = items.size * (cardW + gap) - (m.safeRight - engine.gutter)
        targetScroll = targetScroll.coerceIn(0f, max.coerceAtLeast(0f))
        if (!dragging) scroll += (targetScroll - scroll) * (1f - kotlin.math.exp(-14f * f.dtMs / 1000f))

        ui.withAlpha(a) {
            ui.clip(c, left, rowTop, m.safeRight, rowTop + cardH + m.px(70f)) {
                items.forEachIndexed { i, item ->
                    val x = left + i * (cardW + gap) - scroll
                    if (x + cardW < left - gap || x > m.safeRight) return@forEachIndexed
                    ui.rect(c, x, rowTop, x + cardW, rowTop + cardH, 0xFF0C0C11.toInt(), m.px(14f))
                    val g = engine.game(item.gameId)
                    val bmp = g?.let { Art.get(it.artwork, cardW.toInt(), cardH.toInt()) }
                    if (bmp != null) ui.clip(c, x, rowTop, x + cardW, rowTop + cardH) {
                        Draw.cover(c, bmp, x, rowTop, x + cardW, rowTop + cardH)
                    }
                    ui.rect(c, x, rowTop, x + cardW, rowTop + cardH, 0x33000000.toInt())
                    // play disc
                    ui.circle(c, x + cardW / 2f, rowTop + cardH / 2f, m.px(34f), 0xCC000000.toInt())
                    Icons.draw(c, if (item.kind == "Music") Glyph.Music else Glyph.Play,
                        x + cardW / 2f, rowTop + cardH / 2f, m.px(28f), 0xFFFFFFFF.toInt(), 0f)
                    // duration
                    val ds = m.textPx(17f)
                    val dw = ui.measure(item.dur, ds, Ui.Weight.Medium)
                    ui.rect(c, x + cardW - dw - m.px(24f), rowTop + cardH - m.px(40f),
                        x + cardW - m.px(10f), rowTop + cardH - m.px(10f), 0xCC000000.toInt(),
                        m.px(6f))
                    ui.line(c, item.dur, x + cardW - dw - m.px(17f), rowTop + cardH - m.px(18f),
                        ds, 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
                    ui.lineFit(c, item.title, x, rowTop + cardH + m.px(30f), cardW,
                        m.textPx(23f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
                    ui.line(c, item.kind, x, rowTop + cardH + m.px(54f), m.textPx(18f),
                        Ui.a(Tok.TextSecondary, 0.7f), Ui.Weight.Regular)
                    engine.hits.add(x, rowTop, x + cardW, rowTop + cardH, 13000 + i) {
                        engine.toast("Opening ${item.kind.lowercase()} — simulator demo")
                    }
                }
            }
        }

        if (engine.settings.showHints) {
            ui.line(c, "Swipe to browse · ${items.size} items", m.widthPx / 2f,
                m.heightPx - m.px(30f), m.textPx(20f), Ui.a(Tok.TextSecondary, 0.5f),
                Ui.Weight.Medium, align = Paint.Align.CENTER)
        }
    }

    override fun onDown(x: Float, y: Float): Boolean {
        dragging = false
        dragStartY = y
        scrollStart = targetScroll
        return false
    }

    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (kotlin.math.abs(dx) > 1.5f) {
            dragging = true
            targetScroll = (scroll - dx).coerceAtLeast(0f)
            scroll = targetScroll
            return true
        }
        return false
    }

    override fun onUp(x: Float, y: Float): Boolean {
        dragging = false
        return false
    }
}
