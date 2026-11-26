package com.kaizer.ps5simulator.ui.home

import android.graphics.Canvas
import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Metrics
import com.kaizer.ps5simulator.core.design.Ref
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.design.Ease
import com.kaizer.ps5simulator.core.engine.Engine
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Glyph
import com.kaizer.ps5simulator.core.engine.Icons
import com.kaizer.ps5simulator.core.engine.Scene
import com.kaizer.ps5simulator.core.engine.Sfx
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.core.gfx.Art
import com.kaizer.ps5simulator.core.gfx.Draw
import com.kaizer.ps5simulator.data.catalog.Catalog
import com.kaizer.ps5simulator.data.model.Game
import com.kaizer.ps5simulator.ui.components.Chrome
import com.kaizer.ps5simulator.ui.components.SwitcherStrip
import kotlin.math.abs
import kotlin.math.min

/**
 * HOME — the console dashboard.
 *
 * Layer order mirrors the reference capture:
 *   1. spotlight artwork (full-bleed, centre-cropped, darkened top & bottom)
 *   2. top navigation bar
 *   3. switcher strip
 *   4. game logo lockup (bottom-left of the artwork)
 *   5. "Played …" metadata row
 *   6. Play button + more-options button
 *   7. "Must see" carousel (5:3 art cards)
 *   8. Control Center trigger hint at the bottom edge
 *
 * Artwork cross-fades when the selection changes; the switcher row keeps its own
 * scroll state and the logo/meta block slides in by 24 units with the fade — both
 * measured from console capture frame analysis.
 */
class HomeScene(private val engine: Engine, gameId: String, startTab: Int = 0) : Scene {

    override val id: Int = 3101

    private enum class Tab { Games, Media }

    private var tab: Tab = if (startTab == 1) Tab.Media else Tab.Games
    private val strip = SwitcherStrip(engine)

    private var selectedId: String = gameId
    private var previousId: String = gameId
    private var bgFade: Float = 1f
    private var metaSlide: Float = 1f
    private var mustSeeScroll: Float = 0f
    private var mustSeeTarget: Float = 0f

    private var mustSee: List<Game> = emptyList()
    private var gameTiles: List<Game> = emptyList()
    private var mediaTiles: List<Game> = emptyList()

    private var initialized = false
    private var reveal: Float = 0f

    override fun isAnimating(): Boolean =
        reveal < 1f || bgFade < 1f || metaSlide < 1f ||
            kotlin.math.abs(mustSeeTarget - mustSeeScroll) > 0.5f

    override fun onShow() {
        if (!initialized) {
            initialized = true
            reload()
            reveal = if (engine.animationsOn) 0f else 1f
        }
        engine.sim.homeTab = if (tab == Tab.Media) 1 else 0
    }

    private fun reload() {
        gameTiles = engine.games
        mediaTiles = engine.games.sortedBy { it.index + 7 }.take(10)
        strip.setGames(if (tab == Tab.Games) gameTiles else mediaTiles)
        if (selectedId.isEmpty()) selectedId = gameTiles.firstOrNull()?.id ?: ""
        strip.selected = strip.indexOf(selectedId).coerceIn(0, (strip.tileCount() - 1).coerceAtLeast(0))
        strip.selectedGame()?.let { selectedId = it.id }
        mustSee = Catalog.mustSee(engine.games)
        engine.sim.selectedGameId = selectedId
    }

    private fun selectedGame(): Game? = engine.game(selectedId)

    /** Harness hook: focus a switcher index without simulating a touch. */
    fun selectGameByIndex(index: Int) {
        if (index in 0 until strip.tileCount()) {
            strip.select(index)
            onSelectionChanged()
        }
    }

