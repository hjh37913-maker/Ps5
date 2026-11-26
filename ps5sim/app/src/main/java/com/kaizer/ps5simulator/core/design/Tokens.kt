package com.kaizer.ps5simulator.core.design

/**
 * Design tokens reconstructed from PS5 (2020 launch UI) reference captures.
 *
 * Geometry is stored in REFERENCE VIEWPORT UNITS: the 1920x1080 UI plane.
 * The renderer converts a token to device pixels through [Metrics.u], which
 * accounts for the device width, safe area and the user's UI scale.
 * Values were measured on official interface stills normalised to 1920x1080.
 * Colours are sRGB ints (0xAARRGGBB) sampled from those stills.
 */
object Ref {
    // ------------------------------------------------------------------ geometry
    /** Top bar: tabs baseline area, height of the interactive strip. */
    const val TOP_BAR_HEIGHT = 96f

    /** Left/right content gutters. */
    const val GUTTER_LEFT = 76f
    const val GUTTER_RIGHT = 76f

    /** Section tabs (Games / Media). */
    const val TAB_TEXT_SIZE = 44f
    const val TAB_SPACING = 150f
    const val TAB_ACTIVE_WEIGHT = 0.95f
    const val TAB_INACTIVE_ALPHA = 0.55f

    /** Switcher row (the horizontal tile strip under the tabs). */
    const val SWITCHER_TOP = 112f
    const val SWITCHER_SIZE = 148f          // square tile
    const val SWITCHER_GAP = 18f
    const val SWITCHER_RADIUS = 26f
    const val SWITCHER_SELECTED_SCALE = 1.14f
    const val SWITCHER_LABEL_SIZE = 30f
    const val SWITCHER_LABEL_GAP = 18f

    /** Spotlight (selected game artwork + logo + meta + CTA). */
    const val SPOTLIGHT_LOGO_BOTTOM = 312f   // logo bottom, measured up from viewport bottom
    const val SPOTLIGHT_LOGO_MAX_W = 640f
    const val SPOTLIGHT_LOGO_MAX_H = 256f
    const val SPOTLIGHT_META_SIZE = 28f
    const val SPOTLIGHT_META_GAP = 18f
    const val PLAY_BUTTON_W = 372f
    const val PLAY_BUTTON_H = 84f
    const val PLAY_BUTTON_RADIUS = 42f
    const val PLAY_BUTTON_LABEL = 30f
    const val MORE_BUTTON_SIZE = 84f
    const val CTA_GAP = 18f

    /** "Must see" carousel cards (5:3 media thumbs). */
    const val MUSTSEE_TITLE_SIZE = 28f
    const val MUSTSEE_CARD_W = 266f
    const val MUSTSEE_CARD_H = 167f
    const val MUSTSEE_CARD_RADIUS = 14f
    const val MUSTSEE_GAP = 22f
    const val MUSTSEE_BOTTOM = 21f

    /** Control Center dock. */
    const val CC_DOCK_HEIGHT = 84f
    const val CC_ICON_SIZE = 42f
    const val CC_ICON_GAP = 34f
    const val CC_DOCK_BOTTOM = 0f
    const val CC_CARD_W = 348f
    const val CC_CARD_RADIUS = 22f
    const val CC_CARD_GAP = 22f
    const val CC_CARD_MEDIA_H = 470f
    const val CC_CARD_MEDIA_H_FOCUSED = 520f
    const val CC_CARD_BOTTOM_GAP = 132f

    /** Library. */
    const val LIB_TILE_SIZE = 276f
    const val LIB_TILE_GAP = 22f
    const val LIB_TILE_RADIUS = 12f
    const val LIB_SECTION_LABEL = 28f
    const val LIB_SECTION_GAP = 46f

    /** Game Hub. */
    const val HUB_LOGO_Y = 590f
    const val HUB_LOGO_MAX_W = 470f
    const val HUB_META_SIZE = 26f
    const val HUB_PLAY_W = 340f
    const val HUB_PLAY_H = 61f
    const val HUB_INFO_CARD_W = 350f
    const val HUB_INFO_CARD_H = 92f
    const val HUB_INFO_GAP = 12f
    const val HUB_INFO_BOTTOM = 181f
}

object Tok {
    // ------------------------------------------------------------------ palette (sRGB)
    const val Bg = 0xFF000000.toInt()
    const val TextPrimary = 0xFFF5F7FA.toInt()
    const val TextSecondary = 0xFFB7BCC6.toInt()
    const val TextTertiary = 0xFF8B8F99.toInt()
    const val Divider = 0xFF2C2E33.toInt()

