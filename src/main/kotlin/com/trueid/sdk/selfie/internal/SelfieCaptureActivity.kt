package com.trueid.sdk.selfie.internal

import android.os.Build
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.trueid.sdk.selfie.SelfieCaptureCallback
import com.trueid.sdk.selfie.SelfieCaptureConfig
import com.trueid.sdk.selfie.SelfieCaptureResult
import com.trueid.sdk.selfie.TrueIDSelfieCapture

internal class SelfieCaptureActivity : AppCompatActivity() {

    companion object {
        @Volatile
        var pendingCallback: SelfieCaptureCallback? = null
    }

    private var config: SelfieCaptureConfig = SelfieCaptureConfig()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(TrueIDSelfieCapture.EXTRA_CONFIG, SelfieCaptureConfig::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(TrueIDSelfieCapture.EXTRA_CONFIG)
        } ?: SelfieCaptureConfig()

        if (savedInstanceState == null) {
            val fragment = SelfieCaptureFragment.newInstance(config)
            supportFragmentManager.beginTransaction()
                .replace(android.R.id.content, fragment)
                .commit()
        }

        supportFragmentManager.setFragmentResultListener(
            SelfieCaptureFragment.RESULT_KEY, this
        ) { _, bundle ->
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bundle.getParcelable(SelfieCaptureFragment.RESULT_DATA, SelfieCaptureResult::class.java)
            } else {
                @Suppress("DEPRECATION")
                bundle.getParcelable(SelfieCaptureFragment.RESULT_DATA)
            }
            if (result != null) {
                val data = android.content.Intent().apply {
                    putExtra(TrueIDSelfieCapture.EXTRA_RESULT, result)
                }
                setResult(RESULT_OK, data)
                pendingCallback?.onCaptured(result)
            } else {
                setResult(RESULT_CANCELED)
                pendingCallback?.onCancelled()
            }
            pendingCallback = null
            finish()
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                setResult(RESULT_CANCELED)
                pendingCallback?.onCancelled()
                pendingCallback = null
                finish()
            }
        })
    }
}
