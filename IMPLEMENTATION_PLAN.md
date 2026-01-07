# Complete Implementation Plan for Advanced Features

This document provides a detailed implementation plan for all the advanced customization features. The **JavaScript/TypeScript layer is complete**. This plan focuses on the **Android native implementation**.

## ✅ Already Completed

1. **TypeScript types** - All interfaces defined in [src/types.ts](src/types.ts)
2. **JavaScript event handling** - All event listeners registered in [src/index.ts](src/index.ts)
3. **Basic features** - Click callbacks, drag events, drag-to-dismiss, snap-to-edge

## 📋 Implementation Checklist

### 1. Widget Appearance Customization ⭐⭐⭐

**Files to Modify:**
- `android/src/main/java/com/floatingappwidget/WidgetConfig.kt`
- `android/src/main/java/com/floatingappwidget/WidgetViewManager.kt`

**Implementation Steps:**

#### A. Add to WidgetConfig.kt

```kotlin
data class WidgetAppearanceConfig(
    val backgroundColor: Int = 0xCCFFFFFF.toInt(),
    val borderColor: Int = 0xFFCCCCCC.toInt(),
    val borderWidth: Int = 2, // dp
    val padding: Int = 8, // dp
    val opacity: Float = 1.0f,
    val cornerRadius: Int = 12 // dp
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): WidgetAppearanceConfig {
            if (map == null) return WidgetAppearanceConfig()

            return WidgetAppearanceConfig(
                backgroundColor = parseColor(map.getString("backgroundColor"), 0xCCFFFFFF.toInt()),
                borderColor = parseColor(map.getString("borderColor"), 0xFFCCCCCC.toInt()),
                borderWidth = if (map.hasKey("borderWidth")) map.getInt("borderWidth") else 2,
                padding = if (map.hasKey("padding")) map.getInt("padding") else 8,
                opacity = if (map.hasKey("opacity")) map.getDouble("opacity").toFloat() else 1.0f,
                cornerRadius = if (map.hasKey("cornerRadius")) map.getInt("cornerRadius") else 12
            )
        }

        private fun parseColor(colorString: String?, default: Int): Int {
            if (colorString == null) return default
            return try {
                Color.parseColor(colorString)
            } catch (e: Exception) {
                default
            }
        }
    }
}

// Add to WidgetConfig
data class WidgetConfig(
    // ... existing fields ...
    val appearance: WidgetAppearanceConfig = WidgetAppearanceConfig()
)
```

#### B. Update WidgetViewManager.kt

```kotlin
// In createBackgroundDrawable method
private fun createBackgroundDrawable(config: WidgetConfig): GradientDrawable {
    return GradientDrawable().apply {
        shape = when (config.shape) {
            WidgetConfig.WidgetShape.CIRCLE -> GradientDrawable.OVAL
            WidgetConfig.WidgetShape.ROUNDED -> GradientDrawable.RECTANGLE
        }

        if (config.shape == WidgetConfig.WidgetShape.ROUNDED) {
            cornerRadius = dpToPx(config.appearance.cornerRadius).toFloat()
        }

        // Use custom colors from appearance config
        setColor(config.appearance.backgroundColor)
        setStroke(dpToPx(config.appearance.borderWidth), config.appearance.borderColor)
    }
}

// In showWidget method, update padding
widgetView = ImageView(context).apply {
    // ...existing code...

    // Use custom padding
    val paddingPx = dpToPx(config.appearance.padding)
    setPadding(paddingPx, paddingPx, paddingPx, paddingPx)

    // Set opacity
    alpha = config.appearance.opacity

    // ...rest of code...
}
```

---

### 2. Dismiss Zone Customization ⭐⭐⭐

**Files to Modify:**
- `android/src/main/java/com/floatingappwidget/WidgetConfig.kt`
- `android/src/main/java/com/floatingappwidget/WidgetViewManager.kt`

**Implementation Steps:**

#### A. Add to WidgetConfig.kt

