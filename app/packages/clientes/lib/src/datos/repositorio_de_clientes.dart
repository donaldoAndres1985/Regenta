import 'dart:convert';

import 'package:drift/drift.dart';
import 'package:regenta_core/regenta_core.dart';

import 'cliente_en_lista.dart';
import 'ficha_de_cliente.dart';

/// Acceso al listado de clientes: la red (`GET /api/clientes`) y la copia local
/// en la tabla `catalogos` del núcleo, que es lo que se muestra sin conexión
/// (HU-025 criterio 2).
class RepositorioDeClientes {
  RepositorioDeClientes(this._http, this._db);

  static const _claveDeCache = 'clientes';

  final ClienteHttp _http;
  final BaseLocal _db;

  /// Lo que hay en la copia local. Vacío si nunca se bajó nada, o si la base
  /// del equipo no abre (en Web pasa cuando falta `sqlite3.wasm`): la copia es
  /// una ayuda para cuando no hay señal y no puede impedir que cargue la red.
  Future<List<ClienteEnLista>> enCache() async {
    try {
      final fila = await (_db.select(_db.catalogos)
            ..where((t) => t.clave.equals(_claveDeCache)))
          .getSingleOrNull();
      if (fila == null) return const [];
      return _parsear(fila.contenido);
    } catch (_) {
      return const [];
    }
  }

  /// Lo que devuelve el servidor. Maneja tanto List como Map paginado.
  Future<List<ClienteEnLista>> enRed() async {
    final dynamic res = await _http.get<dynamic>('/api/clientes');
    List<dynamic> lista;
    if (res is List) {
      lista = res;
    } else if (res is Map) {
      lista = (res['clientes'] ?? res['contenido'] ?? const []) as List<dynamic>;
    } else {
      lista = const [];
    }
    return lista
        .map((e) => ClienteEnLista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }

  /// La ficha completa de un cliente, para editarla.
  Future<FichaDeCliente> ver(String clienteId) async {
    final dynamic res = await _http.get<dynamic>('/api/clientes/$clienteId');
    return FichaDeCliente.desdeJson((res as Map).cast<String, dynamic>());
  }

  /// Crea el cliente si la ficha no trae id, o guarda los cambios si lo trae
  /// (HU-021). Devuelve la fila como queda en el listado.
  Future<ClienteEnLista> guardar(FichaDeCliente ficha) async {
    final dynamic res = ficha.id == null
        ? await _http.post<dynamic>('/api/clientes', datos: ficha.aJson())
        : await _http.put<dynamic>('/api/clientes/${ficha.id}', datos: ficha.aJson());
    final guardado = ClienteEnLista.desdeJson((res as Map).cast<String, dynamic>());
    await _agregarAlCache(guardado);
    return guardado;
  }

  Future<void> _agregarAlCache(ClienteEnLista nuevo) async {
    try {
      final existentes = await enCache();
      final actualizados = [
        nuevo,
        ...existentes.where((c) => c.id != nuevo.id),
      ];
      await guardarEnCache(actualizados);
    } catch (_) {}
  }

  /// Deja la copia local lista para la próxima vez que no haya señal.
  Future<void> guardarEnCache(List<ClienteEnLista> clientes) async {
    try {
      await _db.into(_db.catalogos).insertOnConflictUpdate(CatalogosCompanion.insert(
            clave: _claveDeCache,
            tipo: const Value('clientes'),
            contenido: jsonEncode(clientes.map((c) => c.aJson()).toList()),
            actualizadoEn: DateTime.now(),
          ));
    } catch (_) {}
  }

  static List<ClienteEnLista> _parsear(String contenido) {
    final lista = jsonDecode(contenido) as List<dynamic>;
    return lista
        .map((e) => ClienteEnLista.desdeJson((e as Map).cast<String, dynamic>()))
        .toList();
  }
}
