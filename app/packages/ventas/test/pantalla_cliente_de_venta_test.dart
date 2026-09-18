import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// El repositorio de clientes, sin red. Lo que no esté en [clientes] no existe
/// en este negocio.
class _RepoFake implements RepositorioDeClientesDeVenta {
  _RepoFake({List<ClienteDeLaVenta>? clientes})
      : clientes = clientes ??
            [
              const ClienteDeLaVenta(
                id: 'c-1',
                nombre: 'Materiales Cruz S.A.S.',
                tipoDocumento: 'NIT',
                numeroDocumento: '900412883',
                digitoVerificacion: '1',
                condicion: 'crédito 30 d',
              ),
              const ClienteDeLaVenta(
                id: 'c-2',
                nombre: 'Luz Marina Peña',
                tipoDocumento: 'CC',
                numeroDocumento: '52114908',
              ),
            ];

  final List<ClienteDeLaVenta> clientes;

  String? terminoBuscado;
  String? asignadoAId;
  bool quitado = false;
  ClienteNuevo? creado;
  bool creaEnCola = false;
  Object? fallaAlCrear;
  Object? fallaAlAsignar;

  @override
  Future<List<ClienteDeLaVenta>> buscar(String termino) async {
    terminoBuscado = termino;
    if (termino.isEmpty) return clientes;
    return clientes.where((c) => c.coincideCon(termino)).toList();
  }

  @override
  Future<void> asignar({required String ventaId, required String clienteId}) async {
    if (fallaAlAsignar != null) throw fallaAlAsignar!;
    asignadoAId = clienteId;
  }

  @override
  Future<void> quitar({required String ventaId}) async {
    quitado = true;
  }

  @override
  Future<ResultadoDeAlta> crear(ClienteNuevo nuevo) async {
    if (fallaAlCrear != null) throw fallaAlCrear!;
    creado = nuevo;
    if (creaEnCola) return ResultadoDeAlta(id: nuevo.id, quedoEnLaCola: true);
    return ResultadoDeAlta(
      id: nuevo.id,
      cliente: ClienteDeLaVenta(
        id: nuevo.id,
        nombre: nuevo.nombre,
        tipoDocumento: nuevo.tipoDocumento,
        numeroDocumento: nuevo.numeroDocumento,
        digitoVerificacion: nuevo.digitoVerificacion,
      ),
    );
  }
}

