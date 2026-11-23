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

  test('HU-182 · con clave, encolarla otra vez la reemplaza y vuelve a PENDIENTE sin el error', () async {
    final cola = ColaDeSalidaLocal(db);
    const ruta = '/api/compras/recepciones/dispositivo/r-1';
    await cola.encolar(const OperacionEncolable(metodo: 'PUT', ruta: ruta, datos: {'cantidad': 11}, clave: 'r-1'));
    await cola.marcarRechazada('r-1', const ErroresDeValidacion({}, 'Pasa la tolerancia'));
    expect((await cola.operaciones()).single.ultimoError, 'Pasa la tolerancia');

    await cola.encolar(const OperacionEncolable(metodo: 'PUT', ruta: ruta, datos: {'cantidad': 10}, clave: 'r-1'));

    final [op] = await cola.operaciones();
    expect(op.id, 'r-1');
    expect(op.porEnviar, isTrue);
    expect(op.ultimoError, isNull);
    expect(op.datos, {'cantidad': 10});
  });

  test('HU-182 · se listan por el comienzo de la ruta y se quitan', () async {
    final cola = ColaDeSalidaLocal(db);
    await cola.encolar(const OperacionEncolable(metodo: 'PUT', ruta: '/api/compras/recepciones/dispositivo/a', clave: 'a'));
    await cola.encolar(const OperacionEncolable(metodo: 'POST', ruta: '/api/ventas'));

    expect((await cola.operaciones(prefijo: '/api/compras/recepciones/dispositivo/')).map((o) => o.id), ['a']);

    await cola.quitar('a');
    expect(await cola.operaciones(prefijo: '/api/compras/'), isEmpty);
    expect(await cola.operaciones(), hasLength(1));
  });

  test('HU-182 · el motivo junta los mensajes por campo', () {
    expect(motivoDelRechazo(const ErroresDeValidacion({'lineas[0].codigoLote': ['Falta el lote']}, 'Hay campos')),
        'Falta el lote');
    expect(motivoDelRechazo(const RecursoDuplicado('Ya está')), 'Ya está');
  });
}
