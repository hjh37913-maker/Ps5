package com.kaizer.ps5simulator.core.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.kaizer.ps5simulator.data.store.Settings

/**
 * UI sound system. Every sound is synthesized by tools/gen_assets.py (no sampled
 * material). Sounds can be disabled entirely from Settings.
 */
class Sfx(private val settings: Settings) {

    enum class Cue { Nav, NavAlt, Select, Back, Open, Close, Error, Launch }

    private var pool: SoundPool? = null
    private val ids = HashMap<Cue, Int>()
    private var loaded = false
    private var vibrator: Vibrator? = null

    fun attach(context: Context) {
        if (pool != null) return
        val attrs = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        val sp = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attrs).build()
        sp.setOnLoadCompleteListener { _, _, status -> if (status == 0) loaded = true }
        val file = mapOf(
            Cue.Nav to "sounds/nav.wav",
            Cue.NavAlt to "sounds/nav_alt.wav",
            Cue.Select to "sounds/select.wav",
            Cue.Back to "sounds/back.wav",
            Cue.Open to "sounds/open.wav",
            Cue.Close to "sounds/close.wav",
            Cue.Error to "sounds/error.wav",
            Cue.Launch to "sounds/launch.wav",
        )
        try {
            file.forEach { (cue, path) ->
                context.assets.openFd(path).use { fd ->
                    ids[cue] = sp.load(fd, 1)
                }
            }
        } catch (t: Throwable) {
            // sounds are optional; the UI must keep working without them
        }
        pool = sp
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun play(cue: Cue, haptic: Boolean = false) {
        if (settings.sfxEnabled) {
            val id = ids[cue]
            if (id != null && id != 0) {
                pool?.play(id, settings.sfxVolume, settings.sfxVolume, 1, 0, 1f)
            }
        }
        if (haptic && settings.haptics) {
            try {
                vibrator?.vibrate(VibrationEffect.createOneShot(8, 40))
            } catch (t: Throwable) {
                // ignore: haptics unavailable
            }
        }
    }

    fun release() {
        pool?.release()
        pool = null
        ids.clear()
        loaded = false
    }
}
