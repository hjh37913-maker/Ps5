package com.kaizer.ps5simulator.ui.controlcenter

import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Ease
import com.kaizer.ps5simulator.core.design.Ref
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.engine.Engine
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Glyph
import com.kaizer.ps5simulator.core.engine.Icons
import com.kaizer.ps5simulator.core.engine.Scene
import com.kaizer.ps5simulator.core.engine.Sfx
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.data.catalog.Catalog
import com.kaizer.ps5simulator.data.model.ActivityCard
import com.kaizer.ps5simulator.data.model.Game
import com.kaizer.ps5simulator.ui.components.Cards
import kotlin.math.abs

/**
 * CONTROL CENTER (the console's quick menu overlay).
 *
 * Two stacked surfaces, both reconstructed from reference captures:
 *
 *  A. the card row (activity cards for the selected game, horizontally scrollable)
 *     rising from the bottom with a soft blur/scrim behind it;
 *  B. the glyph dock: a single row of monochrome icons — Home, Library, Notifications,
 *     Downloads, Music, Volume, Microphone, Controller, Headset, Power — with the
 *     currently focused glyph drawn in a ring and a small text label under it.
 *
 * Interaction model on touch:
 *   • tap a dock glyph  → focuses it (sound), expands its inline panel for stateful
 *     glyphs (volume slider, music controls, notifications, downloads, power);
 *   • tap an activity card → simulated resume (opens the launch sequence);
 *   • swipe the card row → scrolls cards, exactly like the console's horizontal row;
 *   • swipe down / tap the scrim / press back → closes with the reverse animation.
 */
class ControlCenterScene(private val engine: Engine) : Scene {

    override val id: Int = 3104

    var visible = false
        private set
    var anim = 0f
        private set
    var expandedGlyph = -1
        private set
    private var cardScroll = 0f
    private var cardTarget = 0f
    private var dragging = false
    private var dragStartY = 0f
    private var dragAccum = 0f
    private var dragStartX = 0f

    private val glyphs = listOf(
        Glyph.Home, Glyph.Library, Glyph.Bell, Glyph.Install, Glyph.Music, Glyph.Speaker,
        Glyph.Mic, Glyph.Controller, Glyph.Headset, Glyph.Power,
    )
    private val glyphLabels = listOf(
        "home", "game_library", "notifications", "downloads", "music", "sound",
        "microphone", "accessories", "headset", "power",
    )

    private var activityCards: List<ActivityCard> = emptyList()
    private var activityGame: Game? = null

    fun show() {
        prepareCards()
        visible = true
        expandedGlyph = -1
        engine.sfx.play(Sfx.Cue.Open)
    }

    fun hide() {
        visible = false
        expandedGlyph = -1
        engine.sfx.play(Sfx.Cue.Close)
    }

    fun toggle() {
        if (visible) hide() else show()
    }

    private fun prepareCards() {
        val g = engine.selectedGame()
        activityGame = g
        activityCards = g?.let { Catalog.activitiesFor(it) } ?: emptyList()
        cardScroll = 0f
        cardTarget = 0f
    }

    override fun isAnimating(): Boolean =
        visible || anim > 0.002f || expandedGlyph >= 0 && anim > 0.5f

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        // animate in: console uses a soft rise + fade, ~300 ms
        if (engine.animationsOn) {
            val target = if (visible) 1f else 0f
            val rate = if (visible) 1000f / Tok.DUR_CC_OPEN else 1000f / Tok.DUR_CC_CLOSE
            anim = Ease.approach(anim, target, rate * 2.6f, f.dtMs)
            if (!visible && anim < 0.002f) {
                anim = 0f
                return
            }
        } else {
            anim = if (visible) 1f else 0f
            if (anim == 0f) return
        }
        val ease = if (visible) Ease.out(anim) else Ease.into(anim)

        if (abs(cardTarget - cardScroll) > 0.5f && !dragging) {
            cardScroll += (cardTarget - cardScroll) * (1f - kotlin.math.exp(-13f * f.dtMs / 1000f))
        }

