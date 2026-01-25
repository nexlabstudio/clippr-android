package xyz.useclippr.sdk.models

/**
 * Represents a deep link received by the SDK
 */
data class ClipprLink(
    /** The deep link path (e.g., "/product/123") */
    val path: String,
    
    /** Custom metadata attached to the link */
    val metadata: Map<String, Any?>? = null,
    
    /** Attribution data */
    val attribution: Attribution? = null,
    
    /** How this link was matched */
    val matchType: MatchType = MatchType.DIRECT,
    
    /** Confidence score (0.0 - 1.0) for probabilistic matches */
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
