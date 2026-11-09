import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_reportes/regenta_reportes.dart';

class _RepoReportesFake implements RepositorioDeReportesDeCaja {
  _RepoReportesFake(this._sesiones);

  final List<SesionDeCajaVista> _sesiones;

  @override
  Future<List<SesionDeCajaVista>> sesiones({
    required DateTime desde,
    required DateTime hasta,
    String? estado,
    String? cajaId,
  }) async => _sesiones;
}

void main() {
  testWidgets('Muestra la pantalla de reportes con métricas y lista de sesiones',
      (tester) async {
    final fakeSesiones = [
      const SesionDeCajaVista(
        id: 's1',
        cajaId: 'c1',
        cajaCodigo: 'CAJA-1',
        cajeroNombre: 'Ana Pérez',
        apertura: '2026-11-08 08:00',
        cierre: '2026-11-08 17:00',
        montoApertura: 50000,
        efectivoEsperado: 350000,
        efectivoContado: 350000,
        diferencia: 0,
        estado: 'CERRADA',
      ),
      const SesionDeCajaVista(
        id: 's2',
        cajaId: 'c2',
        cajaCodigo: 'CAJA-2',
        cajeroNombre: 'Carlos Ruiz',
        apertura: '2026-11-08 09:00',
        cierre: '2026-11-08 18:00',
        montoApertura: 50000,
        efectivoEsperado: 280000,
        efectivoContado: 270000,
        diferencia: -10000,
        estado: 'CERRADA',
      ),
    ];

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          repositorioDeReportesDeCajaProvider.overrideWithValue(_RepoReportesFake(fakeSesiones)),
        ],
        child: const MaterialApp(
          home: PantallaReportes(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Reportes Operativos'), findsOneWidget);
    expect(find.text('Turnos Evaluados'), findsOneWidget);
    expect(find.text('Turnos Cuadrados'), findsOneWidget);
    expect(find.text('Turnos Descuadrados'), findsOneWidget);
    expect(find.text('CAJA-1'), findsOneWidget);
    expect(find.text('CAJA-2'), findsOneWidget);
  });
}
