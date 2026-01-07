# Native Code Implementation - Apply These Changes

## Status: WidgetConfig.kt ✅ COMPLETE

The WidgetConfig.kt file has been fully updated with all configuration classes.

## Remaining: WidgetViewManager.kt - Apply These Updates

### 1. Add Imports (at top of file, after existing imports)

```kotlin
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.animation.*
import kotlin.math.max
import kotlin.math.min
```

### 2. Add Fields to WidgetViewManager class (after existing fields around line 55)

```kotlin
// For badge
private var badgeView: TextView? = null

// For long press
private var longPressHandler: Handler? = null
private var longPressRunnable: Runnable? = null
private var isLongPress: Boolean = false
private var pressStartTime: Long = 0
```

### 3. Initialize Handler in showWidget (add after line 71)

```kotlin
// Initialize long press handler
longPressHandler = Handler(Looper.getMainLooper())
```

### 4. Replace showWidget method (starting at line 67)

**REPLACE the entire showWidget method with this:**

```kotlin
@SuppressLint("ClickableViewAccessibility")
fun showWidget(widgetConfig: WidgetConfig) {
    this.config = widgetConfig

    // Initialize WindowManager
    windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    // Initialize long press handler
    longPressHandler = Handler(Looper.getMainLooper())

    // Create container for widget + badge
    widgetContainer = FrameLayout(context)

    // Create widget image view
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

        // Set custom padding
        val paddingPx = dpToPx(widgetConfig.appearance.padding)
        setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

        // Set custom opacity
        alpha = widgetConfig.appearance.opacity

        // Set scale type
        scaleType = ImageView.ScaleType.FIT_CENTER

        // Layout params for image view
        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    widgetContainer?.addView(widgetView)

    // Add badge if configured
    if (widgetConfig.badge != null) {
        badgeView = createBadgeView(widgetConfig.badge)
        widgetContainer?.addView(badgeView)
    }

    // Set touch listener on container
    widgetContainer?.setOnTouchListener { v, event ->
        handleTouch(v, event)
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

    // Show dismiss zone if enabled (check both new and deprecated config)
    val dismissEnabled = widgetConfig.dismissZone.enabled || widgetConfig.enableDragToDismiss
    if (dismissEnabled) {
        // Don't show initially, show on drag
    }

    // Add view to window
    try {
        windowManager?.addView(widgetContainer, layoutParams)

        // Emit show event
        if (widgetConfig.hasShowCallback && context is ReactApplicationContext) {
            FloatingAppWidgetModule.sendEvent(context, "onWidgetShow", null)
        }
    } catch (e: Exception) {
        // Handle exception (e.g., permission not granted)
        throw SecurityException("Failed to add widget view. SYSTEM_ALERT_WINDOW permission may not be granted.", e)
    }
}
```

### 5. Update hideWidget method (replace existing)

```kotlin
fun hideWidget() {
    val wasVisible = widgetContainer != null
    val config = this.config

    try {
        // Cancel any running animations
        snapAnimator?.cancel()
        snapAnimator = null

        // Cancel long press
        longPressHandler?.removeCallbacks(longPressRunnable ?: Runnable {})

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

    // Emit hide event
    if (wasVisible && config?.hasHideCallback == true && context is ReactApplicationContext) {
        FloatingAppWidgetModule.sendEvent(context, "onWidgetHide", null)
    }
}
```

### 6. Update createBackgroundDrawable method (replace existing)

```kotlin
private fun createBackgroundDrawable(config: WidgetConfig): GradientDrawable {
    return GradientDrawable().apply {
        // Set shape
        shape = when (config.shape) {
            WidgetConfig.WidgetShape.CIRCLE -> GradientDrawable.OVAL
            WidgetConfig.WidgetShape.ROUNDED -> GradientDrawable.RECTANGLE
        }

        // Set corner radius for rounded shape (use custom value)
        if (config.shape == WidgetConfig.WidgetShape.ROUNDED) {
            cornerRadius = dpToPx(config.appearance.cornerRadius).toFloat()
        }

        // Use custom colors from appearance config
        setColor(config.appearance.backgroundColor)
        setStroke(dpToPx(config.appearance.borderWidth), config.appearance.borderColor)
    }
}
```

### 7. Add createBadgeView method (new method, add after createBackgroundDrawable)

```kotlin
private fun createBadgeView(badge: BadgeConfig): TextView {
    val badgeSize = dpToPx(badge.size)

    return TextView(context).apply {
        text = badge.text ?: badge.count?.toString() ?: ""
        setTextColor(badge.textColor)
        textSize = badge.textSize.toFloat()
        gravity = Gravity.CENTER

        // Create circular background
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(badge.backgroundColor)
        }

        // Position badge
        val params = FrameLayout.LayoutParams(badgeSize, badgeSize)

        params.gravity = when (badge.position) {
            BadgeConfig.Position.TOP_LEFT -> Gravity.TOP or Gravity.START
            BadgeConfig.Position.TOP_RIGHT -> Gravity.TOP or Gravity.END
            BadgeConfig.Position.BOTTOM_LEFT -> Gravity.BOTTOM or Gravity.START
            BadgeConfig.Position.BOTTOM_RIGHT -> Gravity.BOTTOM or Gravity.END
        }

        layoutParams = params
    }
}
```

