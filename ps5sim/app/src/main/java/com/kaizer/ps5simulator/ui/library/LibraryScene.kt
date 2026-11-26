package com.kaizer.ps5simulator.ui.library

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
import com.kaizer.ps5simulator.data.model.Game
import com.kaizer.ps5simulator.data.model.LibraryFilter
import com.kaizer.ps5simulator.data.model.LibrarySort
import com.kaizer.ps5simulator.ui.components.Cards
import com.kaizer.ps5simulator.ui.components.Chrome
import kotlin.math.ceil

/**
 * GAME LIBRARY.
 *
 * Reference layout: dimmed artwork header with the library tile, "Game Library" title,
 * "Console storage · 20" row, "Sort by: Name (A - Z)" on the right, then a square-cover
 * grid (5 columns at 1920 width, 22 unit gaps, 12 unit radius).
 *
 * Working: search, sort, category filter, favourites, install-state badges.
 */
class LibraryScene(private val engine: Engine) : Scene {

    override val id: Int = 3102

    private var sort = LibrarySort.RECENT
    private var filter = LibraryFilter.ALL
    private var query = ""
    private var searchOpen = false
    private var grid: List<Game> = emptyList()
    private var scroll = 0f
    private var targetScroll = 0f
    private var maxScroll = 0f
    private var dragging = false
    private var reveal = 0f
    private var selectedId: String? = null

    override fun onShow() {
        rebuild()
        reveal = 0f
    }

    private fun rebuild() {
        val all = engine.games
        val favs = engine.sim.favoriteIds
        val inst = engine.sim.installedIds
        var list = when (filter) {
            LibraryFilter.ALL -> all
            LibraryFilter.INSTALLED -> all.filter { inst.contains(it.id) }
            LibraryFilter.FAVORITES -> all.filter { favs.contains(it.id) }
        }
        if (query.isNotBlank()) {
            list = list.filter { it.title.contains(query, ignoreCase = true) }
        }
        list = when (sort) {
            LibrarySort.RECENT -> {
                val order = engine.sim.recentIds()
                list.sortedWith(compareBy<Game> { if (order.indexOf(it.id) >= 0) order.indexOf(it.id) else 99 }
                    .thenBy { it.index })
            }
            LibrarySort.NAME -> list.sortedBy { it.title }
            LibrarySort.SIZE -> list.sortedByDescending { it.sizeGb }
            LibrarySort.RELEASE -> list.sortedByDescending { it.year }
        }
        grid = list
        scroll = 0f
        targetScroll = 0f
    }

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (engine.animationsOn) reveal = (reveal + f.dtMs / 280f).coerceAtMost(1f) else reveal = 1f
        val a = Ease.soft(reveal)

