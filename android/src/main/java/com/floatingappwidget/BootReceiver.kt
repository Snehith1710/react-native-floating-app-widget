package com.floatingappwidget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences

/**
 * Broadcast receiver for device boot events
 * Automatically starts the floating widget service if enabled
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON") {

            // Check if auto-start is enabled
            val prefs = context.getSharedPreferences("FloatingWidget", Context.MODE_PRIVATE)
            val autoStartEnabled = prefs.getBoolean("autoStartOnBoot", false)
            val isServiceEnabled = prefs.getBoolean("serviceEnabled", false)

            if (autoStartEnabled && isServiceEnabled) {
                // Check if permission is granted
                if (PermissionHelper.hasOverlayPermission(context)) {
                    // Reconstruct config from preferences
                    val config = reconstructConfigFromPrefs(prefs)

                    if (config != null) {
                        // Start service
                        FloatingWidgetService.start(context, config)
                    }
                }
            }
        }
    }

    /**
     * Reconstruct WidgetConfig from SharedPreferences
     */
    private fun reconstructConfigFromPrefs(prefs: SharedPreferences): WidgetConfig? {
        return try {
            WidgetConfig(
                icon = null, // Icon is stored separately if needed
                size = prefs.getInt("size", 56),
                shape = WidgetConfig.WidgetShape.valueOf(
                    prefs.getString("shape", "CIRCLE") ?: "CIRCLE"
                ),
                draggable = prefs.getBoolean("draggable", true),
                initialX = if (prefs.contains("initialX")) prefs.getInt("initialX", 0) else null,
                initialY = if (prefs.contains("initialY")) prefs.getInt("initialY", 0) else null,
                notification = NotificationConfig(
                    title = prefs.getString("notificationTitle", "Floating Widget") ?: "Floating Widget",
                    text = prefs.getString("notificationText", "Widget is active") ?: "Widget is active",
                    channelId = prefs.getString("notificationChannelId", "floating_widget_channel")
                        ?: "floating_widget_channel",
                    channelName = prefs.getString("notificationChannelName", "Floating Widget")
                        ?: "Floating Widget",
                    icon = prefs.getString("notificationIcon", null)
                ),
                autoStartOnBoot = prefs.getBoolean("autoStartOnBoot", false),
                hideOnAppOpen = prefs.getBoolean("hideOnAppOpen", true)
            )
        } catch (e: Exception) {
            null
        }
    }
}
