import 'package:drift/drift.dart';

import 'base_local.dart';

/// Que se borra cuando el navegador avisa de que se acerca al limite de
/// almacenamiento.
///
/// La regla no se negocia: primero el catalogo cacheado —que siempre se puede
/// volver a bajar— y **nunca** la cola de sincronizacion, que es lo unico que
/// no esta en ningun otro lado (HU-110, criterio 4).
class PoliticaDePurga {
  const PoliticaDePurga(this._db);

  final BaseLocal _db;

  /// Deja como mucho [conservar] filas de catalogo, borrando las mas viejas
  /// primero (por `actualizadoEn`). Devuelve cuantas borro. No toca
  /// `operaciones_pendientes`.
  Future<int> purgarCatalogo({required int conservar}) async {
    assert(conservar >= 0);
    final total = await _contarCatalogos();
    final sobran = total - conservar;
    if (sobran <= 0) return 0;

    final aBorrar = await (_db.select(_db.catalogos)
          ..orderBy([(t) => OrderingTerm.asc(t.actualizadoEn)])
          ..limit(sobran))
        .get();
    final claves = aBorrar.map((c) => c.clave).toList();

    await (_db.delete(_db.catalogos)..where((t) => t.clave.isIn(claves))).go();
    return claves.length;
  }

  /// Purga todo el catalogo de un tipo (p. ej. 'productos' tras un cambio de
  /// negocio). La cola sigue intacta.
  Future<int> purgarTipo(String tipo) {
    return (_db.delete(_db.catalogos)..where((t) => t.tipo.equals(tipo))).go();
  }

  Future<int> _contarCatalogos() async {
    final consulta = _db.selectOnly(_db.catalogos)
      ..addColumns([_db.catalogos.clave.count()]);
    final fila = await consulta.getSingle();
    return fila.read(_db.catalogos.clave.count()) ?? 0;
  }
}
