package xyz.useclippr.sdk.models

import java.util.Date

data class LinkParameters(
    val path: String,
    val metadata: Map<String, Any?>? = null,
    val campaign: String? = null,
    val source: String? = null,
    val medium: String? = null,
    val socialTags: SocialMetaTags? = null,
    val alias: String? = null,
    val iosFallbackUrl: String? = null,
    val androidFallbackUrl: String? = null,
    val webFallbackUrl: String? = null,
    val expiresAt: Date? = null
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