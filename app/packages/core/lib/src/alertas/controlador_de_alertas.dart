import 'package:flutter/foundation.dart';

import '../http/errores_http.dart';
import 'alerta_vista.dart';
import 'repositorio_de_alertas.dart';

/// El estado del centro de alertas (HU-095). Lo escuchan la campana y el panel:
/// cuando llega o se resuelve una alerta, avisan y la UI se actualiza.
class ControladorDeAlertas extends ChangeNotifier {
  ControladorDeAlertas(this._repo);

  final RepositorioDeAlertas _repo;

  List<AlertaVista> _alertas = const [];
  bool _cargando = false;
  String? _error;

  List<AlertaVista> get alertas => _alertas;
  bool get cargando => _cargando;
  String? get error => _error;

  /// Las que todavía cuentan como pendientes, ya ordenadas (criterio 1).
  List<AlertaVista> get pendientes =>
      [..._alertas.where((a) => a.estaPendiente)]..sort((a, b) => a.compararCon(b));

  /// Criterio 4: la campana muestra el indicador si hay alguna nueva.
  bool get hayNuevas => _alertas.any((a) => a.esNueva);

  int get cuentaNuevas => _alertas.where((a) => a.esNueva).length;

  Future<void> cargar() async {
    _cargando = true;
    _error = null;
    notifyListeners();
    try {
      _alertas = await _repo.mias();
    } on ErrorDeApi catch (e) {
      _error = e.mensaje;
    } finally {
      _cargando = false;
      notifyListeners();
    }
  }

  /// Al abrir el centro: las nuevas pasan a vistas y se apaga el indicador.
  Future<void> abrir() async {
    if (!hayNuevas) return;
    _alertas = [
      for (final a in _alertas)
        a.esNueva
            ? AlertaVista(
                id: a.id,
                tipoCodigo: a.tipoCodigo,
                severidad: a.severidad,
                estado: EstadoAlerta.vista,
                titulo: a.titulo,
                mensaje: a.mensaje,
                generadaEn: a.generadaEn,
                rutaApp: a.rutaApp,
                entidadTipo: a.entidadTipo,
                entidadId: a.entidadId,
              )
            : a,
    ];
    notifyListeners();
    try {
      await _repo.marcarVistas();
    } on ErrorDeApi {
      // el indicador ya se apagó localmente; se reintenta en la próxima carga
    }
  }

  /// Criterio 2: resolver la saca de las pendientes y deja quién la resolvió.
  Future<void> resolver(String alertaId, {String porUsuario = 'yo'}) async {
    final antes = _alertas;
    _alertas = [
      for (final a in _alertas)
        a.id == alertaId ? a.comoResuelta(porUsuario) : a,
    ];
    notifyListeners();
    try {
      await _repo.resolver(alertaId);
    } on ErrorDeApi catch (e) {
      _alertas = antes;
      _error = e.mensaje;
      notifyListeners();
    }
  }
}
