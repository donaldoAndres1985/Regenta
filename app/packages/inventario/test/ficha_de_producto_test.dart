import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// HU-150 · editar un producto desde la ficha.
class _RepoFicha implements RepositorioDeInventario {
  _RepoFicha(this.ficha);

  FichaDeProducto ficha;
  EdicionDeProducto? guardada;
  ErrorDeApi? errorAlGuardar;

  @override
  Future<FichaDeProducto> verProducto(String id) async => ficha;

  @override
  Future<FichaDeProducto> editarProducto(String id, EdicionDeProducto edicion) async {
    if (errorAlGuardar != null) throw errorAlGuardar!;
    guardada = edicion;
    ficha = FichaDeProducto(
      id: id,
      sku: edicion.sku,
      nombre: edicion.nombre,
      categoriaId: edicion.categoriaId,
      unidadMedidaId: edicion.unidadMedidaId,
      tarifaIva: edicion.tarifaIva,
      precioVenta: edicion.precioVenta,
      costo: edicion.costo,
      version: edicion.version + 1,
    );
    return ficha;
  }

  @override
  Future<List<OpcionDeFicha>> categorias() async =>
      const [OpcionDeFicha(id: 'c1', nombre: 'Pinturas')];

  @override
  Future<List<OpcionDeFicha>> unidades() async =>
      const [OpcionDeFicha(id: 'u1', nombre: 'GLN · Galón')];

  List<(String, num)> existencias = const [];
  List<EquivalenciaDeDetallado> equivalenciasDelNegocio = const [];
  (String, String, num)? detallado;
  ErrorDeApi? errorAlDetallar;

  @override
  Future<List<EquivalenciaDeDetallado>> equivalencias() async => equivalenciasDelNegocio;

  @override
  Future<String> detallar(String origenId, String bodegaId, num cantidad) async {
    if (errorAlDetallar != null) throw errorAlDetallar!;
    detallado = (origenId, bodegaId, cantidad);
    return 'Se abrieron 1 Barniz cuñete → 5 Barniz galón';
  }

  AjusteDeExistencia? ajuste;
  ErrorDeApi? errorAlAjustar;

  @override
  Future<void> ajustarExistencia(AjusteDeExistencia a) async {
    if (errorAlAjustar != null) throw errorAlAjustar!;
    ajuste = a;
  }
  List<FraccionDeTabla> fracciones = const [];
  final Map<String, num?> preciosPropios = {};

  @override
  Future<List<FraccionDeTabla>> fraccionesDe(String id) async => fracciones;

  @override
  Future<void> fijarPrecioPropio(String id, String medida, num precio) async {
    preciosPropios[medida] = precio;
    fracciones = [
      for (final f in fracciones)
        f.medida == medida
            ? FraccionDeTabla(medida: f.medida, factor: f.factor, precio: precio, origen: 'PROPIO')
            : f,
    ];
  }

  @override
  Future<void> quitarPrecioPropio(String id, String medida) async {
    preciosPropios[medida] = null;
    fracciones = [
      for (final f in fracciones)
        f.medida == medida
            ? FraccionDeTabla(medida: f.medida, factor: f.factor, precio: 18000)
            : f,
    ];
  }
  final Map<String, String> ubicacionesGuardadas = {};

  @override
  Future<List<UbicacionEnBodega>> ubicacionesDe(String id) async => const [
        UbicacionEnBodega(bodegaId: 'b1', bodegaNombre: 'Principal', ubicacion: 'Pasillo 3 – E2'),
        UbicacionEnBodega(bodegaId: 'b2', bodegaNombre: 'Bodega 2'),
      ];

  @override
  Future<void> fijarUbicacion(String id, String bodegaId, String ubicacion) async {
    ubicacionesGuardadas[bodegaId] = ubicacion;
  }

  @override
  Future<List<(String bodega, num cantidad)>> existenciasDe(String id) async => existencias;

