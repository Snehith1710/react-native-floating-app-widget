package com.floatingappwidget

import android.content.Context
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
}
