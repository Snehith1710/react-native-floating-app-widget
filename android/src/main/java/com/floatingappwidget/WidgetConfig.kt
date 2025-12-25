package com.floatingappwidget

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import com.facebook.react.bridge.ReadableMap

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
    val hideOnAppOpen: Boolean = true
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

            // Parse icon (base64 string)
            val icon: Bitmap? = if (map.hasKey("icon") && !map.isNull("icon")) {
                try {
                    val iconString = map.getString("icon")!!
                    val imageBytes = Base64.decode(iconString, Base64.DEFAULT)
                    BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } catch (e: Exception) {
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
                    map.getBoolean("hideOnAppOpen") else true
            )
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
