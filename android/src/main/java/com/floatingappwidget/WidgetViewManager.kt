package com.floatingappwidget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.BounceInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.WritableMap
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Manages the floating widget view and its interactions
 */
class WidgetViewManager(private val context: Context) {

    private var windowManager: WindowManager? = null
    private var widgetContainer: FrameLayout? = null
    private var widgetView: ImageView? = null
    private var badgeView: TextView? = null
    private var dismissZoneView: TextView? = null
    private var layoutParams: WindowManager.LayoutParams? = null
    private var dismissZoneParams: WindowManager.LayoutParams? = null
    private var config: WidgetConfig? = null

    // For drag functionality
    private var initialX: Int = 0
    private var initialY: Int = 0
    private var initialTouchX: Float = 0f
    private var initialTouchY: Float = 0f
    private var isDragging: Boolean = false
    private var hasMoved: Boolean = false
    private var lastUpdateTime: Long = 0
    private var isLongPressTriggered: Boolean = false
    private var velocityX: Float = 0f
    private var velocityY: Float = 0f
    private var lastX: Float = 0f
    private var lastY: Float = 0f
    private var snapAnimator: ValueAnimator? = null

    // For long press detection
    private val longPressHandler = Handler(Looper.getMainLooper())
    private var longPressRunnable: Runnable? = null
    private var isLongPress: Boolean = false
    private var pressStartTime: Long = 0

    companion object {
        private const val CLICK_DRAG_TOLERANCE = 10 // pixels
        private const val FLING_THRESHOLD = 5000f // pixels per second
        private const val VELOCITY_SAMPLES = 3
    }

    /**
     * Create and show the floating widget
     */
    @SuppressLint("ClickableViewAccessibility")
    fun showWidget(widgetConfig: WidgetConfig) {
        this.config = widgetConfig

        // Initialize WindowManager
        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        // Create container for widget + badge
        widgetContainer = FrameLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

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

            // Set background shape with custom appearance
            background = createBackgroundDrawable(widgetConfig)

            // Set padding from appearance config
            val paddingPx = dpToPx(widgetConfig.appearance.padding)
            setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

            // Set opacity from appearance config
            alpha = widgetConfig.appearance.opacity

            // Set scale type
            scaleType = ImageView.ScaleType.FIT_CENTER

            // Set size
            val sizePx = dpToPx(widgetConfig.size)
            layoutParams = FrameLayout.LayoutParams(sizePx, sizePx)

            // Set touch listener for drag and click
            setOnTouchListener { v, event ->
                handleTouch(v, event)
            }
        }

        // Add widget to container
        widgetContainer?.addView(widgetView)

