package com.kaizer.ps5simulator.ui.overlay

import android.graphics.Paint
import com.kaizer.ps5simulator.core.design.Ease
import com.kaizer.ps5simulator.core.design.Tok
import com.kaizer.ps5simulator.core.engine.Frame
import com.kaizer.ps5simulator.core.engine.Glyph
import com.kaizer.ps5simulator.core.engine.Icons
import com.kaizer.ps5simulator.core.engine.Ui
import com.kaizer.ps5simulator.ui.components.Cards

/**
 * Toast, confirmation, info and text-input layers drawn above every scene.
 * All surfaces use the console's dark glass treatment and are dismissible.
 */
class Overlays {

    private data class Toast(val message: String, val start: Long, val icon: String?)

    private sealed class Modal {
        data class Info(val title: String, val body: String) : Modal()
        data class Confirm(
            val title: String, val body: String, val confirmLabel: String,
            val onConfirm: () -> Unit,
        ) : Modal()
        data class Input(val title: String, val value: String, val onSubmit: (String) -> Unit) : Modal()
    }

    private var toast: Toast? = null
    private var modal: Modal? = null
    private var modalAnim = 0f
    private var inputBuffer = ""

    fun toast(message: String, now: Long, icon: String? = null) {
        toast = Toast(message, now, icon)
    }

    fun info(title: String, body: String) {
        modal = Modal.Info(title, body)
        modalAnim = 0f
    }

    fun confirm(title: String, body: String, confirmLabel: String, onConfirm: () -> Unit) {
        modal = Modal.Confirm(title, body, confirmLabel, onConfirm)
        modalAnim = 0f
    }

    fun input(title: String, value: String, onSubmit: (String) -> Unit) {
        modal = Modal.Input(title, value, onSubmit)
        inputBuffer = value
        modalAnim = 0f
    }

    fun closeTop(): String? {
        val m = modal ?: return null
        modal = null
        return when (m) {
            is Modal.Info -> "info"
            is Modal.Confirm -> "confirm"
            is Modal.Input -> "input"
        }
    }

    val hasModal: Boolean get() = modal != null

    fun typeChar(ch: Char) {
        val m = modal as? Modal.Input ?: return
        if (ch == '\b') {
            if (inputBuffer.isNotEmpty()) inputBuffer = inputBuffer.dropLast(1)
        } else if (ch.isLetterOrDigit() || ch in " _-.") {
            if (inputBuffer.length < 18) inputBuffer += ch
        }
    }

    fun submitInput() {
        val m = modal as? Modal.Input ?: return
        if (inputBuffer.isNotBlank()) m.onSubmit(inputBuffer.trim())
        modal = null
    }

    fun isAnimating(): Boolean = modal != null || toast != null

