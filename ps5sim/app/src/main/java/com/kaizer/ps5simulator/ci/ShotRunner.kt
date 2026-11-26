package com.kaizer.ps5simulator.ci

import android.app.Activity
import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import com.kaizer.ps5simulator.MainActivity
import com.kaizer.ps5simulator.platform.ScreenCapture
import com.kaizer.ps5simulator.platform.SimulatorView
import java.io.File

/**
 * Self-contained instrumentation runner that renders every screen to a PNG.
 *
 * It intentionally depends on nothing but the Android framework: the CI build uses a
 * hand-rolled aapt2 + kotlinc chain, so androidx.test is not on the classpath. Run with:
 *
 *   adb shell am instrument -w com.kaizer.ps5simulator/.ci.ShotRunner
 *
 * PNGs land in the app's external files dir under shots/.
 */
class ShotRunner : Instrumentation() {

    /** scenario name -> game id used for artwork-dependent screens */
    private val scenarios = listOf(
        "home" to "gtavi",
        "home_second" to "spiderman2",
        "hub" to "ghostyotei",
        "library" to "gtavi",
        "control_center" to "ghostyotei",
        "profile" to "gtavi",
        "settings" to "gtavi",
        "store" to "gtavi",
        "launch" to "gtavi",
        "trophies" to "gtavi",
    )

    override fun onCreate(arguments: Bundle?) {
        super.onCreate(arguments)
        start()
    }

    override fun onStart() {
        val results = StringBuilder()
        val outDir = File(targetContext.getExternalFilesDir(null), "shots")
        outDir.mkdirs()
        var ok = 0
        scenarios.forEach { (scenario, gameId) ->
            var activity: MainActivity? = null
            try {
                val intent = Intent(targetContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    action = MainActivity.ACTION_SCENARIO
                    putExtra(MainActivity.EXTRA_SCENARIO, scenario)
                    putExtra("game", gameId)
                }
                activity = startActivitySync(intent) as? MainActivity
                // let layout metrics settle, artwork decode, and enter animations finish
                SystemClock.sleep(2200L)
                var view: SimulatorView? = null
                runOnMainSync {
                    val decor = activity?.window?.decorView ?: return@runOnMainSync
                    view = decor.findViewWithTag(SimulatorView.TAG_SIMULATOR) as? SimulatorView
                }
                val target = view
                if (target != null) {
                    var saved = false
                    runOnMainSync { saved = ScreenCapture.save(target, File(outDir, "$scenario.png")) }
                    results.appendLine("$scenario=${if (saved) "ok" else "save_failed"}")
                    if (saved) ok++
                } else {
                    results.appendLine("$scenario=view_missing")
                }
            } catch (t: Throwable) {
                results.appendLine("$scenario=error:${t.javaClass.simpleName}:${t.message}")
            } finally {
                if (activity != null) {
                    runOnMainSync { activity?.finish() }
                    SystemClock.sleep(400L)
                }
            }
        }
        val summary = "captured=$ok/${scenarios.size}\n$results"
        File(outDir, "summary.txt").writeText(summary)
        val bundle = Bundle().apply {
            putString(SUMMARY_KEY, summary)
            putBoolean(STATUS_KEY, ok == scenarios.size)
        }
        finish(if (ok == scenarios.size) Activity.RESULT_OK else Activity.RESULT_CANCELED, bundle)
    }

    companion object {
        private val ANDROID_CONTENT_ID = 0x01020002 // android.R.id.content
        const val SUMMARY_KEY = "summary"
        const val STATUS_KEY = "all_ok"
    }
}
