import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_reportes/regenta_reportes.dart';

/// El turno de referencia de HU-143: base $ 100.000, venta V-1 mixta (efectivo + débito), V-2 a
/// crédito, un ingreso, un retiro que autorizó Luis y un gasto que Luis anuló; faltaron $ 2.000.
CierreDeCajaVista _cierre(String id, String numero, {String cajero = 'Ana Ruiz', num diferencia = -2000}) =>
    CierreDeCajaVista(
      sesionId: id,
      numero: numero,
      cerradaEn: DateTime(2026, 11, 20, 18, 30),
      caja: 'Caja Principal',
      cajero: cajero,
      totalEsperado: 170000,
      diferenciaTotal: diferencia,
      estado: diferencia == 0 ? 'CUADRADA' : 'DESCUADRADA',
    );

class _RepoFake implements RepositorioDeCierresDeCaja {
  final List<({DateTime desde, DateTime hasta, String? cajeroId, int pagina})> busquedas = [];
  ErrorDeApi? falla;

  @override
  Future<PaginaDeCierresVista> buscar({
    required DateTime desde,
    required DateTime hasta,
    String? cajeroId,
    int pagina = 0,
  }) async {
    if (falla != null) throw falla!;
    busquedas.add((desde: desde, hasta: hasta, cajeroId: cajeroId, pagina: pagina));
    if (cajeroId == 'luis') {
      return PaginaDeCierresVista(cierres: [_cierre('s-9', 'CAJA-09', cajero: 'Luis Gómez', diferencia: 0)], total: 1);
    }
    return pagina == 0
        ? PaginaDeCierresVista(cierres: [_cierre('s-12', 'CAJA-12'), _cierre('s-11', 'CAJA-11', diferencia: 0)], total: 3, hayMas: true)
        : PaginaDeCierresVista(cierres: [_cierre('s-10', 'CAJA-10', diferencia: 0)], pagina: 1, total: 3);
  }

  @override
  Future<List<CajeroVista>> cajeros() async =>
      const [CajeroVista(id: 'ana', nombre: 'Ana Ruiz'), CajeroVista(id: 'luis', nombre: 'Luis Gómez')];

  @override
  Future<ResumenDeCierreVista> resumen(String sesionId) async => ResumenDeCierreVista(
        cierre: _cierre(sesionId, 'CAJA-12'),
        cerradoPor: 'Luis Gómez',
        abiertaEn: DateTime(2026, 11, 20, 8),
        montoApertura: 100000,
        ventasContado: 50000,
        ventasCredito: 50000,
        ingresos: 10000,
        retiros: 40000,
        observaciones: 'Faltó sencillo',
        arqueo: const [
          LineaDeArqueoVista(metodo: 'EFECTIVO', esperado: 100000, contado: 98000, diferencia: -2000),
          LineaDeArqueoVista(metodo: 'TARJETA_DEBITO', esperado: 20000, contado: 20000, diferencia: 0),
          LineaDeArqueoVista(metodo: 'CREDITO', esperado: 50000, contado: 50000, diferencia: 0),
        ],
        totalEsperado: 170000,
        totalContado: 168000,
        totalDiferencia: -2000,
      );

  @override
  Future<MovimientosDeCierreVista> movimientos(String sesionId) async => MovimientosDeCierreVista(
        movimientos: [
          MovimientoDeCierreVista(
              id: 'm-1', tipoNombre: 'Base de apertura', metodoNombre: 'Efectivo', monto: 100000, ocurridoEn: DateTime(2026, 11, 20, 8)),
          const MovimientoDeCierreVista(id: 'm-2', tipoNombre: 'Venta', documento: 'V-1', metodoNombre: 'Efectivo', monto: 30000),
          const MovimientoDeCierreVista(id: 'm-3', tipoNombre: 'Venta', documento: 'V-1', metodoNombre: 'Tarjeta débito', monto: 20000),
          const MovimientoDeCierreVista(id: 'm-4', tipoNombre: 'Retiro', metodoNombre: 'Efectivo', monto: -40000),
          const MovimientoDeCierreVista(id: 'm-5', tipoNombre: 'Gasto', metodoNombre: 'Efectivo', monto: -5000, anulado: true),
          const MovimientoDeCierreVista(id: 'm-6', tipoNombre: 'Anulación', metodoNombre: 'Efectivo', monto: 5000),
        ],
        total: 110000,
      );

