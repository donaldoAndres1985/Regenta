import 'package:flutter/foundation.dart';
import 'package:regenta_core/regenta_core.dart' show ErrorDeApi;

import 'repositorio_de_reportes_de_caja.dart';
import 'sesion_de_caja_vista.dart';

/// El estado del reporte de cierre de caja (HU-063). El listado lo escucha.
class ControladorDeReportesDeCaja extends ChangeNotifier {
  ControladorDeReportesDeCaja(this._repo);

  final RepositorioDeReportesDeCaja _repo;

  List<SesionDeCajaVista> _sesiones = const [];
  bool _cargando = false;
  String? _error;

  List<SesionDeCajaVista> get sesiones => _sesiones;
  bool get cargando => _cargando;
  String? get error => _error;

  /// Cuántos turnos del listado quedaron descuadrados (criterio 3).
  int get descuadres => _sesiones.where((s) => s.descuadrada).length;

  Future<void> cargar({
    required DateTime desde,
    required DateTime hasta,
    String? estado,
  }) async {
    _cargando = true;
    _error = null;
    notifyListeners();
    try {
      _sesiones = await _repo.sesiones(desde: desde, hasta: hasta, estado: estado);
    } on ErrorDeApi catch (e) {
      _error = e.mensaje;
    } finally {
      _cargando = false;
      notifyListeners();
    }
  }
}
