import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_clientes/regenta_clientes.dart';

class _RepoFake implements RepositorioDeClientes {
  _RepoFake({this.cache = const [], this.red = const [], this.redFalla});

  List<ClienteEnLista> cache;
  List<ClienteEnLista> red;
  ErrorDeApi? redFalla;
  int llamadasEnRed = 0;
  int guardados = 0;

  @override
  Future<List<ClienteEnLista>> enCache() async => cache;

  @override
  Future<List<ClienteEnLista>> enRed() async {
    llamadasEnRed++;
    if (redFalla != null) throw redFalla!;
    return red;
  }

  @override
  Future<void> guardarEnCache(List<ClienteEnLista> clientes) async {
    guardados++;
    cache = clientes;
  }
}

ClienteEnLista _c({
  String id = '1',
  String nombre = 'Cliente',
  String tipoDoc = 'CC',
  String? numDoc = '123',
  num saldo = 0,
  num cupo = 0,
  String? segmento,
  bool vencido = false,
}) =>
    ClienteEnLista(
      id: id,
      nombre: nombre,
      tipoDocumento: tipoDoc,
      numeroDocumento: numDoc,
      saldo: saldo,
      cupo: cupo,
      segmento: segmento,
      vencido: vencido,
    );

Future<void> _montar(WidgetTester tester, _RepoFake repo,
    {Size size = const Size(390, 844)}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeClientesProvider.overrideWithValue(repo)],
    child: const MaterialApp(home: PantallaClientes()),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('Criterio 1: el buscador filtra por nombre o documento sin recargar',
      (tester) async {
    final repo = _RepoFake(red: [
      _c(id: '1', nombre: 'Materiales Cruz S.A.S.', tipoDoc: 'NIT', numDoc: '900412883'),
      _c(id: '2', nombre: 'Jorge Rendón Ospina', tipoDoc: 'CC', numDoc: '71884203'),
      _c(id: '3', nombre: 'Ferremax del Norte', tipoDoc: 'NIT', numDoc: '901556019'),
    ]);
    await _montar(tester, repo);
    expect(repo.llamadasEnRed, 1);

    await tester.enterText(find.byType(TextField), 'cruz');
    await tester.pump();
    expect(find.text('Materiales Cruz S.A.S.'), findsOneWidget);
    expect(find.text('Jorge Rendón Ospina'), findsNothing);

    await tester.enterText(find.byType(TextField), '718');
    await tester.pump();
    expect(find.text('Jorge Rendón Ospina'), findsOneWidget);
    expect(find.text('Materiales Cruz S.A.S.'), findsNothing);

    expect(repo.llamadasEnRed, 1); // nunca volvió a pedir
  });

  testWidgets('Criterio 2: sin conexión se ve la copia local con aviso', (tester) async {
    final repo = _RepoFake(
      cache: [_c(id: '1', nombre: 'Luz Elena Vargas'), _c(id: '2', nombre: 'Ferremax del Norte')],
      redFalla: const ErrorDeRed(),
    );
    await _montar(tester, repo);

    expect(find.text('Luz Elena Vargas'), findsOneWidget);
    expect(find.text('Ferremax del Norte'), findsOneWidget);
    expect(find.textContaining('sin conexión'), findsOneWidget);
  });

  testWidgets('Criterio 3: los filtros responden al instante y no recargan', (tester) async {
    final repo = _RepoFake(red: [
      _c(id: '1', nombre: 'Mayorista Sin Saldo', saldo: 0, segmento: 'MAYORISTA'),
      _c(id: '2', nombre: 'Minorista Con Saldo', saldo: 500000, cupo: 1000000),
      _c(id: '3', nombre: 'Minorista Sin Saldo', saldo: 0),
    ]);
    await _montar(tester, repo);

    await tester.tap(find.bySemanticsLabel('Con saldo'));
    await tester.pump();
    expect(find.text('Minorista Con Saldo'), findsOneWidget);
    expect(find.text('Mayorista Sin Saldo'), findsNothing);

    await tester.tap(find.bySemanticsLabel('Mayoristas'));
    await tester.pump();
    expect(find.text('Mayorista Sin Saldo'), findsOneWidget);
    expect(find.text('Minorista Con Saldo'), findsNothing);

    await tester.tap(find.bySemanticsLabel('Vencidos'));
    await tester.pump();
    expect(find.text('Minorista Con Saldo'), findsNothing);

    expect(repo.llamadasEnRed, 1);
  });

  testWidgets('El mismo widget se adapta con LayoutBuilder a móvil y a escritorio',
      (tester) async {
    await _montar(tester, _RepoFake(red: [_c()]));
    expect(find.byKey(const Key('clientes-movil')), findsOneWidget);
    expect(find.byKey(const Key('clientes-escritorio')), findsNothing);

    await tester.binding.setSurfaceSize(const Size(1280, 900));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('clientes-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('clientes-movil')), findsNothing);
    expect(find.byType(PantallaClientes), findsOneWidget);
  });

  testWidgets('En móvil los chips y las filas cumplen el objetivo de toque de 44 px',
      (tester) async {
    final repo = _RepoFake(red: [_c(nombre: 'Materiales Cruz S.A.S.', saldo: 100)]);
    await _montar(tester, repo);

    expect(tester.getSize(find.bySemanticsLabel('Con saldo')).height,
        greaterThanOrEqualTo(44));

    final fila = find
        .ancestor(of: find.text('Materiales Cruz S.A.S.'), matching: find.byType(InkWell))
        .first;
    expect(tester.getSize(fila).height, greaterThanOrEqualTo(44));
  });
}
