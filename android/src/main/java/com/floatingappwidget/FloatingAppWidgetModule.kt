package com.floatingappwidget

import android.content.Context
import android.content.Intent
import com.facebook.react.bridge.Promise
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.bridge.ReadableMap
import com.facebook.react.modules.core.DeviceEventManagerModule

/**
 * React Native module for FloatingAppWidget
 * Provides JS API for managing floating widgets
 */
class FloatingAppWidgetModule(reactContext: ReactApplicationContext) :
    ReactContextBaseJavaModule(reactContext) {

    private var currentConfig: WidgetConfig? = null
    private var isServiceRunning = false

    companion object {
        const val NAME = "FloatingAppWidget"

        /**
         * Send event to React Native JavaScript
         * Safe to call even if React context is not available (e.g., during hot reload)
         */
        fun sendEvent(context: ReactApplicationContext, eventName: String, params: ReadableMap?) {
            try {
                // Check if React instance is active before sending events
                if (!context.hasActiveReactInstance()) {
                    return
                }

                context
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
                    ?.emit(eventName, params)
            } catch (e: Exception) {
                // Silently ignore errors during development hot reload
                // This prevents crashes when React context is being recreated
            }
        }
    }

    override fun getName(): String {
        return NAME
    }

    /**
     * Initialize the floating widget with configuration
     */
    @ReactMethod
    fun init(configMap: ReadableMap, promise: Promise) {
        try {
            // Parse configuration
            val config = WidgetConfig.fromReadableMap(configMap)
            currentConfig = config

            // Save config to shared preferences
            saveConfigToPrefs(config)

            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("INIT_ERROR", "Failed to initialize widget: ${e.message}", e)
        }
    }

    /**
     * Start the floating widget service
     */
    @ReactMethod
    fun start(promise: Promise) {
        try {
            val context = reactApplicationContext

            // Check if initialized
            if (currentConfig == null) {
                promise.reject("NOT_INITIALIZED", "Widget not initialized. Call init() first.")
                return
            }

            // Check permission
            if (!PermissionHelper.hasOverlayPermission(context)) {
                promise.reject(
                    "PERMISSION_DENIED",
                    "SYSTEM_ALERT_WINDOW permission not granted. Call requestPermission() first."
                )
                return
            }

            // Start service
            FloatingWidgetService.start(context, currentConfig!!)

            // Mark service as enabled
            val prefs = context.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("serviceEnabled", true).apply()

            isServiceRunning = true

            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("START_ERROR", "Failed to start widget: ${e.message}", e)
        }
    }

    /**
     * Stop the floating widget service
     */
    @ReactMethod
    fun stop(promise: Promise) {
        try {
            val context = reactApplicationContext

            // Stop service
            FloatingWidgetService.stop(context)

            // Mark service as disabled
            val prefs = context.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE)
            prefs.edit().putBoolean("serviceEnabled", false).apply()

            isServiceRunning = false

            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("STOP_ERROR", "Failed to stop widget: ${e.message}", e)
        }
    }

    /**
     * Trigger pulse animation
     */
    @ReactMethod
    fun pulse(config: ReadableMap, promise: Promise) {
        try {
            val context = reactApplicationContext

            if (!isServiceRunning) {
                promise.reject(
                    "SERVICE_NOT_RUNNING",
                    "Widget service is not running. Call start() first."
                )
                return
            }

            // Send pulse command to service
            val intent = Intent(context, FloatingWidgetService::class.java)
            intent.action = "com.floatingappwidget.PULSE"
            intent.putExtra("count", if (config.hasKey("count")) config.getInt("count") else 3)
            intent.putExtra("duration", if (config.hasKey("duration")) config.getInt("duration") else 500)
            intent.putExtra("scale", if (config.hasKey("scale")) config.getDouble("scale").toFloat() else 1.2f)
            intent.putExtra("alpha", if (config.hasKey("alpha")) config.getDouble("alpha").toFloat() else 0.7f)
            context.startService(intent)

            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("PULSE_ERROR", "Failed to trigger pulse: ${e.message}", e)
        }
    }

    /**
     * Update widget appearance
     */
    @ReactMethod
    fun updateAppearance(appearance: ReadableMap, promise: Promise) {
        try {
            val context = reactApplicationContext

            if (!isServiceRunning) {
                promise.reject(
                    "SERVICE_NOT_RUNNING",
                    "Widget service is not running. Call start() first."
                )
                return
            }

            // Send update appearance command to service
            val intent = Intent(context, FloatingWidgetService::class.java)
            intent.action = "com.floatingappwidget.UPDATE_APPEARANCE"
            intent.putExtra("appearance", appearance.toHashMap() as HashMap<*, *>)
            context.startService(intent)

            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("UPDATE_APPEARANCE_ERROR", "Failed to update appearance: ${e.message}", e)
        }
    }

    /**
     * Update widget badge
     */
    @ReactMethod
    fun updateBadge(badge: ReadableMap, promise: Promise) {
        try {
            val context = reactApplicationContext

            if (!isServiceRunning) {
                promise.reject(
                    "SERVICE_NOT_RUNNING",
                    "Widget service is not running. Call start() first."
                )
                return
            }

            // Send update badge command to service
            val intent = Intent(context, FloatingWidgetService::class.java)
            intent.action = "com.floatingappwidget.UPDATE_BADGE"
            intent.putExtra("badge", badge.toHashMap() as HashMap<*, *>)
            context.startService(intent)

            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("UPDATE_BADGE_ERROR", "Failed to update badge: ${e.message}", e)
        }
    }

    /**
     * Update widget configuration
     */
    @ReactMethod
    fun update(configMap: ReadableMap, promise: Promise) {
        try {
            val context = reactApplicationContext

            // Parse configuration
            val config = WidgetConfig.fromReadableMap(configMap)
            currentConfig = config

            // Save config to shared preferences
            saveConfigToPrefs(config)

            // If service is running, update it
            if (isServiceRunning) {
                // Restart service with new config
                FloatingWidgetService.stop(context)
                FloatingWidgetService.start(context, config)
            }

            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("UPDATE_ERROR", "Failed to update widget: ${e.message}", e)
        }
    }

    /**
     * Check if SYSTEM_ALERT_WINDOW permission is granted
     */
    @ReactMethod
    fun hasPermission(promise: Promise) {
        try {
            val context = reactApplicationContext
            val hasPermission = PermissionHelper.hasOverlayPermission(context)
            promise.resolve(hasPermission)
        } catch (e: Exception) {
            promise.reject("PERMISSION_CHECK_ERROR", "Failed to check permission: ${e.message}", e)
        }
    }

    /**
     * Request SYSTEM_ALERT_WINDOW permission
     */
    @ReactMethod
    fun requestPermission(promise: Promise) {
        try {
            val context = reactApplicationContext
            PermissionHelper.requestOverlayPermission(context)
            promise.resolve(null)
        } catch (e: Exception) {
            promise.reject("PERMISSION_REQUEST_ERROR", "Failed to request permission: ${e.message}", e)
        }
    }

    /**
     * Save configuration to SharedPreferences
     */
    private fun saveConfigToPrefs(config: WidgetConfig) {
        val context = reactApplicationContext
        val prefs = context.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE)

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
            putBoolean("hasClickCallback", config.hasClickCallback)
            putBoolean("hasDragCallback", config.hasDragCallback)
            putBoolean("enableDragToDismiss", config.enableDragToDismiss)
            putInt("dismissZoneHeight", config.dismissZoneHeight)
            putBoolean("snapToEdge", config.snapToEdge)
        }.apply()

        // Save icon if present
        config.icon?.let { bitmap ->
            // In a production app, you might want to save the bitmap to file storage
            // For simplicity, we're not persisting the bitmap here
        }
    }

    /**
     * Open device-specific autostart/background settings
     * Helps users whitelist the app on MIUI, EMUI, ColorOS, etc.
     */
    @ReactMethod
    fun openAutostartSettings(promise: Promise) {
        try {
            val context = reactApplicationContext
            val manufacturer = android.os.Build.MANUFACTURER.lowercase()

            val intent = when {
                // Xiaomi MIUI
                manufacturer.contains("xiaomi") -> {
                    Intent().apply {
                        setClassName(
                            "com.miui.securitycenter",
                            "com.miui.permcenter.autostart.AutoStartManagementActivity"
                        )
                    }
                }
                // Huawei EMUI
                manufacturer.contains("huawei") -> {
                    Intent().apply {
                        setClassName(
                            "com.huawei.systemmanager",
                            "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"
                        )
                    }
                }
                // Oppo ColorOS
                manufacturer.contains("oppo") -> {
                    Intent().apply {
                        setClassName(
                            "com.coloros.safecenter",
                            "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                        )
                    }
                }
                // Vivo FuntouchOS
                manufacturer.contains("vivo") -> {
                    Intent().apply {
                        setClassName(
                            "com.vivo.permissionmanager",
                            "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                        )
                    }
                }
                // OnePlus OxygenOS
                manufacturer.contains("oneplus") -> {
                    Intent().apply {
                        setClassName(
                            "com.oneplus.security",
                            "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"
                        )
                    }
                }
                // Samsung (battery optimization)
                manufacturer.contains("samsung") -> {
                    Intent().apply {
                        action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                }
                // Generic fallback
                else -> {
                    Intent().apply {
                        action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
                }
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            promise.resolve(manufacturer)
        } catch (e: Exception) {
            // Fallback to app settings if specific intent fails
            try {
                val intent = Intent().apply {
                    action = android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                    data = android.net.Uri.fromParts("package", reactApplicationContext.packageName, null)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                reactApplicationContext.startActivity(intent)
                promise.resolve("fallback")
            } catch (e2: Exception) {
                promise.reject("SETTINGS_ERROR", "Failed to open settings: ${e2.message}", e2)
            }
        }
    }

    /**
     * Check if the device manufacturer is known for aggressive battery optimization
     */
    @ReactMethod
    fun isAggressiveDevice(promise: Promise) {
        val manufacturer = android.os.Build.MANUFACTURER.lowercase()
        val isAggressive = manufacturer.contains("xiaomi") ||
                          manufacturer.contains("huawei") ||
                          manufacturer.contains("oppo") ||
                          manufacturer.contains("vivo") ||
                          manufacturer.contains("oneplus")

        promise.resolve(mapOf(
            "isAggressive" to isAggressive,
            "manufacturer" to manufacturer
        ))
    }
}
