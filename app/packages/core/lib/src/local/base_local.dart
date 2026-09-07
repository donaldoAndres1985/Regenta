import 'package:drift/drift.dart';
import 'package:drift_flutter/drift_flutter.dart';

part 'base_local.g.dart';

/// Read-model cacheado: catalogos, listas, lo que siempre se puede volver a
/// bajar del servidor. Es lo que se purga primero cuando falta espacio.
class Catalogos extends Table {
  TextColumn get clave => text()();
  TextColumn get tipo => text().withDefault(const Constant('generico'))();
  TextColumn get contenido => text()();
  DateTimeColumn get actualizadoEn => dateTime()();

  @override
  Set<Column<Object>> get primaryKey => {clave};
}

/// La cola de sincronizacion. Lo unico que NO se puede volver a generar: una
/// venta que se registro sin senal no esta en ningun otro lado. No se purga
/// nunca, pase lo que pase con el esquema o con el espacio.
class OperacionesPendientes extends Table {
  TextColumn get id => text()();
  TextColumn get metodo => text()();
  TextColumn get ruta => text()();
  TextColumn get cuerpo => text().nullable()();
  DateTimeColumn get creadoEn => dateTime()();
  IntColumn get intentos => integer().withDefault(const Constant(0))();
  TextColumn get estado => text().withDefault(const Constant('PENDIENTE'))();

  @override
  Set<Column<Object>> get primaryKey => {id};
}

/// Pares clave/valor para lo del propio almacen (version de esquema aplicada,
/// marca del ultimo pull incremental, etc).
class MetaLocal extends Table {
  TextColumn get clave => text()();
  TextColumn get valor => text()();

  @override
  Set<Column<Object>> get primaryKey => {clave};
}

/// La base local de Regenta. El mismo esquema y el mismo codigo Dart en Android
/// (SQLite nativo) y en Web (`sqlite3.wasm`); el reparto por plataforma lo hace
/// [abrirBaseLocal].
@DriftDatabase(tables: [Catalogos, OperacionesPendientes, MetaLocal])
class BaseLocal extends _$BaseLocal {
  BaseLocal(super.executor);

  /// Para tests: una base en memoria, sin plataforma.
  factory BaseLocal.enMemoria() =>
      BaseLocal(driftDatabase(name: 'regenta_test', native: const DriftNativeOptions()));

  @override
  int get schemaVersion => 2;

  @override
  MigrationStrategy get migration => MigrationStrategy(
        onCreate: (m) => m.createAll(),
        onUpgrade: (m, from, to) async {
          // La cola de sincronizacion sobrevive a cualquier cambio de esquema:
          // aqui solo se tocan las tablas de catalogo y de meta.
          if (from < 2) {
            await m.addColumn(catalogos, catalogos.tipo);
          }
        },
        beforeOpen: (details) async {
          await customStatement('PRAGMA foreign_keys = ON');
        },
      );
}

/// Abre la base real. `drift_flutter` elige SQLite nativo en Android y
/// `sqlite3.wasm` en Web, con el mismo esquema.
BaseLocal abrirBaseLocal({String nombre = 'regenta'}) {
  return BaseLocal(driftDatabase(
    name: nombre,
    web: DriftWebOptions(
      sqlite3Wasm: Uri.parse('sqlite3.wasm'),
      driftWorker: Uri.parse('drift_worker.js'),
    ),
  ));
}
