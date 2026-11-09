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
      SesionDeCajaVista(
        id: 's1',
        numero: 'TURNO-1',
        cajaId: 'c1',
        descuadrada: false,
        abiertaEn: DateTime(2026, 11, 8, 8, 0),
        cerradaEn: DateTime(2026, 11, 8, 17, 0),
        montoEsperado: 350000,
        montoDeclarado: 350000,
        diferencia: 0,
        estado: EstadoSesionCaja.cuadrada,
      ),
      SesionDeCajaVista(
        id: 's2',
        numero: 'TURNO-2',
        cajaId: 'c2',
        descuadrada: true,
        abiertaEn: DateTime(2026, 11, 8, 9, 0),
        cerradaEn: DateTime(2026, 11, 8, 18, 0),
        montoEsperado: 280000,
        montoDeclarado: 270000,
        diferencia: -10000,
        estado: EstadoSesionCaja.descuadrada,
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
    expect(find.text('TURNO-1'), findsOneWidget);
    expect(find.text('TURNO-2'), findsOneWidget);
  });
}
