package com.example

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

object StatusOverlayManager {
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var statusTextView: TextView? = null
    private var priceTextView: TextView? = null
    
    private var isAdded = false
    private var lastPrice: String = "--"
    
    private var updateJob: Job? = null

    @SuppressLint("ClickableViewAccessibility")
    fun show(context: Context) {
        if (isAdded) return
        
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )
        
        layoutParams.gravity = Gravity.TOP or Gravity.START
        layoutParams.x = 0
        layoutParams.y = 200

        val linearLayout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#CC000000")) // Semi-transparent black
            setPadding(24, 16, 24, 16)
        }

        statusTextView = TextView(context).apply {
            text = "Status: Idle"
            setTextColor(Color.WHITE)
            textSize = 14f
        }
        
        priceTextView = TextView(context).apply {
            text = "Last Price: ₹--"
            setTextColor(Color.WHITE)
            textSize = 14f
        }

        linearLayout.addView(statusTextView)
        linearLayout.addView(priceTextView)
        
        overlayView = linearLayout

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        overlayView?.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = layoutParams.x
                    initialY = layoutParams.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                    layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(overlayView, layoutParams)
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(overlayView, layoutParams)
            isAdded = true
            startUpdating(context)
        } catch (e: Exception) {
            Log.e("StatusOverlayManager", "Failed to add overlay: ${e.message}")
        }
    }

    private fun startUpdating(context: Context) {
        updateJob?.cancel()
        updateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive && isAdded) {
                val isActive = AutoAcceptService.isAutomationEnabled(context)
                statusTextView?.text = if (isActive) "Status: Active" else "Status: Idle"
                statusTextView?.setTextColor(if (isActive) Color.GREEN else Color.RED)
                
                priceTextView?.text = "Last Price: $lastPrice"
                delay(1000)
            }
        }
    }

    fun updateLastPrice(price: Float?) {
        lastPrice = if (price != null) "₹${price.toInt()}" else "--"
    }

    fun hide() {
        if (!isAdded) return
        try {
            updateJob?.cancel()
            windowManager?.removeView(overlayView)
            isAdded = false
            overlayView = null
        } catch (e: Exception) {
            Log.e("StatusOverlayManager", "Failed to remove overlay: ${e.message}")
        }
    }
}
