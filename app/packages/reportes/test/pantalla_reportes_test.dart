import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
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
  testWidgets('Encabezado E1-E5 · en web, el encabezado común sin ícono; en móvil, la barra', (tester) async {
    Future<void> montar(Size tamano) async {
      await tester.binding.setSurfaceSize(tamano);
      addTearDown(() => tester.binding.setSurfaceSize(null));
      await tester.pumpWidget(ProviderScope(
        overrides: [repositorioDeReportesDeCajaProvider.overrideWithValue(_RepoReportesFake(const []))],
        child: MaterialApp(home: const PantallaReportes()),
      ));
      await tester.pumpAndSettle();
    }

    await montar(const Size(1440, 900));
    final encabezado = find.byType(EncabezadoDeSeccion);
    expect(encabezado, findsOneWidget);
    expect(find.descendant(of: encabezado, matching: find.byIcon(Icons.bar_chart_rounded)), findsNothing);
    expect(find.descendant(of: encabezado, matching: find.text('Reportes Operativos')), findsOneWidget);

    await montar(const Size(860, 900));
    expect(find.byType(EncabezadoDeSeccion), findsNothing);
    expect(find.byType(AppBar), findsOneWidget);
  });

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
