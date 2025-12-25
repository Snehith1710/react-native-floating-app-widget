package com.floatingappwidget

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import androidx.core.content.ContextCompat

/**
 * Manages the floating widget view and its interactions
 */
class WidgetViewManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var widgetView: ImageView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var config: WidgetConfig? = null

    // For drag functionality
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isDragging: Boolean = false
    private var hasMoved: Boolean = false

    companion object {
        private const val CLICK_DRAG_TOLERANCE = 10 // pixels
    }

    /**
     * Create and show the floating widget
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showWidget(widgetConfig: WidgetConfig) {
        this.config = widgetConfig

        // Initialize WindowManager
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Create widget view
        widgetView = ImageView(context).apply {
            // Set icon
            if (widgetConfig.icon != null) {
                setImageBitmap(widgetConfig.icon)
            } else {
                // Use app icon as default
                val appIcon = context.packageManager.getApplicationIcon(context.packageName)
                setImageDrawable(appIcon)
            }

            // Set background shape
            background = createBackgroundDrawable(widgetConfig)

            // Set padding
            val paddingPx = dpToPx(8)
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            // Set scale type
            scaleType = ImageView.ScaleType.FIT_CENTER

            // Set touch listener for drag and click
            setOnTouchListener { v, event ->
                handleTouch(v, event)
            }
        }

        // Create layout params
        val sizePx = dpToPx(widgetConfig.size)

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            sizePx,
            sizePx,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START

            // Set initial position
            x = widgetConfig.initialX ?: getDefaultX()
            y = widgetConfig.initialY ?: getDefaultY()
        }

        // Add view to window
        try {
            windowManager?.addView(widgetView, layoutParams)
        } catch (e: Exception) {
            // Handle exception (e.g., permission not granted)
            throw SecurityException("Failed to add widget view. SYSTEM_ALERT_WINDOW permission may not be granted.", e)
        }
    }

    /**
     * Remove the widget from screen
     */
    fun hideWidget() {
        try {
            widgetView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            // View might already be removed
        } finally {
            widgetView = null
            layoutParams = null
        }
    }

    /**
     * Update widget configuration
     */
    fun updateWidget(newConfig: WidgetConfig) {
        hideWidget()
        showWidget(newConfig)
    }

    /**
     * Check if widget is currently visible
     */
    fun isVisible(): Boolean {
        return widgetView != null
    }

    /**
     * Handle touch events for drag and click
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouch(view: View, event: MotionEvent): Boolean {
        val config = this.config ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Save initial position
                initialX = layoutParams?.x ?: 0
                initialY = layoutParams?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                hasMoved = false
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!config.draggable) return false

                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                // Check if movement exceeds tolerance
                if (Math.abs(deltaX) > CLICK_DRAG_TOLERANCE ||
                    Math.abs(deltaY) > CLICK_DRAG_TOLERANCE) {
                    isDragging = true
                    hasMoved = true

                    // Update position
                    layoutParams?.apply {
                        x = initialX + deltaX.toInt()
                        y = initialY + deltaY.toInt()
                    }

                    // Update view
                    windowManager?.updateViewLayout(widgetView, layoutParams)
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                if (!hasMoved) {
                    // This was a click, not a drag - open the app
                    openApp()
                }
                isDragging = false
                hasMoved = false
                return true
            }
        }

        return false
    }

    /**
     * Open the app when widget is clicked
     */
    private fun openApp() {
        try {
            val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
            launchIntent?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                context.startActivity(this)
            }
        } catch (e: Exception) {
            // Handle exception
        }
    }

    /**
     * Create background drawable based on shape
     */
    private fun createBackgroundDrawable(config: WidgetConfig): GradientDrawable {
        return GradientDrawable().apply {
            // Set shape
            shape = when (config.shape) {
                WidgetConfig.WidgetShape.CIRCLE -> GradientDrawable.OVAL
                WidgetConfig.WidgetShape.ROUNDED -> GradientDrawable.RECTANGLE
            }

            // Set corner radius for rounded shape
            if (config.shape == WidgetConfig.WidgetShape.ROUNDED) {
                cornerRadius = dpToPx(12).toFloat()
            }

            // Set color (semi-transparent white background)
            setColor(0xCCFFFFFF.toInt())

            // Set stroke (border)
            setStroke(dpToPx(2), 0xFFCCCCCC.toInt())
        }
    }

    /**
     * Convert dp to pixels
     */
    private fun dpToPx(dp: Int): Int {
        return (dp * context.resources.displayMetrics.density).toInt()
    }

    /**
     * Get default X position (right side of screen)
     */
    private fun getDefaultX(): Int {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val widgetSize = dpToPx(config?.size ?: 56)
        return screenWidth - widgetSize - dpToPx(16)
    }

    /**
     * Get default Y position (top of screen)
     */
    private fun getDefaultY(): Int {
        return dpToPx(100)
    }
}
