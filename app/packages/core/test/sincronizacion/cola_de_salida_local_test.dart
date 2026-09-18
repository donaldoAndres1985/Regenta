import 'package:drift/native.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

void main() {
  late BaseLocal db;

  setUp(() => db = BaseLocal(NativeDatabase.memory()));
  tearDown(() => db.close());

  test('encolar guarda metodo, ruta, datos y query para reproducirla despues', () async {
    final cola = ColaDeSalidaLocal(db);

    await cola.encolar(const OperacionEncolable(
      metodo: 'POST',
      ruta: '/api/ventas',
      datos: {'total': 64000},
      query: {'sucursalId': 'principal'},
    ));

    final filas = await db.select(db.operacionesPendientes).get();
    expect(filas, hasLength(1));
    expect(filas.single.metodo, 'POST');
    expect(filas.single.ruta, '/api/ventas');
    expect(filas.single.estado, 'PENDIENTE');
    expect(filas.single.cuerpo, contains('64000'));
    expect(filas.single.cuerpo, contains('principal'));
  });

  test('cada operacion encolada tiene su propio id: no se pisan entre si', () async {
    final cola = ColaDeSalidaLocal(db);

    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));

    final filas = await db.select(db.operacionesPendientes).get();
    expect(filas, hasLength(2));
    expect(filas.map((f) => f.id).toSet(), hasLength(2));
  });
}
