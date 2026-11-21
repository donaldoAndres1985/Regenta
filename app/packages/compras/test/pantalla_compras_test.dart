import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

import 'ayudas_de_ordenes.dart';

void main() {
  testWidgets('Encabezado E1-E5 · en web, el encabezado común sin ícono; en móvil, la barra', (tester) async {
    await montarCompras(tester, const []);
    final encabezado = find.byType(EncabezadoDeSeccion);
    expect(encabezado, findsOneWidget);
    expect(find.descendant(of: encabezado, matching: find.text('Órdenes de Compra')), findsOneWidget);

    await montarCompras(tester, const [], tamano: const Size(390, 844));
    expect(find.byType(EncabezadoDeSeccion), findsNothing);
    expect(find.byType(AppBar), findsOneWidget);
  });

  testWidgets('HU-174 C1 · El listado muestra número, proveedor, estado, fecha esperada y total, sin «Generar Orden Demo»',
      (tester) async {
    await montarCompras(tester, [orden('a', 'OC-1', 'APROBADA', total: 3570000)]);

    expect(find.text('OC-1 · Andina'), findsOneWidget);
    expect(find.text('APROBADA'), findsOneWidget);
    expect(find.text('para el 01/12'), findsOneWidget);
    expect(find.text('\$3.570.000'), findsOneWidget);
    expect(find.text('Generar Orden Demo'), findsNothing);
  });

  testWidgets('HU-174 C1 · Se filtra por estado y por proveedor', (tester) async {
    await montarCompras(tester, [
      orden('a', 'OC-1', 'APROBADA'),
      orden('b', 'OC-2', 'BORRADOR', espera: true),
      orden('c', 'OC-3', 'RECIBIDA', proveedorId: 'p-2', proveedor: 'Pinturas'),
    ]);

    await tester.tap(find.byKey(const Key('ordenes-filtro-porAprobar')));
    await tester.pumpAndSettle();
    expect(find.text('OC-2 · Andina'), findsOneWidget);
    expect(find.text('OC-1 · Andina'), findsNothing);

    await tester.tap(find.byKey(const Key('ordenes-filtro-todas')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('ordenes-filtro-proveedor')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Pinturas').last);
    await tester.pumpAndSettle();
    expect(find.text('OC-3 · Pinturas'), findsOneWidget);
    expect(find.text('OC-1 · Andina'), findsNothing);
  });

  testWidgets('Sin órdenes, lo dice', (tester) async {
    await montarCompras(tester, const []);

    expect(find.text('Todavía no hay órdenes de compra.'), findsOneWidget);
  });

  testWidgets('HU-166 / HU-167 · Los atajos abren proveedores y compra directa', (tester) async {
    var proveedores = 0;
    var directa = 0;
    await montarCompras(tester, const [], onProveedores: () => proveedores++, onCompraDirecta: () => directa++);

    await tester.tap(find.byKey(const Key('compras-proveedores')));
    await tester.tap(find.byKey(const Key('compras-compra-directa')));
    expect(proveedores, 1);
    expect(directa, 1);
  });

  testWidgets('HU-167 · En el celular los atajos son solo íconos y la barra no se desborda', (tester) async {
    await montarCompras(tester, [orden('a', 'OC-1', 'APROBADA')],
        tamano: const Size(390, 844),
        onCompraDirecta: () {},
        onProveedores: () {},
        onCuentasPorPagar: () {},
        onConfiguracion: () {});

    expect(find.byTooltip('Compra directa'), findsOneWidget);
    expect(find.byTooltip('Proveedores'), findsOneWidget);
    expect(find.byTooltip('Cuentas por pagar'), findsOneWidget);
    expect(find.byTooltip('Configuración de compras'), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
