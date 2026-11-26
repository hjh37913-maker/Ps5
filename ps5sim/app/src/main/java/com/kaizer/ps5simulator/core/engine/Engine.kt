package com.kaizer.ps5simulator.core.engine

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Metrics
import com.kaizer.ps5simulator.core.design.Ref
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.design.Typography
import com.kaizer.ps5simulator.core.gfx.Art
import com.kaizer.ps5simulator.data.catalog.Catalog
import com.kaizer.ps5simulator.data.model.Game
import com.kaizer.ps5simulator.data.store.Settings
import com.kaizer.ps5simulator.data.store.SimState
import com.kaizer.ps5simulator.ui.controlcenter.ControlCenterScene
import com.kaizer.ps5simulator.ui.home.HomeScene
import com.kaizer.ps5simulator.ui.hub.HubScene
import com.kaizer.ps5simulator.ui.launch.LaunchScene
import com.kaizer.ps5simulator.ui.library.LibraryScene
import com.kaizer.ps5simulator.ui.media.MediaScene
import com.kaizer.ps5simulator.ui.overlay.Overlays
import com.kaizer.ps5simulator.ui.profile.ProfileScene
import com.kaizer.ps5simulator.ui.screens.AmbientBackground
import com.kaizer.ps5simulator.ui.screens.Backdrop
import com.kaizer.ps5simulator.ui.settings.SettingsScene
import com.kaizer.ps5simulator.ui.store.StoreScene

/**
 * Central simulator state + frame pump.
 *
 * Holds the scene stack, the shared [Frame], wall clock, Control Center state, toast
 * layer and transition values. Scenes are small, independent and only registered here.
 */
class Engine(private val context: Context) {

    val settings = Settings(context)
    val sim = SimState(context)
    val sfx = Sfx(settings)
    val ui = Ui()
    val hits = Hits()
    val overlays = Overlays()
    val backdrop = Backdrop()

    var metrics: Metrics = Metrics(1920f, 1080f, 2.75f, 1f)

    var games: List<Game> = emptyList()
        private set

    // ------------------------------------------------------------------ navigation
    var current: Scene? = null
        private set
    var controlCenter: ControlCenterScene? = null
    private val stack = ArrayDeque<Scene>()
    var transitionAlpha = 1f
        private set
    private var transitionFrom: Scene? = null

    var home: HomeScene? = null
    var library: LibraryScene? = null
    var hub: HubScene? = null
    var profile: ProfileScene? = null
    var settingsScene: SettingsScene? = null
    var store: StoreScene? = null
    var media: MediaScene? = null
    var launch: LaunchScene? = null

    // ------------------------------------------------------------------ timing
    var nowMs: Long = 0L
        private set
    private var lastFrameMs: Long = 0L
    val animationsOn: Boolean get() = settings.animations && !settings.reducedMotion

    var invalidationCallback: () -> Unit = {}

    /** Last touch position (px) — read by draggable controls such as sliders. */
    var lastTouchX: Float = 0f
        private set
    var lastTouchY: Float = 0f
        private set

    fun init() {
        games = Catalog.load(context)
        Art.init(context)
        Art.invalidate = { invalidationCallback() }
        sfx.attach(context)
        Typography.install(context)
        ui.fontRegular = Typography.fontRegular
        ui.fontMedium = Typography.fontMedium
        ui.fontBold = Typography.fontBold
        ui.fontDisplay = Typography.fontDisplay
        val sel = sim.selectedGameId ?: games.firstOrNull()?.id
        home = HomeScene(this, sel ?: "", settings.startTab)
        library = LibraryScene(this)
        profile = ProfileScene(this)
        settingsScene = SettingsScene(this)
        store = StoreScene(this)
        media = MediaScene(this)
        controlCenter = ControlCenterScene(this)
        current = home
        home?.onShow()
    }

    fun game(id: String?): Game? = games.firstOrNull { it.id == id } ?: games.firstOrNull()

    fun selectedGame(): Game? = game(sim.selectedGameId)

