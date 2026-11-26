package com.kaizer.ps5simulator.ui.components

import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.gfx.Art
import com.kaizer.ps5simulator.core.gfx.Draw
import com.kaizer.ps5simulator.core.design.Ref
import com.kaizer.ps5simulator.core.engine.Glyph
import com.kaizer.ps5simulator.core.engine.Icons
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.data.model.Game
import com.kaizer.ps5simulator.data.store.SimState
import kotlin.math.abs

/**
 * The horizontal switcher strip beneath the top bar (the console's app row).
 *
 * Reference geometry (1920x1080 plane):
 *   tile           148 x 148, radius 26
 *   gap            18
 *   selected scale 1.14 with a white 3px ring and a 5px dark inner border
 *   label          30px medium, centred, 18px below the tile
 * The strip scrolls horizontally, keeping the selected tile centred in the visible area
 * once it would otherwise leave the left gutter.
 */
class SwitcherStrip(
    private val engine: com.kaizer.ps5simulator.core.engine.Engine,
) {
    var scroll: Float = 0f
    private var targetScroll: Float = 0f
    var selected: Int = 0
    private var lastContentW: Float = 0f
    private var lastViewW: Float = 0f
    var dragging = false
    private var velocity = 0f

    private val tiles = ArrayList<Tile>()
    private class Tile(val game: Game?, val label: String, val special: Int)

    fun setGames(games: List<Game>) {
        tiles.clear()
        tiles += Tile(null, "PlayStation Store", 1)
        games.forEach { tiles += Tile(it, it.title, 2) }
        tiles += Tile(null, "Game Library", 3)
        if (selected >= tiles.size) selected = 0
    }

    fun select(index: Int) {
        if (tiles.isEmpty()) return
        selected = index.coerceIn(0, tiles.size - 1)
        engine.sfx.play(com.kaizer.ps5simulator.core.engine.Sfx.Cue.Nav)
    }

    fun tileCount(): Int = tiles.size

    fun selectedIndex(): Int = selected

    /** Tile kind: 1 = Store, 2 = game, 3 = Game Library. */
    fun tileKind(index: Int): Int = tiles.getOrNull(index)?.special ?: 0

    fun tileGame(index: Int): Game? = tiles.getOrNull(index)?.game

    fun selectedGame(): Game? = tiles.getOrNull(selected)?.game

    fun scrollBy(dx: Float) {
        val maxScroll = (lastContentW - lastViewW).coerceAtLeast(0f)
        scroll = (scroll - dx).coerceIn(-0f.coerceAtLeast(0f), maxScroll)
        targetScroll = scroll
    }

    fun fling() {
        val maxScroll = (lastContentW - lastViewW).coerceAtLeast(0f)
        targetScroll = (targetScroll - velocity * 120f).coerceIn(0f, maxScroll)
        velocity = 0f
    }

    fun update(dt: Long) {
        val maxScroll = (lastContentW - lastViewW).coerceAtLeast(0f)
        if (!dragging && abs(targetScroll - scroll) > 0.5f) {
            targetScroll = targetScroll.coerceIn(0f, maxScroll)
            scroll += (targetScroll - scroll) * (1f - kotlin.math.exp(-14f * dt / 1000f))
        } else if (!dragging) {
            scroll = scroll.coerceIn(0f, maxScroll)
        }
    }

    /** Draws the strip; [centreIndex] keeps the focus tile visible. */
    fun draw(f: Frame, top: Float, left: Float, right: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (tiles.isEmpty()) return
        val size = m.px(Ref.SWITCHER_SIZE)
        val gap = m.px(Ref.SWITCHER_GAP)
        val step = size + gap
        val viewW = right - left
        lastViewW = viewW
        lastContentW = tiles.size * step

        // keep the focus visible inside the strip
        if (!dragging) {
            val focusL = selected * step
            val focusR = focusL + size
            if (focusL - scroll < 0f) targetScroll = focusL
            if (focusR - scroll > viewW) targetScroll = (focusR - viewW).coerceAtLeast(0f)
        }
        val maxScroll = (lastContentW - viewW).coerceAtLeast(0f)
        val s = scroll.coerceIn(0f, maxScroll)

        ui.clip(c, left, top - size * 0.4f, right, top + size * 1.75f) {
            var i = 0
            while (i < tiles.size) {
                val x = left + i * step - s
                if (x + size > left - size && x < right + size) {
                    drawTile(f, tiles[i], i, x, top, size, left, right)
                }
                i++
            }
        }
        // left fade so cropped tiles dissolve instead of cutting hard at the gutter
        ui.scrim(c, left - m.px(8f), top - size * 0.4f, left + m.px(54f), top + size * 1.6f,
            0xCC000000.toInt(), fromTop = false)
        // horizontal fade
        ui.rect(c, left - m.px(40f), top - size * 0.4f, left, top + size * 1.6f, 0xFF000000.toInt(), 0f)
    }

    private fun drawTile(
        f: Frame, tile: Tile, index: Int, x: Float, top: Float, size: Float,
        left: Float, right: Float,
    ) {
        val ui = f.ui
        val c = f.canvas
        val m = engine.metrics
        val isSel = index == selected
        val radius = m.px(Ref.SWITCHER_RADIUS)
        val scale = if (isSel) Ref.SWITCHER_SELECTED_SCALE else 1f
        val ts = size * scale
        val tx = x - (ts - size) / 2f
        val ty = top - (ts - size) / 2f

        // shadow + base fill
        Chrome.floatShadow(f, tx, ty, tx + ts, ty + ts, radius)

        when (tile.special) {
            1 -> { // PlayStation Store tile: dark rounded square with the blue bag glyph
                ui.rect(c, tx, ty, tx + ts, ty + ts, 0xFF0A1226.toInt(), radius)
                ui.gradientRect(c, tx, ty, tx + ts, ty + ts, 0xFF122040.toInt(),
                    0xFF070C18.toInt(), radius)
                // bag
                val bw = ts * 0.34f
                val bh = ts * 0.27f
                val cx = tx + ts / 2f
                val cy = ty + ts / 2f
                ui.rect(c, cx - bw / 2f, cy - bh * 0.30f, cx + bw / 2f, cy + bh * 0.70f,
                    0xFF2E86DE.toInt(), ts * 0.04f)
                ui.rectOutline(c, cx - bw * 0.30f, cy - bh * 0.78f, cx + bw * 0.30f,
                    cy + bh * 0.10f, 0xFF2E86DE.toInt(), bw * 0.30f, ts * 0.022f)
                ui.line(c, "PS", cx, cy + bh * 0.30f, ts * 0.17f, 0xFFFFFFFF.toInt(),
                    Ui.Weight.Bold, align = android.graphics.Paint.Align.CENTER)
            }
            3 -> { // Game Library tile: white glyph on translucent black
                ui.rect(c, tx, ty, tx + ts, ty + ts, 0x66101014.toInt(), radius)
                ui.rect(c, tx, ty, tx + ts, ty + ts, 0x33000000.toInt(), radius)
                Icons.draw(c, Glyph.Library, tx + ts / 2f, ty + ts / 2f, ts * 0.46f,
                    0xFFFFFFFF.toInt(), ts * 0.035f)
            }
            else -> {
                val g = tile.game ?: return
                ui.rect(c, tx, ty, tx + ts, ty + ts, 0xFF0B0B10.toInt(), radius)
                val bmp = Art.get(g.cardArt, (ts).toInt().coerceAtLeast(64), ts.toInt().coerceAtLeast(64))
                if (bmp != null) {
                    ui.clip(c, tx, ty, tx + ts, ty + ts) {
                        Draw.cover(c, bmp, tx, ty, tx + ts, ty + ts)
                    }
                } else {
                    ui.gradientRect(c, tx, ty, tx + ts, ty + ts, g.accent, g.deep, radius)
                    ui.line(c, g.titleInitials, tx + ts / 2f, ty + ts * 0.58f, ts * 0.22f,
                        0x66FFFFFF.toInt(), Ui.Weight.Display, align = android.graphics.Paint.Align.CENTER)
                }
                // keep tiles readable over bright art
                ui.rect(c, tx, ty, tx + ts, ty + ts, 0x14000000.toInt(), radius)
            }
        }

        if (isSel) {
            // inner dark keyline + white ring, exactly like the focused console tile
            ui.rectOutline(c, tx, ty, tx + ts, ty + ts, 0xFF101014.toInt(), radius, m.px(5f))
            Chrome.focusRing(f, tx + m.px(2.5f), ty + m.px(2.5f), tx + ts - m.px(2.5f),
                ty + ts - m.px(2.5f), radius)
        }

        // label (only for the selected / first special tiles, like the console)
        if (isSel && tile.label.isNotEmpty()) {
            val ls = m.textPx(Ref.SWITCHER_LABEL_SIZE)
            val baseline = ty + ts + m.px(Ref.SWITCHER_LABEL_GAP) + ls * 0.85f
            ui.lineFit(c, tile.label, tx + ts / 2f, baseline, maxOf(ts * 1.9f, m.px(300f)), ls,
                Ui.a(Tok.TextPrimary, 0.95f), Ui.Weight.Medium,
                align = android.graphics.Paint.Align.CENTER)
        }

        engine.hits.add(tx, ty, tx + ts, ty + ts, 9400 + index) {
            if (tile.special == 1) {
                engine.openStore()
            } else if (tile.special == 3) {
                engine.openLibrary()
            } else {
                select(index)
                engine.sfx.play(com.kaizer.ps5simulator.core.engine.Sfx.Cue.NavAlt)
            }
        }
    }

    /** Switcher index offset: games start at 1 (index 0 is the store tile). */
    fun indexOf(gameId: String): Int {
        val i = tiles.indexOfFirst { it.game?.id == gameId }
        return if (i >= 0) i else 1
    }

    fun gameAt(index: Int): Game? = tiles.getOrNull(index)?.game
}
