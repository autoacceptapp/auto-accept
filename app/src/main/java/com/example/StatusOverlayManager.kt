package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.hypot

/**
 * StatusOverlayManager
 *
 * Implements an interactive, draggable Floating Action Button overlay (Pill/Circular) that:
 * 1. Shows ONLY when the Master Auto-Accept switch is ON.
 * 2. Displays the Last Accepted Price in large, bold text (e.g., "₹ 150" or "₹ --").
 * 3. Toggles between Active Auto-Accept (Green) and Voice-Only Standby Radar (Red) on tap.
 * 4. Can be dragged smoothly to any position on the screen.
 */
object StatusOverlayManager {
    private const val TAG = "StatusOverlayManager"

    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var priceTextView: TextView? = null
    private var modeLabelView: TextView? = null
    private var backgroundDrawable: GradientDrawable? = null

    private var isAdded = false
    private var lastPrice: String = "₹ --"
    private var updateJob: Job? = null

    // Colors
    private val COLOR_ACTIVE_GREEN = Color.parseColor("#10B981") // Emerald 500
    private val COLOR_ACTIVE_STROKE = Color.parseColor("#047857") // Emerald 700
    private val COLOR_VOICE_RED = Color.parseColor("#EF4444")    // Red 500
    private val COLOR_VOICE_STROKE = Color.parseColor("#B91C1C") // Red 700

    @SuppressLint("ClickableViewAccessibility")
    fun show(context: Context) {
        // Overlay must ONLY be visible when the Master Auto-Accept switch is ON
        if (!AutoAcceptService.isAutomationEnabled(context)) {
            Log.d(TAG, "Master Auto-Accept switch is OFF. Suppressing overlay show.")
            return
        }

        if (isAdded) {
            updateVisualState(context)
            return
        }

        try {
            windowManager = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
            if (windowManager == null) {
                Log.e(TAG, "WindowManager service not available")
                return
            }

            val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (context is android.accessibilityservice.AccessibilityService) {
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                } else if (android.provider.Settings.canDrawOverlays(context)) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                }
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            }

            val layoutParams = WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                overlayType,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = 32
                y = 260
            }

            val density = context.resources.displayMetrics.density

            // Pill-shaped floating container
            val buttonLayout = LinearLayout(context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                val padH = (16 * density).toInt()
                val padV = (10 * density).toInt()
                setPadding(padH, padV, padH, padV)
                minimumWidth = (84 * density).toInt()
                minimumHeight = (50 * density).toInt()
                isClickable = true
                isFocusable = false
            }

            // Background pill drawable with rounded corners and border
            backgroundDrawable = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 25 * density
            }
            buttonLayout.background = backgroundDrawable

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                buttonLayout.elevation = 14 * density
            }

            // Price text view (e.g. "₹ 150" or "₹ --")
            priceTextView = TextView(context).apply {
                text = lastPrice
                setTextColor(Color.WHITE)
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.CENTER
                includeFontPadding = false
            }

            // Mode indicator sub-label ("AUTO" vs "VOICE")
            modeLabelView = TextView(context).apply {
                text = "AUTO"
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                gravity = Gravity.CENTER
                includeFontPadding = false
            }

            buttonLayout.addView(priceTextView)
            buttonLayout.addView(modeLabelView)

            overlayView = buttonLayout

            // Draggable touch listener with robust click vs drag discrimination
            var initialX = 0
            var initialY = 0
            var initialTouchX = 0f
            var initialTouchY = 0f
            var touchDownTime = 0L
            var isDragging = false
            val touchSlop = ViewConfiguration.get(context).scaledTouchSlop

            overlayView?.setOnTouchListener { v, event ->
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        touchDownTime = System.currentTimeMillis()
                        isDragging = false
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        if (hypot(dx.toDouble(), dy.toDouble()) > touchSlop) {
                            isDragging = true
                        }
                        if (isDragging) {
                            layoutParams.x = (initialX + dx).toInt()
                            layoutParams.y = (initialY + dy).toInt()
                            try {
                                windowManager?.updateViewLayout(overlayView, layoutParams)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed updating layout position: ${e.message}")
                            }
                        }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val duration = System.currentTimeMillis() - touchDownTime
                        val dx = event.rawX - initialTouchX
                        val dy = event.rawY - initialTouchY
                        val isClick = !isDragging && (duration < 350) && (hypot(dx.toDouble(), dy.toDouble()) <= touchSlop)

                        if (isClick) {
                            v.performClick()
                            onFloatingButtonClicked(context)
                        }
                        true
                    }
                    else -> false
                }
            }

            windowManager?.addView(overlayView, layoutParams)
            isAdded = true

            // Set initial colors according to KEY_VOICE_ONLY_MODE
            updateVisualState(context)
            startObserving(context)
            Log.i(TAG, "Floating status button displayed successfully.")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to display floating button overlay: ${e.message}", e)
        }
    }

    /**
     * Toggles between Active Auto-Accept and Voice-Only Mode
     */
    private fun onFloatingButtonClicked(context: Context) {
        val currentVoiceOnly = AutoAcceptService.isVoiceOnlyMode(context)
        val newVoiceOnly = !currentVoiceOnly
        AutoAcceptService.setVoiceOnlyMode(context, newVoiceOnly)

        updateVisualState(context)

        // Show corresponding short Toast
        val toastMessage = if (newVoiceOnly) {
            "Standby: Voice Announcer Only"
        } else {
            "Active: Auto-Accept ON"
        }
        Toast.makeText(context.applicationContext, toastMessage, Toast.LENGTH_SHORT).show()
    }

    /**
     * Updates background color and labels based on Voice-Only mode
     */
    private fun updateVisualState(context: Context) {
        val isVoiceOnly = AutoAcceptService.isVoiceOnlyMode(context)
        val density = context.resources.displayMetrics.density

        val bgColor = if (isVoiceOnly) COLOR_VOICE_RED else COLOR_ACTIVE_GREEN
        val strokeColor = if (isVoiceOnly) COLOR_VOICE_STROKE else COLOR_ACTIVE_STROKE

        backgroundDrawable?.setColor(bgColor)
        backgroundDrawable?.setStroke((2 * density).toInt(), strokeColor)

        modeLabelView?.text = if (isVoiceOnly) "VOICE ONLY" else "AUTO-ACCEPT"
        modeLabelView?.setTextColor(if (isVoiceOnly) Color.parseColor("#FEE2E2") else Color.parseColor("#D1FAE5"))
        priceTextView?.text = lastPrice
    }

    private fun startObserving(context: Context) {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && isAdded) {
                // If automation is switched off elsewhere, automatically hide overlay
                if (!AutoAcceptService.isAutomationEnabled(context)) {
                    hide()
                    break
                }
                updateVisualState(context)
                delay(1200)
            }
        }
    }

    /**
     * Updates the last accepted price display on the button.
     * e.g. "₹ 150" or "₹ --"
     */
    fun updateLastPrice(price: Float?) {
        lastPrice = if (price != null && price > 0f) {
            "₹ ${price.toInt()}"
        } else {
            "₹ --"
        }
        priceTextView?.post {
            priceTextView?.text = lastPrice
        }
    }

    /**
     * Hides and removes the floating button overlay safely
     */
    fun hide() {
        if (!isAdded) return
        try {
            updateJob?.cancel()
            updateJob = null
            overlayView?.let {
                windowManager?.removeView(it)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to remove overlay view: ${e.message}", e)
        } finally {
            isAdded = false
            overlayView = null
            priceTextView = null
            modeLabelView = null
            backgroundDrawable = null
        }
    }
}
