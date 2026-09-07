import 'dart:convert';

import 'package:flutter_secure_storage/flutter_secure_storage.dart';

import 'sesion.dart';

/// Donde vive la sesion entre arranques de la app.
abstract interface class AlmacenDeSesion {
  Future<void> guardar(Sesion sesion);

  /// La sesion guardada, o null si no hay.
  Future<Sesion?> leer();

  Future<void> borrar();
}

/// Implementacion real: `flutter_secure_storage`.
///
/// En Android eso es el keystore ([opcionesAndroid] fuerza
/// `encryptedSharedPreferences`), no unos SharedPreferences en claro. En Web es
/// WebCrypto sobre `localStorage`: sobrevive a cerrar la pestana, que es lo que
/// pide el criterio 4.
class AlmacenSeguroDeSesion implements AlmacenDeSesion {
  AlmacenSeguroDeSesion({FlutterSecureStorage? almacen})
      : _almacen = almacen ??
            const FlutterSecureStorage(
              aOptions: opcionesAndroid,
              iOptions: IOSOptions(accessibility: KeychainAccessibility.first_unlock),
            );

  static const String clave = 'regenta.sesion';
  static const AndroidOptions opcionesAndroid =
      AndroidOptions(encryptedSharedPreferences: true);

  final FlutterSecureStorage _almacen;

  @override
  Future<void> guardar(Sesion sesion) =>
      _almacen.write(key: clave, value: jsonEncode(sesion.toJson()));

  @override
  Future<Sesion?> leer() async {
    final crudo = await _almacen.read(key: clave);
    if (crudo == null || crudo.isEmpty) return null;
    return Sesion.fromJson(jsonDecode(crudo) as Map<String, dynamic>);
  }

  @override
  Future<void> borrar() => _almacen.delete(key: clave);
}
