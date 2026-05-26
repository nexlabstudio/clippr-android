package xyz.useclippr.sdk.internal

import android.content.Context
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal object AdvertisingIdHelper {
    suspend fun getAdvertisingId(context: Context): String? = withContext(Dispatchers.IO) {
        try {
            val info = AdvertisingIdClient.getAdvertisingIdInfo(context)
            if (info.isLimitAdTrackingEnabled) {
                return@withContext null
            }
            info.id
        } catch (e: Exception) {
            Logger.error("Failed to fetch GAID", e)
            null
        }
    }
}
