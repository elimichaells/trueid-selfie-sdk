package com.trueid.sdk.selfie

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class SelfieCaptureTheme(
    val ringActiveColor: Int = 0xFF22C55E.toInt(),
    val ringInactiveColor: Int = 0x3722C55E,
    val ringGlowColor: Int = 0xAA22C55E.toInt(),
    val meshStrokeColor: Int = 0xFF22C55E.toInt(),
    val meshGlowColor: Int = 0x5922C55E,
    val previewDiameterDp: Int = 194,
    val ringWrapperSizeDp: Int = 248,
) : Parcelable
