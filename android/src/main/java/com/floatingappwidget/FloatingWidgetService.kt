package com.floatingappwidget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.facebook.react.bridge.Arguments
import com.facebook.react.bridge.ReactApplicationContext

/**
 * Foreground service that manages the floating widget lifecycle
 */
class FloatingWidgetService : Service() {

    private val binder = LocalBinder()
    private var widgetViewManager: WidgetViewManager? = null
    private var appStateReceiver: AppStateReceiver? = null
    private var currentConfig: WidgetConfig? = null
    private var isWidgetVisible = false
    private var lastAppState: Boolean? = null

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP_SERVICE = "com.floatingappwidget.STOP_SERVICE"

        /**
         * Start the service
         */
        fun start(context: Context, config: WidgetConfig) {
            val intent = Intent(context, FloatingWidgetService::class.java).apply {
                putExtra(context, "config", config)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /**
         * Stop the service
         */
        fun stop(context: Context) {
            val intent = Intent(context, FloatingWidgetService::class.java)
            context.stopService(intent)
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): FloatingWidgetService = this@FloatingWidgetService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onCreate() {
        super.onCreate()

        // Initialize widget view manager
        widgetViewManager = WidgetViewManager(applicationContext)

        // Register app state receiver
        appStateReceiver = AppStateReceiver { isAppInForeground ->
            handleAppStateChange(isAppInForeground)
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(appStateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(appStateReceiver, filter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle stop action
        if (intent?.action == ACTION_STOP_SERVICE) {
            stopSelf()
            return START_NOT_STICKY
        }

        // Get config from intent or use existing
        val config = intent?.getParcelableExtra(applicationContext, "config") ?: currentConfig

        if (config != null) {
            currentConfig = config

            // Create and show notification
            val notification = createNotification(config.notification)
            startForeground(NOTIFICATION_ID, notification)

            // Show widget initially (will be hidden if app is in foreground)
            // Use custom check interval if app state monitoring is enabled
            val checkInterval = if (config.appStateMonitoring.enabled) {
                config.appStateMonitoring.checkInterval.toLong()
            } else {
                1000L // Default 1 second
            }

            AppStateReceiver.updateAppState(applicationContext, { isAppInForeground ->
                handleAppStateChange(isAppInForeground)
            }, checkInterval)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()

        // Hide widget
        widgetViewManager?.hideWidget()

        // Unregister receiver
        try {
            appStateReceiver?.let { unregisterReceiver(it) }
        } catch (e: Exception) {
            // Receiver might not be registered
        }

        widgetViewManager = null
        appStateReceiver = null
    }

    /**
     * Handle app state changes (foreground/background)
     */
    private fun handleAppStateChange(isAppInForeground: Boolean) {
        val config = currentConfig ?: return

        // Emit app state events if callbacks are registered
        if (lastAppState != null && lastAppState != isAppInForeground && config.hasAppStateCallbacks) {
            try {
                if (applicationContext is ReactApplicationContext) {
                    val eventName = if (isAppInForeground) "onAppForeground" else "onAppBackground"
                    val params = Arguments.createMap()
                    FloatingAppWidgetModule.sendEvent(applicationContext as ReactApplicationContext, eventName, params)
                }
            } catch (e: Exception) {
                // Safe to ignore - React context may not be available during hot reload
            }
        }
        lastAppState = isAppInForeground

        if (config.hideOnAppOpen) {
            if (isAppInForeground) {
                // App is in foreground - hide widget
                if (isWidgetVisible) {
                    widgetViewManager?.hideWidget()
                    isWidgetVisible = false
                }
            } else {
                // App is in background - show widget
                if (!isWidgetVisible) {
                    try {
                        widgetViewManager?.showWidget(config)
                        isWidgetVisible = true
                    } catch (e: SecurityException) {
                        // Permission not granted, stop service
                        stopSelf()
                    }
                }
            }
        } else {
            // Always show widget
            if (!isWidgetVisible) {
                try {
                    widgetViewManager?.showWidget(config)
                    isWidgetVisible = true
                } catch (e: SecurityException) {
                    // Permission not granted, stop service
                    stopSelf()
                }
            }
        }
    }

    /**
     * Update widget configuration
     */
    fun updateConfig(newConfig: WidgetConfig) {
        currentConfig = newConfig

        // Update notification
        val notification = createNotification(newConfig.notification)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)

        // Update widget if visible
        if (isWidgetVisible) {
            widgetViewManager?.updateWidget(newConfig)
        }
    }

    /**
     * Create foreground service notification
     */
    private fun createNotification(config: NotificationConfig): Notification {
        // Create notification channel (required for Android O+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                config.channelId,
                config.channelName,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Floating widget service notification"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        // Create intent to open app
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)?.apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntentFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            pendingIntentFlags
        )

        // Create stop action
        val stopIntent = Intent(this, FloatingWidgetService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }

        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            pendingIntentFlags
        )

        // Get icon resource
        val iconRes = try {
            if (config.icon != null) {
                resources.getIdentifier(config.icon, "drawable", packageName)
            } else {
                applicationInfo.icon
            }
        } catch (e: Exception) {
            applicationInfo.icon
        }

        // Build notification
        return NotificationCompat.Builder(this, config.channelId)
            .setContentTitle(config.title)
            .setContentText(config.text)
            .setSmallIcon(if (iconRes != 0) iconRes else applicationInfo.icon)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}

// Extension to make WidgetConfig parcelable (for passing via Intent)
private fun Intent.putExtra(context: Context, name: String, config: WidgetConfig): Intent {
    // Store config in shared preferences instead of passing via intent
    // (since WidgetConfig contains Bitmap which can't be parceled easily)
    val prefs = context.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE)
    val editor = prefs.edit()

    // Basic config
    editor.putInt("size", config.size)
    editor.putString("shape", config.shape.name)
    editor.putBoolean("draggable", config.draggable)
    config.initialX?.let { editor.putInt("initialX", it) }
    config.initialY?.let { editor.putInt("initialY", it) }

    // Notification config
    editor.putString("notificationTitle", config.notification.title)
    editor.putString("notificationText", config.notification.text)
    editor.putString("notificationChannelId", config.notification.channelId)
    editor.putString("notificationChannelName", config.notification.channelName)
    config.notification.icon?.let { editor.putString("notificationIcon", it) }

    // App behavior
    editor.putBoolean("autoStartOnBoot", config.autoStartOnBoot)
    editor.putBoolean("hideOnAppOpen", config.hideOnAppOpen)

    // Callback flags
    editor.putBoolean("hasClickCallback", config.hasClickCallback)
    editor.putBoolean("hasLongPressCallback", config.hasLongPressCallback)
    editor.putBoolean("hasDragCallback", config.hasDragCallback)
    editor.putBoolean("hasShowCallback", config.hasShowCallback)
    editor.putBoolean("hasHideCallback", config.hasHideCallback)
    editor.putBoolean("hasDismissCallback", config.hasDismissCallback)
    editor.putBoolean("hasPositionChangeCallback", config.hasPositionChangeCallback)
    editor.putBoolean("hasAppStateCallbacks", config.hasAppStateCallbacks)
    editor.putLong("longPressDuration", config.longPressDuration)

    // Legacy properties (deprecated)
    editor.putBoolean("enableDragToDismiss", config.enableDragToDismiss)
    editor.putInt("dismissZoneHeight", config.dismissZoneHeight)
    editor.putBoolean("snapToEdge", config.snapToEdge)

    // Appearance config
    editor.putInt("appearanceBackgroundColor", config.appearance.backgroundColor)
    editor.putInt("appearanceBorderColor", config.appearance.borderColor)
    editor.putInt("appearanceBorderWidth", config.appearance.borderWidth)
    editor.putInt("appearancePadding", config.appearance.padding)
    editor.putFloat("appearanceOpacity", config.appearance.opacity)
    editor.putInt("appearanceCornerRadius", config.appearance.cornerRadius)

    // Dismiss zone config
    editor.putBoolean("dismissZoneEnabled", config.dismissZone.enabled)
    editor.putString("dismissZoneShowOn", config.dismissZone.showOn.name)
    editor.putInt("dismissZoneHeightNew", config.dismissZone.height)
    editor.putInt("dismissZoneBackgroundColor", config.dismissZone.backgroundColor)
    editor.putInt("dismissZoneActiveBackgroundColor", config.dismissZone.activeBackgroundColor)

    // Save gradient colors if present
    if (config.dismissZone.gradientColors != null) {
        editor.putString("dismissZoneGradientColors", config.dismissZone.gradientColors.joinToString(","))
    } else {
        editor.remove("dismissZoneGradientColors")
    }
    if (config.dismissZone.activeGradientColors != null) {
        editor.putString("dismissZoneActiveGradientColors", config.dismissZone.activeGradientColors.joinToString(","))
    } else {
        editor.remove("dismissZoneActiveGradientColors")
    }

    editor.putString("dismissZoneGradientOrientation", config.dismissZone.gradientOrientation.name)
    editor.putInt("dismissZoneCornerRadius", config.dismissZone.cornerRadius)
    editor.putString("dismissZoneText", config.dismissZone.text)
    editor.putInt("dismissZoneTextColor", config.dismissZone.textColor)
    editor.putInt("dismissZoneTextSize", config.dismissZone.textSize)
    editor.putString("dismissZonePosition", config.dismissZone.position.name)

    // Animation config
    editor.putLong("animationSnapDuration", config.animations.snapDuration)
    editor.putString("animationSnapInterpolator", config.animations.snapInterpolator.name)
    editor.putBoolean("animationEnableScaleOnPress", config.animations.enableScaleOnPress)
    editor.putFloat("animationPressScale", config.animations.pressScale)
    editor.putBoolean("animationEnableHapticFeedback", config.animations.enableHapticFeedback)

    // Position constraints config
    config.constraints.minX?.let { editor.putInt("constraintsMinX", it) }
    config.constraints.maxX?.let { editor.putInt("constraintsMaxX", it) }
    config.constraints.minY?.let { editor.putInt("constraintsMinY", it) }
    config.constraints.maxY?.let { editor.putInt("constraintsMaxY", it) }
    editor.putBoolean("constraintsKeepOnScreen", config.constraints.keepOnScreen)
    editor.putInt("constraintsSnapToGrid", config.constraints.snapToGrid)

    // Badge config (if present)
    if (config.badge != null) {
        editor.putBoolean("hasBadge", true)
        config.badge.text?.let { editor.putString("badgeText", it) }
        config.badge.count?.let { editor.putInt("badgeCount", it) }
        editor.putString("badgePosition", config.badge.position.name)
        editor.putInt("badgeBackgroundColor", config.badge.backgroundColor)
        editor.putInt("badgeTextColor", config.badge.textColor)
        editor.putInt("badgeSize", config.badge.size)
        editor.putInt("badgeTextSize", config.badge.textSize)
    } else {
        editor.putBoolean("hasBadge", false)
    }

    // App state monitoring config
    editor.putBoolean("appStateMonitoringEnabled", config.appStateMonitoring.enabled)
    editor.putLong("appStateMonitoringCheckInterval", config.appStateMonitoring.checkInterval)

    editor.apply()

    return this
}

private fun Intent.getParcelableExtra(context: Context, name: String): WidgetConfig? {
    val prefs = context.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE)

    return try {
        // Read appearance config
        val appearance = WidgetAppearanceConfig(
            backgroundColor = prefs.getInt("appearanceBackgroundColor", 0xCCFFFFFF.toInt()),
            borderColor = prefs.getInt("appearanceBorderColor", 0xFFCCCCCC.toInt()),
            borderWidth = prefs.getInt("appearanceBorderWidth", 2),
            padding = prefs.getInt("appearancePadding", 8),
            opacity = prefs.getFloat("appearanceOpacity", 1.0f),
            cornerRadius = prefs.getInt("appearanceCornerRadius", 12)
        )

        // Read gradient colors if present
        val gradientColors = prefs.getString("dismissZoneGradientColors", null)?.let { str ->
            str.split(",").map { it.toInt() }.toIntArray()
        }
        val activeGradientColors = prefs.getString("dismissZoneActiveGradientColors", null)?.let { str ->
            str.split(",").map { it.toInt() }.toIntArray()
        }

        // Read dismiss zone config
        val dismissZone = DismissZoneConfig(
            enabled = prefs.getBoolean("dismissZoneEnabled", false),
            showOn = DismissZoneConfig.ShowTrigger.valueOf(
                prefs.getString("dismissZoneShowOn", "ALWAYS") ?: "ALWAYS"
            ),
            height = prefs.getInt("dismissZoneHeightNew", 100),
            backgroundColor = prefs.getInt("dismissZoneBackgroundColor", 0x96FF0000.toInt()),
            activeBackgroundColor = prefs.getInt("dismissZoneActiveBackgroundColor", 0xC8FF0000.toInt()),
            gradientColors = gradientColors,
            activeGradientColors = activeGradientColors,
            gradientOrientation = DismissZoneConfig.GradientOrientation.valueOf(
                prefs.getString("dismissZoneGradientOrientation", "VERTICAL") ?: "VERTICAL"
            ),
            cornerRadius = prefs.getInt("dismissZoneCornerRadius", 0),
            text = prefs.getString("dismissZoneText", "⊗ Release to remove") ?: "⊗ Release to remove",
            textColor = prefs.getInt("dismissZoneTextColor", 0xFFFFFFFF.toInt()),
            textSize = prefs.getInt("dismissZoneTextSize", 16),
            position = DismissZoneConfig.Position.valueOf(
                prefs.getString("dismissZonePosition", "BOTTOM") ?: "BOTTOM"
            )
        )

        // Read animation config
        val animations = AnimationConfig(
            snapDuration = prefs.getLong("animationSnapDuration", 300),
            snapInterpolator = AnimationConfig.InterpolatorType.valueOf(
                prefs.getString("animationSnapInterpolator", "DECELERATE") ?: "DECELERATE"
            ),
            enableScaleOnPress = prefs.getBoolean("animationEnableScaleOnPress", false),
            pressScale = prefs.getFloat("animationPressScale", 0.9f),
            enableHapticFeedback = prefs.getBoolean("animationEnableHapticFeedback", false)
        )

        // Read position constraints config
        val constraints = PositionConstraints(
            minX = if (prefs.contains("constraintsMinX")) prefs.getInt("constraintsMinX", 0) else null,
            maxX = if (prefs.contains("constraintsMaxX")) prefs.getInt("constraintsMaxX", 0) else null,
            minY = if (prefs.contains("constraintsMinY")) prefs.getInt("constraintsMinY", 0) else null,
            maxY = if (prefs.contains("constraintsMaxY")) prefs.getInt("constraintsMaxY", 0) else null,
            keepOnScreen = prefs.getBoolean("constraintsKeepOnScreen", false),
            snapToGrid = prefs.getInt("constraintsSnapToGrid", 0)
        )

        // Read badge config (if present)
        val badge = if (prefs.getBoolean("hasBadge", false)) {
            BadgeConfig(
                text = prefs.getString("badgeText", null),
                count = if (prefs.contains("badgeCount")) prefs.getInt("badgeCount", 0) else null,
                position = BadgeConfig.Position.valueOf(
                    prefs.getString("badgePosition", "TOP_RIGHT") ?: "TOP_RIGHT"
                ),
                backgroundColor = prefs.getInt("badgeBackgroundColor", 0xFFF44336.toInt()),
                textColor = prefs.getInt("badgeTextColor", 0xFFFFFFFF.toInt()),
                size = prefs.getInt("badgeSize", 20),
                textSize = prefs.getInt("badgeTextSize", 10)
            )
        } else {
            null
        }

        // Read app state monitoring config
        val appStateMonitoring = AppStateMonitoringConfig(
            enabled = prefs.getBoolean("appStateMonitoringEnabled", true),
            checkInterval = prefs.getLong("appStateMonitoringCheckInterval", 1000)
        )

        WidgetConfig(
            icon = null, // Icon is stored separately
            size = prefs.getInt("size", 56),
            shape = WidgetConfig.WidgetShape.valueOf(
                prefs.getString("shape", "CIRCLE") ?: "CIRCLE"
            ),
            draggable = prefs.getBoolean("draggable", true),
            initialX = if (prefs.contains("initialX")) prefs.getInt("initialX", 0) else null,
            initialY = if (prefs.contains("initialY")) prefs.getInt("initialY", 0) else null,
            notification = NotificationConfig(
                title = prefs.getString("notificationTitle", "") ?: "",
                text = prefs.getString("notificationText", "") ?: "",
                channelId = prefs.getString("notificationChannelId", "") ?: "",
                channelName = prefs.getString("notificationChannelName", "") ?: "",
                icon = prefs.getString("notificationIcon", null)
            ),
            autoStartOnBoot = prefs.getBoolean("autoStartOnBoot", false),
            hideOnAppOpen = prefs.getBoolean("hideOnAppOpen", true),

            // New configuration objects
            appearance = appearance,
            dismissZone = dismissZone,
            animations = animations,
            constraints = constraints,
            badge = badge,
            appStateMonitoring = appStateMonitoring,

            // Callback flags
            hasClickCallback = prefs.getBoolean("hasClickCallback", false),
            hasLongPressCallback = prefs.getBoolean("hasLongPressCallback", false),
            hasDragCallback = prefs.getBoolean("hasDragCallback", false),
            hasShowCallback = prefs.getBoolean("hasShowCallback", false),
            hasHideCallback = prefs.getBoolean("hasHideCallback", false),
            hasDismissCallback = prefs.getBoolean("hasDismissCallback", false),
            hasPositionChangeCallback = prefs.getBoolean("hasPositionChangeCallback", false),
            hasAppStateCallbacks = prefs.getBoolean("hasAppStateCallbacks", false),

            longPressDuration = prefs.getLong("longPressDuration", 500),

            // Legacy properties (deprecated but still supported)
            enableDragToDismiss = prefs.getBoolean("enableDragToDismiss", false),
            dismissZoneHeight = prefs.getInt("dismissZoneHeight", 100),
            snapToEdge = prefs.getBoolean("snapToEdge", false)
        )
    } catch (e: Exception) {
        null
    }
}
