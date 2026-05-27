package xyz.useclippr.sdk.models

/**
 * Represents a deep link received by the SDK
 */
data class ClipprLink(
    val path: String,
    val url: String? = null,
    val shortCode: String? = null,
    val metadata: Map<String, Any?>? = null,
    val attribution: Attribution? = null,
    val matchType: MatchType = MatchType.DIRECT,
    val confidence: Double? = null
)

/**
 * Attribution data from a link
 */
data class Attribution(
    val campaign: String? = null,
    val source: String? = null,
    val medium: String? = null
)

/**
 * How the deep link was matched
 */
enum class MatchType(val value: String) {
    /** Direct click - app was installed, link opened directly via App Link */
    DIRECT("direct"),
    
    /** Deterministic - matched via Install Referrer (100% accurate) */
    DETERMINISTIC("deterministic"),
    
    /** Probabilistic - matched via device fingerprinting */
    PROBABILISTIC("probabilistic"),
    
    /** No match found */
    NONE("none");
    
    companion object {
        fun fromString(value: String?): MatchType {
            return entries.find { it.value == value } ?: NONE
        }
    }
}
