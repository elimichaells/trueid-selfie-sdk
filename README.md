# TrueID Selfie SDK for Android

A lightweight Android SDK for identity verification via Ghana Card (NIA). Captures a selfie with face detection, submits it alongside a Ghana Card PIN to TrueID, and returns the verification result — all in a single flow.

## Features

- **End-to-end verification** — PIN entry, selfie capture, and NIA verification in one SDK call
- **Standalone selfie capture** — Use just the camera + face detection without verification
- **ML Kit face detection** — Real-time face alignment guidance with visual feedback
- **No heavy dependencies** — Uses `HttpURLConnection` (no OkHttp required)
- **Minimum SDK 24** (Android 7.0+)

## Installation

### Option A — TrueID Maven repository (recommended)

Add the TrueID repository to your project's `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://app.trueid.info/sdk/android") }
    }
}
```

Add the dependency in your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("com.trueid.sdk:trueid-selfie-sdk:2.1.0")
}
```

On-prem institutions: replace `app.trueid.info` with your TrueID server origin.

### Option B — JitPack (legacy)

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

dependencies {
    implementation("com.github.elimichaells:trueid-selfie-sdk:2.0.4")
}
```

## Quick Start

### 1. Initialize the SDK

Call this once, typically in your `Application.onCreate()`:

```kotlin
import com.trueid.sdk.selfie.TrueIDSdk

TrueIDSdk.initialize(
    apiKey = "your-api-key",
    environment = TrueIDSdk.Environment.PRODUCTION
)
```

**Environments:**
| Environment | Base URL |
|---|---|
| `PRODUCTION` | `https://app.trueid.info` |
| `STAGING` | `https://staging.trueid.info` |
| `CUSTOM` | Your own URL (pass `customBaseUrl`) |

### 2. Launch Verification

#### Option A: Activity Result API (Recommended)

```kotlin
import com.trueid.sdk.selfie.TrueIDVerification
import com.trueid.sdk.selfie.VerificationConfig

class MyActivity : AppCompatActivity() {

    private val verify = registerForActivityResult(TrueIDVerification.contract()) { result ->
        if (result == null) {
            // User cancelled
            return@registerForActivityResult
        }
        if (result.isSuccess) {
            Log.d("TrueID", "Verified: ${result.fullName}")
            Log.d("TrueID", "Document: ${result.documentNumber}")
        } else {
            Log.e("TrueID", "Failed: ${result.errorMessage}")
        }
    }

    fun startVerification() {
        verify.launch(VerificationConfig())
    }
}
```

#### Option B: Callback API

```kotlin
import com.trueid.sdk.selfie.TrueIDVerification
import com.trueid.sdk.selfie.VerificationCallback
import com.trueid.sdk.selfie.VerificationConfig
import com.trueid.sdk.selfie.VerificationResult
import com.trueid.sdk.selfie.VerificationError

TrueIDVerification.launch(this, VerificationConfig(), object : VerificationCallback {
    override fun onCompleted(result: VerificationResult) {
        if (result.isSuccess) {
            // Verification successful
        } else {
            // Verification failed: result.errorMessage
        }
    }

    override fun onCancelled() {
        // User pressed back / cancel
    }

    override fun onError(error: VerificationError) {
        when (error) {
            is VerificationError.NetworkError -> { /* No internet / timeout */ }
            is VerificationError.ApiError -> { /* Server error: error.code, error.message */ }
            is VerificationError.CaptureError -> { /* Camera / selfie issue */ }
            is VerificationError.SdkNotInitialized -> { /* Forgot to call TrueIDSdk.initialize() */ }
        }
    }
})
```

### 3. Configuration Options

```kotlin
val config = VerificationConfig(
    forceNia = false,               // Force NIA lookup even if local match exists
    enforceFaceComparison = true,   // Require face match on local lookups
    transactionType = "onboarding", // Optional label for your records
    captureConfig = SelfieCaptureConfig(
        captureMode = CaptureMode.AUTO,       // AUTO or MANUAL
        initialCamera = CameraFacing.FRONT,   // FRONT or BACK
        allowCameraSwitch = true,
        showFaceMesh = true,                  // Show face contour overlay
        outputWidth = 480,
        outputHeight = 640,
        jpegQuality = 92,
    )
)

verify.launch(config)
```

## Standalone Selfie Capture

If you only need the selfie camera (no PIN entry or verification), use `TrueIDSelfieCapture`:

```kotlin
import com.trueid.sdk.selfie.TrueIDSelfieCapture
import com.trueid.sdk.selfie.SelfieCaptureConfig
import com.trueid.sdk.selfie.ResultFormat

private val selfie = registerForActivityResult(TrueIDSelfieCapture.contract()) { result ->
    result?.imageBytes?.let { bytes ->
        // Use the captured selfie image
    }
    result?.base64?.let { b64 ->
        // Base64-encoded image
    }
}

fun captureSelfie() {
    selfie.launch(SelfieCaptureConfig(
        resultFormat = ResultFormat.ALL  // BYTE_ARRAY, FILE_PATH, BASE64, or ALL
    ))
}
```

No API key or initialization required for standalone capture.

## Verification Result

On successful verification, `VerificationResult` contains:

| Field | Type | Description |
|---|---|---|
| `verified` | `Boolean` | Whether the identity was verified |
| `isSuccess` | `Boolean` | `verified && errorMessage == null` |
| `fullName` | `String?` | Full name from the Ghana Card |
| `documentNumber` | `String?` | Ghana Card number |
| `nationality` | `String?` | Nationality |
| `dateOfBirth` | `String?` | Date of birth |
| `gender` | `String?` | Gender |
| `expiryDate` | `String?` | Card expiry date |
| `phoneNumber` | `String?` | Phone number on record |
| `email` | `String?` | Email on record |
| `selfieUrl` | `String?` | URL of the captured selfie |
| `niaPhotoUrl` | `String?` | URL of the NIA photo on file |
| `lookupSource` | `String?` | Where the match was found |
| `scanRecordId` | `String?` | Record ID for this verification |
| `transactionType` | `String?` | Your transaction type label |
| `errorMessage` | `String?` | Error description (if failed) |
| `errorCode` | `String?` | Error code (if failed) |

## Permissions

The SDK declares these permissions in its manifest (merged automatically):

- `android.permission.CAMERA` — Required for selfie capture
- `android.permission.INTERNET` — Required for API calls

You do **not** need to add these to your app's manifest. However, you must handle the runtime camera permission request before launching the SDK.

## Getting an API Key

To obtain an API key for verification, sign up at [app.trueid.info](https://app.trueid.info) or contact the TrueID team.

## Requirements

- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 35
- **Kotlin:** 2.x
- **AndroidX** required

## License

Proprietary. Contact TrueID for licensing terms.
