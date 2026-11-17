import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// HU-163 · detallar desde el POS cuando falta existencia.
class _RepoFake implements RepositorioDeVentas {
  @override
  dynamic noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _Detallado implements ConsultaDeDetallado {
  _Detallado({this.hayOferta = true});

  final bool hayOferta;
  (String, String, num)? hecho;

  @override
  Future<OfertaDeDetalladoPos?> oferta(String productoId, String bodegaId) async => hayOferta
      ? const OfertaDeDetalladoPos(
          origenId: 'cunete', origen: 'Pintulux blanco cuñete', destino: 'Pintulux blanco galón', factor: 5)
      : null;

  @override
  Future<num> detallar(String origenId, String bodegaId, num cantidad) async {
    hecho = (origenId, bodegaId, cantidad);
    return 5;
  }
}

const _galonSinStock = ProductoBuscado(
  id: 'galon',
  sku: 'GAL-B',
  nombre: 'Pintulux blanco galón',
  precioVenta: 60000,
  nivelStock: NivelStock.cero,
  existencia: 0,
);

Future<ProviderContainer> _montar(WidgetTester tester, _Detallado detallado, {bool permiso = true}) async {
  await tester.binding.setSurfaceSize(const Size(1200, 800));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final container = ProviderContainer(overrides: [
    repositorioDeVentasProvider.overrideWithValue(_RepoFake()),
    bodegaDeVentaProvider.overrideWithValue('bodega-1'),
    consultaDeDetalladoProvider.overrideWithValue(detallado),
    puedeDetallarEnPosProvider.overrideWithValue(permiso),
  ]);
  addTearDown(container.dispose);
  await tester.pumpWidget(UncontrolledProviderScope(
    container: container,
    child: const MaterialApp(home: Scaffold(body: PantallaPos())),
  ));
  await tester.pumpAndSettle();
  container.read(controladorDelPosProvider.notifier).agregar(_galonSinStock);
  await tester.pumpAndSettle();
  return container;
}

void main() {
  testWidgets('HU-163 C1/C2 · sin galones y con cuñetes, se ofrece detallar; al aceptar, la línea queda vendible', (tester) async {
    final detallado = _Detallado();
    final container = await _montar(tester, detallado);

    expect(find.text('¿Detallar 1 Pintulux blanco cuñete → 5 Pintulux blanco galón?'), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Detallar'));
    await tester.pumpAndSettle();

    expect(detallado.hecho, ('cunete', 'bodega-1', 1));
    final estado = container.read(controladorDelPosProvider);
    expect(estado.lineas.single.sinStock, isFalse);
    expect(estado.lineas.single.producto.existencia, 5);
    expect(estado.hayLineasSinStock, isFalse);
  });

  testWidgets('HU-163 C3 · sin permiso de detallar, no se ofrece', (tester) async {
    await _montar(tester, _Detallado(), permiso: false);

    expect(find.textContaining('¿Detallar'), findsNothing);
  });

  testWidgets('HU-163 C4 · sin equivalencia hacia el producto, el POS se comporta como hoy', (tester) async {
    final container = await _montar(tester, _Detallado(hayOferta: false));

    expect(find.textContaining('¿Detallar'), findsNothing);
    expect(container.read(controladorDelPosProvider).lineas.single.sinStock, isTrue);
  });
}
