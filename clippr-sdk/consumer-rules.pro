# ClipprSDK Consumer ProGuard Rules
# These rules are automatically applied to apps using the SDK

# Keep public API
-keep class xyz.useclippr.sdk.Clippr { *; }
-keep class xyz.useclippr.sdk.models.** { *; }
