/// Environment for the TrueID SDK.
enum TrueIdEnvironment {
  production,
  staging,
  custom,
}

/// Capture mode for the selfie camera.
enum CaptureMode {
  auto,
  manual,
}

/// Camera facing direction.
enum CameraFacing {
  front,
  back,
}

/// Result format for standalone selfie capture.
enum ResultFormat {
  byteArray,
  filePath,
  base64,
  all,
}

/// Configuration for the verification flow.
class VerificationConfig {
  /// Force NIA lookup even if a local match exists.
  final bool forceNia;

  /// Require face match on local lookups.
  final bool enforceFaceComparison;

  /// Optional transaction type label for your records.
  final String? transactionType;

  /// Selfie capture settings.
  final SelfieCaptureConfig captureConfig;

  const VerificationConfig({
    this.forceNia = false,
    this.enforceFaceComparison = true,
    this.transactionType,
    this.captureConfig = const SelfieCaptureConfig(),
  });

  Map<String, dynamic> toMap() => {
        'forceNia': forceNia,
        'enforceFaceComparison': enforceFaceComparison,
        'transactionType': transactionType,
        'captureMode': captureConfig.captureMode.name,
        'initialCamera': captureConfig.initialCamera.name,
        'allowCameraSwitch': captureConfig.allowCameraSwitch,
        'showFaceMesh': captureConfig.showFaceMesh,
        'outputWidth': captureConfig.outputWidth,
        'outputHeight': captureConfig.outputHeight,
        'jpegQuality': captureConfig.jpegQuality,
      };
}

/// Configuration for the selfie camera.
class SelfieCaptureConfig {
  final CaptureMode captureMode;
  final CameraFacing initialCamera;
  final bool allowCameraSwitch;
  final bool showFaceMesh;
  final int outputWidth;
  final int outputHeight;
  final int jpegQuality;
  final ResultFormat resultFormat;

  const SelfieCaptureConfig({
    this.captureMode = CaptureMode.auto,
    this.initialCamera = CameraFacing.front,
    this.allowCameraSwitch = true,
    this.showFaceMesh = true,
    this.outputWidth = 480,
    this.outputHeight = 640,
    this.jpegQuality = 92,
    this.resultFormat = ResultFormat.base64,
  });

  Map<String, dynamic> toMap() => {
        'captureMode': captureMode.name,
        'initialCamera': initialCamera.name,
        'allowCameraSwitch': allowCameraSwitch,
        'showFaceMesh': showFaceMesh,
        'outputWidth': outputWidth,
        'outputHeight': outputHeight,
        'jpegQuality': jpegQuality,
        'resultFormat': resultFormat.name,
      };
}

/// Result of an identity verification.
class VerificationResult {
  final bool verified;
  final String? lookupSource;
  final String? scanRecordId;
  final String? fullName;
  final String? documentNumber;
  final String? nationality;
  final String? dateOfBirth;
  final String? gender;
  final String? expiryDate;
  final String? phoneNumber;
  final String? email;
  final String? selfieUrl;
  final String? niaPhotoUrl;
  final String? transactionType;
  final String? errorMessage;
  final String? errorCode;

  /// True when the identity was verified and no error occurred.
  bool get isSuccess => verified && errorMessage == null;

  const VerificationResult({
    required this.verified,
    this.lookupSource,
    this.scanRecordId,
    this.fullName,
    this.documentNumber,
    this.nationality,
    this.dateOfBirth,
    this.gender,
    this.expiryDate,
    this.phoneNumber,
    this.email,
    this.selfieUrl,
    this.niaPhotoUrl,
    this.transactionType,
    this.errorMessage,
    this.errorCode,
  });

  factory VerificationResult.fromMap(Map<dynamic, dynamic> map) {
    return VerificationResult(
      verified: map['verified'] as bool? ?? false,
      lookupSource: map['lookupSource'] as String?,
      scanRecordId: map['scanRecordId'] as String?,
      fullName: map['fullName'] as String?,
      documentNumber: map['documentNumber'] as String?,
      nationality: map['nationality'] as String?,
      dateOfBirth: map['dateOfBirth'] as String?,
      gender: map['gender'] as String?,
      expiryDate: map['expiryDate'] as String?,
      phoneNumber: map['phoneNumber'] as String?,
      email: map['email'] as String?,
      selfieUrl: map['selfieUrl'] as String?,
      niaPhotoUrl: map['niaPhotoUrl'] as String?,
      transactionType: map['transactionType'] as String?,
      errorMessage: map['errorMessage'] as String?,
      errorCode: map['errorCode'] as String?,
    );
  }

  @override
  String toString() =>
      'VerificationResult(verified: $verified, fullName: $fullName, documentNumber: $documentNumber)';
}

/// Result of a standalone selfie capture.
class SelfieCaptureResult {
  /// Raw image bytes (when resultFormat includes BYTE_ARRAY or ALL).
  final List<int>? imageBytes;

  /// Base64-encoded image (when resultFormat includes BASE64 or ALL).
  final String? base64;

  /// File path to saved image (when resultFormat includes FILE_PATH or ALL).
  final String? filePath;

  const SelfieCaptureResult({
    this.imageBytes,
    this.base64,
    this.filePath,
  });

  factory SelfieCaptureResult.fromMap(Map<dynamic, dynamic> map) {
    return SelfieCaptureResult(
      imageBytes: (map['imageBytes'] as List<dynamic>?)?.cast<int>(),
      base64: map['base64'] as String?,
      filePath: map['filePath'] as String?,
    );
  }
}

/// Error from the TrueID SDK.
class TrueIdException implements Exception {
  final String code;
  final String message;

  const TrueIdException({required this.code, required this.message});

  @override
  String toString() => 'TrueIdException($code): $message';
}