Future<void> _montar(
  WidgetTester tester,
  _RepoFake repo, {
  Size size = const Size(390, 844),
  Set<String> permisos = const {'CLIENTES_CLIENTE_CREAR'},
  ClienteDeLaVenta? asignado,
  void Function(ClienteDeLaVenta?)? onAsignado,
}) async {
  await tester.binding.setSurfaceSize(size);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeClientesDeVentaProvider.overrideWithValue(repo),
      ventaEnCursoProvider.overrideWithValue('venta-1'),
      permisosDeLaSesionProvider.overrideWithValue(permisos),
    ],
    child: MaterialApp(
      home: PantallaClienteDeVenta(asignado: asignado, onAsignado: onAsignado),
    ),
  ));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('Criterio 1: Consumidor final queda fijo arriba y marcado cuando no hay cliente',
      (tester) async {
    await _montar(tester, _RepoFake());

    expect(find.text('Consumidor final'), findsOneWidget);
    expect(find.bySemanticsLabel('Consumidor final, seleccionado'), findsOneWidget);
  });

  testWidgets('Criterio 2: tocar un cliente lo asigna a la venta y lo devuelve al POS',
      (tester) async {
    final repo = _RepoFake();
    ClienteDeLaVenta? devuelto;

    await _montar(tester, repo, onAsignado: (c) => devuelto = c);
    await tester.tap(find.text('Materiales Cruz S.A.S.'));
    await tester.pumpAndSettle();

    expect(repo.asignadoAId, 'c-1');
    expect(devuelto?.id, 'c-1');
  });

  testWidgets('Criterio 3: si el cliente no existe en este negocio, se avisa y no se asigna',
      (tester) async {
    final repo = _RepoFake()..fallaAlAsignar = const ErrorDesconocido(404, 'Ese cliente no existe');

    await _montar(tester, repo);
    await tester.tap(find.text('Materiales Cruz S.A.S.'));
    await tester.pumpAndSettle();

    expect(find.textContaining('no existe'), findsWidgets);
  });

  testWidgets('Criterio 4: con cliente asignado, Quitar vuelve a consumidor final',
      (tester) async {
    final repo = _RepoFake();
    ClienteDeLaVenta? devuelto = const ClienteDeLaVenta(
        id: 'c-1', nombre: 'Materiales Cruz S.A.S.', tipoDocumento: 'NIT', numeroDocumento: '900412883');

    await _montar(tester, repo,
        asignado: devuelto, onAsignado: (c) => devuelto = c);
    await tester.tap(find.bySemanticsLabel('Quitar el cliente de la venta'));
    await tester.pumpAndSettle();

    expect(repo.quitado, isTrue);
    expect(devuelto, isNull);
  });

  testWidgets('La búsqueda pide al repositorio, que solo ve los clientes del negocio',
      (tester) async {
    final repo = _RepoFake();

    await _montar(tester, repo);
    await tester.enterText(find.byType(TextField).first, 'cruz');
    await tester.pumpAndSettle(const Duration(milliseconds: 400));

    expect(repo.terminoBuscado, 'cruz');
    expect(find.text('Luz Marina Peña'), findsNothing);
  });

  testWidgets('HU-114 criterio 4: sin permiso de crear, se busca y se asigna pero no se crea',
      (tester) async {
    await _montar(tester, _RepoFake(), permisos: const {});

    expect(find.text('Crear cliente nuevo'), findsNothing);
    expect(find.text('Materiales Cruz S.A.S.'), findsOneWidget);
  });

  testWidgets('HU-114 criterio 1: con documento, nombre y correo se crea y se asigna',
      (tester) async {
    final repo = _RepoFake();
    ClienteDeLaVenta? devuelto;

    await _montar(tester, repo, size: const Size(1280, 900), onAsignado: (c) => devuelto = c);
    await tester.tap(find.text('Crear cliente nuevo'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('campo-numero-documento')), '900412883');
    await tester.enterText(find.byKey(const Key('campo-nombre')), 'Nueva Ferretería SAS');
    await tester.enterText(find.byKey(const Key('campo-correo')), 'compras@nueva.co');
    await tester.pumpAndSettle();
    await tester.tap(find.text('Crear y asignar'));
    await tester.pumpAndSettle();

    expect(repo.creado?.nombre, 'Nueva Ferretería SAS');
    expect(repo.creado?.email, 'compras@nueva.co');
    expect(devuelto?.nombre, 'Nueva Ferretería SAS');
  });

  testWidgets('HU-114 criterio 3: el dígito de verificación del NIT se calcula al escribirlo',
      (tester) async {
    await _montar(tester, _RepoFake(), size: const Size(1280, 900));
    await tester.tap(find.text('Crear cliente nuevo'));
    await tester.pumpAndSettle();

    await tester.enterText(find.byKey(const Key('campo-numero-documento')), '900123456');
    await tester.pumpAndSettle();

    final dv = tester.widget<TextField>(find.byKey(const Key('campo-dv')));
    expect(dv.controller?.text, '8');
  });

  testWidgets('HU-114 criterio 2: el documento repetido ofrece asignar el que ya existe',
      (tester) async {
    final repo = _RepoFake()
      ..fallaAlCrear = const RecursoDuplicado('Ya hay un cliente con ese documento');

    await _montar(tester, repo, size: const Size(1280, 900));
    await tester.tap(find.text('Crear cliente nuevo'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('campo-numero-documento')), '900412883');
    await tester.enterText(find.byKey(const Key('campo-nombre')), 'Materiales Cruz S.A.S.');
    await tester.enterText(find.byKey(const Key('campo-correo')), 'compras@cruz.co');
    await tester.tap(find.text('Crear y asignar'));
    await tester.pumpAndSettle();

    expect(find.textContaining('ya existe'), findsWidgets);
  });

  testWidgets('HU-114 criterio 5: sin conexión se crea con su propio id y queda en la cola',
      (tester) async {
    final repo = _RepoFake()..creaEnCola = true;
    ClienteDeLaVenta? devuelto;

    await _montar(tester, repo, size: const Size(1280, 900), onAsignado: (c) => devuelto = c);
    await tester.tap(find.text('Crear cliente nuevo'));
    await tester.pumpAndSettle();
    await tester.enterText(find.byKey(const Key('campo-numero-documento')), '901222333');
    await tester.enterText(find.byKey(const Key('campo-nombre')), 'Sin señal SAS');
    await tester.enterText(find.byKey(const Key('campo-correo')), 'offline@correo.co');
    await tester.tap(find.text('Crear y asignar'));
    await tester.pumpAndSettle();

    expect(repo.creado?.id, isNotEmpty);
    expect(devuelto?.id, repo.creado?.id);
  });

  testWidgets('En móvil es una pantalla; en escritorio, un panel al lado del carrito',
      (tester) async {
    await _montar(tester, _RepoFake());
    expect(find.byKey(const Key('cliente-venta-movil')), findsOneWidget);
    expect(find.byKey(const Key('cliente-venta-escritorio')), findsNothing);

    await _montar(tester, _RepoFake(), size: const Size(1280, 900));
    expect(find.byKey(const Key('cliente-venta-escritorio')), findsOneWidget);
    expect(find.byKey(const Key('cliente-venta-movil')), findsNothing);
  });

  testWidgets('Cada fila se puede tocar de pie y con una mano: 44 px de alto',
      (tester) async {
    await _montar(tester, _RepoFake());

    expect(tester.getSize(find.bySemanticsLabel('Consumidor final, seleccionado')).height,
        greaterThanOrEqualTo(44));
  });
}
