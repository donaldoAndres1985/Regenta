import 'dart:convert';
import 'dart:math' as math;

import 'package:dio/dio.dart';
import 'package:drift/drift.dart';

import '../http/errores_http.dart';
import '../local/base_local.dart';
import 'resumen_de_cola.dart';

/// Saca operaciones de [OperacionesPendientes] y las reproduce contra el
/// backend (HU-111). No conoce nada de negocio: cada fila ya trae el método,
/// la ruta y el cuerpo con que se hubiera llamado si hubiera habido red.
///
/// - Éxito: la fila sale de la cola.
/// - Falla por red o por el servidor (5xx): transitorio, se reintenta con
///   backoff exponencial (criterio 2); la fila nunca se borra por esto.
/// - 409 (choca con algo que ya existe en el servidor): CONFLICTO — no se
///   reintenta solo, hace falta que alguien lo resuelva (criterio 4), y no
///   bloquea el resto de la cola (mismo criterio que HU-102 del lado del
///   servidor).
/// - Cualquier otro rechazo (401, 402, 403, 422, desconocido): RECHAZADA, ya
///   no va a aplicar sola por más veces que se reintente.
class TrabajadorDeSincronizacion {
  TrabajadorDeSincronizacion({
    required Dio dio,
    required BaseLocal db,
    DateTime Function()? ahora,
  })  : _dio = dio,
        _db = db,
        _ahora = ahora ?? (() => DateTime.now().toUtc());

  final Dio _dio;
  final BaseLocal _db;
  final DateTime Function() _ahora;

  static const _baseDelay = Duration(seconds: 5);
  static const _maxDelay = Duration(minutes: 30);

  /// Una pasada: procesa lo que ya está en tiempo de reintentarse, en el
  /// orden en que se encoló, y devuelve cómo quedó la cola al terminar.
  Future<ResumenDeCola> ejecutarUnaPasada() async {
    final ahora = _ahora();
    final tabla = _db.operacionesPendientes;
    final pendientes = await (_db.select(tabla)
          ..where((t) =>
              t.estado.equals('PENDIENTE') &
              (t.proximoIntentoEn.isNull() | t.proximoIntentoEn.isSmallerOrEqualValue(ahora)))
          ..orderBy([(t) => OrderingTerm.asc(t.creadoEn)]))
        .get();

    for (final fila in pendientes) {
      await _procesarUna(fila);
    }

    return resumen();
  }

  /// El estado actual de la cola, sin tocar nada (para la pantalla de sincronización).
  Future<ResumenDeCola> resumen() async {
    final tabla = _db.operacionesPendientes;
    final pendientes = await (_db.selectOnly(tabla)
          ..addColumns([tabla.id.count()])
          ..where(tabla.estado.equals('PENDIENTE')))
        .map((r) => r.read(tabla.id.count()) ?? 0)
        .getSingle();
    final conflictos = await (_db.selectOnly(tabla)
          ..addColumns([tabla.id.count()])
          ..where(tabla.estado.equals('CONFLICTO')))
        .map((r) => r.read(tabla.id.count()) ?? 0)
        .getSingle();
    return ResumenDeCola(pendientes: pendientes, conflictos: conflictos);
  }

  /// Igual que [resumen], pero reactivo: la pantalla se actualiza sola
  /// apenas cambia la cola, sin que nadie la tenga que refrescar a mano.
  Stream<ResumenDeCola> observar() {
    final tabla = _db.operacionesPendientes;
    return _db.select(tabla).watch().map((filas) => ResumenDeCola(
          pendientes: filas.where((f) => f.estado == 'PENDIENTE').length,
          conflictos: filas.where((f) => f.estado == 'CONFLICTO').length,
        ));
  }

  Future<void> _procesarUna(OperacionesPendiente fila) async {
    final envelope = fila.cuerpo == null
        ? const <String, Object?>{}
        : (jsonDecode(fila.cuerpo!) as Map<String, dynamic>);
    final datos = envelope['datos'];
    final query = (envelope['query'] as Map?)?.cast<String, dynamic>();

    try {
      await _dio.request<Object?>(
        fila.ruta,
        data: datos,
        queryParameters: query,
        options: Options(method: fila.metodo),
      );
      await (_db.delete(_db.operacionesPendientes)..where((t) => t.id.equals(fila.id))).go();
    } on DioException catch (fallo) {
      await _registrarFallo(fila, mapearError(fallo));
    }
  }

  Future<void> _registrarFallo(OperacionesPendiente fila, ErrorDeApi error) async {
    final companion = switch (error) {
      ErrorDeRed() || ErrorDelServidor() => OperacionesPendientesCompanion(
          intentos: Value(fila.intentos + 1),
          proximoIntentoEn: Value(_ahora().add(_backoff(fila.intentos + 1))),
        ),
      RecursoDuplicado() => const OperacionesPendientesCompanion(
          estado: Value('CONFLICTO'),
          proximoIntentoEn: Value(null),
        ),
      _ => const OperacionesPendientesCompanion(
          estado: Value('RECHAZADA'),
          proximoIntentoEn: Value(null),
        ),
    };
    await (_db.update(_db.operacionesPendientes)..where((t) => t.id.equals(fila.id)))
        .write(companion);
  }

  Duration _backoff(int intentos) {
    final segundos = _baseDelay.inSeconds * math.pow(2, intentos - 1);
    final limitado = math.min(segundos.toInt(), _maxDelay.inSeconds);
    return Duration(seconds: limitado);
  }
}