        // Create badge if configured
        if (widgetConfig.badge != null) {
            createBadgeView(widgetConfig.badge, dpToPx(widgetConfig.size))?.let { badge ->
                badgeView = badge
                widgetContainer?.addView(badge)
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
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START

            // Set initial position
            x = widgetConfig.initialX ?: getDefaultX()
            y = widgetConfig.initialY ?: getDefaultY()
        }

        // Don't show dismiss zone on widget creation - it will be shown when needed
        // based on the showOn trigger (ALWAYS on drag, or LONG_PRESS on long press)

        // Add view to window
        try {
            windowManager?.addView(widgetContainer, layoutParams)

            // Send show lifecycle event
            if (widgetConfig.hasShowCallback && context is ReactApplicationContext) {
                sendLifecycleEvent(context, "onWidgetShow")
            }
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
            // Cancel any running animations
            snapAnimator?.cancel()
            snapAnimator = null

            // Cancel long press detection
            longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
            longPressRunnable = null

            // Send hide lifecycle event
            val config = this.config
            if (config?.hasHideCallback == true && context is ReactApplicationContext) {
                sendLifecycleEvent(context, "onWidgetHide")
            }

            // Clean up bitmaps to prevent memory leaks
            widgetView?.let { imageView ->
                val drawable = imageView.drawable
                if (drawable is BitmapDrawable) {
                    drawable.bitmap?.recycle()
                }
                imageView.setImageDrawable(null)
            }

            widgetContainer?.let { view ->
                windowManager?.removeView(view)
            }
            dismissZoneView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            // View might already be removed
        } finally {
            widgetView = null
            widgetContainer = null
            badgeView = null
            dismissZoneView = null
            layoutParams = null
            dismissZoneParams = null
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
     * Handle touch events for drag, click, and long press
     */
    @SuppressLint("ClickableViewAccessibility")
    private fun handleTouch(view: View, event: MotionEvent): Boolean {
        val config = this.config ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // Cancel any ongoing animations
                snapAnimator?.cancel()

                // Save initial position
                initialX = layoutParams?.x ?: 0
                initialY = layoutParams?.y ?: 0
                initialTouchX = event.rawX
                initialTouchY = event.rawY
                isDragging = false
                hasMoved = false
                isLongPress = false
                pressStartTime = System.currentTimeMillis()

                // Initialize velocity tracking
                lastX = event.rawX
                lastY = event.rawY
                lastUpdateTime = System.currentTimeMillis()
                velocityX = 0f
                velocityY = 0f

                // Animate press effect
                animatePress(true)
                performHapticFeedback()

                // Start long press detection
                if (config.hasLongPressCallback) {
                    longPressRunnable = Runnable { handleLongPress() }
                    longPressHandler.postDelayed(longPressRunnable!!, config.longPressDuration)
                }

                // Show dismiss zone if enabled with ALWAYS trigger
                if ((config.dismissZone.enabled || config.enableDragToDismiss) &&
                    config.dismissZone.showOn == DismissZoneConfig.ShowTrigger.ALWAYS) {
                    showDismissZone()
                }

                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (!config.draggable) return false

                val deltaX = event.rawX - initialTouchX
                val deltaY = event.rawY - initialTouchY

                // Check if movement exceeds tolerance
                if (abs(deltaX) > CLICK_DRAG_TOLERANCE ||
                    abs(deltaY) > CLICK_DRAG_TOLERANCE) {
                    isDragging = true
                    hasMoved = true

                    // Cancel long press detection
                    longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                    longPressRunnable = null

                    // Reset press animation
                    animatePress(false)

                    // Calculate velocity
                    val currentTime = System.currentTimeMillis()
                    val timeDelta = (currentTime - lastUpdateTime) / 1000f // seconds
                    if (timeDelta > 0) {
                        velocityX = (event.rawX - lastX) / timeDelta
                        velocityY = (event.rawY - lastY) / timeDelta
                    }
                    lastX = event.rawX
                    lastY = event.rawY
                    lastUpdateTime = currentTime

                    // Update position with constraints
                    val rawX = initialX + deltaX.toInt()
                    val rawY = initialY + deltaY.toInt()
                    val (newX, newY) = applyConstraints(rawX, rawY)

                    layoutParams?.apply {
                        x = newX
                        y = newY
                    }

                    // Update view
                    windowManager?.updateViewLayout(widgetContainer, layoutParams)

                    // Update dismiss zone if drag-to-dismiss is enabled
                    if (config.dismissZone.enabled || config.enableDragToDismiss) {
                        val isInDismissZone = isInDismissZone(newY)
                        updateDismissZoneAppearance(isInDismissZone)

                        // Send drag event if callback is registered
                        if (config.hasDragCallback && context is ReactApplicationContext) {
                            sendDragEvent(context, newX, newY, isInDismissZone)
                        }
                    }
                }
                return true
            }

            MotionEvent.ACTION_UP -> {
                // Cancel long press detection
                longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                longPressRunnable = null

                // Reset long press flag for next interaction
                isLongPressTriggered = false

                // Reset press animation
                animatePress(false)

                // Hide dismiss zone
                if (config.dismissZone.enabled || config.enableDragToDismiss) {
                    hideDismissZone()

                    // Check if widget should be dismissed
                    if (hasMoved && isInDismissZone(layoutParams?.y ?: 0)) {
                        // Send dismiss lifecycle event
                        if (config.hasDismissCallback && context is ReactApplicationContext) {
                            sendLifecycleEvent(context, "onWidgetDismiss")
                        }
                        hideWidget()
                        return true
                    }
                }

                if (!hasMoved && !isLongPress) {
                    // This was a click, not a drag or long press
                    handleWidgetClick()
                } else if (hasMoved) {
                    // Send position change event
                    val currentX = layoutParams?.x ?: 0
                    val currentY = layoutParams?.y ?: 0
                    if (config.hasPositionChangeCallback && context is ReactApplicationContext) {
                        try {
                            val params = Arguments.createMap().apply {
                                putInt("x", currentX)
                                putInt("y", currentY)
                            }
                            FloatingAppWidgetModule.sendEvent(context, "onWidgetPositionChange", params)
                        } catch (e: Exception) {
                            // Safe to ignore - React context may not be available
                        }
                    }

                    // Handle snap to edge if enabled
                    if (config.snapToEdge) {
                        snapToEdge()
                    }
                }

                isDragging = false
                hasMoved = false
                isLongPress = false
                return true
            }

            MotionEvent.ACTION_CANCEL -> {
                // Cancel long press detection
                longPressRunnable?.let { longPressHandler.removeCallbacks(it) }
                longPressRunnable = null

                // Reset press animation
                animatePress(false)

                // Hide dismiss zone
                if (config.dismissZone.enabled || config.enableDragToDismiss) {
                    hideDismissZone()
                }

                isDragging = false
                hasMoved = false
                isLongPress = false
                return true
            }
        }

        return false
    }

