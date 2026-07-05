package com.trueid.sdk.selfie.internal

import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputFilter
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.trueid.sdk.selfie.ResultFormat
import com.trueid.sdk.selfie.SelfieCaptureResult
import com.trueid.sdk.selfie.VerificationCallback
import com.trueid.sdk.selfie.VerificationConfig
import com.trueid.sdk.selfie.VerificationError
import com.trueid.sdk.selfie.VerificationResult
import kotlinx.coroutines.launch

/**
 * Full-screen activity that orchestrates the verification flow:
 * 1. PIN entry
 * 2. Selfie capture (delegates to SelfieCaptureFragment)
 * 3. API call to TrueID
 * 4. Returns VerificationResult
 */
internal class VerificationActivity : AppCompatActivity() {

    companion object {
        internal const val EXTRA_VERIFICATION_CONFIG = "trueid_verification_config"
        internal const val EXTRA_VERIFICATION_RESULT = "trueid_verification_result"

        @Volatile
        var pendingCallback: VerificationCallback? = null
    }

    private lateinit var config: VerificationConfig
    private var pinNumber: String = ""
    private var currentStep = Step.PIN_ENTRY

    private enum class Step { PIN_ENTRY, SELFIE_CAPTURE, VERIFYING }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        config = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(EXTRA_VERIFICATION_CONFIG, VerificationConfig::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(EXTRA_VERIFICATION_CONFIG)
        } ?: VerificationConfig()

