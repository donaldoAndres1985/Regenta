import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

/// Repositorio en memoria: se comporta como el backend de HU-166.
class _RepoFake implements RepositorioDeProveedores {
  _RepoFake(List<ProveedorDelNegocio> iniciales) : proveedores = [...iniciales];

  final List<ProveedorDelNegocio> proveedores;
  final Map<String, List<ProductoDelProveedor>> catalogos = {};
  final List<ProductoDelProveedor> asociados = [];
  List<ProductoEncontrado> encontrados = const [];

  @override
  Future<List<ProveedorDelNegocio>> listar() async => [...proveedores];

  @override
  Future<ProveedorDelNegocio> guardar(ProveedorDelNegocio p) async {
    if (p.id == null && proveedores.any((x) => x.numeroDocumento == p.numeroDocumento.trim())) {
      throw const RecursoDuplicado('Ya existe un proveedor con ese documento.');
    }
    final guardado = ProveedorDelNegocio(
      id: p.id ?? 'nuevo-${proveedores.length}',
      tipoDocumento: p.tipoDocumento,
      numeroDocumento: p.numeroDocumento.trim(),
      razonSocial: p.razonSocial.trim(),
      diasCredito: p.diasCredito,
      cupoCredito: p.cupoCredito,
    );
    proveedores.removeWhere((x) => x.id == guardado.id);
    proveedores.add(guardado);
    return guardado;
  }

  ProveedorDelNegocio _cambiarActivo(String id, bool activo) {
    final i = proveedores.indexWhere((p) => p.id == id);
    final p = proveedores[i];
    final nuevo = ProveedorDelNegocio(
      id: p.id,
      numeroDocumento: p.numeroDocumento,
      razonSocial: p.razonSocial,
      diasCredito: p.diasCredito,
      cupoCredito: p.cupoCredito,
      deuda: p.deuda,
      activo: activo,
    );
    proveedores[i] = nuevo;
    return nuevo;
  }

  @override
  Future<ProveedorDelNegocio> desactivar(String id) async => _cambiarActivo(id, false);

  @override
  Future<ProveedorDelNegocio> reactivar(String id) async => _cambiarActivo(id, true);

  @override
  Future<List<ProductoDelProveedor>> productos(String proveedorId) async =>
      [...?catalogos[proveedorId]];

  @override
  Future<ProductoDelProveedor> asociar(String proveedorId, ProductoDelProveedor producto) async {
    asociados.add(producto);
    final lista = catalogos.putIfAbsent(proveedorId, () => []);
    lista.removeWhere((p) => p.productoId == producto.productoId);
    lista.add(producto);
    return producto;
  }

  @override
  Future<void> quitar(String proveedorId, String productoId) async {
    catalogos[proveedorId]?.removeWhere((p) => p.productoId == productoId);
  }

  @override
  Future<List<ProductoEncontrado>> buscarProductos(String termino) async => encontrados;
}

const _andina = ProveedorDelNegocio(
  id: 'p-1',
  numeroDocumento: '900123456',
  razonSocial: 'Distribuidora Andina SAS',
  diasCredito: 30,
  cupoCredito: 1000000,
  deuda: 750000,
);

const _pinturas = ProveedorDelNegocio(
  id: 'p-2',
  numeroDocumento: '800555111',
  razonSocial: 'Pinturas del Valle',
  cupoCredito: 500000,
  deuda: 650000,
  superaCupo: true,
);

Future<_RepoFake> _montar(WidgetTester tester,
    {List<ProveedorDelNegocio>? proveedores, Size tamano = const Size(1440, 900)}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = _RepoFake(proveedores ?? const [_andina, _pinturas]);
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeProveedoresProvider.overrideWithValue(repo)],
    child: const MaterialApp(home: PantallaProveedores()),
  ));
  await tester.pumpAndSettle();
  return repo;
}

