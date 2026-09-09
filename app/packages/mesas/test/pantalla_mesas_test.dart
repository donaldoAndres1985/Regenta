import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_mesas/regenta_mesas.dart';

/// Doble del repositorio: guarda en memoria lo que la pantalla le pide.
class _RepoFake implements RepositorioDeMesas {
  _RepoFake({PlanoDelSalon? plano}) : _plano = plano ?? PlanoDelSalon.vacio;

  PlanoDelSalon _plano;
  ErrorDeApi? errorAlCargar;
  ErrorDeApi? errorAlCrear;
  ErrorDeApi? errorAlEliminar;
  ErrorDeApi? errorAlAbrir;

  final List<({String id, int x, int y})> movimientos = [];
  final List<String> eliminadas = [];
  final List<String> creadas = [];
  final List<({String mesaId, int comensales})> aperturas = [];
  final List<String> cuentasPedidas = [];
  final List<String> sesionesCerradas = [];
  final List<String> limpiadas = [];
  final Map<String, SesionDeMesa> _sesionPorMesa = {};

  @override
  Future<PlanoDelSalon> plano() async {
    if (errorAlCargar != null) throw errorAlCargar!;
    return _plano;
  }

  @override
  Future<MesaEnPlano> crearMesa({
    required String codigo,
    String? zonaId,
    String? nombre,
    int? capacidad,
    String? forma,
    int posX = 0,
    int posY = 0,
  }) async {
    if (errorAlCrear != null) throw errorAlCrear!;
    creadas.add(codigo);
    final nueva = MesaEnPlano(
      id: 'nueva-$codigo',
      codigo: codigo,
      zonaId: zonaId,
      nombre: nombre,
      capacidad: capacidad ?? 4,
      forma: forma ?? 'CUADRADA',
      estado: 'LIBRE',
      posX: posX,
      posY: posY,
      ancho: 80,
      alto: 80,
    );
    _plano = PlanoDelSalon(
      zonas: _plano.zonas,
      sinZona: [..._plano.sinZona, nueva],
    );
    return nueva;
  }

  @override
  Future<MesaEnPlano> moverMesa(String mesaId, int posX, int posY) async {
    movimientos.add((id: mesaId, x: posX, y: posY));
    return _mesa(mesaId).conPosicion(posX, posY);
  }

  @override
  Future<void> eliminarMesa(String mesaId) async {
    if (errorAlEliminar != null) throw errorAlEliminar!;
    eliminadas.add(mesaId);
    _plano = PlanoDelSalon(
      zonas: [
        for (final z in _plano.zonas)
          ZonaConMesas(
            id: z.id,
            nombre: z.nombre,
            orden: z.orden,
            color: z.color,
            mesas: z.mesas.where((m) => m.id != mesaId).toList(),
          ),
      ],
      sinZona: _plano.sinZona.where((m) => m.id != mesaId).toList(),
    );
  }

  @override
  Future<void> crearZona({required String nombre, int? orden, String? color}) async {}

  @override
  Future<SesionDeMesa?> sesionDe(String mesaId) async => _sesionPorMesa[mesaId];

  @override
  Future<SesionDeMesa> abrirSesion(String mesaId, int numComensales) async {
    if (errorAlAbrir != null) throw errorAlAbrir!;
    aperturas.add((mesaId: mesaId, comensales: numComensales));
    final s = SesionDeMesa(
      id: 'ses-$mesaId',
      mesaPrincipalId: mesaId,
      estado: 'ABIERTA',
      numComensales: numComensales,
      minutosAbierta: 0,
    );
    _sesionPorMesa[mesaId] = s;
    _cambiarEstado(mesaId, 'OCUPADA');
    return s;
  }

  @override
  Future<void> pedirCuenta(String sesionId) async {
    cuentasPedidas.add(sesionId);
  }

