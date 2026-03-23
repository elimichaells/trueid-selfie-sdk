import 'package:flutter/services.dart';
import 'models.dart';

/// Main entry point for the TrueID SDK.
///
/// Call [initialize] once before using [verify] or [captureSelfie].
///
/// ```dart
/// TrueIdSdk.initialize(apiKey: 'your-api-key');
///
/// final result = await TrueIdSdk.verify();
/// ```
class TrueIdSdk {
  static const MethodChannel _channel = MethodChannel('com.trueid.sdk/flutter');

  TrueIdSdk._();

  /// Initialize the SDK with your API key.
  ///
  /// Must be called before [verify]. Not required for [captureSelfie].
  ///
  /// [apiKey] — Your TrueID API key from app.trueid.info.
  /// [environment] — Target environment (defaults to production).
  /// [customBaseUrl] — Required when environment is [TrueIdEnvironment.custom].
  static Future<void> initialize({
    required String apiKey,
    TrueIdEnvironment environment = TrueIdEnvironment.production,
    String? customBaseUrl,
  }) async {
    await _channel.invokeMethod('initialize', {
      'apiKey': apiKey,
      'environment': environment.name,
      'customBaseUrl': customBaseUrl,
    });
  }

  /// Launch the full verification flow (PIN → selfie → NIA verification).
  ///
  /// Returns a [VerificationResult] on completion.
  /// Throws [TrueIdException] on error, or returns `null` if the user cancelled.
  ///
  /// ```dart
  /// final result = await TrueIdSdk.verify(
  ///   config: VerificationConfig(
  ///     forceNia: false,
  ///     enforceFaceComparison: true,
  ///   ),
  /// );
  /// ```
  static Future<VerificationResult?> verify({
    VerificationConfig config = const VerificationConfig(),
  }) async {
    try {
      final result = await _channel.invokeMethod('verify', config.toMap());
      if (result == null) return null;
      return VerificationResult.fromMap(Map<dynamic, dynamic>.from(result));
    } on PlatformException catch (e) {
      throw TrueIdException(
        code: e.code,
        message: e.message ?? 'Verification failed',
      );
    }
  }

  /// Launch standalone selfie capture (no verification).
  ///
  /// Does not require [initialize] to be called first.
  ///
  /// Returns a [SelfieCaptureResult] on success, or `null` if cancelled.
  static Future<SelfieCaptureResult?> captureSelfie({
    SelfieCaptureConfig config = const SelfieCaptureConfig(),
  }) async {
    try {
      final result =
          await _channel.invokeMethod('captureSelfie', config.toMap());
      if (result == null) return null;
      return SelfieCaptureResult.fromMap(Map<dynamic, dynamic>.from(result));
    } on PlatformException catch (e) {
      throw TrueIdException(
        code: e.code,
        message: e.message ?? 'Capture failed',
      );
    }
  }
}
