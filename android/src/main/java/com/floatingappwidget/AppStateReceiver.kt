package com.floatingappwidget

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper

/**
 * Broadcast receiver for monitoring app state (foreground/background)
 */
class AppStateReceiver(
    private val onAppStateChanged: (isAppInForeground: Boolean) -> Unit
) : BroadcastReceiver() {

    companion object {
        private val handler = Handler(Looper.getMainLooper())
        private var checkRunnable: Runnable? = null

        /**
         * Check if app is currently in foreground
         */
        fun isAppInForeground(context: Context): Boolean {
            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

            // For Android 5.0+, use RunningAppProcessInfo
            val appProcesses = activityManager.runningAppProcesses ?: return false

            val packageName = context.packageName
            for (appProcess in appProcesses) {
                if (appProcess.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND &&
                    appProcess.processName == packageName) {
                    return true
                }
            }

            return false
        }

        /**
         * Start monitoring app state with custom interval
         */
        fun updateAppState(context: Context, callback: (Boolean) -> Unit, checkInterval: Long = 1000) {
            // Cancel any existing checks
            checkRunnable?.let { handler.removeCallbacks(it) }

            // Check immediately
            callback(isAppInForeground(context))

            // Start periodic checks with custom interval
            checkRunnable = object : Runnable {
                override fun run() {
                    callback(isAppInForeground(context))
                    handler.postDelayed(this, checkInterval)
                }
            }
            handler.post(checkRunnable!!)
        }

        /**
         * Stop monitoring app state
         */
        fun stopMonitoring() {
            checkRunnable?.let { handler.removeCallbacks(it) }
            checkRunnable = null
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_SCREEN_OFF -> {
                // Screen is off, app is effectively in background
                onAppStateChanged(false)
            }
            Intent.ACTION_SCREEN_ON -> {
                // Screen is on, check actual app state
                val isInForeground = isAppInForeground(context)
                onAppStateChanged(isInForeground)
            }
        }
    }
}