  @override
  Future<DetalleDeMovimientoVista> movimiento(String movimientoId) async => switch (movimientoId) {
        'm-2' => const DetalleDeMovimientoVista(
            id: 'm-2',
            tipoNombre: 'Venta',
            documento: 'V-1',
            metodoNombre: 'Efectivo',
            monto: 30000,
            registradoPor: 'Ana Ruiz',
            ventaId: 'venta-1',
            pagos: [
              PagoDelCobroVista(metodoNombre: 'Efectivo', monto: 30000),
              PagoDelCobroVista(metodoNombre: 'Tarjeta débito', monto: 20000),
            ],
          ),
        'm-4' => const DetalleDeMovimientoVista(
            id: 'm-4',
            tipoNombre: 'Retiro',
            metodoNombre: 'Efectivo',
            monto: -40000,
            concepto: 'Consignación',
            registradoPor: 'Ana Ruiz',
            autorizadoPor: 'Luis Gómez',
            pagos: [PagoDelCobroVista(metodoNombre: 'Efectivo', monto: 40000)],
          ),
        _ => const DetalleDeMovimientoVista(
            id: 'm-5',
            tipoNombre: 'Gasto',
            metodoNombre: 'Efectivo',
            monto: -5000,
            concepto: 'Domicilio',
            registradoPor: 'Ana Ruiz',
            anuladoPor: 'Luis Gómez',
            motivoAnulacion: 'Se registró dos veces',
          ),
      };
}

