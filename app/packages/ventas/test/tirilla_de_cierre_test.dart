import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// HU-142 · la tirilla de cierre de caja.
final _tirilla = TirillaDeCierreVista.desdeJson({
  'sesionId': 's-1',
  'numero': 'CAJA-12',
  'caja': 'Caja Principal',
  'cajeroApertura': 'Ana Gómez',
  'cajeroCierre': 'Luis Pérez',
  'abiertaEn': '2026-11-14T08:00:00-05:00',
  'cerradaEn': '2026-11-14T18:30:00-05:00',
  'base': 50000,
  'lineas': [
    {'metodo': 'EFECTIVO', 'cuadrable': true, 'esperado': 150000, 'contado': 145000,
      'diferencia': -5000, 'movimientos': 4},
    {'metodo': 'TARJETA_DEBITO', 'cuadrable': false, 'esperado': 80000, 'contado': 80000,
      'diferencia': 0, 'movimientos': 2},
  ],
  'diferenciaTotal': -5000,
  'estado': 'DESCUADRADA',
  'observaciones': 'Faltó un billete',
});

List<String> _lineas({bool reimpresion = false}) => GeneradorTirillaDeCierre.lineas(
      _tirilla,
      razonSocial: 'Ferretería Central SAS',
      nit: '900123456-7',
      reimpresion: reimpresion,
    );

class _RepoCajaFake implements RepositorioDeCaja {
  bool cerrada = false;
  String? tirillaPedida;

  @override
  Future<List<CajaVista>> listarCajas() async =>
      const [CajaVista(id: 'caja-1', codigo: 'CAJA-01', nombre: 'Caja Principal')];

  @override
  Future<SesionCajaActiva?> consultarActiva(String cajaId) async => cerrada
      ? null
      : const SesionCajaActiva(id: 's-1', cajaId: 'caja-1', cajeroId: 'c-1',
          fechaApertura: '2026-11-14T08:00:00Z', montoApertura: 50000, estado: 'ABIERTA');

  @override
  Future<List<MovimientoCajaVista>> movimientos(String sesionId) async => const [];

  @override
  Future<List<LineaDeArqueoVista>> arqueoPorMetodo(String sesionId) async => const [
        LineaDeArqueoVista(metodo: 'EFECTIVO', cuadrable: true, esperado: 50000, movimientos: 1),
      ];

  @override
  Future<SesionCajaActiva> cerrarSesion({
    required String sesionId,
    required Map<String, num> contados,
  }) async {
    cerrada = true;
    return SesionCajaActiva(id: sesionId, cajaId: 'caja-1', cajeroId: 'c-1',
        fechaApertura: '', montoApertura: 50000, estado: 'CUADRADA');
  }

  @override
  Future<TirillaDeCierreVista> tirillaDeCierre(String sesionId) async {
    tirillaPedida = sesionId;
    return _tirilla;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  test(
      'HU-142 C2 · La tirilla trae negocio, caja, número, cajeros, fechas, base, '
      'arqueo por método, diferencia total, estado y observaciones', () {
    final texto = _lineas().join('\n');
    // La tirilla muestra la hora local del dispositivo: el test no depende de la zona.
    String local(String iso) {
      final f = DateTime.parse(iso).toLocal();
      String dos(int n) => n.toString().padLeft(2, '0');
      return '${f.year}-${dos(f.month)}-${dos(f.day)} ${dos(f.hour)}:${dos(f.minute)}';
    }

    for (final esperado in [
      'Ferretería Central SAS', '900123456-7', 'Caja Principal', 'CAJA-12',
      'Ana Gómez', 'Luis Pérez',
      local('2026-11-14T08:00:00-05:00'), local('2026-11-14T18:30:00-05:00'),
      formatearPesos(50000), 'Efectivo', formatearPesos(150000), formatearPesos(145000),
      formatearPesos(-5000), 'Tarjeta débito', 'NO SE CUADRA', 'DESCUADRADA', 'Faltó un billete',
    ]) {
      expect(texto, contains(esperado), reason: 'falta «$esperado» en la tirilla');
    }
    expect(texto, isNot(contains('REIMPRESIÓN')));
  });

  test('HU-142 C3 · La reimpresión lleva la marca «REIMPRESIÓN»', () {
    expect(_lineas(reimpresion: true), contains('*** REIMPRESIÓN ***'));
  });

  test('HU-142 C4 · La tirilla usa el mismo formato térmico que el tiquete del POS', () {
    expect(GeneradorTirillaDeCierre.formato, same(GeneradorTicketPos.formato80mm));
  });

  testWidgets(
      'HU-142 C1 · Dado un cierre confirmado, la app pregunta si imprimir la tirilla '
      'y, si acepto, la manda a imprimir', (tester) async {
    final repo = _RepoCajaFake();
    TirillaDeCierreVista? impresa;
    bool? comoReimpresion;
    tester.view.physicalSize = const Size(1400, 2400);
    tester.view.devicePixelRatio = 1;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(ProviderScope(
      overrides: [
        repositorioDeCajaProvider.overrideWithValue(repo),
        impresoraDeTirillaProvider.overrideWithValue((t, {reimpresion = false}) async {
          impresa = t;
          comoReimpresion = reimpresion;
        }),
      ],
      child: const MaterialApp(home: PantallaCaja()),
    ));
    await tester.pumpAndSettle();

    await tester.tap(find.text('Cerrar Turno (Arqueo)'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const ValueKey('contado-EFECTIVO')), '50000');
    await tester.pump();
    await tester.tap(find.text('Confirmar cierre'));
    await tester.pumpAndSettle();

    expect(find.text('¿Imprimir tirilla de cierre?'), findsOneWidget);
    await tester.tap(find.text('Imprimir'));
    await tester.pumpAndSettle();

    expect(repo.tirillaPedida, 's-1');
    expect(impresa?.numero, 'CAJA-12');
    expect(comoReimpresion, isFalse);
  });
}
