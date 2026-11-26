package com.kaizer.ps5simulator.core.gfx

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.util.LruCache
import android.os.Handler
import android.os.Looper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

/**
 * Asynchronous artwork cache.
 *
 * All artwork is packed in the APK assets. Decoding happens on a background dispatcher,
 * down-sampled to roughly the on-screen size, and cached in an LRU sized against the
 * process memory class (a Galaxy A21s gets a 24 MB budget). The UI thread only ever
 * touches an already-decoded bitmap.
 */
object Art {

    private val pool = Executors.newFixedThreadPool(2) { r ->
        Thread(r, "ps5-art").apply { priority = Thread.NORM_PRIORITY - 1 }
    }
    private val main = Handler(Looper.getMainLooper())
    private val inflight = ConcurrentHashMap<String, Boolean>()
    private var cache: LruCache<String, Bitmap>? = null
    private var appContext: Context? = null

    /** Requests that arrived while nothing was decoded yet; triggers invalidation. */
    var invalidate: () -> Unit = {}

    fun init(context: Context) {
        if (cache != null) return
        appContext = context.applicationContext
        val maxKb = (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceIn(16 * 1024, 64 * 1024)
        cache = object : LruCache<String, Bitmap>(maxKb) {
            override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
        }
    }

    private fun key(path: String, w: Int, h: Int) = "$path@${w}x$h"

    fun get(path: String, targetW: Int, targetH: Int): Bitmap? {
        val c = cache ?: return null
        val k = key(path, targetW, targetH)
        c.get(k)?.let { return it }
        if (inflight.putIfAbsent(k, true) == null) {
            pool.execute {
                val bmp = decode(path, targetW, targetH)
                main.post {
                    inflight.remove(k)
                    if (bmp != null) {
                        c.put(k, bmp)
                        invalidate()
                    }
                }
            }
        }
        return null
    }

    /** Drops a single decoded variant (used when the layout size bucket changes). */
    fun evict(pathPrefix: String) {
        val c = cache ?: return
        c.snapshot().keys.filter { it.startsWith(pathPrefix) }.forEach { c.remove(it) }
    }

    private fun decode(path: String, targetW: Int, targetH: Int): Bitmap? {
        val ctx = appContext ?: return null
        val assetPath = path.removePrefix("file:///android_asset/")
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            ctx.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (bounds.outWidth / (sample * 2) >= targetW && bounds.outHeight / (sample * 2) >= targetH &&
                sample < 8
            ) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val bmp = ctx.assets.open(assetPath).use { BitmapFactory.decodeStream(it, null, opts) }
            bmp?.let { b ->
                if (b.config != Bitmap.Config.ARGB_8888 && path.endsWith(".png")) b else b
            }
        } catch (t: Throwable) {
            null
        }
    }

    fun clear() {
        cache?.evictAll()
        inflight.clear()
    }

    fun memoryKb(): Int {
        val c = cache ?: return 0
        return c.snapshot().values.sumOf { it.byteCount / 1024 }
    }
}

/** Bitmap drawing helpers: cover-fit (no stretching) and fit-inside. */
object Draw {

    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val src = Rect()
    private val dst = RectF()

    fun <T> use(block: (Paint) -> T): T = block(paint)

    /** Draws [bmp] cropped to fill the target rect exactly (centre crop). */
    fun cover(c: Canvas, bmp: Bitmap, l: Float, t: Float, r: Float, b: Float, alpha: Float = 1f) {
        if (r <= l || b <= t) return
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val tw = r - l
        val th = b - t
        val scale = maxOf(tw / bw, th / bh)
        val sw = tw / scale
        val sh = th / scale
        val sx = (bw - sw) / 2f
        val sy = (bh - sh) / 2f
        src.set(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
        dst.set(l, t, r, b)
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        c.drawBitmap(bmp, src, dst, paint)
        paint.alpha = 255
    }

    /** Draws [bmp] fully inside the box, preserving aspect ratio, centred. */
    fun inside(c: Canvas, bmp: Bitmap, l: Float, t: Float, r: Float, b: Float, alpha: Float = 1f) {
        if (r <= l || b <= t) return
        val scale = minOf((r - l) / bmp.width, (b - t) / bmp.height)
        val w = bmp.width * scale
        val h = bmp.height * scale
        val cx = (l + r) / 2f
        val cy = (t + b) / 2f
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        c.drawBitmap(bmp, null, RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f), paint)
        paint.alpha = 255
    }

    /** Aspect-fill variant that first crops the supplied source inset (used for hero crops). */
    fun coverBias(
        c: Canvas, bmp: Bitmap, l: Float, t: Float, r: Float, b: Float,
        biasX: Float, biasY: Float, alpha: Float = 1f,
    ) {
        if (r <= l || b <= t) return
        val bw = bmp.width.toFloat()
        val bh = bmp.height.toFloat()
        val tw = r - l
        val th = b - t
        val scale = maxOf(tw / bw, th / bh)
        val sw = tw / scale
        val sh = th / scale
        val sx = (bw - sw) * biasX.coerceIn(0f, 1f)
        val sy = (bh - sh) * biasY.coerceIn(0f, 1f)
        src.set(sx.toInt(), sy.toInt(), (sx + sw).toInt(), (sy + sh).toInt())
        dst.set(l, t, r, b)
        paint.alpha = (alpha * 255).toInt().coerceIn(0, 255)
        c.drawBitmap(bmp, src, dst, paint)
        paint.alpha = 255
    }
}
