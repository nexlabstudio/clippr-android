package xyz.clppr.sdk.internal

import android.util.Log

/**
 * Internal logger for the SDK
 */
internal object Logger {
    private const val TAG = "ClipprSDK"
    
    var isEnabled: Boolean = false
    
    fun debug(message: String) {
        if (isEnabled) {
            Log.d(TAG, message)
        }
    }
    
    fun info(message: String) {
        if (isEnabled) {
            Log.i(TAG, message)
        }
    }
    
    fun error(message: String, throwable: Throwable? = null) {
        // Always log errors
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.e(TAG, message)
        }
    }
}