void main() {
  late _RepoFake repo;
  final ventasAbiertas = <(String, String?)>[];

  setUp(() {
    repo = _RepoFake();
    ventasAbiertas.clear();
  });

  Future<void> montar(WidgetTester tester, Widget pantalla) async {
    await tester.binding.setSurfaceSize(const Size(420, 1400));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(ProviderScope(
      overrides: [
        repositorioDeCierresDeCajaProvider.overrideWithValue(repo),
        abrirVentaDesdeCajaProvider.overrideWithValue((_, id, numero) => ventasAbiertas.add((id, numero))),
      ],
      child: MaterialApp(home: pantalla),
    ));
    await tester.pumpAndSettle();
  }

  String texto(WidgetTester tester, String clave) => tester.widget<Text>(find.byKey(Key(clave))).data!;

  testWidgets('C1 · Los cierres de los últimos 7 días, el más reciente primero, con su información', (tester) async {
    await montar(tester, PantallaCierresDeCaja(hoy: DateTime(2026, 11, 23)));

    final b = repo.busquedas.single;
    expect(b.desde, DateTime(2026, 11, 16));
    expect(b.hasta, DateTime(2026, 11, 23));
    expect(b.cajeroId, isNull);
    expect(find.text('CAJA-12 · Caja Principal'), findsOneWidget);
    expect(find.text('Ana Ruiz · 20/11/2026 18:30'), findsNWidgets(2));
    expect(texto(tester, 'cierre-esperado-s-12'), r'$170.000');
    expect(texto(tester, 'cierre-diferencia-s-12'), r'Diferencia -$2.000');
    expect(texto(tester, 'cierre-diferencia-s-11'), 'Sin diferencia');
    expect(texto(tester, 'cierre-estado-s-12'), 'DESCUADRADA');
    expect(texto(tester, 'cierres-conteo'), '2 de 3 cierres');
  });

  testWidgets('C1 · «Cargar más» trae la página siguiente; sin más, el botón se va', (tester) async {
    await montar(tester, PantallaCierresDeCaja(hoy: DateTime(2026, 11, 23)));

    await tester.tap(find.byKey(const Key('cierres-cargar-mas')));
    await tester.pumpAndSettle();

    expect(repo.busquedas.last.pagina, 1);
    expect(find.byKey(const Key('cierre-s-10')), findsOneWidget);
    expect(texto(tester, 'cierres-conteo'), '3 de 3 cierres');
    expect(find.byKey(const Key('cierres-cargar-mas')), findsNothing);
  });

  testWidgets('C1 · Filtrar por cajero y por rango vuelve a buscar desde la primera página', (tester) async {
    await montar(tester, PantallaCierresDeCaja(hoy: DateTime(2026, 11, 23)));

    await tester.tap(find.byKey(const Key('cierres-cajero')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Luis Gómez').last);
    await tester.pumpAndSettle();
    expect(repo.busquedas.last.cajeroId, 'luis');
    expect(repo.busquedas.last.pagina, 0);
    expect(find.byKey(const Key('cierre-s-9')), findsOneWidget);
    expect(find.byKey(const Key('cierre-s-12')), findsNothing);

    await tester.tap(find.byKey(const Key('cierres-30-dias')));
    await tester.pumpAndSettle();
    expect(repo.busquedas.last.desde, DateTime(2026, 10, 24));
    expect(repo.busquedas.last.cajeroId, 'luis');
  });

  testWidgets('C2 · El detalle: ventas de contado y a crédito, ingresos, retiros, gastos y el arqueo por método',
      (tester) async {
    await montar(tester, PantallaCierresDeCaja(hoy: DateTime(2026, 11, 23)));
    await tester.tap(find.byKey(const Key('cierre-s-12')));
    await tester.pumpAndSettle();

    expect(texto(tester, 'cierre-ventas-contado'), r'$50.000');
    expect(texto(tester, 'cierre-ventas-credito'), r'$50.000');
    expect(texto(tester, 'cierre-ingresos'), r'$10.000');
    expect(texto(tester, 'cierre-retiros'), r'−$40.000');
    expect(texto(tester, 'cierre-gastos'), r'−$0');
    expect(find.text('Cerró Luis Gómez el 20/11/2026 18:30'), findsOneWidget);
    final efectivo = find.byKey(const Key('cierre-arqueo-EFECTIVO'));
    expect(find.descendant(of: efectivo, matching: find.text(r'$98.000')), findsOneWidget);
    expect(find.descendant(of: efectivo, matching: find.text(r'-$2.000')), findsOneWidget);
    final total = find.byKey(const Key('cierre-arqueo-total'));
    expect(find.descendant(of: total, matching: find.text(r'$170.000')), findsOneWidget);
    expect(find.descendant(of: total, matching: find.text(r'-$2.000')), findsOneWidget);
  });

  testWidgets('C3 · Todos los movimientos con tipo, documento, método y monto, y la fila del total', (tester) async {
    await montar(tester, const PantallaDetalleDeCierre(sesionId: 's-12'));

    expect(find.text('Venta · V-1'), findsNWidgets(2));
    expect(find.text('Efectivo · anulado'), findsOneWidget);
    expect(texto(tester, 'movimiento-monto-m-4'), r'-$40.000');
    expect(texto(tester, 'cierre-movimientos-total'), r'$110.000');
  });

  testWidgets('C4 · Un movimiento: quién lo registró y quién lo autorizó', (tester) async {
    await montar(tester, const PantallaDetalleDeCierre(sesionId: 's-12'));
    await tester.ensureVisible(find.byKey(const Key('movimiento-m-4')));
    await tester.tap(find.byKey(const Key('movimiento-m-4')));
    await tester.pumpAndSettle();

    expect(texto(tester, 'movimiento-titulo'), 'Retiro');
    expect(texto(tester, 'movimiento-monto'), r'-$40.000');
    expect(texto(tester, 'movimiento-registro'), 'Ana Ruiz');
    expect(texto(tester, 'movimiento-autorizo'), 'Luis Gómez');
    expect(find.text('Consignación'), findsOneWidget);
    expect(find.byKey(const Key('movimiento-ir-a-venta')), findsNothing);
  });

  testWidgets('C4 · Un movimiento anulado dice quién lo anuló y por qué', (tester) async {
    await montar(tester, const PantallaDetalleDeCierre(sesionId: 's-12'));
    await tester.ensureVisible(find.byKey(const Key('movimiento-m-5')));
    await tester.tap(find.byKey(const Key('movimiento-m-5')));
    await tester.pumpAndSettle();

    expect(texto(tester, 'movimiento-anulo'), 'Luis Gómez');
    expect(find.text('Se registró dos veces'), findsOneWidget);
  });

  testWidgets('C4 / C5 · Un cobro mixto muestra todos sus métodos y lleva a su venta', (tester) async {
    await montar(tester, const PantallaDetalleDeCierre(sesionId: 's-12'));
    await tester.ensureVisible(find.byKey(const Key('movimiento-m-2')));
    await tester.tap(find.byKey(const Key('movimiento-m-2')));
    await tester.pumpAndSettle();

    expect(find.text('Pago mixto'), findsOneWidget);
    expect(texto(tester, 'movimiento-pago-Efectivo'), r'$30.000');
    expect(texto(tester, 'movimiento-pago-Tarjeta débito'), r'$20.000');

    await tester.tap(find.byKey(const Key('movimiento-ir-a-venta')));
    await tester.pumpAndSettle();
    expect(ventasAbiertas.single, ('venta-1', 'V-1'));
  });

  testWidgets('Sin CAJA_TURNO_VER el buscador lo dice', (tester) async {
    repo.falla = const SinPermiso();
    await montar(tester, PantallaCierresDeCaja(hoy: DateTime(2026, 11, 23)));

    expect(texto(tester, 'cierres-error'), 'No tienes permiso para ver los cierres de caja.');
  });
}