        if (savedInstanceState == null) {
            showPinEntry()
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
                onSelfieCaptured(result)
            } else {
                showPinEntry()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentStep) {
                    Step.SELFIE_CAPTURE -> showPinEntry()
                    Step.VERIFYING -> Unit
                    Step.PIN_ENTRY -> {
                        setResult(RESULT_CANCELED)
                        pendingCallback?.onCancelled()
                        pendingCallback = null
                        finish()
                    }
                }
            }
        })
    }

    private fun showPinEntry() {
        currentStep = Step.PIN_ENTRY

        val dp = { value: Int ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
        }

        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#0F172A"))
            isFillViewport = true
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(dp(32), dp(48), dp(32), dp(32))
        }

        val iconText = TextView(this).apply {
            text = "\uD83C\uDDEC\uD83C\uDDED"
            textSize = 48f
            gravity = Gravity.CENTER
        }
        container.addView(iconText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(16) })

        val title = TextView(this).apply {
            text = "Identity Verification"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        container.addView(title, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        val subtitle = TextView(this).apply {
            text = "Enter your Ghana Card PIN to begin verification"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        container.addView(subtitle, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(32) })

        val pinInput = EditText(this).apply {
            hint = "GHA-XXXXXXXXX-X"
            setHintTextColor(Color.parseColor("#64748B"))
            setTextColor(Color.WHITE)
            textSize = 18f
            gravity = Gravity.CENTER
            typeface = Typeface.MONOSPACE
            setBackgroundColor(Color.parseColor("#1E293B"))
            setPadding(dp(16), dp(16), dp(16), dp(16))
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS
            filters = arrayOf(InputFilter.LengthFilter(20))
            imeOptions = EditorInfo.IME_ACTION_DONE
            if (pinNumber.isNotEmpty()) setText(pinNumber)
        }
        container.addView(pinInput, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        val errorText = TextView(this).apply {
            setTextColor(Color.parseColor("#EF4444"))
            textSize = 13f
            gravity = Gravity.CENTER
            visibility = View.GONE
        }
        container.addView(errorText, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(24) })

        val continueBtn = Button(this).apply {
            text = "Continue"
            setTextColor(Color.WHITE)
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setBackgroundColor(Color.parseColor("#1A6DAB"))
            setPadding(dp(16), dp(14), dp(16), dp(14))
            isAllCaps = false
        }
        container.addView(continueBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(16) })

        val cancelBtn = TextView(this).apply {
            text = "Cancel"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(dp(16), dp(8), dp(16), dp(8))
        }
        container.addView(cancelBtn, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ))

        val onContinue = {
            val pin = pinInput.text.toString().trim()
            if (pin.isEmpty()) {
                errorText.text = "Please enter your Ghana Card PIN"
                errorText.visibility = View.VISIBLE
            } else {
                errorText.visibility = View.GONE
                pinNumber = pin
                val imm = getSystemService(INPUT_METHOD_SERVICE) as? InputMethodManager
                imm?.hideSoftInputFromWindow(pinInput.windowToken, 0)
                showSelfieCapture()
            }
        }

        continueBtn.setOnClickListener { onContinue() }
        pinInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onContinue()
                true
            } else false
        }

        cancelBtn.setOnClickListener {
            setResult(RESULT_CANCELED)
            pendingCallback?.onCancelled()
            pendingCallback = null
            finish()
        }

        root.addView(container)
        setContentView(root)
    }

    private fun showSelfieCapture() {
        currentStep = Step.SELFIE_CAPTURE

        val captureConfig = config.captureConfig.copy(
            resultFormat = ResultFormat.BASE64
        )

        val fragment = SelfieCaptureFragment.newInstance(captureConfig)
        supportFragmentManager.beginTransaction()
            .replace(android.R.id.content, fragment)
            .commit()
    }

    private fun onSelfieCaptured(captureResult: SelfieCaptureResult) {
        val selfieBase64 = captureResult.base64
        if (selfieBase64.isNullOrEmpty()) {
            pendingCallback?.onError(VerificationError.CaptureError("Failed to capture selfie image"))
            pendingCallback = null
            finish()
            return
        }

        showVerifyingScreen()

        lifecycleScope.launch {
            try {
                val result = TrueIDApiClient.verifySelfie(
                    pinNumber = pinNumber,
                    selfieBase64 = selfieBase64,
                    burstFrames = captureResult.burstFrames.orEmpty(),
                    forceNia = config.forceNia,
                    enforceFaceComparison = config.enforceFaceComparison,
                    livenessPassed = config.livenessPassed,
                    transactionType = config.transactionType,
                )

                val data = android.content.Intent().apply {
                    putExtra(EXTRA_VERIFICATION_RESULT, result)
                }
                setResult(RESULT_OK, data)
                pendingCallback?.onCompleted(result)
            } catch (e: VerificationError.ApiError) {
                val failResult = VerificationResult(
                    verified = false,
                    lookupSource = null,
                    scanRecordId = null,
                    fullName = null,
                    documentNumber = null,
                    nationality = null,
                    dateOfBirth = null,
                    gender = null,
                    expiryDate = null,
                    phoneNumber = null,
                    email = null,
                    selfieUrl = null,
                    niaPhotoUrl = null,
                    transactionType = null,
                    errorMessage = e.message,
                    errorCode = e.code,
                )
                val data = android.content.Intent().apply {
                    putExtra(EXTRA_VERIFICATION_RESULT, failResult)
                }
                setResult(RESULT_OK, data)
                pendingCallback?.onCompleted(failResult)
            } catch (e: VerificationError) {
                pendingCallback?.onError(e)
            } catch (e: Exception) {
                pendingCallback?.onError(VerificationError.NetworkError(e.message ?: "Verification failed"))
            } finally {
                pendingCallback = null
                finish()
            }
        }
    }

    private fun showVerifyingScreen() {
        currentStep = Step.VERIFYING

        val dp = { value: Int ->
            TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value.toFloat(), resources.displayMetrics).toInt()
        }

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#0F172A"))
            setPadding(dp(32), dp(48), dp(32), dp(32))
        }

        val progress = ProgressBar(this).apply {
            isIndeterminate = true
        }
        container.addView(progress, LinearLayout.LayoutParams(
            dp(64), dp(64)
        ).apply { bottomMargin = dp(24); gravity = Gravity.CENTER_HORIZONTAL })

        val label = TextView(this).apply {
            text = "Verifying identity..."
            setTextColor(Color.WHITE)
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        container.addView(label, LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { bottomMargin = dp(8) })

        val sublabel = TextView(this).apply {
            text = "This may take a few seconds"
            setTextColor(Color.parseColor("#94A3B8"))
            textSize = 14f
            gravity = Gravity.CENTER
        }
        container.addView(sublabel)

        setContentView(container)
    }
}
