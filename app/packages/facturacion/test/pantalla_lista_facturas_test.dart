import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
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
