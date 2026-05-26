package xyz.useclippr.sdk

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.useclippr.sdk.internal.*
import xyz.useclippr.sdk.models.ClipprConfig
import xyz.useclippr.sdk.models.ClipprLink
import xyz.useclippr.sdk.models.LinkParameters
import xyz.useclippr.sdk.models.MatchType
import xyz.useclippr.sdk.models.ShortLink
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * Clippr SDK for deep linking and attribution
 */
object Clippr {
    
    // Internal components
    private var config: ClipprConfig? = null
    private var apiClient: APIClient? = null
    private var storage: Storage? = null
    private var deviceInfo: DeviceInfo? = null
    private var installReferrerHelper: InstallReferrerHelper? = null
    
    // Coroutine scope for background work
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    // State
    private var _isInitialized = false
    private var pendingInitialLink: ClipprLink? = null
    private var initialLinkRetrieved = false
    private var deferredLinkResult: ClipprLink? = null
    private var deferredLinkChecked = false
    private val deferredLinkMutex = Mutex()
    
    /**
     * Whether the SDK has been initialized
     */
    val isInitialized: Boolean
        get() = _isInitialized
    
    /**
     * Callback for receiving deep links while app is running
     */
    var onLink: ((ClipprLink) -> Unit)? = null
    
    /**
     * Initialize the Clippr SDK
     *
     * @param context Application context
     * @param apiKey Your Clippr API key
     * @param debug Enable debug logging (default: false)
     */
    @JvmStatic
    @JvmOverloads
    fun initialize(
        context: Context,
        apiKey: String,
        debug: Boolean = false
    ) {
        initialize(context, ClipprConfig(apiKey = apiKey, debug = debug))
    }
    
    /**
     * Initialize the Clippr SDK with a configuration
     *
     * @param context Application context
     * @param config SDK configuration
     */
    @JvmStatic
    fun initialize(context: Context, config: ClipprConfig) {
        val appContext = context.applicationContext
        
        this.config = config
        this.storage = Storage(appContext)
        this.deviceInfo = DeviceInfo(appContext, storage!!)
        this.apiClient = APIClient(config)
        this.installReferrerHelper = InstallReferrerHelper(appContext)
        this._isInitialized = true
        
        Logger.isEnabled = config.debug
        Logger.info("Clippr SDK initialized")

        startDeferredLinkCheck()
    }
    
    /**
     * Get the link that opened the app.
     * This handles both direct App Links AND deferred deep links.
     *
     * @return The ClipprLink if one exists, null otherwise
     */
    @JvmStatic
    suspend fun getInitialLink(): ClipprLink? {
        if (!_isInitialized) {
            Logger.error("SDK not initialized")
            return null
        }
        
        initialLinkRetrieved = true
        
        // If we have a direct link from App Link, return it immediately
        pendingInitialLink?.let { link ->
            Logger.debug("Returning direct link: ${link.path}")
            pendingInitialLink = null
            return link
        }
        
        // Otherwise, wait for deferred link check to complete
        Logger.debug("Waiting for deferred link check...")
        deferredLinkMutex.withLock {
            // Check is already done, return cached result
            if (deferredLinkChecked) {
                return deferredLinkResult
            }
        }
        
        // Wait for the check to complete
        return suspendCoroutine { continuation ->
            scope.launch {
                deferredLinkMutex.withLock {
                    // Wait until check is done
                    while (!deferredLinkChecked) {
                        kotlinx.coroutines.delay(50)
                    }
                    continuation.resume(deferredLinkResult)
                }
            }
        }
    }
    
    /**
     * Get the initial link with a callback (for Java interop)
     */
    @JvmStatic
    fun getInitialLink(callback: (ClipprLink?) -> Unit) {
        scope.launch {
            val link = getInitialLink()
            callback(link)
        }
    }
    
    /**
     * Track a custom event
     *
     * @param eventName Name of the event (e.g., "purchase", "signup")
     * @param params Optional parameters to attach to the event
     */
    @JvmStatic
    @JvmOverloads
    suspend fun track(eventName: String, params: Map<String, Any?>? = null) {
        val client = apiClient ?: throw ClipprException.NotInitialized()
        val device = deviceInfo ?: throw ClipprException.NotInitialized()
        
        client.trackEvent(
            deviceId = device.deviceId,
            eventName = eventName,
            params = params,
            revenue = null,
            currency = null
        )
    }
    
    /**
     * Track a revenue event
     *
     * @param eventName Name of the event
     * @param revenue Revenue amount
     * @param currency Currency code (e.g., "USD")
     * @param params Optional additional parameters
     */
    @JvmStatic
    @JvmOverloads
    suspend fun trackRevenue(
        eventName: String,
        revenue: Double,
        currency: String,
        params: Map<String, Any?>? = null
    ) {
        val client = apiClient ?: throw ClipprException.NotInitialized()
        val device = deviceInfo ?: throw ClipprException.NotInitialized()
        
        client.trackEvent(
            deviceId = device.deviceId,
            eventName = eventName,
            params = params,
            revenue = revenue,
            currency = currency
        )
    }
    
