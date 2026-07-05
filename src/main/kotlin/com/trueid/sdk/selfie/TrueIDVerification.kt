package com.trueid.sdk.selfie

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import com.trueid.sdk.selfie.internal.VerificationActivity

/**
 * Entry point for end-to-end identity verification.
 *
 * **Setup** — call [TrueIDSdk.initialize] once before using:
 * ```kotlin
 * TrueIDSdk.initialize(
 *     apiKey = "your-api-key",
 *     environment = TrueIDSdk.Environment.PRODUCTION
 * )
 * ```
 *
 * **Option 1 — Activity Result API:**
 * ```kotlin
 * private val verify = registerForActivityResult(TrueIDVerification.contract()) { result ->
 *     if (result != null && result.verified) {
 *         // success
 *     }
 * }
 *
 * verify.launch(VerificationConfig())
 * ```
 *
 * **Option 2 — Callback:**
 * ```kotlin
 * TrueIDVerification.launch(this, VerificationConfig(), object : VerificationCallback {
 *     override fun onCompleted(result: VerificationResult) { }
 *     override fun onCancelled() { }
 *     override fun onError(error: VerificationError) { }
 * })
 * ```
 */
object TrueIDVerification {

    /**
     * Returns an [ActivityResultContract] for use with `registerForActivityResult()`.
     */
    fun contract(): ActivityResultContract<VerificationConfig, VerificationResult?> {
        return VerificationContract()
    }

    /**
     * Launch the verification flow with a callback (alternative to Activity Result API).
     */
    fun launch(
        activity: ComponentActivity,
        config: VerificationConfig = VerificationConfig(),
        callback: VerificationCallback,
    ) {
        TrueIDSdk.ensureInitialized()
        VerificationActivity.pendingCallback = callback
        val intent = Intent(activity, VerificationActivity::class.java).apply {
            putExtra(VerificationActivity.EXTRA_VERIFICATION_CONFIG, config)
        }
        activity.startActivity(intent)
    }

    private class VerificationContract :
        ActivityResultContract<VerificationConfig, VerificationResult?>() {

        override fun createIntent(context: Context, input: VerificationConfig): Intent {
            TrueIDSdk.ensureInitialized()
            return Intent(context, VerificationActivity::class.java).apply {
                putExtra(VerificationActivity.EXTRA_VERIFICATION_CONFIG, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): VerificationResult? {
            if (resultCode != Activity.RESULT_OK) return null
            return intent?.getParcelableExtra(VerificationActivity.EXTRA_VERIFICATION_RESULT)
        }
    }
}
