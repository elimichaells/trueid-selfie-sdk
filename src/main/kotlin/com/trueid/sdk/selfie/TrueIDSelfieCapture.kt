package com.trueid.sdk.selfie

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContract
import androidx.fragment.app.Fragment
import com.trueid.sdk.selfie.internal.SelfieCaptureActivity
import com.trueid.sdk.selfie.internal.SelfieCaptureFragment

object TrueIDSelfieCapture {

    internal const val EXTRA_CONFIG = "trueid_selfie_config"
    internal const val EXTRA_RESULT = "trueid_selfie_result"

    /**
     * Returns an ActivityResultContract for use with registerForActivityResult().
     *
     * Usage:
     * ```
     * private val selfie = registerForActivityResult(TrueIDSelfieCapture.contract()) { result ->
     *     result?.imageBytes?.let { uploadSelfie(it) }
     * }
     *
     * // Launch:
     * selfie.launch(SelfieCaptureConfig())
     * ```
     */
    fun contract(): ActivityResultContract<SelfieCaptureConfig, SelfieCaptureResult?> {
        return SelfieCaptureContract()
    }

    /**
     * Launch selfie capture with a callback (alternative to Activity Result API).
     */
    fun launch(
        activity: ComponentActivity,
        config: SelfieCaptureConfig = SelfieCaptureConfig(),
        callback: SelfieCaptureCallback,
    ) {
        SelfieCaptureActivity.pendingCallback = callback
        val intent = Intent(activity, SelfieCaptureActivity::class.java).apply {
            putExtra(EXTRA_CONFIG, config)
        }
        activity.startActivity(intent)
    }

    /**
     * Create a Fragment for embedding in your own layout.
     * Listen for results via Fragment Result API with key [SelfieCaptureFragment.RESULT_KEY].
     */
    fun createFragment(
        config: SelfieCaptureConfig = SelfieCaptureConfig(),
    ): Fragment {
        return SelfieCaptureFragment.newInstance(config)
    }

    private class SelfieCaptureContract :
        ActivityResultContract<SelfieCaptureConfig, SelfieCaptureResult?>() {

        override fun createIntent(context: Context, input: SelfieCaptureConfig): Intent {
            return Intent(context, SelfieCaptureActivity::class.java).apply {
                putExtra(EXTRA_CONFIG, input)
            }
        }

        override fun parseResult(resultCode: Int, intent: Intent?): SelfieCaptureResult? {
            if (resultCode != Activity.RESULT_OK) return null
            return intent?.getParcelableExtra(EXTRA_RESULT)
        }
    }
}
