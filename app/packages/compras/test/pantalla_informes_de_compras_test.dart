import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

import 'ayudas_de_ordenes.dart';

/// Lo de la factura de referencia de la HU-183: directa $ 248.000, con orden $ 119.000 y una
/// devolución de $ 11.900 a Distribuidora Andina.
const _andina = CompraPorProveedorVista(
  proveedorId: 'prov-1',
  proveedor: 'Distribuidora Andina',
  recepciones: 2,
  directas: ImporteDeCompra(base: 200000, iva: 38000, total: 248000),
  conOrden: ImporteDeCompra(base: 100000, iva: 19000, total: 119000),
  devoluciones: ImporteDeCompra(base: 10000, iva: 1900, total: 11900),
  neto: ImporteDeCompra(base: 290000, iva: 55100, total: 355100),
);

const _acetaminofen = CompraPorProductoVista(
  productoId: 'p-ace-0001',
  producto: 'Acetaminofén 500 mg',
  cantidad: 19,
  costoTotal: 194500,
  costoPromedio: 10236.84,
);

class _RepoFake implements RepositorioDeInformesDeCompras {
  final List<PeriodoDeInforme> periodos = [];
  final List<PeriodoDeInforme> periodosDePagos = [];

  @override
  Future<InformeDeAntiguedadVista> antiguedad() async => const InformeDeAntiguedadVista(
        proveedores: [
          AntiguedadDeProveedorVista(
            proveedorId: 'prov-1',
            proveedor: 'Distribuidora Andina',
            cuentas: 3,
            moraMaxima: 100,
            saldo: TramosDeMora(alDia: 248000, de1a30: 100000, masDe90: 50000, total: 398000),
          ),
          AntiguedadDeProveedorVista(
            proveedorId: 'prov-2',
            proveedor: 'Pinturas del Valle',
            cuentas: 1,
            saldo: TramosDeMora(alDia: 156000, total: 156000),
          ),
        ],
        totales: TramosDeMora(alDia: 404000, de1a30: 100000, masDe90: 50000, total: 554000),
      );

  @override
  Future<List<PendientePorRecibirVista>> pendientesPorRecibir() async => [
        PendientePorRecibirVista(
          ordenId: 'oc-1',
          numero: 'OC-0231',
          proveedor: 'Distribuidora Andina',
          estado: 'ENVIADA',
          fechaEsperada: DateTime(2026, 11, 20),
          atrasada: true,
          diasDeAtraso: 3,
          valorPendiente: 1008000,
          lineas: const [
            LineaPendienteVista(linea: 1, producto: 'Acetaminofén 500 mg x100', pedida: 120, faltante: 120, valor: 1008000),
          ],
        ),
        PendientePorRecibirVista(
          ordenId: 'oc-2',
          numero: 'OC-0232',
          proveedor: 'Pinturas del Valle',
          estado: 'PARCIAL',
          fechaEsperada: DateTime(2026, 11, 28),
          valorPendiente: 54000,
          lineas: const [
            LineaPendienteVista(linea: 2, producto: 'Brocha 3"', pedida: 5, recibida: 2, faltante: 3, valor: 54000),
          ],
        ),
      ];

  @override
  Future<List<FaltanteDeOrdenVista>> faltantes() async => [
        FaltanteDeOrdenVista(
          ordenId: 'oc-6',
          numero: 'OC-0220',
          proveedor: 'Distribuidora Andina',
          cerradaEn: DateTime(2026, 11, 12, 10),
          motivo: 'El proveedor no tenía más',
          valorFaltante: 30000,
          lineas: const [
            LineaPendienteVista(linea: 1, producto: 'Acetaminofén', pedida: 10, recibida: 7, faltante: 3, valor: 30000),
          ],
        ),
      ];

  @override
  Future<InformeDePagosVista> pagos(PeriodoDeInforme periodo) async {
    periodosDePagos.add(periodo);
    return const InformeDePagosVista(
      filas: [
        PagosPorProveedorYMetodoVista(
            proveedorId: 'prov-1', proveedor: 'Distribuidora Andina', metodo: 'TRANSFERENCIA', pagos: 2, total: 150000),
        PagosPorProveedorYMetodoVista(
            proveedorId: 'prov-2', proveedor: 'Pinturas del Valle', metodo: 'CHEQUE', pagos: 1, total: 80000),
      ],
      porMetodo: {'TRANSFERENCIA': 150000, 'CHEQUE': 80000},
      total: 230000,
    );
  }
  final List<(InformeDeCompras, String, PeriodoDeInforme?, String?)> archivos = [];
  ErrorDeApi? falla;

