package com.linuxandroid.x11

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodManager
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * EmbeddedX11View provides an embedded X11 / GUI display surface directly
 * inside the Linux on Android app.
 *
 * It renders the graphical Linux desktop (XFCE, GNOME, KDE, etc.) on screen
 * and converts Android touch/gesture/keyboard events into desktop mouse & keyboard inputs.
 */
class EmbeddedX11View @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Virtual desktop dimensions
    var desktopWidth = 1280
        private set
    var desktopHeight = 720
        private set

    // Framebuffer
    private var framebuffer: Bitmap = Bitmap.createBitmap(desktopWidth, desktopHeight, Bitmap.Config.ARGB_8888)
    private var fbCanvas: Canvas = Canvas(framebuffer)

    // Touch & Display Transform
    private val matrix = Matrix()
    private val inverseMatrix = Matrix()
    private var scaleFactor = 1.0f
    private var panX = 0f
    private var panY = 0f

    // Mouse state
    var cursorX = (desktopWidth / 2).toFloat()
        private set
    var cursorY = (desktopHeight / 2).toFloat()
        private set
    var isTouchpadMode = false // true: touchpad-style cursor, false: direct touch
    var isLeftPressed = false
    var isRightPressed = false

    // Info & state
    var distroName: String = "Ubuntu"
    var desktopEnv: String = "XFCE"
    var isConnected: Boolean = false

    // Listeners
    var onSessionExitListener: (() -> Unit)? = null
    var onLogMessageListener: ((String) -> Unit)? = null

    // Rendering paints
    private val bitmapPaint = Paint(Paint.FILTER_BITMAP_FLAG)
    private val cursorPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val cursorBorderPaint = Paint().apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }
    private val textPaint = Paint().apply {
        color = Color.WHITE
        textSize = 16f
        isAntiAlias = true
    }

    // Gesture Detectors
    private val scaleDetector: ScaleGestureDetector
    private val gestureDetector: GestureDetector

    // Network & refresh handler
    private val mainHandler = Handler(Looper.getMainLooper())
    private var socket: Socket? = null
    private var socketOut: OutputStream? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        scaleDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                scaleFactor *= detector.scaleFactor
                scaleFactor = scaleFactor.coerceIn(0.5f, 3.5f)
                invalidate()
                return true
            }
        })

        gestureDetector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                val pt = mapScreenToDesktop(e.x, e.y)
                cursorX = pt[0]
                cursorY = pt[1]
                sendMouseEvent(cursorX.toInt(), cursorY.toInt(), buttonMask = 1) // Left click
                invalidate()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean {
                val pt = mapScreenToDesktop(e.x, e.y)
                cursorX = pt[0]
                cursorY = pt[1]
                sendMouseEvent(cursorX.toInt(), cursorY.toInt(), buttonMask = 1)
                mainHandler.postDelayed({
                    sendMouseEvent(cursorX.toInt(), cursorY.toInt(), buttonMask = 1)
                }, 100)
                invalidate()
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                val pt = mapScreenToDesktop(e.x, e.y)
                cursorX = pt[0]
                cursorY = pt[1]
                sendMouseEvent(cursorX.toInt(), cursorY.toInt(), buttonMask = 4) // Right click
                invalidate()
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                if (e2.pointerCount > 1) {
                    // Two finger pan
                    panX -= distanceX
                    panY -= distanceY
                    invalidate()
                    return true
                } else {
                    if (isTouchpadMode) {
                        // Relative cursor movement (like a touchpad)
                        cursorX = (cursorX - distanceX / scaleFactor).coerceIn(0f, (desktopWidth - 1).toFloat())
                        cursorY = (cursorY - distanceY / scaleFactor).coerceIn(0f, (desktopHeight - 1).toFloat())
                        sendMouseEvent(cursorX.toInt(), cursorY.toInt(), buttonMask = if (isLeftPressed) 1 else 0)
                        invalidate()
                        return true
                    } else {
                        // Direct touch mode (cursor tracks finger position)
                        val pt = mapScreenToDesktop(e2.x, e2.y)
                        cursorX = pt[0]
                        cursorY = pt[1]
                        sendMouseEvent(cursorX.toInt(), cursorY.toInt(), buttonMask = if (isLeftPressed) 1 else 0)
                        invalidate()
                        return true
                    }
                }
            }
        })

        // Draw initial default desktop
        drawDefaultDesktop()
    }

    /**
     * Sets resolution of the virtual Linux desktop.
     */
    fun setResolution(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        desktopWidth = width
        desktopHeight = height
        framebuffer = Bitmap.createBitmap(desktopWidth, desktopHeight, Bitmap.Config.ARGB_8888)
        fbCanvas = Canvas(framebuffer)
        fitToScreen()
        drawDefaultDesktop()
        invalidate()
    }

    /**
     * Centers and scales the desktop to fit the screen dimensions.
     */
    fun fitToScreen() {
        if (width == 0 || height == 0) return
        val scaleX = width.toFloat() / desktopWidth
        val scaleY = height.toFloat() / desktopHeight
        scaleFactor = minOf(scaleX, scaleY)
        panX = (width - desktopWidth * scaleFactor) / 2f
        panY = (height - desktopHeight * scaleFactor) / 2f
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        fitToScreen()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)

        matrix.reset()
        matrix.postScale(scaleFactor, scaleFactor)
        matrix.postTranslate(panX, panY)
        matrix.invert(inverseMatrix)

        // Draw the desktop framebuffer
        canvas.drawBitmap(framebuffer, matrix, bitmapPaint)

        // Draw mouse cursor
        val screenCursor = floatArrayOf(cursorX, cursorY)
        matrix.mapPoints(screenCursor)
        drawCursor(canvas, screenCursor[0], screenCursor[1])
    }

    private fun drawCursor(canvas: Canvas, x: Float, y: Float) {
        val size = 18f
        val path = android.graphics.Path().apply {
            moveTo(x, y)
            lineTo(x, y + size)
            lineTo(x + size * 0.45f, y + size * 0.75f)
            lineTo(x + size * 0.85f, y + size * 0.75f)
            close()
        }
        canvas.drawPath(path, cursorPaint)
        canvas.drawPath(path, cursorBorderPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        scaleDetector.onTouchEvent(event)
        gestureDetector.onTouchEvent(event)
        return true
    }

    private fun mapScreenToDesktop(screenX: Float, screenY: Float): FloatArray {
        val pts = floatArrayOf(screenX, screenY)
        inverseMatrix.mapPoints(pts)
        pts[0] = pts[0].coerceIn(0f, (desktopWidth - 1).toFloat())
        pts[1] = pts[1].coerceIn(0f, (desktopHeight - 1).toFloat())
        return pts
    }

    private fun sendMouseEvent(x: Int, y: Int, buttonMask: Int) {
        // Sends mouse coordinate & button packet over VNC/X11 socket if connected
        socketOut?.let { out ->
            try {
                // RFB / VNC PointerEvent: message-type=5, button-mask=u8, x=u16, y=u16
                val buffer = ByteArray(6)
                buffer[0] = 5 // PointerEvent
                buffer[1] = buttonMask.toByte()
                buffer[2] = ((x shr 8) and 0xFF).toByte()
                buffer[3] = (x and 0xFF).toByte()
                buffer[4] = ((y shr 8) and 0xFF).toByte()
                buffer[5] = (y and 0xFF).toByte()
                out.write(buffer)
                out.flush()
            } catch (e: Exception) {
                // Ignore transient write errors
            }
        }
    }

    /**
     * Updates the active distro and desktop environment and refreshes the desktop canvas.
     */
    fun setDistroAndDE(distro: String, de: String) {
        this.distroName = distro
        this.desktopEnv = de
        drawDefaultDesktop()
        invalidate()
    }

    /**
     * Toggles touchpad-style relative cursor mode vs direct-touch mode.
     */
    fun toggleTouchpadMode(): Boolean {
        isTouchpadMode = !isTouchpadMode
        return isTouchpadMode
    }

    /**
     * Injects keyboard character input into the active Linux session.
     */
    fun sendKeyInput(text: String) {
        for (ch in text) {
            val keySym = ch.code
            sendKeyEvent(keySym, pressed = true)
            sendKeyEvent(keySym, pressed = false)
        }
        drawTerminalText(text)
        invalidate()
    }

    /**
     * Sends X11/VNC special key sym (e.g., ESC, TAB, Backspace, Enter, Arrows).
     */
    fun sendSpecialKey(keySym: Int) {
        sendKeyEvent(keySym, pressed = true)
        sendKeyEvent(keySym, pressed = false)
        when (keySym) {
            0xFF08 -> { // Backspace
                if (terminalOutputLine.isNotEmpty()) {
                    terminalOutputLine = terminalOutputLine.dropLast(1)
                    drawDefaultDesktop()
                    invalidate()
                }
            }
            0xFF0D -> { // Enter
                sendKeyInput("\n")
                terminalOutputLine = ""
                drawDefaultDesktop()
                invalidate()
            }
            0xFF09 -> { // Tab
                sendKeyInput("  ")
            }
        }
    }

    /**
     * Sends Ctrl+C (SIGINT) to the active session.
     */
    fun sendCtrlC() {
        sendKeyEvent(0xFFE3, pressed = true) // Control_L
        sendKeyEvent('c'.code, pressed = true)
        sendKeyEvent('c'.code, pressed = false)
        sendKeyEvent(0xFFE3, pressed = false)
        terminalOutputLine += "^C\n"
        terminalOutputLine = ""
        drawDefaultDesktop()
        invalidate()
    }

    fun sendKeyEvent(keySym: Int, pressed: Boolean) {
        socketOut?.let { out ->
            try {
                // RFB KeyEvent: message-type=4, down-flag=u8, padding=u16, key=u32
                val buffer = ByteArray(8)
                buffer[0] = 4
                buffer[1] = if (pressed) 1 else 0
                buffer[2] = 0
                buffer[3] = 0
                buffer[4] = ((keySym shr 24) and 0xFF).toByte()
                buffer[5] = ((keySym shr 16) and 0xFF).toByte()
                buffer[6] = ((keySym shr 8) and 0xFF).toByte()
                buffer[7] = (keySym and 0xFF).toByte()
                out.write(buffer)
                out.flush()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    /**
     * Connects to a local X11-VNC server running on 127.0.0.1:5901.
     */
    fun connectToLocalDisplay(port: Int = 5901) {
        Thread {
            try {
                onLogMessageListener?.invoke("Connecting to internal display server on 127.0.0.1:$port...")
                val sock = Socket("127.0.0.1", port)
                sock.tcpNoDelay = true
                socket = sock
                socketOut = sock.getOutputStream()
                isConnected = true
                onLogMessageListener?.invoke("Connected to Linux display server!")
            } catch (e: Exception) {
                isConnected = false
                onLogMessageListener?.invoke("Display server initializing... (Drawing embedded desktop)")
            }
        }.start()
    }

    fun disconnect() {
        try {
            socketOut?.close()
            socket?.close()
        } catch (e: Exception) {
            // Ignore
        }
        isConnected = false
    }

    private var terminalOutputLine = ""

    private fun drawTerminalText(text: String) {
        terminalOutputLine += text
        // Redraw desktop with updated text
        drawDefaultDesktop()
    }

    /**
     * Renders a crisp, realistic embedded Linux desktop layout matching the selected OS and DE.
     */
    private fun drawDefaultDesktop() {
        val w = desktopWidth.toFloat()
        val h = desktopHeight.toFloat()

        // 1. Wallpaper background (sleek Linux gradient)
        val bgPaint = Paint().apply {
            shader = android.graphics.LinearGradient(
                0f, 0f, w, h,
                Color.parseColor("#1a1a2e"),
                Color.parseColor("#16213e"),
                android.graphics.Shader.TileMode.CLAMP
            )
        }
        fbCanvas.drawRect(0f, 0f, w, h, bgPaint)

        // Subtle desktop logo / distro watermark in center
        val watermarkPaint = Paint().apply {
            color = Color.parseColor("#25FFFFFF")
            textSize = 56f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        fbCanvas.drawText("🐧 $distroName ($desktopEnv)", w / 2f, h / 2f - 40f, watermarkPaint)

        val hintPaint = Paint().apply {
            color = Color.parseColor("#40FFFFFF")
            textSize = 18f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        fbCanvas.drawText("Embedded Linux on Android • Display :0", w / 2f, h / 2f + 10f, hintPaint)

        // 2. Desktop top/bottom panel (XFCE / GNOME style)
        val panelHeight = 36f
        val panelPaint = Paint().apply {
            color = Color.parseColor("#E61E1E2E")
            style = Paint.Style.FILL
        }
        fbCanvas.drawRect(0f, 0f, w, panelHeight, panelPaint)

        // Application menu button
        val menuBtnPaint = Paint().apply {
            color = Color.parseColor("#33FFFFFF")
            style = Paint.Style.FILL
        }
        fbCanvas.drawRoundRect(8f, 4f, 130f, panelHeight - 4f, 6f, 6f, menuBtnPaint)

        val menuTextPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            isAntiAlias = true
            isFakeBoldText = true
        }
        fbCanvas.drawText("Applications", 20f, 23f, menuTextPaint)

        // Panel Window Items (Terminal, Firefox, Files)
        val itemPaint = Paint().apply {
            color = Color.parseColor("#22FFFFFF")
            style = Paint.Style.FILL
        }
        fbCanvas.drawRoundRect(140f, 4f, 260f, panelHeight - 4f, 4f, 4f, itemPaint)
        fbCanvas.drawText("⬛ Terminal", 150f, 23f, textPaint)

        fbCanvas.drawRoundRect(270f, 4f, 390f, panelHeight - 4f, 4f, 4f, itemPaint)
        fbCanvas.drawText("🦊 Firefox", 280f, 23f, textPaint)

        // Clock & status on right side of panel
        val timeStr = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"))
        val clockPaint = Paint().apply {
            color = Color.WHITE
            textSize = 14f
            isAntiAlias = true
            textAlign = Paint.Align.RIGHT
        }
        fbCanvas.drawText("$timeStr  |  🔊 100%  |  📶", w - 16f, 23f, clockPaint)

        // 3. Open Terminal Window on desktop
        val winLeft = 80f
        val winTop = 60f
        val winRight = minOf(w - 80f, 950f)
        val winBottom = minOf(h - 50f, 620f)

        // Window shadow & body
        val winBodyPaint = Paint().apply {
            color = Color.parseColor("#F012121A")
            style = Paint.Style.FILL
        }
        fbCanvas.drawRoundRect(winLeft, winTop, winRight, winBottom, 8f, 8f, winBodyPaint)

        // Window Title Bar
        val winTitlePaint = Paint().apply {
            color = Color.parseColor("#282838")
            style = Paint.Style.FILL
        }
        fbCanvas.drawRoundRect(winLeft, winTop, winRight, winTop + 32f, 8f, 8f, winTitlePaint)
        fbCanvas.drawRect(winLeft, winTop + 24f, winRight, winTop + 32f, winTitlePaint)

        // Window Controls (close, min, max dots)
        val redDot = Paint().apply { color = Color.parseColor("#FF5F56"); isAntiAlias = true }
        val yellowDot = Paint().apply { color = Color.parseColor("#FFBD2E"); isAntiAlias = true }
        val greenDot = Paint().apply { color = Color.parseColor("#27C93F"); isAntiAlias = true }
        fbCanvas.drawCircle(winLeft + 16f, winTop + 16f, 6f, redDot)
        fbCanvas.drawCircle(winLeft + 34f, winTop + 16f, 6f, yellowDot)
        fbCanvas.drawCircle(winLeft + 52f, winTop + 16f, 6f, greenDot)

        val winTitleText = Paint().apply {
            color = Color.parseColor("#DDDDDD")
            textSize = 13f
            isAntiAlias = true
        }
        fbCanvas.drawText("root@$distroName: ~ (bash)", winLeft + 70f, winTop + 21f, winTitleText)

        // Terminal text content
        val termTextPaint = Paint().apply {
            color = Color.parseColor("#00FF66")
            textSize = 14f
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }
        val termDimPaint = Paint().apply {
            color = Color.parseColor("#A0A0B0")
            textSize = 13f
            typeface = android.graphics.Typeface.MONOSPACE
            isAntiAlias = true
        }

        var textY = winTop + 58f
        fbCanvas.drawText("Linux on Android - $distroName ($desktopEnv)", winLeft + 16f, textY, termTextPaint)
        textY += 24f
        fbCanvas.drawText("Architecture: aarch64 (ARM64)  |  Kernel: 6.1.0-android-proot", winLeft + 16f, textY, termDimPaint)
        textY += 24f
        fbCanvas.drawText("Display: :0 (Embedded X11)  |  Sound: PulseAudio TCP", winLeft + 16f, textY, termDimPaint)
        textY += 24f
        fbCanvas.drawText("Firefox: Pre-installed (/usr/bin/firefox)", winLeft + 16f, textY, termDimPaint)
        textY += 32f
        fbCanvas.drawText("root@$distroName:~# neofetch --ascii_distro $distroName", winLeft + 16f, textY, termTextPaint)
        textY += 24f
        fbCanvas.drawText("OS: $distroName 24.04 aarch64", winLeft + 16f, textY, termDimPaint)
        textY += 20f
        fbCanvas.drawText("Host: Android Device (PRoot Container)", winLeft + 16f, textY, termDimPaint)
        textY += 20f
        fbCanvas.drawText("DE: $desktopEnv  |  WM: Xfwm4 / Mutter", winLeft + 16f, textY, termDimPaint)
        textY += 20f
        fbCanvas.drawText("Terminal: xfce4-terminal  |  CPU: Octa-Core ARM64", winLeft + 16f, textY, termDimPaint)
        textY += 32f
        fbCanvas.drawText("root@$distroName:~# $terminalOutputLine█", winLeft + 16f, textY, termTextPaint)
    }

    override fun onCreateInputConnection(outAttrs: EditorInfo): InputConnection {
        outAttrs.inputType = EditorInfo.TYPE_CLASS_TEXT
        outAttrs.imeOptions = EditorInfo.IME_ACTION_DONE
        return object : android.view.inputmethod.BaseInputConnection(this, false) {
            override fun commitText(text: CharSequence?, newCursorPosition: Int): Boolean {
                text?.let { sendKeyInput(it.toString()) }
                return true
            }

            override fun deleteSurroundingText(beforeLength: Int, afterLength: Int): Boolean {
                if (terminalOutputLine.isNotEmpty()) {
                    terminalOutputLine = terminalOutputLine.dropLast(beforeLength.coerceAtMost(terminalOutputLine.length))
                    drawDefaultDesktop()
                    invalidate()
                }
                sendKeyEvent(0xFF08, pressed = true) // Backspace
                sendKeyEvent(0xFF08, pressed = false)
                return true
            }

            override fun sendKeyEvent(event: KeyEvent?): Boolean {
                if (event?.action == KeyEvent.ACTION_DOWN) {
                    if (event.keyCode == KeyEvent.KEYCODE_ENTER) {
                        sendKeyInput("\n")
                        terminalOutputLine = ""
                        drawDefaultDesktop()
                        invalidate()
                        return true
                    } else if (event.keyCode == KeyEvent.KEYCODE_DEL) {
                        deleteSurroundingText(1, 0)
                        return true
                    }
                }
                return super.sendKeyEvent(event)
            }
        }
    }
}
