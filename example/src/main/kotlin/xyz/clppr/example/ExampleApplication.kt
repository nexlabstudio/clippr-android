package xyz.clppr.example

import android.app.Application
import xyz.clppr.sdk.Clippr

class ExampleApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Clippr SDK
        Clippr.initialize(
            context = this,
            apiKey = "clippr_live_your_api_key_here",
            debug = true // Enable for development
        )
    }
}
