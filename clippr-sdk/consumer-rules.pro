# ClipprSDK Consumer ProGuard Rules
# These rules are automatically applied to apps using the SDK

# Keep public API
-keep class xyz.clppr.sdk.Clippr { *; }
-keep class xyz.clppr.sdk.models.** { *; }