    /**
     * Handle widget click
     */
    private fun handleWidgetClick() {
        val config = this.config ?: return

        if (config.hasClickCallback && context is ReactApplicationContext) {
            // Send click event to React Native
            sendClickEvent(context)
        } else {
            // Default behavior: open the app
            openApp()
        }
    }

    /**
     * Open the app when widget is clicked (default behavior)
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
     * Send click event to React Native
     */
    private fun sendClickEvent(context: ReactApplicationContext) {
        try {
            val params = Arguments.createMap().apply {
                putDouble("timestamp", System.currentTimeMillis().toDouble())
            }
            FloatingAppWidgetModule.sendEvent(context, "onWidgetClick", params)
        } catch (e: Exception) {
            // Safe to ignore - React context may not be available
        }
    }

    /**
     * Send drag event to React Native
     */
    private fun sendDragEvent(context: ReactApplicationContext, x: Int, y: Int, inDismissZone: Boolean) {
        try {
            val params = Arguments.createMap().apply {
                putInt("x", x)
                putInt("y", y)
                putBoolean("inDismissZone", inDismissZone)
            }
            FloatingAppWidgetModule.sendEvent(context, "onWidgetDrag", params)
        } catch (e: Exception) {
            // Safe to ignore - React context may not be available
        }
    }

    /**
     * Snap widget to nearest screen edge with smooth animation
     */
    private fun snapToEdge() {
        val config = this.config ?: return
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val currentX = layoutParams?.x ?: 0
        val currentY = layoutParams?.y ?: 0

        // Determine which edge is closer
        val distanceToLeft = currentX
        val distanceToRight = screenWidth - currentX - dpToPx(config.size)
        val targetX = if (distanceToLeft < distanceToRight) 0 else screenWidth - dpToPx(config.size)

        // Create smooth animation with custom settings
        snapAnimator = ValueAnimator.ofInt(currentX, targetX).apply {
            duration = config.animations.snapDuration.toLong()
            interpolator = getInterpolator(config.animations.snapInterpolator)
            addUpdateListener { animation ->
                val animatedX = animation.animatedValue as Int
                layoutParams?.apply {
                    x = animatedX
                }
                try {
                    windowManager?.updateViewLayout(widgetContainer, layoutParams)
                } catch (e: Exception) {
                    // View might have been removed
                    animation.cancel()
                }
            }
            start()
        }
    }

    /**
     * Check if widget is in dismiss zone
     */
    private fun isInDismissZone(y: Int): Boolean {
        val config = this.config ?: return false
        if (!config.dismissZone.enabled && !config.enableDragToDismiss) return false

        val displayMetrics = context.resources.displayMetrics
        val screenHeight = displayMetrics.heightPixels

        // Use new config if available, fallback to legacy
        val dismissZoneHeight = if (config.dismissZone.enabled) {
            dpToPx(config.dismissZone.height)
        } else {
            dpToPx(config.dismissZoneHeight)
        }

        // Check position based on dismiss zone placement
        return if (config.dismissZone.position == DismissZoneConfig.Position.TOP) {
            y <= dismissZoneHeight
        } else {
            // BOTTOM (default)
            val dismissZoneTop = screenHeight - dismissZoneHeight
            y >= dismissZoneTop
        }
    }