```kotlin
data class DismissZoneConfig(
    val enabled: Boolean = false,
    val height: Int = 100, // dp
    val backgroundColor: Int = Color.argb(150, 255, 0, 0),
    val activeBackgroundColor: Int = Color.argb(200, 255, 0, 0),
    val text: String = "⊗ Release to remove",
    val textColor: Int = Color.WHITE,
    val textSize: Int = 16, // sp
    val position: Position = Position.BOTTOM
) {
    enum class Position {
        TOP, BOTTOM
    }

    companion object {
        fun fromReadableMap(map: ReadableMap?): DismissZoneConfig {
            if (map == null) return DismissZoneConfig()

            return DismissZoneConfig(
                enabled = if (map.hasKey("enabled")) map.getBoolean("enabled") else false,
                height = if (map.hasKey("height")) map.getInt("height") else 100,
                backgroundColor = parseColor(map.getString("backgroundColor"), Color.argb(150, 255, 0, 0)),
                activeBackgroundColor = parseColor(map.getString("activeBackgroundColor"), Color.argb(200, 255, 0, 0)),
                text = map.getString("text") ?: "⊗ Release to remove",
                textColor = parseColor(map.getString("textColor"), Color.WHITE),
                textSize = if (map.hasKey("textSize")) map.getInt("textSize") else 16,
                position = when (map.getString("position")) {
                    "top" -> Position.TOP
                    else -> Position.BOTTOM
                }
            )
        }

        private fun parseColor(colorString: String?, default: Int): Int {
            if (colorString == null) return default
            return try {
                Color.parseColor(colorString)
            } catch (e: Exception) {
                default
            }
        }
    }
}

// Add to WidgetConfig
data class WidgetConfig(
    // ... existing fields ...
    val dismissZone: DismissZoneConfig = DismissZoneConfig()
)
```

#### B. Update WidgetViewManager.kt

```kotlin
// Update showDismissZone method
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

// Update updateDismissZoneAppearance method
private fun updateDismissZoneAppearance(isInZone: Boolean) {
    val config = this.config?.dismissZone ?: return

    dismissZoneView?.apply {
        alpha = if (isInZone) 1.0f else 0.5f
        setBackgroundColor(if (isInZone) config.activeBackgroundColor else config.backgroundColor)
    }
}

// Update isInDismissZone method
private fun isInDismissZone(y: Int): Boolean {
    val config = this.config ?: return false
    val dismissConfig = config.dismissZone

    if (!dismissConfig.enabled) return false

    val displayMetrics = context.resources.displayMetrics
    val screenHeight = displayMetrics.heightPixels
    val dismissZoneHeight = dpToPx(dismissConfig.height)

    return when (dismissConfig.position) {
        DismissZoneConfig.Position.TOP -> y <= dismissZoneHeight
        DismissZoneConfig.Position.BOTTOM -> y >= screenHeight - dismissZoneHeight
    }
}
```

---

### 3. Animation Customization ⭐⭐

**Files to Modify:**
- `android/src/main/java/com/floatingappwidget/WidgetConfig.kt`
- `android/src/main/java/com/floatingappwidget/WidgetViewManager.kt`

**Implementation Steps:**

#### A. Add to WidgetConfig.kt

```kotlin
data class AnimationConfig(
    val snapDuration: Long = 300, // ms
    val snapInterpolator: InterpolatorType = InterpolatorType.DECELERATE,
    val enableScaleOnPress: Boolean = false,
    val pressScale: Float = 0.9f,
    val enableHapticFeedback: Boolean = false
) {
    enum class InterpolatorType {
        DECELERATE, ACCELERATE, LINEAR, BOUNCE, OVERSHOOT
    }

    companion object {
        fun fromReadableMap(map: ReadableMap?): AnimationConfig {
            if (map == null) return AnimationConfig()

            return AnimationConfig(
                snapDuration = if (map.hasKey("snapDuration")) map.getInt("snapDuration").toLong() else 300,
                snapInterpolator = when (map.getString("snapInterpolator")) {
                    "accelerate" -> InterpolatorType.ACCELERATE
                    "linear" -> InterpolatorType.LINEAR
                    "bounce" -> InterpolatorType.BOUNCE
                    "overshoot" -> InterpolatorType.OVERSHOOT
                    else -> InterpolatorType.DECELERATE
                },
                enableScaleOnPress = if (map.hasKey("enableScaleOnPress")) map.getBoolean("enableScaleOnPress") else false,
                pressScale = if (map.hasKey("pressScale")) map.getDouble("pressScale").toFloat() else 0.9f,
                enableHapticFeedback = if (map.hasKey("enableHapticFeedback")) map.getBoolean("enableHapticFeedback") else false
            )
        }
    }
}

// Add to WidgetConfig
data class WidgetConfig(
    // ... existing fields ...
    val animations: AnimationConfig = AnimationConfig()
)
```

#### B. Update WidgetViewManager.kt

