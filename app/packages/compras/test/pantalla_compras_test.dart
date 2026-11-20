import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

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
  testWidgets('Encabezado E1-E5 · en web, el encabezado común sin ícono; en móvil, la barra', (tester) async {
    Future<void> montar(Size tamano) async {
      await tester.binding.setSurfaceSize(tamano);
      addTearDown(() => tester.binding.setSurfaceSize(null));
      await tester.pumpWidget(ProviderScope(
        overrides: [repositorioDeRecepcionesProvider.overrideWithValue(_RepoComprasFake(const []))],
        child: MaterialApp(home: PantallaCompras(onOrdenSeleccionada: (_) {})),
      ));
      await tester.pumpAndSettle();
    }

    await montar(const Size(1440, 900));
    final encabezado = find.byType(EncabezadoDeSeccion);
    expect(encabezado, findsOneWidget);
    expect(find.descendant(of: encabezado, matching: find.byIcon(Icons.local_shipping_outlined)), findsNothing);
    expect(find.descendant(of: encabezado, matching: find.text('Órdenes de Compra')), findsOneWidget);
    expect(find.descendant(of: encabezado, matching: find.byTooltip('Nueva Orden')), findsOneWidget);

    await montar(const Size(390, 844));
    expect(find.byType(EncabezadoDeSeccion), findsNothing);
    expect(find.byType(AppBar), findsOneWidget);
  });

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

  testWidgets('HU-166 · El botón Proveedores abre el listado de proveedores', (tester) async {
    await tester.binding.setSurfaceSize(const Size(1440, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    var abiertos = 0;
    await tester.pumpWidget(ProviderScope(
      overrides: [repositorioDeRecepcionesProvider.overrideWithValue(_RepoComprasFake(const []))],
      child: MaterialApp(
        home: PantallaCompras(onOrdenSeleccionada: (_) {}, onProveedores: () => abiertos++),
      ),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('compras-proveedores')));
    expect(abiertos, 1);
  });
}
