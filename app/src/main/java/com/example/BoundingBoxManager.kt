package com.example

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.util.Log
import android.view.View
import android.view.WindowManager
import kotlinx.coroutines.*

class BoundingBoxOverlayView(context: Context) : View(context) {
    private val paint = Paint().apply {
        color = Color.parseColor("#FF00FF") // Magenta
        style = Paint.Style.STROKE
        strokeWidth = 12f
    }
    private val fillPaint = Paint().apply {
        color = Color.parseColor("#33FF00FF") // Transparent Magenta
        style = Paint.Style.FILL
    }
    private var currentRect: Rect? = null

    fun setRect(rect: Rect?) {
        currentRect = rect
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        currentRect?.let {
            canvas.drawRect(it, fillPaint)
            canvas.drawRect(it, paint)
        }
    }
}

object BoundingBoxManager {
    private var windowManager: WindowManager? = null
    private var overlayView: BoundingBoxOverlayView? = null
    private var isAdded = false
    private var clearJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main)

    fun show(context: Context) {
        if (isAdded) return
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        overlayView = BoundingBoxOverlayView(context)

        try {
            windowManager?.addView(overlayView, layoutParams)
            isAdded = true
        } catch (e: Exception) {
            Log.e("BoundingBoxManager", "Failed to add overlay: ${e.message}")
        }
    }

    fun updateBox(rect: Rect?) {
        if (!isAdded) return
        scope.launch {
            overlayView?.setRect(rect)
            clearJob?.cancel()
            if (rect != null) {
                clearJob = launch {
                    delay(1500) // Clear after 1.5 seconds if not updated
                    overlayView?.setRect(null)
                }
            }
        }
    }

    fun hide() {
        if (!isAdded) return
        try {
            clearJob?.cancel()
            windowManager?.removeView(overlayView)
            isAdded = false
            overlayView = null
        } catch (e: Exception) {
            Log.e("BoundingBoxManager", "Failed to remove overlay: ${e.message}")
        }
    }
}
