package xyz.useclippr.sdk.internal

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

/**
 * Manages persistent storage for the SDK
 */
internal class Storage(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    
    /**
     * Get or create a persistent device ID
     */
    val deviceId: String
        get() {
            val existing = prefs.getString(KEY_DEVICE_ID, null)
            if (existing != null) {
                return existing
            }
            
            val newId = UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
            Logger.debug("Generated new device ID: $newId")
            return newId
        }
    
    /**
     * Whether we've already checked for deferred deep link this install
     */
    var hasCheckedDeferredLink: Boolean
        get() = prefs.getBoolean(KEY_CHECKED_DEFERRED, false)
        set(value) = prefs.edit().putBoolean(KEY_CHECKED_DEFERRED, value).apply()
    
    /**
     * Last deferred link path (to avoid re-delivering same link)
     */
    var lastDeferredLinkPath: String?
        get() = prefs.getString(KEY_LAST_DEFERRED_PATH, null)
        set(value) = prefs.edit().putString(KEY_LAST_DEFERRED_PATH, value).apply()
    
    /**
     * Stored install referrer from Play Store
     */
    var installReferrer: String?
        get() = prefs.getString(KEY_INSTALL_REFERRER, null)
        set(value) = prefs.edit().putString(KEY_INSTALL_REFERRER, value).apply()
    
    /**
     * Clear all stored data (for testing)
     */
    fun clear() {
        prefs.edit().clear().apply()
    }
    
    companion object {
        private const val PREFS_NAME = "clippr_sdk_prefs"
        private const val KEY_DEVICE_ID = "clippr_device_id"
        private const val KEY_CHECKED_DEFERRED = "clippr_checked_deferred"
        private const val KEY_LAST_DEFERRED_PATH = "clippr_last_deferred_path"
        private const val KEY_INSTALL_REFERRER = "clippr_install_referrer"
    }
}
