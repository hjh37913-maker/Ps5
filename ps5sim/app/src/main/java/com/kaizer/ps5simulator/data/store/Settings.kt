package com.kaizer.ps5simulator.data.store

import android.content.Context
import android.content.SharedPreferences

/** Persisted user preferences. Every toggle in the Settings screen is wired here. */
class Settings(context: Context) {

    private val sp: SharedPreferences =
        context.getSharedPreferences("ps5sim_settings", Context.MODE_PRIVATE)

    enum class Keys(val key: String, val def: Any) {
        LANGUAGE("language", "en"),
        UI_SCALE("ui_scale", 1.0f),
        ANIMATIONS("animations", true),
        SFX_ENABLED("sfx", true),
        SFX_VOLUME("sfx_volume", 0.8f),
        MUSIC_ENABLED("music", true),
        BLUR_QUALITY("blur_quality", 1),          // 0 off, 1 balanced, 2 high
        DYNAMIC_BACKGROUND("dynamic_bg", true),
        SHOW_HINTS("show_hints", true),
        SHOW_CLOCK("show_clock", true),
        HAPTICS("haptics", true),
        REDUCED_MOTION("reduced_motion", false),
        GLASS_ALPHA("glass_alpha", 1.0f),
        START_TAB("start_tab", 0),                // 0 games, 1 media
        PROFILE_NAME("profile_name", "Kaizer"),
        CHECKED_ONBOARDING("onboarding_done", false),
    }

    var language: String
        get() = sp.getString(Keys.LANGUAGE.key, "en") ?: "en"
        set(v) = sp.edit().putString(Keys.LANGUAGE.key, v).apply()

    var uiScale: Float
        get() = sp.getFloat(Keys.UI_SCALE.key, 1.0f)
        set(v) = sp.edit().putFloat(Keys.UI_SCALE.key, v.coerceIn(0.85f, 1.15f)).apply()

    var animations: Boolean
        get() = sp.getBoolean(Keys.ANIMATIONS.key, true)
        set(v) = sp.edit().putBoolean(Keys.ANIMATIONS.key, v).apply()

    var reducedMotion: Boolean
        get() = sp.getBoolean(Keys.REDUCED_MOTION.key, false)
        set(v) = sp.edit().putBoolean(Keys.REDUCED_MOTION.key, v).apply()

    var sfxEnabled: Boolean
        get() = sp.getBoolean(Keys.SFX_ENABLED.key, true)
        set(v) = sp.edit().putBoolean(Keys.SFX_ENABLED.key, v).apply()

    var sfxVolume: Float
        get() = sp.getFloat(Keys.SFX_VOLUME.key, 0.8f)
        set(v) = sp.edit().putFloat(Keys.SFX_VOLUME.key, v.coerceIn(0f, 1f)).apply()

    var musicEnabled: Boolean
        get() = sp.getBoolean(Keys.MUSIC_ENABLED.key, true)
        set(v) = sp.edit().putBoolean(Keys.MUSIC_ENABLED.key, v).apply()

    var blurQuality: Int
        get() = sp.getInt(Keys.BLUR_QUALITY.key, 1)
        set(v) = sp.edit().putInt(Keys.BLUR_QUALITY.key, v.coerceIn(0, 2)).apply()

    var dynamicBackground: Boolean
        get() = sp.getBoolean(Keys.DYNAMIC_BACKGROUND.key, true)
        set(v) = sp.edit().putBoolean(Keys.DYNAMIC_BACKGROUND.key, v).apply()

    var showHints: Boolean
        get() = sp.getBoolean(Keys.SHOW_HINTS.key, true)
        set(v) = sp.edit().putBoolean(Keys.SHOW_HINTS.key, v).apply()

    var showClock: Boolean
        get() = sp.getBoolean(Keys.SHOW_CLOCK.key, true)
        set(v) = sp.edit().putBoolean(Keys.SHOW_CLOCK.key, v).apply()

    var haptics: Boolean
        get() = sp.getBoolean(Keys.HAPTICS.key, true)
        set(v) = sp.edit().putBoolean(Keys.HAPTICS.key, v).apply()

    var glassAlpha: Float
        get() = sp.getFloat(Keys.GLASS_ALPHA.key, 1.0f)
        set(v) = sp.edit().putFloat(Keys.GLASS_ALPHA.key, v.coerceIn(0.5f, 1f)).apply()

    var startTab: Int
        get() = sp.getInt(Keys.START_TAB.key, 0)
        set(v) = sp.edit().putInt(Keys.START_TAB.key, v.coerceIn(0, 1)).apply()

    var profileName: String
        get() = sp.getString(Keys.PROFILE_NAME.key, "Kaizer") ?: "Kaizer"
        set(v) = sp.edit().putString(Keys.PROFILE_NAME.key, v.ifBlank { "Kaizer" }).apply()

    var onboardingDone: Boolean
        get() = sp.getBoolean(Keys.CHECKED_ONBOARDING.key, false)
        set(v) = sp.edit().putBoolean(Keys.CHECKED_ONBOARDING.key, v).apply()

    fun resetAll() {
        sp.edit().clear().apply()
    }

    /** True when a user changed nothing; used for the "Reset to defaults" row. */
    fun isDefault(): Boolean = sp.all.isEmpty()
}