  @override
  Future<List<CompraPorProveedorVista>> porProveedor(PeriodoDeInforme periodo) async {
    if (falla != null) throw falla!;
    periodos.add(periodo);
    return const [_andina];
  }

  @override
  Future<List<CompraPorProductoVista>> porProducto(PeriodoDeInforme periodo) async {
    if (falla != null) throw falla!;
    return const [_acetaminofen];
  }

  @override
  Future<List<CostoHistoricoVista>> historialDeCostos(String productoId) async => [
        CostoHistoricoVista(
          fecha: DateTime(2026, 11, 20),
          proveedor: 'Distribuidora Andina',
          recepcionId: 'r-1',
          recepcion: 'REC-1',
          cantidad: 10,
          costoEntrada: 10500,
        ),
        CostoHistoricoVista(
          fecha: DateTime(2026, 11, 5),
          proveedor: 'Distribuidora Andina',
          recepcionId: 'r-2',
          recepcion: 'REC-2',
          conOrden: true,
          cantidad: 10,
          costoEntrada: 10000,
        ),
      ];

  @override
  Future<List<int>> archivo(InformeDeCompras informe, String formato,
      {PeriodoDeInforme? periodo, String? productoId}) async {
    archivos.add((informe, formato, periodo, productoId));
    return const [37, 80, 68, 70];
  }
}

class _GuardadorFake implements GuardadorDeInformes {
  final List<(String, List<int>)> guardados = [];

  @override
  Future<void> guardar(String nombre, List<int> bytes) async => guardados.add((nombre, bytes));
}

