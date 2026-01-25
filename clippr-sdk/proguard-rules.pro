# ClipprSDK ProGuard Rules

# Keep public API
-keep class xyz.clppr.sdk.Clippr { *; }
-keep class xyz.clppr.sdk.models.** { *; }

# Keep OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# Keep Install Referrer
-keep class com.android.installreferrer.** { *; }
