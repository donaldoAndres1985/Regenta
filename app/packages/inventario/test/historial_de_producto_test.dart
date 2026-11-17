import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// HU-160 · historial de movimientos del producto.
class _Repo implements RepositorioDeInventario {
  final List<(DateTime?, DateTime?, String?)> consultas = [];

  @override
  Future<List<MovimientoDeHistorial>> historialDe(String productoId,
      {DateTime? desde, DateTime? hasta, String? bodegaId}) async {
    consultas.add((desde, hasta, bodegaId));
    return [
      MovimientoDeHistorial.desdeJson({
        'id': 'm2', 'ocurridoEn': '2026-11-10T15:00:00Z', 'bodegaId': 'b1', 'tipo': 'SALIDA_AJUSTE',
        'cantidad': -5, 'saldoPosterior': 3, 'motivo': 'Avería: se cayó', 'usuario': 'Ana Pérez',
      }),
      MovimientoDeHistorial.desdeJson({
        'id': 'm1', 'ocurridoEn': '2026-11-01T15:00:00Z', 'bodegaId': 'b1', 'tipo': 'ENTRADA_COMPRA',
        'cantidad': 10, 'saldoPosterior': 10, 'motivo': 'Compra FC-1', 'usuario': 'Ana Pérez',
      }),
    ];
  }

  @override
  Future<List<UbicacionEnBodega>> ubicacionesDe(String id) async =>
      const [UbicacionEnBodega(bodegaId: 'b1', bodegaNombre: 'Principal')];

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

Future<_Repo> _montar(WidgetTester tester) async {
  tester.view.physicalSize = const Size(1280, 1000);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  final repo = _Repo();
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeInventarioProvider.overrideWithValue(repo)],
    child: const MaterialApp(
        home: PantallaHistorialDeProducto(productoId: 'p1', nombre: 'Barniz galón')),
  ));
  await tester.pumpAndSettle();
  return repo;
}

void main() {
  testWidgets('HU-160 C1 · cada movimiento con tipo, cantidad con signo, saldo, motivo y usuario, del más nuevo al más viejo', (tester) async {
    await _montar(tester);

    expect(find.text('Ajuste (salida)'), findsOneWidget);
    expect(find.text('-5'), findsOneWidget);
    expect(find.text('+10'), findsOneWidget);
    expect(find.text('Saldo 3'), findsOneWidget);
    expect(find.textContaining('Avería: se cayó'), findsOneWidget);
    expect(find.textContaining('Ana Pérez'), findsNWidgets(2));
    final yAveria = tester.getTopLeft(find.text('-5')).dy;
    final yCompra = tester.getTopLeft(find.text('+10')).dy;
    expect(yAveria < yCompra, isTrue, reason: 'el más nuevo arriba');
  });

  testWidgets('HU-160 C2 · se filtra por rango de fechas y por bodega', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('historial-30-dias')));
    await tester.pumpAndSettle();
    expect(repo.consultas.last.$1, isNotNull);

    await tester.tap(find.byKey(const Key('historial-bodega')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Principal').last);
    await tester.pumpAndSettle();
    expect(repo.consultas.last.$3, 'b1');
  });

  testWidgets('HU-160 C3 · no hay forma de editar ni borrar un movimiento', (tester) async {
    await _montar(tester);

    expect(find.byIcon(Icons.delete_outline), findsNothing);
    expect(find.byIcon(Icons.edit_outlined), findsNothing);
  });
}
