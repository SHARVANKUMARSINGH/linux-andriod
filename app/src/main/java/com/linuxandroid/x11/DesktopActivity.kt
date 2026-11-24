package com.linuxandroid.x11

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.HorizontalScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.R
import com.linuxandroid.SessionManager

/**
 * DesktopActivity hosts the embedded X11 GUI desktop directly inside the app,
 * providing interactive controls, soft keyboard injection, resolution switching,
 * touch gestures, and key shortcuts without requiring external companion apps.
 */
class DesktopActivity : AppCompatActivity() {

    private lateinit var embeddedX11View: EmbeddedX11View
    private lateinit var btnKeyboard: TextView
    private lateinit var btnMouseMode: TextView
    private lateinit var btnKeysToggle: TextView
    private lateinit var btnResolution: TextView
    private lateinit var btnFitScreen: TextView
    private lateinit var btnExit: TextView
    private lateinit var quickKeysBar: HorizontalScrollView

    private lateinit var sessionManager: SessionManager
    private var isKeyboardVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable immersive full-screen mode & keep screen awake
        enableImmersiveMode()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_desktop)

        sessionManager = SessionManager(this)

        // Initialize views
        embeddedX11View = findViewById(R.id.embeddedX11View)
        btnKeyboard = findViewById(R.id.btnKeyboard)
        btnMouseMode = findViewById(R.id.btnMouseMode)
        btnKeysToggle = findViewById(R.id.btnKeysToggle)
        btnResolution = findViewById(R.id.btnResolution)
        btnFitScreen = findViewById(R.id.btnFitScreen)
        btnExit = findViewById(R.id.btnExit)
        quickKeysBar = findViewById(R.id.quickKeysBar)

        // Configure distro & desktop environment from session or intent
        val distro = intent.getStringExtra("EXTRA_DISTRO") ?: sessionManager.distroName
        val de = intent.getStringExtra("EXTRA_DESKTOP") ?: sessionManager.desktopEnv
        embeddedX11View.setDistroAndDE(distro, de)

        setupToolbarActions()
        setupQuickKeys()

        // Attempt connection to local display server
        embeddedX11View.connectToLocalDisplay(5901)

        Toast.makeText(
            this,
            "🐧 Linux Desktop ($distro $de) Started!\nTap ⌨️ for keyboard, pinch to zoom",
            Toast.LENGTH_LONG
        ).show()
    }

    private fun enableImmersiveMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            )
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            enableImmersiveMode()
        }
    }

    private fun setupToolbarActions() {
        // 1. Keyboard Toggle
        btnKeyboard.setOnClickListener {
            toggleSoftKeyboard()
        }

        // 2. Mouse Mode Toggle (Direct Touch vs Touchpad Cursor)
        btnMouseMode.setOnClickListener {
            val isTouchpad = embeddedX11View.toggleTouchpadMode()
            btnMouseMode.text = if (isTouchpad) "🖱️ Pad" else "👆 Touch"
            Toast.makeText(
                this,
                if (isTouchpad) "Touchpad Mode: Drag to move cursor, tap to click" else "Direct Touch: Cursor follows finger",
                Toast.LENGTH_SHORT
            ).show()
        }

        // 3. Quick Keys Bar Toggle
        btnKeysToggle.setOnClickListener {
            if (quickKeysBar.visibility == View.VISIBLE) {
                quickKeysBar.visibility = View.GONE
            } else {
                quickKeysBar.visibility = View.VISIBLE
            }
        }

        // 4. Resolution Selector
        btnResolution.setOnClickListener {
            showResolutionDialog()
        }

        // 5. Fit to Screen
        btnFitScreen.setOnClickListener {
            embeddedX11View.fitToScreen()
            Toast.makeText(this, "Desktop fitted to display", Toast.LENGTH_SHORT).show()
        }

        // 6. Exit Button
        btnExit.setOnClickListener {
            confirmExit()
        }
    }

    private fun toggleSoftKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        embeddedX11View.requestFocus()
        if (isKeyboardVisible) {
            imm.hideSoftInputFromWindow(embeddedX11View.windowToken, 0)
            isKeyboardVisible = false
        } else {
            imm.showSoftInput(embeddedX11View, InputMethodManager.SHOW_IMPLICIT)
            isKeyboardVisible = true
        }
    }

    private fun showResolutionDialog() {
        val resolutions = arrayOf(
            "1280 × 720 (HD - 16:9)",
            "1920 × 1080 (Full HD - 16:9)",
            "1024 × 768 (XGA - 4:3)",
            "800 × 600 (SVGA - 4:3)"
        )
        AlertDialog.Builder(this)
            .setTitle("Select Desktop Resolution")
            .setItems(resolutions) { _, which ->
                when (which) {
                    0 -> {
                        embeddedX11View.setResolution(1280, 720)
                        btnResolution.text = "🖥️ 720p"
                    }
                    1 -> {
                        embeddedX11View.setResolution(1920, 1080)
                        btnResolution.text = "🖥️ 1080p"
                    }
                    2 -> {
                        embeddedX11View.setResolution(1024, 768)
                        btnResolution.text = "🖥️ 768p"
                    }
                    3 -> {
                        embeddedX11View.setResolution(800, 600)
                        btnResolution.text = "🖥️ 600p"
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun setupQuickKeys() {
        findViewById<TextView>(R.id.keyEsc).setOnClickListener {
            embeddedX11View.sendSpecialKey(0xFF1B) // ESC
        }
        findViewById<TextView>(R.id.keyTab).setOnClickListener {
            embeddedX11View.sendSpecialKey(0xFF09) // TAB
        }
        findViewById<TextView>(R.id.keyCtrlC).setOnClickListener {
            embeddedX11View.sendCtrlC()
        }
        findViewById<TextView>(R.id.keyCtrlZ).setOnClickListener {
            embeddedX11View.sendKeyEvent(0xFFE3, pressed = true) // Control_L
            embeddedX11View.sendKeyEvent('z'.code, pressed = true)
            embeddedX11View.sendKeyEvent('z'.code, pressed = false)
            embeddedX11View.sendKeyEvent(0xFFE3, pressed = false)
        }
        findViewById<TextView>(R.id.keyAlt).setOnClickListener {
            embeddedX11View.sendKeyEvent(0xFFE9, pressed = true) // Alt_L
            embeddedX11View.sendKeyEvent(0xFFE9, pressed = false)
        }
        findViewById<TextView>(R.id.keySuper).setOnClickListener {
            embeddedX11View.sendKeyEvent(0xFFEB, pressed = true) // Super_L
            embeddedX11View.sendKeyEvent(0xFFEB, pressed = false)
        }
        findViewById<TextView>(R.id.keyUp).setOnClickListener {
            embeddedX11View.sendSpecialKey(0xFF52) // Up
        }
        findViewById<TextView>(R.id.keyDown).setOnClickListener {
            embeddedX11View.sendSpecialKey(0xFF54) // Down
        }
        findViewById<TextView>(R.id.keyLeft).setOnClickListener {
            embeddedX11View.sendSpecialKey(0xFF51) // Left
        }
        findViewById<TextView>(R.id.keyRight).setOnClickListener {
            embeddedX11View.sendSpecialKey(0xFF53) // Right
        }
        findViewById<TextView>(R.id.keyEnter).setOnClickListener {
            embeddedX11View.sendSpecialKey(0xFF0D) // Enter
        }
    }

    private fun confirmExit() {
        AlertDialog.Builder(this)
            .setTitle("Exit Desktop Session?")
            .setMessage("Your Linux container environment remains active in the background. Would you like to close the embedded display?")
            .setPositiveButton("Close Display") { _, _ ->
                embeddedX11View.disconnect()
                finish()
            }
            .setNegativeButton("Stay", null)
            .show()
    }

    override fun onBackPressed() {
        confirmExit()
    }

    override fun onDestroy() {
        super.onDestroy()
        embeddedX11View.disconnect()
    }
}