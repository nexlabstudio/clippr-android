package xyz.useclippr.sdk.internal

import android.content.Context
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
import java.util.Locale
import java.util.TimeZone

/**
 * Collects device information for fingerprinting and matching
 */
internal class DeviceInfo(
    private val context: Context,
    private val storage: Storage
) {

    private var cachedGaid: String? = null

    val deviceId: String
        get() = storage.deviceId

    val advertisingId: String?
        get() = cachedGaid
    
    /**
     * Platform identifier
     */
    val platform: String = "android"
    
    /**
     * User agent string
     */
    val userAgent: String
        get() {
            val appName = context.packageName
            val appVersion = try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                "unknown"
            }
            return "ClipprSDK/1.0 ($deviceModel; Android $osVersion) $appName/$appVersion"
        }
    
    /**
     * Device model (e.g., "Pixel 7")
     */
    val deviceModel: String
        get() = "${Build.MANUFACTURER} ${Build.MODEL}"
    
    /**
     * OS version (e.g., "14")
     */
    val osVersion: String
        get() = Build.VERSION.RELEASE
    
    /**
     * App version
     */
    val appVersion: String?
        get() = try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        } catch (e: Exception) {
            null
        }
    
    /**
     * Screen resolution (e.g., "1080x2400")
     */
    val screenResolution: String
        get() {
            val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            return "${metrics.widthPixels}x${metrics.heightPixels}"
        }
    
    /**
     * Timezone identifier (e.g., "America/New_York")
     */
    val timezone: String
        get() = TimeZone.getDefault().id
    
    /**
     * Preferred language (e.g., "en-US")
     */
    val language: String
        get() = Locale.getDefault().toLanguageTag()
    
    /**
     * Package name of the app
     */
    val packageName: String
        get() = context.packageName
    
    suspend fun fetchAdvertisingId() {
        cachedGaid = AdvertisingIdHelper.getAdvertisingId(context)
    }

    fun buildMatchPayload(installReferrer: String? = null): Map<String, Any?> {
        val payload = mutableMapOf<String, Any?>(
            "device_id" to deviceId,
            "platform" to platform,
            "user_agent" to userAgent,
            "screen_resolution" to screenResolution,
            "timezone" to timezone,
            "language" to language
        )

        cachedGaid?.let {
            payload["advertising_id"] = it
        }

        if (installReferrer != null) {
            payload["install_referrer"] = installReferrer
        }

        return payload
    }

    fun buildInstallPayload(): Map<String, Any?> {
        val payload = mutableMapOf<String, Any?>(
            "device_id" to deviceId,
            "platform" to platform,
            "os_version" to osVersion,
            "app_version" to appVersion,
            "device_model" to deviceModel
        )

        cachedGaid?.let {
            payload["advertising_id"] = it
        }

        return payload
    }
}