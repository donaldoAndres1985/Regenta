import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

class _FakeRepoVentas implements RepositorioDeVentas {
  final List<VentaEnProceso> ventas = [];
  String? ultimaVentaAnulada;
  String? ultimoMotivo;

  @override
  Future<List<VentaEnProceso>> buscarVentas({String? q}) async {
    if (q == null || q.trim().isEmpty) return ventas;
    return ventas.where((v) => v.coincideCon(q)).toList();
  }

  @override
  Future<void> anularVenta(String ventaId, String motivo) async {
    ultimaVentaAnulada = ventaId;
    ultimoMotivo = motivo;
    final idx = ventas.indexWhere((v) => v.id == ventaId);
    if (idx != -1) {
      final v = ventas[idx];
      ventas[idx] = VentaEnProceso(
        id: v.id,
        numero: v.numero,
        estado: 'ANULADA',
        total: v.total,
        facturada: v.facturada,
        clienteSnapshot: v.clienteSnapshot,
      );
    }
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  Widget crearApp(Widget child, _FakeRepoVentas repo) {
    return ProviderScope(
      overrides: [
        repositorioDeVentasProvider.overrideWithValue(repo),
      ],
      child: MaterialApp(
        home: child,
      ),
    );
  }

  testWidgets('PantallaBuscarVenta muestra ventas y anula con motivo obligatorio',
      (tester) async {
    final repo = _FakeRepoVentas();
    repo.ventas.add(
      const VentaEnProceso(
        id: 'v-101',
        numero: '001',
        estado: 'CONFIRMADA',
        total: 50000,
        clienteSnapshot: '{"nombreCompleto": "Carlos Perez"}',
      ),
    );

    await tester.pumpWidget(crearApp(const PantallaBuscarVenta(), repo));
    await tester.pumpAndSettle();

    // Se muestra la venta con su cliente y número
    expect(find.text('Venta #001'), findsOneWidget);
    expect(find.text('Cliente: Carlos Perez'), findsOneWidget);

    // Tocar botón anular
    await tester.tap(find.byKey(const Key('boton-anular-venta-v-101')));
    await tester.pumpAndSettle();

    // Diálogo de anulación visible
    expect(find.byKey(const Key('dialogo-anular-venta')), findsOneWidget);

    // Si confirmo sin motivo, muestra error y no anula
    await tester.tap(find.byKey(const Key('boton-confirmar-anulacion')));
    await tester.pumpAndSettle();

    expect(find.text('Indica el motivo de la anulación'), findsOneWidget);
    expect(repo.ultimaVentaAnulada, isNull);

    // Escribir motivo
    await tester.enterText(
        find.byKey(const Key('input-motivo-anulacion')), 'Cliente desistió de la compra');
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('boton-confirmar-anulacion')));
    await tester.pumpAndSettle();

    // Diálogo cerrado y venta anulada en el repo
    expect(find.byKey(const Key('dialogo-anular-venta')), findsNothing);
    expect(repo.ultimaVentaAnulada, 'v-101');
    expect(repo.ultimoMotivo, 'Cliente desistió de la compra');
    expect(find.text('Venta 001 anulada. El stock fue reintegrado al inventario.'), findsOneWidget);
  });

  testWidgets('Venta facturada no permite anulación directa y avisa nota crédito DIAN',
      (tester) async {
    final repo = _FakeRepoVentas();
    repo.ventas.add(
      const VentaEnProceso(
        id: 'v-102',
        numero: '002',
        estado: 'CONFIRMADA',
        total: 80000,
        facturada: true,
      ),
    );

    await tester.pumpWidget(crearApp(const PantallaBuscarVenta(), repo));
    await tester.pumpAndSettle();

    expect(find.text('DIAN'), findsOneWidget);

    await tester.tap(find.byKey(const Key('boton-anular-venta-v-102')));
    await tester.pumpAndSettle();

    // No debe abrir el diálogo
    expect(find.byKey(const Key('dialogo-anular-venta')), findsNothing);
    expect(
      find.text('La venta 002 ya fue facturada electrónicamente: emite una nota crédito para revertirla'),
      findsOneWidget,
    );
  });

  testWidgets('PantallaBuscarVenta enfoca el buscador al abrir y cierra con Escape',
      (tester) async {
    final repo = _FakeRepoVentas();
    var cerro = false;

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeVentasProvider.overrideWithValue(repo),
        ],
        child: MaterialApp(
          home: Builder(
            builder: (ctx) => Scaffold(
              body: ElevatedButton(
                onPressed: () async {
                  await Navigator.of(ctx).push(
                    MaterialPageRoute(builder: (_) => const PantallaBuscarVenta()),
                  );
                  cerro = true;
                },
                child: const Text('Abrir'),
              ),
            ),
          ),
        ),
      ),
    );

    await tester.tap(find.text('Abrir'));
    await tester.pumpAndSettle();

    final inputFinder = find.byKey(const Key('buscar-venta-input'));
    expect(inputFinder, findsOneWidget);

    final editable = tester.widget<EditableText>(find.descendant(
      of: inputFinder,
      matching: find.byType(EditableText),
    ));
    expect(editable.focusNode.hasFocus, isTrue);

    // Pulsar Escape para cerrar
    await tester.sendKeyEvent(LogicalKeyboardKey.escape);
    await tester.pumpAndSettle();

    expect(cerro, isTrue);
  });

  testWidgets('PantallaBuscarVenta filtra multi-palabra en cualquier orden',
      (tester) async {
    final repo = _FakeRepoVentas();
    repo.ventas.addAll([
      const VentaEnProceso(
        id: 'v-1',
        numero: '001',
        estado: 'CONFIRMADA',
        total: 10000,
        clienteSnapshot: '{"nombreCompleto": "Carlos Alberto Perez"}',
      ),
      const VentaEnProceso(
        id: 'v-2',
        numero: '002',
        estado: 'CONFIRMADA',
        total: 20000,
        clienteSnapshot: '{"nombreCompleto": "Adelmo Carrascal"}',
      ),
    ]);

    await tester.pumpWidget(crearApp(const PantallaBuscarVenta(), repo));
    await tester.pumpAndSettle();

    // Inicialmente ambas ventas están visibles
    expect(find.text('Venta #001'), findsOneWidget);
    expect(find.text('Venta #002'), findsOneWidget);

    // Búsqueda multi-palabra en cualquier orden: "perez 001"
    await tester.enterText(find.byKey(const Key('buscar-venta-input')), 'perez 001');
    await tester.pump(const Duration(milliseconds: 250));
    await tester.pumpAndSettle();

    expect(find.text('Venta #001'), findsOneWidget);
    expect(find.text('Venta #002'), findsNothing);

    // Búsqueda multi-palabra con texto intermedio: "adel carra"
    await tester.enterText(find.byKey(const Key('buscar-venta-input')), 'adel carra');
    await tester.pump(const Duration(milliseconds: 250));
    await tester.pumpAndSettle();

    expect(find.text('Venta #001'), findsNothing);
    expect(find.text('Venta #002'), findsOneWidget);
  });
}
