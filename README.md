# Clippr Android SDK

Deep linking and mobile attribution SDK for Android.

## Installation

### Maven Central

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("xyz.useclippr:clippr:0.0.4")
}
```

Or with Groovy `build.gradle`:

```groovy
dependencies {
    implementation 'xyz.useclippr:clippr:0.0.4'
}
```

### JitPack

Add JitPack repository to your `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

Then add the dependency:

```kotlin
dependencies {
    implementation("com.github.nexlabstudio:clippr-android:0.0.4")
}
```

## Quick Start

### 1. Initialize the SDK

```kotlin
import xyz.useclippr.sdk.Clippr

class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        Clippr.initialize(
            context = this,
            apiKey = "your_api_key_here",
            debug = BuildConfig.DEBUG
        )
    }
}
```

### 2. Handle Deep Links

```kotlin
import xyz.useclippr.sdk.Clippr
import xyz.useclippr.sdk.models.ClipprLink

class MainActivity : AppCompatActivity() {
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Handle App Link that opened this activity
        Clippr.handle(intent)
        
        // Get the initial link (direct or deferred)
        lifecycleScope.launch {
            val link = Clippr.getInitialLink()
            if (link != null) {
                handleDeepLink(link)
            }
        }
        
        // Listen for links while app is running
        Clippr.onLink = { link ->
            handleDeepLink(link)
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle links when activity is already open
        Clippr.handle(intent)
    }
    
    private fun handleDeepLink(link: ClipprLink) {
        Log.d("Clippr", "Deep link path: ${link.path}")
        Log.d("Clippr", "Metadata: ${link.metadata}")
        Log.d("Clippr", "Campaign: ${link.attribution?.campaign}")
        
        // Navigate based on path
        when {
            link.path.startsWith("/product/") -> {
                val productId = link.path.substringAfterLast("/")
                // Navigate to product
            }
        }
    }
}
```

### 3. Configure App Links

Add the intent filter to your `AndroidManifest.xml`:

```xml
<activity android:name=".MainActivity">
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        
        <data
            android:scheme="https"
            android:host="yourapp.clppr.xyz" />
    </intent-filter>
</activity>
```

Your Asset Links file is automatically hosted by Clippr at:
`https://yourapp.clppr.xyz/.well-known/assetlinks.json`

### 4. Track Events (Optional)

```kotlin
// Track a simple event
lifecycleScope.launch {
    Clippr.track("signup_completed")
}

// Track with parameters
lifecycleScope.launch {
    Clippr.track("add_to_cart", mapOf(
        "product_id" to "12345",
        "price" to 29.99
    ))
}

// Track revenue
lifecycleScope.launch {
    Clippr.trackRevenue(
        eventName = "purchase",
        revenue = 99.99,
        currency = "USD",
        params = mapOf("product_id" to "12345")
    )
}

// Using callback (Java-friendly)
Clippr.track("button_clicked", null) { error ->
    if (error != null) {
        Log.e("Clippr", "Failed to track", error)
    }
}
```

### 5. Create Short Links (Optional)

```kotlin
import xyz.useclippr.sdk.models.LinkParameters
import xyz.useclippr.sdk.models.SocialMetaTags

// Create a simple short link
lifecycleScope.launch {
    val shortLink = Clippr.createLink(
        LinkParameters(path = "/product/123")
    )
    Log.d("Clippr", "Short link: ${shortLink.url}")
}

// Create a link with attribution and social tags
lifecycleScope.launch {
    val shortLink = Clippr.createLink(
        LinkParameters(
            path = "/product/123",
            metadata = mapOf("discount" to "20%"),
            campaign = "summer_sale",
            source = "facebook",
            medium = "social",
            alias = "summer-deal",  // Custom short code
            socialTags = SocialMetaTags(
                title = "Check out this deal!",
                description = "Get 20% off on selected items",
                imageUrl = "https://example.com/promo.jpg"
            )
        )
    )

    // Share the link
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, shortLink.url)
    }
    startActivity(Intent.createChooser(shareIntent, "Share via"))
}

// Using callback (Java-friendly)
Clippr.createLink(LinkParameters("/referral/user123")) { shortLink, error ->
    if (error != null) {
        Log.e("Clippr", "Failed to create link", error)
    } else {
        Log.d("Clippr", "Created: ${shortLink?.url}")
    }
}
```

## Java Usage

```java
import xyz.useclippr.sdk.Clippr;
import xyz.useclippr.sdk.models.ClipprLink;

public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Handle App Link
        Clippr.handle(getIntent());
        
        // Get initial link
        Clippr.getInitialLink(link -> {
            if (link != null) {
                handleDeepLink(link);
            }
        });
        
        // Listen for links
        Clippr.setOnLink(link -> {
            handleDeepLink(link);
        });
    }
    
    private void handleDeepLink(ClipprLink link) {
        Log.d("Clippr", "Path: " + link.getPath());
    }
}
```

## API Reference

### Clippr

| Method | Description |
|--------|-------------|
| `initialize(context, apiKey, debug)` | Initialize the SDK |
| `getInitialLink()` | Get the link that opened the app |
| `onLink` | Callback for links received while app is running |
| `handle(intent)` | Handle incoming App Links |
| `track(eventName, params)` | Track a custom event |
| `trackRevenue(eventName, revenue, currency, params)` | Track a revenue event |
| `createLink(parameters)` | Create a short link for sharing |

### ClipprLink

| Property | Type | Description |
|----------|------|-------------|
| `path` | `String` | The deep link path (e.g., "/product/123") |
| `metadata` | `Map<String, Any?>?` | Custom metadata attached to the link |
| `attribution` | `Attribution?` | Campaign attribution data |
| `matchType` | `MatchType` | How the link was matched |
| `confidence` | `Double?` | Match confidence (0.0 - 1.0) |

### MatchType

| Value | Description |
|-------|-------------|
| `DIRECT` | User clicked link with app installed |
| `DETERMINISTIC` | Matched via Install Referrer (100% accurate) |
| `PROBABILISTIC` | Matched via device fingerprinting |
| `NONE` | No match found |

### LinkParameters

| Property | Type | Description |
|----------|------|-------------|
| `path` | `String` | Deep link path (e.g., "/product/123") |
| `metadata` | `Map<String, Any?>?` | Custom metadata to attach |
| `campaign` | `String?` | Campaign name for attribution |
| `source` | `String?` | Traffic source (e.g., "facebook") |
| `medium` | `String?` | Marketing medium (e.g., "social") |
| `alias` | `String?` | Custom short code |
| `socialTags` | `SocialMetaTags?` | Social preview tags |

### SocialMetaTags

| Property | Type | Description |
|----------|------|-------------|
| `title` | `String?` | Title for social previews |
| `description` | `String?` | Description for social previews |
| `imageUrl` | `String?` | Image URL for social previews |

### ShortLink

| Property | Type | Description |
|----------|------|-------------|
| `url` | `String` | The full short URL |
| `shortCode` | `String` | The short code or alias |
| `path` | `String` | The original deep link path |

## Debug Mode

Enable debug logging during development:

```kotlin
Clippr.initialize(
    context = this,
    apiKey = "your_api_key",
    debug = true
)
```

## Requirements

- Android API 21+ (Android 5.0)
- Kotlin 1.9+ or Java 8+

## License

MIT License. See LICENSE for details.
