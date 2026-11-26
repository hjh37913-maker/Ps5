package com.kaizer.ps5simulator.ui.profile

import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Ease
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.engine.Engine
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Glyph
import com.kaizer.ps5simulator.core.engine.Icons
import com.kaizer.ps5simulator.core.engine.Scene
import com.kaizer.ps5simulator.core.engine.Sfx
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.data.catalog.Catalog
import com.kaizer.ps5simulator.data.model.FriendStatus
import com.kaizer.ps5simulator.ui.components.Chrome
import com.kaizer.ps5simulator.ui.trophies.TrophiesScene

/**
 * PROFILE — console profile page.
 *
 * Reference structure: circular avatar top-left, display name + online ID, trophy level
 * meter, the four trophy counters, then Friends / Recently played / Settings rows.
 * All data is local demo content.
 */
class ProfileScene(private val engine: Engine) : Scene {

    override val id: Int = 3105

    private var reveal = 0f
    private var tab = 0 // 0 overview, 1 friends, 2 activity

    override fun isAnimating(): Boolean = reveal < 1f

    override fun onShow() { reveal = 0f }

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (engine.animationsOn) reveal = (reveal + f.dtMs / 280f).coerceAtMost(1f) else reveal = 1f
        val a = Ease.soft(reveal)

        // background: soft dark gradient with the selected game's accent as a light pool
        ui.gradientRect(c, 0f, 0f, m.widthPx, m.heightPx, 0xFF10131C.toInt(),
            0xFF06070A.toInt())
        val g = engine.selectedGame()
        if (g != null) {
            // a barely-there wash of the selected game's accent, never a spotlight
            ui.withAlpha(0.12f) {
                ui.gradientRect(c, 0f, 0f, m.widthPx, m.px(330f), g.accent, 0x00000000, 0f)
            }
        }
        ui.scrim(c, 0f, 0f, m.widthPx, m.px(240f), 0x66000000, fromTop = true)

        Chrome.topBar(f, 0, listOf(engine.s("games"), engine.s("media")), onTab = { i -> if (i == 1) engine.openMedia() },
            showClock = engine.settings.showClock)
        Chrome.backRow(f, engine.s("profile"))

