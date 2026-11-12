import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
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

  /// Lo que el servidor responde al guardar; `null` es "lo aceptó".
  ErrorDeApi? guardarFalla;
  final List<FichaDeCliente> guardadas = [];
  Map<String, FichaDeCliente> fichas = {};

  @override
  Future<FichaDeCliente> ver(String clienteId) async => fichas[clienteId]!;

  @override
  Future<ClienteEnLista> guardar(FichaDeCliente ficha) async {
    if (guardarFalla != null) throw guardarFalla!;
    guardadas.add(ficha);
    final guardado = _c(
      id: ficha.id ?? 'nuevo',
      nombre: ficha.razonSocial ?? '${ficha.nombres ?? ''} ${ficha.apellidos ?? ''}'.trim(),
      numDoc: ficha.numeroDocumento,
      tipoDoc: ficha.tipoDocumento,
    );
    red = [guardado, ...red.where((c) => c.id != guardado.id)];
    return guardado;
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
    expect(find.text('MATERIALES CRUZ S.A.S.'), findsOneWidget);
    expect(find.text('JORGE RENDÓN OSPINA'), findsNothing);

    await tester.enterText(find.byType(TextField), '718');
    await tester.pump();
    expect(find.text('JORGE RENDÓN OSPINA'), findsOneWidget);
    expect(find.text('MATERIALES CRUZ S.A.S.'), findsNothing);

    expect(repo.llamadasEnRed, 1); // nunca volvió a pedir
  });

  testWidgets('Criterio 2: sin conexión se ve la copia local con aviso', (tester) async {
    final repo = _RepoFake(
      cache: [_c(id: '1', nombre: 'Luz Elena Vargas'), _c(id: '2', nombre: 'Ferremax del Norte')],
      redFalla: const ErrorDeRed(),
    );
    await _montar(tester, repo);

    expect(find.text('LUZ ELENA VARGAS'), findsOneWidget);
    expect(find.text('FERREMAX DEL NORTE'), findsOneWidget);
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
    expect(find.text('MINORISTA CON SALDO'), findsOneWidget);
    expect(find.text('MAYORISTA SIN SALDO'), findsNothing);

    await tester.tap(find.bySemanticsLabel('Mayoristas'));
    await tester.pump();
    expect(find.text('MAYORISTA SIN SALDO'), findsOneWidget);
    expect(find.text('MINORISTA CON SALDO'), findsNothing);

    await tester.tap(find.bySemanticsLabel('Vencidos'));
    await tester.pump();
    expect(find.text('MINORISTA CON SALDO'), findsNothing);

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
        .ancestor(of: find.text('MATERIALES CRUZ S.A.S.'), matching: find.byType(InkWell))
        .first;
    expect(tester.getSize(fila).height, greaterThanOrEqualTo(44));
  });

  testWidgets('Gestión: «Nuevo cliente» está siempre a la mano, crea y lo deja en el listado',
      (tester) async {
    final repo = _RepoFake(red: [_c(id: '1', nombre: 'Ferremax del Norte')]);
    await _montar(tester, repo, size: const Size(1280, 900));

    await tester.tap(find.byKey(const Key('clientes-nuevo')));
    await tester.pumpAndSettle();
    expect(find.text('Nuevo cliente'), findsWidgets);

    await tester.enterText(find.byKey(const Key('cliente-numero')), '71884203');
    await tester.enterText(find.byKey(const Key('cliente-nombres')), 'Jorge');
    await tester.enterText(find.byKey(const Key('cliente-apellidos')), 'Rendón');
    await tester.enterText(find.byKey(const Key('cliente-telefono')), '3001234567');
    await tester.tap(find.byKey(const Key('cliente-guardar')));
    await tester.pumpAndSettle();

    expect(repo.guardadas, hasLength(1));
    expect(repo.guardadas.single.id, isNull);
    expect(repo.guardadas.single.aJson()['numeroDocumento'], '71884203');
    expect(repo.guardadas.single.aJson()['email'], isNull);
    expect(find.text('JORGE RENDÓN'), findsOneWidget);
    expect(find.text('FERREMAX DEL NORTE'), findsOneWidget);
  });

  testWidgets('Gestión: si el servidor no lo acepta se dice por qué y no queda un cliente fantasma',
      (tester) async {
    final repo = _RepoFake(red: [_c(id: '1', nombre: 'Ferremax del Norte')])
      ..guardarFalla = const SinPermiso('No tienes permiso para esta accion');
    await _montar(tester, repo, size: const Size(1280, 900));

    await tester.tap(find.byKey(const Key('clientes-nuevo')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('cliente-numero')), '123');
    await tester.enterText(find.byKey(const Key('cliente-nombres')), 'Fantasma');
    await tester.tap(find.byKey(const Key('cliente-guardar')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('cliente-error')), findsOneWidget);
    expect(find.textContaining('No tienes permiso'), findsOneWidget);

    await tester.tap(find.text('Cancelar'));
    await tester.pumpAndSettle();
    expect(find.text('FANTASMA'), findsNothing);
    expect(find.text('1 cliente'), findsOneWidget);
  });

  testWidgets('Gestión: sin lo mínimo no se manda nada al servidor', (tester) async {
    final repo = _RepoFake();
    await _montar(tester, repo, size: const Size(1280, 900));

    await tester.tap(find.byKey(const Key('clientes-nuevo')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('cliente-guardar')));
    await tester.pumpAndSettle();

    expect(find.textContaining('Falta el número de documento'), findsOneWidget);
    expect(repo.guardadas, isEmpty);
  });

  testWidgets('Gestión: tocar un cliente abre su ficha y guarda los cambios sobre el mismo id',
      (tester) async {
    final repo = _RepoFake(red: [
      _c(id: '7', nombre: 'Materiales Cruz S.A.S.', tipoDoc: 'NIT', numDoc: '900412883'),
    ])
      ..fichas = {
        '7': const FichaDeCliente(
          id: '7',
          tipoPersona: 'JURIDICA',
          tipoDocumento: 'NIT',
          numeroDocumento: '900412883',
          digitoVerificacion: '1',
          razonSocial: 'Materiales Cruz S.A.S.',
          notas: 'Paga los viernes',
        ),
      };
    await _montar(tester, repo, size: const Size(1280, 900));

    await tester.tap(find.text('MATERIALES CRUZ S.A.S.'));
    await tester.pumpAndSettle();
    expect(find.text('Editar cliente'), findsOneWidget);

    await tester.enterText(
        find.byKey(const Key('cliente-razon-social')), 'Materiales Cruz y Cía');
    await tester.tap(find.byKey(const Key('cliente-guardar')));
    await tester.pumpAndSettle();

    final enviada = repo.guardadas.single;
    expect(enviada.id, '7');
    expect(enviada.aJson()['razonSocial'], 'Materiales Cruz y Cía');
    // Lo que no se tocó viaja igual: guardar no puede borrar las notas.
    expect(enviada.aJson()['notas'], 'Paga los viernes');
    expect(find.text('MATERIALES CRUZ Y CÍA'), findsOneWidget);
    expect(find.text('MATERIALES CRUZ S.A.S.'), findsNothing);
  });

  testWidgets('Foco inicial en buscador al entrar a clientes', (tester) async {
    final repo = _RepoFake(red: [_c()]);
    await _montar(tester, repo);

    final editable = tester.widget<EditableText>(find.byType(EditableText).first);
    expect(editable.focusNode.hasFocus, isTrue);
  });

  testWidgets('Escape cierra el formulario de cliente', (tester) async {
    final repo = _RepoFake(red: [_c()]);
    await _montar(tester, repo, size: const Size(1280, 900));

    await tester.tap(find.byKey(const Key('clientes-nuevo')));
    await tester.pumpAndSettle();
    expect(find.byType(FormularioDeCliente), findsOneWidget);

    await tester.sendKeyEvent(LogicalKeyboardKey.escape);
    await tester.pumpAndSettle();
    expect(find.byType(FormularioDeCliente), findsNothing);
  });
}