    // ------------------------------------------------------------------ scene graph
    fun openHub(gameId: String) {
        val g = game(gameId) ?: return
        sim.selectedGameId = g.id
        hub = HubScene(this, g.id)
        push(hub!!, Sfx.Cue.Select)
    }

    fun openLaunch(g: Game) {
        launch = LaunchScene(this, g.id)
        push(launch!!, Sfx.Cue.Launch)
    }

    fun openProfile() { (profile ?: return).let { push(it, Sfx.Cue.Select) } }
    fun openSettings() { (settingsScene ?: return).let { push(it, Sfx.Cue.Select) } }
    fun openLibrary() { (library ?: return).let { push(it, Sfx.Cue.Select) } }
    fun openStore() { (store ?: return).let { push(it, Sfx.Cue.Select) } }
    fun openMedia() { (media ?: return).let { push(it, Sfx.Cue.Select) } }
    fun openTrophies() {
        push(com.kaizer.ps5simulator.ui.trophies.TrophiesScene(this), Sfx.Cue.Select)
    }

    fun push(scene: Scene, cue: Sfx.Cue = Sfx.Cue.Open) {
        current?.let { stack.addLast(it); it.onHide() }
        current = scene
        scene.onShow()
        transitionAlpha = if (animationsOn) 0f else 1f
        transitionFrom = stack.lastOrNull()
        sfx.play(cue)
    }

    fun pop(): Boolean {
        val prev = stack.removeLastOrNull() ?: return false
        current?.onHide()
        current = prev
        prev.onShow()
        transitionAlpha = if (animationsOn) 0f else 1f
        transitionFrom = null
        sfx.play(Sfx.Cue.Back)
        return true
    }

    fun popToRoot() {
        while (stack.size > 0) {
            val prev = stack.removeLastOrNull() ?: break
            prev.onHide()
        }
        current = home
        home?.onShow()
        transitionAlpha = 1f
    }

    fun back(): Boolean {
        controlCenter?.let { cc ->
            if (cc.visible) {
                cc.hide()
                return true
            }
        }
        overlays.closeTop()?.let { return true }
        current?.let { if (it.back()) return true }
        if (current === home) return false
        return pop()
    }

    // ------------------------------------------------------------------ toasts / modal
    fun toast(message: String, icon: String? = null) = overlays.toast(message, nowMs, icon)

    fun confirm(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit) =
        overlays.confirm(title, body, confirmLabel, onConfirm)

    fun showInfo(title: String, body: String) = overlays.info(title, body)

    fun openControlCenter() {
        controlCenter?.show()
    }

    fun showTextInput(title: String, value: String, onSubmit: (String) -> Unit) =
        overlays.input(title, value, onSubmit)

    fun showSearch(onSubmit: (String) -> Unit) = overlays.input("Search library", "", onSubmit)

    /** Re-applies the UI scale after a settings change. */
    fun applyUiScale() {
        metrics = metrics.copy(userScale = settings.uiScale)
        invalidationCallback()
    }

    // ------------------------------------------------------------------ touch routing
    private var edgeSwipe = false
    private var edgeStartY = 0f

    fun onDown(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
        // bottom-edge swipe up opens the Control Center (touch stand-in for the PS button)
        edgeStartY = y
        edgeSwipe = y > metrics.heightPx - metrics.px(56f)
        if (overlays.onDown(x, y)) return
        controlCenter?.let { cc ->
            if (cc.visible) {
                cc.onDown(x, y)
                return
            }
        }
        // hit regions persist from the last draw pass (immediate-mode contract)
        if (hits.down(x, y)) return
        current?.onDown(x, y)
    }

    fun onDrag(x: Float, y: Float, dx: Float, dy: Float) {
        lastTouchX = x
        lastTouchY = y
        if (edgeSwipe) {
            if (edgeStartY - y > metrics.px(54f)) {
                edgeSwipe = false
                controlCenter?.show()
            }
            return
        }
        if (overlays.onDrag(x, y, dx, dy)) return
        controlCenter?.let { cc ->
            if (cc.visible) {
                cc.onDrag(x, y, dx, dy)
                return
            }
        }
        current?.onDrag(x, y, dx, dy)
    }

