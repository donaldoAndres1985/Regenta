import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';

class _RepoComprasFake implements RepositorioDeRecepciones {
  _RepoComprasFake(this._ordenes);

  final List<OrdenResumenVista> _ordenes;

  @override
  Future<List<OrdenResumenVista>> listarOrdenes() async => _ordenes;

  @override
  Future<OrdenRecibible> verOrden(String ordenId) {
    throw UnimplementedError();
  }

  @override
  Future<ResultadoDeRecepcion> registrar({
    required String ordenId,
    required String bodegaId,
    String? facturaProveedor,
    required List<LineaParaRecibir> lineas,
  }) {
    throw UnimplementedError();
  }
}

void main() {
  testWidgets('Muestra la lista de órdenes de compra con sus datos',
      (tester) async {
    final fakeOrdenes = [
      const OrdenResumenVista(
        id: 'oc-1',
        numero: 'OC-001',
        estado: 'APROBADA',
        total: 540000,
        fechaEmision: '2026-11-08',
      ),
      const OrdenResumenVista(
        id: 'oc-2',
        numero: 'OC-002',
        estado: 'RECIBIDA',
        total: 120000,
        fechaEmision: '2026-11-07',
      ),
    ];

    String? ordenSeleccionada;

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeRecepcionesProvider.overrideWithValue(_RepoComprasFake(fakeOrdenes)),
        ],
        child: MaterialApp(
          home: PantallaCompras(
            onOrdenSeleccionada: (id) => ordenSeleccionada = id,
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('OC-001'), findsOneWidget);
    expect(find.text('OC-002'), findsOneWidget);
    expect(find.text('APROBADA'), findsOneWidget);
    expect(find.text('RECIBIDA'), findsOneWidget);

    await tester.tap(find.text('Recibir').first);
    expect(ordenSeleccionada, equals('oc-1'));
  });

  testWidgets('Muestra mensaje cuando no hay órdenes de compra',
      (tester) async {
    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeRecepcionesProvider.overrideWithValue(_RepoComprasFake(const [])),
        ],
        child: MaterialApp(
          home: PantallaCompras(
            onOrdenSeleccionada: (_) {},
          ),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('No hay órdenes de compra registradas.'), findsOneWidget);
  });
}
