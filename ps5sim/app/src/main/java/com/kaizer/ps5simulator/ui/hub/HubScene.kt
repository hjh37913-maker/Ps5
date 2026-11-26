package com.kaizer.ps5simulator.ui.hub

import android.graphics.Paint
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
import com.kaizer.ps5simulator.ui.components.Cards
import com.kaizer.ps5simulator.ui.components.Chrome
import com.kaizer.ps5simulator.ui.launch.LaunchScene

/**
 * GAME HUB — the per-game page.
 *
 * Reference layout (1920x1080 plane):
 *   artwork          full-bleed, biased to the upper third
 *   back + title     top-left at 52 units
 *   logo lockup      x=120, vertically centred around y=392 (left third)
 *   meta line        28px, "Played …h ago · 21% h played"
 *   Play button      300x74 pill, focus ring by default
 *   info cards       bottom row, 4 cards 400 wide, 150 tall, 22 gaps
 *   activity row     right half: activity cards in a horizontal scroller
 */
class HubScene(private val engine: Engine, private val gameId: String) : Scene {

    override val id: Int = 3103

    private val game: Game? get() = engine.game(gameId)
    private val activities by lazy { game?.let { Catalog.activitiesFor(it) } ?: emptyList() }

    private var enter: Float = 0f
    private var activityScroll = 0f
    private var activityTarget = 0f
    private var focusedIndex = 0

    override fun isAnimating(): Boolean = enter < 1f

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val g = game ?: return
        if (engine.animationsOn) enter = (enter + f.dtMs / Tok.DUR_HUB_OPEN.toFloat()).coerceAtMost(1f)
        else enter = 1f
        val a = Ease.out(enter)

