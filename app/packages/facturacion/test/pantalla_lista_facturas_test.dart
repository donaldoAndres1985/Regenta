import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_facturacion/regenta_facturacion.dart';

class _RepoFake implements RepositorioDeFacturas {
  _RepoFake(this._facturas);

  final List<FacturaResumenVista> _facturas;

  @override
  Future<List<FacturaResumenVista>> listar() async => _facturas;

  @override
  Future<FacturaVista> ver(String facturaId) {
    throw UnimplementedError();
  }

  @override
  Future<String> enviarPorCorreo(String facturaId, {String? correo}) {
    throw UnimplementedError();
  }
}

void main() {
  testWidgets('Encabezado E1-E5 · en web, el encabezado común con ícono; en móvil, la barra', (tester) async {
    Future<void> montar(Size tamano) async {
      await tester.binding.setSurfaceSize(tamano);
      addTearDown(() => tester.binding.setSurfaceSize(null));
      await tester.pumpWidget(ProviderScope(
        overrides: [repositorioDeFacturasProvider.overrideWithValue(_RepoFake(const []))],
        child: MaterialApp(home: PantallaListaFacturas(onFacturaSeleccionada: (_) {})),
      ));
      await tester.pumpAndSettle();
    }

    await montar(const Size(1440, 900));
    final encabezado = find.byType(EncabezadoDeSeccion);
    expect(encabezado, findsOneWidget);
    expect(find.descendant(of: encabezado, matching: find.byIcon(Icons.receipt_long_outlined)), findsOneWidget);
    expect(find.descendant(of: encabezado, matching: find.text('Facturas Electrónicas')), findsOneWidget);

    await montar(const Size(390, 844));
    expect(find.byType(EncabezadoDeSeccion), findsNothing);
    expect(find.byType(AppBar), findsOneWidget);
  });

  testWidgets('Muestra la lista de facturas con sus números y totales',
      (tester) async {
    final fakeFacturas = [
      const FacturaResumenVista(
        id: 'fac-1',
        numeroCompleto: 'FE-1001',
        origenTipo: 'VENTA',
        estado: 'ACEPTADA',
        total: 150000,
      ),
      const FacturaResumenVista(
        id: 'fac-2',
        numeroCompleto: 'FE-1002',
        origenTipo: 'COMANDA',
        estado: 'RECHAZADA',
        total: 45000,
      ),
    ];

    String? facturaSeleccionada;

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeFacturasProvider.overrideWithValue(_RepoFake(fakeFacturas)),
        ],
        child: MaterialApp(
          home: PantallaListaFacturas(
            onFacturaSeleccionada: (id) => facturaSeleccionada = id,
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('FE-1001'), findsOneWidget);
    expect(find.text('FE-1002'), findsOneWidget);
    expect(find.text('ACEPTADA'), findsOneWidget);
    expect(find.text('RECHAZADA'), findsOneWidget);

    await tester.tap(find.text('FE-1001'));
    expect(facturaSeleccionada, equals('fac-1'));
  });

  testWidgets('Muestra mensaje de lista vacía si no hay facturas',
      (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeFacturasProvider.overrideWithValue(_RepoFake(const [])),
        ],
        child: MaterialApp(
          home: PantallaListaFacturas(
            onFacturaSeleccionada: (_) {},
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('No hay facturas emitidas todavía.'), findsOneWidget);
  });
}
