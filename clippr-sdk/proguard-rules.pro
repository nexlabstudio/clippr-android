# ClipprSDK ProGuard Rules

# Keep public API
-keep class xyz.useclippr.sdk.Clippr { *; }
-keep class xyz.useclippr.sdk.models.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep Install Referrer
-keep class com.android.installreferrer.** { *; }

# Keep Google Play Services Ads Identifier
# TODO(mastersam07): Enable this for GAID
# -keep class com.google.android.gms.ads.identifier.** { *; }