    fun draw(f: Frame) {
        val m = f.engine.metrics
        val ui = f.ui
        val c = f.canvas
        if (modalAnim < 1f) modalAnim = (modalAnim + f.dtMs / 200f).coerceAtMost(1f)

        // toast, bottom-centre above the Control Center hint
        toast?.let { t ->
            val age = f.now - t.start
            if (age > Tok.DUR_TOAST) {
                toast = null
            } else {
                val enter = Ease.soft((age / 200f).coerceAtMost(1f))
                val exit = Ease.soft(((Tok.DUR_TOAST - age) / 240f).coerceAtMost(1f))
                val a = minOf(enter, exit)
                val s = m.textPx(22f)
                val w = ui.measure(t.message, s, Ui.Weight.Medium) + m.px(64f) + m.px(if (t.icon != null) 30f else 0f)
                val h = m.px(58f)
                val x = (m.widthPx - w) / 2f
                val y = m.heightPx - m.px(130f) - (1f - enter) * m.px(12f)
                ui.withAlpha(a) {
                    ui.rect(c, x, y, x + w, y + h, 0xE6101218.toInt(), h / 2f)
                    ui.rectOutline(c, x, y, x + w, y + h, 0x26FFFFFF.toInt(), h / 2f, 1.2f)
                    if (t.icon != null) {
                        Icons.draw(c, Glyph.Info, x + m.px(28f), y + h / 2f, m.px(20f),
                            Ui.a(Tok.TextPrimary, 0.9f), m.px(1.6f))
                    }
                    ui.line(c, t.message, x + w / 2f + (if (t.icon != null) m.px(15f) else 0f),
                        y + h / 2f + s * 0.36f, s, Ui.a(Tok.TextPrimary, 0.95f),
                        Ui.Weight.Medium, align = Paint.Align.CENTER)
                }
            }
        }

        // modal
        modal?.let { md ->
            val a = Ease.out(modalAnim)
            ui.rect(c, 0f, 0f, m.widthPx, m.heightPx, ((0.72f * a * 255).toInt() shl 24), 0f)
            val pw = minOf(m.widthPx * 0.62f, m.px(900f))
            val bodyLines = when (md) {
                is Modal.Info -> md.body
                is Modal.Confirm -> md.body
                is Modal.Input -> "Enter a new value."
            }
            val ph = when (md) {
                is Modal.Info -> m.px(340f)
                is Modal.Confirm -> m.px(280f)
                is Modal.Input -> m.px(260f)
            }
            val px = (m.widthPx - pw) / 2f
            val py = ((m.heightPx - ph) / 2f) - (1f - a) * m.px(20f)
            ui.withAlpha(a) {
                ui.rect(c, px, py, px + pw, py + ph, Tok.PanelGlassLight, m.px(26f))
                ui.rectOutline(c, px, py, px + pw, py + ph, Tok.PanelBorder, m.px(26f), 1.4f)
                val title = when (md) {
                    is Modal.Info -> md.title
                    is Modal.Confirm -> md.title
                    is Modal.Input -> md.title
                }
                ui.lineFit(c, title, px + m.px(36f), py + m.px(56f), pw - m.px(72f),
                    m.textPx(30f), 0xFFFFFFFF.toInt(), Ui.Weight.Bold)
                ui.paragraph(c, bodyLines, px + m.px(36f), py + m.px(100f), pw - m.px(72f),
                    m.textPx(21f), Ui.a(Tok.TextSecondary, 0.9f), m.px(30f), maxLines = 5)

                when (md) {
                    is Modal.Info -> {
                        Cards.chip(f, "Close", px + pw - m.px(150f), py + ph - m.px(76f), true) {
                            modal = null
                        }
                    }
                    is Modal.Confirm -> {
                        Cards.chip(f, "Cancel", px + pw - m.px(300f), py + ph - m.px(76f), false) {
                            modal = null
                        }
                        Cards.chip(f, md.confirmLabel, px + pw - m.px(160f),
                            py + ph - m.px(76f), true) {
                            modal = null
                            md.onConfirm()
                        }
                    }
                    is Modal.Input -> {
                        val iw = pw - m.px(72f)
                        ui.rect(c, px + m.px(36f), py + m.px(122f), px + m.px(36f) + iw,
                            py + m.px(178f), 0x1FFFFFFF.toInt(), m.px(12f))
                        ui.rectOutline(c, px + m.px(36f), py + m.px(122f), px + m.px(36f) + iw,
                            py + m.px(178f), 0x66FFFFFF.toInt(), m.px(12f), 1.4f)
                        ui.line(c, inputBuffer + "|", px + m.px(54f), py + m.px(158f),
                            m.textPx(24f), 0xFFFFFFFF.toInt(), Ui.Weight.Medium)
                        // on-screen keyboard: rows of letters
                        val rows = listOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
                        var ky = py + m.px(196f)
                        rows.forEach { rowStr ->
                            var kx = px + m.px(36f)
                            val kw = (iw - m.px(9f) * (rowStr.length - 1)) / rowStr.length
                            rowStr.forEach { ch ->
                                ui.rect(c, kx, ky, kx + kw, ky + m.px(26f), 0x26FFFFFF.toInt(),
                                    m.px(4f))
                                ui.line(c, ch.toString(), kx + kw / 2f, ky + m.px(18f),
                                    m.textPx(15f), 0xE6FFFFFF.toInt(), Ui.Weight.Medium,
                                    align = Paint.Align.CENTER)
                                f.engine.hits.add(kx, ky, kx + kw, ky + m.px(26f),
                                    20000 + ch.hashCode()) {
                                    typeChar(ch.lowercaseChar())
                                    f.engine.sfx.play(
                                        com.kaizer.ps5simulator.core.engine.Sfx.Cue.Nav)
                                }
                                kx += kw + m.px(9f)
                            }
                            ky += m.px(30f)
                        }
                        Cards.chip(f, "⌫", px + m.px(36f), ky + m.px(6f), false) {
                            typeChar('\b')
                        }
                        Cards.chip(f, "Save", px + pw - m.px(140f), ky + m.px(6f), true) {
                            submitInput()
                        }
                    }
                }
            }
        }
    }

    fun onDown(x: Float, y: Float): Boolean = modal != null || toast != null

    fun onDrag(x: Float, y: Float, dx: Float, dy: Float): Boolean = modal != null

    fun onUp(x: Float, y: Float): Boolean = modal != null || toast != null

    fun onCancel() {
        // keep modal (prevents accidental dismissal)
    }
}
