import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

class _RepoFake implements RepositorioDeVentas {
  /// Lo que devuelve cualquier búsqueda de 3 o más caracteres.
  List<ProductoBuscado> resultados = const [];

  /// Búsquedas con respuesta propia (gana sobre [resultados]).
  final Map<String, List<ProductoBuscado>> porTermino = {};

  /// Códigos de barras que el backend sabe resolver; el resto da 404.
  final Map<String, CodigoResuelto> codigos = {};

  final List<String> terminos = [];
  final List<List<LineaParaEnviar>> ventasConfirmadas = [];

  @override
  Future<List<ProductoBuscado>> buscar(String termino) async {
    terminos.add(termino);
    if (porTermino.containsKey(termino)) return porTermino[termino]!;
    return termino.trim().length >= 3 ? resultados : const [];
  }

  @override
  Future<CodigoResuelto> resolverCodigo(String codigo) async {
    final resuelto = codigos[codigo];
    if (resuelto == null) {
      throw const ErrorDesconocido(404, 'Ningun producto tiene ese codigo');
    }
    return resuelto;
  }

  @override
  Future<ProductoBuscado> verProducto(String productoId) async => [
    ...resultados,
    for (final l in porTermino.values) ...l,
  ].firstWhere((p) => p.id == productoId);

  /// Cuando es true, el cobro simula que no había señal y la venta quedó en la cola.
  bool sinSenal = false;

  /// HU-137: lo que contesta Ventas en `/api/ventas/reglas-de-cobro`.
  num? montoDelNegocio;

  @override
  Future<num?> montoParaIdentificarComprador() async => montoDelNegocio;

  @override
  Future<ResultadoDeCobro> confirmarVenta({
    required String bodegaId,
    required List<LineaParaEnviar> lineas,
    String? clienteId,
  }) async {
    ventasConfirmadas.add(lineas);
    return ResultadoDeCobro(
      origenOfflineId: 'offline-1',
      venta: sinSenal ? null : const VentaCreada(id: 'v1', numero: 'FV-1'),
    );
  }
}

class _EscanerFake implements EscanerDeCodigos {
  @override
  Future<bool> get disponible async => false;
  @override
  Future<String?> escanearUnCodigo(BuildContext context) async =>
      throw const EscaneoNoDisponible();
}

ProductoBuscado _prod({
  String id = 'p1',
  String nombre = 'Cemento gris 50 kg',
  String sku = 'CEM-050',
  NivelStock nivel = NivelStock.normal,
  num precio = 32000,
}) => ProductoBuscado(
  id: id,
  sku: sku,
  nombre: nombre,
  precioVenta: precio,
  nivelStock: nivel,
);

const _rebote = Duration(milliseconds: 300);

Future<void> _montar(
  WidgetTester tester,
  _RepoFake repo, {
  Size size = const Size(390, 844),
  void Function(String)? onCobrada,
  num? montoParaIdentificar,
}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(
    ProviderScope(
      overrides: [
        repositorioDeVentasProvider.overrideWithValue(repo),
        bodegaDeVentaProvider.overrideWithValue('bodega-1'),
        escanerProvider.overrideWithValue(_EscanerFake()),
        if (montoParaIdentificar != null)
          montoParaIdentificarCompradorProvider.overrideWithValue(
            montoParaIdentificar,
          ),
      ],
      child: MaterialApp(home: PantallaPos(onVentaCobrada: onCobrada)),
    ),
  );
  await tester.pump();
}

EstadoDelPos _estado(WidgetTester tester) => ProviderScope.containerOf(
  tester.element(find.byType(PantallaPos)),
).read(controladorDelPosProvider);

Finder get _buscador => find.byKey(const Key('pos-buscador'));

String _textoDelBuscador(WidgetTester tester) => tester
    .widget<TextField>(
      find.descendant(of: _buscador, matching: find.byType(TextField)),
    )
    .controller!
    .text;

Future<void> _escribir(WidgetTester tester, String texto) async {
  await tester.enterText(
    find.descendant(of: _buscador, matching: find.byType(TextField)),
    texto,
  );
}

Future<void> _enter(WidgetTester tester) async {
  await tester.testTextInput.receiveAction(TextInputAction.done);
  await tester.pump();
  await tester.pump();
}

