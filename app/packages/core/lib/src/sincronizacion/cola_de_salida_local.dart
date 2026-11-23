import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:meta/meta.dart';
import 'package:uuid/uuid.dart';

import '../http/cola_de_salida.dart';
import '../http/errores_http.dart';
import '../local/base_local.dart';

/// Una operación que está en la cola, como la ve quien la armó (HU-182): para listarla «por
/// enviar» o «con error», corregirla y reenviarla.
@immutable
class OperacionEnCola {
  const OperacionEnCola({
    required this.id,
    required this.metodo,
    required this.ruta,
    required this.estado,
    required this.creadoEn,
    this.datos,
    this.ultimoError,
  });

  final String id;
  final String metodo;
  final String ruta;
  final Object? datos;

  /// `PENDIENTE`, `CONFLICTO` o `RECHAZADA`.
  final String estado;
  final String? ultimoError;
  final DateTime creadoEn;

  bool get porEnviar => estado == 'PENDIENTE';
  bool get conError => !porEnviar;
}

/// La [ColaDeSalida] de verdad (HU-111): lo que [ClienteHttp] encola por
/// falta de red queda en [OperacionesPendientes], y de ahí lo saca
/// [TrabajadorDeSincronizacion] cuando vuelve la conexión.
///
/// HU-182 · Con [OperacionEncolable.clave], la operación queda con ese id: encolarla otra vez la
/// reemplaza (vuelve a PENDIENTE con los datos nuevos) en vez de duplicarla.
class ColaDeSalidaLocal implements ColaDeSalida {
  ColaDeSalidaLocal(this._db, {Uuid? uuid}) : _uuid = uuid ?? const Uuid();

  final BaseLocal _db;
  final Uuid _uuid;

  @override
  Future<void> encolar(OperacionEncolable operacion) async {
    await _db.into(_db.operacionesPendientes).insertOnConflictUpdate(OperacionesPendientesCompanion.insert(
          id: operacion.clave ?? _uuid.v4(),
          metodo: operacion.metodo,
          ruta: operacion.ruta,
          cuerpo: Value(jsonEncode({'datos': operacion.datos, 'query': operacion.query})),
          creadoEn: DateTime.now().toUtc(),
          intentos: const Value(0),
          estado: const Value('PENDIENTE'),
          proximoIntentoEn: const Value(null),
          ultimoError: const Value(null),
        ));
  }

  /// Las operaciones cuya ruta empieza con [prefijo], de la más vieja a la más nueva.
  Future<List<OperacionEnCola>> operaciones({String prefijo = ''}) async =>
      (await _consulta(prefijo).get()).map(_comoOperacion).toList();

  /// Igual que [operaciones], pero se actualiza sola cuando cambia la cola.
  Stream<List<OperacionEnCola>> observar({String prefijo = ''}) =>
      _consulta(prefijo).watch().map((filas) => filas.map(_comoOperacion).toList());

  /// Ya llegó al servidor (p. ej. se reenvió a mano): sale de la cola.
  Future<void> quitar(String id) =>
      (_db.delete(_db.operacionesPendientes)..where((t) => t.id.equals(id))).go();

  /// El servidor la rechazó al reenviarla a mano: queda con el motivo, sin reintentarse sola.
  Future<void> marcarRechazada(String id, ErrorDeApi error) =>
      (_db.update(_db.operacionesPendientes)..where((t) => t.id.equals(id))).write(OperacionesPendientesCompanion(
        estado: Value(error is RecursoDuplicado ? 'CONFLICTO' : 'RECHAZADA'),
        proximoIntentoEn: const Value(null),
        ultimoError: Value(motivoDelRechazo(error)),
      ));

  SimpleSelectStatement<$OperacionesPendientesTable, OperacionesPendiente> _consulta(String prefijo) =>
      _db.select(_db.operacionesPendientes)
        ..where((t) => t.ruta.like('${prefijo.replaceAll('%', r'\%')}%'))
        ..orderBy([(t) => OrderingTerm.asc(t.creadoEn)]);

  static OperacionEnCola _comoOperacion(OperacionesPendiente f) {
    final envelope = f.cuerpo == null ? const <String, Object?>{} : jsonDecode(f.cuerpo!) as Map<String, dynamic>;
    return OperacionEnCola(
      id: f.id,
      metodo: f.metodo,
      ruta: f.ruta,
      datos: envelope['datos'],
      estado: f.estado,
      ultimoError: f.ultimoError,
      creadoEn: f.creadoEn,
    );
  }
}

/// HU-182 · Lo que se le muestra a quien armó la operación cuando el servidor la rechaza: el
/// mensaje del backend y, si vino por campo, el primero de cada campo.
String motivoDelRechazo(ErrorDeApi error) {
  if (error is ErroresDeValidacion && error.porCampo.isNotEmpty) {
    final porCampo = [
      for (final e in error.porCampo.entries)
        if (e.value.isNotEmpty) e.value.first,
    ];
    if (porCampo.isNotEmpty) return porCampo.join(' · ');
  }
  return error.mensaje;
}
