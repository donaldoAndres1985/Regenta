import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_messaging/firebase_messaging.dart';
import 'package:flutter/foundation.dart';
import 'package:regenta_core/regenta_core.dart';

/// La configuración de Firebase llega por `--dart-define` al compilar, nunca
/// en el repo: ni `google-services.json` ni claves en el código (HU-123).
///
/// ```
/// flutter build apk --dart-define=FIREBASE_API_KEY=... \
///   --dart-define=FIREBASE_APP_ID=... \
///   --dart-define=FIREBASE_MESSAGING_SENDER_ID=... \
///   --dart-define=FIREBASE_PROJECT_ID=...
/// ```
///
/// Sin ella la app funciona igual, solo que sin push.
class ConfiguracionDeFirebase {
  static const apiKey = String.fromEnvironment('FIREBASE_API_KEY');
  static const appId = String.fromEnvironment('FIREBASE_APP_ID');
  static const messagingSenderId = String.fromEnvironment('FIREBASE_MESSAGING_SENDER_ID');
  static const projectId = String.fromEnvironment('FIREBASE_PROJECT_ID');

  static bool get completa =>
      apiKey.isNotEmpty && appId.isNotEmpty && messagingSenderId.isNotEmpty && projectId.isNotEmpty;
}

/// El push del dispositivo. En Web, o sin la configuración de Firebase, no hay:
/// en Web el canal no aplica (C5). [alRecibir] corre cuando llega un push con
/// la app abierta.
Future<ServicioDePush> crearServicioDePush({VoidCallback? alRecibir}) async {
  if (kIsWeb || !ConfiguracionDeFirebase.completa) return const SinPush();
  try {
    await Firebase.initializeApp(
      options: const FirebaseOptions(
        apiKey: ConfiguracionDeFirebase.apiKey,
        appId: ConfiguracionDeFirebase.appId,
        messagingSenderId: ConfiguracionDeFirebase.messagingSenderId,
        projectId: ConfiguracionDeFirebase.projectId,
      ),
    );
    final mensajeria = FirebaseMessaging.instance;
    if (alRecibir != null) {
      FirebaseMessaging.onMessage.listen((_) => alRecibir());
    }
    return ServicioDePushFirebase(mensajeria);
  } catch (e) {
    debugPrint('Push no disponible: $e');
    return const SinPush();
  }
}

/// Firebase Cloud Messaging detrás del contrato del núcleo.
class ServicioDePushFirebase implements ServicioDePush {
  ServicioDePushFirebase(this._mensajeria);

  final FirebaseMessaging _mensajeria;

  @override
  String get plataforma => defaultTargetPlatform == TargetPlatform.iOS ? 'IOS' : 'ANDROID';

  @override
  Future<PermisoDePush> permiso() async =>
      _traducir((await _mensajeria.getNotificationSettings()).authorizationStatus);

  @override
  Future<PermisoDePush> pedirPermiso() async =>
      _traducir((await _mensajeria.requestPermission()).authorizationStatus);

  // El backend envía por "registration token" con la API HTTP v1 de FCM
  // (alertas.PasarelaFcm). firebase_messaging 17 marca getToken/onTokenRefresh
  // como obsoletos en favor de register()/onRegistered, que entregan un
  // Installation ID para otro modo de envío. Mientras el backend use tokens,
  // estos siguen siendo los correctos.
  @override
  // ignore: deprecated_member_use
  Future<String?> token() => _mensajeria.getToken();

  @override
  // ignore: deprecated_member_use
  Stream<String> get tokensNuevos => _mensajeria.onTokenRefresh;

  static PermisoDePush _traducir(AuthorizationStatus estado) => switch (estado) {
        AuthorizationStatus.authorized || AuthorizationStatus.provisional => PermisoDePush.concedido,
        AuthorizationStatus.denied || AuthorizationStatus.deniedPermanently => PermisoDePush.denegado,
        AuthorizationStatus.notDetermined => PermisoDePush.sinPreguntar,
      };
}
