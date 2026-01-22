package com.floatingappwidget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.util.Base64
import com.facebook.react.bridge.ReadableMap

/**
 * Widget appearance configuration
 */
data class WidgetAppearanceConfig(
    val backgroundColor: Int = 0xCCFFFFFF.toInt(),
    val borderColor: Int = 0xFFCCCCCC.toInt(),
    val borderWidth: Int = 2, // dp
    val padding: Int = 8, // dp
    val opacity: Float = 1.0f,
    val cornerRadius: Int = 12, // dp
    val elevation: Int = 0, // dp
    val shadowColor: Int = Color.BLACK
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
                cornerRadius = if (map.hasKey("cornerRadius")) map.getInt("cornerRadius") else 12,
                elevation = if (map.hasKey("elevation")) map.getInt("elevation") else 0,
                shadowColor = parseColor(map.getString("shadowColor"), Color.BLACK)
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

/**
 * Dismiss zone configuration
 */
data class DismissZoneConfig(
    val enabled: Boolean = false,
    val showOn: ShowTrigger = ShowTrigger.ALWAYS,
    val height: Int = 100, // dp
    val backgroundColor: Int = Color.argb(150, 255, 0, 0),
    val activeBackgroundColor: Int = Color.argb(200, 255, 0, 0),
    val gradientColors: IntArray? = null,
    val activeGradientColors: IntArray? = null,
    val gradientOrientation: GradientOrientation = GradientOrientation.VERTICAL,
    val cornerRadius: Int = 0, // dp
    val text: String = "⊗ Release to remove",
    val textColor: Int = Color.WHITE,
    val textSize: Int = 16, // sp
    val position: Position = Position.BOTTOM,
    // New circular button properties
    val style: Style = Style.CIRCULAR,
    val buttonSize: Int = 60, // dp
    val activationRadius: Int = 100, // dp
    val icon: String = "✕",
    val iconSize: Int = 24, // sp
    val elevation: Int = 4, // dp
    val activeElevation: Int = 8, // dp
    val inactiveOpacity: Float = 0.6f,
    val activeOpacity: Float = 1.0f,
    val activeBorderColor: Int = Color.argb(77, 0, 0, 0), // 30% black
    val activeBorderWidth: Int = 2, // dp
    val margin: Int = 40, // dp
    // Activation radius background colors
    val radiusBackgroundColor: Int = Color.argb(26, 255, 255, 255), // 10% white for inactive
    val activeRadiusBackgroundColor: Int = Color.argb(51, 0, 0, 0), // 20% black for active
    // Dismiss behavior
    val dismissBehavior: DismissBehavior = DismissBehavior.HIDE
) {
    enum class Position {
        TOP, BOTTOM
    }

    enum class ShowTrigger {
        ALWAYS,      // Show on any drag
        LONG_PRESS   // Only show after long press
    }

    enum class DismissBehavior {
        HIDE,        // Hide widget, keep service running (widget reappears on next trigger)
        DESTROY      // Stop service completely (requires init() to restart)
    }

    enum class GradientOrientation {
        HORIZONTAL,      // Left to right
        VERTICAL,        // Top to bottom
        DIAGONAL_TL_BR,  // Top-left to bottom-right
        DIAGONAL_BL_TR   // Bottom-left to top-right
    }

    enum class Style {
        BAR,       // Full-width horizontal bar (legacy)
        CIRCULAR   // Circular button with icon (modern)
    }

    companion object {
        fun fromReadableMap(map: ReadableMap?): DismissZoneConfig {
            if (map == null) return DismissZoneConfig()

            // Parse gradient colors if provided
            val gradientColors = if (map.hasKey("gradientColors")) {
                val array = map.getArray("gradientColors")
                if (array != null && array.size() >= 2) {
                    IntArray(array.size()) { i ->
                        parseColor(array.getString(i), Color.TRANSPARENT)
                    }
                } else null
            } else null

            val activeGradientColors = if (map.hasKey("activeGradientColors")) {
                val array = map.getArray("activeGradientColors")
                if (array != null && array.size() >= 2) {
                    IntArray(array.size()) { i ->
                        parseColor(array.getString(i), Color.TRANSPARENT)
                    }
                } else null
            } else null

            return DismissZoneConfig(
                enabled = if (map.hasKey("enabled")) map.getBoolean("enabled") else false,
                showOn = when (map.getString("showOn")) {
                    "longPress" -> ShowTrigger.LONG_PRESS
                    else -> ShowTrigger.ALWAYS
                },
                height = if (map.hasKey("height")) map.getInt("height") else 100,
                backgroundColor = parseColor(map.getString("backgroundColor"), Color.argb(150, 255, 0, 0)),
                activeBackgroundColor = parseColor(map.getString("activeBackgroundColor"), Color.argb(200, 255, 0, 0)),
                gradientColors = gradientColors,
                activeGradientColors = activeGradientColors,
                gradientOrientation = when (map.getString("gradientOrientation")) {
                    "horizontal" -> GradientOrientation.HORIZONTAL
                    "diagonal-tl-br" -> GradientOrientation.DIAGONAL_TL_BR
                    "diagonal-bl-tr" -> GradientOrientation.DIAGONAL_BL_TR
                    else -> GradientOrientation.VERTICAL
                },
                cornerRadius = if (map.hasKey("cornerRadius")) map.getInt("cornerRadius") else 0,
                text = map.getString("text") ?: "⊗ Release to remove",
                textColor = parseColor(map.getString("textColor"), Color.WHITE),
                textSize = if (map.hasKey("textSize")) map.getInt("textSize") else 16,
                position = when (map.getString("position")) {
                    "top" -> Position.TOP
                    else -> Position.BOTTOM
                },
                // New circular button properties
                style = when (map.getString("style")) {
                    "bar" -> Style.BAR
                    else -> Style.CIRCULAR
                },
                buttonSize = if (map.hasKey("buttonSize")) map.getInt("buttonSize") else 60,
                activationRadius = if (map.hasKey("activationRadius")) map.getInt("activationRadius") else 100,
                icon = map.getString("icon") ?: "✕",
                iconSize = if (map.hasKey("iconSize")) map.getInt("iconSize") else 24,
                elevation = if (map.hasKey("elevation")) map.getInt("elevation") else 4,
                activeElevation = if (map.hasKey("activeElevation")) map.getInt("activeElevation") else 8,
                inactiveOpacity = if (map.hasKey("inactiveOpacity")) map.getDouble("inactiveOpacity").toFloat() else 0.6f,
                activeOpacity = if (map.hasKey("activeOpacity")) map.getDouble("activeOpacity").toFloat() else 1.0f,
                activeBorderColor = parseColor(map.getString("activeBorderColor"), Color.argb(77, 0, 0, 0)),
                activeBorderWidth = if (map.hasKey("activeBorderWidth")) map.getInt("activeBorderWidth") else 2,
                margin = if (map.hasKey("margin")) map.getInt("margin") else 40,
                radiusBackgroundColor = parseColor(map.getString("radiusBackgroundColor"), Color.argb(26, 255, 255, 255)),
                activeRadiusBackgroundColor = parseColor(map.getString("activeRadiusBackgroundColor"), Color.argb(51, 0, 0, 0)),
                dismissBehavior = when (map.getString("dismissBehavior")) {
                    "destroy" -> DismissBehavior.DESTROY
                    else -> DismissBehavior.HIDE
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

/**
 * Animation configuration
 */
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

/**
 * Position constraints configuration
 */
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

/**
 * Badge configuration
 */
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

/**
 * App state monitoring configuration
 */
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

/**
 * Widget configuration data class
 */
data class WidgetConfig(
    val icon: Bitmap? = null,
    val size: Int = 56, // dp
    val shape: WidgetShape = WidgetShape.CIRCLE,
    val draggable: Boolean = true,
    val initialX: Int? = null,
    val initialY: Int? = null,
    val notification: NotificationConfig,
    val autoStartOnBoot: Boolean = false,
    val hideOnAppOpen: Boolean = true,

    // New configuration objects
    val appearance: WidgetAppearanceConfig = WidgetAppearanceConfig(),
    val dismissZone: DismissZoneConfig = DismissZoneConfig(),
    val animations: AnimationConfig = AnimationConfig(),
    val constraints: PositionConstraints = PositionConstraints(),
    val badge: BadgeConfig? = null,
    val appStateMonitoring: AppStateMonitoringConfig = AppStateMonitoringConfig(),

    // Callback flags
    val hasClickCallback: Boolean = false,
    val hasLongPressCallback: Boolean = false,
    val hasDragCallback: Boolean = false,
    val hasShowCallback: Boolean = false,
    val hasHideCallback: Boolean = false,
    val hasDismissCallback: Boolean = false,
    val hasPositionChangeCallback: Boolean = false,
    val hasAppStateCallbacks: Boolean = false,

    // Other settings
    val longPressDuration: Long = 500, // ms

    // Backward compatibility (deprecated)
    val enableDragToDismiss: Boolean = false,
    val dismissZoneHeight: Int = 100, // dp
    val snapToEdge: Boolean = false
) {
    enum class WidgetShape {
        CIRCLE,
        ROUNDED
    }

    companion object {
        /**
         * Parse widget configuration from React Native ReadableMap
         */
        fun fromReadableMap(map: ReadableMap): WidgetConfig {
            // Parse notification config
            val notificationMap = map.getMap("notification")
                ?: throw IllegalArgumentException("notification config is required")

            val notification = NotificationConfig(
                title = notificationMap.getString("title")
                    ?: throw IllegalArgumentException("notification.title is required"),
                text = notificationMap.getString("text")
                    ?: throw IllegalArgumentException("notification.text is required"),
                channelId = notificationMap.getString("channelId") ?: "floating_widget_channel",
                channelName = notificationMap.getString("channelName") ?: "Floating Widget",
                icon = notificationMap.getString("icon")
            )

            // Parse icon (base64 string) with proper scaling to prevent OOM
            // Only load if there's sufficient memory available
            val icon: Bitmap? = if (map.hasKey("icon") && !map.isNull("icon")) {
                try {
                    // Check available memory before attempting to load
                    val runtime = Runtime.getRuntime()
                    val maxMemory = runtime.maxMemory()
                    val usedMemory = runtime.totalMemory() - runtime.freeMemory()
                    val availableMemory = maxMemory - usedMemory

                    // Only attempt to load icon if we have at least 2MB free (reduced from 10MB)
                    if (availableMemory < 2 * 1024 * 1024) {
                        android.util.Log.w("FloatingWidget", "Insufficient memory for icon: ${availableMemory / 1024}KB available")
                        null // Skip icon loading - insufficient memory
                    } else {
                        val iconString = map.getString("icon")!!

                        // Skip if icon data is too large (> 100KB base64)
                        if (iconString.length > 100000) {
                            android.util.Log.w("FloatingWidget", "Icon base64 too large: ${iconString.length} bytes")
                            null
                        } else {
                            val imageBytes = Base64.decode(iconString, Base64.DEFAULT)

                    // Decode with inJustDecodeBounds first to get dimensions
                    val options = BitmapFactory.Options()
                    options.inJustDecodeBounds = true
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                    // Get target size from config or use default (56dp is typical widget size)
                    val targetSize = if (map.hasKey("size")) map.getInt("size") else 56
                    // Use 2x density for small widgets to reduce memory
                    val targetPixels = (targetSize * 2)

                    // Calculate inSampleSize to reduce memory usage
                    options.inSampleSize = calculateInSampleSize(options, targetPixels, targetPixels)
                    options.inJustDecodeBounds = false
                    // Use ARGB_8888 for better quality (icons need alpha)
                    options.inPreferredConfig = Bitmap.Config.ARGB_8888
                    options.inDither = true
                    // Mutable bitmap for potential future modifications
                    options.inMutable = false

                            // Decode the scaled bitmap
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)

                            // Log successful decode
                            if (bitmap != null) {
                                android.util.Log.d("FloatingWidget", "Icon loaded: ${bitmap.width}x${bitmap.height}, ${bitmap.byteCount / 1024}KB")
                            }

                            bitmap
                        }
                    }
                } catch (e: OutOfMemoryError) {
                    // Explicitly catch OOM and return null instead of crashing
                    android.util.Log.e("FloatingWidget", "OutOfMemoryError loading icon", e)
                    null
                } catch (e: Exception) {
                    android.util.Log.e("FloatingWidget", "Error loading icon", e)
                    null
                }
            } else {
                null
            }

            // Parse shape
            val shape = if (map.hasKey("shape")) {
                when (map.getString("shape")) {
                    "rounded" -> WidgetShape.ROUNDED
                    else -> WidgetShape.CIRCLE
                }
            } else {
                WidgetShape.CIRCLE
            }

            // Parse initial position
            var initialX: Int? = null
            var initialY: Int? = null
            if (map.hasKey("initialPosition") && !map.isNull("initialPosition")) {
                val posMap = map.getMap("initialPosition")
                if (posMap != null) {
                    initialX = posMap.getInt("x")
                    initialY = posMap.getInt("y")
                }
            }

            // Parse new configuration objects
            val appearance = WidgetAppearanceConfig.fromReadableMap(map.getMap("appearance"))
            val dismissZoneConfig = DismissZoneConfig.fromReadableMap(map.getMap("dismissZone"))
            val animationConfig = AnimationConfig.fromReadableMap(map.getMap("animations"))
            val posConstraints = PositionConstraints.fromReadableMap(map.getMap("constraints"))
            val badgeConfig = BadgeConfig.fromReadableMap(map.getMap("badge"))
            var appStateMonitoring = AppStateMonitoringConfig.fromReadableMap(map.getMap("appStateMonitoring"))

            // Handle backward compatibility for deprecated properties
            val finalDismissZone = if (map.hasKey("enableDragToDismiss") || map.hasKey("dismissZoneHeight")) {
                dismissZoneConfig.copy(
                    enabled = if (map.hasKey("enableDragToDismiss")) map.getBoolean("enableDragToDismiss") else dismissZoneConfig.enabled,
                    height = if (map.hasKey("dismissZoneHeight")) map.getInt("dismissZoneHeight") else dismissZoneConfig.height
                )
            } else {
                dismissZoneConfig
            }

            // Set hasCallbacks flag if callbacks are registered
            if (map.hasKey("hasAppStateCallbacks") && map.getBoolean("hasAppStateCallbacks")) {
                appStateMonitoring = appStateMonitoring.copy(hasCallbacks = true)
            }

            return WidgetConfig(
                icon = icon,
                size = if (map.hasKey("size")) map.getInt("size") else 56,
                shape = shape,
                draggable = if (map.hasKey("draggable")) map.getBoolean("draggable") else true,
                initialX = initialX,
                initialY = initialY,
                notification = notification,
                autoStartOnBoot = if (map.hasKey("autoStartOnBoot"))
                    map.getBoolean("autoStartOnBoot") else false,
                hideOnAppOpen = if (map.hasKey("hideOnAppOpen"))
                    map.getBoolean("hideOnAppOpen") else true,

                // New configuration objects
                appearance = appearance,
                dismissZone = finalDismissZone,
                animations = animationConfig,
                constraints = posConstraints,
                badge = badgeConfig,
                appStateMonitoring = appStateMonitoring,

                // Callback flags
                hasClickCallback = if (map.hasKey("hasClickCallback"))
                    map.getBoolean("hasClickCallback") else false,
                hasLongPressCallback = if (map.hasKey("hasLongPressCallback"))
                    map.getBoolean("hasLongPressCallback") else false,
                hasDragCallback = if (map.hasKey("hasDragCallback"))
                    map.getBoolean("hasDragCallback") else false,
                hasShowCallback = if (map.hasKey("hasShowCallback"))
                    map.getBoolean("hasShowCallback") else false,
                hasHideCallback = if (map.hasKey("hasHideCallback"))
                    map.getBoolean("hasHideCallback") else false,
                hasDismissCallback = if (map.hasKey("hasDismissCallback"))
                    map.getBoolean("hasDismissCallback") else false,
                hasPositionChangeCallback = if (map.hasKey("hasPositionChangeCallback"))
                    map.getBoolean("hasPositionChangeCallback") else false,
                hasAppStateCallbacks = if (map.hasKey("hasAppStateCallbacks"))
                    map.getBoolean("hasAppStateCallbacks") else false,

                // Other settings
                longPressDuration = if (map.hasKey("longPressDuration"))
                    map.getInt("longPressDuration").toLong() else 500,

                // Backward compatibility (deprecated)
                enableDragToDismiss = if (map.hasKey("enableDragToDismiss"))
                    map.getBoolean("enableDragToDismiss") else false,
                dismissZoneHeight = if (map.hasKey("dismissZoneHeight"))
                    map.getInt("dismissZoneHeight") else 100,
                snapToEdge = if (map.hasKey("snapToEdge"))
                    map.getBoolean("snapToEdge") else false
            )
        }

        /**
         * Calculate the largest inSampleSize value that is a power of 2 and keeps both
         * height and width larger than the requested height and width.
         * This helps reduce memory usage when loading bitmaps.
         */
        private fun calculateInSampleSize(
            options: BitmapFactory.Options,
            reqWidth: Int,
            reqHeight: Int
        ): Int {
            val height = options.outHeight
            val width = options.outWidth
            var inSampleSize = 1

            if (height > reqHeight || width > reqWidth) {
                val halfHeight = height / 2
                val halfWidth = width / 2

                // Calculate the largest inSampleSize value that is a power of 2 and keeps both
                // height and width larger than the requested height and width.
                while ((halfHeight / inSampleSize) >= reqHeight &&
                    (halfWidth / inSampleSize) >= reqWidth
                ) {
                    inSampleSize *= 2
                }
            }

            return inSampleSize
        }
    }
}

/**
 * Notification configuration for foreground service
 */
data class NotificationConfig(
    val title: String,
    val text: String,
    val channelId: String,
    val channelName: String,
    val icon: String? = null
)