void main() {
  final hoy = DateTime(2026, 11, 23);
  late _RepoFake repo;
  late _GuardadorFake guardador;

  setUp(() {
    repo = _RepoFake();
    guardador = _GuardadorFake();
  });

  Future<void> montar(WidgetTester tester) async {
    await tester.binding.setSurfaceSize(const Size(420, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(ProviderScope(
      overrides: [
        repositorioDeInformesDeComprasProvider.overrideWithValue(repo),
        guardadorDeInformesProvider.overrideWithValue(guardador),
      ],
      child: MaterialApp(home: PantallaInformesDeCompras(hoy: hoy)),
    ));
    await tester.pumpAndSettle();
  }

  String texto(WidgetTester tester, String clave) => tester.widget<Text>(find.byKey(Key(clave))).data!;

  Future<void> abrir(WidgetTester tester, String pestana) async {
    await tester.ensureVisible(find.byKey(Key('informes-tab-$pestana')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(Key('informes-tab-$pestana')));
    await tester.pumpAndSettle();
  }

  testWidgets('C1 · Por proveedor: neto, directas, con orden y devoluciones del mes en curso', (tester) async {
    await montar(tester);

    expect(repo.periodos.single, PeriodoDeInforme(DateTime(2026, 11), DateTime(2026, 11, 23)));
    expect(texto(tester, 'informes-proveedor-total'), r'$355.100');
    expect(texto(tester, 'informes-proveedor-neto-prov-1'), r'$355.100');
    expect(texto(tester, 'informes-directas-prov-1'), r'$248.000');
    expect(texto(tester, 'informes-con-orden-prov-1'), r'$119.000');
    expect(texto(tester, 'informes-devoluciones-prov-1'), r'− $11.900');
    expect(find.text(r'Base $290.000 · IVA $55.100'), findsOneWidget);
  });

  testWidgets('C1 · «Mes pasado» vuelve a pedir con el mes anterior completo', (tester) async {
    await montar(tester);

    await tester.tap(find.byKey(const Key('informes-mes-pasado')));
    await tester.pumpAndSettle();

    expect(repo.periodos.last, PeriodoDeInforme(DateTime(2026, 10), DateTime(2026, 10, 31)));
    expect(repo.periodos.last.query, {'desde': '2026-10-01', 'hasta': '2026-10-31'});
  });

  testWidgets('C2 y C3 · Por producto, y al tocarlo su historial de costos con proveedor y recepción',
      (tester) async {
    await montar(tester);

    await tester.tap(find.byKey(const Key('informes-tab-producto')));
    await tester.pumpAndSettle();
    expect(texto(tester, 'informes-producto-total-p-ace-0001'), r'$194.500');
    expect(texto(tester, 'informes-producto-detalle-p-ace-0001'), r'19 und · promedio $10.237');

    await tester.tap(find.byKey(const Key('informes-producto-p-ace-0001')));
    await tester.pumpAndSettle();

    expect(texto(tester, 'informes-historial-producto'), 'Acetaminofén 500 mg');
    expect(texto(tester, 'informes-historial-costo-r-1'), r'$10.500');
    expect(texto(tester, 'informes-historial-costo-r-2'), r'$10.000');
    expect(find.text('20/11/2026 · REC-1 · directa · 10 und'), findsOneWidget);
    expect(find.text('05/11/2026 · REC-2 · con orden · 10 und'), findsOneWidget);
    // Subió respecto a la entrada anterior.
    expect(find.byIcon(Icons.arrow_upward), findsOneWidget);
  });

  testWidgets('C4 · Lo que se ve se descarga en PDF o en Excel', (tester) async {
    await montar(tester);

    await tester.tap(find.byKey(const Key('informes-descargar')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('informes-descargar-pdf')));
    await tester.pumpAndSettle();

    final (informe, formato, periodo, _) = repo.archivos.single;
    expect(informe, InformeDeCompras.porProveedor);
    expect(formato, 'PDF');
    expect(periodo, PeriodoDeInforme(DateTime(2026, 11), DateTime(2026, 11, 23)));
    expect(guardador.guardados.single.$1, 'compras-por-proveedor-2026-11-01-a-2026-11-23.pdf');

    await tester.tap(find.byKey(const Key('informes-tab-producto')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('informes-producto-p-ace-0001')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('informes-descargar')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('informes-descargar-xlsx')));
    await tester.pumpAndSettle();

    expect(repo.archivos.last.$1, InformeDeCompras.historialDeCostos);
    expect(repo.archivos.last.$4, 'p-ace-0001');
    expect(guardador.guardados.last.$1, 'historial-de-costos-p-ace-00.xlsx');
  });

  testWidgets('C4 · El historial sin producto elegido no se descarga', (tester) async {
    await montar(tester);
    await abrir(tester, 'historial');
    expect(find.byKey(const Key('informes-historial-sin-producto')), findsOneWidget);

    await tester.tap(find.byKey(const Key('informes-descargar')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('informes-descargar-pdf')));
    await tester.pumpAndSettle();

    expect(repo.archivos, isEmpty);
    expect(find.text('Elige primero un producto en «Por producto».'), findsOneWidget);
  });

  group('HU-184', () {
    testWidgets('C1 · Antigüedad: el saldo de cada proveedor por días de mora y el total de cada columna',
        (tester) async {
      await montar(tester);
      await abrir(tester, 'antiguedad');

      expect(find.byKey(const Key('informes-este-mes')), findsNothing, reason: 'la antigüedad va a hoy');
      expect(texto(tester, 'informes-antiguedad-total'), r'$554.000');
      // Columnas: al día, 1-30, 31-60, 61-90, +90.
      expect(texto(tester, 'informes-antiguedad-columna-0'), r'$404.000');
      expect(texto(tester, 'informes-antiguedad-columna-1'), r'$100.000');
      expect(texto(tester, 'informes-antiguedad-columna-2'), '—');
      expect(texto(tester, 'informes-antiguedad-columna-4'), r'$50.000');
      expect(texto(tester, 'informes-antiguedad-saldo-prov-1'), r'$398.000');
      expect(texto(tester, 'informes-antiguedad-prov-1-0'), r'$248.000');
      expect(texto(tester, 'informes-antiguedad-prov-1-4'), r'$50.000');
      expect(find.text('3 facturas · la más vieja con 100 días de mora'), findsOneWidget);
      expect(find.text('1 factura · al día'), findsOneWidget);
    });

    testWidgets('C2 · Por recibir: por orden y línea lo que falta y la fecha esperada; la atrasada marcada',
        (tester) async {
      await montar(tester);
      await abrir(tester, 'por-recibir');

      expect(find.byKey(const Key('informes-atrasada-oc-1')), findsOneWidget);
      expect(find.text('ATRASADA · 3 DÍAS'), findsOneWidget);
      expect(find.byKey(const Key('informes-atrasada-oc-2')), findsNothing);
      expect(texto(tester, 'informes-por-recibir-fecha-oc-1'), 'Se esperaba el 20/11/2026');
      expect(texto(tester, 'informes-por-recibir-oc-1-1'), 'Faltan 120 und');
      expect(texto(tester, 'informes-por-recibir-oc-2-2'), 'Faltan 3 und');
      expect(find.text('Pedido 5 und · llegó 2 und'), findsOneWidget);
      expect(find.text('Por recibir · 1 orden atrasada'), findsOneWidget);
      expect(texto(tester, 'informes-por-recibir-total'), r'$1.062.000');
    });

    testWidgets('C3 · Faltantes: lo que nunca llegó de las órdenes cerradas', (tester) async {
      await montar(tester);
      await abrir(tester, 'faltantes');

      expect(texto(tester, 'informes-faltantes-motivo-oc-6'), 'Cerrada el 12/11/2026 · El proveedor no tenía más');
      expect(texto(tester, 'informes-faltantes-oc-6-1'), 'No llegó 3 und');
      expect(texto(tester, 'informes-faltantes-total'), r'$30.000');
    });

    testWidgets('C4 · Pagos por proveedor y método en el periodo elegido', (tester) async {
      await montar(tester);
      await abrir(tester, 'pagos');

      expect(texto(tester, 'informes-pagos-total'), r'$230.000');
      expect(find.text(r'Transferencia $150.000'), findsOneWidget);
      expect(find.text(r'Cheque $80.000'), findsOneWidget);
      expect(find.text('Transferencia · 2 pagos'), findsOneWidget);
      expect(texto(tester, 'informes-pagos-total-prov-2-CHEQUE'), r'$80.000');

      await tester.tap(find.byKey(const Key('informes-mes-pasado')));
      await tester.pumpAndSettle();
      expect(repo.periodosDePagos.last, PeriodoDeInforme(DateTime(2026, 10), DateTime(2026, 10, 31)));
    });

    testWidgets('La antigüedad se descarga a la fecha de hoy, sin periodo', (tester) async {
      await montar(tester);
      await abrir(tester, 'antiguedad');

      await tester.tap(find.byKey(const Key('informes-descargar')));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('informes-descargar-xlsx')));
      await tester.pumpAndSettle();

      final (informe, formato, periodo, _) = repo.archivos.single;
      expect(informe, InformeDeCompras.antiguedad);
      expect(formato, 'XLSX');
      expect(periodo, isNull);
      expect(guardador.guardados.single.$1, 'cuentas-por-pagar-por-antiguedad-2026-11-23.xlsx');
    });
  });

  testWidgets('C5 · Sin COMPRAS_INFORME_VER el backend responde 403 y la pantalla lo dice', (tester) async {
    repo.falla = const SinPermiso();
    await montar(tester);
    expect(find.text('No tienes permiso para ver los informes de compras.'), findsOneWidget);
  });

  testWidgets('C5 · Sin el permiso, el atajo «Informes» no aparece en Compras', (tester) async {
    await montarCompras(tester, const []);
    expect(find.byKey(const Key('compras-informes')), findsNothing);
  });

  testWidgets('Con el permiso, el atajo «Informes» de Compras abre la pantalla', (tester) async {
    await tester.binding.setSurfaceSize(const Size(1440, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(ProviderScope(
      overrides: [
        repositorioDeOrdenesProvider.overrideWithValue(RepoOrdenesFake(const [])),
        repositorioDeInformesDeComprasProvider.overrideWithValue(repo),
        puedeVerInformesDeComprasProvider.overrideWithValue(true),
      ],
      child: MaterialApp(home: PantallaCompras(onOrdenSeleccionada: (_) {})),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('compras-informes')));
    await tester.pumpAndSettle();

    expect(find.byType(PantallaInformesDeCompras), findsOneWidget);
  });
}
