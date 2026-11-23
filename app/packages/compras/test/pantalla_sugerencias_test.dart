import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

class _RepoFake implements RepositorioDeSugerencias {
  _RepoFake(this.grupos);

  List<GrupoDeSugerenciasVista> grupos;
  final List<GrupoDeSugerenciasVista> aceptados = [];
  final List<(String, String)> asignados = [];
  final List<String> descartados = [];
  ErrorDeApi? falla;
  Map<String, String> nombresDeBodega = const {'b-1': 'Principal'};

  @override
  Future<List<GrupoDeSugerenciasVista>> listar() async => grupos;

  @override
  Future<Map<String, String>> bodegas() async => nombresDeBodega;

  @override
  Future<List<ProveedorParaAsignar>> proveedores() async => const [
        ProveedorParaAsignar(id: 'p-1', nombre: 'Distribuidora Andina'),
        ProveedorParaAsignar(id: 'p-2', nombre: 'Pinturas del Valle'),
      ];

  @override
  Future<OrdenDesdeSugerencias> aceptar(GrupoDeSugerenciasVista grupo) async {
    if (falla != null) throw falla!;
    aceptados.add(grupo);
    grupos = [for (final g in grupos) if (g.clave != grupo.clave) g];
    return const OrdenDesdeSugerencias(id: 'o-9', numero: 'OC-9');
  }

  @override
  Future<void> asignarProveedor(String sugerenciaId, String proveedorId) async {
    if (falla != null) throw falla!;
    asignados.add((sugerenciaId, proveedorId));
    final huerfana = grupos.firstWhere((g) => g.sinProveedor).sugerencias.firstWhere((s) => s.id == sugerenciaId);
    grupos = [
      GrupoDeSugerenciasVista(
        proveedorId: proveedorId,
        proveedorNombre: 'Distribuidora Andina',
        bodegaId: 'b-1',
        costoTotalEstimado: 43200,
        sugerencias: [
          SugerenciaVista(
            id: huerfana.id,
            productoId: huerfana.productoId,
            nombre: huerfana.nombre,
            bodegaId: 'b-1',
            existencia: huerfana.existencia,
            stockMinimo: huerfana.stockMinimo,
            stockObjetivo: huerfana.stockObjetivo,
            cantidadSugerida: 48,
            proveedorId: proveedorId,
            costoEstimado: 900,
          ),
        ],
      ),
      for (final g in grupos) if (!g.sinProveedor) g,
    ];
  }

  @override
  Future<void> descartar(String sugerenciaId) async {
    descartados.add(sugerenciaId);
    grupos = [
      for (final g in grupos)
        GrupoDeSugerenciasVista(
          proveedorId: g.proveedorId,
          proveedorNombre: g.proveedorNombre,
          bodegaId: g.bodegaId,
          sinProveedor: g.sinProveedor,
          costoTotalEstimado: g.costoTotalEstimado,
          sugerencias: [for (final s in g.sugerencias) if (s.id != sugerenciaId) s],
        ),
    ].where((g) => g.sugerencias.isNotEmpty).toList();
  }
}

const _andina = GrupoDeSugerenciasVista(
  proveedorId: 'p-1',
  proveedorNombre: 'Distribuidora Andina',
  bodegaId: 'b-1',
  costoTotalEstimado: 48000,
  sugerencias: [
    SugerenciaVista(
      id: 's-1',
      productoId: 'pr-1',
      nombre: 'Acetaminofén 500 mg',
      bodegaId: 'b-1',
      existencia: 8,
      stockMinimo: 10,
      stockObjetivo: 50,
      cantidadSugerida: 48,
      proveedorId: 'p-1',
      costoEstimado: 1000,
    ),
  ],
);

const _sinProveedor = GrupoDeSugerenciasVista(
  sinProveedor: true,
  sugerencias: [
    SugerenciaVista(
      id: 's-2',
      productoId: 'pr-2',
      nombre: 'Guantes de nitrilo',
      bodegaId: 'b-1',
      existencia: 2,
      stockMinimo: 10,
      stockObjetivo: 50,
      cantidadSugerida: 48,
      sinProveedor: true,
    ),
  ],
);

Future<_RepoFake> _montar(WidgetTester tester,
    {Size tamano = const Size(1440, 900), List<GrupoDeSugerenciasVista>? grupos, List<String>? abiertas}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = _RepoFake(grupos ?? [_andina, _sinProveedor]);
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeSugerenciasProvider.overrideWithValue(repo)],
    child: MaterialApp(home: PantallaSugerencias(onOrdenCreada: abiertas?.add)),
  ));
  await tester.pumpAndSettle();
  return repo;
}