```kotlin
// Add import
import android.view.animation.*
import android.os.VibrationEffect
import android.os.Vibrator

// Update snapToEdge method
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
                windowManager?.updateViewLayout(widgetView, layoutParams)
            } catch (e: Exception) {
                animation.cancel()
            }
        }
        start()
    }
}

// Add scale animation on press
private fun animatePress(pressed: Boolean) {
    val config = this.config?.animations ?: return
    if (!config.enableScaleOnPress) return

    val scale = if (pressed) config.pressScale else 1.0f

    widgetView?.animate()
        ?.scaleX(scale)
        ?.scaleY(scale)
        ?.setDuration(100)
        ?.start()
}

// Add haptic feedback
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

// Update handleTouch to include press animation and haptics
private fun handleTouch(view: View, event: MotionEvent): Boolean {
    // ... existing code ...

    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            animatePress(true)
            performHapticFeedback()
            // ... rest of existing code ...
        }

        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
            animatePress(false)
            // ... rest of existing code ...
        }
    }

    // ... rest of method ...
}
```

---

### 4. Position Constraints ⭐⭐

**Files to Modify:**
- `android/src/main/java/com/floatingappwidget/WidgetConfig.kt`
- `android/src/main/java/com/floatingappwidget/WidgetViewManager.kt`

**Implementation Steps:**

#### A. Add to WidgetConfig.kt

```kotlin
data class PositionConstraints(
    val minX: Int? = null,
    val maxX: Int? = null,
    val minY: Int? = null,
    val maxY: Int? = null,
    val keepOnScreen: Boolean = false,
    val snapToGrid: Int = 0 // dp
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): PositionConstraints {
            if (map == null) return PositionConstraints()

            return PositionConstraints(
                minX = if (map.hasKey("minX")) map.getInt("minX") else null,
                maxX = if (map.hasKey("maxX")) map.getInt("maxX") else null,
                minY = if (map.hasKey("minY")) map.getInt("minY") else null,
                maxY = if (map.hasKey("maxY")) map.getInt("maxY") else null,
                keepOnScreen = if (map.hasKey("keepOnScreen")) map.getBoolean("keepOnScreen") else false,
                snapToGrid = if (map.hasKey("snapToGrid")) map.getInt("snapToGrid") else 0
            )
        }
    }
}

// Add to WidgetConfig
data class WidgetConfig(
    // ... existing fields ...
    val constraints: PositionConstraints = PositionConstraints()
)
```

#### B. Update WidgetViewManager.kt

```kotlin
// Add method to apply constraints
private fun applyConstraints(x: Int, y: Int): Pair<Int, Int> {
    val config = this.config ?: return Pair(x, y)
    val constraints = config.constraints
    var newX = x
    var newY = y

    // Apply min/max constraints
    constraints.minX?.let { newX = maxOf(newX, it) }
    constraints.maxX?.let { newX = minOf(newX, it) }
    constraints.minY?.let { newY = maxOf(newY, it) }
    constraints.maxY?.let { newY = minOf(newY, it) }

    // Keep on screen
    if (constraints.keepOnScreen) {
        val displayMetrics = context.resources.displayMetrics
        val screenWidth = displayMetrics.widthPixels
        val screenHeight = displayMetrics.heightPixels
        val widgetSize = dpToPx(config.size)

        newX = maxOf(0, minOf(newX, screenWidth - widgetSize))
        newY = maxOf(0, minOf(newY, screenHeight - widgetSize))
    }

    // Snap to grid
    if (constraints.snapToGrid > 0) {
        val gridSize = dpToPx(constraints.snapToGrid)
        newX = (newX / gridSize) * gridSize
        newY = (newY / gridSize) * gridSize
    }

    return Pair(newX, newY)
}

// Update handleTouch in ACTION_MOVE
MotionEvent.ACTION_MOVE -> {
    // ... existing code ...

    // Update position with constraints
    val newX = initialX + deltaX.toInt()
    val newY = initialY + deltaY.toInt()

    val (constrainedX, constrainedY) = applyConstraints(newX, newY)

    layoutParams?.apply {
        x = constrainedX
        y = constrainedY
    }

    // ... rest of code ...
}
```

---

### 5. Long Press Gesture ⭐

**Files to Modify:**
- `android/src/main/java/com/floatingappwidget/WidgetConfig.kt`
- `android/src/main/java/com/floatingappwidget/WidgetViewManager.kt`

**Implementation Steps:**

