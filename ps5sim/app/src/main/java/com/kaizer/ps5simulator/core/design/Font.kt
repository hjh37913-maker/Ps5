package com.kaizer.ps5simulator.core.design

import android.content.Context
import android.graphics.Typeface

/**
 * Typography.
 *
 * The console UI face is a proprietary geometric humanist sans (wide spacing, tall
 * x-height). We ship legal open-licensed lookalikes instead of the original font file:
 *   - Manrope (OFL) for titles, display and buttons (weight 600/700/800),
 *   - Figtree  (OFL) for metadata, captions and settings rows (weight 400/500).
 * Weights map 1:1 to the console's four text roles.
 */
object Typography {
    private var regular: Typeface? = null
    private var medium: Typeface? = null
    private var bold: Typeface? = null
    private var display: Typeface? = null
    private var light: Typeface? = null

    fun install(context: Context) {
        if (regular != null) return
        val a = context.assets
        regular = safe(a, "fonts/Figtree-Regular.ttf")
        medium = safe(a, "fonts/Manrope-SemiBold.ttf")
        bold = safe(a, "fonts/Manrope-Bold.ttf")
        display = safe(a, "fonts/Manrope-ExtraBold.ttf")
        light = safe(a, "fonts/Figtree-Medium.ttf")
    }

    private fun safe(assets: android.content.res.AssetManager, path: String): Typeface = try {
        Typeface.createFromAsset(assets, path)
    } catch (t: Throwable) {
        Typeface.create("sans-serif", Typeface.NORMAL)
    }

    val fontRegular: Typeface get() = regular ?: Typeface.create("sans-serif", Typeface.NORMAL)
    val fontMedium: Typeface get() = medium ?: Typeface.create("sans-serif-medium", Typeface.NORMAL)
    val fontBold: Typeface get() = bold ?: Typeface.create("sans-serif", Typeface.BOLD)
    val fontDisplay: Typeface get() = display ?: Typeface.create("sans-serif", Typeface.BOLD)
    val fontLight: Typeface get() = light ?: Typeface.create("sans-serif", Typeface.NORMAL)
}

