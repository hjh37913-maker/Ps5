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

    override fun onShow() { reveal = 0f }

    override fun draw(f: Frame) {
        val m = engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (engine.animationsOn) reveal = (reveal + f.dtMs / 260f).coerceAtMost(1f) else reveal = 1f
        val a = Ease.soft(reveal)

        ui.gradientRect(c, 0f, 0f, m.widthPx, m.heightPx, 0xFF12141C.toInt(), 0xFF06070A.toInt())
        ui.glow(c, m.widthPx * 0.2f, m.px(80f), m.widthPx * 0.45f, 0x1A4D8DF6.toInt())
        Chrome.backRow(f, "Settings")

        ui.withAlpha(a) {
            val left = engine.gutter
            val right = m.safeRight - engine.gutter
            ui.line(c, "Settings", left, m.px(120f), m.textPx(50f), 0xFFFFFFFF.toInt(),
                Ui.Weight.Display)
            ui.line(c, "Changes are saved to this device", left, m.px(156f), m.textPx(21f),
                Ui.a(Tok.TextSecondary, 0.7f), Ui.Weight.Regular)

            var y = m.px(210f) - scroll
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

            section("CONSOLE")
            valueRow("Profile name", engine.settings.profileName) {
                engine.showTextInput("Profile name", engine.settings.profileName) { v ->
                    engine.settings.profileName = v
                    engine.toast("Profile name updated")
                }
            }
            valueRow("Language", languageLabel()) {
                cycleLanguage()
            }
            valueRow("Start tab", if (engine.settings.startTab == 0) "Games" else "Media") {
                engine.settings.startTab = 1 - engine.settings.startTab
                engine.sfx.play(Sfx.Cue.Select)
            }

            section("INTERFACE")
            toggleRow("Animations", engine.settings.animations) {
                engine.settings.animations = !engine.settings.animations
            }
            toggleRow("Reduced motion", engine.settings.reducedMotion) {
                engine.settings.reducedMotion = !engine.settings.reducedMotion
            }
            toggleRow("Dynamic background", engine.settings.dynamicBackground) {
                engine.settings.dynamicBackground = !engine.settings.dynamicBackground
            }
            toggleRow("Show clock", engine.settings.showClock) {
                engine.settings.showClock = !engine.settings.showClock
            }
            toggleRow("Show hints", engine.settings.showHints) {
                engine.settings.showHints = !engine.settings.showHints
            }
            valueRow("UI scale", "${(engine.settings.uiScale * 100).toInt()}%") {
                val next = engine.settings.uiScale + 0.05f
                engine.settings.uiScale = if (next > 1.15f) 0.90f else next
                engine.applyUiScale()
                engine.sfx.play(Sfx.Cue.Select)
            }

            section("AUDIO & FEEDBACK")
            toggleRow("UI sound effects", engine.settings.sfxEnabled) {
                engine.settings.sfxEnabled = !engine.settings.sfxEnabled
                if (engine.settings.sfxEnabled) engine.sfx.play(Sfx.Cue.Select)
            }
            valueRow("Sound volume", "${(engine.settings.sfxVolume * 100).toInt()}%") {
                engine.settings.sfxVolume =
                    if (engine.settings.sfxVolume >= 0.95f) 0.2f else engine.settings.sfxVolume + 0.2f
                engine.sfx.play(Sfx.Cue.Select)
            }
            toggleRow("Haptics", engine.settings.haptics) {
                engine.settings.haptics = !engine.settings.haptics
            }
            toggleRow("Music", engine.settings.musicEnabled) {
                engine.settings.musicEnabled = !engine.settings.musicEnabled
                engine.sim.musicPlaying = engine.settings.musicEnabled
            }

            section("SYSTEM")
            valueRow("About this app", "Version 2.0.0") {
                engine.showInfo(
                    "PS5 Interface Simulator",
                    "An interactive, original reconstruction of the PlayStation 5 interface " +
                        "built for Android. This app is a fan project: it does not run real " +
                        "games or connect to PSN. All artwork is original concept art generated " +
                        "for this project; fonts are Manrope and Figtree (SIL OFL).",
                )
            }
            valueRow("Reset preferences", "Restore defaults") {
                engine.confirm("Reset preferences", "All simulator settings return to defaults. Your game library stays untouched.", "Reset") {
                    engine.settings.resetAll()
                    engine.applyUiScale()
                    engine.toast("Preferences reset")
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
