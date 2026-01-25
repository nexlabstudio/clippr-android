package xyz.useclippr.sdk.internal

import android.content.Context
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Helper for retrieving the Play Store install referrer.
 * This enables deterministic attribution (100% accurate).
 */
internal class InstallReferrerHelper(private val context: Context) {
    
    /**
     * Get the install referrer from Play Store.
     * Returns null if not available or on error.
     */
    suspend fun getInstallReferrer(): ReferrerResult? = suspendCancellableCoroutine { continuation ->
        val client = InstallReferrerClient.newBuilder(context).build()
        
        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                when (responseCode) {
                    InstallReferrerClient.InstallReferrerResponse.OK -> {
                        try {
                            val details: ReferrerDetails = client.installReferrer
                            val result = ReferrerResult(
                                referrer = details.installReferrer,
                                clickTimestamp = details.referrerClickTimestampSeconds,
                                installTimestamp = details.installBeginTimestampSeconds
                            )
                            Logger.debug("Install referrer retrieved: ${result.referrer}")
                            continuation.resume(result)
                        } catch (e: Exception) {
                            Logger.error("Failed to get referrer details", e)
                            continuation.resume(null)
                        } finally {
                            client.endConnection()
                        }
                    }
                    InstallReferrerClient.InstallReferrerResponse.FEATURE_NOT_SUPPORTED -> {
                        Logger.debug("Install referrer not supported on this device")
                        continuation.resume(null)
                        client.endConnection()
                    }
                    InstallReferrerClient.InstallReferrerResponse.SERVICE_UNAVAILABLE -> {
                        Logger.debug("Install referrer service unavailable")
                        continuation.resume(null)
                        client.endConnection()
                    }
                    else -> {
                        Logger.debug("Install referrer unknown response: $responseCode")
                        continuation.resume(null)
                        client.endConnection()
                    }
                }
            }
            
            override fun onInstallReferrerServiceDisconnected() {
                Logger.debug("Install referrer service disconnected")
                // Connection will be retried automatically on next request
            }
        })
        
        continuation.invokeOnCancellation {
            try {
                client.endConnection()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
    
    /**
     * Parse Clippr click ID from referrer string.
     * Format: "utm_source=clippr&clippr_click_id=xxx"
     */
    fun parseClickId(referrer: String?): String? {
        if (referrer == null) return null
        
        // Parse as URL query params
        return referrer.split("&")
            .map { it.split("=") }
            .filter { it.size == 2 }
            .associate { it[0] to it[1] }
            .get("clippr_click_id")
    }
}

/**
 * Result from install referrer API
 */
internal data class ReferrerResult(
    val referrer: String,
    val clickTimestamp: Long,
    val installTimestamp: Long
)
