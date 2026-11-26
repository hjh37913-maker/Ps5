package com.kaizer.ps5simulator.ui.screens

import android.graphics.Canvas
import android.graphics.Color

/** Full-screen backdrop: deep navy black with a subtle vertical lift, matching console. */
class Backdrop {
    fun draw(c: Canvas, w: Float, h: Float) {
        c.drawColor(Color.BLACK)
    }
}
