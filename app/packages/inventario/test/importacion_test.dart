import 'dart:typed_data';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// HU-164 · importar productos desde Excel.
FilaDeImportacion _fila(int n, String sku, String nombre, [List<String> errores = const []]) =>
    FilaDeImportacion.desdeJson({'numero': n, 'sku': sku, 'nombre': nombre, 'errores': errores});

class _Repo implements RepositorioDeInventario {
  bool importado = false;
  List<FilaDeImportacion>? pedidasOmitidas;

  @override
  Future<List<int>> plantillaDeImportacion() async => [1, 2, 3];

  @override
  Future<VistaPreviaDeImportacion> vistaPreviaDeImportacion(ArchivoElegido archivo) async =>
      VistaPreviaDeImportacion(filas: [
        _fila(2, 'BAR-1', 'Barniz galón'),
        _fila(3, 'LAC-1', 'Laca galón'),
        _fila(4, 'X-2', 'Tarifa mala', ['La tarifa «IVA_8» no es IVA_19, IVA_5, EXENTO ni EXCLUIDO']),
      ], validas: 2, conError: 1);

  @override
  Future<ResultadoDeImportacion> importarProductos(ArchivoElegido archivo) async {
    importado = true;
    return ResultadoDeImportacion(importados: 2, omitidas: [
      _fila(4, 'X-2', 'Tarifa mala', ['La tarifa «IVA_8» no es IVA_19, IVA_5, EXENTO ni EXCLUIDO']),
    ]);
  }

  @override
  Future<List<int>> omitidasComoExcel(List<FilaDeImportacion> omitidas) async {
    pedidasOmitidas = omitidas;
    return [9, 9];
  }

  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Archivos implements ArchivosDeImportacion {
  final Map<String, List<int>> guardados = {};

  @override
  Future<ArchivoElegido?> elegirExcel() async =>
      ArchivoElegido(nombre: 'productos.xlsx', bytes: Uint8List.fromList([1]));

  @override
  Future<void> guardar(String nombre, List<int> bytes) async => guardados[nombre] = bytes;
}

Future<(_Repo, _Archivos)> _montar(WidgetTester tester) async {
  tester.view.physicalSize = const Size(1280, 1200);
  tester.view.devicePixelRatio = 1.0;
  addTearDown(tester.view.reset);
  final repo = _Repo();
  final archivos = _Archivos();
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeInventarioProvider.overrideWithValue(repo),
      archivosDeImportacionProvider.overrideWithValue(archivos),
    ],
    child: const MaterialApp(home: PantallaImportacion()),
  ));
  await tester.pumpAndSettle();
  return (repo, archivos);
}

void main() {
  testWidgets('HU-164 C1 · «Descargar plantilla» guarda el .xlsx de la plantilla', (tester) async {
    final (_, archivos) = await _montar(tester);

    await tester.tap(find.byKey(const Key('importacion-plantilla')));
    await tester.pumpAndSettle();

    expect(archivos.guardados['plantilla-productos.xlsx'], [1, 2, 3]);
  });

  testWidgets('HU-164 C2/C3 · la vista previa muestra los errores por fila y al confirmar importa las válidas', (tester) async {
    final (repo, _) = await _montar(tester);

    await tester.tap(find.byKey(const Key('importacion-elegir')));
    await tester.pumpAndSettle();

    expect(find.text('BAR-1'), findsOneWidget);
    expect(find.textContaining('IVA_8'), findsOneWidget);
    expect(find.text('Importar 2 productos (se omiten 1 con error)'), findsOneWidget);
    expect(repo.importado, isFalse, reason: 'la vista previa no graba nada');

    await tester.tap(find.byKey(const Key('importacion-confirmar')));
    await tester.pumpAndSettle();

    expect(repo.importado, isTrue);
    expect(find.text('Se importaron 2 productos. Se omitieron 1 filas con error.'), findsOneWidget);
  });

  testWidgets('HU-164 C5 · las filas omitidas se descargan en un .xlsx', (tester) async {
    final (repo, archivos) = await _montar(tester);

    await tester.tap(find.byKey(const Key('importacion-elegir')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('importacion-confirmar')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const Key('importacion-omitidas')));
    await tester.pumpAndSettle();

    expect(repo.pedidasOmitidas!.single.sku, 'X-2');
    expect(archivos.guardados['filas-con-error.xlsx'], [9, 9]);
  });
}
