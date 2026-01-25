# Clippr Android SDK

Deep linking and mobile attribution SDK for Android.

## Installation

### Gradle (Maven Central)

Add the dependency to your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("xyz.clppr:clippr-sdk:1.0.0")
}
```

Or with Groovy `build.gradle`:

```groovy
dependencies {
    implementation 'xyz.clppr:clippr-sdk:1.0.0'
}
```

## Quick Start

### 1. Initialize the SDK

```kotlin
import xyz.clppr.sdk.Clippr

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
import xyz.clppr.sdk.Clippr
import xyz.clppr.sdk.models.ClipprLink

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

## Java Usage

```java
import xyz.clppr.sdk.Clippr;
import xyz.clppr.sdk.models.ClipprLink;

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
