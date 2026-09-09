import 'package:flutter/foundation.dart';
import 'package:regenta_core/regenta_core.dart' show ErrorDeApi;

import 'ocupacion_vista.dart';
import 'repositorio_de_calendario.dart';

/// El estado del calendario de ocupación (HU-075). La franja visible es de una
/// semana; moverse a la semana anterior o siguiente vuelve a pedir los datos.
class ControladorDeCalendario extends ChangeNotifier {
  ControladorDeCalendario(this._repo, {this.diasPorVentana = 7});

  final RepositorioDeCalendario _repo;
  final int diasPorVentana;

  OcupacionDelCalendario? _ocupacion;
  bool _cargando = false;
  String? _error;
  DateTime _desde = _hoy();
  String? _tipoRecursoId;

  OcupacionDelCalendario? get ocupacion => _ocupacion;
  bool get cargando => _cargando;
  String? get error => _error;
  DateTime get desde => _desde;
  DateTime get hasta => _desde.add(Duration(days: diasPorVentana - 1));

  Future<void> cargar({DateTime? desde, String? tipoRecursoId}) async {
    _desde = _soloFecha(desde ?? _desde);
    _tipoRecursoId = tipoRecursoId ?? _tipoRecursoId;
    await _pedir();
  }

  Future<void> semanaAnterior() =>
      cargar(desde: _desde.subtract(Duration(days: diasPorVentana)));

  Future<void> semanaSiguiente() =>
      cargar(desde: _desde.add(Duration(days: diasPorVentana)));

  Future<void> _pedir() async {
    _cargando = true;
    _error = null;
    notifyListeners();
    try {
      _ocupacion = await _repo.ocupacion(
        desde: _desde,
        hasta: hasta,
        tipoRecursoId: _tipoRecursoId,
      );
    } on ErrorDeApi catch (e) {
      _error = e.mensaje;
    } finally {
      _cargando = false;
      notifyListeners();
    }
  }

  static DateTime _hoy() => _soloFecha(DateTime.now());

  static DateTime _soloFecha(DateTime d) => DateTime(d.year, d.month, d.day);
}
