/// El permiso de notificaciones del dispositivo (HU-123).
enum PermisoDePush {
  /// En este dispositivo no hay push: la Web, o una compilación sin Firebase.
  noDisponible,

  /// Nunca se le preguntó al usuario.
  sinPreguntar,

  concedido,

  /// El usuario dijo que no; solo se cambia desde los ajustes del teléfono.
  denegado,
}

/// Lo que el núcleo necesita del push del dispositivo, sin saber de Firebase
/// (HU-123). La carcasa trae la implementación real; las pruebas, una simulada.
abstract class ServicioDePush {
  /// `ANDROID` o `IOS`, como lo espera `POST /api/alertas/dispositivos`.
  String get plataforma;

  /// El permiso actual, **sin preguntarle nada al usuario**.
  Future<PermisoDePush> permiso();

  /// Muestra el diálogo del sistema. Solo se llama cuando el usuario pidió
  /// recibir las alertas en el celular, nunca al arrancar.
  Future<PermisoDePush> pedirPermiso();

  /// El token FCM de este dispositivo, o `null` si todavía no hay.
  Future<String?> token();

  /// FCM cambia el token de vez en cuando; cada nuevo hay que registrarlo.
  Stream<String> get tokensNuevos;
}

/// Sin push: la Web (en Web el canal no aplica) o una compilación sin la
/// configuración de Firebase. Nunca pregunta y nunca registra nada.
class SinPush implements ServicioDePush {
  const SinPush();

  @override
  String get plataforma => 'WEB';

  @override
  Future<PermisoDePush> permiso() async => PermisoDePush.noDisponible;

  @override
  Future<PermisoDePush> pedirPermiso() async => PermisoDePush.noDisponible;

  @override
  Future<String?> token() async => null;

  @override
  Stream<String> get tokensNuevos => const Stream.empty();
}
