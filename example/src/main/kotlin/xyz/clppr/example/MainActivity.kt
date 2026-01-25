package xyz.clppr.example

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import xyz.clppr.example.databinding.ActivityMainBinding
import xyz.clppr.sdk.Clippr
import xyz.clppr.sdk.models.ClipprLink

class MainActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityMainBinding
    private var eventsSent = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupUI()
        
        // Handle App Link that opened this activity
        Clippr.handle(intent)
        
        // Get the initial link (direct or deferred)
        lifecycleScope.launch {
            updateStatus("Checking for deep link...")
            
            val link = Clippr.getInitialLink()
            if (link != null) {
                handleDeepLink(link, "getInitialLink")
            } else {
                updateStatus("No deep link found")
            }
        }
        
        // Listen for links while app is running
        Clippr.onLink = { link ->
            handleDeepLink(link, "onLink")
        }
    }
    
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        // Handle links when activity is already open
        Clippr.handle(intent)
    }
    
    private fun setupUI() {
        binding.btnTrackEvent.setOnClickListener {
            trackTestEvent()
        }
        
        binding.btnTrackPurchase.setOnClickListener {
            trackPurchase()
        }
    }
    
    private fun handleDeepLink(link: ClipprLink, source: String) {
        Log.d(TAG, "Deep link received via $source:")
        Log.d(TAG, "  Path: ${link.path}")
        Log.d(TAG, "  Match Type: ${link.matchType}")
        Log.d(TAG, "  Confidence: ${link.confidence}")
        Log.d(TAG, "  Campaign: ${link.attribution?.campaign}")
        
        updateStatus("Link received via $source")
        updateLinkInfo(link)
    }
    
    private fun updateStatus(status: String) {
        runOnUiThread {
            binding.tvStatus.text = status
            binding.tvEventCount.text = "Events sent: $eventsSent"
        }
    }
    
    private fun updateLinkInfo(link: ClipprLink) {
        runOnUiThread {
            binding.tvLinkPath.text = "Path: ${link.path}"
            binding.tvMatchType.text = "Match Type: ${link.matchType.value}"
            binding.tvConfidence.text = "Confidence: ${link.confidence?.let { "%.1f%%".format(it * 100) } ?: "N/A"}"
            
            val attribution = link.attribution
            binding.tvCampaign.text = "Campaign: ${attribution?.campaign ?: "none"}"
            binding.tvSource.text = "Source: ${attribution?.source ?: "none"}"
            binding.tvMedium.text = "Medium: ${attribution?.medium ?: "none"}"
            
            binding.tvMetadata.text = "Metadata: ${link.metadata ?: "none"}"
        }
    }
    
    private fun trackTestEvent() {
        lifecycleScope.launch {
            try {
                Clippr.track("test_event", mapOf(
                    "button" to "test_button",
                    "timestamp" to System.currentTimeMillis()
                ))
                eventsSent++
                updateStatus("Event tracked successfully!")
            } catch (e: Exception) {
                updateStatus("Error: ${e.message}")
            }
        }
    }
    
    private fun trackPurchase() {
        lifecycleScope.launch {
            try {
                Clippr.trackRevenue(
                    eventName = "purchase",
                    revenue = 9.99,
                    currency = "USD",
                    params = mapOf("product_id" to "demo_product")
                )
                eventsSent++
                updateStatus("Purchase tracked!")
            } catch (e: Exception) {
                updateStatus("Error: ${e.message}")
            }
        }
    }
    
    companion object {
        private const val TAG = "ClipprExample"
    }
}
