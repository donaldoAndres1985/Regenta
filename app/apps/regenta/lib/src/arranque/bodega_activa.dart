import 'package:flutter/foundation.dart';

/// El id real de la bodega desde la que se vende (HU-029).
///
/// Antes de esto, `bodegaActivaProvider` mandaba el `negocioId` como si fuera
/// un id de bodega -un placeholder, porque la sesión todavía no trae
/// sucursal/bodega-. El backend nunca valida ese id contra la tabla
/// `bodegas`, así que la saga de confirmación (HU-038) nunca encontraba la
/// fila de existencia correcta y cualquier venta real se rechazaba con "no
/// hay stock suficiente", sin importar cuánto hubiera.
///
/// Mientras la sesión no traiga su propia sucursal/bodega, esto guarda el id
/// de la primera bodega del negocio (hoy siempre hay una sola; el día que
/// haya varias, acá es donde cambia a un selector real).
class BodegaActiva extends ChangeNotifier {
  String? _id;

  String? get id => _id;

  void fijar(String id) {
    if (id == _id) return;
    _id = id;
    notifyListeners();
  }

  void limpiar() {
    if (_id == null) return;
    _id = null;
    notifyListeners();
  }
}
