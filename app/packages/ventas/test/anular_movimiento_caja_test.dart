import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// HU-141 C10 · anular un movimiento manual desde la lista del turno.
class _RepoCajaFake implements RepositorioDeCaja {
  final _movs = <MovimientoCajaVista>[
    const MovimientoCajaVista(
        id: 'mov-base', sesionId: 's-1', tipo: 'APERTURA', monto: 50000,
        concepto: 'Base inicial', fecha: '2026-11-14T08:00:00Z'),
    const MovimientoCajaVista(
        id: 'mov-venta', sesionId: 's-1', tipo: 'VENTA', monto: 30000,
        concepto: 'Venta FV-1', fecha: '2026-11-14T09:00:00Z'),
    const MovimientoCajaVista(
        id: 'mov-retiro', sesionId: 's-1', tipo: 'RETIRO', monto: 40000,
        concepto: 'Consignación', fecha: '2026-11-14T10:00:00Z'),
  ];
  String? anulado;
  String? motivo;

  @override
  Future<List<CajaVista>> listarCajas() async =>
      const [CajaVista(id: 'caja-1', codigo: 'CAJA-01', nombre: 'Caja Principal')];

  @override
  Future<SesionCajaActiva?> consultarActiva(String cajaId) async => const SesionCajaActiva(
      id: 's-1', cajaId: 'caja-1', cajeroId: 'c-1', fechaApertura: '2026-11-14T08:00:00Z',
      montoApertura: 50000, estado: 'ABIERTA');

  @override
  Future<List<MovimientoCajaVista>> movimientos(String sesionId) async => List.of(_movs);

  @override
  Future<MovimientoCajaVista> anularMovimiento({
    required String sesionId,
    required String movimientoId,
    required String motivo,
  }) async {
    anulado = movimientoId;
    this.motivo = motivo;
    final anulacion = MovimientoCajaVista(
        id: 'mov-anul', sesionId: sesionId, tipo: 'ANULACION', monto: 40000,
        concepto: 'Anulación: $motivo', fecha: '2026-11-14T11:00:00Z', anulaA: movimientoId);
    _movs.add(anulacion);
    return anulacion;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<void> _montar(WidgetTester tester, _RepoCajaFake repo, {required bool puedeAnular}) async {
  tester.view.physicalSize = const Size(1400, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        repositorioDeCajaProvider.overrideWithValue(repo),
        puedeAnularMovimientosDeCajaProvider.overrideWithValue(puedeAnular),
      ],
      child: const MaterialApp(home: PantallaCaja()),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets(
      'HU-141 C10 · Con el permiso, veo «Anular» solo en los movimientos manuales, '
      'pide motivo y el anulado queda marcado', (tester) async {
    final repo = _RepoCajaFake();
    await _montar(tester, repo, puedeAnular: true);

    expect(find.byKey(const ValueKey('anular-mov-retiro')), findsOneWidget);
    expect(find.byKey(const ValueKey('anular-mov-venta')), findsNothing);
    expect(find.byKey(const ValueKey('anular-mov-base')), findsNothing);

    await tester.tap(find.byKey(const ValueKey('anular-mov-retiro')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const ValueKey('motivo-anulacion')), 'Se registró dos veces');
    await tester.pump(); // el botón se habilita al haber motivo
    await tester.tap(find.text('Confirmar anulación'));
    await tester.pumpAndSettle();

    expect(repo.anulado, 'mov-retiro');
    expect(repo.motivo, 'Se registró dos veces');
    expect(find.byKey(const ValueKey('anulado-mov-retiro')), findsOneWidget);
    expect(find.byKey(const ValueKey('anular-mov-retiro')), findsNothing);
  });

  testWidgets(
      'HU-141 C1 · Dado un retiro anulado, el efectivo esperado del turno vuelve a como estaba',
      (tester) async {
    final repo = _RepoCajaFake();
    final contenedor = ProviderContainer(overrides: [
      repositorioDeCajaProvider.overrideWithValue(repo),
    ]);
    addTearDown(contenedor.dispose);
    final ctrl = contenedor.read(controladorDeCajaProvider.notifier);
    await ctrl.cargar();
    // Base 50.000 + venta 30.000 - retiro 40.000.
    expect(contenedor.read(controladorDeCajaProvider).efectivoEsperado, 40000);

    await ctrl.anularMovimiento('mov-retiro', 'Duplicado');

    expect(contenedor.read(controladorDeCajaProvider).efectivoEsperado, 80000);
  });

  testWidgets('HU-141 C10 · Sin motivo, no se puede confirmar la anulación', (tester) async {
    final repo = _RepoCajaFake();
    await _montar(tester, repo, puedeAnular: true);

    await tester.tap(find.byKey(const ValueKey('anular-mov-retiro')));
    await tester.pumpAndSettle();

    final boton = find.widgetWithText(FilledButton, 'Confirmar anulación');
    expect(tester.widget<FilledButton>(boton).onPressed, isNull);
  });

  testWidgets('HU-141 C2 · Sin el permiso, no veo «Anular» en ningún movimiento', (tester) async {
    await _montar(tester, _RepoCajaFake(), puedeAnular: false);

    expect(find.byKey(const ValueKey('anular-mov-retiro')), findsNothing);
  });
}
