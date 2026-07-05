package com.trueid.sdk.selfie

/**
 * Main initialization point for the TrueID SDK.
 *
 * Call [initialize] once (e.g. in Application.onCreate) before using
 * [TrueIDVerification].
 *
 * ```kotlin
 * TrueIDSdk.initialize(
 *     apiKey = "your-api-key",
 *     environment = TrueIDSdk.Environment.PRODUCTION
 * )
 * ```
 */
object TrueIDSdk {

    enum class Environment(val baseUrl: String) {
        PRODUCTION("https://app.trueid.info"),
        STAGING("https://staging.trueid.info"),
        /** For local development — set a custom base URL via [initialize]. */
        CUSTOM("")
    }

    internal var apiKey: String = ""
        private set

    internal var baseUrl: String = Environment.PRODUCTION.baseUrl
        private set

    internal var isInitialized: Boolean = false
        private set

    /**
     * Initialize the SDK with your organization API key.
     *
     * @param apiKey Your TrueID API key (from owner.trueid.info or app.trueid.info)
     * @param environment The target environment (defaults to PRODUCTION)
     * @param customBaseUrl Only used when environment is [Environment.CUSTOM]
     */
    fun initialize(
        apiKey: String,
        environment: Environment = Environment.PRODUCTION,
        customBaseUrl: String? = null,
    ) {
        require(apiKey.isNotBlank()) { "TrueID API key must not be blank" }

        this.apiKey = apiKey
        this.baseUrl = when (environment) {
            Environment.CUSTOM -> {
                requireNotNull(customBaseUrl) { "customBaseUrl is required when using Environment.CUSTOM" }
                customBaseUrl.trimEnd('/')
            }
            else -> environment.baseUrl
        }
        this.isInitialized = true
    }

    internal fun ensureInitialized() {
        check(isInitialized) {
            "TrueIDSdk.initialize() must be called before using verification features"
        }
    }
}