void main() {
  testWidgets('HU-181 C1 / C2 · Cada producto muestra su existencia, mínimo, objetivo y cuánto pedir', (tester) async {
    await _montar(tester);

    expect(find.text('Distribuidora Andina'), findsOneWidget);
    expect(find.text('Acetaminofén 500 mg'), findsOneWidget);
    expect(find.text('Hay 8 · mínimo 10 · objetivo 50'), findsOneWidget);
    expect(find.text('48 und'), findsNWidgets(2));
    expect(find.text(formatearPesos(48000)), findsOneWidget);
  });

  testWidgets('HU-181 C4 · «Crear orden» vuelve el grupo una orden en borrador y la abre', (tester) async {
    final abiertas = <String>[];
    final repo = await _montar(tester, abiertas: abiertas);

    await tester.tap(find.byKey(const Key('sugerencias-crear-orden-p-1-b-1')));
    await tester.pumpAndSettle();

    expect(repo.aceptados.single.proveedorId, 'p-1');
    expect(repo.aceptados.single.bodegaId, 'b-1');
    expect(find.text('Orden OC-9 creada en borrador para Distribuidora Andina.'), findsOneWidget);
    expect(find.byKey(const Key('sugerencias-grupo-p-1-b-1')), findsNothing);
    expect(abiertas, ['o-9']);
  });

  testWidgets('HU-181 C4 · Si el servidor la rechaza, se muestra el motivo y el grupo sigue ahí', (tester) async {
    final repo = await _montar(tester);
    repo.falla = const ErroresDeValidacion({}, 'Indica la bodega destino de la orden');

    await tester.tap(find.byKey(const Key('sugerencias-crear-orden-p-1-b-1')));
    await tester.pumpAndSettle();

    expect(find.text('Indica la bodega destino de la orden'), findsOneWidget);
    expect(find.byKey(const Key('sugerencias-grupo-p-1-b-1')), findsOneWidget);
  });

  testWidgets('HU-181 C5 · Sin proveedor va al final, sin «Crear orden» y con «Asignar proveedor»', (tester) async {
    await _montar(tester);

    expect(find.text('Sin proveedor'), findsOneWidget);
    expect(find.text('Asígnales un proveedor para poder pedirlos.'), findsOneWidget);
    expect(find.byKey(const Key('sugerencias-crear-orden-sin-proveedor')), findsNothing);
    expect(find.byKey(const Key('sugerencia-asignar-s-2')), findsOneWidget);
    expect(find.byKey(const Key('sugerencia-asignar-s-1')), findsNothing);
    final andina = tester.getTopLeft(find.byKey(const Key('sugerencias-grupo-p-1-b-1'))).dy;
    final sin = tester.getTopLeft(find.byKey(const Key('sugerencias-grupo-sin-proveedor'))).dy;
    expect(sin, greaterThan(andina));
  });

  testWidgets('HU-181 C5 · Asignarle un proveedor lo pasa a ese grupo', (tester) async {
    final repo = await _montar(tester, grupos: [_sinProveedor]);

    await tester.tap(find.byKey(const Key('sugerencia-asignar-s-2')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('asignar-proveedor')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Distribuidora Andina').last);
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('asignar-guardar')));
    await tester.pumpAndSettle();

    expect(repo.asignados.single, ('s-2', 'p-1'));
    expect(find.text('Sin proveedor'), findsNothing);
    expect(find.byKey(const Key('sugerencias-crear-orden-p-1-b-1')), findsOneWidget);
    expect(find.text('Guantes de nitrilo queda con Distribuidora Andina como proveedor preferido.'), findsOneWidget);
  });

  testWidgets('HU-181 · «Asignar» no se puede tocar sin elegir proveedor', (tester) async {
    final repo = await _montar(tester, grupos: [_sinProveedor]);

    await tester.tap(find.byKey(const Key('sugerencia-asignar-s-2')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('asignar-guardar')));
    await tester.pumpAndSettle();

    expect(repo.asignados, isEmpty);
    expect(find.byKey(const Key('asignar-proveedor')), findsOneWidget);
  });

  testWidgets('HU-181 · Descartar saca el producto de la lista', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('sugerencia-descartar-s-1')));
    await tester.pumpAndSettle();

    expect(repo.descartados, ['s-1']);
    expect(find.text('Acetaminofén 500 mg'), findsNothing);
  });

  testWidgets('HU-181 · Con dos bodegas, cada grupo dice de cuál es', (tester) async {
    final repo = _RepoFake(const []);
    repo.nombresDeBodega = {'b-1': 'Principal', 'b-2': 'Sede norte'};
    repo.grupos = [
      _andina,
      const GrupoDeSugerenciasVista(
        proveedorId: 'p-1',
        proveedorNombre: 'Distribuidora Andina',
        bodegaId: 'b-2',
        sugerencias: [
          SugerenciaVista(id: 's-3', productoId: 'pr-1', nombre: 'Acetaminofén 500 mg', bodegaId: 'b-2',
              cantidadSugerida: 12, proveedorId: 'p-1'),
        ],
      ),
    ];
    await tester.binding.setSurfaceSize(const Size(1440, 900));
    addTearDown(() => tester.binding.setSurfaceSize(null));
    await tester.pumpWidget(ProviderScope(
      overrides: [repositorioDeSugerenciasProvider.overrideWithValue(repo)],
      child: const MaterialApp(home: PantallaSugerencias()),
    ));
    await tester.pumpAndSettle();

    expect(find.text('1 producto · Principal'), findsOneWidget);
    expect(find.text('1 producto · Sede norte'), findsOneWidget);
  });

  testWidgets('HU-181 · Sin nada por reponer lo dice', (tester) async {
    await _montar(tester, grupos: const []);

    expect(find.byKey(const Key('sugerencias-vacio')), findsOneWidget);
  });

  testWidgets('HU-181 · En el celular cabe sin desbordarse', (tester) async {
    await _montar(tester, tamano: const Size(390, 844));

    expect(find.byKey(const Key('sugerencias-lista')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
