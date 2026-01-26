package xyz.useclippr.sdk.models

/**
 * Configuration for the Clippr SDK
 */
data class ClipprConfig(
    /** Your Clippr API key */
    val apiKey: String,
    
    /** Enable debug logging */
    val debug: Boolean = false,
    
    /** Request timeout in milliseconds */
    val timeoutMs: Long = 10_000L,
    
    /** Base URL for API (defaults to Clippr's API) */
    val baseUrl: String = DEFAULT_BASE_URL
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://api.clppr.xyz/v1"
    }
}