        // A. scrim over the scene
        ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, ((ease * 0xB8).toInt() shl 24), 0f)

        // B. activity cards row
        drawCards(f, ease)

        // C. dock
        drawDock(f, ease)

        // D. expanded panel for the focused glyph
        if (expandedGlyph >= 0 && ease > 0.6f) drawPanel(f, expandedGlyph, ease)
    }

    // ------------------------------------------------------------------ cards
    private fun drawCards(f: Frame, ease: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val cards = activityCards
        if (cards.isEmpty()) return
        val cardW = m.px(Ref.CC_CARD_W)
        val cardH = m.px(Ref.CC_CARD_MEDIA_H)
        val gap = m.px(Ref.CC_CARD_GAP)
        val bottom = m.heightPx - m.px(Ref.CC_CARD_BOTTOM_GAP)
        val top = bottom - cardH
        val left = engine.gutter - m.px(10f)
        val right = m.safeRight
        val rise = (1f - ease) * m.px(120f)
        val max = cards.size * (cardW + gap) - (right - left)
        cardTarget = cardTarget.coerceIn(0f, max.coerceAtLeast(0f))

        ui.withAlpha(ease) {
            // subtle PS-blue focus bar above the cards (console homes the dock under a
            // thin focus indicator when the card row has focus)
            ui.clip(c, left - m.px(20f), top - m.px(20f) + rise, right, bottom + m.px(20f) + rise) {
                cards.forEachIndexed { i, card ->
                    val x = left + i * (cardW + gap) - cardScroll
                    if (x + cardW < left - gap || x > right + gap) return@forEachIndexed
                    val isFocused = x >= left - cardW * 0.25f && x < left + m.px(30f)
                    Cards.activityCard(f, card, activityGame, x, top + rise, cardW, cardH,
                        isFocused) {
                        resume(card)
                    }
                }
            }
            // (the reference shows no pagination dots: the row simply scrolls)
            if (false && cards.size > 1) {
                val dots = cards.size
                val dw = m.px(6f)
                val gapD = m.px(8f)
                val totalW = dots * dw + (dots - 1) * gapD
                var dx = left + m.px(10f)
                val dy = bottom + m.px(20f) + rise
                for (i in 0 until dots) {
                    val active = i == (cardScroll / (cardW + gap)).toInt()
                    ui.circle(c, dx + dw / 2f, dy, dw / 2f,
                        if (active) 0xE6FFFFFF.toInt() else 0x55FFFFFF.toInt())
                    dx += dw + gapD
                }
                if (totalW > 0) Unit
            }
        }
    }

    private fun resume(card: ActivityCard) {
        val g = activityGame ?: return
        engine.sim.recordLaunch(g.id)
        hide()
        engine.openLaunch(g)
    }

    // ------------------------------------------------------------------ dock
    private fun drawDock(f: Frame, ease: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val dockBottom = m.heightPx - m.px(34f)
        val iconSize = m.px(Ref.CC_ICON_SIZE)
        val labelGap = m.px(18f)
        val labelSize = m.textPx(18f)
        val totalW = glyphs.size * iconSize + (glyphs.size - 1) * m.px(Ref.CC_ICON_GAP)
        var x = (m.widthPx - totalW) / 2f
        val cy = dockBottom - iconSize / 2f - m.px(6f)
        val arise = (1f - ease) * m.px(40f)

        // the focused glyph keeps a label under the dock, as on console
        val focusedLabel = if (expandedGlyph >= 0) engine.s(glyphLabels[expandedGlyph]) else ""


        ui.withAlpha(ease) {
            // dock backdrop: soft dark plate so the glyphs always read
            ui.rect(c, 0f, cy - iconSize + arise, m.widthPx, m.heightPx, 0x00000000.toInt(), 0f)
            val plateTop = cy - iconSize * 1.35f
            ui.scrim(c, 0f, plateTop, m.widthPx, m.heightPx, 0xE6000000.toInt(), fromTop = false)

            glyphs.forEachIndexed { i, g ->
                val gx = x + i * (iconSize + m.px(Ref.CC_ICON_GAP))
                val isFocus = i == expandedGlyph
                // focus ring
                if (isFocus) {
                    ui.rectOutline(c, gx - iconSize * 0.34f, cy - iconSize * 0.94f + arise,
                        gx + iconSize * 1.34f, cy + iconSize * 0.94f + arise,
                        0xD9FFFFFF.toInt(), iconSize * 0.92f, 2f)
                }
                val alphaColor = if (isFocus) 0xFFFFFFFF.toInt() else Ui.a(Tok.Glyph, 0.88f)
                drawDockGlyph(f, g, gx + iconSize / 2f, cy + arise, iconSize, alphaColor, i)
                engine.hits.add(gx - m.px(10f), cy - iconSize + arise, gx + iconSize + m.px(10f),
                    cy + iconSize + arise, 14000 + i) {
                    onGlyph(i)
                }
                x = gx - i * (iconSize + m.px(Ref.CC_ICON_GAP)) // keep loop tidy
            }

            // label under the focused glyph only
            if (focusedLabel.isNotEmpty()) {
                ui.line(c, focusedLabel, m.widthPx / 2f, m.heightPx - m.px(8f) + arise,
                    labelSize, Ui.a(Tok.TextPrimary, 0.85f), Ui.Weight.Medium,
                    align = Paint.Align.CENTER)
            }
        }
    }

    private fun drawDockGlyph(f: Frame, g: Glyph, cx: Float, cy: Float, size: Float, color: Int, index: Int) {
        val ui = f.ui
        val c = f.canvas
        val m = engine.metrics
        Icons.draw(c, g, cx, cy, size, color, m.px(2.2f))
        // state badges (console shows tiny counters and status dots)
        when (index) {
            2 -> { // notifications count
                val n = Catalog.notifications().count { it.unread }
                if (n > 0) {
                    ui.line(c, n.toString(), cx + size * 0.52f, cy + size * 0.58f,
                        m.textPx(17f), 0xFF4D8DF6.toInt(), Ui.Weight.Bold,
                        align = Paint.Align.CENTER)
                }
            }
            3 -> { // downloads progress dot
                ui.circle(c, cx + size * 0.42f, cy + size * 0.42f, size * 0.09f,
                    0xFF4D8DF6.toInt())
            }
            4 -> if (engine.sim.musicPlaying) {
                ui.circle(c, cx + size * 0.42f, cy - size * 0.42f, size * 0.09f,
                    0xFF3ED17C.toInt())
            }
            6 -> if (engine.sim.microphoneMuted) {
                ui.circle(c, cx + size * 0.42f, cy + size * 0.42f, size * 0.09f,
                    0xFFE8B04B.toInt())
            }
            7 -> { // controller battery-ish pip
                val w = size * 0.42f
                val h = size * 0.2f
                ui.rect(c, cx - w / 2f, cy + size * 0.44f, cx + w / 2f, cy + size * 0.44f + h,
                    0x66FFFFFF.toInt(), h / 2f)
                ui.rect(c, cx - w / 2f, cy + size * 0.44f, cx - w / 2f + w * 0.72f,
                    cy + size * 0.44f + h, 0xFF3ED17C.toInt(), h / 2f)
            }
        }
    }

    private fun onGlyph(i: Int) {
        engine.sfx.play(Sfx.Cue.NavAlt)
        when (i) {
            0 -> { hide(); engine.popToRoot() }
            1 -> { hide(); engine.openLibrary() }
            9 -> engine.confirm(engine.s("turn_off"), "This is the simulator's power menu. The app will return to the home screen.", engine.s("power")) {
                hide(); engine.popToRoot()
            }
            else -> expandedGlyph = if (expandedGlyph == i) -1 else i
        }
    }

    // ------------------------------------------------------------------ panels
    private fun drawPanel(f: Frame, index: Int, ease: Float) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        val pw = m.widthPx * 0.46f
        val ph = when (index) { 5, 4 -> m.px(230f); 2 -> m.px(360f); 3 -> m.px(280f); else -> m.px(200f) }
        val px = (m.widthPx - pw) / 2f
        val py = m.heightPx - m.px(Ref.CC_CARD_BOTTOM_GAP) - m.px(Ref.CC_CARD_MEDIA_H) * 0f -
            m.px(190f) - ph
        ui.withAlpha(ease) {
            ui.rect(c, px, py, px + pw, py + ph, Tok.PanelGlass, m.px(24f))
            ui.rectOutline(c, px, py, px + pw, py + ph, Tok.PanelBorder, m.px(24f), 1.2f)
            when (index) {
                5 -> soundPanel(f, px, py, pw, ph)
                4 -> musicPanel(f, px, py, pw, ph)
                2 -> notificationsPanel(f, px, py, pw, ph)
                3 -> downloadsPanel(f, px, py, pw, ph)
                6 -> micPanel(f, px, py, pw, ph)
                7, 8 -> accessoriesPanel(f, px, py, pw, ph, index)
                else -> genericPanel(f, px, py, pw, ph, index)
            }
        }
    }

    private fun panelTitle(f: Frame, x: Float, y: Float, text: String) {
        f.ui.line(f.canvas, text, x, y, f.engine.metrics.textPx(26f), 0xFFFFFFFF.toInt(),
            Ui.Weight.Bold)
    }

    private fun soundPanel(f: Frame, px: Float, py: Float, pw: Float, ph: Float) {
        val m = engine.metrics
        val ui = f.ui
        panelTitle(f, px + m.px(28f), py + m.px(40f), engine.s("sound"))
        ui.line(f.canvas, "Speaker", px + m.px(28f), py + m.px(76f), m.textPx(19f),
            Ui.a(Tok.TextSecondary, 0.8f), Ui.Weight.Medium)
        Cards.slider(f, px + m.px(28f), py + m.px(92f), pw - m.px(56f), m.px(22f),
            engine.sim.speakerVolume / 100f, Glyph.Speaker.ordinal) { v ->
            engine.sim.speakerVolume = (v * 100).toInt()
        }
        ui.line(f.canvas, "${engine.sim.speakerVolume}", px + pw - m.px(28f), py + m.px(76f),
            m.textPx(19f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium, align = Paint.Align.RIGHT)
        var bx = px + m.px(28f)
        bx += Cards.chip(f, if (engine.sim.microphoneMuted) "Mic muted" else "Mic on", bx,
            py + ph - m.px(60f), engine.sim.microphoneMuted) {
            engine.sim.microphoneMuted = !engine.sim.microphoneMuted
            engine.sfx.play(Sfx.Cue.Select)
        } + m.px(12f)
        Cards.chip(f, if (engine.settings.sfxEnabled) "UI sounds on" else "UI sounds off", bx,
            py + ph - m.px(60f), engine.settings.sfxEnabled) {
            engine.settings.sfxEnabled = !engine.settings.sfxEnabled
            if (engine.settings.sfxEnabled) engine.sfx.play(Sfx.Cue.Select)
        }
    }

    private fun musicPanel(f: Frame, px: Float, py: Float, pw: Float, ph: Float) {
        val m = engine.metrics
        val ui = f.ui
        panelTitle(f, px + m.px(28f), py + m.px(40f), engine.s("music"))
        ui.lineFit(f.canvas, engine.sim.nowPlaying, px + m.px(28f), py + m.px(76f),
            pw - m.px(56f), m.textPx(20f), Ui.a(Tok.TextSecondary, 0.85f), Ui.Weight.Medium)
        Cards.slider(f, px + m.px(28f), py + m.px(94f), pw - m.px(56f), m.px(18f),
            engine.sim.musicVolume / 100f, Glyph.Music.ordinal) { v ->
            engine.sim.musicVolume = (v * 100).toInt()
        }
        // transport
        val cy = py + ph - m.px(48f)
        val cx = px + pw / 2f
        Icons.draw(f.canvas, Glyph.Prev, cx - m.px(80f), cy, m.px(24f), 0xE6FFFFFF.toInt(), 0f)
        f.engine.hits.add(cx - m.px(100f), cy - m.px(30f), cx - m.px(60f), cy + m.px(30f), 15001) {
            engine.sfx.play(Sfx.Cue.NavAlt)
        }
        ui.circle(f.canvas, cx, cy, m.px(28f), 0xF2FFFFFF.toInt())
        Icons.draw(f.canvas, if (engine.sim.musicPlaying) Glyph.Pause else Glyph.Play, cx, cy,
            m.px(22f), 0xFF101014.toInt(), 0f)
        f.engine.hits.add(cx - m.px(28f), cy - m.px(28f), cx + m.px(28f), cy + m.px(28f), 15002) {
            engine.sim.musicPlaying = !engine.sim.musicPlaying
            engine.sfx.play(Sfx.Cue.Select)
        }
        Icons.draw(f.canvas, Glyph.Next, cx + m.px(80f), cy, m.px(24f), 0xE6FFFFFF.toInt(), 0f)
        f.engine.hits.add(cx + m.px(60f), cy - m.px(30f), cx + m.px(100f), cy + m.px(30f), 15003) {
            engine.sfx.play(Sfx.Cue.NavAlt)
        }
    }

    private fun notificationsPanel(f: Frame, px: Float, py: Float, pw: Float, ph: Float) {
        val m = engine.metrics
        val ui = f.ui
        panelTitle(f, px + m.px(28f), py + m.px(40f), engine.s("notifications"))
        val list = Catalog.notifications()
        var y = py + m.px(72f)
        list.take(4).forEach { n ->
            ui.circle(f.canvas, px + m.px(36f), y + m.px(2f), m.px(5f),
                if (n.unread) 0xFF4D8DF6.toInt() else 0x44FFFFFF.toInt())
            ui.lineFit(f.canvas, n.title, px + m.px(50f), y + m.px(8f), pw - m.px(150f),
                m.textPx(21f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
            ui.line(f.canvas, n.time, px + pw - m.px(28f), y + m.px(8f), m.textPx(16f),
                Ui.a(Tok.TextTertiary, 0.8f), Ui.Weight.Regular, align = Paint.Align.RIGHT)
            ui.lineFit(f.canvas, n.body, px + m.px(50f), y + m.px(32f), pw - m.px(78f),
                m.textPx(17f), Ui.a(Tok.TextSecondary, 0.75f), Ui.Weight.Regular)
            y += m.px(58f)
        }
        val bx = px + m.px(28f)
        Cards.chip(f, if (engine.sim.notificationsMuted) "Muted" else "Do not disturb", bx,
            py + ph - m.px(52f), engine.sim.notificationsMuted) {
            engine.sim.notificationsMuted = !engine.sim.notificationsMuted
            engine.sim.doNotDisturb = engine.sim.notificationsMuted
            engine.sfx.play(Sfx.Cue.Select)
        }
    }

    private fun downloadsPanel(f: Frame, px: Float, py: Float, pw: Float, ph: Float) {
        val m = engine.metrics
        val ui = f.ui
        panelTitle(f, px + m.px(28f), py + m.px(40f), engine.s("downloads"))
        val g = activityGame
        if (g == null) {
            ui.line(f.canvas, "Nothing downloading", px + m.px(28f), py + m.px(90f),
                m.textPx(20f), Ui.a(Tok.TextSecondary, 0.7f), Ui.Weight.Regular)
            return
        }
        ui.lineFit(f.canvas, "${g.title} — update 1.14", px + m.px(28f), py + m.px(80f),
            pw - m.px(56f), m.textPx(20f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
        val barY = py + m.px(104f)
        ui.rect(f.canvas, px + m.px(28f), barY, px + pw - m.px(28f), barY + m.px(8f),
            0x33FFFFFF.toInt(), m.px(4f))
        ui.rect(f.canvas, px + m.px(28f), barY, px + m.px(28f) + (pw - m.px(56f)) * 0.64f,
            barY + m.px(8f), 0xFF4D8DF6.toInt(), m.px(4f))
        ui.line(f.canvas, "64%  ·  2.1 GB of ${g.sizeGb.toInt()} GB", px + m.px(28f),
            py + m.px(140f), m.textPx(17f), Ui.a(Tok.TextSecondary, 0.75f), Ui.Weight.Regular)
        Cards.chip(f, "Pause", px + m.px(28f), py + ph - m.px(60f), false) {
            engine.toast("Download paused (simulator)")
        }
        Cards.chip(f, "Cancel", px + m.px(128f), py + ph - m.px(60f), false) {
            engine.toast("Download cancelled (simulator)")
        }
    }

    private fun micPanel(f: Frame, px: Float, py: Float, pw: Float, ph: Float) {
        val m = engine.metrics
        panelTitle(f, px + m.px(28f), py + m.px(40f), engine.s("microphone"))
        Cards.chip(f, if (engine.sim.microphoneMuted) "Muted" else "Open", px + m.px(28f),
            py + m.px(64f), engine.sim.microphoneMuted) {
            engine.sim.microphoneMuted = !engine.sim.microphoneMuted
            engine.sfx.play(Sfx.Cue.Select)
        }
        f.ui.line(f.canvas, "Party chat is a simulator demo", px + m.px(28f), py + ph - m.px(30f),
            m.textPx(17f), Ui.a(Tok.TextTertiary, 0.8f), Ui.Weight.Regular)
    }

    private fun accessoriesPanel(f: Frame, px: Float, py: Float, pw: Float, ph: Float, index: Int) {
        val m = engine.metrics
        val ui = f.ui
        panelTitle(f, px + m.px(28f), py + m.px(40f), engine.s(if (index == 7) "accessories" else "headset"))
        ui.line(f.canvas, "DualSense wireless controller", px + m.px(28f), py + m.px(80f),
            m.textPx(20f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
        ui.circle(f.canvas, px + m.px(34f), py + m.px(112f), m.px(5f), Tok.OnlineGreen)
        ui.line(f.canvas, "Connected · battery 72%", px + m.px(48f), py + m.px(117f),
            m.textPx(17f), Ui.a(Tok.TextSecondary, 0.8f), Ui.Weight.Regular)
        Cards.chip(f, if (engine.sim.bluetoothOn) "Bluetooth on" else "Bluetooth off",
            px + m.px(28f), py + ph - m.px(60f), engine.sim.bluetoothOn) {
            engine.sim.bluetoothOn = !engine.sim.bluetoothOn
            engine.sfx.play(Sfx.Cue.Select)
        }
    }

    private fun genericPanel(f: Frame, px: Float, py: Float, pw: Float, ph: Float, index: Int) {
        val m = engine.metrics
        panelTitle(f, px + m.px(28f), py + m.px(40f), engine.s(glyphLabels[index]))
        Cards.chip(f, "On", px + m.px(28f), py + m.px(70f), true) {
            engine.toast("${engine.s(glyphLabels[index])}: ${engine.s("on")} (simulator)")
        }
        Cards.chip(f, "Off", px + m.px(140f), py + m.px(70f), false) {
            engine.toast("${engine.s(glyphLabels[index])}: ${engine.s("off")} (simulator)")
        }
    }

    // ------------------------------------------------------------------ input
    override fun onDown(x: Float, y: Float): Boolean {
        dragStartY = y
        dragStartX = x
        dragAccum = 0f
        dragging = false
        return false
    }

    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        val m = engine.metrics
        // swipe the card row horizontally
        val cardsTop = m.heightPx - m.px(Ref.CC_CARD_BOTTOM_GAP) - m.px(Ref.CC_CARD_MEDIA_H)
        if (y < cardsTop) return false
        if (abs(dx) > abs(dy) && abs(dx) > 1f) {
            dragging = true
            cardTarget = (cardTarget - dx).coerceAtLeast(0f)
            cardScroll = cardTarget
            dragAccum += dx
            return true
        }
        if (dy > m.px(90f) && expandedGlyph < 0) {
            hide()
            return true
        }
        return false
    }

    override fun onUp(x: Float, y: Float): Boolean {
        dragging = false
        return false
    }

    override fun back(): Boolean {
        if (!visible) return false
        hide()
        return true
    }
}