#### A. Add to WidgetConfig.kt

```kotlin
// Add to WidgetConfig
data class WidgetConfig(
    // ... existing fields ...
    val longPressDuration: Long = 500, // ms
    val hasLongPressCallback: Boolean = false
)
```

#### B. Update WidgetViewManager.kt

```kotlin
// Add fields
private var longPressHandler: Handler? = null
private var longPressRunnable: Runnable? = null
private var isLongPress: Boolean = false
private var pressStartTime: Long = 0

// Initialize handler in init or showWidget
longPressHandler = Handler(Looper.getMainLooper())

// Update handleTouch
private fun handleTouch(view: View, event: MotionEvent): Boolean {
    val config = this.config ?: return false

    when (event.action) {
        MotionEvent.ACTION_DOWN -> {
            // ... existing code ...

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

            return true
        }

        MotionEvent.ACTION_MOVE -> {
            // Cancel long press if moved
            if (abs(deltaX) > CLICK_DRAG_TOLERANCE || abs(deltaY) > CLICK_DRAG_TOLERANCE) {
                longPressHandler?.removeCallbacks(longPressRunnable ?: return@ACTION_MOVE)
            }

            // ... rest of existing code ...
        }

        MotionEvent.ACTION_UP -> {
            // Cancel long press
            longPressHandler?.removeCallbacks(longPressRunnable ?: return true)

            if (!hasMoved && !isLongPress) {
                // Regular click
                handleWidgetClick()
            }

            isLongPress = false

            // ... rest of existing code ...
        }

        MotionEvent.ACTION_CANCEL -> {
            longPressHandler?.removeCallbacks(longPressRunnable ?: return true)
            isLongPress = false

            // ... rest of existing code ...
        }
    }

    return false
}

// Add handleLongPress method
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
}
```

---

### 6. Badge Support ⭐⭐

**Files to Modify:**
- `android/src/main/java/com/floatingappwidget/WidgetConfig.kt`
- `android/src/main/java/com/floatingappwidget/WidgetViewManager.kt`

**Implementation Steps:**

#### A. Add to WidgetConfig.kt

```kotlin
data class BadgeConfig(
    val text: String? = null,
    val count: Int? = null,
    val position: Position = Position.TOP_RIGHT,
    val backgroundColor: Int = Color.parseColor("#F44336"),
    val textColor: Int = Color.WHITE,
    val size: Int = 20, // dp
    val textSize: Int = 10 // sp
) {
    enum class Position {
        TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT
    }

    companion object {
        fun fromReadableMap(map: ReadableMap?): BadgeConfig? {
            if (map == null) return null

            val text = map.getString("text")
            val count = if (map.hasKey("count")) map.getInt("count") else null

            if (text == null && count == null) return null

            return BadgeConfig(
                text = text ?: count?.toString(),
                count = count,
                position = when (map.getString("position")) {
                    "top-left" -> Position.TOP_LEFT
                    "bottom-right" -> Position.BOTTOM_RIGHT
                    "bottom-left" -> Position.BOTTOM_LEFT
                    else -> Position.TOP_RIGHT
                },
                backgroundColor = parseColor(map.getString("backgroundColor"), Color.parseColor("#F44336")),
                textColor = parseColor(map.getString("textColor"), Color.WHITE),
                size = if (map.hasKey("size")) map.getInt("size") else 20,
                textSize = if (map.hasKey("textSize")) map.getInt("textSize") else 10
            )
        }

        private fun parseColor(colorString: String?, default: Int): Int {
            if (colorString == null) return default
            return try {
                Color.parseColor(colorString)
            } catch (e: Exception) {
                default
            }
        }
    }
}

// Add to WidgetConfig
data class WidgetConfig(
    // ... existing fields ...
    val badge: BadgeConfig? = null
)
```

#### B. Update WidgetViewManager.kt

