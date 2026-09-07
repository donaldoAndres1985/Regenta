import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:regenta_core/regenta_core.dart';

import 'cliente_en_lista.dart';

/// Acceso al listado de clientes: la red (`GET /api/clientes`) y la copia local
/// en la tabla `catalogos` del núcleo, que es lo que se muestra sin conexión
/// (HU-025 criterio 2).
class RepositorioDeClientes {
  RepositorioDeClientes(this._http, this._db);

  static const _claveDeCache = 'clientes';

  final ClienteHttp _http;
  final BaseLocal _db;

  /// Lo que hay en la copia local. Vacío si nunca se bajó nada.
  Future<List<ClienteEnLista>> enCache() async {
    final fila = await (_db.select(_db.catalogos)
          ..where((t) => t.clave.equals(_claveDeCache)))
        .getSingleOrNull();
    if (fila == null) return const [];
    return _parsear(fila.contenido);
  }

  /// Lo que devuelve el servidor. Lanza [ErrorDeApi] si no hay red.
  Future<List<ClienteEnLista>> enRed() async {
    final cuerpo = await _http.get<Map<String, dynamic>>('/api/clientes');
    final lista = (cuerpo['clientes'] ?? cuerpo['contenido'] ?? const [])
        as List<dynamic>;
    return lista
        .map((e) => ClienteEnLista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  /// Deja la copia local lista para la próxima vez que no haya señal.
  Future<void> guardarEnCache(List<ClienteEnLista> clientes) async {
    await _db.into(_db.catalogos).insertOnConflictUpdate(CatalogosCompanion.insert(
          clave: _claveDeCache,
          tipo: const Value('clientes'),
          contenido: jsonEncode(clientes.map((c) => c.aJson()).toList()),
          actualizadoEn: DateTime.now(),
        ));
  }

  static List<ClienteEnLista> _parsear(String contenido) {
    final lista = jsonDecode(contenido) as List<dynamic>;
    return lista
        .map((e) => ClienteEnLista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }
}
