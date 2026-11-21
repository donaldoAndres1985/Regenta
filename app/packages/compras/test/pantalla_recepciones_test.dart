import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

/// Recepciones y devoluciones en memoria, con el cálculo de la nota de HU-178.
class _RepoFake implements RepositorioDeDevoluciones {
  RecepcionVista recepcionDeReferencia = const RecepcionVista(
    id: 'r-1',
    numero: 'REC-1',
    estado: 'CONFIRMADA',
    total: 248000,
    proveedorNombre: 'Andina',
    facturaProveedor: 'FV-100',
    lineas: [
      LineaRecibidaVista(id: 'la', productoId: 'a', nombre: 'Acetaminofén', cantidad: 10, codigoLote: 'L-1',
          costoEntrada: 10500),
      LineaRecibidaVista(id: 'lb', productoId: 'b', nombre: 'Ibuprofeno', cantidad: 5, costoEntrada: 21000),
    ],
  );
  final List<DevolucionVista> hechas = [];
  final List<(String, Map<String, num>)> pedidas = [];
  final List<String> anuladas = [];
  ErrorDeApi? falla;

  @override
  Future<List<RecepcionVista>> recepciones() async => [recepcionDeReferencia];

  @override
  Future<RecepcionVista> recepcion(String id) async => recepcionDeReferencia;

  @override
  Future<List<DevolucionVista>> devolucionesDe(String recepcionId) async => [...hechas];

  @override
  Future<DevolucionVista> devolver(String recepcionId, String motivo, Map<String, num> cantidades) async {
    if (falla != null) throw falla!;
    pedidas.add((motivo, {for (final e in cantidades.entries) if (e.value > 0) e.key: e.value}));
    final d = DevolucionVista(
      id: 'd-${hechas.length + 1}', numero: 'DEV-${hechas.length + 1}', motivo: motivo.trim(), valorNota: 23800,
      aplicadoACuenta: 23800, devueltoPorLinea: {for (final e in cantidades.entries) if (e.value > 0) e.key: e.value},
    );
    hechas.add(d);
    return d;
  }

  @override
  Future<RecepcionVista> anular(String recepcionId, String motivo) async {
    if (falla != null) throw falla!;
    anuladas.add(motivo);
    return RecepcionVista(
      id: 'r-1', numero: 'REC-1', estado: 'ANULADA', total: 248000, motivoAnulacion: motivo,
      lineas: recepcionDeReferencia.lineas,
    );
  }
}

Future<_RepoFake> _abrir(WidgetTester tester, {Size tamano = const Size(1280, 1000)}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = _RepoFake();
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeDevolucionesProvider.overrideWithValue(repo)],
    child: const MaterialApp(home: PantallaRecepciones()),
  ));
  await tester.pumpAndSettle();
  await tester.tap(find.byKey(const Key('recepcion-fila-r-1')));
  await tester.pumpAndSettle();
  return repo;
}

Future<void> _devolver(WidgetTester tester, {String? cantidadA, String motivo = 'Averiadas'}) async {
  await tester.tap(find.byKey(const Key('recepcion-devolver')));
  await tester.pumpAndSettle();
  if (cantidadA != null) await tester.enterText(find.byKey(const Key('devolver-cantidad-la')), cantidadA);
  await tester.enterText(find.byKey(const Key('devolver-motivo')), motivo);
  await tester.tap(find.byKey(const Key('devolver-registrar')));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('HU-178 · Se listan las recepciones y se abre una con su lote y su costo de entrada', (tester) async {
    await _abrir(tester);

    expect(find.text('Andina'), findsOneWidget);
    expect(find.textContaining('lote L-1'), findsOneWidget);
    expect(find.textContaining('entró a \$10.500'), findsOneWidget);
  });

  testWidgets('HU-178 C1 / C6 · Devolver 2 und de A (de su lote): se pide al servidor y se ve la nota', (tester) async {
    final repo = await _abrir(tester);

    await _devolver(tester, cantidadA: '2');

    expect(repo.pedidas.single.$2, {'la': 2});
    expect(find.text('Devolución DEV-1: nota crédito por \$23.800.'), findsOneWidget);
    expect(find.text('recibido 10 · devuelto 2'), findsOneWidget);
    expect(find.textContaining('rebajó \$23.800 de la factura'), findsOneWidget);
    expect(find.byKey(const Key('recepcion-anular')), findsNothing, reason: 'con devoluciones ya no se anula');
  });

  testWidgets('HU-178 C4 · Más de lo que queda por devolver no se deja registrar', (tester) async {
    final repo = await _abrir(tester);
    await _devolver(tester, cantidadA: '8');

    await _devolver(tester, cantidadA: '3');

    expect(find.text('De «Acetaminofén» se pueden devolver 2.'), findsOneWidget);
    expect(repo.pedidas, hasLength(1));
  });

  testWidgets('HU-178 C5 · Si el servidor responde que no hay existencia, se ve el motivo', (tester) async {
    final repo = await _abrir(tester);
    repo.falla = const RecursoDuplicado('No hay en la bodega lo que se quiere devolver: De «Acetaminofén» quedan 1');

    await _devolver(tester, cantidadA: '2');

    expect(find.textContaining('quedan 1'), findsOneWidget);
    expect(find.byKey(const Key('devolver-registrar')), findsOneWidget, reason: 'el diálogo sigue abierto');
  });

  testWidgets('HU-178 · Sin cantidades no se devuelve nada', (tester) async {
    final repo = await _abrir(tester);

    await _devolver(tester);

    expect(find.text('Escribe cuánto devuelves de algún producto.'), findsOneWidget);
    expect(repo.pedidas, isEmpty);
  });

  testWidgets('HU-177 · Anular la recepción con su motivo', (tester) async {
    final repo = await _abrir(tester);

    await tester.tap(find.byKey(const Key('recepcion-anular')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('anular-recepcion-motivo')), 'Se registró dos veces');
    await tester.pump();
    await tester.tap(find.byKey(const Key('anular-recepcion-confirmar')));
    await tester.pumpAndSettle();

    expect(repo.anuladas, ['Se registró dos veces']);
    expect(find.text('Anulada: Se registró dos veces'), findsOneWidget);
    expect(find.byKey(const Key('recepcion-devolver')), findsNothing);
  });

  testWidgets('En el celular la recepción y el diálogo caben sin desbordarse', (tester) async {
    await _abrir(tester, tamano: const Size(390, 844));
    await tester.tap(find.byKey(const Key('recepcion-devolver')));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
  });
}
