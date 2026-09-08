import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

class _RepoFake implements RepositorioDeAlertas {
  _RepoFake(this._alertas);

  final List<AlertaVista> _alertas;
  final List<String> resueltas = [];
  int vistasMarcadas = 0;

  @override
  Future<List<AlertaVista>> mias() async => _alertas;

  @override
  Future<void> marcarVistas() async => vistasMarcadas++;

  @override
  Future<AlertaVista> resolver(String alertaId) async {
    resueltas.add(alertaId);
    final a = _alertas.firstWhere((x) => x.id == alertaId);
    return a.comoResuelta('yo');
  }
}

AlertaVista _alerta({
  required String id,
  SeveridadAlerta severidad = SeveridadAlerta.media,
  EstadoAlerta estado = EstadoAlerta.nueva,
  String titulo = 'Título',
  String ruta = '/x',
  int hMin = 5,
}) =>
    AlertaVista(
      id: id,
      tipoCodigo: 'STOCK_MINIMO',
      severidad: severidad,
      estado: estado,
      titulo: titulo,
      mensaje: 'mensaje $id',
      generadaEn: DateTime.now().subtract(Duration(minutes: hMin)),
      rutaApp: ruta,
      entidadTipo: 'Producto',
      entidadId: 'p-$id',
    );

Future<ControladorDeAlertas> _cargado(_RepoFake repo) async {
  final c = ControladorDeAlertas(repo);
  await c.cargar();
  return c;
}

void main() {
  testWidgets('Criterio 1: el centro muestra las pendientes, nuevas primero y por severidad',
      (tester) async {
    final repo = _RepoFake([
      _alerta(id: 'vieja-vista', estado: EstadoAlerta.vista, severidad: SeveridadAlerta.critica),
      _alerta(id: 'media-nueva', severidad: SeveridadAlerta.media),
      _alerta(id: 'critica-nueva', severidad: SeveridadAlerta.critica),
      _alerta(id: 'resuelta', estado: EstadoAlerta.resuelta),
    ]);
    final c = await _cargado(repo);

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: CentroDeAlertas(controlador: c, onAbrirEntidad: (_) {}),
      ),
    ));
    await tester.pump();

    final filas = tester.widgetList<InkWell>(find.byWidgetPredicate(
        (w) => w is InkWell && w.key is ValueKey && '${w.key}'.contains('alerta-'))).toList();
    expect(filas, hasLength(3)); // la resuelta no aparece
    expect('${filas[0].key}', contains('critica-nueva'));
    expect('${filas[1].key}', contains('media-nueva'));
    expect('${filas[2].key}', contains('vieja-vista'));
  });

  testWidgets('Criterio 2: resolver saca la alerta de las pendientes y avisa al backend',
      (tester) async {
    final repo = _RepoFake([_alerta(id: 'a1'), _alerta(id: 'a2')]);
    final c = await _cargado(repo);

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(body: CentroDeAlertas(controlador: c, onAbrirEntidad: (_) {})),
    ));
    await tester.pump();

    await tester.tap(find.descendant(
      of: find.byKey(const Key('alerta-a1')),
      matching: find.bySemanticsLabel('Resolver'),
    ));
    await tester.pump();

    expect(repo.resueltas, ['a1']);
    expect(find.byKey(const Key('alerta-a1')), findsNothing);
    expect(find.byKey(const Key('alerta-a2')), findsOneWidget);
  });

  testWidgets('Criterio 3: tocar una alerta navega a la entidad que la originó', (tester) async {
    final repo = _RepoFake([_alerta(id: 'a1', ruta: '/inventario/productos/p-9')]);
    final c = await _cargado(repo);
    AlertaVista? abierta;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        body: CentroDeAlertas(controlador: c, onAbrirEntidad: (a) => abierta = a),
      ),
    ));
    await tester.pump();

    await tester.tap(find.byKey(const Key('alerta-a1')));
    await tester.pump();

    expect(abierta, isNotNull);
    expect(abierta!.rutaApp, '/inventario/productos/p-9');
    expect(abierta!.entidadId, 'p-a1');
  });

  testWidgets('Criterio 4: la campana muestra el indicador solo si hay alertas nuevas',
      (tester) async {
    final repo = _RepoFake([_alerta(id: 'a1', estado: EstadoAlerta.nueva)]);
    final c = await _cargado(repo);
    var abierto = 0;

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          actions: [CampanaDeAlertas(controlador: c, onAbrir: () => abierto++)],
        ),
      ),
    ));
    await tester.pump();
    expect(find.byKey(const Key('campana-indicador')), findsOneWidget);

    await tester.tap(find.bySemanticsLabel('Alertas, 1 nuevas'));
    await tester.pump();

    expect(abierto, 1);
    expect(repo.vistasMarcadas, 1);
    expect(find.byKey(const Key('campana-indicador')), findsNothing);
  });

  testWidgets('Sin nuevas, la campana no muestra indicador', (tester) async {
    final repo = _RepoFake([_alerta(id: 'a1', estado: EstadoAlerta.vista)]);
    final c = await _cargado(repo);

    await tester.pumpWidget(MaterialApp(
      home: Scaffold(
        appBar: AppBar(actions: [CampanaDeAlertas(controlador: c, onAbrir: () {})]),
      ),
    ));
    await tester.pump();

    expect(find.byKey(const Key('campana-indicador')), findsNothing);
  });
}
