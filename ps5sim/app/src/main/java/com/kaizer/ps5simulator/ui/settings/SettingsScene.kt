package com.kaizer.ps5simulator.ui.settings

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
import com.kaizer.ps5simulator.ui.components.Cards
import com.kaizer.ps5simulator.ui.components.Chrome

/**
 * SETTINGS — console-style settings list with live, persisted switches.
 *
 * Sections mirror the console tree (console / audio / display / system) but every row
 * here maps to a real preference in [com.kaizer.ps5simulator.data.store.Settings].
 */
class SettingsScene(private val engine: Engine) : Scene {

    override val id: Int = 3106

    private var reveal = 0f
    private var scroll = 0f
    private var target = 0f
    private var dragStart = 0f
    private var scrollStart = 0f

    override fun isAnimating(): Boolean = reveal < 1f

    override fun onShow() { reveal = 0f }

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (engine.animationsOn) reveal = (reveal + f.dtMs / 260f).coerceAtMost(1f) else reveal = 1f
        val a = Ease.soft(reveal)

        // Dimmed console behind the sheet: keeps the screen in the console's visual family
        // instead of looking like a bare Android settings page.
        val bg = engine.selectedGame()
        if (bg != null) {
            val bmp = com.kaizer.ps5simulator.core.gfx.Art.get(
                bg.artwork, m.widthPx.toInt(), m.heightPx.toInt())
            if (bmp != null) {
                com.kaizer.ps5simulator.core.gfx.Draw.cover(c, bmp, 0f, 0f, m.widthPx, m.heightPx)
            }
        }
        // translucent vertical grade so the sheet still reads as glass over the console
        ui.gradientRect(c, 0f, 0f, m.widthPx, m.heightPx, 0xD90A0B0F.toInt(), 0xF207080B.toInt())
        Chrome.backRow(f, engine.s("settings"))

