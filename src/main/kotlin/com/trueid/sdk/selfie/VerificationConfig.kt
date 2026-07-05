package com.trueid.sdk.selfie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Configuration for the end-to-end verification flow.
 *
 * @param captureConfig Selfie capture settings (camera, face alignment, etc.)
 * @param forceNia When true, always queries NIA even if a local match exists
 * @param enforceFaceComparison When true, requires face match even on local lookups
 * @param livenessPassed Optional active challenge outcome supplied by the host app
 * @param transactionType Optional transaction type identifier for your records
 */
@Parcelize
data class VerificationConfig(
    val captureConfig: SelfieCaptureConfig = SelfieCaptureConfig(
        resultFormat = ResultFormat.BASE64
    ),
    val forceNia: Boolean = false,
    val enforceFaceComparison: Boolean = true,
    val livenessPassed: Boolean? = null,
    val transactionType: String? = null,
) : Parcelable