  @override
  Future<void> cerrarSesion(String sesionId) async {
    sesionesCerradas.add(sesionId);
    String? mesaId;
    _sesionPorMesa.forEach((k, v) {
      if (v.id == sesionId) mesaId = k;
    });
    if (mesaId != null) {
      _sesionPorMesa.remove(mesaId);
      _cambiarEstado(mesaId!, 'SUCIA');
    }
  }

  @override
  Future<void> marcarLimpia(String mesaId) async {
    limpiadas.add(mesaId);
    _cambiarEstado(mesaId, 'LIBRE');
  }

  void _cambiarEstado(String mesaId, String estado) {
    MesaEnPlano mapear(MesaEnPlano m) => m.id != mesaId
        ? m
        : MesaEnPlano(
            id: m.id,
            zonaId: m.zonaId,
            codigo: m.codigo,
            nombre: m.nombre,
            capacidad: m.capacidad,
            forma: m.forma,
            estado: estado,
            posX: m.posX,
            posY: m.posY,
            ancho: m.ancho,
            alto: m.alto,
          );
    _plano = PlanoDelSalon(
      zonas: [
        for (final z in _plano.zonas)
          ZonaConMesas(
            id: z.id,
            nombre: z.nombre,
            orden: z.orden,
            color: z.color,
            mesas: z.mesas.map(mapear).toList(),
          ),
      ],
      sinZona: _plano.sinZona.map(mapear).toList(),
    );
  }

  MesaEnPlano _mesa(String id) => _plano.todas.firstWhere((m) => m.id == id);
}

MesaEnPlano _m({
  String id = 'm1',
  String codigo = 'M1',
  String estado = 'LIBRE',
  String? zonaId,
  int capacidad = 4,
  int posX = 0,
  int posY = 0,
}) =>
    MesaEnPlano(
      id: id,
      codigo: codigo,
      estado: estado,
      zonaId: zonaId,
      capacidad: capacidad,
      forma: 'CUADRADA',
      posX: posX,
      posY: posY,
      ancho: 80,
      alto: 80,
    );

PlanoDelSalon _planoCon(List<MesaEnPlano> sinZona, {List<ZonaConMesas> zonas = const []}) =>
    PlanoDelSalon(zonas: zonas, sinZona: sinZona);

