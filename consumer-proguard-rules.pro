# TrueID Selfie SDK - ProGuard rules for consumers

# Keep public API
-keep class com.trueid.sdk.selfie.TrueIDSelfieCapture { *; }
-keep class com.trueid.sdk.selfie.SelfieCaptureConfig { *; }
-keep class com.trueid.sdk.selfie.SelfieCaptureConfig$* { *; }
-keep class com.trueid.sdk.selfie.SelfieCaptureResult { *; }
-keep class com.trueid.sdk.selfie.SelfieCaptureCallback { *; }
-keep class com.trueid.sdk.selfie.SelfieCaptureTheme { *; }
-keep class com.trueid.sdk.selfie.CaptureMode { *; }
-keep class com.trueid.sdk.selfie.CameraFacing { *; }
-keep class com.trueid.sdk.selfie.ResultFormat { *; }
-keep class com.trueid.sdk.selfie.SelfieCaptureError { *; }
-keep class com.trueid.sdk.selfie.SelfieCaptureError$* { *; }

# Keep Verification API
-keep class com.trueid.sdk.selfie.TrueIDSdk { *; }
-keep class com.trueid.sdk.selfie.TrueIDSdk$* { *; }
-keep class com.trueid.sdk.selfie.TrueIDVerification { *; }
-keep class com.trueid.sdk.selfie.VerificationConfig { *; }
-keep class com.trueid.sdk.selfie.VerificationResult { *; }
-keep class com.trueid.sdk.selfie.VerificationCallback { *; }
-keep class com.trueid.sdk.selfie.VerificationError { *; }
-keep class com.trueid.sdk.selfie.VerificationError$* { *; }

# Keep ML Kit
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**
