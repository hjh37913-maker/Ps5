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
     * Screenshot harness entry point. Used by the CI instrumentation runner and by the
     * debug-only automation scripts; it never exists in release builds.
     */
    private fun applyIntentScenario(v: SimulatorView) {
        val debuggable = (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        if (!debuggable) return
        val action = intent?.action ?: return
        if (action != ACTION_SCENARIO) return
        val scenario = intent.getStringExtra(EXTRA_SCENARIO) ?: "home"
        v.postDelayed({
            when (scenario) {
                "library" -> v.engine.openLibrary()
                "store" -> v.engine.openStore()
                "store_detail" -> v.engine.openHub("gtavi")
                "hub" -> v.engine.openHub("ghostyotei")
                "control_center" -> v.engine.openControlCenter()
                "profile" -> v.engine.openProfile()
                "settings" -> v.engine.openSettings()
                "launch" -> v.engine.openLaunch(v.engine.game("gtavi")!!)
                "home_second" -> v.engine.home?.let { }
                "trophies" -> v.engine.showInfo("Trophies", "Screenshot scenario stub")
                else -> Unit
            }
        }, 450L)
    }

    companion object {
        const val ACTION_SCENARIO = "com.kaizer.ps5simulator.SCENARIO"
        const val EXTRA_SCENARIO = "scenario"
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
