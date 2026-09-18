import 'dart:convert';

import 'package:drift/drift.dart' show Value;
import 'package:uuid/uuid.dart';

import '../http/cola_de_salida.dart';
import '../local/base_local.dart';

/// La [ColaDeSalida] de verdad (HU-111): lo que [ClienteHttp] encola por
/// falta de red queda en [OperacionesPendientes], y de ahí lo saca
/// [TrabajadorDeSincronizacion] cuando vuelve la conexión.
class ColaDeSalidaLocal implements ColaDeSalida {
  ColaDeSalidaLocal(this._db, {Uuid? uuid}) : _uuid = uuid ?? const Uuid();

  final BaseLocal _db;
  final Uuid _uuid;

  @override
  Future<void> encolar(OperacionEncolable operacion) async {
    await _db.into(_db.operacionesPendientes).insert(OperacionesPendientesCompanion.insert(
          id: _uuid.v4(),
          metodo: operacion.metodo,
          ruta: operacion.ruta,
          cuerpo: Value(jsonEncode({'datos': operacion.datos, 'query': operacion.query})),
          creadoEn: DateTime.now().toUtc(),
        ));
  }
}