    fun onUp(x: Float, y: Float) {
        lastTouchX = x
        lastTouchY = y
        if (overlays.onUp(x, y)) return
        controlCenter?.let { cc ->
            if (cc.visible) {
                cc.onUp(x, y)
                return
            }
        }
        if (hits.up(x, y)) return
        current?.onUp(x, y)
    }

    fun onCancel() {
        edgeSwipe = false
        hits.cancel()
        overlays.onCancel()
    }

    // ------------------------------------------------------------------ frame
    private val fadePaint = Paint()

    fun draw(canvas: Canvas, now: Long) {
        nowMs = now
        val dt = if (lastFrameMs == 0L) 16L else (now - lastFrameMs).coerceIn(0L, 64L)
        lastFrameMs = now
        hits.clear()
        AmbientBackground.update(dt)

        val f = Frame(canvas, ui, this, now, dt)
        canvas.drawColor(Color.BLACK)
        current?.draw(f)

        controlCenter?.let { cc ->
            if (cc.visible || cc.isAnimating()) cc.draw(f)
        }
        overlays.draw(f)
        if (virtualPadVisible) com.kaizer.ps5simulator.ui.components.VirtualPad.draw(f)
        drawTransition(f)
    }

    private fun drawTransition(f: Frame) {
        if (!animationsOn) {
            transitionAlpha = 1f
            return
        }
        if (transitionAlpha < 1f) {
            transitionAlpha = (transitionAlpha + f.dtMs / 260f).coerceAtMost(1f)
            fadePaint.color = Color.argb(((1f - transitionAlpha) * 140).toInt(), 0, 0, 0)
            f.canvas.drawRect(0f, 0f, metrics.widthPx, metrics.heightPx, fadePaint)
        }
    }

    fun isAnimating(): Boolean =
        transitionAlpha < 1f || (controlCenter?.isAnimating() ?: false) || overlays.isAnimating() ||
            (current?.isAnimating() ?: false)

    /**
     * True when the next frame would change something. Ambient motion (dynamic
     * background) counts as animation, so the loop runs continuously only when that
     * option is enabled or something is actually moving — otherwise we draw on demand.
     */
    fun needsFrame(): Boolean =
        isAnimating() || (settings.dynamicBackground && animationsOn)

    fun release() {
        sfx.release()
        Art.clear()
    }

    fun updateMetrics(m: Metrics) {
        metrics = m
    }

    /** Virtual DualSense overlay (touch adaptation), toggled from Settings/home hint. */
    var virtualPadVisible: Boolean = false
        private set

    fun toggleVirtualPad() {
        virtualPadVisible = !virtualPadVisible
        toast(if (virtualPadVisible) "${s("virtual_pad")} — on" else "${s("virtual_pad")} — off")
        invalidationCallback()
    }

    // ------------------------------------------------------------------ convenience
    /**
     * Left content gutter.
     *
     * Reference gutter is 175 units from the screen edge. On displays wider than the
     * 1920 reference we keep the spotlight block at ~55% of the width (as measured on
     * the stills) instead of pushing it further right, matching the console's behaviour
     * of keeping the title block visually anchored.
     */
    val gutter: Float get() = run {
        val u = metrics.u
        val ref = Ref.GUTTER_LEFT * u
        val extra = metrics.widthPx - 1920f * u
        if (extra <= 0f) ref else ref + extra * 0.30f
    }

    val hour: Int get() = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val minute: Int get() = java.util.Calendar.getInstance().get(java.util.Calendar.MINUTE)

    /** Localised UI string for the current in-app language. */
    fun s(key: String): String = com.kaizer.ps5simulator.core.design.Strings.get(
        settings.language, key)

    fun clockText(): String {
        val h24 = hour
        val h12 = if (h24 % 12 == 0) 12 else h24 % 12
        return String.format("%d:%02d", h12, minute) + if (h24 < 12) " AM" else " PM"
    }
}
