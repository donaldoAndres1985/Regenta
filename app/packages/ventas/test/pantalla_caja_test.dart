import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

class _RepoCajaFake implements RepositorioDeCaja {
  _RepoCajaFake({this.sesionActiva});

  final List<CajaVista> cajas = const [
    CajaVista(id: 'caja-1', codigo: 'CAJA-01', nombre: 'Caja Principal'),
  ];
  SesionCajaActiva? sesionActiva;
  final List<MovimientoCajaVista> _movs = [];

  @override
  Future<List<CajaVista>> listarCajas() async => cajas;

  @override
  Future<SesionCajaActiva?> consultarActiva(String cajaId) async => sesionActiva;

  @override
  Future<SesionCajaActiva> abrirSesion({
    required String cajaId,
    required num montoApertura,
  }) async {
    final nueva = SesionCajaActiva(
      id: 'sesion-1',
      cajaId: cajaId,
      cajeroId: 'cajero-1',
      fechaApertura: '2026-11-08T10:00:00Z',
      montoApertura: montoApertura,
      estado: 'ABIERTA',
    );
    sesionActiva = nueva;
    _movs.add(
      MovimientoCajaVista(
        id: 'mov-apertura',
        sesionId: nueva.id,
        tipo: 'APERTURA',
        monto: montoApertura,
        concepto: 'Base inicial',
        fecha: '2026-11-08T10:00:00Z',
      ),
    );
    return nueva;
  }

  @override
  Future<List<MovimientoCajaVista>> movimientos(String sesionId) async => _movs;

  @override
  Future<MovimientoCajaVista> registrarIngreso({
    required String sesionId,
    required num monto,
    required String concepto,
  }) async {
    final mov = MovimientoCajaVista(
      id: 'mov-${_movs.length + 1}',
      sesionId: sesionId,
      tipo: 'INGRESO',
      monto: monto,
      concepto: concepto,
      fecha: '2026-11-08T11:00:00Z',
    );
    _movs.add(mov);
    return mov;
  }

  @override
  Future<MovimientoCajaVista> registrarRetiro({
    required String sesionId,
    required num monto,
    required String concepto,
  }) async {
    final mov = MovimientoCajaVista(
      id: 'mov-${_movs.length + 1}',
      sesionId: sesionId,
      tipo: 'RETIRO',
      monto: monto,
      concepto: concepto,
      fecha: '2026-11-08T12:00:00Z',
    );
    _movs.add(mov);
    return mov;
  }

  @override
  Future<List<LineaDeArqueoVista>> arqueoPorMetodo(String sesionId) async => const [
        LineaDeArqueoVista(metodo: 'EFECTIVO', cuadrable: true, esperado: 0, movimientos: 0),
      ];

  @override
  Future<SesionCajaActiva> cerrarSesion({
    required String sesionId,
    required Map<String, num> contados,
  }) async {
    final cerrada = SesionCajaActiva(
      id: sesionId,
      cajaId: sesionActiva?.cajaId ?? 'caja-1',
      cajeroId: 'cajero-1',
      fechaApertura: sesionActiva?.fechaApertura ?? '',
      montoApertura: sesionActiva?.montoApertura ?? 0,
      estado: 'CERRADA',
    );
    sesionActiva = null;
    return cerrada;
  }
}

void main() {
  testWidgets('Muestra estado cerrado cuando no hay sesion activa',
      (tester) async {
    final repo = _RepoCajaFake(sesionActiva: null);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeCajaProvider.overrideWithValue(repo),
        ],
        child: const MaterialApp(
          home: PantallaCaja(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Gestión de Caja y Turnos'), findsOneWidget);
    expect(find.text('Turno Cerrado en Caja Principal'), findsOneWidget);
    expect(find.text('Abrir Turno de Caja'), findsOneWidget);
  });

  testWidgets('Abre turno con dialogo y muestra métricas de la sesion',
      (tester) async {
    final repo = _RepoCajaFake(sesionActiva: null);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeCajaProvider.overrideWithValue(repo),
        ],
        child: const MaterialApp(
          home: PantallaCaja(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    await tester.tap(find.text('Abrir Turno de Caja'));
    await tester.pumpAndSettle();

    expect(find.text('Apertura de Turno'), findsOneWidget);
    await tester.tap(find.text('Confirmar Apertura'));
    await tester.pumpAndSettle();

    expect(find.text('Base Inicial'), findsOneWidget);
    expect(find.text('Efectivo Esperado'), findsOneWidget);
    expect(find.text('Registrar Ingreso'), findsOneWidget);
    expect(find.text('Registrar Retiro / Gasto'), findsOneWidget);
  });
}
