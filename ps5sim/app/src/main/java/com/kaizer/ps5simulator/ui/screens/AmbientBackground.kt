package com.kaizer.ps5simulator.ui.screens

/**
 * Ambient background animation driver (very slow drift/parallax used by the Home
 * spotlight). Kept as a static clock so scenes can sample it without allocations.
 */
object AmbientBackground {
    private var t = 0f
    fun update(dtMs: Long) { t = (t + dtMs / 1000f) % 1000f }
    fun time(): Float = t
}
