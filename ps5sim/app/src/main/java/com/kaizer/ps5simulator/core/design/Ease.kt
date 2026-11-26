package com.kaizer.ps5simulator.core.design

/** Cubic-bezier evaluation used by hand-rolled animations. */
object Ease {
    /** Solves x(t)=x for t, then returns y(t). Standard CSS timing function maths. */
    fun bezier(ctrl: FloatArray, x: Float): Float {
        val cx = x.coerceIn(0f, 1f)
        val x1 = ctrl[0]; val y1 = ctrl[1]; val x2 = ctrl[2]; val y2 = ctrl[3]
        fun bx(t: Float): Float {
            val mt = 1 - t
            return 3 * mt * mt * t * x1 + 3 * mt * t * t * x2 + t * t * t
        }
        fun by(t: Float): Float {
            val mt = 1 - t
            return 3 * mt * mt * t * y1 + 3 * mt * t * t * y2 + t * t * t
        }
        var lo = 0f
        var hi = 1f
        var t = cx
        repeat(12) {
            val v = bx(t)
            if (v < cx) lo = t else hi = t
            t = (lo + hi) / 2f
        }
        return by(t)
    }

    fun out(t: Float): Float = bezier(Tok.EaseConsole, t)
    fun into(t: Float): Float = bezier(Tok.EaseInConsole, t)
    fun soft(t: Float): Float = bezier(Tok.EaseSoft, t)

    /** Frame-rate independent approach factor for exponential smoothing. */
    fun approach(current: Float, target: Float, ratePerSecond: Float, dtMs: Long): Float {
        if (kotlin.math.abs(target - current) < 0.0005f) return target
        val k = 1f - kotlin.math.exp(-ratePerSecond * dtMs / 1000f)
        return current + (target - current) * k
    }
}