    /**
     * Track an event with a callback (for Java interop)
     */
    @JvmStatic
    @JvmOverloads
    fun track(eventName: String, params: Map<String, Any?>? = null, callback: ((Exception?) -> Unit)? = null) {
        scope.launch {
            try {
                track(eventName, params)
                callback?.invoke(null)
            } catch (e: Exception) {
                callback?.invoke(e)
            }
        }
    }

    /**
     * Create a short link for sharing
     *
     * @param parameters Link parameters including path, metadata, and social tags
     * @return The created short link
     * @throws ClipprException if creation fails
     */
    @JvmStatic
    suspend fun createLink(parameters: LinkParameters): ShortLink {
        val client = apiClient ?: throw ClipprException.NotInitialized()
        return client.createLink(parameters)
    }

    /**
     * Create a short link with a callback (for Java interop)
     */
    @JvmStatic
    fun createLink(parameters: LinkParameters, callback: (ShortLink?, Exception?) -> Unit) {
        scope.launch {
            try {
                val link = createLink(parameters)
                callback(link, null)
            } catch (e: Exception) {
                callback(null, e)
            }
        }
    }

    /**
     * Handle an App Link intent.
     * Call this from your Activity's onCreate or onNewIntent.
     *
     * @param intent The intent that opened the activity
     * @return true if Clippr handled the intent, false otherwise
     */
    @JvmStatic
    fun handle(intent: Intent?): Boolean {
        if (!_isInitialized) {
            Logger.error("SDK not initialized")
            return false
        }
        
        val uri = intent?.data ?: return false
        
        Logger.debug("Handling intent: $uri")
        
        // Parse the URI to extract path
        val link = parseAppLink(uri) ?: run {
            Logger.debug("URI not a Clippr link")
            return false
        }
        
        // If getInitialLink hasn't been called yet, store as pending
        if (!initialLinkRetrieved) {
            Logger.debug("Storing as initial link")
            pendingInitialLink = link
        } else {
            // Otherwise, deliver via onLink callback
            Logger.debug("Delivering via onLink callback")
            onLink?.invoke(link)
        }
        
        return true
    }
    
    private fun startDeferredLinkCheck() {
        val storage = this.storage ?: return
        
        // Only check once per install
        if (storage.hasCheckedDeferredLink) {
            Logger.debug("Already checked for deferred link this install")
            scope.launch {
                deferredLinkMutex.withLock {
                    deferredLinkChecked = true
                    deferredLinkResult = null
                }
            }
            return
        }
        
        scope.launch {
            val result = checkForDeferredLink()
            deferredLinkMutex.withLock {
                deferredLinkResult = result
                deferredLinkChecked = true
            }
        }
    }
    
    private suspend fun checkForDeferredLink(): ClipprLink? {
        val client = apiClient ?: return null
        val device = deviceInfo ?: return null
        val storage = this.storage ?: return null
        val referrerHelper = installReferrerHelper ?: return null
        
        Logger.debug("Checking for deferred deep link...")

        try {
            val referrerResult = coroutineScope {
                val gaidJob = async { device.fetchAdvertisingId() }
                val referrerDeferred = async { referrerHelper.getInstallReferrer() }
                val result = referrerDeferred.await()
                gaidJob.await()
                result
            }
            val referrer = referrerResult?.referrer

            if (referrer != null) {
                storage.installReferrer = referrer
            }

            val payload = device.buildMatchPayload(installReferrer = referrer)
            
            val match = client.match(payload) ?: run {
                Logger.debug("No deferred link found")
                storage.hasCheckedDeferredLink = true
                return null
            }
            
            Logger.debug("Deferred link found: ${match.deepLinkPath}")
            
            // Don't deliver the same link twice
            if (match.deepLinkPath == storage.lastDeferredLinkPath) {
                Logger.debug("Same link as last time, skipping")
                return null
            }
            
            storage.hasCheckedDeferredLink = true
            storage.lastDeferredLinkPath = match.deepLinkPath
            
            // Track the install
            try {
                client.trackInstall(device.buildInstallPayload())
            } catch (e: Exception) {
                Logger.error("Failed to track install", e)
            }
            
            return ClipprLink(
                path = match.deepLinkPath,
                metadata = match.metadata,
                attribution = match.attribution,
                matchType = match.matchType,
                confidence = match.confidence
            )
        } catch (e: Exception) {
            Logger.error("Failed to check deferred link", e)
            return null
        }
    }
    
    private fun parseAppLink(uri: Uri): ClipprLink? {
        // Extract path from URI
        var path = uri.path ?: return null
        
        // If path is empty or just "/", this might not be a valid deep link
        if (path.isEmpty() || path == "/") {
            return null
        }
        
        // Parse query parameters as metadata
        val metadata: Map<String, Any?>? = uri.queryParameterNames.takeIf { it.isNotEmpty() }?.let { params ->
            params.associateWith { uri.getQueryParameter(it) }
        }
        
        return ClipprLink(
            path = path,
            metadata = metadata,
            attribution = null,
            matchType = MatchType.DIRECT,
            confidence = 1.0
        )
    }
    
    /**
     * Reset the SDK state (for testing only)
     */
    internal fun reset() {
        config = null
        apiClient = null
        storage?.clear()
        storage = null
        deviceInfo = null
        installReferrerHelper = null
        _isInitialized = false
        pendingInitialLink = null
        initialLinkRetrieved = false
        deferredLinkResult = null
        deferredLinkChecked = false
        onLink = null
    }
}