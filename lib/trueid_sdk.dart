/// TrueID SDK for Flutter — identity verification via Ghana Card (NIA).
///
/// ```dart
/// // Initialize once
/// TrueIdSdk.initialize(apiKey: 'your-api-key');
///
/// // Verify identity
/// final result = await TrueIdSdk.verify();
/// if (result.verified) {
///   print('Hello, ${result.fullName}');
/// }
/// ```
library trueid_sdk;

export 'src/trueid_sdk.dart';
export 'src/models.dart';