void main() {
  testWidgets('HU-166 C1 · El listado muestra documento, nombre, días de crédito, deuda y cupo', (tester) async {
    await _montar(tester);

    expect(find.text('DISTRIBUIDORA ANDINA SAS'), findsOneWidget);
    expect(find.text('NIT 900123456 · 30 DÍAS'), findsOneWidget);
    expect(find.text('\$750.000'), findsOneWidget);
    expect(find.text('cupo \$1.000.000'), findsOneWidget);
    expect(find.text('NIT 800555111 · CONTADO'), findsOneWidget);
  });

  testWidgets('HU-166 C1 · Busca por nombre o por documento', (tester) async {
    await _montar(tester);

    await tester.enterText(find.byKey(const Key('proveedores-buscar')), 'pintur');
    await tester.pump();
    expect(find.text('PINTURAS DEL VALLE'), findsOneWidget);
    expect(find.text('DISTRIBUIDORA ANDINA SAS'), findsNothing);

    await tester.enterText(find.byKey(const Key('proveedores-buscar')), '900123');
    await tester.pump();
    expect(find.text('DISTRIBUIDORA ANDINA SAS'), findsOneWidget);
    expect(find.text('PINTURAS DEL VALLE'), findsNothing);
  });

  testWidgets('HU-166 C5 / I6 · La deuda por encima del cupo se marca, sin bloquear nada', (tester) async {
    await _montar(tester);

    expect(find.text('SUPERA CUPO'), findsOneWidget);
    expect(find.text('debes \$1.400.000'), findsOneWidget);
  });

  testWidgets('HU-166 C2 · Un NIT repetido muestra «Ya existe un proveedor con ese documento.»', (tester) async {
    await _montar(tester);

    await tester.tap(find.byKey(const Key('proveedores-nuevo')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('proveedor-numero')), '900123456');
    await tester.enterText(find.byKey(const Key('proveedor-razon-social')), 'Otra Andina');
    await tester.tap(find.byKey(const Key('proveedor-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('Ya existe un proveedor con ese documento.'), findsOneWidget);
    expect(find.byType(FormularioDeProveedor), findsOneWidget, reason: 'la ficha sigue abierta para corregir');
  });

  testWidgets('HU-166 C3 · Desactivar pide confirmación; el inactivo sale de Activos y se reactiva', (tester) async {
    await _montar(tester);

    await tester.tap(find.byKey(const Key('proveedor-fila-p-1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('proveedor-desactivar')));
    await tester.pumpAndSettle();
    expect(find.text('¿Desactivar a Distribuidora Andina SAS?'), findsOneWidget);
    await tester.tap(find.byKey(const Key('proveedor-confirmar-desactivar')));
    await tester.pumpAndSettle();

    expect(find.text('DISTRIBUIDORA ANDINA SAS'), findsNothing, reason: 'el filtro por omisión es Activos');

    await tester.tap(find.byKey(const Key('proveedores-filtro-inactivos')));
    await tester.pumpAndSettle();
    expect(find.text('DISTRIBUIDORA ANDINA SAS'), findsOneWidget);
    expect(find.text('INACTIVO'), findsOneWidget);

    await tester.tap(find.byKey(const Key('proveedor-fila-p-1')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('proveedor-aviso-inactivo')), findsOneWidget);
    expect(find.byKey(const Key('proveedor-guardar')), findsNothing);
    await tester.tap(find.byKey(const Key('proveedor-reactivar')));
    await tester.pumpAndSettle();

    await tester.tap(find.byKey(const Key('proveedores-filtro-activos')));
    await tester.pumpAndSettle();
    expect(find.text('DISTRIBUIDORA ANDINA SAS'), findsOneWidget);
  });

  testWidgets('HU-166 C3 · Cancelar la confirmación no desactiva nada', (tester) async {
    final repo = await _montar(tester);

    await tester.tap(find.byKey(const Key('proveedor-fila-p-1')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('proveedor-desactivar')));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cancelar').last);
    await tester.pumpAndSettle();

    expect(repo.proveedores.firstWhere((p) => p.id == 'p-1').activo, isTrue);
  });

  testWidgets('HU-166 C4 · En el catálogo, la estrella marca al proveedor como preferido del producto',
      (tester) async {
    final repo = await _montar(tester);
    repo.catalogos['p-1'] = [
      const ProductoDelProveedor(productoId: 'prod-1', sku: 'ACE-500', nombre: 'Acetaminofén 500 mg', costoUltimo: 9800),
    ];

    await tester.tap(find.byKey(const Key('proveedor-catalogo-p-1')));
    await tester.pumpAndSettle();
    expect(find.text('Acetaminofén 500 mg'), findsOneWidget);
    expect(find.byIcon(Icons.star_border), findsOneWidget);

    await tester.tap(find.byKey(const Key('catalogo-preferido-prod-1')));
    await tester.pumpAndSettle();

    expect(repo.asociados.single.preferido, isTrue);
    expect(find.byIcon(Icons.star), findsOneWidget);
  });

  testWidgets('HU-166 · Agregar un producto al catálogo con su costo', (tester) async {
    final repo = await _montar(tester);
    repo.encontrados = const [ProductoEncontrado(id: 'prod-9', nombre: 'Thinner galón', sku: 'THI-1')];

    await tester.tap(find.byKey(const Key('proveedor-catalogo-p-1')));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('catalogo-buscar')), 'thinner');
    await tester.testTextInput.receiveAction(TextInputAction.done);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Thinner galón'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('condiciones-costo')), '18500');
    await tester.tap(find.byKey(const Key('condiciones-preferido')));
    await tester.tap(find.byKey(const Key('condiciones-agregar')));
    await tester.pumpAndSettle();

    final agregado = repo.asociados.single;
    expect(agregado.productoId, 'prod-9');
    expect(agregado.costoUltimo, 18500);
    expect(agregado.preferido, isTrue);
    expect(find.byKey(const Key('catalogo-lista')), findsOneWidget);
  });

  testWidgets('HU-166 · Sin proveedores, invita a registrar el primero', (tester) async {
    await _montar(tester, proveedores: const []);

    expect(find.text('Todavía no hay proveedores registrados.'), findsOneWidget);
    expect(find.text('Registrar primer proveedor'), findsOneWidget);
  });

  testWidgets('HU-166 · En el celular, el listado y la ficha caben sin desbordarse', (tester) async {
    await _montar(tester, tamano: const Size(390, 844));
    expect(find.byKey(const Key('proveedores-movil')), findsOneWidget);

    await tester.tap(find.byKey(const Key('proveedor-fila-p-1')));
    await tester.pumpAndSettle();
    expect(find.byKey(const Key('proveedor-desactivar')), findsOneWidget);
    expect(find.byKey(const Key('proveedor-guardar')), findsOneWidget);
    expect(tester.takeException(), isNull);
  });
}
