package xyz.useclippr.sdk.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import xyz.useclippr.sdk.models.Attribution
import xyz.useclippr.sdk.models.ClipprConfig
import xyz.useclippr.sdk.models.MatchType
import java.util.concurrent.TimeUnit

/**
 * Errors that can occur during API operations
 */
sealed class ClipprException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NotInitialized : ClipprException("Clippr SDK not initialized. Call Clippr.initialize() first.")
    class InvalidResponse : ClipprException("Invalid response from server")
    class NetworkError(cause: Throwable) : ClipprException("Network error: ${cause.message}", cause)
    class ServerError(val code: Int, message: String?) : ClipprException("Server error $code: ${message ?: "Unknown"}")
}

/**
 * Internal API client for communicating with Clippr backend
 */
internal class APIClient(private val config: ClipprConfig) {
    
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
        .readTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
        .writeTimeout(config.timeoutMs, TimeUnit.MILLISECONDS)
        .build()
    
    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()
    
    /**
     * Check for deferred deep link match
     * POST /v1/sdk/match
     */
    suspend fun match(payload: Map<String, Any?>): MatchResponse? = withContext(Dispatchers.IO) {
        val response = post("/v1/sdk/match", payload)
        
        val matched = response.optBoolean("matched", false)
        if (!matched) {
            return@withContext null
        }
        
        val attribution = response.optJSONObject("attribution")?.let {
            Attribution(
                campaign = it.optString("campaign").takeIf { s -> s.isNotEmpty() },
                source = it.optString("source").takeIf { s -> s.isNotEmpty() },
                medium = it.optString("medium").takeIf { s -> s.isNotEmpty() }
            )
        }
        
        MatchResponse(
            deepLinkPath = response.optString("deep_link_path", ""),
            metadata = response.optJSONObject("metadata")?.toMap(),
            matchType = MatchType.fromString(response.optString("match_type")),
            confidence = response.optDouble("confidence").takeIf { !it.isNaN() },
            attribution = attribution
        )
    }
    
    /**
     * Track app install
     * POST /v1/sdk/install
     */
    suspend fun trackInstall(payload: Map<String, Any?>) = withContext(Dispatchers.IO) {
        post("/v1/sdk/install", payload)
        Logger.debug("Install tracked successfully")
    }
    
    /**
     * Track custom event
     * POST /v1/sdk/events
     */
    suspend fun trackEvent(
        deviceId: String,
        eventName: String,
        params: Map<String, Any?>?,
        revenue: Double?,
        currency: String?
    ) = withContext(Dispatchers.IO) {
        val body = mutableMapOf<String, Any?>(
            "device_id" to deviceId,
            "event_name" to eventName
        )
        
        if (params != null) {
            body["event_params"] = params
        }
        
        if (revenue != null) {
            body["revenue"] = revenue
        }
        
        if (currency != null) {
            body["currency"] = currency
        }
        
        post("/v1/sdk/events", body)
        Logger.debug("Event '$eventName' tracked successfully")
    }
    
    /**
     * Make a POST request
     */
    private fun post(endpoint: String, body: Map<String, Any?>): JSONObject {
        val url = "${config.baseUrl}$endpoint"
        val jsonBody = JSONObject(body).toString()
        
        Logger.debug("POST $endpoint")
        if (config.debug) {
            Logger.debug("Body: $jsonBody")
        }
        
        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-API-Key", config.apiKey)
            .post(jsonBody.toRequestBody(jsonMediaType))
            .build()
        
        try {
            client.newCall(request).execute().use { response ->
                val responseBody = response.body?.string() ?: "{}"
                
                Logger.debug("Response status: ${response.code}")
                
                if (!response.isSuccessful) {
                    val errorMessage = try {
                        JSONObject(responseBody).optString("error")
                    } catch (e: Exception) {
                        null
                    }
                    throw ClipprException.ServerError(response.code, errorMessage)
                }
                
                return JSONObject(responseBody)
            }
        } catch (e: ClipprException) {
            throw e
        } catch (e: Exception) {
            throw ClipprException.NetworkError(e)
        }
    }
    
    /**
     * Convert JSONObject to Map
     */
    private fun JSONObject.toMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        keys().forEach { key ->
            map[key] = when (val value = get(key)) {
                is JSONObject -> value.toMap()
                JSONObject.NULL -> null
                else -> value
            }
        }
        return map
    }
}

/**
 * Response from match API
 */
internal data class MatchResponse(
    val deepLinkPath: String,
    val metadata: Map<String, Any?>?,
    val matchType: MatchType,
    val confidence: Double?,
    val attribution: Attribution?
)