    /**
     * Show dismiss zone with custom configuration
     */
    private fun showDismissZone() {
        if (dismissZoneView != null) return

        val config = this.config ?: return

        // Check if we should show the dismiss zone based on the trigger mode
        if (config.dismissZone.showOn == DismissZoneConfig.ShowTrigger.LONG_PRESS) {
            // Only show if long press was triggered
            if (!isLongPressTriggered) {
                return
            }
        }
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels

        // Use new config if available, fallback to legacy
        val dismissZoneHeight = if (config.dismissZone.enabled) {
            dpToPx(config.dismissZone.height)
        } else {
            dpToPx(config.dismissZoneHeight)
        }

        val dismissZoneText = if (config.dismissZone.enabled) {
            config.dismissZone.text
        } else {
            "⊗ Release to remove"
        }

        val dismissZoneTextColor = if (config.dismissZone.enabled) {
            config.dismissZone.textColor
        } else {
            Color.WHITE
        }

        val dismissZoneTextSize = if (config.dismissZone.enabled) {
            config.dismissZone.textSize.toFloat()
        } else {
            16f
        }

        // Create dismiss zone view with gradient or solid color background
        dismissZoneView = TextView(context).apply {
            text = dismissZoneText
            setTextColor(dismissZoneTextColor)
            gravity = Gravity.CENTER
            textSize = dismissZoneTextSize
            alpha = 0.5f

            // Set background with gradient or solid color
            background = createDismissZoneBackground(config, false)
        }

        val overlayType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        dismissZoneParams = WindowManager.LayoutParams(
            screenWidth,
            dismissZoneHeight,
            overlayType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            // Position based on config
            gravity = if (config.dismissZone.position == DismissZoneConfig.Position.TOP) {
                Gravity.TOP or Gravity.CENTER_HORIZONTAL
            } else {
                Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            }
        }

        try {
            windowManager?.addView(dismissZoneView, dismissZoneParams)
        } catch (e: Exception) {
            // Handle exception
            dismissZoneView = null
        }
    }

    /**
     * Hide dismiss zone
     */
    private fun hideDismissZone() {
        try {
            dismissZoneView?.let { view ->
                windowManager?.removeView(view)
            }
        } catch (e: Exception) {
            // View might already be removed
        } finally {
            dismissZoneView = null
            dismissZoneParams = null
        }
    }

    /**
     * Update dismiss zone appearance based on whether widget is inside
     */
    private fun updateDismissZoneAppearance(isInZone: Boolean) {
        val config = this.config ?: return

        dismissZoneView?.apply {
            alpha = if (isInZone) 1.0f else 0.5f
            background = createDismissZoneBackground(config, isInZone)
        }
    }

    /**
     * Create background drawable for dismiss zone with gradient or solid color
     */
    private fun createDismissZoneBackground(config: WidgetConfig, isActive: Boolean): GradientDrawable {
        val dismissConfig = config.dismissZone

        return GradientDrawable().apply {
            // Determine colors - gradient takes priority over solid color
            val useGradient = if (isActive) {
                dismissConfig.activeGradientColors != null
            } else {
                dismissConfig.gradientColors != null
            }

            if (useGradient) {
                // Use gradient colors
                val colors = if (isActive) {
                    dismissConfig.activeGradientColors ?: dismissConfig.gradientColors!!
                } else {
                    dismissConfig.gradientColors!!
                }

                // Set gradient orientation
                orientation = when (dismissConfig.gradientOrientation) {
                    DismissZoneConfig.GradientOrientation.HORIZONTAL -> GradientDrawable.Orientation.LEFT_RIGHT
                    DismissZoneConfig.GradientOrientation.VERTICAL -> GradientDrawable.Orientation.TOP_BOTTOM
                    DismissZoneConfig.GradientOrientation.DIAGONAL_TL_BR -> GradientDrawable.Orientation.TL_BR
                    DismissZoneConfig.GradientOrientation.DIAGONAL_BL_TR -> GradientDrawable.Orientation.BL_TR
                }

                // Set gradient colors
                setColors(colors)
            } else {
                // Use solid color (legacy or when gradient not specified)
                val backgroundColor = if (config.dismissZone.enabled) {
                    if (isActive) {
                        dismissConfig.activeBackgroundColor
                    } else {
                        dismissConfig.backgroundColor
                    }
                } else {
                    // Legacy behavior
                    if (isActive) Color.argb(200, 255, 0, 0) else Color.argb(150, 255, 0, 0)
                }
                setColor(backgroundColor)
            }

            // Set corner radius for curved edges
            if (dismissConfig.cornerRadius > 0) {
                cornerRadius = dpToPx(dismissConfig.cornerRadius).toFloat()
            }
        }
    }

    /**
     * Create background drawable based on shape and appearance config
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
                cornerRadius = dpToPx(config.appearance.cornerRadius).toFloat()
            }

            // Set background color from appearance config
            setColor(config.appearance.backgroundColor)

            // Set stroke (border) from appearance config
            setStroke(dpToPx(config.appearance.borderWidth), config.appearance.borderColor)
        }
    }

    /**
     * Create badge view for notifications/status
     */
    private fun createBadgeView(badgeConfig: BadgeConfig, widgetSize: Int): TextView? {
        val badgeText = when {
            badgeConfig.text != null -> badgeConfig.text
            badgeConfig.count != null && badgeConfig.count > 0 -> {
                if (badgeConfig.count > 99) "99+" else badgeConfig.count.toString()
            }
            else -> return null
        }

        val badgeSize = dpToPx(badgeConfig.size)
        val badge = TextView(context).apply {
            text = badgeText
            setTextColor(badgeConfig.textColor)
            textSize = badgeConfig.textSize.toFloat()
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(badgeConfig.backgroundColor)
            }
            layoutParams = FrameLayout.LayoutParams(badgeSize, badgeSize).apply {
                // Position badge based on config
                gravity = when (badgeConfig.position) {
                    BadgeConfig.Position.TOP_RIGHT -> Gravity.TOP or Gravity.END
                    BadgeConfig.Position.TOP_LEFT -> Gravity.TOP or Gravity.START
                    BadgeConfig.Position.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
                    BadgeConfig.Position.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
                }
            }
        }