Future<void> _agregar(
  WidgetTester tester,
  _RepoFake repo,
  ProductoBuscado p,
) async {
  repo.resultados = [p];
  await _escribir(tester, 'cem');
  await tester.pump(_rebote);
  await tester.tap(
    find.descendant(
      of: find.byKey(const Key('pos-resultados')),
      matching: find.text(p.nombre),
    ),
  );
  await tester.pump();
}

void main() {
  testWidgets(
    'R9: en móvil el buscador, los +/- y Cobrar miden al menos 44 px',
    (tester) async {
      final repo = _RepoFake();
      await _montar(tester, repo, size: const Size(390, 844));

      expect(tester.getSize(_buscador).height, greaterThanOrEqualTo(44));
      expect(
        find.bySemanticsLabel('Escanear código'),
        findsNothing,
        reason: 'el código entra por el mismo buscador',
      );

      await _agregar(tester, repo, _prod());
      final mas = tester.getSize(find.bySemanticsLabel('Agregar uno').first);
      expect(mas.width, greaterThanOrEqualTo(44));
      expect(mas.height, greaterThanOrEqualTo(44));
      expect(
        tester.getSize(find.widgetWithText(FilledButton, 'Cobrar')).height,
        greaterThanOrEqualTo(44),
      );
    },
  );

  testWidgets(
    'R10: cliente, buscador y venta van de arriba hacia abajo a todo el ancho',
    (tester) async {
      await _montar(tester, _RepoFake(), size: const Size(1280, 900));

      expect(find.byKey(const Key('pos-escritorio')), findsOneWidget);
      expect(find.byKey(const Key('pos-movil')), findsNothing);
      final cliente = tester.getRect(find.byKey(const Key('pos-cliente')));
      final buscador = tester.getRect(_buscador);
      final venta = tester.getRect(find.byKey(const Key('pos-carrito')));
      expect(cliente.top, lessThan(buscador.top));
      expect(buscador.top, lessThan(venta.top));
      expect(venta.width, greaterThan(1200), reason: 'no hay columna lateral');
      expect(find.byKey(const Key('pos-resultados')), findsNothing);

      // Totales y Cobrar debajo del listado, dentro de la misma tarjeta.
      final cobrar = tester.getRect(
        find.widgetWithText(FilledButton, 'Cobrar'),
      );
      expect(venta.contains(cobrar.center), isTrue);
      expect(
        find.text('Busca o escanea un producto para empezar la venta.'),
        findsOneWidget,
      );
    },
  );

  testWidgets(
    'R10a: sin cliente se ofrece agregarlo; con cliente se cambia o se quita',
    (tester) async {
      await _montar(tester, _RepoFake(), size: const Size(1280, 900));

      expect(find.text('Consumidor final'), findsOneWidget);
      expect(find.text('Agregar cliente'), findsOneWidget);

      final contenedor = ProviderScope.containerOf(
        tester.element(find.byType(PantallaPos)),
      );
      contenedor
          .read(controladorDelPosProvider.notifier)
          .fijarCliente(
            const ClienteDeLaVenta(
              id: 'c-1',
              nombre: 'Materiales Cruz S.A.S.',
              tipoDocumento: 'NIT',
              numeroDocumento: '900412883',
              condicion: 'crédito 30 d',
            ),
          );
      await tester.pump();

      expect(find.text('Materiales Cruz S.A.S.'), findsOneWidget);
      expect(find.text('MS'), findsOneWidget, reason: 'iniciales del primer y último término');
      expect(find.text('Cambiar cliente'), findsOneWidget);

      await tester.tap(find.text('Quitar'));
      await tester.pump();
      expect(find.text('Consumidor final'), findsOneWidget);
      expect(_estado(tester).cliente, isNull);
    },
  );

  testWidgets(
    'R10b: los resultados se despliegan sobre la venta y se cierran al agregar',
    (tester) async {
      final repo = _RepoFake()..resultados = [_prod()];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, 'cem');
      await tester.pump(_rebote);

      final resultados = tester.getRect(
        find.byKey(const Key('pos-resultados')),
      );
      final venta = tester.getRect(find.byKey(const Key('pos-carrito')));
      expect(resultados.top, greaterThanOrEqualTo(venta.top - 1));
      expect(
        find.textContaining('CEM-050'),
        findsWidgets,
        reason: 'muestra el código',
      );

      await tester.tap(
        find.descendant(
          of: find.byKey(const Key('pos-resultados')),
          matching: find.text('Cemento gris 50 kg'),
        ),
      );
      await tester.pump();
      expect(find.byKey(const Key('pos-resultados')), findsNothing);
      expect(_estado(tester).lineas.single.producto.id, 'p1');
    },
  );

  group('Búsqueda con Enter', () {
    testWidgets(
      'Enter con un solo resultado agrega ese producto y deja el buscador listo',
      (tester) async {
        final repo = _RepoFake()..resultados = [_prod()];
        await _montar(tester, repo, size: const Size(1280, 900));

        await _escribir(tester, 'cem');
        await tester.pump(_rebote);
        await _enter(tester);

        expect(_estado(tester).lineas.single.producto.id, 'p1');
        expect(_textoDelBuscador(tester), isEmpty);
        expect(find.byKey(const Key('pos-resultados')), findsNothing);
        final campo = tester.widget<TextField>(
          find.descendant(of: _buscador, matching: find.byType(TextField)),
        );
        expect(
          campo.focusNode!.hasFocus,
          isTrue,
          reason: 'listo para el siguiente producto',
        );
      },
    );

    testWidgets('Enter con varios resultados agrega el primero', (
      tester,
    ) async {
      final repo = _RepoFake()
        ..resultados = [
          _prod(id: 'p1', nombre: 'Cemento gris 50 kg'),
          _prod(id: 'p2', nombre: 'Cemento blanco 1 kg', sku: 'CEM-001'),
        ];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, 'cem');
      await tester.pump(_rebote);
      await _enter(tester);

      expect(_estado(tester).lineas.single.producto.id, 'p1');
    });

    testWidgets('con la flecha abajo, Enter agrega el resaltado', (
      tester,
    ) async {
      final repo = _RepoFake()
        ..resultados = [
          _prod(id: 'p1', nombre: 'Cemento gris 50 kg'),
          _prod(id: 'p2', nombre: 'Cemento blanco 1 kg', sku: 'CEM-001'),
        ];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, 'cem');
      await tester.pump(_rebote);
      await tester.sendKeyEvent(LogicalKeyboardKey.arrowDown);
      await tester.pump();
      await _enter(tester);

      expect(_estado(tester).lineas.single.producto.id, 'p2');
    });

    testWidgets(
      'Enter antes de que salga la búsqueda busca en ese momento y agrega el primero',
      (tester) async {
        final repo = _RepoFake()..resultados = [_prod()];
        await _montar(tester, repo, size: const Size(1280, 900));

        await _escribir(tester, 'cem');
        await _enter(tester);
        await tester.pump(_rebote);

        expect(_estado(tester).lineas.single.producto.id, 'p1');
        expect(
          repo.terminos.where((t) => t == 'cem'),
          hasLength(1),
          reason: 'el rebote pendiente se cancela: no se busca dos veces',
        );
      },
    );

    testWidgets(
      'un código numérico largo se agrega directo por código exacto, sin desplegable',
      (tester) async {
        final tubo = _prod(id: 'p2', nombre: 'Tubo PVC 1/2"', sku: 'PVC-012');
        final repo = _RepoFake()
          ..codigos['7702001234567'] = const CodigoResuelto(
            productoId: 'p2',
            factor: 1,
            sku: 'PVC-012',
          )
          ..porTermino['PVC-012'] = [tubo];
        await _montar(tester, repo, size: const Size(1280, 900));

        await _escribir(tester, '7702001234567');
        await _enter(tester);
        await tester.pump(_rebote);

        expect(_estado(tester).lineas.single.producto.id, 'p2');
        expect(find.byKey(const Key('pos-resultados')), findsNothing);
        expect(
          repo.terminos,
          isNot(contains('7702001234567')),
          reason: 'un código no abre la búsqueda por texto',
        );
      },
    );

    testWidgets(
      'un código sin código de barras se resuelve por la referencia exacta',
      (tester) async {
        final repo = _RepoFake()
          ..porTermino['100001169'] = [
            _prod(
              id: 'p9',
              nombre: 'Cemento Argos blanco 1 kg',
              sku: '100001169',
            ),
            _prod(
              id: 'p8',
              nombre: 'Cemento Argos blanco 40 kg',
              sku: '1000011690',
            ),
          ];
        await _montar(tester, repo, size: const Size(1280, 900));

        await _escribir(tester, '100001169');
        await _enter(tester);

        expect(_estado(tester).lineas.single.producto.id, 'p9');
      },
    );

    testWidgets(
      'un producto repetido suma 1 a la cantidad en vez de otra fila',
      (tester) async {
        final repo = _RepoFake()..resultados = [_prod()];
        await _montar(tester, repo, size: const Size(1280, 900));

        for (var i = 0; i < 2; i++) {
          await _escribir(tester, 'cem');
          await tester.pump(_rebote);
          await _enter(tester);
        }

        expect(_estado(tester).lineas, hasLength(1));
        expect(_estado(tester).lineas.single.cantidad, 2);
      },
    );

    testWidgets('sin resultados avisa «No se encontró» y no agrega nada', (
      tester,
    ) async {
      final repo = _RepoFake()..resultados = const [];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, 'xyz');
      await tester.pump(_rebote);
      expect(
        find.text('No se encontró «xyz»'),
        findsOneWidget,
        reason: 'en el desplegable',
      );

      await _enter(tester);
      expect(_estado(tester).lineas, isEmpty);
      expect(
        find.text('No se encontró «xyz»'),
        findsWidgets,
        reason: 'y como aviso',
      );
    });

    testWidgets('un código que no existe avisa y no agrega nada', (
      tester,
    ) async {
      final repo = _RepoFake();
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, '999999999');
      await _enter(tester);

      expect(_estado(tester).lineas, isEmpty);
      expect(find.text('No se encontró «999999999»'), findsOneWidget);
    });
  });

  group('Atajos', () {
    testWidgets('Esc cierra el desplegable sin borrar lo escrito', (
      tester,
    ) async {
      final repo = _RepoFake()..resultados = [_prod()];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, 'cem');
      await tester.pump(_rebote);
      expect(find.byKey(const Key('pos-resultados')), findsOneWidget);

      await tester.sendKeyEvent(LogicalKeyboardKey.escape);
      await tester.pump();
      expect(find.byKey(const Key('pos-resultados')), findsNothing);
      expect(_textoDelBuscador(tester), 'cem');
    });

    testWidgets('F2 lleva el foco al buscador y F12 cobra', (tester) async {
      final repo = _RepoFake()..resultados = [_prod()];
      await _montar(tester, repo, size: const Size(1280, 900));
      await _escribir(tester, 'cem');
      await tester.pump(_rebote);
      await _enter(tester);

      FocusManager.instance.primaryFocus?.unfocus();
      await tester.pump();
      await tester.sendKeyEvent(LogicalKeyboardKey.f2);
      await tester.pump();
      final campo = tester.widget<TextField>(
        find.descendant(of: _buscador, matching: find.byType(TextField)),
      );
      expect(campo.focusNode!.hasFocus, isTrue);

      await tester.sendKeyEvent(LogicalKeyboardKey.f12);
      await tester.pump();
      await tester.pump();
      expect(repo.ventasConfirmadas, hasLength(1));
    });
  });

  testWidgets(
    'R8: agregar un producto sin stock avisa y deshabilita Cobrar antes de confirmar',
    (tester) async {
      final repo = _RepoFake();
      await _montar(tester, repo, size: const Size(390, 844));

      await _agregar(tester, repo, _prod(nivel: NivelStock.cero));

      expect(find.text('Hay líneas sin stock'), findsOneWidget);
      final cobrar = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Cobrar'),
      );
      expect(cobrar.onPressed, isNull);

      // Al quitar la línea, Cobrar se rehabilita.
      await tester.tap(find.bySemanticsLabel('Quitar uno').first);
      await tester.pump();
      expect(find.text('Hay líneas sin stock'), findsNothing);
    },
  );

  testWidgets(
    'HU-137 criterios 2 y 4: sobre el monto y sin cliente, se avisa antes de Cobrar y no se cobra',
    (tester) async {
      final repo = _RepoFake();
      await _montar(
        tester,
        repo,
        size: const Size(390, 844),
        montoParaIdentificar: 50000,
      );

      await _agregar(tester, repo, _prod());
      expect(
        find.byKey(const Key('aviso-identificar-comprador')),
        findsNothing,
        reason: 'bajo el monto, nada cambia',
      );

      await tester.tap(
        find.bySemanticsLabel('Agregar uno').first,
      ); // 64.000 > 50.000
      await tester.pump();

      expect(
        find.byKey(const Key('aviso-identificar-comprador')),
        findsOneWidget,
      );
      expect(find.textContaining('identifica al comprador'), findsOneWidget);
      final aviso = tester.getTopLeft(
        find.byKey(const Key('aviso-identificar-comprador')),
      );
      final boton = tester.getTopLeft(
        find.widgetWithText(FilledButton, 'Cobrar'),
      );
      expect(
        aviso.dy,
        lessThan(boton.dy),
        reason: 'el aviso va antes del botón, no después',
      );
      final cobrar = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Cobrar'),
      );
      expect(cobrar.onPressed, isNull);
    },
  );

  testWidgets(
    'HU-137 criterio 1: el monto que configuró el negocio llega solo, desde Ventas',
    (tester) async {
      final repo = _RepoFake()..montoDelNegocio = 50000;
      await _montar(tester, repo, size: const Size(390, 844));
      await tester.pump();

      await _agregar(tester, repo, _prod(precio: 64000));

      expect(
        find.byKey(const Key('aviso-identificar-comprador')),
        findsOneWidget,
      );
      expect(repo.ventasConfirmadas, isEmpty);
    },
  );

  testWidgets(
    'HU-137 criterio 2: con el comprador identificado, la misma venta se cobra',
    (tester) async {
      final repo = _RepoFake();
      await _montar(
        tester,
        repo,
        size: const Size(390, 844),
        montoParaIdentificar: 50000,
      );
      await _agregar(tester, repo, _prod(precio: 64000));

      final contenedor = ProviderScope.containerOf(
        tester.element(find.byType(PantallaPos)),
      );
      contenedor
          .read(controladorDelPosProvider.notifier)
          .fijarCliente(
            const ClienteDeLaVenta(
              id: 'c-1',
              nombre: 'Materiales Cruz S.A.S.',
              tipoDocumento: 'NIT',
              numeroDocumento: '900412883',
            ),
          );
      await tester.pump();

      expect(
        find.byKey(const Key('aviso-identificar-comprador')),
        findsNothing,
      );
      final cobrar = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Cobrar'),
      );
      expect(cobrar.onPressed, isNotNull);
    },
  );

  testWidgets(
    'HU-137 criterio 3: sin monto configurado se cobra como siempre, por grande que sea',
    (tester) async {
      final repo = _RepoFake();
      await _montar(tester, repo, size: const Size(390, 844));
      await _agregar(tester, repo, _prod(precio: 99000000));

      expect(
        find.byKey(const Key('aviso-identificar-comprador')),
        findsNothing,
      );
      final cobrar = tester.widget<FilledButton>(
        find.widgetWithText(FilledButton, 'Cobrar'),
      );
      expect(cobrar.onPressed, isNotNull);
    },
  );

  testWidgets(
    'R10: el mismo widget se adapta con LayoutBuilder a móvil y a escritorio',
    (tester) async {
      await _montar(tester, _RepoFake(), size: const Size(390, 844));
      expect(find.byType(LayoutBuilder), findsWidgets);
      expect(find.byKey(const Key('pos-movil')), findsOneWidget);
      expect(find.byType(PantallaPos), findsOneWidget);

      await tester.binding.setSurfaceSize(const Size(1280, 900));
      await tester.pump();
      expect(find.byKey(const Key('pos-escritorio')), findsOneWidget);
      expect(find.byKey(const Key('pos-movil')), findsNothing);
      expect(find.byType(PantallaPos), findsOneWidget);
    },
  );

  testWidgets(
    'R11: Cobrar arma la venta contra el backend con las líneas del carrito',
    (tester) async {
      final repo = _RepoFake();
      String? cobrada;
      await _montar(
        tester,
        repo,
        size: const Size(390, 844),
        onCobrada: (n) => cobrada = n,
      );

      await _agregar(tester, repo, _prod());
      await tester.tap(
        find.bySemanticsLabel('Agregar uno').first,
      ); // cantidad 2
      await tester.pump();

      await tester.tap(find.widgetWithText(FilledButton, 'Cobrar'));
      await tester.pump();
      await tester.pump();

      expect(repo.ventasConfirmadas, hasLength(1));
      expect(repo.ventasConfirmadas.single.single.cantidad, 2);
      expect(cobrada, 'FV-1');
    },
  );

  testWidgets(
    'HU-043 criterio 1: sin señal, cobrar avisa que la venta quedó guardada',
    (tester) async {
      final repo = _RepoFake()..sinSenal = true;
      String? cobrada;
      await _montar(
        tester,
        repo,
        size: const Size(390, 844),
        onCobrada: (n) => cobrada = n,
      );

      await _agregar(tester, repo, _prod());
      await tester.tap(find.widgetWithText(FilledButton, 'Cobrar'));
      await tester.pump();
      await tester.pump();

      expect(
        repo.ventasConfirmadas,
        hasLength(1),
        reason: 'la venta se cobró igual',
      );
      expect(
        cobrada,
        isNull,
        reason: 'todavía no tiene número: lo asigna el servidor al subir',
      );
      expect(
        find.textContaining('sube sola cuando vuelva la conexión'),
        findsOneWidget,
      );
    },
  );

  group('R12: varias ventas a la vez', () {
    testWidgets('al abrir hay tres ventas y un +, y la activa es la 1', (
      tester,
    ) async {
      await _montar(tester, _RepoFake(), size: const Size(1280, 900));

      for (final n in [1, 2, 3]) {
        expect(find.byKey(Key('venta-$n')), findsOneWidget);
      }
      expect(find.byKey(const Key('venta-nueva')), findsOneWidget);
      expect(_estado(tester).ventaActiva, 0);
    });

    testWidgets(
      'pasar a la 2 da una venta en blanco y volver a la 1 la recupera',
      (tester) async {
        final repo = _RepoFake();
        await _montar(tester, repo, size: const Size(1280, 900));
        await _agregar(tester, repo, _prod());
        await tester.tap(find.bySemanticsLabel('Agregar uno').first);
        await tester.pump();

        await tester.tap(find.byKey(const Key('venta-2')));
        await tester.pump();
        expect(find.text('Cemento gris 50 kg'), findsNothing);
        expect(
          find.text('Busca o escanea un producto para empezar la venta.'),
          findsOneWidget,
        );

        await tester.tap(find.byKey(const Key('venta-1')));
        await tester.pump();
        expect(find.text('Cemento gris 50 kg'), findsOneWidget);
        expect(_estado(tester).lineas.single.cantidad, 2);
      },
    );

    testWidgets('el + abre una cuarta venta y la deja activa', (tester) async {
      await _montar(tester, _RepoFake(), size: const Size(1280, 900));

      await tester.tap(find.byKey(const Key('venta-nueva')));
      await tester.pump();

      expect(find.byKey(const Key('venta-4')), findsOneWidget);
      expect(_estado(tester).ventaActiva, 3);
    });

    testWidgets('Poner en espera guarda la venta y pasa a una en blanco', (
      tester,
    ) async {
      final repo = _RepoFake();
      await _montar(tester, repo, size: const Size(1280, 900));
      await _agregar(tester, repo, _prod());

      await tester.tap(find.text('Poner en espera'));
      await tester.pump();

      expect(_estado(tester).ventaActiva, 1);
      expect(_estado(tester).lineas, isEmpty);
      expect(_estado(tester).productosEnVenta(0), 1);
    });

    testWidgets('el botón de una venta con productos dice cuántos lleva', (
      tester,
    ) async {
      final repo = _RepoFake();
      await _montar(tester, repo, size: const Size(1280, 900));
      await _agregar(tester, repo, _prod());
      await tester.tap(find.byKey(const Key('venta-2')));
      await tester.pump();

      expect(
        find.bySemanticsLabel(RegExp(r'Venta 1.*1 producto')),
        findsOneWidget,
      );
    });

    testWidgets('cobrar la venta 2 no toca la venta 1', (tester) async {
      final repo = _RepoFake();
      await _montar(tester, repo, size: const Size(1280, 900));
      await _agregar(
        tester,
        repo,
        _prod(id: 'p1', nombre: 'Cemento gris 50 kg'),
      );

      await tester.tap(find.byKey(const Key('venta-2')));
      await tester.pump();
      await _agregar(tester, repo, _prod(id: 'p2', nombre: 'Tubo PVC'));
      await tester.tap(find.widgetWithText(FilledButton, 'Cobrar'));
      await tester.pump();
      await tester.pump();

      expect(repo.ventasConfirmadas.single.single.productoId, 'p2');
      expect(_estado(tester).ventaActiva, 1);
      expect(_estado(tester).lineas, isEmpty);

      await tester.tap(find.byKey(const Key('venta-1')));
      await tester.pump();
      expect(find.text('Cemento gris 50 kg'), findsOneWidget);
    });
  });

  group('Rediseño POS e interacción mejorada', () {
    testWidgets('Enter con 1 resultado agrega a la venta', (tester) async {
      final repo = _RepoFake();
      repo.porTermino['cemen'] = [_prod(id: 'p1', nombre: 'Cemento gris 50 kg')];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, 'cemen');
      await tester.pump(_rebote);
      await _enter(tester);

      expect(_estado(tester).lineas.length, 1);
      expect(_estado(tester).lineas.single.producto.nombre, 'Cemento gris 50 kg');
      expect(_textoDelBuscador(tester), isEmpty);
    });

    testWidgets('Enter con varios resultados agrega el resaltado (por defecto el primero)', (tester) async {
      final repo = _RepoFake();
      repo.porTermino['cemen'] = [
        _prod(id: 'p1', nombre: 'Cemento gris 50 kg'),
        _prod(id: 'p2', nombre: 'Cemento blanco 1 kg'),
      ];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, 'cemen');
      await tester.pump(_rebote);
      await _enter(tester);

      expect(_estado(tester).lineas.length, 1);
      expect(_estado(tester).lineas.single.producto.id, 'p1');
    });

    testWidgets('código numérico exacto (6+ dígitos) busca por código y agrega directo sin desplegable', (tester) async {
      final repo = _RepoFake();
      repo.codigos['10001204'] = const CodigoResuelto(productoId: 'p1', factor: 1, sku: '10001204');
      repo.porTermino['10001204'] = [_prod(id: 'p1', nombre: 'Cemento gris 50 kg', sku: '10001204')];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, '10001204');
      await _enter(tester);

      expect(_estado(tester).lineas.length, 1);
      expect(_estado(tester).lineas.single.producto.sku, '10001204');
      expect(find.byKey(const Key('pos-resultados')), findsNothing);
    });

    testWidgets('producto repetido suma 1 a la cantidad existente en vez de crear otra fila', (tester) async {
      final repo = _RepoFake();
      final prod = _prod(id: 'p1', nombre: 'Cemento gris 50 kg');
      repo.resultados = [prod];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _agregar(tester, repo, prod);
      expect(_estado(tester).lineas.length, 1);
      expect(_estado(tester).lineas.single.cantidad, 1);

      await _agregar(tester, repo, prod);
      expect(_estado(tester).lineas.length, 1);
      expect(_estado(tester).lineas.single.cantidad, 2);
    });

    testWidgets('búsqueda sin resultados muestra mensaje dentro del desplegable', (tester) async {
      final repo = _RepoFake();
      repo.porTermino['inexistente'] = const [];
      await _montar(tester, repo, size: const Size(1280, 900));

      await _escribir(tester, 'inexistente');
      await tester.pump(_rebote);

      expect(find.text('No se encontró «inexistente»'), findsOneWidget);
    });

    testWidgets('cantidad es editable por teclado', (tester) async {
      final repo = _RepoFake();
      await _montar(tester, repo, size: const Size(1280, 900));
      await _agregar(tester, repo, _prod(id: 'p1', nombre: 'Cemento gris 50 kg'));

      expect(_estado(tester).lineas.single.cantidad, 1);

      await tester.tap(find.text('1'));
      await tester.pump();

      final textFields = find.byType(TextField);
      expect(textFields, findsNWidgets(2));
      final stepperInput = textFields.last;

      await tester.enterText(stepperInput, '12');
      await tester.testTextInput.receiveAction(TextInputAction.done);
      await tester.pump();

      expect(_estado(tester).lineas.single.cantidad, 12);
    });
  });
}
