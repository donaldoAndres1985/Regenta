import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// La pantalla de Inventario en una sola ventana (design/comportamiento/Inventario.md R1-R12).
/// Absorbe el inventario general (HU-153/154): la lista sale de `GET /api/inventario/general`.
class _Repo implements RepositorioDeInventario {
  _Repo({int productos = 3}) : _todas = [for (var i = 0; i < productos; i++) _fila(i)];

  final List<FilaDeInventario> _todas;
  final List<ConsultaDeInventario> consultas = [];
  ErrorDeApi? error;
  bool sinFilas = false;
  num? valorCosto = 79500;

  @override
  Future<ResultadoDeInventario> inventarioGeneral(ConsultaDeInventario consulta) async {
    consultas.add(consulta);
    if (error != null) throw error!;
    final filas = sinFilas ? <FilaDeInventario>[] : _todas;
    final desde = (consulta.pagina * consulta.tamano).clamp(0, filas.length);
    final hasta = (desde + consulta.tamano).clamp(0, filas.length);
    return ResultadoDeInventario(
      filas: filas.sublist(desde, hasta),
      totalFilas: filas.length,
      valorCosto: valorCosto,
      valorPrecio: 106500,
      bajoMinimo: 1,
    );
  }

  @override
  Future<List<OpcionDeFicha>> categorias() async =>
      const [OpcionDeFicha(id: 'c1', nombre: 'Pinturas'), OpcionDeFicha(id: 'c2', nombre: 'Herramientas')];

  @override
  Future<List<OpcionDeFicha>> unidades() async => const [OpcionDeFicha(id: 'u1', nombre: 'UND · Unidad')];

  @override
  Future<List<(String bodega, num cantidad)>> existenciasDe(String id) async =>
      const [('Principal', 2), ('Bodega 2', 1)];

  (List<String>, String?, String)? tarifaEnBloque;

  @override
  Future<int> cambiarTarifaEnBloque(
      {List<String> productoIds = const [], String? categoriaId, required String tarifaIva}) async {
    tarifaEnBloque = (productoIds, categoriaId, tarifaIva);
    return productoIds.length;
  }

  CodigoResuelto? codigo;
  final List<String> codigosResueltos = [];

  @override
  Future<CodigoResuelto> resolverCodigo(String codigo) async {
    codigosResueltos.add(codigo);
    if (this.codigo == null) throw const ErrorDesconocido(404, 'Ningun producto tiene ese codigo');
    return this.codigo!;
  }

  NuevoProducto? creado;
  ErrorDeApi? errorAlCrear;

  @override
  Future<String> crearProducto(NuevoProducto nuevo) async {
    if (errorAlCrear != null) throw errorAlCrear!;
    creado = nuevo;
    return 'nuevo-1';
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

FilaDeInventario _fila(int i) => FilaDeInventario.desdeJson({
      'id': 'p$i',
      'sku': 'SKU-$i',
      'nombre': i == 0 ? 'Barniz galón' : 'Producto $i',
      'categoria': 'Pinturas',
      'tarifaIva': 'IVA_19',
      'tarifaPorRevisar': i == 0,
      'existencia': i == 1 ? 0 : 3,
      'costo': 26500,
      'precio': 35500,
      'valorCosto': 79500,
      'valorPrecio': 106500,
      'activo': true,
      'stockMinimo': 5,
      'nivel': i == 1 ? 'CERO' : (i == 0 ? 'BAJO' : 'NORMAL'),
    });

class _Escaner implements EscanerDeCodigos {
  _Escaner({this.hayCamara = true, this.codigo});
  final bool hayCamara;
  final String? codigo;

  @override
  Future<bool> get disponible async => hayCamara;

  @override
  Future<String?> escanearUnCodigo(BuildContext context) async {
    if (!hayCamara) throw const EscaneoNoDisponible();
    return codigo;
  }
}

Future<_Repo> _montar(
  WidgetTester tester, {
  _Repo? repo,
  _Escaner? escaner,
  void Function(String, num)? onSel,
  Size tamano = const Size(1440, 900),
  bool puedeImportar = true,
}) async {
  tester.view.physicalSize = tamano;
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  final r = repo ?? _Repo();
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeInventarioProvider.overrideWithValue(r),
      escanerProvider.overrideWithValue(escaner ?? _Escaner(hayCamara: false)),
      puedeEditarProductosProvider.overrideWithValue(true),
      puedeImportarProvider.overrideWithValue(puedeImportar),
    ],
    child: MaterialApp(home: PantallaInventario(onProductoSeleccionado: onSel ?? (_, _) {})),
  ));
  await tester.pumpAndSettle();
  return r;
}

