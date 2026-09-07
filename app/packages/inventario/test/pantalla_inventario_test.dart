import 'package:flutter/material.dart';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:regenta_inventario/regenta_inventario.dart';

/// Doble del repositorio: registra los términos con que se buscó y devuelve lo
/// que se le programe.
class _RepoFake implements RepositorioDeInventario {
  _RepoFake({this.resultado, this.codigo});

  ResultadoDeBusqueda? resultado;
  CodigoResuelto? codigo;
  ErrorDeApi? error;

  final List<String?> terminosBuscados = [];
  final List<String> codigosResueltos = [];

  @override
  Future<ResultadoDeBusqueda> buscar({
    String? termino,
    String? categoriaId,
    bool soloBajoMinimo = false,
    int? limite,
  }) async {
    terminosBuscados.add(termino);
    if (error != null) throw error!;
    return resultado ?? const ResultadoDeBusqueda(productos: [], total: 0);
  }

  @override
  Future<CodigoResuelto> resolverCodigo(String codigo) async {
    codigosResueltos.add(codigo);
    if (this.codigo == null) {
      throw const ErrorDesconocido(404, 'Ningun producto tiene ese codigo');
    }
    return this.codigo!;
  }
}

class _EscanerFake implements EscanerDeCodigos {
  _EscanerFake({this.hayCamara = true, this.codigo});

  final bool hayCamara;
  String? codigo;

  @override
  Future<bool> get disponible async => hayCamara;

  @override
  Future<String?> escanearUnCodigo(BuildContext context) async {
    if (!hayCamara) throw const EscaneoNoDisponible();
    return codigo;
  }
}

ProductoEncontrado _producto({
  String id = 'p1',
  String nombre = 'Cemento gris 50 kg',
  String sku = 'CEM-050',
  NivelStock nivel = NivelStock.normal,
  num stock = 142,
  num precio = 32000,
}) =>
    ProductoEncontrado(
      id: id,
      sku: sku,
      codigoBarras: null,
      nombre: nombre,
      categoriaNombre: 'Cementos',
      precioVenta: precio,
      stockTotal: stock,
      nivelStock: nivel,
    );

Future<void> _montar(
  WidgetTester tester, {
  required _RepoFake repo,
  _EscanerFake? escaner,
  void Function(String, num)? onSel,
}) async {
  await tester.pumpWidget(ProviderScope(
    overrides: [
      repositorioDeInventarioProvider.overrideWithValue(repo),
      escanerProvider.overrideWithValue(escaner ?? _EscanerFake(hayCamara: false)),
    ],
    child: MaterialApp(
      home: PantallaInventario(
        onProductoSeleccionado: onSel ?? (_, _) {},
      ),
    ),
  ));
  await tester.pump(); // primer build
  await tester.pump(const Duration(milliseconds: 400)); // búsqueda inicial
}

void main() {
  testWidgets('Criterio 1: con tres caracteres se busca; con menos, no', (tester) async {
    final repo = _RepoFake(resultado: ResultadoDeBusqueda(productos: [_producto()], total: 1));
    await _montar(tester, repo: repo);

    // La búsqueda al abrir va sin término (lista todo).
    expect(repo.terminosBuscados, [null]);

    await tester.enterText(find.byType(TextField).first, 'ce');
    await tester.pump(const Duration(milliseconds: 400));
    expect(repo.terminosBuscados.last, isNull, reason: 'con 2 caracteres no se filtra');

    await tester.enterText(find.byType(TextField).first, 'cem');
    await tester.pump(const Duration(milliseconds: 400));
    expect(repo.terminosBuscados.last, 'cem');
  });

  testWidgets('Criterio 2: al escanear un código válido se abre el producto', (tester) async {
    String? idAbierto;
    num? factorAbierto;
    final repo = _RepoFake(
      resultado: const ResultadoDeBusqueda(productos: [], total: 0),
      codigo: const CodigoResuelto(
          productoId: 'p9', sku: 'CEM-050', nombre: 'Cemento', factor: 1, esAlterno: false),
    );
    await _montar(
      tester,
      repo: repo,
      escaner: _EscanerFake(hayCamara: true, codigo: '7701234567890'),
      onSel: (id, factor) {
        idAbierto = id;
        factorAbierto = factor;
      },
    );

    await tester.tap(find.bySemanticsLabel('Escanear código'));
    await tester.pumpAndSettle();

    expect(repo.codigosResueltos, ['7701234567890']);
    expect(idAbierto, 'p9');
    expect(factorAbierto, 1);
  });

  testWidgets('Criterio 3: sin cámara (web) el botón lleva a la captura manual', (tester) async {
    String? idAbierto;
    final repo = _RepoFake(
      resultado: const ResultadoDeBusqueda(productos: [], total: 0),
      codigo: const CodigoResuelto(
          productoId: 'p3', sku: 'X', nombre: 'X', factor: 1, esAlterno: false),
    );
    await _montar(
      tester,
      repo: repo,
      escaner: _EscanerFake(hayCamara: false),
      onSel: (id, _) => idAbierto = id,
    );

    await tester.tap(find.bySemanticsLabel('Escanear código'));
    await tester.pumpAndSettle();
    expect(find.text('Ingresar código'), findsOneWidget);

    await tester.enterText(find.byType(TextField).last, 'MANUAL-1');
    await tester.tap(find.text('Buscar'));
    await tester.pumpAndSettle();

    expect(repo.codigosResueltos, ['MANUAL-1']);
    expect(idAbierto, 'p3');
  });

  testWidgets('Criterio 4: un código alterno abre el producto con su factor', (tester) async {
    num? factorAbierto;
    final repo = _RepoFake(
      resultado: const ResultadoDeBusqueda(productos: [], total: 0),
      codigo: const CodigoResuelto(
          productoId: 'p1', sku: 'CEM-050', nombre: 'Cemento', factor: 12, esAlterno: true),
    );
    await _montar(
      tester,
      repo: repo,
      escaner: _EscanerFake(hayCamara: true, codigo: 'CAJACEM12'),
      onSel: (_, factor) => factorAbierto = factor,
    );

    await tester.tap(find.bySemanticsLabel('Escanear código'));
    await tester.pumpAndSettle();

    expect(repo.codigosResueltos, ['CAJACEM12']);
    expect(factorAbierto, 12);
  });

  testWidgets('Estado vacío y estado de error se ven distintos', (tester) async {
    final repo = _RepoFake(resultado: const ResultadoDeBusqueda(productos: [], total: 0));
    await _montar(tester, repo: repo);
    expect(find.textContaining('No hay productos'), findsOneWidget);

    repo.error = const ErrorDeRed();
    await tester.enterText(find.byType(TextField).first, 'abcd');
    await tester.pump(const Duration(milliseconds: 400));
    expect(find.textContaining('Sin conexión'), findsOneWidget);
    expect(find.text('Reintentar'), findsOneWidget);
  });
}
