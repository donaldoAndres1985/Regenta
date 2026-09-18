import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_comandas/regenta_comandas.dart';

class _RepoFake implements RepositorioDeComandas {
  _RepoFake({ComandaVista? comanda, this.items = const [], this.grupos = const []})
      : _comanda = comanda ?? _comandaVacia();

  ComandaVista _comanda;
  List<ItemDeCarta> items;
  List<GrupoModificadores> grupos;
  List<CategoriaDeCarta> categorias = const [];
  ErrorDeApi? errorAlAgregar;
  bool encolarAgregar = false;
  bool encolarEnvio = false;
  int llamadasAComanda = 0;
  final List<({String itemId, num cantidad, List<String> mods, String? notas})> agregadas = [];
  final List<String> avanzadas = [];

  static ComandaVista _comandaVacia() => const ComandaVista(
        id: 'c1',
        numero: 'CMD-0001',
        estado: 'ABIERTA',
        numComensales: 4,
        subtotal: 0,
        impuestoTotal: 0,
        propinaSugerida: 0,
        total: 0,
        lineas: [],
      );

  @override
  Future<ComandaVista> comanda(String comandaId) async {
    llamadasAComanda++;
    return _comanda;
  }

  @override
  Future<List<ItemDeCarta>> itemsDeCarta(String cartaId) async => items;

  @override
  Future<List<CategoriaDeCarta>> categoriasDeCarta(String cartaId) async =>
      categorias.isNotEmpty ? categorias : [CategoriaDeCarta(nombre: 'Todos', items: items)];

  @override
  Future<List<GrupoModificadores>> gruposDeItem(String itemId) async => grupos;

  @override
  Future<ResultadoDeComanda> agregarLinea(
    String comandaId, {
    required String itemMenuId,
    required num cantidad,
    List<String> modificadorIds = const [],
    String? notas,
  }) async {
    if (errorAlAgregar != null) throw errorAlAgregar!;
    if (encolarAgregar) return const ResultadoDeComanda.encolado();
    agregadas.add((itemId: itemMenuId, cantidad: cantidad, mods: modificadorIds, notas: notas));
    final item = items.firstWhere((i) => i.id == itemMenuId);
    final nueva = LineaVista(
      id: 'l${_comanda.lineas.length + 1}',
      linea: _comanda.lineas.length + 1,
      nombre: item.nombre,
      estado: 'PENDIENTE',
      curso: 'FUERTE',
      secuenciaEnvio: 1,
      cantidad: cantidad,
      total: item.precio * cantidad,
      notas: notas,
      modificadores: const [],
    );
    final lineas = [..._comanda.lineas, nueva];
    final total = lineas.fold<num>(0, (s, l) => s + l.total);
    _comanda = ComandaVista(
      id: _comanda.id,
      numero: _comanda.numero,
      estado: _comanda.estado,
      numComensales: _comanda.numComensales,
      subtotal: total,
      impuestoTotal: 0,
      propinaSugerida: (total * 0.1).round(),
      total: total,
      lineas: lineas,
    );
    return ResultadoDeComanda.confirmado(_comanda);
  }

  @override
  Future<ResultadoDeComanda> enviarACocina(String comandaId) async {
    if (encolarEnvio) return const ResultadoDeComanda.encolado();
    _comanda = ComandaVista(
      id: _comanda.id,
      numero: _comanda.numero,
      estado: 'EN_COCINA',
      numComensales: _comanda.numComensales,
      subtotal: _comanda.subtotal,
      impuestoTotal: 0,
      propinaSugerida: _comanda.propinaSugerida,
      total: _comanda.total,
      lineas: [
        for (final l in _comanda.lineas)
          l.estado == 'PENDIENTE' ? _conEstado(l, 'ENVIADA') : l,
      ],
    );
    return ResultadoDeComanda.confirmado(_comanda);
  }

  @override
  Future<ComandaVista> avanzarLinea(String comandaId, String lineaId) async {
    avanzadas.add(lineaId);
    _comanda = ComandaVista(
      id: _comanda.id,
      numero: _comanda.numero,
      estado: _comanda.estado,
      numComensales: _comanda.numComensales,
      subtotal: _comanda.subtotal,
      impuestoTotal: 0,
      propinaSugerida: _comanda.propinaSugerida,
      total: _comanda.total,
      lineas: [
        for (final l in _comanda.lineas)
          l.id == lineaId ? _conEstado(l, l.siguienteEstado ?? l.estado) : l,
      ],
    );
    return _comanda;
  }

