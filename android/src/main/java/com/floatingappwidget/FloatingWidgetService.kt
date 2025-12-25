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

/**
 * Foreground service that manages the floating widget lifecycle
 */
class FloatingWidgetService : Service() {

    private val binder = LocalBinder()
    private var widgetViewManager: WidgetViewManager? = null
    private var appStateReceiver: AppStateReceiver? = null
    private var currentConfig: WidgetConfig? = null
    private var isWidgetVisible = false

    companion object {
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP_SERVICE = "com.floatingappwidget.STOP_SERVICE"

        /**
         * Start the service
         */
        fun start(context: Context, config: WidgetConfig) {
            val intent = Intent(context, FloatingWidgetService::class.java).apply {
                putExtra("config", config)
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
        val config = intent?.getParcelableExtra<WidgetConfig>("config") ?: currentConfig

        if (config != null) {
            currentConfig = config

            // Create and show notification
            val notification = createNotification(config.notification)
            startForeground(NOTIFICATION_ID, notification)

            // Show widget initially (will be hidden if app is in foreground)
            AppStateReceiver.updateAppState(applicationContext) { isAppInForeground ->
                handleAppStateChange(isAppInForeground)
            }
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
private fun Intent.putExtra(name: String, config: WidgetConfig): Intent {
    // Store config in shared preferences instead of passing via intent
    // (since WidgetConfig contains Bitmap which can't be parceled easily)
    val prefs = applicationContext.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE)
    prefs.edit().apply {
        putInt("size", config.size)
        putString("shape", config.shape.name)
        putBoolean("draggable", config.draggable)
        config.initialX?.let { putInt("initialX", it) }
        config.initialY?.let { putInt("initialY", it) }
        putString("notificationTitle", config.notification.title)
        putString("notificationText", config.notification.text)
        putString("notificationChannelId", config.notification.channelId)
        putString("notificationChannelName", config.notification.channelName)
        config.notification.icon?.let { putString("notificationIcon", it) }
        putBoolean("autoStartOnBoot", config.autoStartOnBoot)
        putBoolean("hideOnAppOpen", config.hideOnAppOpen)
    }.apply()

    return this
}

private fun Intent.getParcelableExtra(name: String): WidgetConfig? {
    val prefs = applicationContext.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE)

    return try {
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
            hideOnAppOpen = prefs.getBoolean("hideOnAppOpen", true)
        )
    } catch (e: Exception) {
        null
    }
}
