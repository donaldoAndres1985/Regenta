import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_reportes/regenta_reportes.dart';

/// HU-142 C3 · reimprimir la tirilla de un turno cerrado desde el listado de turnos.
class _RepoReportesFake implements RepositorioDeReportesDeCaja {
  @override
  Future<List<SesionDeCajaVista>> sesiones({
    required DateTime desde,
    required DateTime hasta,
    String? estado,
    String? cajaId,
  }) async =>
      [
        SesionDeCajaVista(
          id: 's-cerrada', numero: 'TURNO-9', cajaId: 'c1', descuadrada: false,
          abiertaEn: DateTime(2026, 11, 14, 8), cerradaEn: DateTime(2026, 11, 14, 18),
          montoEsperado: 100000, montoDeclarado: 100000, diferencia: 0,
          estado: EstadoSesionCaja.cuadrada,
        ),
      ];
}

Future<void> _abrirDetalle(WidgetTester tester, List<Override> overrides) async {
  tester.view.physicalSize = const Size(1400, 2400);
  tester.view.devicePixelRatio = 1;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeReportesDeCajaProvider.overrideWithValue(_RepoReportesFake()),
      ...overrides,
    ],
    child: const MaterialApp(home: PantallaReportes()),
  ));
  await tester.pumpAndSettle();
  await tester.tap(find.text('TURNO-9'));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets(
      'HU-142 C3 · Dada una sesión cerrada en el listado de turnos, '
      'cuando pido reimprimir, se reimprime su tirilla', (tester) async {
    String? reimpresa;
    await _abrirDetalle(tester, [
      reimprimirTirillaDeCajaProvider.overrideWithValue((sesionId) async => reimpresa = sesionId),
    ]);

    await tester.tap(find.text('Reimprimir tirilla'));
    await tester.pumpAndSettle();

    expect(reimpresa, 's-cerrada');
  });

  testWidgets('HU-142 C3 · Sin impresora conectada a la app, no se ofrece reimprimir',
      (tester) async {
    await _abrirDetalle(tester, const []);

    expect(find.text('Reimprimir tirilla'), findsNothing);
  });
}