  LineaVista _conEstado(LineaVista l, String estado) => LineaVista(
        id: l.id,
        linea: l.linea,
        nombre: l.nombre,
        estado: estado,
        curso: l.curso,
        secuenciaEnvio: l.secuenciaEnvio,
        cantidad: l.cantidad,
        total: l.total,
        notas: l.notas,
        demoraMin: l.demoraMin,
        modificadores: l.modificadores,
      );
}

ItemDeCarta _it(String id, String nombre, num precio) =>
    ItemDeCarta(id: id, nombre: nombre, precio: precio, disponible: true);

Future<void> _montar(WidgetTester tester, _RepoFake repo,
    {Size size = const Size(390, 844), String? cartaId = 'carta1'}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeComandasProvider.overrideWithValue(repo)],
    child: MaterialApp(home: PantallaComanda(comandaId: 'c1', cartaId: cartaId)),
  ));
  await tester.pumpAndSettle();
}

/// Ejercita el selector de lista de escritorio (HU-091 R12: en móvil «Añadir»
/// es la rejilla por categorías, ver `_HojaAnadirCategorias` más abajo).
Future<void> _abrirYAgregar(WidgetTester tester, String itemId,
    {String cantidad = '2'}) async {
  await tester.binding.setSurfaceSize(const Size(1280, 900));
  await tester.pumpAndSettle();
  await tester.tap(find.byKey(const Key('comanda-anadir')));
  await tester.pumpAndSettle();
  await tester.tap(find.byKey(const Key('anadir-item')));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Bandeja paisa · \$32.000').last);
  await tester.pumpAndSettle();
  await tester.enterText(find.byKey(const Key('anadir-cantidad')), cantidad);
  await tester.tap(find.byKey(const Key('anadir-guardar')));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('Criterios 2 y 3: agregar una línea muestra la línea y actualiza el total',
      (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)]);
    await _montar(tester, repo);

    expect(find.text('Todavía no hay líneas. Pulsa «Añadir».'), findsOneWidget);
    await _abrirYAgregar(tester, 'bandeja', cantidad: '2');

    expect(repo.agregadas.single.itemId, 'bandeja');
    expect(find.byKey(const Key('linea-l1')), findsOneWidget);
    expect(find.descendant(
            of: find.byKey(const Key('linea-l1')), matching: find.text('Bandeja paisa')),
        findsOneWidget);
    expect(find.descendant(
            of: find.byKey(const Key('comanda-total')), matching: find.text('\$64.000')),
        findsNothing); // el total va en su propio widget
    expect(find.text('\$64.000'), findsWidgets);
  });

  testWidgets('Criterio 1: se puede agregar tras enviar a cocina; la nueva línea nace PENDIENTE',
      (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)]);
    await _montar(tester, repo);
    await _abrirYAgregar(tester, 'bandeja', cantidad: '1');

    await tester.tap(find.byKey(const Key('comanda-enviar')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('enviar-confirmar'))); // HU-091 criterio 5
    await tester.pumpAndSettle();
    expect(find.descendant(
            of: find.byKey(const Key('comanda-estado')), matching: find.text('EN COCINA')),
        findsOneWidget);
    expect(find.descendant(
            of: find.byKey(const Key('linea-l1')), matching: find.text('ENVIADA')),
        findsOneWidget);

    await _abrirYAgregar(tester, 'bandeja', cantidad: '1');
    expect(find.descendant(
            of: find.byKey(const Key('linea-l2')), matching: find.text('PENDIENTE')),
        findsOneWidget);
  });

  testWidgets('Criterio 5: si faltan modificadores obligatorios, el 422 se muestra y la hoja no se cierra',
      (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)])
      ..errorAlAgregar = const ErroresDeValidacion({
        'modificadorIds': ['Falta elegir en un grupo obligatorio de modificadores'],
      });
    await _montar(tester, repo, size: const Size(1280, 900)); // selector de escritorio

    await tester.tap(find.byKey(const Key('comanda-anadir')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('anadir-item')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Bandeja paisa · \$32.000').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('anadir-guardar')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('anadir-guardar')), findsOneWidget); // sigue abierta
    expect(find.text('Falta elegir en un grupo obligatorio de modificadores'), findsOneWidget);
    expect(repo.agregadas, isEmpty);
  });

  testWidgets('Un modificador elegido viaja en la petición', (tester) async {
    final repo = _RepoFake(
      items: [_it('bandeja', 'Bandeja paisa', 32000)],
      grupos: [
        const GrupoModificadores(id: 'g1', nombre: 'Término', min: 1, max: 1, opciones: [
          OpcionModificador(id: 'm1', nombre: 'Término medio', precioExtra: 0),
          OpcionModificador(id: 'm2', nombre: 'Tres cuartos', precioExtra: 0),
        ]),
      ],
    );
    await _montar(tester, repo, size: const Size(1280, 900)); // selector de escritorio

    await tester.tap(find.byKey(const Key('comanda-anadir')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('anadir-item')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Bandeja paisa · \$32.000').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('anadir-mod-m2')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('anadir-guardar')));
    await tester.pumpAndSettle();

    expect(repo.agregadas.single.mods, ['m2']);
  });

  testWidgets('El mismo widget se adapta con LayoutBuilder a móvil y a escritorio',
      (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)]);
    await _montar(tester, repo);
    expect(find.byKey(const Key('comanda-movil')), findsOneWidget);

    await tester.binding.setSurfaceSize(const Size(1280, 900));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('comanda-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('comanda-movil')), findsNothing);
  });

  testWidgets('Sin cartaId, el botón Añadir queda deshabilitado', (tester) async {
    final repo = _RepoFake();
    await _montar(tester, repo, cartaId: null);
    final boton = tester.widget<OutlinedButton>(find.byKey(const Key('comanda-anadir')));
    expect(boton.onPressed, isNull);
  });

  // ---- HU-091 ----

  testWidgets('Criterio 5: cancelar el diálogo no envía a cocina', (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)]);
    await _montar(tester, repo);
    await _abrirYAgregar(tester, 'bandeja', cantidad: '1');

    await tester.tap(find.byKey(const Key('comanda-enviar')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('enviar-cancelar')));
    await tester.pumpAndSettle();

    expect(find.descendant(
            of: find.byKey(const Key('linea-l1')), matching: find.text('PENDIENTE')),
        findsOneWidget);
  });

  testWidgets('Criterio 1: en el celular, un ítem sin modificadores se agrega de un toque',
      (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)]);
    await _montar(tester, repo); // 390x844 por defecto

    await tester.tap(find.byKey(const Key('comanda-anadir')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('anadir-categoria-0')), findsOneWidget);
    await tester.tap(find.byKey(const Key('anadir-item-bandeja')));
    await tester.pumpAndSettle();

    final agregada = repo.agregadas.single;
    expect(agregada.itemId, 'bandeja');
    expect(agregada.cantidad, 1);
    expect(agregada.mods, isEmpty);
    expect(agregada.notas, isNull);
    expect(find.byKey(const Key('anadir-aviso')), findsOneWidget);
  });

  testWidgets('Criterio 2: un ítem con modificadores pide antes de sumarlo', (tester) async {
    final repo = _RepoFake(
      items: [_it('bandeja', 'Bandeja paisa', 32000)],
      grupos: [
        const GrupoModificadores(id: 'g1', nombre: 'Término', min: 1, max: 1, opciones: [
          OpcionModificador(id: 'm1', nombre: 'Término medio', precioExtra: 0),
        ]),
      ],
    );
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('comanda-anadir')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('anadir-item-bandeja')));
    await tester.pumpAndSettle();

    expect(repo.agregadas, isEmpty); // no se agregó de un toque: hay modificadores
    expect(find.byKey(const Key('personalizar-guardar')), findsOneWidget);

    await tester.tap(find.byKey(const Key('personalizar-mod-m1')));
    await tester.tap(find.byKey(const Key('personalizar-guardar')));
    await tester.pumpAndSettle();

    expect(repo.agregadas.single.mods, ['m1']);
  });

  testWidgets('Criterio 3: una pulsación larga permite agregar una nota que viaja con la línea',
      (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)]);
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('comanda-anadir')));
    await tester.pumpAndSettle();
    await tester.longPress(find.byKey(const Key('anadir-item-bandeja')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('personalizar-notas')), 'sin cebolla');
    await tester.tap(find.byKey(const Key('personalizar-guardar')));
    await tester.pumpAndSettle();

    expect(repo.agregadas.single.notas, 'sin cebolla');
  });

  testWidgets('Criterio 4: sin señal, agregar queda encolado y la línea se ve local',
      (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)])
      ..encolarAgregar = true;
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('comanda-anadir')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('anadir-item-bandeja')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('linea-local-1')), findsOneWidget);
    expect(find.textContaining('Sin señal'), findsOneWidget);
  });

  testWidgets('Criterio 4: sin señal, enviar a cocina queda encolado y las líneas se ven ENVIADA',
      (tester) async {
    final repo = _RepoFake(items: [_it('bandeja', 'Bandeja paisa', 32000)]);
    await _montar(tester, repo);
    await _abrirYAgregar(tester, 'bandeja', cantidad: '1');
    repo.encolarEnvio = true;

    await tester.tap(find.byKey(const Key('comanda-enviar')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('enviar-confirmar')));
    await tester.pumpAndSettle();

    expect(find.descendant(
            of: find.byKey(const Key('linea-l1')), matching: find.text('ENVIADA')),
        findsOneWidget);
    expect(find.textContaining('Sin señal'), findsOneWidget);
  });

  // ---- HU-086 ----

  LineaVista lineaVista({
    String id = 'la',
    String nombre = 'Bandeja paisa',
    String estado = 'PENDIENTE',
    String curso = 'FUERTE',
    int secuencia = 1,
    int? demora,
  }) =>
      LineaVista(
        id: id,
        linea: 1,
        nombre: nombre,
        estado: estado,
        curso: curso,
        secuenciaEnvio: secuencia,
        cantidad: 1,
        total: 32000,
        modificadores: const [],
        demoraMin: demora,
      );

  ComandaVista comandaCon(List<LineaVista> lineas, {String estado = 'EN_COCINA'}) => ComandaVista(
        id: 'c1',
        numero: 'CMD-0001',
        estado: estado,
        numComensales: 4,
        subtotal: 0,
        impuestoTotal: 0,
        propinaSugerida: 0,
        total: lineas.fold<num>(0, (s, l) => s + l.total),
        lineas: lineas,
      );

  testWidgets('Criterio 1: tocar una línea la avanza al siguiente estado', (tester) async {
    final repo = _RepoFake(comanda: comandaCon([lineaVista(id: 'la', estado: 'PENDIENTE')]));
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('linea-la')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('linea-avanzar')));
    await tester.pumpAndSettle();

    expect(repo.avanzadas, ['la']);
    expect(find.descendant(
            of: find.byKey(const Key('linea-la')), matching: find.text('ENVIADA')),
        findsOneWidget);
  });

  testWidgets('Criterio 2: cada línea muestra su propio estado', (tester) async {
    final repo = _RepoFake(comanda: comandaCon([
      lineaVista(id: 'la', estado: 'LISTA'),
      lineaVista(id: 'lb', estado: 'PENDIENTE'),
    ]));
    await _montar(tester, repo);

    expect(find.descendant(
            of: find.byKey(const Key('linea-la')), matching: find.text('LISTA')),
        findsOneWidget);
    expect(find.descendant(
            of: find.byKey(const Key('linea-lb')), matching: find.text('PENDIENTE')),
        findsOneWidget);
  });

  testWidgets('Criterio 3: la demora en cocina se ve en la línea', (tester) async {
    final repo = _RepoFake(comanda: comandaCon([lineaVista(id: 'la', estado: 'LISTA', demora: 25)]));
    await _montar(tester, repo);
    expect(find.byKey(const Key('linea-demora-la')), findsOneWidget);
    expect(find.text('25 min'), findsOneWidget);
  });

  testWidgets('Criterio 4: una línea de curso POSTRE con secuencia 2 lo indica', (tester) async {
    final repo = _RepoFake(comanda: comandaCon([
      lineaVista(id: 'la', nombre: 'Postre de natas', curso: 'POSTRE', secuencia: 2),
    ]));
    await _montar(tester, repo);
    expect(find.byKey(const Key('linea-curso-la')), findsOneWidget);
    expect(find.textContaining('va #2'), findsOneWidget);
  });
}