        ui.withAlpha(a) {
            val left = engine.gutter
            val right = m.safeRight - engine.gutter
            ui.line(c, engine.s("settings"), left, m.px(120f), m.textPx(50f), 0xFFFFFFFF.toInt(),
                Ui.Weight.Display)
            ui.line(c, engine.s("settings_sub"), left, m.px(156f), m.textPx(21f),
                Ui.a(Tok.TextSecondary, 0.7f), Ui.Weight.Regular)

            var y = m.px(196f) - scroll
            val rowH = m.px(68f)

            fun section(title: String) {
                y += m.px(28f)
                ui.line(c, title, left, y, m.textPx(20f), Ui.a(Tok.TextSecondary, 0.6f),
                    Ui.Weight.Medium, letterSpacing = 0.06f)
                y += m.px(22f)
            }

            fun toggleRow(title: String, on: Boolean, change: () -> Unit) {
                Cards.row(f, left, right, y, rowH, title, null, chevron = false, onTap = {
                    change(); engine.sfx.play(Sfx.Cue.Select)
                })
                val tw = m.px(64f)
                val th = m.px(34f)
                val tw2 = right - m.px(4f) - tw
                Cards.toggle(f, tw2, y + (rowH - th) / 2f, on, tw, th)
                if (engine.hits.pick(tw2, y + rowH / 2f) < 0) {
                    engine.hits.add(tw2, y, right, y + rowH, 18000 + title.hashCode()) {
                        change(); engine.sfx.play(Sfx.Cue.Select)
                    }
                }
                Chrome.divider(f, left, right, y + rowH, 0.06f)
                y += rowH
            }

            fun valueRow(title: String, value: String, tap: () -> Unit) {
                Cards.row(f, left, right, y, rowH, title, value, chevron = false, onTap = { tap() })
                Chrome.divider(f, left, right, y + rowH, 0.06f)
                y += rowH
            }

            ui.clip(c, 0f, m.px(196f), m.widthPx, m.heightPx - m.px(20f)) {

            section(engine.s("profile").uppercase())
            valueRow(engine.s("profile_name"), engine.settings.profileName) {
                engine.showTextInput("Profile name", engine.settings.profileName) { v ->
                    engine.settings.profileName = v
                    engine.toast("Profile name updated")
                }
            }
            valueRow(engine.s("language"), languageLabel()) {
                cycleLanguage()
            }
            valueRow(engine.s("start_tab"), if (engine.settings.startTab == 0) engine.s("games") else engine.s("media")) {
                engine.settings.startTab = 1 - engine.settings.startTab
                engine.sfx.play(Sfx.Cue.Select)
            }

            section("INTERFACE")
            toggleRow(engine.s("animations"), engine.settings.animations) {
                engine.settings.animations = !engine.settings.animations
            }
            toggleRow(engine.s("reduced_motion"), engine.settings.reducedMotion) {
                engine.settings.reducedMotion = !engine.settings.reducedMotion
            }
            toggleRow(engine.s("dynamic_background"), engine.settings.dynamicBackground) {
                engine.settings.dynamicBackground = !engine.settings.dynamicBackground
            }
            toggleRow(engine.s("show_clock"), engine.settings.showClock) {
                engine.settings.showClock = !engine.settings.showClock
            }
            toggleRow(engine.s("show_hints"), engine.settings.showHints) {
                engine.settings.showHints = !engine.settings.showHints
            }
            valueRow(engine.s("ui_scale"), "${(engine.settings.uiScale * 100).toInt()}%") {
                val next = engine.settings.uiScale + 0.05f
                engine.settings.uiScale = if (next > 1.15f) 0.90f else next
                engine.applyUiScale()
                engine.sfx.play(Sfx.Cue.Select)
            }

            section("AUDIO")
            toggleRow(engine.s("sfx"), engine.settings.sfxEnabled) {
                engine.settings.sfxEnabled = !engine.settings.sfxEnabled
                if (engine.settings.sfxEnabled) engine.sfx.play(Sfx.Cue.Select)
            }
            valueRow(engine.s("volume"), "${(engine.settings.sfxVolume * 100).toInt()}%") {
                engine.settings.sfxVolume =
                    if (engine.settings.sfxVolume >= 0.95f) 0.2f else engine.settings.sfxVolume + 0.2f
                engine.sfx.play(Sfx.Cue.Select)
            }
            toggleRow(engine.s("haptics"), engine.settings.haptics) {
                engine.settings.haptics = !engine.settings.haptics
            }
            toggleRow(engine.s("music"), engine.settings.musicEnabled) {
                engine.settings.musicEnabled = !engine.settings.musicEnabled
                engine.sim.musicPlaying = engine.settings.musicEnabled
            }

            // touch adaptation: optional DualSense overlay
            Cards.row(f, left, right, y, rowH, engine.s("virtual_pad"), null, chevron = false,
                onTap = { engine.toggleVirtualPad() })
            val vpw = m.px(64f)
            val vph = m.px(34f)
            Cards.toggle(f, right - m.px(4f) - vpw, y + (rowH - vph) / 2f,
                engine.virtualPadVisible, vpw, vph)
            Chrome.divider(f, left, right, y + rowH, 0.06f)
            y += rowH

            section("SYSTEM")
            valueRow(engine.s("about"), "Version 2.0.0") {
                engine.showInfo(
                    "PS5 Interface Simulator",
                    "An interactive, original reconstruction of the PlayStation 5 interface " +
                        "built for Android. This app is a fan project: it does not run real " +
                        "games or connect to PSN. All artwork is original concept art generated " +
                        "for this project; fonts are Manrope and Figtree (SIL OFL).",
                )
            }
            valueRow(engine.s("reset"), engine.s("restore_defaults")) {
                engine.confirm("Reset preferences", "All simulator settings return to defaults. Your game library stays untouched.", "Reset") {
                    engine.settings.resetAll()
                    engine.applyUiScale()
                    engine.toast("Preferences reset")
                }
            }
            }
            y += m.px(40f)
            ui.line(c, "PS5 Interface Simulator · fan project · no affiliation with Sony",
                left, y, m.textPx(16f), Ui.a(Tok.TextTertiary, 0.6f), Ui.Weight.Regular)
            y += m.px(40f)

            val content = y + scroll - m.px(210f)
            val maxScroll = (content - (m.heightPx - m.px(210f) - m.px(20f))).coerceAtLeast(0f)
            target = target.coerceIn(0f, maxScroll)
            scroll += (target - scroll) * (1f - kotlin.math.exp(-16f * f.dtMs / 1000f))
        }
    }

    private fun languageLabel(): String = when (engine.settings.language) {
        "ru" -> "Русский"
        "uk" -> "Українська"
        else -> "English"
    }

    private fun cycleLanguage() {
        engine.settings.language = when (engine.settings.language) {
            "en" -> "uk"
            "uk" -> "ru"
            else -> "en"
        }
        engine.sfx.play(Sfx.Cue.Select)
        engine.toast("Language: ${languageLabel()}")
    }

    override fun onDown(x: Float, y: Float): Boolean {
        dragStart = y
        scrollStart = target
        return false
    }

    override fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean {
        if (kotlin.math.abs(dy) > 2f) {
            target = (scrollStart - (y - dragStart)).coerceAtLeast(0f)
            scroll = target
            return true
        }
        return false
    }
}
