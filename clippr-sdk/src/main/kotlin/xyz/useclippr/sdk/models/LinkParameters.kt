package xyz.useclippr.sdk.models

/**
 * Parameters for creating a short link
 */
data class LinkParameters(
    /** Deep link path (e.g., "/product/123") */
    val path: String,
    
    /** Custom metadata to attach to the link */
    val metadata: Map<String, Any?>? = null,
    
    /** Campaign name for attribution */
    val campaign: String? = null,
    
    /** Traffic source (e.g., "facebook", "twitter") */
    val source: String? = null,
    
    /** Marketing medium (e.g., "social", "email") */
    val medium: String? = null,
    
    /** Social meta tags for link previews */
    val socialTags: SocialMetaTags? = null,
    
    /** Custom alias for the short link (e.g., "summer-sale" → yourapp.clppr.xyz/summer-sale) */
    val alias: String? = null
)

/**
 * Social meta tags for Open Graph previews
 */
data class SocialMetaTags(
    /** Title shown in link preview */
    val title: String? = null,
    
    /** Description shown in link preview */
    val description: String? = null,
    
    /** Image URL shown in link preview */
    val imageUrl: String? = null
)

/**
 * Response from creating a short link
 */
data class ShortLink(
    /** The full short URL (e.g., "https://yourapp.clppr.xyz/abc123") */
    val url: String,
    
    /** The short code or alias */
    val shortCode: String,
    
    /** The original deep link path */
    val path: String
)