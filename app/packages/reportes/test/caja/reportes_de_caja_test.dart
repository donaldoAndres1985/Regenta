import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_reportes/regenta_reportes.dart';

class _RepoFake implements RepositorioDeReportesDeCaja {
  _RepoFake(this._sesiones);

  final List<SesionDeCajaVista> _sesiones;
  final List<String?> estadosPedidos = [];

  @override
  Future<List<SesionDeCajaVista>> sesiones({
    required DateTime desde,
    required DateTime hasta,
    String? estado,
    String? cajaId,
  }) async {
    estadosPedidos.add(estado);
    return estado == null
        ? _sesiones
        : _sesiones.where((s) => s.estado.name.toUpperCase() == estado).toList();
  }
}

SesionDeCajaVista _sesion({
  required String id,
  required EstadoSesionCaja estado,
  required bool descuadrada,
  num? diferencia,
}) =>
    SesionDeCajaVista(
      id: id,
      numero: 'CAJA-$id',
      cajaId: 'c1',
      estado: estado,
      descuadrada: descuadrada,
      abiertaEn: DateTime.now(),
      cerradaEn: DateTime.now(),
      montoEsperado: 100000,
      montoDeclarado: descuadrada ? 90000 : 100000,
      diferencia: diferencia,
    );

Future<ControladorDeReportesDeCaja> _cargado(_RepoFake repo, {String? estado}) async {
  final c = ControladorDeReportesDeCaja(repo);
  await c.cargar(desde: DateTime(2026, 9, 1), hasta: DateTime(2026, 9, 8), estado: estado);
  return c;
}

void main() {
  testWidgets('Criterio 2: el listado muestra las sesiones del rango con su estado',
      (tester) async {
    final repo = _RepoFake([
      _sesion(id: 'a', estado: EstadoSesionCaja.cuadrada, descuadrada: false, diferencia: 0),
      _sesion(id: 'b', estado: EstadoSesionCaja.descuadrada, descuadrada: true, diferencia: -10000),
    ]);
    final c = await _cargado(repo);

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: ListaDeSesionesDeCaja(controlador: c, onAbrirSesion: (_) {}),
      ),
    ));
    await tester.pump();

    expect(find.byKey(const Key('sesion-a')), findsOneWidget);
    expect(find.byKey(const Key('sesion-b')), findsOneWidget);
    expect(find.text('CAJA-a'), findsOneWidget);
  });

  testWidgets('Criterio 3: la sesión descuadrada destaca frente a la cuadrada', (tester) async {
    final repo = _RepoFake([
      _sesion(id: 'ok', estado: EstadoSesionCaja.cuadrada, descuadrada: false, diferencia: 0),
      _sesion(id: 'mal', estado: EstadoSesionCaja.descuadrada, descuadrada: true, diferencia: -20000),
    ]);
    final c = await _cargado(repo);

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: ListaDeSesionesDeCaja(controlador: c, onAbrirSesion: (_) {})),
    ));
    await tester.pump();

    expect(find.byKey(const Key('sesion-descuadrada-mal')), findsOneWidget);
    expect(find.byKey(const Key('sesion-descuadrada-ok')), findsNothing);
    expect(find.text('Descuadre'), findsOneWidget);
    expect(find.textContaining('descuadrado'), findsOneWidget);
  });

  testWidgets('Criterio 1: tocar una sesión lleva a su reporte', (tester) async {
    final repo = _RepoFake([
      _sesion(id: 'x', estado: EstadoSesionCaja.cuadrada, descuadrada: false, diferencia: 0),
    ]);
    final c = await _cargado(repo);
    SesionDeCajaVista? abierta;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: ListaDeSesionesDeCaja(controlador: c, onAbrirSesion: (s) => abierta = s),
      ),
    ));
    await tester.pump();

    await tester.tap(find.byKey(const Key('sesion-x')));
    await tester.pump();

    expect(abierta, isNotNull);
    expect(abierta!.numero, 'CAJA-x');
  });

  testWidgets('El filtro por estado se pasa al repositorio', (tester) async {
    final repo = _RepoFake([
      _sesion(id: 'a', estado: EstadoSesionCaja.cuadrada, descuadrada: false),
      _sesion(id: 'b', estado: EstadoSesionCaja.descuadrada, descuadrada: true),
    ]);
    final c = await _cargado(repo, estado: 'DESCUADRADA');

    expect(repo.estadosPedidos, ['DESCUADRADA']);
    expect(c.sesiones.map((s) => s.id), ['b']);
  });
}