        return badge
    }

    /**
     * Animate press effect if enabled
     */
    private fun animatePress(pressed: Boolean) {
        val config = this.config ?: return
        if (!config.animations.enableScaleOnPress) return

        val targetScale = if (pressed) config.animations.pressScale else 1.0f
        widgetView?.animate()
            ?.scaleX(targetScale)
            ?.scaleY(targetScale)
            ?.setDuration(100)
            ?.start()
    }

    /**
     * Perform haptic feedback if enabled
     */
    private fun performHapticFeedback() {
        val config = this.config ?: return
        if (!config.animations.enableHapticFeedback) return

        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (e: Exception) {
            // Vibrator not available or permission issue
        }
    }

    /**
     * Apply position constraints
     */
    private fun applyConstraints(x: Int, y: Int): Pair<Int, Int> {
        val config = this.config ?: return Pair(x, y)
        val constraints = config.constraints
        var newX = x
        var newY = y

        // Apply min/max constraints
        if (constraints.minX != null) {
            newX = max(newX, constraints.minX)
        }
        if (constraints.maxX != null) {
            newX = min(newX, constraints.maxX)
        }
        if (constraints.minY != null) {
            newY = max(newY, constraints.minY)
        }
        if (constraints.maxY != null) {
            newY = min(newY, constraints.maxY)
        }

        // Keep on screen if enabled
        if (constraints.keepOnScreen) {
            val displayMetrics = context.resources.displayMetrics
            val screenWidth = displayMetrics.widthPixels
            val screenHeight = displayMetrics.heightPixels
            val widgetSize = dpToPx(config.size)

            newX = max(0, min(newX, screenWidth - widgetSize))
            newY = max(0, min(newY, screenHeight - widgetSize))
        }

        // Snap to grid if enabled
        if (constraints.snapToGrid > 0) {
            val gridSize = dpToPx(constraints.snapToGrid)
            newX = (newX / gridSize) * gridSize
            newY = (newY / gridSize) * gridSize
        }

        return Pair(newX, newY)
    }

    /**
     * Handle long press detection
     */
    private fun handleLongPress() {
        val config = this.config ?: return

        isLongPress = true
        isLongPressTriggered = true // Set flag for dismiss zone trigger
        performHapticFeedback()

        // If dismiss zone is enabled and set to show on long press, show it now
        if (config.dismissZone.enabled &&
            config.dismissZone.showOn == DismissZoneConfig.ShowTrigger.LONG_PRESS) {
            showDismissZone()
        }

        if (config.hasLongPressCallback && context is ReactApplicationContext) {
            try {
                val duration = System.currentTimeMillis() - pressStartTime
                val params = Arguments.createMap().apply {
                    putDouble("timestamp", System.currentTimeMillis().toDouble())
                    putDouble("duration", duration.toDouble())
                }
                FloatingAppWidgetModule.sendEvent(context, "onWidgetLongPress", params)
            } catch (e: Exception) {
                // Safe to ignore - React context may not be available
            }
        }
    }

    /**
     * Send lifecycle event to React Native
     */
    private fun sendLifecycleEvent(context: ReactApplicationContext, eventName: String) {
        try {
            val params = Arguments.createMap()
            FloatingAppWidgetModule.sendEvent(context, eventName, params)
        } catch (e: Exception) {
            // Safe to ignore - React context may not be available
        }
    }

    /**
     * Get interpolator based on config
     */
    private fun getInterpolator(type: AnimationConfig.InterpolatorType): Interpolator {
        return when (type) {
            AnimationConfig.InterpolatorType.DECELERATE -> DecelerateInterpolator()
            AnimationConfig.InterpolatorType.ACCELERATE -> AccelerateInterpolator()
            AnimationConfig.InterpolatorType.LINEAR -> LinearInterpolator()
            AnimationConfig.InterpolatorType.BOUNCE -> BounceInterpolator()
            AnimationConfig.InterpolatorType.OVERSHOOT -> OvershootInterpolator()
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
