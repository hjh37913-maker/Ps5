package com.kaizer.ps5simulator.platform

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.view.View
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a view into a PNG without root/screencap permissions. Used by the CI
 * instrumentation harness to produce fidelity screenshots for every screen.
 */
object ScreenCapture {
    fun save(view: View, file: File): Boolean = try {
        val w = view.width.coerceAtLeast(1)
        val h = view.height.coerceAtLeast(1)
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.BLACK)
        view.draw(canvas)
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bmp.recycle()
        true
    } catch (t: Throwable) {
        false
    }
}