    // ------------------------------------------------------------------ input
    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        val m = engine.metrics
        val stripTop = m.px(Ref.SWITCHER_TOP)
        val stripBottom = stripTop + m.px(Ref.SWITCHER_SIZE) * 1.3f
        if (y in stripTop..stripBottom) {
            strip.dragging = true
            strip.scrollBy(dx)
            return true
        }
        val cardsTop = mustSeeTop()
        if (y > cardsTop - m.px(20f)) {
            val max = mustSeeContentWidth() - (m.safeRight - engine.gutter)
            mustSeeTarget = (mustSeeTarget - dx).coerceIn(0f, max.coerceAtLeast(0f))
            return true
        }
        // horizontal swipe anywhere on the spotlight changes the selected game
        if (abs(dx) > abs(dy) && abs(dx) > m.px(6f)) {
            dragAccum += dx
            if (abs(dragAccum) > m.px(84f)) {
                val next = strip.selected + if (dragAccum < 0) 1 else -1
                if (next in 0 until strip.tileCount()) {
                    strip.select(next)
                    onSelectionChanged()
                }
                dragAccum = 0f
            }
            return true
        }
        return false
    }

    private var dragAccum = 0f

    override fun onDown(x: Float, y: Float): Boolean {
        dragAccum = 0f
        return false
    }

    override fun onUp(x: Float, y: Float): Boolean {
        if (strip.dragging) {
            strip.dragging = false
            strip.fling()
            return true
        }
        return false
    }

    private fun onSelectionChanged() {
        val g = strip.selectedGame()
        if (g != null && g.id != selectedId) {
            previousId = selectedId
            selectedId = g.id
            bgFade = 0f
            metaSlide = 0f
            engine.sim.selectedGameId = g.id
            engine.sfx.play(Sfx.Cue.Nav)
        }
    }

    override fun back(): Boolean = tab == Tab.Media && run {
        tab = Tab.Games
        reload()
        true
    }

    // ------------------------------------------------------------------ layout helpers
    private fun mustSeeTop(): Float {
        val m = engine.metrics
        return m.heightPx - m.px(Ref.MUSTSEE_CARD_H) - m.px(Ref.MUSTSEE_BOTTOM)
    }

    private fun mustSeeContentWidth(): Float {
        val m = engine.metrics
        return mustSee.size * (m.px(Ref.MUSTSEE_CARD_W) + m.px(Ref.MUSTSEE_GAP))
    }

    // ------------------------------------------------------------------ draw
    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (f.engine.animationsOn && reveal < 1f) {
            reveal = (reveal + f.dtMs / 420f).coerceAtMost(1f)
        } else if (!f.engine.animationsOn) {
            reveal = 1f
        }
        if (bgFade < 1f) bgFade = (bgFade + f.dtMs / Tok.DUR_SPOTLIGHT_CROSSFADE.toFloat()).coerceAtMost(1f)
        if (metaSlide < 1f) metaSlide = (metaSlide + f.dtMs / 260f).coerceAtMost(1f)
        strip.update(f.dtMs)
        if (abs(mustSeeTarget - mustSeeScroll) > 0.5f) {
            mustSeeScroll += (mustSeeTarget - mustSeeScroll) * (1f - kotlin.math.exp(-12f * f.dtMs / 1000f))
        }

        val fade = Ease.soft(reveal)

        // 1 — spotlight artwork ---------------------------------------------------
        val prev = engine.game(previousId)
        val cur = selectedGame()
        if (tab == Tab.Games && prev != null && bgFade < 1f) {
            drawBackdrop(f, prev, 1f)
            drawBackdrop(f, cur, bgFade)
        } else {
            drawBackdrop(f, if (tab == Tab.Games) cur else mediaArt(), fade)
        }
        // overall dark grade so chrome reads on any artwork
        ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, 0x4D000000.toInt())

        // 2 — top bar --------------------------------------------------------------
        // gradient scrim behind the top bar (console art stays visible at the very top)
        ui.scrim(c, 0f, 0f, m.widthPx, m.px(300f), 0x99000000.toInt(), fromTop = true)
        val titles = listOf("Games", "Media")
        val activeTab = if (tab == Tab.Games) 0 else 1
        val onTab: (Int) -> Unit = { i ->
            val next = if (i == 0) Tab.Games else Tab.Media
            if (next != tab) {
                tab = next
                engine.sfx.play(Sfx.Cue.Select)
                reload()
                mustSeeScroll = 0f
                mustSeeTarget = 0f
            }
        }
        Chrome.topBar(f, activeTab, titles, onTab = onTab, showClock = engine.settings.showClock)

        if (engine.settings.showClock) {
            val avatarX = m.safeRight - engine.gutter - m.px(238f)
            engine.hits.add(avatarX - m.px(30f), m.px(10f), avatarX + m.px(30f), m.px(80f), 9250) {
                engine.openProfile()
            }
        }

        // 3 — switcher strip ------------------------------------------------------
        val stripTop = m.px(Ref.SWITCHER_TOP)
        strip.draw(f, stripTop, engine.gutter - m.px(24f), m.safeRight - m.px(24f))
        onSelectionChanged()

        // 4..6 — spotlight text block --------------------------------------------
        if (tab == Tab.Games) {
            drawSpotlightText(f, cur, fade * (1f - bgFade * 0f))
        } else {
            drawMediaText(f, mediaArt(), fade)
        }

        // 7 — must see carousel ----------------------------------------------------
        drawMustSee(f, fade)


        // reveal curtain (home opens with a fade-through-black on console)
        if (reveal < 1f) {
            ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, ((1f - fade) * 255).toInt() shl 24, 0f)
        }
    }

    private fun mediaArt(): Game? =
        mediaTiles.getOrNull((strip.selected - 1).coerceAtLeast(0) % mediaTiles.size.coerceAtLeast(1))

    /**
     * Artwork window. In the console capture the key art occupies the horizontal band
     * between the top chrome and the "Must see" row (y 199…~860 of 1080); above and
     * below the background is graded to near-black. We reproduce that window instead of
     * stretching the art full-bleed, which is what makes the home screen read as PS5.
     */
    private fun drawBackdrop(f: Frame, g: Game?, alpha: Float) {
        val m = engine.metrics
        val c = f.canvas
        val ui = f.ui
        ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, 0xFF04050A.toInt())
        if (g == null) return
        val artTop = m.px(198f)
        val artBottom = m.heightPx - m.px(Ref.MUSTSEE_CARD_H) - m.px(64f)
        val bmp = Art.get(g.artwork, (m.widthPx).toInt(), (artBottom - artTop).toInt().coerceAtLeast(64))
        if (bmp != null) {
            ui.withAlpha(alpha) {
                Draw.coverBias(c, bmp, 0f, artTop, m.widthPx, artBottom, 0.5f, 0.30f)
            }
        } else {
            ui.withAlpha(alpha * 0.9f) {
                ui.gradientRect(c, 0f, artTop, m.widthPx, artBottom, g.accent, g.deep)
                ui.glow(c, m.widthPx * 0.62f, (artTop + artBottom) / 2f, m.widthPx * 0.42f, g.accent)
            }
        }
        // dynamic light drift (disabled by "dynamic background" in settings)
        if (engine.settings.dynamicBackground && engine.animationsOn) {
            val t = com.kaizer.ps5simulator.ui.screens.AmbientBackground.time()
            val dx = kotlin.math.sin(t * 0.18f) * m.px(26f)
            val dy = kotlin.math.cos(t * 0.13f) * m.px(14f)
            ui.withAlpha(0.10f * alpha) {
                ui.glow(c, m.widthPx * 0.7f + dx, m.heightPx * 0.35f + dy, m.widthPx * 0.34f,
                    g.accent)
            }
        }
        ui.withAlpha(alpha) {
            ui.scrim(c, 0f, 0f, m.widthPx, m.px(420f), 0xB3000000.toInt(), fromTop = true)
            ui.scrim(c, 0f, m.heightPx * 0.42f, m.widthPx, m.heightPx, 0xCC000000.toInt(),
                fromTop = false)
        }
    }

    private fun drawSpotlightText(f: Frame, g: Game?, alpha: Float) {
        if (g == null) return
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val left = engine.gutter
        val slide = (1f - Ease.out(metaSlide)) * m.px(26f)
        val a = alpha * Ease.soft(metaSlide)

        ui.withAlpha(a) {
            val bmp = Art.get("file:///android_asset/games/${g.id}/hero.png", 960, 320)
            val logoH = m.px(Ref.SPOTLIGHT_LOGO_MAX_H)
            val logoW = m.px(Ref.SPOTLIGHT_LOGO_MAX_W)
            val logoBottom = m.heightPx - m.px(Ref.SPOTLIGHT_LOGO_BOTTOM)
            val ly = logoBottom - logoH
            if (bmp != null) {
                // lockup is left-aligned: draw at its natural width, fitted into the box
                Draw.inside(c, bmp, left - slide, ly, left + logoW - slide, ly + logoH)
            }
            // meta row (measured ~25 units under the logo block)
            val metaSize = m.textPx(Ref.SPOTLIGHT_META_SIZE)
            val metaY = logoBottom + m.px(32f)
            val plays = engine.sim.launchCount(g.id)
            val meta = if (plays == 0) "New to console · ${g.genre}" else "Played ${plays * 2 + 1} h ago · ${g.genre}"
            ui.line(c, meta, left, metaY, metaSize, Ui.a(Tok.TextPrimary, 0.86f), Ui.Weight.Medium)

            // The 2020 launch interface has no launch button on Home: tapping the
            // artwork or the selected tile opens the Game Hub, where Play lives.
            val hintY = metaY + m.px(40f)
            ui.line(c, "Press  Play  on the tile to open the game hub", left, hintY,
                m.textPx(20f), Ui.a(Tok.TextSecondary, 0.0f), Ui.Weight.Medium)
            // invisible wide tap target over the artwork opens the hub
            engine.hits.add(left, m.px(220f), m.widthPx * 0.72f,
                m.heightPx - m.px(Ref.MUSTSEE_CARD_H + 80f), 9502) { engine.openHub(g.id) }
        }
    }

    private fun drawMediaText(f: Frame, g: Game?, alpha: Float) {
        if (g == null) return
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val left = engine.gutter
        ui.withAlpha(alpha) {
            val size = m.textPx(54f)
            val y = m.heightPx - m.px(Ref.SPOTLIGHT_LOGO_BOTTOM) - m.px(86f)
            ui.lineFit(c, g.title, left, y, m.widthPx * 0.55f, size, 0xFFFFFFFF.toInt(),
                Ui.Weight.Display)
            ui.line(c, "Media  ·  ${g.genre}", left, y + m.px(44f), m.textPx(28f),
                Ui.a(Tok.TextPrimary, 0.8f), Ui.Weight.Medium)
            val btnY = y + m.px(70f)
            val bw = m.px(Ref.PLAY_BUTTON_W)
            val bh = m.px(Ref.PLAY_BUTTON_H)
            ui.rect(c, left, btnY, left + bw, btnY + bh, 0x2EFFFFFF.toInt(), bh / 2f)
            ui.rectOutline(c, left, btnY, left + bw, btnY + bh, 0x40FFFFFF.toInt(), bh / 2f, 2f)
            ui.line(c, "Open", left + bw / 2f, btnY + bh / 2f + m.textPx(10f),
                m.textPx(Ref.PLAY_BUTTON_LABEL), 0xFFFFFFFF.toInt(), Ui.Weight.Bold,
                align = Paint.Align.CENTER)
            engine.hits.add(left, btnY, left + bw, btnY + bh, 9600) {
                engine.toast("Media playback is a simulator demo")
            }
        }
    }

    private fun drawMustSee(f: Frame, alpha: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val left = engine.gutter
        val cardW = m.px(Ref.MUSTSEE_CARD_W)
        val cardH = m.px(Ref.MUSTSEE_CARD_H)
        val gap = m.px(Ref.MUSTSEE_GAP)
        val top = mustSeeTop()
        val labelY = top - m.px(20f)
        ui.withAlpha(alpha) {
            Chrome.sectionLabel(f, "Must see", left, labelY)
            ui.clip(c, left - m.px(10f), top - m.px(8f), m.safeRight, top + cardH + m.px(18f)) {
                mustSee.forEachIndexed { i, g ->
                    val x = left + i * (cardW + gap) - mustSeeScroll
                    if (x + cardW < left - gap || x > m.safeRight + gap) return@forEachIndexed
                    ui.rect(c, x, top, x + cardW, top + cardH, 0xFF0B0B0F.toInt(),
                        m.px(Ref.MUSTSEE_CARD_RADIUS))
                    val bmp = Art.get(g.artwork, cardW.toInt().coerceAtLeast(64),
                        cardH.toInt().coerceAtLeast(64))
                    if (bmp != null) {
                        ui.clip(c, x, top, x + cardW, top + cardH) {
                            Draw.cover(c, bmp, x, top, x + cardW, top + cardH)
                        }
                    } else {
                        ui.gradientRect(c, x, top, x + cardW, top + cardH, g.accent, g.deep,
                            m.px(Ref.MUSTSEE_CARD_RADIUS))
                    }
                    ui.rectOutline(c, x, top, x + cardW, top + cardH, 0x1AFFFFFF.toInt(),
                        m.px(Ref.MUSTSEE_CARD_RADIUS), 1.5f)
                    // title plate
                    ui.scrim(c, x, top + cardH * 0.55f, x + cardW, top + cardH, 0xCC000000.toInt(),
                        fromTop = false)
                    ui.lineFit(c, g.title, x + m.px(14f), top + cardH - m.px(16f), cardW - m.px(28f),
                        m.textPx(22f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
                    engine.hits.add(x, top, x + cardW, top + cardH, 9700 + i) {
                        engine.openHub(g.id)
                    }
                }
            }
        }
    }
}