  @override
  Future<FichaDeProducto> cambiarActivo(String id, {required bool activo}) async {
    ficha = FichaDeProducto(
      id: id,
      sku: ficha.sku,
      nombre: ficha.nombre,
      categoriaId: ficha.categoriaId,
      unidadMedidaId: ficha.unidadMedidaId,
      tarifaIva: ficha.tarifaIva,
      precioVenta: ficha.precioVenta,
      costo: ficha.costo,
      version: ficha.version + 1,
      activo: activo,
    );
    return ficha;
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

/// Retiene cada respuesta hasta que el test la suelta, y anota qué se pidió.
class _RepoLento extends _RepoFicha {
  _RepoLento(super.ficha);

  final List<String> pedidos = [];
  final Completer<void> soltar = Completer<void>();

  Future<T> _espera<T>(String que, Future<T> Function() valor) async {
    pedidos.add(que);
    await soltar.future;
    return valor();
  }

  @override
  Future<FichaDeProducto> verProducto(String id) => _espera('ficha', () => super.verProducto(id));
  @override
  Future<List<OpcionDeFicha>> categorias() => _espera('categorias', super.categorias);
  @override
  Future<List<OpcionDeFicha>> unidades() => _espera('unidades', super.unidades);
  @override
  Future<List<UbicacionEnBodega>> ubicacionesDe(String id) =>
      _espera('ubicaciones', () => super.ubicacionesDe(id));
  @override
  Future<List<FraccionDeTabla>> fraccionesDe(String id) =>
      _espera('fracciones', () => super.fraccionesDe(id));
  @override
  Future<List<EquivalenciaDeDetallado>> equivalencias() =>
      _espera('equivalencias', super.equivalencias);
}

const _barniz = FichaDeProducto(
  id: 'p1',
  sku: '7707031403512',
  nombre: 'Barniz galón',
  categoriaId: 'c1',
  unidadMedidaId: 'u1',
  tarifaIva: 'IVA_19',
  tarifaPorRevisar: true,
  precioVenta: 35500,
  costo: 26500,
  version: 3,
);

Future<void> _montar(WidgetTester tester, _RepoFicha repo, {bool puedeEditar = true}) async {
  tester.view.physicalSize = const Size(1280, 2000); // la ficha completa, sin scroll
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeInventarioProvider.overrideWithValue(repo),
      puedeEditarProductosProvider.overrideWithValue(puedeEditar),
      puedeGestionarFraccionesProvider.overrideWithValue(puedeEditar),
      puedeAjustarExistenciasProvider.overrideWithValue(puedeEditar),
      puedeDetallarProvider.overrideWithValue(puedeEditar),
    ],
    child: const MaterialApp(home: PantallaFichaDeProducto(productoId: 'p1')),
  ));
  await tester.pumpAndSettle();
}