        // artwork with a slow parallax push as the hub opens (console feel)
        ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, g.deep)
        val bmp = Art.get(g.artwork, m.widthPx.toInt(), m.heightPx.toInt())
        val zoom = 1f + (1f - a) * 0.04f
        if (bmp != null) {
            val w = m.widthPx * zoom
            val h = m.heightPx * zoom
            Draw.coverBias(c, bmp, (m.widthPx - w) / 2f, (m.heightPx - h) / 2f - m.px(10f) * (1 - a),
                (m.widthPx + w) / 2f, (m.heightPx + h) / 2f, 0.5f, 0.30f)
        } else {
            ui.gradientRect(c, 0f, 0f, m.widthPx, m.heightPx, g.accent, g.deep)
        }
        // graded scrims: top chrome + bottom info deck
        ui.scrim(c, 0f, 0f, m.widthPx, m.px(300f), 0x99000000.toInt(), fromTop = true)
        ui.scrim(c, 0f, m.heightPx * 0.35f, m.widthPx, m.heightPx, 0x99000000.toInt(), fromTop = false)
        // left plate behind the lockup (console keeps art but plates the text with a soft shadow)
        ui.glow(c, m.px(300f), m.px(500f), m.px(560f), 0x40000000.toInt())

        chromeTop(f, g)

        ui.withAlpha(a) {
            // logo lockup
            val left = engine.gutter
            val logoH = m.px(158f)
            val logoW = m.px(Ref.HUB_LOGO_MAX_W)
            val ly = m.px(Ref.HUB_LOGO_Y) - logoH / 2f
            val hero = Art.get(g.heroArt, 640, 360)
            if (hero != null) {
                ui.clip(c, left, ly, left + logoW, ly + logoH) {
                    Draw.inside(c, hero, left, ly, left + logoW, ly + logoH, 0.96f)
                }
            } else {
                ui.lineFit(c, g.title, left, ly + logoH * 0.7f, logoW, m.textPx(56f),
                    0xFFFFFFFF.toInt(), Ui.Weight.Display)
            }
            // meta
            val metaY = ly + logoH + m.px(28f)
            val plays = engine.sim.launchCount(g.id)
            val pct = (activities.firstOrNull { it.category == "Story" }?.progress ?: 20)
            val meta = if (plays == 0) "New · ${g.genre} · ${g.publisher}"
            else "Played ${plays * 2 + 1} h ago · $pct% h played · ${g.genre}"
            ui.lineFit(c, meta, left, metaY, m.widthPx * 0.5f, m.textPx(Ref.HUB_META_SIZE),
                Ui.a(Tok.TextPrimary, 0.9f), Ui.Weight.Medium)

            // Play pill
            val btnY = metaY + m.px(20f)
            val bw = m.px(Ref.HUB_PLAY_W)
            val bh = m.px(Ref.HUB_PLAY_H)
            ui.rect(c, left, btnY, left + bw, btnY + bh, 0xF2FFFFFF.toInt(), bh / 2f)
            val lblSize = m.textPx(28f)
            val tri = lblSize * 0.5f
            val textW = ui.measure("Play", lblSize, Ui.Weight.Bold)
            val groupW = tri * 2.2f + lblSize * 0.5f + textW
            val gx = left + bw / 2f - groupW / 2f
            val cy = btnY + bh / 2f
            Icons.draw(c, Glyph.Play, gx + tri, cy, tri * 2.2f, 0xFF0B0B10.toInt(), 0f)
            ui.line(c, engine.s("play"), gx + tri * 2.2f + lblSize * 0.5f, cy + lblSize * 0.36f, lblSize,
                0xFF0B0B10.toInt(), Ui.Weight.Bold)
            Chrome.focusRing(f, left, btnY, left + bw, btnY + bh, bh / 2f)
            engine.hits.add(left, btnY, left + bw, btnY + bh, 10001) { launch(g) }

            // secondary actions row (installed state, favourite, art notice)
            val secY = btnY + bh + m.px(20f)
            drawSecondaryActions(f, g, left, secY)
        }

        // gallery affordance (screenshots + video), top-right of the artwork
        ui.withAlpha(Ease.soft(a)) {
            val chipW = m.px(232f)
            val chipH = m.px(52f)
            val cx = m.safeRight - engine.gutter - chipW
            val cy = m.px(26f)
            ui.rect(c, cx, cy, cx + chipW, cy + chipH, 0x66000000, chipH / 2f)
            ui.rectOutline(c, cx, cy, cx + chipW, cy + chipH, 0x33FFFFFF, chipH / 2f, 1.2f)
            Icons.draw(c, Glyph.Camera, cx + m.px(26f), cy + chipH / 2f, m.px(20f),
                0xE6FFFFFF.toInt(), m.px(1.6f))
            ui.line(c, "Screenshots & media", cx + m.px(46f), cy + chipH / 2f + m.textPx(8f),
                m.textPx(19f), 0xE6FFFFFF.toInt(), Ui.Weight.Medium)
            engine.hits.add(cx, cy, cx + chipW, cy + chipH, 10020) {
                engine.push(GalleryScene(engine, g.id), Sfx.Cue.Select)
            }
        }

        // info cards
        drawInfoCards(f, g, a)

        // activity cards scroller on the right (only when the screen is wide enough)
        if (m.widthPx > m.px(1500f)) drawActivities(f, a)
    }

    private fun chromeTop(f: Frame, g: Game) {
        val m = engine.metrics
        val ui = f.ui
        val left = engine.gutter
        val cy = m.px(52f)
        val size = m.px(30f)
        Icons.draw(f.canvas, Glyph.ArrowBack, left + size * 0.5f, cy, size,
            Ui.a(Tok.TextPrimary, 0.95f), m.px(2.2f))
        ui.line(f.canvas, g.title, left + size * 1.5f, cy + m.textPx(11f), m.textPx(30f),
            Ui.a(Tok.TextPrimary, 0.95f), Ui.Weight.Medium)
        f.engine.hits.add(left - m.px(20f), cy - m.px(28f), left + m.px(400f), cy + m.px(28f),
            10000) { engine.pop() }
        if (engine.settings.showClock) {
            val clock = engine.clockText()
            val cs = m.textPx(30f)
            val cw = ui.measure(clock, cs, Ui.Weight.Medium)
            ui.line(f.canvas, clock, m.safeRight - engine.gutter - cw, cy + m.textPx(11f), cs,
                Ui.a(Tok.TextPrimary, 0.92f), Ui.Weight.Medium)
        }
    }

    private fun drawSecondaryActions(f: Frame, g: Game, left: Float, y: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val installed = engine.sim.installedIds.contains(g.id)
        val fav = engine.sim.favoriteIds.contains(g.id)
        var x = left
        // install / installed chip
        val s = m.textPx(19f)
        val label = if (installed) "Installed" else "Not installed"
        val w = ui.measure(label, s, Ui.Weight.Medium) + m.px(52f)
        val h = m.px(40f)
        ui.rect(c, x, y, x + w, y + h, 0x2EFFFFFF.toInt(), h / 2f)
        Icons.draw(c, if (installed) Glyph.Check else Glyph.Install, x + m.px(18f), y + h / 2f,
            m.px(18f), 0xE6FFFFFF.toInt(), m.px(1.6f))
        ui.line(c, label, x + m.px(32f), y + h / 2f + s * 0.36f, s, 0xE6FFFFFF.toInt(),
            Ui.Weight.Medium)
        f.engine.hits.add(x, y, x + w, y + h, 10010) {
            engine.sim.toggleInstalled(g.id)
            engine.sfx.play(Sfx.Cue.Select)
            engine.toast(if (engine.sim.installedIds.contains(g.id)) "Installed (simulator)" else "Removed (simulator)")
        }
        x += w + m.px(12f)
        // favourite
        val fw = m.px(40f) + ui.measure("Favourite", s, Ui.Weight.Medium)
        ui.rect(c, x, y, x + m.px(52f) + ui.measure("Favourite", s, Ui.Weight.Medium), y + h,
            0x2EFFFFFF.toInt(), h / 2f)
        Icons.draw(c, Glyph.Star, x + m.px(20f), y + h / 2f, m.px(18f),
            if (fav) 0xFFFFD166.toInt() else 0xE6FFFFFF.toInt(), 0f)
        ui.line(c, "Favourite", x + m.px(34f), y + h / 2f + s * 0.36f, s, 0xE6FFFFFF.toInt(),
            Ui.Weight.Medium)
        f.engine.hits.add(x, y, x + m.px(52f) + ui.measure("Favourite", s, Ui.Weight.Medium),
            y + h, 10011) {
            val added = engine.sim.toggleFavorite(g.id)
            engine.sfx.play(Sfx.Cue.Select)
            engine.toast(if (added) "Added to favourites" else "Removed from favourites")
        }
        x += m.px(64f) + ui.measure("Favourite", s, Ui.Weight.Medium)
        // art notice
        ui.line(c, engine.s("art_notice"), x, y + h / 2f + m.textPx(8f),
            m.textPx(16f), Ui.a(Tok.TextTertiary, 0.5f), Ui.Weight.Medium)
    }

    private fun drawInfoCards(f: Frame, g: Game, a: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val left = engine.gutter
        val cw = m.px(Ref.HUB_INFO_CARD_W) * 0.92f
        val ch = m.px(Ref.HUB_INFO_CARD_H) * 1.34f
        val gap = m.px(Ref.HUB_INFO_GAP)
        val y = m.heightPx - m.px(Ref.HUB_INFO_BOTTOM) - ch
        val stats = listOf(
            Triple("Time to beat", Glyph.Clock, listOf("20" to "h · Story", "33" to "h · Extras")),
            Triple("Your time", Glyph.Activity, listOf("${engine.sim.playMinutes(g.id) / 60}" to "h · played", "${engine.sim.launchCount(g.id)}" to "sessions")),
            Triple("Reviews", Glyph.Star, listOf("95" to "% positive", "12k" to "ratings")),
            Triple("Achievements", Glyph.Trophy, listOf("20" to "of 37", "${g.sizeGb.toInt()}" to "GB install")),
        )
        ui.withAlpha(Ease.soft(a)) {
            stats.forEachIndexed { i, (title, glyph, lines) ->
                val x = left + i * (cw + gap)
                if (x + cw > m.safeRight) return@forEachIndexed
                ui.rect(c, x, y, x + cw, y + ch, 0x8C0E1014.toInt(), m.px(16f))
                ui.rectOutline(c, x, y, x + cw, y + ch, 0x1AFFFFFF.toInt(), m.px(16f), 1.2f)
                Icons.draw(c, glyph, x + m.px(18f), y + m.px(19f), m.px(15f),
                    Ui.a(Tok.TextSecondary, 0.9f), m.px(1.4f))
                ui.line(c, title, x + m.px(34f), y + m.px(24f), m.textPx(15f),
                    Ui.a(Tok.TextSecondary, 0.9f), Ui.Weight.Medium)
                var vx = x + m.px(18f)
                lines.forEach { (v, lbl) ->
                    val vs = m.textPx(24f)
                    val vw = ui.line(c, v, vx, y + m.px(58f), vs, 0xFFFFFFFF.toInt(), Ui.Weight.Display)
                    ui.line(c, lbl, vx, y + m.px(74f), m.textPx(13f),
                        Ui.a(Tok.TextSecondary, 0.65f), Ui.Weight.Regular)
                    vx += vw + m.px(18f)
                }
                ui.rect(c, x + m.px(22f), y + ch - m.px(30f), x + cw - m.px(22f),
                    y + ch - m.px(27f), 0x22FFFFFF.toInt(), 1.5f)
                val p = (activities.firstOrNull()?.progress ?: 30).coerceIn(0, 100) / 100f
                ui.rect(c, x + m.px(22f), y + ch - m.px(30f),
                    x + m.px(22f) + (cw - m.px(44f)) * p, y + ch - m.px(27f),
                    0xFF4D8DF6.toInt(), 1.5f)
                ui.line(c, g.genre, x + m.px(22f), y + ch - m.px(10f), m.textPx(16f),
                    Ui.a(Tok.TextSecondary, 0.6f), Ui.Weight.Regular)
            }
        }
    }

    private fun drawActivities(f: Frame, a: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val cardW = m.px(258f)
        val cardH = m.px(378f)
        val gap = m.px(14f)
        val right = m.safeRight - engine.gutter
        val left = right - cardW * 2.6f
        val y = m.heightPx - m.px(Ref.HUB_INFO_BOTTOM) - m.px(Ref.HUB_INFO_CARD_H) * 1.34f
            - m.px(26f) - cardH
        if (y < m.px(300f)) return
        ui.withAlpha(Ease.soft(a) * 0.98f) {
            ui.clip(c, left - m.px(10f), y - m.px(10f), right + m.px(10f), y + cardH + m.px(10f)) {
                val g2 = game
                if (g2 != null) {
                    activities.forEachIndexed { i, act ->
                        val x = left + i * (cardW + gap) - activityScroll
                        if (x + cardW < left || x > right) return@forEachIndexed
                        Cards.activityCard(f, act, g2, x, y, cardW, cardH, false) {
                            launch(g2)
                        }
                    }
                }
            }
        }
    }

    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        val m = engine.metrics
        if (m.widthPx > m.px(1500f)) {
            val max = activities.size * (m.px(300f) + m.px(16f)) - m.px(300f) * 2.6f
            activityScroll = (activityScroll - dx).coerceIn(0f, max.coerceAtLeast(0f))
            activityTarget = activityScroll
            return true
        }
        return false
    }

    private fun launch(g: Game) {
        engine.sim.recordLaunch(g.id)
        engine.openLaunch(g)
    }
}