        // backdrop: dimmed selected artwork, PS5 library keeps the current game's art behind
        val bg = engine.selectedGame()
        ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, 0xFF07070A.toInt())
        if (bg != null) {
            val bmp = Art.get(bg.artwork, m.widthPx.toInt(), m.heightPx.toInt())
            if (bmp != null) ui.withAlpha(0.62f * a) {
                Draw.coverBias(c, bmp, 0f, 0f, m.widthPx, m.heightPx, 0.6f, 0.25f)
                ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, 0x66000000.toInt())
            }
        }
        ui.scrim(c, 0f, 0f, m.widthPx, m.heightPx, 0x8C000000.toInt(), fromTop = false)
        ui.scrim(c, 0f, 0f, m.widthPx, m.px(420f), 0xCC000000.toInt(), fromTop = true)

        Chrome.topBar(f, 0, listOf("Games", "Media"), onTab = { i ->
            if (i == 1) engine.openMedia()
        }, showClock = engine.settings.showClock)

        val left = engine.gutter
        var y = m.px(150f)
        // library tile + title
        val tileS = m.px(84f)
        ui.rect(c, left, y, left + tileS, y + tileS, 0xCC101014.toInt(), m.px(18f))
        Icons.draw(c, Glyph.Library, left + tileS / 2f, y + tileS / 2f, tileS * 0.5f,
            0xFFFFFFFF.toInt(), m.px(2.4f))
        ui.line(c, "Game Library", left + tileS + m.px(28f), y + tileS * 0.68f, m.textPx(34f),
            0xFFFFFFFF.toInt(), Ui.Weight.Bold)
        engine.hits.add(left, y, left + tileS, y + tileS, 11001) { engine.pop() }
        y += tileS + m.px(40f)

        // storage line + sort control
        val count = grid.size
        ui.line(c, "Console storage · $count", left, y, m.textPx(26f),
            Ui.a(Tok.TextSecondary, 0.85f), Ui.Weight.Medium)
        // sort chip
        val sortLabel = "Sort by: ${sort.label}"
        val ss = m.textPx(22f)
        val sw = ui.measure(sortLabel, ss, Ui.Weight.Medium)
        val sx = m.safeRight - engine.gutter - sw
        Icons.draw(c, Glyph.Sort, sx - m.px(18f), y - m.px(8f), m.px(20f),
            Ui.a(Tok.TextSecondary, 0.8f), m.px(1.8f))
        ui.line(c, sortLabel, sx, y, ss, Ui.a(Tok.TextPrimary, 0.9f), Ui.Weight.Medium)
        engine.hits.add(sx - m.px(40f), y - m.px(30f), m.safeRight, y + m.px(12f), 11002) {
            sort = LibrarySort.entries[(sort.ordinal + 1) % LibrarySort.entries.size]
            engine.sfx.play(Sfx.Cue.Select)
            rebuild()
            engine.toast("Sorted by ${sort.label}")
        }
        // filter + search chips
        y += m.px(34f)
        var cx = left
        LibraryFilter.entries.forEach { fl ->
            cx += Cards.chip(f, fl.label, cx, y, filter == fl) {
                filter = fl
                engine.sfx.play(Sfx.Cue.Nav)
                rebuild()
            } + m.px(12f)
        }
        // search
        cx = Cards.chip(f, if (query.isBlank()) "Search" else "Search: $query", cx, y,
            searchOpen && query.isNotBlank()) {
            engine.showSearch { q ->
                query = q
                rebuild()
            }
        }
        y += m.px(48f) + m.px(34f)

        // grid
        val cols = if (m.widthPx > m.px(1500f)) 5 else 4
        val available = m.safeRight - engine.gutter
        val gap = m.px(Ref.LIB_TILE_GAP)
        val tile = ((available - gap * (cols - 1)) / cols).coerceAtMost(m.px(Ref.LIB_TILE_SIZE * 1.15f))
        val stepX = tile + gap
        val stepY = tile + m.px(16f) + m.px(52f)
        val rows = ceil(grid.size.toFloat() / cols).toInt()
        val contentH = rows * stepY
        maxScroll = (contentH - (m.heightPx - y - m.px(30f))).coerceAtLeast(0f)
        targetScroll = targetScroll.coerceIn(0f, maxScroll)
        if (!dragging) {
            scroll += (targetScroll - scroll) * (1f - kotlin.math.exp(-16f * f.dtMs / 1000f))
        }

        ui.withAlpha(a) {
            ui.clip(c, 0f, y - m.px(10f), m.widthPx, m.heightPx) {
                grid.forEachIndexed { i, g ->
                    val col = i % cols
                    val row = i / cols
                    val x = left + col * stepX
                    val ty = y + row * stepY - scroll
                    if (ty + stepY < y - m.px(20f) || ty > m.heightPx) return@forEachIndexed
                    val focused = g.id == selectedId
                    Cards.libraryTile(f, g, x, ty, tile, focused,
                        engine.sim.installedIds.contains(g.id),
                        engine.sim.favoriteIds.contains(g.id)) {
                        selectedId = g.id
                        engine.sim.selectedGameId = g.id
                        engine.openHub(g.id)
                    }
                    // title + meta under the tile
                    ui.lineFit(c, g.title, x, ty + tile + m.px(26f), tile, m.textPx(22f),
                        0xFFFFFFFF.toInt(), Ui.Weight.Medium)
                    ui.lineFit(c, "${g.genre} · ${g.sizeGb.toInt()} GB", x,
                        ty + tile + m.px(48f), tile, m.textPx(17f),
                        Ui.a(Tok.TextSecondary, 0.65f), Ui.Weight.Regular)
                }
            }
        }

        if (grid.isEmpty()) {
            ui.line(c, if (query.isBlank()) "Nothing here yet" else "No games match “$query”",
                m.widthPx / 2f, m.heightPx / 2f, m.textPx(30f), Ui.a(Tok.TextSecondary, 0.8f),
                Ui.Weight.Medium, align = Paint.Align.CENTER)
        }
    }

    override fun onDown(x: Float, y: Float): Boolean {
        dragStart = y
        scrollStart = targetScroll
        dragAccum = 0f
        return false
    }

    private var dragStart = 0f
    private var scrollStart = 0f
    private var dragAccum = 0f

    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (kotlin.math.abs(dy) > 2f) {
            dragging = true
            targetScroll = (scrollStart - (y - dragStart)).coerceIn(0f, maxScroll)
            scroll = targetScroll
            return true
        }
        return false
    }

    override fun onUp(x: Float, y: Float): Boolean {
        dragging = false
        return false
    }

    override fun back(): Boolean {
        if (searchOpen) {
            searchOpen = false
            return true
        }
        return false
    }
}
