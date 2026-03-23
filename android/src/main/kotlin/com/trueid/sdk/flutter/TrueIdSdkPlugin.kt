package com.trueid.sdk.flutter

import android.app.Activity
import com.trueid.sdk.selfie.*
import com.trueid.sdk.selfie.internal.VerificationActivity
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts

class TrueIdSdkPlugin : FlutterPlugin, MethodCallHandler, ActivityAware {
    private lateinit var channel: MethodChannel
    private var activity: Activity? = null
    private var pendingResult: Result? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "com.trueid.sdk/flutter")
        channel.setMethodCallHandler(this)
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener { requestCode, resultCode, data ->
            handleActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addActivityResultListener { requestCode, resultCode, data ->
            handleActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        when (call.method) {
            "initialize" -> handleInitialize(call, result)
            "verify" -> handleVerify(call, result)
            "captureSelfie" -> handleCaptureSelfie(call, result)
            else -> result.notImplemented()
        }
    }

    private fun handleInitialize(call: MethodCall, result: Result) {
        val apiKey = call.argument<String>("apiKey")
        if (apiKey.isNullOrBlank()) {
            result.error("INVALID_ARGUMENT", "apiKey is required", null)
            return
        }

        val envName = call.argument<String>("environment") ?: "production"
        val customBaseUrl = call.argument<String>("customBaseUrl")

        val environment = when (envName) {
            "staging" -> TrueIDSdk.Environment.STAGING
            "custom" -> TrueIDSdk.Environment.CUSTOM
            else -> TrueIDSdk.Environment.PRODUCTION
        }

        try {
            TrueIDSdk.initialize(
                apiKey = apiKey,
                environment = environment,
                customBaseUrl = customBaseUrl,
            )
            result.success(null)
        } catch (e: Exception) {
            result.error("INIT_ERROR", e.message, null)
        }
    }

    private fun handleVerify(call: MethodCall, result: Result) {
        val currentActivity = activity
        if (currentActivity == null) {
            result.error("NO_ACTIVITY", "Plugin is not attached to an activity", null)
            return
        }

        if (pendingResult != null) {
            result.error("ALREADY_ACTIVE", "A verification is already in progress", null)
            return
        }

        pendingResult = result

        val config = VerificationConfig(
            forceNia = call.argument<Boolean>("forceNia") ?: false,
            enforceFaceComparison = call.argument<Boolean>("enforceFaceComparison") ?: true,
            transactionType = call.argument<String>("transactionType"),
            captureConfig = SelfieCaptureConfig(
                captureMode = when (call.argument<String>("captureMode")) {
                    "manual" -> CaptureMode.MANUAL
                    else -> CaptureMode.AUTO
                },
                initialCamera = when (call.argument<String>("initialCamera")) {
                    "back" -> CameraFacing.BACK
                    else -> CameraFacing.FRONT
                },
                allowCameraSwitch = call.argument<Boolean>("allowCameraSwitch") ?: true,
                showFaceMesh = call.argument<Boolean>("showFaceMesh") ?: true,
                outputWidth = call.argument<Int>("outputWidth") ?: 480,
                outputHeight = call.argument<Int>("outputHeight") ?: 640,
                jpegQuality = call.argument<Int>("jpegQuality") ?: 92,
                resultFormat = ResultFormat.BASE64,
            ),
        )

        val intent = Intent(currentActivity, VerificationActivity::class.java).apply {
            putExtra(VerificationActivity.EXTRA_VERIFICATION_CONFIG, config)
        }

        VerificationActivity.pendingCallback = object : VerificationCallback {
            override fun onCompleted(verificationResult: VerificationResult) {
                val map = hashMapOf<String, Any?>(
                    "verified" to verificationResult.verified,
                    "lookupSource" to verificationResult.lookupSource,
                    "scanRecordId" to verificationResult.scanRecordId,
                    "fullName" to verificationResult.fullName,
                    "documentNumber" to verificationResult.documentNumber,
                    "nationality" to verificationResult.nationality,
                    "dateOfBirth" to verificationResult.dateOfBirth,
                    "gender" to verificationResult.gender,
                    "expiryDate" to verificationResult.expiryDate,
                    "phoneNumber" to verificationResult.phoneNumber,
                    "email" to verificationResult.email,
                    "selfieUrl" to verificationResult.selfieUrl,
                    "niaPhotoUrl" to verificationResult.niaPhotoUrl,
                    "transactionType" to verificationResult.transactionType,
                    "errorMessage" to verificationResult.errorMessage,
                    "errorCode" to verificationResult.errorCode,
                )
                pendingResult?.success(map)
                pendingResult = null
            }

            override fun onCancelled() {
                pendingResult?.success(null)
                pendingResult = null
            }

            override fun onError(error: VerificationError) {
                val code = when (error) {
                    is VerificationError.SdkNotInitialized -> "SDK_NOT_INITIALIZED"
                    is VerificationError.NetworkError -> "NETWORK_ERROR"
                    is VerificationError.ApiError -> error.code ?: "API_ERROR"
                    is VerificationError.CaptureError -> "CAPTURE_ERROR"
                }
                pendingResult?.error(code, error.message, null)
                pendingResult = null
            }
        }

        currentActivity.startActivity(intent)
    }

    private fun handleCaptureSelfie(call: MethodCall, result: Result) {
        val currentActivity = activity
        if (currentActivity == null) {
            result.error("NO_ACTIVITY", "Plugin is not attached to an activity", null)
            return
        }

        if (pendingResult != null) {
            result.error("ALREADY_ACTIVE", "A capture is already in progress", null)
            return
        }

        pendingResult = result

        val resultFormatStr = call.argument<String>("resultFormat") ?: "base64"
        val resultFormat = when (resultFormatStr) {
            "byteArray" -> ResultFormat.BYTE_ARRAY
            "filePath" -> ResultFormat.FILE_PATH
            "all" -> ResultFormat.ALL
            else -> ResultFormat.BASE64
        }

        val config = SelfieCaptureConfig(
            captureMode = when (call.argument<String>("captureMode")) {
                "manual" -> CaptureMode.MANUAL
                else -> CaptureMode.AUTO
            },
            initialCamera = when (call.argument<String>("initialCamera")) {
                "back" -> CameraFacing.BACK
                else -> CameraFacing.FRONT
            },
            allowCameraSwitch = call.argument<Boolean>("allowCameraSwitch") ?: true,
            showFaceMesh = call.argument<Boolean>("showFaceMesh") ?: true,
            outputWidth = call.argument<Int>("outputWidth") ?: 480,
            outputHeight = call.argument<Int>("outputHeight") ?: 640,
            jpegQuality = call.argument<Int>("jpegQuality") ?: 92,
            resultFormat = resultFormat,
        )

        val callback = object : SelfieCaptureCallback {
            override fun onCaptured(captureResult: SelfieCaptureResult) {
                val map = hashMapOf<String, Any?>(
                    "base64" to captureResult.base64,
                    "filePath" to captureResult.filePath,
                )
                if (captureResult.imageBytes != null) {
                    map["imageBytes"] = captureResult.imageBytes!!.toList()
                }
                pendingResult?.success(map)
                pendingResult = null
            }

            override fun onCancelled() {
                pendingResult?.success(null)
                pendingResult = null
            }

            override fun onError(error: SelfieCaptureError) {
                pendingResult?.error("CAPTURE_ERROR", error.message, null)
                pendingResult = null
            }
        }

        if (currentActivity is ComponentActivity) {
            TrueIDSelfieCapture.launch(currentActivity, config, callback)
        } else {
            result.error("INCOMPATIBLE_ACTIVITY", "Activity must be a ComponentActivity", null)
            pendingResult = null
        }
    }

    @Suppress("DEPRECATION")
    private fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        // The VerificationActivity uses callbacks, not activity results for the Flutter bridge,
        // so we don't need to handle requestCode here. The callback handles everything.
        return false
    }
}