### 8. Add helper methods for animations and interactions (add these new methods)

```kotlin
/**
 * Animate widget scale on press
 */
private fun animatePress(pressed: Boolean) {
    val config = this.config?.animations ?: return
    if (!config.enableScaleOnPress) return

    val scale = if (pressed) config.pressScale else 1.0f

    widgetContainer?.animate()
        ?.scaleX(scale)
        ?.scaleY(scale)
        ?.setDuration(100)
        ?.start()
}

/**
 * Perform haptic feedback
 */
private fun performHapticFeedback() {
    val config = this.config?.animations ?: return
    if (!config.enableHapticFeedback) return

    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        @Suppress("DEPRECATION")
        vibrator?.vibrate(50)
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
    constraints.minX?.let { newX = max(newX, it) }
    constraints.maxX?.let { newX = min(newX, it) }
    constraints.minY?.let { newY = max(newY, it) }
    constraints.maxY?.let { newY = min(newY, it) }

    // Keep on screen
    if (constraints.keepOnScreen) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val widgetSize = dpToPx(config.size)

        newX = max(0, min(newX, screenWidth - widgetSize))
        newY = max(0, min(newY, screenHeight - widgetSize))
    }

    // Snap to grid
    if (constraints.snapToGrid > 0) {
        val gridSize = dpToPx(constraints.snapToGrid)
        newX = (newX / gridSize) * gridSize
        newY = (newY / gridSize) * gridSize
    }

    return Pair(newX, newY)
}

/**
 * Handle long press
 */
private fun handleLongPress() {
    val config = this.config ?: return

    if (config.hasLongPressCallback && context is ReactApplicationContext) {
        val duration = System.currentTimeMillis() - pressStartTime

        val params = Arguments.createMap().apply {
            putDouble("timestamp", System.currentTimeMillis().toDouble())
            putDouble("duration", duration.toDouble())
        }

        FloatingAppWidgetModule.sendEvent(context, "onWidgetLongPress", params)
    }

    // Also perform haptic feedback for long press
    performHapticFeedback()
}
```

### 9. Update snapToEdge method (replace existing)

```kotlin
private fun snapToEdge() {
    val config = this.config ?: return
    val animConfig = config.animations
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val currentX = layoutParams?.x ?: 0

    val distanceToLeft = currentX
    val distanceToRight = screenWidth - currentX - dpToPx(config.size)
    val targetX = if (distanceToLeft < distanceToRight) 0 else screenWidth - dpToPx(config.size)

    // Get interpolator based on config
    val interpolator = when (animConfig.snapInterpolator) {
        AnimationConfig.InterpolatorType.ACCELERATE -> AccelerateInterpolator()
        AnimationConfig.InterpolatorType.LINEAR -> LinearInterpolator()
        AnimationConfig.InterpolatorType.BOUNCE -> BounceInterpolator()
        AnimationConfig.InterpolatorType.OVERSHOOT -> OvershootInterpolator()
        else -> DecelerateInterpolator()
    }

    snapAnimator = ValueAnimator.ofInt(currentX, targetX).apply {
        duration = animConfig.snapDuration
        this.interpolator = interpolator
        addUpdateListener { animation ->
            val animatedX = animation.animatedValue as Int
            layoutParams?.apply {
                x = animatedX
            }
            try {
                windowManager?.updateViewLayout(widgetContainer, layoutParams)
            } catch (e: Exception) {
                animation.cancel()
            }
        }
        start()
    }
}
```

### 10. Update showDismissZone method (replace existing)

```kotlin
private fun showDismissZone() {
    if (dismissZoneView != null) return

    val config = this.config ?: return
    val dismissConfig = config.dismissZone
    val displayMetrics = context.resources.displayMetrics
    val screenWidth = displayMetrics.widthPixels
    val dismissZoneHeight = dpToPx(dismissConfig.height)

    dismissZoneView = TextView(context).apply {
        text = dismissConfig.text
        setTextColor(dismissConfig.textColor)
        gravity = Gravity.CENTER
        setBackgroundColor(dismissConfig.backgroundColor)
        textSize = dismissConfig.textSize.toFloat()
        alpha = 0.5f
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
        gravity = when (dismissConfig.position) {
            DismissZoneConfig.Position.TOP -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            DismissZoneConfig.Position.BOTTOM -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        }
    }

    try {
        windowManager?.addView(dismissZoneView, dismissZoneParams)
    } catch (e: Exception) {
        dismissZoneView = null
    }
}
```

### 11. Update updateDismissZoneAppearance method (replace existing)

```kotlin
private fun updateDismissZoneAppearance(isInZone: Boolean) {
    val config = this.config?.dismissZone ?: return

    dismissZoneView?.apply {
        alpha = if (isInZone) 1.0f else 0.5f
        setBackgroundColor(if (isInZone) config.activeBackgroundColor else config.backgroundColor)
    }
}
```