```kotlin
// Change widgetView from ImageView to FrameLayout with ImageView + Badge
private var widgetContainer: FrameLayout? = null
private var widgetImageView: ImageView? = null
private var badgeView: TextView? = null

// Update showWidget method
fun showWidget(widgetConfig: WidgetConfig) {
    // ... existing code ...

    // Create container
    widgetContainer = FrameLayout(context)

    // Create image view
    widgetImageView = ImageView(context).apply {
        // ... existing image view code ...

        layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
    }

    widgetContainer?.addView(widgetImageView)

    // Add badge if configured
    if (widgetConfig.badge != null) {
        badgeView = createBadgeView(widgetConfig.badge)
        widgetContainer?.addView(badgeView)
    }

    // Set touch listener on container
    widgetContainer?.setOnTouchListener { v, event ->
        handleTouch(v, event)
    }

    // ... rest of layout params code ...

    // Add container to window instead of imageView
    windowManager?.addView(widgetContainer, layoutParams)
}

// Add method to create badge view
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

// Add method to update badge
fun updateBadge(badge: BadgeConfig?) {
    badgeView?.let { view ->
        widgetContainer?.removeView(view)
    }

    if (badge != null) {
        badgeView = createBadgeView(badge)
        widgetContainer?.addView(badgeView)
    } else {
        badgeView = null
    }
}
```

---

### 7. Lifecycle Callbacks ⭐⭐⭐

**Files to Modify:**
- `android/src/main/java/com/floatingappwidget/WidgetViewManager.kt`
- `android/src/main/java/com/floatingappwidget/FloatingWidgetService.kt`

**Implementation Steps:**

Update WidgetViewManager.kt to emit events:

```kotlin
// In showWidget method - emit onWidgetShow event
fun showWidget(widgetConfig: WidgetConfig) {
    // ... existing code ...

    // Emit show event
    if (widgetConfig.hasShowCallback && context is ReactApplicationContext) {
        FloatingAppWidgetModule.sendEvent(context as ReactApplicationContext, "onWidgetShow", null)
    }
}

// In hideWidget method - emit onWidgetHide event
fun hideWidget() {
    val wasVisible = widgetView != null

    // ... existing cleanup code ...

    // Emit hide event
    if (wasVisible && config?.hasHideCallback == true && context is ReactApplicationContext) {
        FloatingAppWidgetModule.sendEvent(context as ReactApplicationContext, "onWidgetHide", null)
    }
}

// In handleTouch ACTION_UP - emit onWidgetDismiss when dismissed
if (hasMoved && isInDismissZone(layoutParams?.y ?: 0)) {
    // Emit dismiss event before hiding
    if (config.hasDismissCallback && context is ReactApplicationContext) {
        FloatingAppWidgetModule.sendEvent(context, "onWidgetDismiss", null)
    }

    hideWidget()
    return true
}

// In handleTouch ACTION_UP - emit onWidgetPositionChange
if (hasMoved && !isInDismissZone(layoutParams?.y ?: 0)) {
    if (config.hasPositionChangeCallback && context is ReactApplicationContext) {
        val params = Arguments.createMap().apply {
            putInt("x", layoutParams?.x ?: 0)
            putInt("y", layoutParams?.y ?: 0)
        }
        FloatingAppWidgetModule.sendEvent(context, "onWidgetPositionChange", params)
    }
}
```

---

### 8. App State Monitoring Control ⭐⭐

**Files to Modify:**
- `android/src/main/java/com/floatingappwidget/WidgetConfig.kt`
- `android/src/main/java/com/floatingappwidget/AppStateReceiver.kt`
- `android/src/main/java/com/floatingappwidget/FloatingWidgetService.kt`

**Implementation Steps:**

#### A. Add to WidgetConfig.kt

```kotlin
data class AppStateMonitoringConfig(
    val enabled: Boolean = true,
    val checkInterval: Long = 1000, // ms
    val hasCallbacks: Boolean = false
) {
    companion object {
        fun fromReadableMap(map: ReadableMap?): AppStateMonitoringConfig {
            if (map == null) return AppStateMonitoringConfig()

            return AppStateMonitoringConfig(
                enabled = if (map.hasKey("enabled")) map.getBoolean("enabled") else true,
                checkInterval = if (map.hasKey("checkInterval")) map.getInt("checkInterval").toLong() else 1000,
                hasCallbacks = false // Set by JavaScript layer
            )
        }
    }
}

// Add to WidgetConfig
data class WidgetConfig(
    // ... existing fields ...
    val appStateMonitoring: AppStateMonitoringConfig = AppStateMonitoringConfig()
)
```

#### B. Update AppStateReceiver.kt

```kotlin
// Update updateAppState to use custom interval
fun updateAppState(context: Context, interval: Long, callback: (Boolean) -> Unit) {
    checkRunnable?.let { handler.removeCallbacks(it) }

    callback(isAppInForeground(context))

    checkRunnable = object : Runnable {
        override fun run() {
            callback(isAppInForeground(context))
            handler.postDelayed(this, interval)
        }
    }
    handler.post(checkRunnable!!)
}
```