Future<void> _montar(WidgetTester tester, _RepoFake repo,
    {Size size = const Size(390, 844), bool puedeEditar = true}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeMesasProvider.overrideWithValue(repo)],
    child: MaterialApp(home: PantallaMesas(puedeEditar: puedeEditar)),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('Criterio 1: en modo edición se crea una mesa con código, capacidad, zona y forma',
      (tester) async {
    final repo = _RepoFake(
      plano: _planoCon(const [], zonas: [
        const ZonaConMesas(id: 'z1', nombre: 'Terraza', orden: 0, mesas: []),
      ]),
    );
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesas-toggle-edicion')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('mesas-anadir')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('mesa-form-codigo')), 't7');
    await tester.enterText(find.byKey(const Key('mesa-form-capacidad')), '6');
    await tester.tap(find.byKey(const Key('mesa-form-guardar')));
    await tester.pumpAndSettle();

    expect(repo.creadas, ['t7']);
    expect(find.byKey(const Key('mesa-nueva-t7')), findsOneWidget);
  });

  testWidgets('Criterio 2: un código repetido deja el formulario abierto con el mensaje del 409',
      (tester) async {
    final repo = _RepoFake(plano: _planoCon(const []))
      ..errorAlCrear = const RecursoDuplicado('Ya hay una mesa con el código M1');
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesas-toggle-edicion')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('mesas-anadir')));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('mesa-form-codigo')), 'M1');
    await tester.tap(find.byKey(const Key('mesa-form-guardar')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('mesa-form-guardar')), findsOneWidget); // sigue abierto
    expect(find.text('Ya hay una mesa con el código M1'), findsOneWidget);
  });

  testWidgets('Criterio 3: arrastrar una mesa en modo edición guarda su nueva posición',
      (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1', codigo: 'M1', posX: 10, posY: 10)]));
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesas-toggle-edicion')));
    await tester.pumpAndSettle();

    // Horizontal para no competir con el scroll vertical de la lista, en pasos
    // pequeños para que el pan recognizer supere su "slop" y siga reportando. Se
    // comprueba que la posición se guardó y avanzó a la derecha, no el píxel
    // exacto (eso lo cubren MesaTest.mover y GestionDeMesasTest.moverPersiste).
    final centro = tester.getCenter(find.byKey(const Key('mesa-m1')));
    final gesto = await tester.startGesture(centro);
    for (var i = 0; i < 10; i++) {
      await gesto.moveBy(const Offset(12, 0));
      await tester.pump();
    }
    await gesto.up();
    await tester.pumpAndSettle();

    expect(repo.movimientos, hasLength(1));
    expect(repo.movimientos.single.id, 'm1');
    expect(repo.movimientos.single.x, greaterThan(10));
    expect(repo.movimientos.single.y, 10);
  });

  testWidgets('Fuera del modo edición la mesa no se arrastra', (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1')]));
    await _montar(tester, repo);

    await tester.drag(find.byKey(const Key('mesa-m1')), const Offset(40, 25));
    await tester.pumpAndSettle();

    expect(repo.movimientos, isEmpty);
  });

  testWidgets('Criterio 4: borrar una mesa con sesión abierta muestra el mensaje del 409',
      (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1', codigo: 'M1', estado: 'OCUPADA')]))
      ..errorAlEliminar =
          const RecursoDuplicado('La mesa tiene una sesión abierta; ciérrala antes de borrarla');
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesas-toggle-edicion')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('mesa-borrar-m1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('mesas-confirmar-borrado')));
    await tester.pumpAndSettle();

    expect(repo.eliminadas, isEmpty);
    expect(find.text('La mesa tiene una sesión abierta; ciérrala antes de borrarla'),
        findsOneWidget);
  });

  testWidgets('Una mesa libre se borra y desaparece del plano', (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1', codigo: 'M1')]));
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesas-toggle-edicion')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('mesa-borrar-m1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('mesas-confirmar-borrado')));
    await tester.pumpAndSettle();

    expect(repo.eliminadas, ['m1']);
    expect(find.byKey(const Key('mesa-m1')), findsNothing);
  });

  testWidgets('R3: el filtro de zona no vuelve a pedir el plano', (tester) async {
    final repo = _RepoFake(
      plano: _planoCon([
        _m(id: 'b1', codigo: 'B1'),
      ], zonas: [
        ZonaConMesas(id: 'z1', nombre: 'Salón', orden: 0, mesas: [_m(id: 's1', codigo: 'S1', zonaId: 'z1')]),
        ZonaConMesas(id: 'z2', nombre: 'Terraza', orden: 1, mesas: [_m(id: 't1', codigo: 'T1', zonaId: 'z2')]),
      ]),
    );
    await _montar(tester, repo);

    expect(find.byKey(const Key('mesa-s1')), findsOneWidget);
    expect(find.byKey(const Key('mesa-t1')), findsOneWidget);

    await tester.tap(find.bySemanticsLabel('Salón'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('mesa-s1')), findsOneWidget);
    expect(find.byKey(const Key('mesa-t1')), findsNothing);
    expect(find.byKey(const Key('mesa-b1')), findsNothing);

    await tester.tap(find.bySemanticsLabel('Sin zona'));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('mesa-b1')), findsOneWidget);
    expect(find.byKey(const Key('mesa-s1')), findsNothing);
  });

  testWidgets('El mismo widget se adapta con LayoutBuilder a móvil y a escritorio',
      (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m()]));
    await _montar(tester, repo);
    expect(find.byKey(const Key('mesas-movil')), findsOneWidget);
    expect(find.byKey(const Key('mesas-escritorio')), findsNothing);

    await tester.binding.setSurfaceSize(const Size(1280, 900));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('mesas-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('mesas-movil')), findsNothing);
  });

  testWidgets('Sin permiso de edición no se ve el botón de editar', (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m()]));
    await _montar(tester, repo, puedeEditar: false);
    expect(find.byKey(const Key('mesas-toggle-edicion')), findsNothing);
  });

  testWidgets('El plano vacío lo dice; un error de red ofrece reintentar', (tester) async {
    final repo = _RepoFake()..errorAlCargar = const ErrorDeRed();
    await _montar(tester, repo);
    expect(find.text('No se pudo cargar el plano'), findsOneWidget);

    repo.errorAlCargar = null;
    await tester.tap(find.byKey(const Key('mesas-reintentar')));
    await tester.pumpAndSettle();
    expect(find.text('Todavía no hay mesas en el salón'), findsOneWidget);
  });

  testWidgets('En móvil los chips de zona cumplen el objetivo de toque de 44 px',
      (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m()]));
    await _montar(tester, repo);
    expect(tester.getSize(find.bySemanticsLabel('Todas')).height,
        greaterThanOrEqualTo(44));
  });

  // ---- HU-082 ----

  testWidgets('Criterio 1: tocar una mesa libre la abre con N comensales y queda OCUPADA',
      (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1', codigo: 'M1')]));
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesa-m1')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('hoja-comensales')), '3');
    await tester.tap(find.byKey(const Key('hoja-abrir')));
    await tester.pumpAndSettle();

    expect(repo.aperturas, [(mesaId: 'm1', comensales: 3)]);
    // La hoja se cerró y la mesa se repintó como ocupada.
    expect(find.byKey(const Key('hoja-abrir')), findsNothing);
    expect(find.descendant(
            of: find.byKey(const Key('mesa-m1')), matching: find.text('Ocupada')),
        findsOneWidget);
  });

  testWidgets('Criterio 2: si la mesa ya tiene sesión, el 409 se muestra y la hoja no se cierra',
      (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1')]))
      ..errorAlAbrir = const RecursoDuplicado('Esa mesa ya tiene una sesión abierta');
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesa-m1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('hoja-abrir')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('hoja-abrir')), findsOneWidget);
    expect(find.text('Esa mesa ya tiene una sesión abierta'), findsOneWidget);
  });

  testWidgets('Criterio 3: cerrar la sesión desde la hoja deja la mesa por limpiar (SUCIA)',
      (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1', codigo: 'M1', estado: 'OCUPADA')]));
    repo._sesionPorMesa['m1'] = const SesionDeMesa(
      id: 'ses-m1',
      mesaPrincipalId: 'm1',
      estado: 'ABIERTA',
      numComensales: 4,
      minutosAbierta: 52,
    );
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesa-m1')));
    await tester.pumpAndSettle();
    expect(find.text('4 comensales · abierta hace 52 min'), findsOneWidget);

    await tester.tap(find.byKey(const Key('hoja-cerrar')));
    await tester.pumpAndSettle();

    expect(repo.sesionesCerradas, ['ses-m1']);
    expect(find.descendant(
            of: find.byKey(const Key('mesa-m1')), matching: find.text('Por limpiar')),
        findsOneWidget);
  });

  testWidgets('Criterio 4: marcar limpia devuelve la mesa a LIBRE', (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1', codigo: 'M1', estado: 'SUCIA')]));
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('mesa-m1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('hoja-limpiar')));
    await tester.pumpAndSettle();

    expect(repo.limpiadas, ['m1']);
    expect(find.descendant(
            of: find.byKey(const Key('mesa-m1')), matching: find.text('Libre')),
        findsOneWidget);
  });

  testWidgets('En modo edición tocar una mesa no abre la hoja de sesión', (tester) async {
    final repo = _RepoFake(plano: _planoCon([_m(id: 'm1')]));
    await _montar(tester, repo);
    await tester.tap(find.byKey(const Key('mesas-toggle-edicion')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('mesa-m1')), warnIfMissed: false);
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('hoja-abrir')), findsNothing);
    expect(repo.aperturas, isEmpty);
  });
}
