import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

void main() {
  late BaseLocal db;

  setUp(() => db = BaseLocal(NativeDatabase.memory()));
  tearDown(() => db.close());

  Future<void> sembrarCatalogo(int cuantos) async {
    for (var i = 0; i < cuantos; i++) {
      await db.into(db.catalogos).insert(CatalogosCompanion.insert(
            clave: 'c$i',
            tipo: const Value('productos'),
            contenido: '[]',
            actualizadoEn: DateTime.utc(2026, 1, 1).add(Duration(minutes: i)),
          ));
    }
  }

  Future<void> encolar(int cuantas) async {
    for (var i = 0; i < cuantas; i++) {
      await db.into(db.operacionesPendientes).insert(OperacionesPendientesCompanion.insert(
            id: 'op$i',
            metodo: 'POST',
            ruta: '/api/ventas',
            creadoEn: DateTime.utc(2026),
          ));
    }
  }

  Future<int> catalogos() async =>
      (await db.select(db.catalogos).get()).length;
  Future<int> cola() async =>
      (await db.select(db.operacionesPendientes).get()).length;

  test('criterio 4: purgar catalogo deja las N mas nuevas y no toca la cola', () async {
    await sembrarCatalogo(5);
    await encolar(3);

    final borradas = await PoliticaDePurga(db).purgarCatalogo(conservar: 2);

    expect(borradas, 3);
    expect(await catalogos(), 2);
    expect(await cola(), 3, reason: 'la cola de sincronizacion es sagrada');

    final quedan = await db.select(db.catalogos).get();
    expect(quedan.map((c) => c.clave).toSet(), {'c3', 'c4'},
        reason: 'se conservan las mas recientes');
  });

  test('purgar cuando ya hay menos que el objetivo no borra nada', () async {
    await sembrarCatalogo(2);
    await encolar(1);

    expect(await PoliticaDePurga(db).purgarCatalogo(conservar: 10), 0);
    expect(await catalogos(), 2);
    expect(await cola(), 1);
  });

  test('purgar un tipo entero de catalogo no toca la cola', () async {
    await sembrarCatalogo(4);
    await db.into(db.catalogos).insert(CatalogosCompanion.insert(
          clave: 'ajuste',
          tipo: const Value('config'),
          contenido: '{}',
          actualizadoEn: DateTime.utc(2026),
        ));
    await encolar(2);

    final borradas = await PoliticaDePurga(db).purgarTipo('productos');

    expect(borradas, 4);
    expect(await catalogos(), 1);
    expect(await cola(), 2);
  });

  test('purgar con la cola vacia y sin catalogo no revienta', () async {
    expect(await PoliticaDePurga(db).purgarCatalogo(conservar: 0), 0);
  });
}
