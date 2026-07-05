package com.trueid.sdk.selfie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Result of an end-to-end identity verification.
 */
@Parcelize
data class VerificationResult(
    val verified: Boolean,
    val lookupSource: String?,
    val scanRecordId: String?,
    val fullName: String?,
    val documentNumber: String?,
    val nationality: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val expiryDate: String?,
    val phoneNumber: String?,
    val email: String?,
    val selfieUrl: String?,
    val niaPhotoUrl: String?,
    val transactionType: String?,
    val errorMessage: String?,
    val errorCode: String?,
) : Parcelable {

    val isSuccess: Boolean get() = verified && errorMessage == null
}

/**
 * Callback interface for the verification flow.
 */
interface VerificationCallback {
    /** Verification completed (check [result.verified] for the match outcome). */
    fun onCompleted(result: VerificationResult)

    /** User cancelled the flow. */
    fun onCancelled()

    /** An error occurred before or during verification. */
    fun onError(error: VerificationError)
}

sealed class VerificationError(override val message: String) : Throwable(message) {
    class SdkNotInitialized : VerificationError("TrueIDSdk.initialize() must be called first")
    class NetworkError(message: String) : VerificationError(message)
    class ApiError(val code: String?, message: String) : VerificationError(message)
    class CaptureError(message: String) : VerificationError(message)
}