#### C. Update FloatingWidgetService.kt

```kotlin
// Update handleAppStateChange to emit events
private fun handleAppStateChange(isAppInForeground: Boolean) {
    val config = currentConfig ?: return

    // Emit app state events if callbacks registered
    if (config.appStateMonitoring.hasCallbacks) {
        val eventName = if (isAppInForeground) "onAppForeground" else "onAppBackground"

        // Get ReactApplicationContext and emit event
        // (You'll need to pass this from FloatingAppWidgetModule)
        reactContext?.let { context ->
            FloatingAppWidgetModule.sendEvent(context, eventName, null)
        }
    }

    // ... rest of existing code for showing/hiding widget ...
}

// Update onStartCommand to use custom interval
if (config.appStateMonitoring.enabled) {
    AppStateReceiver.updateAppState(
        applicationContext,
        config.appStateMonitoring.checkInterval
    ) { isAppInForeground ->
        handleAppStateChange(isAppInForeground)
    }
}
```

---

## 📝 Additional Tasks

### Update Config Parsing

For each new config section, update `WidgetConfig.fromReadableMap()`:

```kotlin
fun fromReadableMap(map: ReadableMap): WidgetConfig {
    // ... existing parsing ...

    return WidgetConfig(
        // ... existing fields ...
        appearance = WidgetAppearanceConfig.fromReadableMap(map.getMap("appearance")),
        dismissZone = DismissZoneConfig.fromReadableMap(map.getMap("dismissZone")),
        animations = AnimationConfig.fromReadableMap(map.getMap("animations")),
        constraints = PositionConstraints.fromReadableMap(map.getMap("constraints")),
        badge = BadgeConfig.fromReadableMap(map.getMap("badge")),
        appStateMonitoring = AppStateMonitoringConfig.fromReadableMap(map.getMap("appStateMonitoring")),
        longPressDuration = if (map.hasKey("longPressDuration")) map.getInt("longPressDuration").toLong() else 500,
        hasLongPressCallback = if (map.hasKey("hasLongPressCallback")) map.getBoolean("hasLongPressCallback") else false,
        // ... etc
    )
}
```

### Update Persistence

Update all the SharedPreferences save/load methods in:
- `FloatingAppWidgetModule.saveConfigToPrefs()`
- `FloatingWidgetService.Intent.putExtra()` and `Intent.getParcelableExtra()`

Add serialization for all new config objects.

---

## 🧪 Testing Checklist

After implementation, test each feature:

- [ ] Widget appearance (colors, borders, padding, opacity)
- [ ] Dismiss zone (top/bottom, custom colors/text)
- [ ] Animation (interpolators, scale on press, haptic feedback)
- [ ] Position constraints (boundaries, keep on screen, snap to grid)
- [ ] Long press detection
- [ ] Badge display and positioning
- [ ] Lifecycle callbacks (show, hide, dismiss, position change)
- [ ] App state monitoring (custom interval, callbacks)
- [ ] Backward compatibility with old API

---

## 📚 Documentation

After implementation:

1. Update main [README.md](README.md) with all new features
2. Create detailed API documentation
3. Add examples to [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md)
4. Update [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) with new features
5. Add troubleshooting section for new features

---

## ⏱️ Estimated Implementation Time

- **Appearance customization**: 2-3 hours
- **Dismiss zone customization**: 2-3 hours
- **Animation customization**: 3-4 hours
- **Position constraints**: 3-4 hours
- **Long press gesture**: 2-3 hours
- **Badge support**: 4-5 hours
- **Lifecycle callbacks**: 2-3 hours
- **App state monitoring**: 2-3 hours
- **Testing and documentation**: 4-6 hours

**Total**: 24-34 hours of development time

---

## 🎯 Priority Order for Implementation

1. **Appearance customization** - High impact, low complexity
2. **Lifecycle callbacks** - Critical for integration
3. **Dismiss zone customization** - Improves UX, relatively easy
4. **Animation customization** - Enhances feel, moderate complexity
5. **Badge support** - Useful feature, moderate complexity
6. **Position constraints** - Nice to have, moderate complexity
7. **Long press gesture** - Additional interaction, easy
8. **App state monitoring controls** - Advanced use case, low priority

---

This plan provides everything needed to complete the implementation. The TypeScript/JavaScript layer is ready and all event handling is in place. Focus on the native Android code following this guide!
