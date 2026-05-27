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
import xyz.useclippr.sdk.models.LinkParameters
import xyz.useclippr.sdk.models.MatchType
import xyz.useclippr.sdk.models.ShortLink
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
     * POST /sdk/match
     */
    suspend fun match(payload: Map<String, Any?>): MatchResponse? = withContext(Dispatchers.IO) {
        val response = post("/sdk/match", payload)
        
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
            metadata = response.optJSONObject("metadata")?.jsonToMap(),
            matchType = MatchType.fromString(response.optString("match_type")),
            confidence = response.optDouble("confidence").takeIf { !it.isNaN() },
            attribution = attribution
        )
    }
    
    /**
     * Track app install
     * POST /sdk/install
     */
    suspend fun trackInstall(payload: Map<String, Any?>) = withContext(Dispatchers.IO) {
        post("/sdk/install", payload)
        Logger.debug("Install tracked successfully")
    }
    
    /**
     * Track custom event
     * POST /sdk/events
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
        
        post("/sdk/events", body)
        Logger.debug("Event '$eventName' tracked successfully")
    }

    /**
     * Resolve a short code or alias to full link details. Used to enrich
     * App Link clicks with backend-stored attribution + canonical deep-link path.
     */
    suspend fun resolveLink(identifier: String): ResolvedLink = withContext(Dispatchers.IO) {
        val response = get("/sdk/links/resolve/$identifier")
        val attribution = Attribution(
            campaign = response.optString("campaign").takeIf { it.isNotEmpty() },
            source = response.optString("source").takeIf { it.isNotEmpty() },
            medium = response.optString("medium").takeIf { it.isNotEmpty() }
        )
        ResolvedLink(
            deepLinkPath = response.optString("deep_link_path"),
            metadata = response.optJSONObject("metadata")?.jsonToMap(),
            attribution = attribution.takeIf { it.campaign != null || it.source != null || it.medium != null }
        )
    }

    /**
     * Create a short link
     */
    suspend fun createLink(parameters: LinkParameters): ShortLink = withContext(Dispatchers.IO) {
        val body = mutableMapOf<String, Any?>(
            "deep_link_path" to parameters.path
        )

        parameters.metadata?.let { body["metadata"] = it }
        parameters.campaign?.let { body["campaign"] = it }
        parameters.source?.let { body["source"] = it }
        parameters.medium?.let { body["medium"] = it }
        parameters.alias?.let { body["alias"] = it }
        parameters.iosFallbackUrl?.let { body["ios_fallback_url"] = it }
        parameters.androidFallbackUrl?.let { body["android_fallback_url"] = it }
        parameters.webFallbackUrl?.let { body["web_fallback_url"] = it }
        parameters.expiresAt?.let {
            body["expires_at"] = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.US).apply {
                timeZone = java.util.TimeZone.getTimeZone("UTC")
            }.format(it)
        }

        parameters.socialTags?.let { tags ->
            tags.title?.let { body["social_title"] = it }
            tags.description?.let { body["social_description"] = it }
            tags.imageUrl?.let { body["social_image_url"] = it }
        }

        val response = post("/sdk/links", body)

        val shortUrl = response.optString("short_url").takeIf { it.isNotEmpty() }
            ?: throw ClipprException.InvalidResponse()
        val shortCode = response.optString("short_code").takeIf { it.isNotEmpty() }
            ?: throw ClipprException.InvalidResponse()

        Logger.debug("Short link created: $shortUrl")

        ShortLink(
            url = shortUrl,
            shortCode = shortCode,
            path = parameters.path
        )
    }

    /**
     * Make a GET request
     */
    private fun get(endpoint: String): JSONObject {
        val url = "${config.baseUrl}$endpoint"

        Logger.debug("GET $endpoint")

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("X-API-Key", config.apiKey)
            .get()
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
    private fun JSONObject.jsonToMap(): Map<String, Any?> {
        val map = mutableMapOf<String, Any?>()
        keys().forEach { key ->
            map[key] = when (val value = get(key)) {
                is JSONObject -> value.jsonToMap()
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

internal data class ResolvedLink(
    val deepLinkPath: String,
    val metadata: Map<String, Any?>?,
    val attribution: Attribution?
)