### 12. Update isInDismissZone method (replace existing)

```kotlin
private fun isInDismissZone(y: Int): Boolean {
    val config = this.config ?: return false
    val dismissConfig = config.dismissZone

    // Check both new and deprecated config
    val enabled = dismissConfig.enabled || config.enableDragToDismiss
    if (!enabled) return false

    val displayMetrics = context.resources.displayMetrics
    val screenHeight = displayMetrics.heightPixels
    val dismissZoneHeight = dpToPx(dismissConfig.height)

    return when (dismissConfig.position) {
        DismissZoneConfig.Position.TOP -> y <= dismissZoneHeight
        DismissZoneConfig.Position.BOTTOM -> y >= screenHeight - dismissZoneHeight
    }
}
```

### 13. COMPLETELY REPLACE handleTouch method

**This is the most complex change. Replace the entire handleTouch method with:**

```kotlin
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

            // Initialize velocity tracking
            lastX = event.rawX
            lastY = event.rawY
            lastUpdateTime = System.currentTimeMillis()
            velocityX = 0f
            velocityY = 0f

            // Animate press
            animatePress(true)
            performHapticFeedback()

            // Start long press detection
            if (config.hasLongPressCallback) {
                isLongPress = false
                pressStartTime = System.currentTimeMillis()

                longPressRunnable = Runnable {
                    isLongPress = true
                    handleLongPress()
                }

                longPressHandler?.postDelayed(longPressRunnable!!, config.longPressDuration)
            }

            // Show dismiss zone if enabled
            val dismissEnabled = config.dismissZone.enabled || config.enableDragToDismiss
            if (dismissEnabled) {
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

                // Cancel long press if moved
                longPressHandler?.removeCallbacks(longPressRunnable ?: Runnable {})

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
                val newX = initialX + deltaX.toInt()
                val newY = initialY + deltaY.toInt()

                val (constrainedX, constrainedY) = applyConstraints(newX, newY)

                layoutParams?.apply {
                    x = constrainedX
                    y = constrainedY
                }

                // Update view
                windowManager?.updateViewLayout(widgetContainer, layoutParams)

                // Update dismiss zone if drag-to-dismiss is enabled
                val dismissEnabled = config.dismissZone.enabled || config.enableDragToDismiss
                if (dismissEnabled) {
                    val isInDismissZone = isInDismissZone(constrainedY)
                    updateDismissZoneAppearance(isInDismissZone)

                    // Send drag event if callback is registered
                    if (config.hasDragCallback && context is ReactApplicationContext) {
                        sendDragEvent(context, constrainedX, constrainedY, isInDismissZone)
                    }
                }
            }
            return true
        }

        MotionEvent.ACTION_UP -> {
            // Reset press animation
            animatePress(false)

            // Cancel long press
            longPressHandler?.removeCallbacks(longPressRunnable ?: Runnable {})

            // Hide dismiss zone
            val dismissEnabled = config.dismissZone.enabled || config.enableDragToDismiss
            if (dismissEnabled) {
                hideDismissZone()

                // Check if widget should be dismissed
                if (hasMoved && isInDismissZone(layoutParams?.y ?: 0)) {
                    // Emit dismiss event before hiding
                    if (config.hasDismissCallback && context is ReactApplicationContext) {
                        FloatingAppWidgetModule.sendEvent(context, "onWidgetDismiss", null)
                    }

                    hideWidget()
                    return true
                }
            }

            if (!hasMoved && !isLongPress) {
                // This was a click, not a drag or long press
                handleWidgetClick()
            } else if (hasMoved) {
                // Drag ended
                // Handle snap to edge if enabled (check both new and deprecated)
                val snapEnabled = config.snapToEdge || config.animations.snapInterpolator != AnimationConfig.InterpolatorType.DECELERATE
                if (snapEnabled || config.snapToEdge) {
                    snapToEdge()
                }

                // Emit position change event
                if (config.hasPositionChangeCallback && context is ReactApplicationContext) {
                    val params = Arguments.createMap().apply {
                        putInt("x", layoutParams?.x ?: 0)
                        putInt("y", layoutParams?.y ?: 0)
                    }
                    FloatingAppWidgetModule.sendEvent(context, "onWidgetPositionChange", params)
                }
            }

            isDragging = false
            hasMoved = false
            isLongPress = false
            return true
        }

        MotionEvent.ACTION_CANCEL -> {
            // Reset press animation
            animatePress(false)

            // Cancel long press
            longPressHandler?.removeCallbacks(longPressRunnable ?: Runnable {})

            // Hide dismiss zone
            val dismissEnabled = config.dismissZone.enabled || config.enableDragToDismiss
            if (dismissEnabled) {
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
```

---

## That's it for WidgetViewManager.kt!

All the major methods are updated. The file should now compile and work with all new features.

## Next: Update AppStateReceiver.kt

See separate instructions for AppStateReceiver updates (app state monitoring intervals).
