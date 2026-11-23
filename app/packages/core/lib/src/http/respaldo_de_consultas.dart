import 'dart:convert';

import 'package:drift/drift.dart' show Value;

import '../local/base_local.dart';

/// HU-182 · Dónde guarda [ClienteHttp] la última respuesta de una consulta marcada con
/// `conRespaldo`, para devolverla cuando no hay señal (p. ej. abrir la orden en la bodega).
abstract interface class RespaldoDeConsultas {
  Future<void> guardar(String ruta, Object? respuesta);

  /// La última respuesta guardada para [ruta], o `null` si nunca se consultó con señal.
  Future<Object?> leer(String ruta);
}

/// El respaldo de verdad: en [MetaLocal] de la base local, con la clave `respaldo:<ruta>`.
class RespaldoDeConsultasLocal implements RespaldoDeConsultas {
  RespaldoDeConsultasLocal(this._db);

  final BaseLocal _db;

  static String _clave(String ruta) => 'respaldo:$ruta';

  @override
  Future<void> guardar(String ruta, Object? respuesta) async {
    await _db.into(_db.metaLocal).insertOnConflictUpdate(
          MetaLocalCompanion(clave: Value(_clave(ruta)), valor: Value(jsonEncode(respuesta))),
        );
  }

  @override
  Future<Object?> leer(String ruta) async {
    final fila = await (_db.select(_db.metaLocal)..where((t) => t.clave.equals(_clave(ruta)))).getSingleOrNull();
    return fila == null ? null : jsonDecode(fila.valor);
  }
}
