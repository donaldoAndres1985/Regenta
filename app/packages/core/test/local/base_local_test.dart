import 'package:drift/drift.dart';
import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:sqlite3/sqlite3.dart';

void main() {
  test('criterio 1/2: el mismo esquema Dart abre y opera una base', () async {
    final db = BaseLocal(NativeDatabase.memory());
    addTearDown(db.close);

    await db.into(db.operacionesPendientes).insert(OperacionesPendientesCompanion.insert(
          id: 'op-1',
          metodo: 'POST',
          ruta: '/api/ventas',
          creadoEn: DateTime.utc(2026),
        ));
    final ops = await db.select(db.operacionesPendientes).get();

    expect(ops.single.estado, 'PENDIENTE');
    expect(ops.single.intentos, 0);
  });

  test('criterio 3: al migrar el esquema, la cola de sincronizacion sobrevive', () async {
    // Base "v1": catalogos sin la columna `tipo`, y una operacion ya encolada.
    final crudo = sqlite3.openInMemory();
    crudo.execute('''
      CREATE TABLE operaciones_pendientes (
        id TEXT NOT NULL PRIMARY KEY, metodo TEXT NOT NULL, ruta TEXT NOT NULL,
        cuerpo TEXT, creado_en INTEGER NOT NULL,
        intentos INTEGER NOT NULL DEFAULT 0, estado TEXT NOT NULL DEFAULT 'PENDIENTE');
      CREATE TABLE catalogos (
        clave TEXT NOT NULL PRIMARY KEY, contenido TEXT NOT NULL,
        actualizado_en INTEGER NOT NULL);
      CREATE TABLE meta_local (clave TEXT NOT NULL PRIMARY KEY, valor TEXT NOT NULL);
      INSERT INTO operaciones_pendientes (id, metodo, ruta, creado_en)
        VALUES ('venta-sin-senal', 'POST', '/api/ventas', 0);
      PRAGMA user_version = 1;
    ''');

    // Se abre con el esquema actual (v2): drift corre onUpgrade(1, 2).
    final db = BaseLocal(NativeDatabase.opened(crudo));
    addTearDown(db.close);

    final ops = await db.select(db.operacionesPendientes).get();
    expect(ops.single.id, 'venta-sin-senal',
        reason: 'la operacion encolada no se pierde al migrar');

    // Y la columna nueva ya existe: se puede insertar con `tipo`.
    await db.into(db.catalogos).insert(CatalogosCompanion.insert(
          clave: 'productos',
          contenido: '[]',
          actualizadoEn: DateTime.utc(2026),
          tipo: const Value('productos'),
        ));
    final cat = await db.select(db.catalogos).getSingle();
    expect(cat.tipo, 'productos');
  });
}
