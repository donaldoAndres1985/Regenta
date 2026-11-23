import 'dart:async';

import 'package:flutter/foundation.dart';

import '../../http/errores_http.dart';
import 'repositorio_de_dispositivos.dart';
import 'servicio_de_push.dart';

/// Que las alertas lleguen al celular (HU-123).
///
/// **El permiso no se pide al arrancar.** Al entrar a la sesión ([alEntrar])
/// solo se mira si ya estaba concedido y, en ese caso, se registra el token sin
/// molestar a nadie. El diálogo del sistema sale únicamente cuando el usuario
/// toca *Activar* en el centro de alertas ([activar]), que es cuando sabe
/// para qué sirve.
///
/// Cada token nuevo que entregue FCM se vuelve a registrar. En Web no hay push:
/// el estado es [PermisoDePush.noDisponible] y no se muestra nada.
class RegistroDePush extends ChangeNotifier {
  RegistroDePush(this._servicio, this._repo, {this.modelo, this.versionApp});

  final ServicioDePush _servicio;
  final RepositorioDeDispositivos _repo;
  final String? modelo;
  final String? versionApp;

  PermisoDePush _permiso = PermisoDePush.noDisponible;
  bool _registrado = false;
  bool _ocupado = false;
  String? _error;
  StreamSubscription<String>? _renovaciones;

  PermisoDePush get permiso => _permiso;

  /// El token de este celular quedó registrado en el negocio de la sesión.
  bool get registrado => _registrado;
  bool get ocupado => _ocupado;
  String? get error => _error;

  /// Mostrar la invitación a activarlas: hay push y nunca se preguntó.
  bool get invitarAActivar => _permiso == PermisoDePush.sinPreguntar;

  /// Al entrar a la sesión. Nunca abre el diálogo del permiso.
  Future<void> alEntrar() async {
    _permiso = await _servicio.permiso();
    notifyListeners();
    if (_permiso == PermisoDePush.concedido) {
      await _registrarToken();
    }
  }

  /// El usuario pidió recibirlas en el celular: ahora sí se pregunta.
  Future<void> activar() async {
    if (_permiso == PermisoDePush.noDisponible || _ocupado) return;
    _ocupado = true;
    _error = null;
    notifyListeners();
    try {
      _permiso = await _servicio.pedirPermiso();
      if (_permiso == PermisoDePush.concedido) {
        await _registrarToken();
      }
    } finally {
      _ocupado = false;
      notifyListeners();
    }
  }

  Future<void> _registrarToken() async {
    final token = await _servicio.token();
    if (token == null || token.isEmpty) return;
    await _registrar(token);
    _renovaciones ??= _servicio.tokensNuevos.listen(_registrar);
  }

  Future<void> _registrar(String token) async {
    try {
      await _repo.registrar(
        tokenFcm: token,
        plataforma: _servicio.plataforma,
        modelo: modelo,
        versionApp: versionApp,
      );
      _registrado = true;
      _error = null;
    } on ErrorDeApi catch (e) {
      // Sin red o el servidor caído: se vuelve a intentar al próximo inicio de
      // sesión o con el próximo token. La alerta igual está en el centro.
      _registrado = false;
      _error = e.mensaje;
    }
    notifyListeners();
  }

  @override
  void dispose() {
    _renovaciones?.cancel();
    super.dispose();
  }
}