Future<void> _escribir(WidgetTester tester, String texto) async {
  await tester.enterText(find.byKey(const Key('inventario-buscar')), texto);
  await tester.pump(const Duration(milliseconds: 400));
  await tester.pumpAndSettle();
}

void main() {
  group('HU-035 · búsqueda y escaneo', () {
    testWidgets('Criterio 1: con tres caracteres se busca; con menos, no', (tester) async {
      final repo = await _montar(tester);
      expect(repo.consultas.single.q, isNull, reason: 'al abrir lista todo');

      await _escribir(tester, 'ce');
      expect(repo.consultas.last.q, isNull, reason: 'con 2 caracteres no se filtra');

      await _escribir(tester, 'cem');
      expect(repo.consultas.last.q, 'cem');
      expect(repo.consultas.last.pagina, 0);
    });

    testWidgets('Criterio 2 y 4: al escanear se abre el producto con su factor', (tester) async {
      String? id;
      num? factor;
      final repo = _Repo()
        ..codigo = const CodigoResuelto(
            productoId: 'p9', sku: 'CEM-050', nombre: 'Cemento', factor: 12, esAlterno: true);
      await _montar(tester,
          repo: repo,
          escaner: _Escaner(codigo: 'CAJACEM12'),
          onSel: (i, f) {
            id = i;
            factor = f;
          });

      await tester.tap(find.bySemanticsLabel('Escanear código'));
      await tester.pumpAndSettle();

      expect(repo.codigosResueltos, ['CAJACEM12']);
      expect((id, factor), ('p9', 12));
    });

    testWidgets('Criterio 3: sin cámara (web) el botón lleva a la captura manual', (tester) async {
      String? id;
      final repo = _Repo()
        ..codigo = const CodigoResuelto(productoId: 'p3', sku: 'X', nombre: 'X', factor: 1, esAlterno: false);
      await _montar(tester, repo: repo, onSel: (i, _) => id = i);

      await tester.tap(find.bySemanticsLabel('Escanear código'));
      await tester.pumpAndSettle();
      expect(find.text('Ingresar código'), findsOneWidget);
      await tester.enterText(find.byType(TextField).last, 'MANUAL-1');
      await tester.tap(find.text('Buscar'));
      await tester.pumpAndSettle();

      expect(repo.codigosResueltos, ['MANUAL-1']);
      expect(id, 'p3');
    });

    testWidgets('Estado vacío y estado de error se ven distintos', (tester) async {
      final repo = _Repo()..sinFilas = true;
      await _montar(tester, repo: repo);
      expect(find.text('No hay productos todavía.'), findsOneWidget);

      repo.error = const ErrorDeRed();
      await _escribir(tester, 'abcd');
      expect(find.text('Sin conexión. Revisa tu red e intenta de nuevo.'), findsOneWidget);
      expect(find.text('Reintentar'), findsOneWidget);
    });
  });

  group('R5 · la lista se carga por partes', () {
    testWidgets('pide 50 al abrir y la página siguiente al llegar al final', (tester) async {
      final repo = await _montar(tester, repo: _Repo(productos: 120));
      expect(repo.consultas.single.pagina, 0);
      expect(repo.consultas.single.tamano, 50);
      expect(find.byKey(const Key('fila-p0')), findsOneWidget);

      await tester.scrollUntilVisible(find.byKey(const Key('fila-p70')), 400,
          scrollable: find.descendant(
              of: find.byKey(const Key('inventario-lista')), matching: find.byType(Scrollable)));
      await tester.pumpAndSettle();

      expect(repo.consultas.map((c) => c.pagina), containsAllInOrder([0, 1]));
      expect(find.byKey(const Key('fila-p70')), findsOneWidget);
    });
  });

  group('R6/R7 · web: tabla y panel', () {
    testWidgets('HU-153 C1 · cada fila con SKU, nombre, stock, mínimo, precio y estado; el valorizado al costo', (tester) async {
      await _montar(tester);

      expect(find.text('SKU-0'), findsOneWidget);
      expect(find.text('Barniz galón'), findsOneWidget);
      expect(find.text('BAJO MÍNIMO'), findsOneWidget);
      expect(find.text('AGOTADO'), findsOneWidget);
      expect(find.text('EN STOCK'), findsOneWidget);
      expect(find.text(formatearPesos(35500)), findsWidgets);
      expect(find.descendant(of: find.byKey(const Key('panel-valorizado')), matching: find.text(formatearPesos(79500))),
          findsOneWidget);
      expect(find.text('al costo promedio'), findsOneWidget);
    });

    testWidgets('R11 · sin permiso de costo, el valorizado es a precio de venta', (tester) async {
      await _montar(tester, repo: _Repo()..valorCosto = null);

      expect(find.descendant(of: find.byKey(const Key('panel-valorizado')), matching: find.text(formatearPesos(106500))),
          findsOneWidget);
      expect(find.text('a precio de venta'), findsOneWidget);
    });

    testWidgets('R9 · elegir una fila muestra su existencia por bodega en el panel', (tester) async {
      await _montar(tester);

      await tester.tap(find.text('Barniz galón'));
      await tester.pumpAndSettle();

      final panel = find.byKey(const Key('panel-bodegas'));
      expect(find.descendant(of: panel, matching: find.text('Principal')), findsOneWidget);
      expect(find.descendant(of: panel, matching: find.text('Bodega 2')), findsOneWidget);
    });
  });

  group('R8 · filtros', () {
    testWidgets('HU-153 C2 · tarifa por revisar, existencia negativa e inactivos se mandan al backend', (tester) async {
      final repo = await _montar(tester);

      Future<void> elegir(String clave) async {
        await tester.tap(find.byKey(const Key('inventario-filtros')));
        await tester.pumpAndSettle();
        await tester.tap(find.byKey(Key(clave)));
        await tester.pumpAndSettle();
      }

      await elegir('filtro-por-revisar');
      expect(repo.consultas.last.tarifaPorRevisar, isTrue);
      await elegir('filtro-negativa');
      expect(repo.consultas.last.existenciaNegativa, isTrue);
      await elegir('filtro-inactivos');
      expect(repo.consultas.last.estado, 'INACTIVOS');
    });

    testWidgets('los chips filtran por categoría y por bajo mínimo, con su conteo', (tester) async {
      final repo = await _montar(tester);

      await tester.tap(find.text('PINTURAS'));
      await tester.pumpAndSettle();
      expect(repo.consultas.last.categoriaId, 'c1');

      expect(find.text('BAJO MÍNIMO · 1'), findsOneWidget);
      await tester.tap(find.text('BAJO MÍNIMO · 1'));
      await tester.pumpAndSettle();
      expect(repo.consultas.last.soloBajoMinimo, isTrue);
    });

    testWidgets('HU-153 C3 · varias palabras se mandan como búsqueda', (tester) async {
      final repo = await _montar(tester);
      await _escribir(tester, 'barniz trans');
      expect(repo.consultas.last.q, 'barniz trans');
    });
  });

  group('HU-154 · tarifa en bloque desde la misma ventana', () {
    testWidgets('HU-154 C1 · cambiar la tarifa de los seleccionados pide confirmar y no cambia el precio', (tester) async {
      final repo = await _montar(tester);

      await tester.tap(find.byKey(const Key('seleccionar-p0')));
      await tester.pump();
      await tester.tap(find.byKey(const Key('cambiar-tarifa')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('IVA 5 %').last);
      await tester.pumpAndSettle();

      expect(find.text('¿Cambiar la tarifa de 1 productos a IVA 5 %? El precio no cambia.'), findsOneWidget);
      await tester.tap(find.widgetWithText(FilledButton, 'Cambiar'));
      await tester.pumpAndSettle();
      expect(repo.tarifaEnBloque!.$1, ['p0']);
      expect(repo.tarifaEnBloque!.$2, isNull);
      expect(repo.tarifaEnBloque!.$3, 'IVA_5');
    });

    testWidgets('HU-154 C1 · sin selección, con una categoría elegida, cambia la categoría completa', (tester) async {
      final repo = await _montar(tester);

      await tester.tap(find.text('PINTURAS'));
      await tester.pumpAndSettle();
      await tester.tap(find.byKey(const Key('cambiar-tarifa')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Exento (0 %)').last);
      await tester.pumpAndSettle();
      expect(find.textContaining('toda la categoría Pinturas'), findsOneWidget);
      await tester.tap(find.widgetWithText(FilledButton, 'Cambiar'));
      await tester.pumpAndSettle();

      expect(repo.tarifaEnBloque!.$1, isEmpty);
      expect(repo.tarifaEnBloque!.$2, 'c1');
      expect(repo.tarifaEnBloque!.$3, 'EXENTO');
    });
  });

  group('R10 · las demás opciones en un solo menú', () {
    testWidgets('Más opciones trae unidades, fracciones, equivalencias e importar', (tester) async {
      await _montar(tester);

      await tester.tap(find.byKey(const Key('inventario-mas')));
      await tester.pumpAndSettle();

      expect(find.text('Unidades de medida'), findsOneWidget);
      expect(find.text('Tablas de fracciones'), findsOneWidget);
      expect(find.text('Equivalencias de detallado'), findsOneWidget);
      expect(find.text('Importar desde Excel'), findsOneWidget);
    });

    testWidgets('sin permiso de importar, Importar no aparece', (tester) async {
      await _montar(tester, puedeImportar: false);
      await tester.tap(find.byKey(const Key('inventario-mas')));
      await tester.pumpAndSettle();
      expect(find.text('Importar desde Excel'), findsNothing);
    });
  });

  group('R12 · nuevo producto', () {
    Future<void> llenar(WidgetTester tester) async {
      await tester.tap(find.byKey(const Key('inventario-nuevo')));
      await tester.pumpAndSettle();
      await tester.enterText(find.byKey(const Key('nuevo-sku')), 'BRO-12');
      await tester.enterText(find.byKey(const Key('nuevo-nombre')), 'Broca 12 mm');
      await tester.enterText(find.byKey(const Key('nuevo-precio')), '15000');
      await tester.enterText(find.byKey(const Key('nuevo-minimo')), '5');
      await tester.tap(find.byKey(const Key('nuevo-categoria')));
      await tester.pumpAndSettle();
      await tester.tap(find.text('Herramientas').last);
      await tester.pumpAndSettle();
    }

    testWidgets('se crea con categoría, unidad, tarifa, precio y mínimo', (tester) async {
      final repo = await _montar(tester);
      await llenar(tester);
      await tester.tap(find.widgetWithText(FilledButton, 'Crear'));
      await tester.pump();

      final c = repo.creado!;
      expect((c.sku, c.nombre, c.categoriaId, c.unidadMedidaId, c.tarifaIva, c.precioVenta, c.stockMinimo),
          ('BRO-12', 'Broca 12 mm', 'c2', 'u1', 'IVA_19', 15000, 5));
    });

    testWidgets('si el backend lo rechaza, el mensaje se ve y el diálogo no se cierra', (tester) async {
      final repo = _Repo()..errorAlCrear = const ErroresDeValidacion({}, 'Ese SKU ya lo usa otro producto.');
      await _montar(tester, repo: repo);
      await llenar(tester);
      await tester.tap(find.widgetWithText(FilledButton, 'Crear'));
      await tester.pumpAndSettle();

      expect(find.text('Ese SKU ya lo usa otro producto.'), findsOneWidget);
      expect(find.byKey(const Key('nuevo-sku')), findsOneWidget);
    });
  });

  group('móvil', () {
    testWidgets('bajo 900 px la lista se apila y tocar un producto abre su detalle', (tester) async {
      String? agregado;
      await _montar(tester, tamano: const Size(390, 844), onSel: (i, _) => agregado = i);

      expect(find.byKey(const Key('panel-valorizado')), findsNothing);
      await tester.tap(find.text('Barniz galón'));
      await tester.pumpAndSettle();
      expect(find.text('Ver o editar ficha'), findsOneWidget);

      await tester.tap(find.text('Agregar a venta'));
      await tester.pumpAndSettle();
      expect(agregado, 'p0');
    });
  });
}
