package com.kaizer.ps5simulator

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import com.kaizer.ps5simulator.platform.SimulatorView

/**
 * Single activity host. The simulator is one hardware-accelerated custom view so the
 * interface keeps a console-tight frame budget and never depends on Android widgets.
 */
class MainActivity : Activity() {

    private var view: SimulatorView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
        )
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
        }
        hideSystemBars()
        val v = SimulatorView(this)
        view = v
        setContentView(v)
        applyIntentScenario(v)
    }

    /**
     * Debug-only navigation hook used by the CI screenshot harness:
     *
     *   adb shell am start -n com.kaizer.ps5simulator/.MainActivity --es scenario hub
     *
     * Release builds ignore it. It never alters product behaviour.
     */
    private fun applyIntentScenario(v: SimulatorView) {
        val debuggable = (applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) return
        val scenario = intent?.getStringExtra(EXTRA_SCENARIO) ?: return
        val gameId = intent.getStringExtra(EXTRA_GAME)
        v.postDelayed({
            val e = v.engine
            if (gameId != null) e.sim.selectedGameId = gameId
            when (scenario) {
                "home" -> Unit
                "home_second" -> e.home?.selectGameByIndex(2)
                "hub" -> e.openHub(gameId ?: "ghostyotei")
                "library" -> e.openLibrary()
                "store" -> e.openStore()
                "control_center" -> e.openControlCenter()
                "profile" -> e.openProfile()
                "settings" -> e.openSettings()
                "trophies" -> e.openTrophies()
                "media" -> e.openMedia()
                "gallery" -> e.game(gameId ?: "gtavi")?.let {
                    e.push(com.kaizer.ps5simulator.ui.hub.GalleryScene(e, it.id), com.kaizer.ps5simulator.core.engine.Sfx.Cue.Select)
                }
                "launch" -> e.game(gameId ?: "gtavi")?.let { e.openLaunch(it) }
            }
        }, 400L)
    }

    companion object {
        const val EXTRA_SCENARIO = "scenario"
        const val EXTRA_GAME = "game"
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    private fun hideSystemBars() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            )
    }

    override fun onBackPressed() {
        val v = view
        if (v == null || !v.handleBack()) {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }
}
