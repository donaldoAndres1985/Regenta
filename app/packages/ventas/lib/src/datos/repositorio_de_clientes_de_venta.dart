import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:uuid/uuid.dart';

import 'cliente_de_la_venta.dart';

/// De dónde salen los clientes que se pueden asignar a la venta (HU-113 y
/// HU-114).
///
/// Es una interfaz para que la pantalla se pueda probar sin red y sin base.
abstract interface class RepositorioDeClientesDeVenta {
  Future<List<ClienteDeLaVenta>> buscar(String termino);

  Future<void> asignar({required String ventaId, required String clienteId});

  Future<void> quitar({required String ventaId});

  Future<ResultadoDeAlta> crear(ClienteNuevo nuevo);
}

/// La implementación de verdad: red cuando hay, copia local cuando no.
///
/// «No poder buscar un cliente nunca bloquea el cobro»
/// (`design/comportamiento/ClienteVenta.md`): si la red falla, se busca en lo
/// último que se sincronizó y la venta sigue.
class RepositorioDeClientesHttp implements RepositorioDeClientesDeVenta {
  RepositorioDeClientesHttp(this._http, this._db, {Uuid? uuid})
      : _uuid = uuid ?? const Uuid();

  static const _claveDeCache = 'clientes-de-venta';

  final ClienteHttp _http;
  final BaseLocal _db;
  final Uuid _uuid;

  static bool _esUuidValido(String id) =>
      RegExp(r'^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$').hasMatch(id.trim());

  @override
  Future<List<ClienteDeLaVenta>> buscar(String termino) async {
    try {
      final cuerpo = await _http.get<dynamic>(
        '/api/clientes',
        query: termino.trim().length >= 3 ? {'q': termino.trim()} : null,
      );
      final lista = cuerpo is List
          ? cuerpo
          : (((cuerpo as Map)['clientes'] ??
              (cuerpo as Map)['contenido'] ??
              const <dynamic>[]) as List<dynamic>);
      final clientes = lista
          .map((c) => ClienteDeLaVenta.desdeJson((c as Map).cast<String, dynamic>()))
          .toList();
      if (clientes.isNotEmpty) {
        await _guardarEnCacheSiSePuede(clientes);
      }
      if (termino.trim().isNotEmpty) {
        return clientes.where((c) => c.coincideCon(termino)).toList();
      }
      return clientes;
    } on ErrorDeRed {
      // Solo la falta de red recurre al cache en silencio: un 403 por falta de
      // permiso, una sesión vencida o un error del servidor deben llegar hasta
      // el controlador para que el usuario sepa por qué no ve clientes, en vez
      // de parecer "este negocio no tiene clientes".
      return _enCacheSiSePuede(termino);
    }
  }

  /// La copia local es una ayuda para cuando no hay señal, no un requisito: si
  /// la base del equipo no abre (en Web pasa cuando falta `sqlite3.wasm`), lo
  /// que respondió el servidor se muestra igual.
  Future<void> _guardarEnCacheSiSePuede(List<ClienteDeLaVenta> clientes) async {
    try {
      await _guardarEnCache(clientes);
    } catch (_) {}
  }

  Future<List<ClienteDeLaVenta>> _enCacheSiSePuede(String termino) async {
    try {
      return await _enCache(termino);
    } catch (_) {
      return const [];
    }
  }

  @override
  Future<void> asignar({required String ventaId, required String clienteId}) async {
    // Si no es un id de venta real en el servidor (ej. borrador local en POS), no manda HTTP.
    if (!_esUuidValido(ventaId)) return;
    try {
      await _http.put<dynamic>('/api/ventas/$ventaId/cliente',
          datos: {'clienteId': clienteId});
    } catch (_) {}
  }

  @override
  Future<void> quitar({required String ventaId}) async {
    if (!_esUuidValido(ventaId)) return;
    try {
      await _http.delete<dynamic>('/api/ventas/$ventaId/cliente');
    } catch (_) {}
  }

  @override
  Future<ResultadoDeAlta> crear(ClienteNuevo nuevo) async {
    final dynamic respuesta = await _http.post<dynamic>(
      '/api/clientes/expres',
      datos: nuevo.aJson(),
      // HU-114 criterio 5: sin señal se encola y sube con el mismo id, así que
      // el reintento devuelve el mismo cliente en vez de crear otro.
      encolable: true,
    );
    final cliente = respuesta is Encolado
        ? ClienteDeLaVenta(
            id: nuevo.id,
            nombre: nuevo.nombre,
            tipoDocumento: nuevo.tipoDocumento,
            numeroDocumento: nuevo.numeroDocumento,
            digitoVerificacion: nuevo.digitoVerificacion,
          )
        : ClienteDeLaVenta.desdeJson((respuesta as Map).cast<String, dynamic>());
    await _agregarAlCache(cliente);
    if (respuesta is Encolado) {
      return ResultadoDeAlta(id: nuevo.id, cliente: cliente, quedoEnLaCola: true);
    }
    return ResultadoDeAlta(
      id: nuevo.id,
      cliente: cliente,
    );
  }

  Future<void> _agregarAlCache(ClienteDeLaVenta nuevoCliente) async {
    try {
      final existentes = await _enCache('');
      final actualizados = [
        nuevoCliente,
        ...existentes.where((c) => c.id != nuevoCliente.id),
      ];
      await _guardarEnCache(actualizados);
    } catch (_) {}
  }

  /// Un id nuevo para un cliente que se crea desde la venta.
  String nuevoId() => _uuid.v4();

  Future<List<ClienteDeLaVenta>> _enCache(String termino) async {
    final fila = await (_db.select(_db.catalogos)
          ..where((c) => c.clave.equals(_claveDeCache)))
        .getSingleOrNull();
    if (fila == null) return const [];
    final lista = jsonDecode(fila.contenido) as List<dynamic>;
    return lista
        .map((c) => ClienteDeLaVenta.desdeJson(c as Map<String, dynamic>))
        .where((c) => c.coincideCon(termino))
        .toList();
  }

  Future<void> _guardarEnCache(List<ClienteDeLaVenta> clientes) async {
    await _db.into(_db.catalogos).insertOnConflictUpdate(CatalogosCompanion.insert(
          clave: _claveDeCache,
          tipo: const Value('clientes'),
          contenido: jsonEncode(clientes.map((c) => c.aJson()).toList()),
          actualizadoEn: DateTime.now(),
        ));
  }
}
