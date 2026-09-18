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

  @override
  Future<List<ClienteDeLaVenta>> buscar(String termino) async {
    try {
      final cuerpo = await _http.get<dynamic>(
        '/api/clientes',
        query: termino.trim().length >= 3 ? {'q': termino.trim()} : null,
      );
      final lista = cuerpo is List
          ? cuerpo
          : ((cuerpo as Map<String, dynamic>)['clientes'] ??
              cuerpo['contenido'] ??
              const <dynamic>[]) as List<dynamic>;
      final clientes = lista
          .map((c) => ClienteDeLaVenta.desdeJson(c as Map<String, dynamic>))
          .toList();
      await _guardarEnCache(clientes);
      return clientes;
    } on ErrorDeRed {
      return _enCache(termino);
    }
  }

  @override
  Future<void> asignar({required String ventaId, required String clienteId}) async {
    // No es encolable: asignar el cliente cambia la venta que se está armando,
    // y el POS necesita saber ya si sirvió. Lo que sí sube en la cola es la
    // venta entera, con su clienteId adentro.
    await _http.put<Map<String, dynamic>>('/api/ventas/$ventaId/cliente',
        datos: {'clienteId': clienteId});
  }

  @override
  Future<void> quitar({required String ventaId}) async {
    await _http.delete<Map<String, dynamic>>('/api/ventas/$ventaId/cliente');
  }

  @override
  Future<ResultadoDeAlta> crear(ClienteNuevo nuevo) async {
    final respuesta = await _http.post<Map<String, dynamic>>(
      '/api/clientes/expres',
      datos: nuevo.aJson(),
      // HU-114 criterio 5: sin señal se encola y sube con el mismo id, así que
      // el reintento devuelve el mismo cliente en vez de crear otro.
      encolable: true,
    );
    if (respuesta is Encolado) {
      return ResultadoDeAlta(id: nuevo.id, quedoEnLaCola: true);
    }
    return ResultadoDeAlta(
      id: nuevo.id,
      cliente: ClienteDeLaVenta.desdeJson(respuesta as Map<String, dynamic>),
    );
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
