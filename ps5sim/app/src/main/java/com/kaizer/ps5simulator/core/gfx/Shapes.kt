package com.kaizer.ps5simulator.core.engine

import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF

/**
 * Vector glyph set drawn in the console's minimal-line style. Original drawings —
 * proportions matched to the shapes visible in interface stills (home, bell, music,
 * speaker, mic, controller, headset, power, wifi, bluetooth, volume …).
 */
enum class Glyph {
    Home, Library, Profile, Friends, Bell, Party, Music, Speaker, Mic, Controller,
    Headset, Power, Wifi, Bluetooth, Search, Settings, Menu, Play, Pause, Next, Prev,
    ChevronRight, ChevronDown, Plus, Check, Star, Trophy, Screen, Clock, Info, Trash,
    Globe, Shield, Install, Sort, Filter, Camera, Broadcast, VolumeMute, Keyboard,
    Accessibility, Store, GamePad, Activity, ArrowBack,
}

object Icons {

    private val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val f = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    private val path = Path()

    /**
     * Draws [glyph] in a square of [size] centred at (cx, cy). [stroke] is the line
     * width; the icon geometry is authored on a 24x24 grid.
     */
    fun draw(c: Canvas, glyph: Glyph, cx: Float, cy: Float, size: Float, color: Int,
             stroke: Float = size * 0.075f, filled: Boolean = false) {
        val s = size / 24f
        p.color = color
        p.strokeWidth = stroke
        f.color = color
        c.save()
        c.translate(cx - size / 2f, cy - size / 2f)
        c.scale(s, s)
        path.reset()
        when (glyph) {
            Glyph.Home -> {
                path.moveTo(3f, 11.2f); path.lineTo(12f, 3.6f); path.lineTo(21f, 11.2f)
                path.lineTo(21f, 20.4f); path.lineTo(14.4f, 20.4f); path.lineTo(14.4f, 14.4f)
                path.lineTo(9.6f, 14.4f); path.lineTo(9.6f, 20.4f); path.lineTo(3f, 20.4f)
                path.close()
                c.drawPath(path, p)
            }
            Glyph.Library -> {
                c.drawRoundRect(3.5f, 5f, 9.5f, 19f, 1.2f, 1.2f, p)
                c.drawRoundRect(14.5f, 5f, 20.5f, 19f, 1.2f, 1.2f, p)
            }
            Glyph.Profile -> {
                c.drawCircle(12f, 8.4f, 3.8f, p)
                path.moveTo(4.6f, 20.4f)
                path.quadTo(12f, 12.6f, 19.4f, 20.4f)
                c.drawPath(path, p)
            }
            Glyph.Friends -> {
                c.drawCircle(9f, 8.6f, 3.3f, p)
                path.moveTo(2.6f, 19.6f); path.quadTo(9f, 13.2f, 15.4f, 19.6f)
                c.drawPath(path, p)
                c.drawCircle(17.2f, 10f, 2.6f, p)
                path.moveTo(17f, 15.4f); path.quadTo(20.6f, 15.8f, 21.8f, 18.8f)
                c.drawPath(path, p)
            }
            Glyph.Bell -> {
                path.moveTo(5.4f, 17.4f)
                path.lineTo(5.4f, 10.6f)
                path.quadTo(5.4f, 5.4f, 12f, 5.4f)
                path.quadTo(18.6f, 5.4f, 18.6f, 10.6f)
                path.lineTo(18.6f, 17.4f)
                path.lineTo(5.4f, 17.4f)
                path.close()
                c.drawPath(path, p)
                c.drawCircle(12f, 19.6f, 1.8f, p)
            }
            Glyph.Party -> {
                c.drawCircle(9f, 8.4f, 3.2f, p)
                path.moveTo(3f, 19.2f); path.quadTo(9f, 13.4f, 15f, 19.2f)
                c.drawPath(path, p)
                c.drawCircle(17.4f, 9f, 2.6f, p)
                path.moveTo(18.4f, 3f); path.lineTo(18.4f, 5.2f)
                path.moveTo(18.4f, 12.8f); path.lineTo(18.4f, 15f)
                path.moveTo(15f, 9f); path.lineTo(16.8f, 9f)
                path.moveTo(20f, 9f); path.lineTo(21.8f, 9f)
                c.drawPath(path, p)
            }
            Glyph.Music -> {
                path.moveTo(9.4f, 17.2f); path.lineTo(9.4f, 5.8f); path.lineTo(19f, 4f)
                path.lineTo(19f, 15.4f)
                c.drawPath(path, p)
                c.drawCircle(7f, 17.6f, 2.5f, p)
                c.drawCircle(16.6f, 15.8f, 2.5f, p)
            }
            Glyph.Speaker -> {
                path.moveTo(4f, 9.4f); path.lineTo(8f, 9.4f); path.lineTo(12.6f, 5.2f)
                path.lineTo(12.6f, 18.8f); path.lineTo(8f, 14.6f); path.lineTo(4f, 14.6f)
                path.close()
                c.drawPath(path, p)
                path.moveTo(15.8f, 9.2f); path.quadTo(17.6f, 12f, 15.8f, 14.8f)
                path.moveTo(18.4f, 6.8f); path.quadTo(21.6f, 12f, 18.4f, 17.2f)
                c.drawPath(path, p)
            }
            Glyph.VolumeMute -> {
                path.moveTo(4f, 9.4f); path.lineTo(8f, 9.4f); path.lineTo(12.6f, 5.2f)
                path.lineTo(12.6f, 18.8f); path.lineTo(8f, 14.6f); path.lineTo(4f, 14.6f)
                path.close()
                c.drawPath(path, p)
                path.moveTo(16f, 9.4f); path.lineTo(21f, 14.6f)
                path.moveTo(21f, 9.4f); path.lineTo(16f, 14.6f)
                c.drawPath(path, p)
            }
            Glyph.Mic -> {
                c.drawRoundRect(9.2f, 3f, 14.8f, 13f, 2.8f, 2.8f, p)
                path.moveTo(6.4f, 11.6f); path.quadTo(6.4f, 16.4f, 12f, 16.4f)
                path.quadTo(17.6f, 16.4f, 17.6f, 11.6f)
                path.moveTo(12f, 16.6f); path.lineTo(12f, 20.4f)
                path.moveTo(8.6f, 20.6f); path.lineTo(15.4f, 20.6f)
                c.drawPath(path, p)
            }
            Glyph.Controller -> {
                path.moveTo(7.4f, 7.4f)
                path.lineTo(16.6f, 7.4f)
                path.quadTo(21.4f, 7.6f, 21.6f, 13.2f)
                path.quadTo(21.8f, 18.4f, 18.8f, 18.4f)
                path.quadTo(16.6f, 18.4f, 15.2f, 15.6f)
                path.lineTo(8.8f, 15.6f)
                path.quadTo(7.4f, 18.4f, 5.2f, 18.4f)
                path.quadTo(2.2f, 18.4f, 2.4f, 13.2f)
                path.quadTo(2.6f, 7.6f, 7.4f, 7.4f)
                path.close()
                c.drawPath(path, p)
                path.moveTo(6.6f, 10.4f); path.lineTo(6.6f, 13.6f)
                path.moveTo(5f, 12f); path.lineTo(8.2f, 12f)
                c.drawPath(path, p)
                c.drawCircle(16.6f, 10.8f, 0.9f, f)
                c.drawCircle(18.4f, 12.8f, 0.9f, f)
            }
            Glyph.Headset -> {
                path.moveTo(4.4f, 14.2f)
                path.lineTo(4.4f, 11.6f)
                path.quadTo(4.4f, 4.8f, 12f, 4.8f)
                path.quadTo(19.6f, 4.8f, 19.6f, 11.6f)
                path.lineTo(19.6f, 14.2f)
                c.drawPath(path, p)
                c.drawRoundRect(2.8f, 13f, 7f, 19.4f, 2f, 2f, p)
                c.drawRoundRect(17f, 13f, 21.2f, 19.4f, 2f, 2f, p)
            }
            Glyph.Power -> {
                path.moveTo(12f, 3.4f); path.lineTo(12f, 11.4f)
                path.moveTo(7f, 5.8f)
                path.quadTo(3.8f, 8.4f, 3.8f, 13.2f)
                path.quadTo(3.8f, 20.2f, 12f, 20.2f)
                path.quadTo(20.2f, 20.2f, 20.2f, 13.2f)
                path.quadTo(20.2f, 8.4f, 17f, 5.8f)
                c.drawPath(path, p)
            }
            Glyph.Wifi -> {
                path.moveTo(3f, 9.4f)
                path.quadTo(12f, 1.8f, 21f, 9.4f)
                path.moveTo(6.2f, 13f)
                path.quadTo(12f, 8f, 17.8f, 13f)
                path.moveTo(9.4f, 16.6f)
                path.quadTo(12f, 14.6f, 14.6f, 16.6f)
                c.drawPath(path, p)
                c.drawCircle(12f, 19.8f, 1.4f, f)
            }
            Glyph.Bluetooth -> {
                path.moveTo(8f, 7.4f); path.lineTo(16f, 16.4f); path.lineTo(12f, 20.4f)
                path.lineTo(12f, 3.6f); path.lineTo(16f, 7.6f); path.lineTo(8f, 16.6f)
                c.drawPath(path, p)
            }
            Glyph.Search -> {
                c.drawCircle(10.6f, 10.6f, 6.4f, p)
                path.moveTo(15.4f, 15.4f); path.lineTo(20.6f, 20.6f)
                c.drawPath(path, p)
            }
            Glyph.Settings -> {
                c.drawCircle(12f, 12f, 3.2f, p)
                for (i in 0 until 8) {
                    val a = Math.toRadians(i * 45.0)
                    val cx1 = 12f + (Math.cos(a) * 6.4f).toFloat()
                    val cy1 = 12f + (Math.sin(a) * 6.4f).toFloat()
                    val cx2 = 12f + (Math.cos(a) * 9.2f).toFloat()
                    val cy2 = 12f + (Math.sin(a) * 9.2f).toFloat()
                    path.moveTo(cx1, cy1); path.lineTo(cx2, cy2)
                }
                c.drawPath(path, p)
            }
            Glyph.Menu -> {
                path.moveTo(4f, 7f); path.lineTo(20f, 7f)
                path.moveTo(4f, 12f); path.lineTo(20f, 12f)
                path.moveTo(4f, 17f); path.lineTo(20f, 17f)
                c.drawPath(path, p)
            }
            Glyph.Play -> {
                path.moveTo(8f, 5.2f); path.lineTo(19f, 12f); path.lineTo(8f, 18.8f)
                path.close()
                c.drawPath(path, f)
            }
            Glyph.Pause -> {
                c.drawRoundRect(7f, 5f, 10.2f, 19f, 1f, 1f, f)
                c.drawRoundRect(13.8f, 5f, 17f, 19f, 1f, 1f, f)
            }
            Glyph.Next -> {
                path.moveTo(6f, 5.6f); path.lineTo(15f, 12f); path.lineTo(6f, 18.4f)
                path.close(); c.drawPath(path, f)
                c.drawRoundRect(16.4f, 5.6f, 18.6f, 18.4f, 1f, 1f, f)
            }
            Glyph.Prev -> {
                path.moveTo(18f, 5.6f); path.lineTo(9f, 12f); path.lineTo(18f, 18.4f)
                path.close(); c.drawPath(path, f)
                c.drawRoundRect(5.4f, 5.6f, 7.6f, 18.4f, 1f, 1f, f)
            }
            Glyph.ChevronRight -> {
                path.moveTo(9.4f, 5.6f); path.lineTo(15.8f, 12f); path.lineTo(9.4f, 18.4f)
                c.drawPath(path, p)
            }
            Glyph.ChevronDown -> {
                path.moveTo(5.6f, 9.4f); path.lineTo(12f, 15.8f); path.lineTo(18.4f, 9.4f)
                c.drawPath(path, p)
            }
            Glyph.ArrowBack -> {
                path.moveTo(19f, 12f); path.lineTo(5.4f, 12f)
                path.moveTo(11f, 5.6f); path.lineTo(4.8f, 12f); path.lineTo(11f, 18.4f)
                c.drawPath(path, p)
            }
            Glyph.Plus -> {
                path.moveTo(12f, 5f); path.lineTo(12f, 19f)
                path.moveTo(5f, 12f); path.lineTo(19f, 12f)
                c.drawPath(path, p)
            }
            Glyph.Check -> {
                path.moveTo(5f, 12.6f); path.lineTo(10f, 17.6f); path.lineTo(19.4f, 6.6f)
                c.drawPath(path, p)
            }
            Glyph.Star -> {
                for (i in 0 until 10) {
                    val a = Math.toRadians(-90.0 + i * 36.0)
                    val rr = if (i % 2 == 0) 9.4f else 4.2f
                    val x = 12f + (Math.cos(a) * rr).toFloat()
                    val y = 12f + (Math.sin(a) * rr).toFloat()
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                path.close(); c.drawPath(path, f)
            }
            Glyph.Trophy -> {
                path.moveTo(8f, 4.4f); path.lineTo(16f, 4.4f); path.lineTo(16f, 10.4f)
                path.quadTo(16f, 14.4f, 12f, 14.4f)
                path.quadTo(8f, 14.4f, 8f, 10.4f)
                path.close()
                c.drawPath(path, p)
                path.moveTo(8f, 5.8f); path.quadTo(4.6f, 5.8f, 5.4f, 9f)
                path.quadTo(5.8f, 10.6f, 8f, 10.6f)
                path.moveTo(16f, 5.8f); path.quadTo(19.4f, 5.8f, 18.6f, 9f)
                path.quadTo(18.2f, 10.6f, 16f, 10.6f)
                path.moveTo(12f, 14.6f); path.lineTo(12f, 18.4f)
                path.moveTo(8f, 20f); path.lineTo(16f, 20f)
                path.moveTo(9f, 18.4f); path.lineTo(15f, 18.4f)
                c.drawPath(path, p)
            }
            Glyph.Screen -> {
                c.drawRoundRect(3f, 5f, 21f, 16.4f, 1.6f, 1.6f, p)
                path.moveTo(8.4f, 20f); path.lineTo(15.6f, 20f)
                path.moveTo(12f, 16.6f); path.lineTo(12f, 20f)
                c.drawPath(path, p)
            }
            Glyph.Clock -> {
                c.drawCircle(12f, 12f, 8.4f, p)
                path.moveTo(12f, 6.8f); path.lineTo(12f, 12.2f); path.lineTo(16f, 14.4f)
                c.drawPath(path, p)
            }
            Glyph.Info -> {
                c.drawCircle(12f, 12f, 8.6f, p)
                path.moveTo(12f, 10.8f); path.lineTo(12f, 16.6f)
                c.drawPath(path, p)
                c.drawCircle(12f, 7.6f, 1.15f, f)
            }
            Glyph.Trash -> {
                path.moveTo(4.6f, 7f); path.lineTo(19.4f, 7f)
                path.moveTo(9f, 7f); path.lineTo(9f, 4.4f); path.lineTo(15f, 4.4f); path.lineTo(15f, 7f)
                path.moveTo(6.4f, 7.4f); path.lineTo(7.4f, 20f); path.lineTo(16.6f, 20f)
                path.lineTo(17.6f, 7.4f)
                c.drawPath(path, p)
            }
            Glyph.Globe -> {
                c.drawCircle(12f, 12f, 8.6f, p)
                path.moveTo(12f, 3.4f); path.quadTo(6f, 12f, 12f, 20.6f)
                path.quadTo(18f, 12f, 12f, 3.4f)
                path.moveTo(3.6f, 12f); path.lineTo(20.4f, 12f)
                c.drawPath(path, p)
            }
            Glyph.Shield -> {
                path.moveTo(12f, 3.2f); path.lineTo(19.6f, 6.4f); path.lineTo(19.6f, 12.4f)
                path.quadTo(19.6f, 18.4f, 12f, 20.8f)
                path.quadTo(4.4f, 18.4f, 4.4f, 12.4f); path.lineTo(4.4f, 6.4f)
                path.close()
                c.drawPath(path, p)
            }
            Glyph.Install -> {
                path.moveTo(12f, 3.6f); path.lineTo(12f, 14.4f)
                path.moveTo(7.4f, 10f); path.lineTo(12f, 14.8f); path.lineTo(16.6f, 10f)
                path.moveTo(4f, 19.6f); path.lineTo(20f, 19.6f)
                c.drawPath(path, p)
            }
            Glyph.Sort -> {
                path.moveTo(6f, 5f); path.lineTo(6f, 19f)
                path.moveTo(3f, 8f); path.lineTo(6f, 5f); path.lineTo(9f, 8f)
                path.moveTo(18f, 19f); path.lineTo(18f, 5f)
                path.moveTo(15f, 16f); path.lineTo(18f, 19f); path.lineTo(21f, 16f)
                c.drawPath(path, p)
            }
            Glyph.Filter -> {
                path.moveTo(3.6f, 5.4f); path.lineTo(20.4f, 5.4f); path.lineTo(14f, 12.6f)
                path.lineTo(14f, 19.4f); path.lineTo(10f, 17.2f); path.lineTo(10f, 12.6f)
                path.close()
                c.drawPath(path, p)
            }
            Glyph.Camera -> {
                c.drawRoundRect(3f, 7.4f, 21f, 19.4f, 2.2f, 2.2f, p)
                c.drawCircle(12f, 13.4f, 3.6f, p)
                path.moveTo(8f, 7.4f); path.lineTo(9.4f, 4.6f); path.lineTo(14.6f, 4.6f)
                path.lineTo(16f, 7.4f)
                c.drawPath(path, p)
            }
            Glyph.Broadcast -> {
                c.drawCircle(12f, 12f, 2.4f, f)
                path.moveTo(7.2f, 7.2f); path.quadTo(3.6f, 12f, 7.2f, 16.8f)
                path.moveTo(16.8f, 7.2f); path.quadTo(20.4f, 12f, 16.8f, 16.8f)
                path.moveTo(4.4f, 4.4f); path.quadTo(-0.4f, 12f, 4.4f, 19.6f)
                path.moveTo(19.6f, 4.4f); path.quadTo(24.4f, 12f, 19.6f, 19.6f)
                c.drawPath(path, p)
            }
            Glyph.Keyboard -> {
                c.drawRoundRect(2.4f, 6.4f, 21.6f, 17.6f, 2f, 2f, p)
                path.moveTo(6f, 10.2f); path.lineTo(6.01f, 10.2f)
                path.moveTo(10f, 10.2f); path.lineTo(10.01f, 10.2f)
                path.moveTo(14f, 10.2f); path.lineTo(14.01f, 10.2f)
                path.moveTo(18f, 10.2f); path.lineTo(18.01f, 10.2f)
                path.moveTo(7.4f, 14f); path.lineTo(16.6f, 14f)
                c.drawPath(path, p)
            }
            Glyph.Accessibility -> {
                c.drawCircle(12f, 5f, 2f, f)
                path.moveTo(5f, 9.4f); path.lineTo(19f, 9.4f)
                path.moveTo(12f, 7.6f); path.lineTo(12f, 14.6f)
                path.moveTo(12f, 14.6f); path.lineTo(8f, 20.4f)
                path.moveTo(12f, 14.6f); path.lineTo(16f, 20.4f)
                c.drawPath(path, p)
            }
            Glyph.Store -> {
                path.moveTo(4f, 9.6f); path.lineTo(4.8f, 4.4f); path.lineTo(19.2f, 4.4f)
                path.lineTo(20f, 9.6f)
                c.drawPath(path, p)
                path.moveTo(5f, 9.6f); path.lineTo(5f, 19.8f); path.lineTo(19f, 19.8f)
                path.lineTo(19f, 9.6f)
                c.drawPath(path, p)
                path.moveTo(9.4f, 19.8f); path.lineTo(9.4f, 13.6f); path.lineTo(14.6f, 13.6f)
                path.lineTo(14.6f, 19.8f)
                c.drawPath(path, p)
            }
            Glyph.GamePad -> {
                path.moveTo(8f, 8f); path.lineTo(16f, 8f)
                path.quadTo(20.4f, 8.4f, 20.4f, 13f)
                path.quadTo(20.4f, 17f, 18f, 17f)
                path.quadTo(16.2f, 17f, 15f, 14.8f)
                path.lineTo(9f, 14.8f)
                path.quadTo(8f, 17f, 6f, 17f)
                path.quadTo(3.6f, 17f, 3.6f, 13f)
                path.quadTo(3.6f, 8.4f, 8f, 8f); path.close()
                c.drawPath(path, p)
            }
            Glyph.Activity -> {
                path.moveTo(3f, 12f); path.lineTo(8f, 12f); path.lineTo(10.6f, 5.6f)
                path.lineTo(14f, 18f); path.lineTo(16.4f, 12f); path.lineTo(21f, 12f)
                c.drawPath(path, p)
            }
        }
        c.restore()
    }

    /** Filled volume slider track helper (console sliders are capsule shaped). */
    fun slider(c: Canvas, l: Float, t: Float, r: Float, b: Float, value: Float, color: Int,
               trackColor: Int, paint: Paint) {
        val rad = (b - t) / 2f
        paint.color = trackColor
        c.drawRoundRect(RectF(l, t, r, b), rad, rad, paint)
        if (value > 0.01f) {
            paint.color = color
            c.drawRoundRect(RectF(l, t, l + (r - l) * value.coerceIn(0f, 1f), b), rad, rad, paint)
        }
    }
}