        ui.withAlpha(a) {
            val left = engine.gutter
            var y = m.px(150f)
            // avatar
            val r = m.px(78f)
            Chrome.avatar(c, ui, left + r, y + r, r, Catalog.profile.avatarSeed, ring = true)
            // trophy level badge on the avatar corner
            val bx = left + r * 2f - m.px(6f)
            val by = y + r * 2f - m.px(18f)
            ui.rect(c, bx - m.px(28f), by - m.px(20f), bx + m.px(28f), by + m.px(20f),
                0xF2FFFFFF.toInt(), m.px(20f))
            Icons.draw(c, Glyph.Trophy, bx - m.px(16f), by, m.px(18f), 0xFF101014.toInt(), m.px(1.6f))
            ui.line(c, "${Catalog.profile.trophyLevel}", bx + m.px(4f), by + m.textPx(8f),
                m.textPx(22f), 0xFF101014.toInt(), Ui.Weight.Bold)

            val tx = left + r * 2f + m.px(34f)
            val name = engine.settings.profileName
            ui.line(c, name, tx, y + m.px(50f), m.textPx(46f), 0xFFFFFFFF.toInt(), Ui.Weight.Display)
            ui.line(c, "${Catalog.profile.onlineName} · United Kingdom", tx, y + m.px(84f),
                m.textPx(22f), Ui.a(Tok.TextSecondary, 0.85f), Ui.Weight.Medium)
            if (Catalog.profile.plusMember) {
                val py = y + m.px(100f)
                ui.rectOutline(c, tx, py, tx + m.px(120f), py + m.px(30f),
                    0xFFF4C542.toInt(), m.px(4f), 1.4f)
                ui.line(c, "PS PLUS", tx + m.px(16f), py + m.px(21f), m.textPx(15f),
                    0xFFF4C542.toInt(), Ui.Weight.Bold)
            }
            // level progress
            val ly = y + r * 2f + m.px(30f)
            val lw = m.widthPx * 0.34f
            ui.line(c, "${engine.s("trophy_level")} ${Catalog.profile.trophyLevel}", left, ly, m.textPx(21f),
                Ui.a(Tok.TextPrimary, 0.9f), Ui.Weight.Medium)
            ui.line(c, "${Catalog.profile.levelPercent}%", left + lw, ly, m.textPx(21f),
                Ui.a(Tok.TextSecondary, 0.8f), Ui.Weight.Medium, align = Paint.Align.RIGHT)
            ui.rect(c, left, ly + m.px(12f), left + lw, ly + m.px(18f), 0x33FFFFFF.toInt(), m.px(3f))
            ui.rect(c, left, ly + m.px(12f), left + lw * (Catalog.profile.levelPercent / 100f),
                ly + m.px(18f), 0xFFFFFFFF.toInt(), m.px(3f))

            // trophy counters
            y = ly + m.px(52f)
            val counters = listOf(
                Triple(Glyph.Trophy, Tok.TrophyPlatinum, Catalog.profile.platinum),
                Triple(Glyph.Trophy, Tok.TrophyGold, Catalog.profile.gold),
                Triple(Glyph.Trophy, Tok.TrophySilver, Catalog.profile.silver),
                Triple(Glyph.Trophy, Tok.TrophyBronze, Catalog.profile.bronze),
            )
            var x = left
            counters.forEach { (glyph, color, count) ->
                Icons.draw(c, glyph, x + m.px(11f), y + m.px(4f), m.px(22f), color, m.px(1.8f))
                val w = ui.line(c, count.toString(), x + m.px(30f), y + m.px(10f),
                    m.textPx(24f), 0xFFFFFFFF.toInt(), Ui.Weight.Bold)
                x += m.px(30f) + w + m.px(38f)
            }
            engine.hits.add(left, y - m.px(16f), left + m.px(460f), y + m.px(26f), 16001) {
                engine.push(TrophiesScene(engine), Sfx.Cue.Select)
            }

            // tabs
            y += m.px(56f)
            val tabs = listOf(engine.s("overview"), engine.s("friends"), engine.s("activity"))
            var tx2 = left
            tabs.forEachIndexed { i, t ->
                val s = m.textPx(23f)
                val w = ui.measure(t, s, Ui.Weight.Medium)
                ui.line(c, t, tx2, y, s,
                    if (tab == i) 0xFFFFFFFF.toInt() else Ui.a(Tok.TextSecondary, 0.7f),
                    Ui.Weight.Medium)
                if (tab == i) ui.rect(c, tx2, y + m.px(10f), tx2 + w, y + m.px(12.5f),
                    0xFFFFFFFF.toInt(), 1f)
                engine.hits.add(tx2 - m.px(10f), y - m.px(26f), tx2 + w + m.px(10f),
                    y + m.px(20f), 16010 + i) {
                    tab = i
                    engine.sfx.play(Sfx.Cue.NavAlt)
                }
                tx2 += w + m.px(40f)
            }
            y += m.px(34f)

            when (tab) {
                0 -> drawOverview(f, left, y)
                1 -> drawFriends(f, left, y)
                else -> drawActivity(f, left, y)
            }
        }
    }

    private fun drawOverview(f: Frame, left: Float, top: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        // recently played row
        ui.line(c, engine.s("recently_played"), left, top + m.px(14f), m.textPx(26f),
            Ui.a(Tok.TextPrimary, 0.92f), Ui.Weight.Medium)
        val tile = m.px(150f)
        val gap = m.px(16f)
        val recents = engine.sim.recentIds().mapNotNull { engine.game(it) }
            .ifEmpty { engine.games.take(6) }.take(6)
        recents.forEachIndexed { i, g ->
            val x = left + i * (tile + gap)
            if (x + tile > m.safeRight) return@forEachIndexed
            val y = top + m.px(34f)
            ui.rect(c, x, y, x + tile, y + tile, 0xFF0B0B0F.toInt(), m.px(10f))
            val bmp = com.kaizer.ps5simulator.core.gfx.Art.get(g.coverArt, tile.toInt(), tile.toInt())
            if (bmp != null) ui.clip(c, x, y, x + tile, y + tile) {
                com.kaizer.ps5simulator.core.gfx.Draw.cover(c, bmp, x, y, x + tile, y + tile)
            } else {
                ui.gradientRect(c, x, y, x + tile, y + tile, g.accent, g.deep, m.px(10f))
            }
            engine.hits.add(x, y, x + tile, y + tile, 16100 + i) { engine.openHub(g.id) }
        }
        // quick actions
        var y = top + m.px(34f) + tile + m.px(36f)
        val actions = listOf(
            Triple(Glyph.Trophy, engine.s("trophies"), { engine.push(TrophiesScene(engine), Sfx.Cue.Select) }),
            Triple(Glyph.Settings, engine.s("settings"), { engine.openSettings() }),
            Triple(Glyph.Library, engine.s("game_library"), { engine.openLibrary() }),
        )
        actions.forEachIndexed { i, (glyph, label, action) ->
            val rowY = y + i * m.px(58f)
            Icons.draw(c, glyph, left + m.px(14f), rowY + m.px(18f), m.px(22f),
                Ui.a(Tok.TextSecondary, 0.9f), m.px(1.8f))
            ui.line(c, label, left + m.px(40f), rowY + m.px(26f), m.textPx(24f),
                Ui.a(Tok.TextPrimary, 0.95f), Ui.Weight.Medium)
            Icons.draw(c, Glyph.ChevronRight, m.widthPx * 0.46f, rowY + m.px(18f), m.px(20f),
                Ui.a(Tok.TextTertiary, 0.8f), m.px(1.8f))
            Chrome.divider(f, left, m.widthPx * 0.46f, rowY + m.px(48f), 0.08f)
            engine.hits.add(left, rowY, m.widthPx * 0.46f, rowY + m.px(50f), 16200 + i) { action() }
        }
    }

    private fun drawFriends(f: Frame, left: Float, top: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        Catalog.friends.forEachIndexed { i, fr ->
            val y = top + i * m.px(66f)
            Chrome.avatar(c, ui, left + m.px(18f), y + m.px(20f), m.px(20f), fr.avatarSeed)
            ui.line(c, fr.name, left + m.px(52f), y + m.px(26f), m.textPx(23f),
                0xFFFFFFFF.toInt(), Ui.Weight.Medium)
            val col = when (fr.status) {
                FriendStatus.PLAYING -> Tok.OnlineGreen
                FriendStatus.ONLINE -> 0xFF7FB7FF.toInt()
                FriendStatus.AWAY -> Tok.WarningAmber
                FriendStatus.OFFLINE -> 0xFF6B6F78.toInt()
            }
            ui.circle(c, left + m.px(52f) + ui.measure(fr.name, m.textPx(23f),
                Ui.Weight.Medium) + m.px(14f), y + m.px(18f), m.px(5f), col)
            ui.line(c, fr.detail, left + m.px(52f), y + m.px(48f), m.textPx(17f),
                Ui.a(Tok.TextSecondary, 0.7f), Ui.Weight.Regular)
            Chrome.divider(f, left, m.widthPx * 0.62f, y + m.px(60f), 0.07f)
        }
    }

    private fun drawActivity(f: Frame, left: Float, top: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val recents = engine.games.take(5)
        recents.forEachIndexed { i, g ->
            val y = top + i * m.px(62f)
            ui.line(c, g.title, left, y + m.px(24f), m.textPx(23f), 0xFFFFFFFF.toInt(),
                Ui.Weight.Medium)
            val mins = engine.sim.playMinutes(g.id)
            ui.line(c, "${mins / 60} h ${mins % 60} min · ${g.genre}", left, y + m.px(46f),
                m.textPx(17f), Ui.a(Tok.TextSecondary, 0.7f), Ui.Weight.Regular)
            val pct = (20 + (g.index * 13) % 70)
            ui.rect(c, m.widthPx * 0.40f, y + m.px(24f), m.widthPx * 0.62f, y + m.px(30f),
                0x2EFFFFFF.toInt(), m.px(3f))
            ui.rect(c, m.widthPx * 0.40f, y + m.px(24f),
                m.widthPx * 0.40f + m.widthPx * 0.22f * (pct / 100f), y + m.px(30f),
                0xFFFFFFFF.toInt(), m.px(3f))
            Chrome.divider(f, left, m.widthPx * 0.62f, y + m.px(56f), 0.07f)
        }
    }
}