void main() {
  test('HU-150 C1 · \$ 38.000 al 19 % con costo \$ 26.500: base 31.932,77, IVA 6.067,23, margen 20,50 %, beneficio 17,01 %', () {
    final c = CalculoDeFicha(precio: 38000, tarifa: 'IVA_19', costo: 26500);

    expect(c.base, 31932.77);
    expect(c.iva, 6067.23);
    expect(c.margenPct, 20.50);
    expect(c.beneficioPct, 17.01);
  });

  testWidgets('HU-150 C1 · al cambiar el precio la ficha recalcula base, IVA y margen, y guarda', (tester) async {
    final repo = _RepoFicha(_barniz);
    await _montar(tester, repo);

    await tester.enterText(find.byKey(const Key('ficha-precio')), '38000');
    await tester.pump();

    expect(find.text(formatearPesos(31932.77)), findsOneWidget);
    expect(find.text(formatearPesos(6067.23)), findsOneWidget);
    expect(find.textContaining('20,5'), findsOneWidget);

    await tester.tap(find.byKey(const Key('ficha-guardar')));
    await tester.pumpAndSettle();

    expect(repo.guardada!.precioVenta, 38000);
    expect(repo.guardada!.version, 3, reason: 'manda la versión que leyó');
    expect(find.text('Producto guardado'), findsOneWidget);
  });

  testWidgets('HU-150 C3 · la marca «tarifa por revisar» se ve y desaparece al guardar', (tester) async {
    final repo = _RepoFicha(_barniz);
    await _montar(tester, repo);
    expect(find.text('Tarifa por revisar'), findsOneWidget);

    await tester.tap(find.byKey(const Key('ficha-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('Tarifa por revisar'), findsNothing);
  });

  testWidgets('HU-150 C5 · si alguien guardó antes, se muestra el mensaje del backend', (tester) async {
    final repo = _RepoFicha(_barniz)
      ..errorAlGuardar = const ErroresDeValidacion({}, 'Alguien cambió este producto; recarga antes de guardar.');
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('Alguien cambió este producto; recarga antes de guardar.'), findsOneWidget);
  });

  testWidgets('HU-151 C1 · desactivar pide confirmación y avisa cuántas unidades hay y dónde', (tester) async {
    final repo = _RepoFicha(_barniz)..existencias = const [('Principal', 8)];
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-desactivar')));
    await tester.pumpAndSettle();

    expect(find.textContaining('No aparecerá en el POS ni en las búsquedas'), findsOneWidget);
    expect(find.textContaining('Tiene 8 unidades en Principal'), findsOneWidget);

    await tester.tap(find.widgetWithText(FilledButton, 'Desactivar'));
    await tester.pumpAndSettle();

    expect(repo.ficha.activo, isFalse);
    expect(find.text('Inactivo'), findsOneWidget);
    expect(find.byKey(const Key('ficha-reactivar')), findsOneWidget);
  });

  testWidgets('HU-151 C4 · un producto inactivo se reactiva', (tester) async {
    final repo = _RepoFicha(FichaDeProducto(
      id: 'p1',
      sku: 'X',
      nombre: 'Barniz galón',
      categoriaId: 'c1',
      unidadMedidaId: 'u1',
      tarifaIva: 'IVA_19',
      precioVenta: 35500,
      version: 1,
      activo: false,
    ));
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-reactivar')));
    await tester.pumpAndSettle();

    expect(repo.ficha.activo, isTrue);
    expect(find.byKey(const Key('ficha-desactivar')), findsOneWidget);
  });

  testWidgets('HU-152 C1 · la ficha muestra y guarda la ubicación de cada bodega', (tester) async {
    final repo = _RepoFicha(_barniz);
    await _montar(tester, repo);

    expect(find.text('Pasillo 3 – E2'), findsOneWidget);
    await tester.enterText(find.byKey(const Key('ubicacion-b2')), 'Estiba 5');
    await tester.tap(find.byKey(const Key('ficha-guardar')));
    await tester.pumpAndSettle();

    expect(repo.ubicacionesGuardadas, {'b2': 'Estiba 5'},
        reason: 'solo se manda la que cambió');
  });

  testWidgets('HU-157 C1/C4 · la ficha muestra cada fracción con su origen y deja poner un precio propio', (tester) async {
    final repo = _RepoFicha(_barniz)
      ..fracciones = const [FraccionDeTabla(medida: '¼ galón', factor: 0.25, precio: 18000)];
    await _montar(tester, repo);

    expect(find.text('¼ galón'), findsOneWidget);
    expect(find.text('de la tabla'), findsOneWidget);

    await tester.tap(find.byKey(const Key('precio-propio-¼ galón')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('precio-propio-valor')), '20000');
    await tester.tap(find.byKey(const Key('precio-propio-guardar')));
    await tester.pumpAndSettle();

    expect(repo.preciosPropios['¼ galón'], 20000);
    expect(find.text('propio'), findsOneWidget);
  });

  testWidgets('HU-157 C3 · quitar el precio propio vuelve al de la tabla', (tester) async {
    final repo = _RepoFicha(_barniz)
      ..fracciones = const [FraccionDeTabla(medida: '¼ galón', factor: 0.25, precio: 20000, origen: 'PROPIO')];
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('quitar-propio-¼ galón')));
    await tester.pumpAndSettle();

    expect(repo.preciosPropios.containsKey('¼ galón'), isTrue);
    expect(repo.preciosPropios['¼ galón'], isNull);
    expect(find.text('de la tabla'), findsOneWidget);
  });

  testWidgets('HU-159 C1 · se resta una cantidad de una bodega con su motivo', (tester) async {
    final repo = _RepoFicha(_barniz);
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-ajustar')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Restar'));
    await tester.enterText(find.byKey(const Key('ajuste-cantidad')), '5');
    await tester.tap(find.byKey(const Key('ajuste-tipo')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Avería').last);
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('ajuste-motivo')), 'Se cayó del estante');
    await tester.tap(find.byKey(const Key('ajuste-aplicar')));
    await tester.pumpAndSettle();

    final a = repo.ajuste!;
    expect(a.bodegaId, 'b1');
    expect(a.operacion, 'RESTAR');
    expect(a.cantidad, 5);
    expect(a.tipo, 'AVERIA');
    expect(a.motivo, 'Se cayó del estante');
    expect(find.text('Existencia ajustada'), findsOneWidget);
  });

  testWidgets('HU-159 C2 · si no alcanza la existencia, se ve el mensaje del backend y no se cierra', (tester) async {
    final repo = _RepoFicha(_barniz)
      ..errorAlAjustar = const ErroresDeValidacion(
          {}, 'Solo hay 3 unidades de Barniz galón en Principal. No puedes sacar 5.');
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-ajustar')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Restar'));
    await tester.enterText(find.byKey(const Key('ajuste-cantidad')), '5');
    await tester.enterText(find.byKey(const Key('ajuste-motivo')), 'x');
    await tester.tap(find.byKey(const Key('ajuste-aplicar')));
    await tester.pumpAndSettle();

    expect(find.text('Solo hay 3 unidades de Barniz galón en Principal. No puedes sacar 5.'), findsOneWidget);
    expect(find.byKey(const Key('ajuste-aplicar')), findsOneWidget);
  });

  testWidgets('HU-162 · un producto con equivalencia se detalla desde su ficha', (tester) async {
    final repo = _RepoFicha(_barniz)
      ..equivalenciasDelNegocio = const [
        EquivalenciaDeDetallado(
            id: 'e1', origenId: 'p1', origen: 'Barniz cuñete', destinoId: 'p2', destino: 'Barniz galón', factor: 5),
      ];
    await _montar(tester, repo);

    expect(find.text('1 Barniz cuñete = 5 Barniz galón'), findsOneWidget);
    await tester.tap(find.byKey(const Key('ficha-detallar')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('detallar-cantidad')), '1');
    await tester.tap(find.byKey(const Key('detallar-aplicar')));
    await tester.pumpAndSettle();

    expect(repo.detallado, ('p1', 'b1', 1));
    expect(find.text('Se abrieron 1 Barniz cuñete → 5 Barniz galón'), findsOneWidget);
  });

  testWidgets('HU-162 C3 · si no alcanza, se ve el mensaje del backend y no se cierra', (tester) async {
    final repo = _RepoFicha(_barniz)
      ..equivalenciasDelNegocio = const [
        EquivalenciaDeDetallado(
            id: 'e1', origenId: 'p1', origen: 'Barniz cuñete', destinoId: 'p2', destino: 'Barniz galón', factor: 5),
      ]
      ..errorAlDetallar = const ErroresDeValidacion({}, 'Solo hay 1 de Barniz cuñete. No puedes abrir 2.');
    await _montar(tester, repo);

    await tester.tap(find.byKey(const Key('ficha-detallar')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('detallar-cantidad')), '2');
    await tester.tap(find.byKey(const Key('detallar-aplicar')));
    await tester.pumpAndSettle();

    expect(find.text('Solo hay 1 de Barniz cuñete. No puedes abrir 2.'), findsOneWidget);
    expect(find.byKey(const Key('detallar-aplicar')), findsOneWidget);
  });

  testWidgets('HU-150 C4 · sin permiso de editar, la ficha se ve sin poder guardar', (tester) async {
    await _montar(tester, _RepoFicha(_barniz), puedeEditar: false);

    expect(find.byKey(const Key('ficha-guardar')), findsNothing);
    final precio = tester.widget<TextField>(
        find.descendant(of: find.byKey(const Key('ficha-precio')), matching: find.byType(TextField)));
    expect(precio.enabled, isFalse);
  });

  testWidgets('Ficha · los datos se piden todos a la vez, no uno detrás del otro', (tester) async {
    final repo = _RepoLento(_barniz);
    tester.view.physicalSize = const Size(1280, 2000);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.reset);
    await tester.pumpWidget(ProviderScope(
      overrides: [
        repositorioDeInventarioProvider.overrideWithValue(repo),
        puedeEditarProductosProvider.overrideWithValue(true),
        puedeDetallarProvider.overrideWithValue(true),
      ],
      child: const MaterialApp(home: PantallaFichaDeProducto(productoId: 'p1')),
    ));
    await tester.pump();

    expect(repo.pedidos,
        containsAll(['ficha', 'categorias', 'unidades', 'ubicaciones', 'fracciones', 'equivalencias']));

    repo.soltar.complete();
    await tester.pumpAndSettle();
    expect(find.text('Barniz galón'), findsWidgets);
  });
}