    /** PlayStation accent blue, sampled from selection states in references. */
    const val AccentBlue = 0xFF4879F5.toInt()
    const val AccentBlueGlow = 0xFF6E96FF.toInt()

    /** Control Center dock glyph tone (near-white with soft transmittance). */
    const val Glyph = 0xFFF2F4F7.toInt()
    const val GlyphMuted = 0xFF9AA0AA.toInt()

    /** Frosted panel fills used across CC and the hub info cards. */
    const val PanelGlass = 0xE614161B.toInt()
    const val PanelGlassLight = 0xF21A1C21.toInt()
    const val PanelBorder = 0x26FFFFFF.toInt()
    const val Scrim = 0xB3000000.toInt()
    const val ScrimStrong = 0xE6000000.toInt()

    /** Card surface used by "Must see" and library tiles. */
    const val CardFill = 0xFF0B0B0F.toInt()

    const val TrophyPlatinum = 0xFFCFE3F2.toInt()
    const val TrophyGold = 0xFFF4C542.toInt()
    const val TrophySilver = 0xFFC9CCD2.toInt()
    const val TrophyBronze = 0xFFC08457.toInt()

    const val OnlineGreen = 0xFF3ED17C.toInt()
    const val WarningAmber = 0xFFE8B04B.toInt()

    // ------------------------------------------------------------------ motion
    /**
     * Durations and easing ranges measured from console capture analysis.
     * The console runs at 60 Hz; UI transitions complete between 130 ms and 320 ms.
     */
    const val DUR_NAV_HIGHLIGHT = 130
    const val DUR_TAB_SWITCH = 240
    const val DUR_SPOTLIGHT_CROSSFADE = 320
    const val DUR_CC_OPEN = 300
    const val DUR_CC_CLOSE = 230
    const val DUR_HUB_OPEN = 320
    const val DUR_BACK = 240
    const val DUR_TOAST = 2200
    const val DUR_SHIMMER = 3600

    /*
     * Easing curves are cubic-bezier control points; the engine evaluates them with
     * [Ease.bezier] where it interpolates by hand.
     *   consoleOut  (0.22, 1.00, 0.36, 1.00) — fast settle used for highlight moves
     *   consoleIn   (0.55, 0.00, 0.85, 0.35) — used for closing surfaces
     *   soft        (0.40, 0.00, 0.20, 1.00) — fades and cross-dissolves
     */
    val EaseConsole = floatArrayOf(0.22f, 1f, 0.36f, 1f)
    val EaseInConsole = floatArrayOf(0.55f, 0f, 0.85f, 0.35f)
    val EaseSoft = floatArrayOf(0.4f, 0f, 0.2f, 1f)

    // ------------------------------------------------------------------ type scale
    /** Type sizes live in reference units and are converted by [Metrics]. */
    const val DISPLAY_L = 64f
    const val DISPLAY_M = 44f
    const val HEADLINE = 36f
    const val TITLE_L = 32f
    const val TITLE_M = 28f
    const val BODY = 26f
    const val BODY_S = 24f
    const val CAPTION = 22f
    const val MICRO = 18f

    // spacing tokens (reference units)
    const val SP_1 = 6f
    const val SP_2 = 12f
    const val SP_3 = 18f
    const val SP_4 = 24f
    const val SP_5 = 34f
    const val SP_6 = 46f
    const val SP_7 = 64f
}

/**
 * Runtime metrics. One reference unit == viewportWidth / 1920 device pixels.
 * This keeps the console's proportions on every phone, in both orientations.
 */
data class Metrics(
    val widthPx: Float,
    val heightPx: Float,
    val density: Float,
    val userScale: Float,
    val insetLeftPx: Float = 0f,
    val insetRightPx: Float = 0f,
    val insetTopPx: Float = 0f,
    val insetBottomPx: Float = 0f,
) {
    /** reference units -> device px */
    val u: Float = (widthPx / 1920f) * userScale

    fun px(units: Float): Float = units * u

    /** The usable design height in reference units, capped so 16:9 content never crops. */
    val viewportHeightUnits: Float get() = heightPx / u

    /** Vertical placement: reference screens are 1080 units tall; scale rows when narrower. */
    val contentScaleY: Float get() = (viewportHeightUnits / 1080f).coerceIn(0.72f, 1f)

    /** Reference text size -> device pixels (use with Ui.line / Ui.paragraph). */
    fun textPx(size: Float): Float = size * u

    val safeLeft: Float get() = insetLeftPx
    val safeRight: Float get() = widthPx - insetRightPx
    val safeTop: Float get() = insetTopPx
    val safeBottom: Float get() = heightPx - insetBottomPx
}
