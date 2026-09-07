import 'package:flutter/foundation.dart';

import 'almacen_de_sesion.dart';
import 'cliente_auth.dart';
import 'sesion.dart';

/// Por que se cerro la sesion. El router lo usa para llevar al login con un
/// mensaje que se entienda.
enum MotivoDeCierre {
  cierreManual('Cerraste la sesion.'),
  refrescoRechazado('Tu sesion caduco. Entra de nuevo.'),
  sinSesionGuardada('No hay una sesion iniciada.');

  const MotivoDeCierre(this.mensaje);

  final String mensaje;
}

/// Hay sesion, o no la hay.
sealed class EstadoDeSesion {
  const EstadoDeSesion();
}

class ConSesion extends EstadoDeSesion {
  const ConSesion(this.sesion);
  final Sesion sesion;
}

class SinSesion extends EstadoDeSesion {
  const SinSesion([this.motivo]);
  final MotivoDeCierre? motivo;
}

/// El motor de la sesion: la carga al arrancar, la guarda cifrada, la refresca
/// sola antes de que caduque y la cierra con un motivo cuando el refresco ya no
/// sirve. Es un [ChangeNotifier]: el router escucha y redirige.
class MotorDeSesion extends ChangeNotifier {
  MotorDeSesion({
    required ClienteAuth cliente,
    required AlmacenDeSesion almacen,
    DateTime Function()? ahora,
    Duration margenDeRefresco = const Duration(seconds: 60),
    String? dispositivoId,
    String? plataforma,
  })  : _cliente = cliente,
        _almacen = almacen,
        _ahora = ahora ?? DateTime.now,
        _margen = margenDeRefresco,
        _dispositivoId = dispositivoId,
        _plataforma = plataforma;

  final ClienteAuth _cliente;
  final AlmacenDeSesion _almacen;
  final DateTime Function() _ahora;
  final Duration _margen;
  final String? _dispositivoId;
  final String? _plataforma;

  Sesion? _sesion;
  MotivoDeCierre? _motivo;
  Future<void>? _refrescoEnCurso;

  EstadoDeSesion get estado =>
      _sesion != null ? ConSesion(_sesion!) : SinSesion(_motivo);

  Sesion? get sesion => _sesion;

  /// Al arrancar la app (o al volver a una pestana): recupera la sesion
  /// guardada. Si el acceso ya vencio pero el refresco sigue vivo, la renueva y
  /// se sigue dentro. Criterio 4.
  Future<void> iniciar() async {
    final guardada = await _almacen.leer();
    if (guardada == null) {
      _cerrar(MotivoDeCierre.sinSesionGuardada, borrar: false);
      return;
    }
    _sesion = guardada;
    if (guardada.accesoVigente(_ahora())) {
      _motivo = null;
      notifyListeners();
      return;
    }
    try {
      await _refrescar();
    } on RefrescoRechazado {
      await _cerrarPorRefrescoRechazado();
    }
  }

  Future<void> entrar(Credenciales credenciales) async {
    final sesion = await _cliente.entrar(_conDispositivo(credenciales));
    _sesion = sesion;
    _motivo = null;
    await _almacen.guardar(sesion);
    notifyListeners();
  }

  /// Devuelve un token de acceso que sirve ahora mismo. Si le queda menos que el
  /// margen —o si se pide `forzar`— lo refresca antes, sin que la pantalla se
  /// entere. Criterio 2. Si el refresco es rechazado, cierra la sesion y
  /// relanza [RefrescoRechazado].
  Future<String> accesoVigente({bool forzar = false}) async {
    final actual = _sesion;
    if (actual == null) {
      throw StateError('No hay sesion: no se puede pedir un token de acceso');
    }
    if (!forzar && actual.accesoVigente(_ahora(), margen: _margen)) {
      return actual.tokenDeAcceso;
    }
    try {
      await _refrescar();
    } on RefrescoRechazado {
      await _cerrarPorRefrescoRechazado();
      rethrow;
    }
    return _sesion!.tokenDeAcceso;
  }

  /// Cierre a peticion del usuario.
  Future<void> cerrar([MotivoDeCierre motivo = MotivoDeCierre.cierreManual]) async {
    await _cerrar(motivo, borrar: true);
  }

  // Un solo refresco a la vez: si tres peticiones lo piden juntas, comparten
  // la misma llamada.
  Future<void> _refrescar() {
    return _refrescoEnCurso ??=
        _hacerRefresco().whenComplete(() => _refrescoEnCurso = null);
  }

  Future<void> _hacerRefresco() async {
    final actual = _sesion;
    if (actual == null) throw const RefrescoRechazado();
    final nueva = await _cliente.refrescar(
      tokenDeRefresco: actual.tokenDeRefresco,
      dispositivoId: _dispositivoId,
      plataforma: _plataforma,
    );
    _sesion = nueva;
    _motivo = null;
    await _almacen.guardar(nueva);
    notifyListeners();
  }

  Future<void> _cerrarPorRefrescoRechazado() =>
      _cerrar(MotivoDeCierre.refrescoRechazado, borrar: true);

  Future<void> _cerrar(MotivoDeCierre motivo, {required bool borrar}) async {
    _sesion = null;
    _motivo = motivo;
    if (borrar) await _almacen.borrar();
    notifyListeners();
  }

  Credenciales _conDispositivo(Credenciales c) => Credenciales(
        email: c.email,
        password: c.password,
        negocioId: c.negocioId,
        dispositivoId: c.dispositivoId ?? _dispositivoId,
        plataforma: c.plataforma ?? _plataforma,
      );
}
