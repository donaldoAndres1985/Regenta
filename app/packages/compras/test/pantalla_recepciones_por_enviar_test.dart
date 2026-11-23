import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';

import 'ayudas_de_ordenes.dart';

class _BandejaFake implements BandejaDeRecepciones {
  _BandejaFake(List<RecepcionPorEnviar> iniciales) : _lista = iniciales;

  List<RecepcionPorEnviar> _lista;
  final _cambios = StreamController<List<RecepcionPorEnviar>>.broadcast();
  final List<String> enviadas = [];
  String? respuesta;

  @override
  Stream<List<RecepcionPorEnviar>> observar() async* {
    yield _lista;
    yield* _cambios.stream;
  }

  void _publicar(List<RecepcionPorEnviar> nueva) {
    _lista = nueva;
    _cambios.add(nueva);
  }

  @override
  Future<String?> enviarAhora(RecepcionPorEnviar r) async {
    enviadas.add(r.id);
    if (respuesta == null) _publicar([for (final x in _lista) if (x.id != r.id) x]);
    return respuesta;
  }

  @override
  Future<void> descartar(String id) async => _publicar([for (final x in _lista) if (x.id != id) x]);
}

RecepcionPorEnviar _r(String id, {bool conError = false, String? error}) => RecepcionPorEnviar(
      id: id,
      ordenId: 'oc-1',
      ordenNumero: 'OC-0231',
      lineas: 2,
      guardadaEn: DateTime(2026, 11, 22, 10, 42),
      conError: conError,
      error: error,
      datos: const {'recepcion': {'ordenId': 'oc-1', 'lineas': []}},
    );

Future<_BandejaFake> _montar(WidgetTester tester, Widget pantalla, List<RecepcionPorEnviar> recepciones) async {
  await tester.binding.setSurfaceSize(const Size(390, 844));
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final bandeja = _BandejaFake(recepciones);
  await tester.pumpWidget(ProviderScope(
    overrides: [bandejaDeRecepcionesProvider.overrideWithValue(bandeja)],
    child: MaterialApp(home: pantalla),
  ));
  await tester.pumpAndSettle();
  return bandeja;
}

void main() {
  final porEnviar = _r('id-1');
  final conError = _r('id-2', conError: true, error: 'La línea 1 pasa lo pedido más la tolerancia de 5 %');

  testWidgets('HU-182 C1 / C3 · Se ven «por enviar» y «con error» con su motivo', (tester) async {
    await _montar(tester, const PantallaRecepcionesPorEnviar(), [porEnviar, conError]);

    expect(find.text('POR ENVIAR'), findsOneWidget);
    expect(find.text('CON ERROR'), findsOneWidget);
    expect(find.text('La línea 1 pasa lo pedido más la tolerancia de 5 %'), findsOneWidget);
    expect(find.text('2 productos · guardada el 22/11 10:42'), findsNWidgets(2));
    expect(find.byKey(const Key('por-enviar-enviar-id-1')), findsOneWidget);
    expect(find.byKey(const Key('por-enviar-corregir-id-2')), findsOneWidget);
    expect(find.byKey(const Key('por-enviar-enviar-id-2')), findsNothing);
    expect(tester.takeException(), isNull);
  });

  testWidgets('HU-182 · «Enviar ahora» la manda y, si entra, sale de la lista', (tester) async {
    final bandeja = await _montar(tester, const PantallaRecepcionesPorEnviar(), [porEnviar]);

    await tester.tap(find.byKey(const Key('por-enviar-enviar-id-1')));
    await tester.pumpAndSettle();

    expect(bandeja.enviadas, ['id-1']);
    expect(find.text('La recepción de OC-0231 entró.'), findsOneWidget);
    expect(find.byKey(const Key('por-enviar-vacio')), findsOneWidget);
  });

  testWidgets('HU-182 · Sin señal todavía, lo dice y sigue en la lista', (tester) async {
    final bandeja = await _montar(tester, const PantallaRecepcionesPorEnviar(), [porEnviar]);
    bandeja.respuesta = 'Todavía no hay señal: sigue guardada y subirá sola.';

    await tester.tap(find.byKey(const Key('por-enviar-enviar-id-1')));
    await tester.pumpAndSettle();

    expect(find.text('Todavía no hay señal: sigue guardada y subirá sola.'), findsOneWidget);
    expect(find.byKey(const Key('por-enviar-id-1')), findsOneWidget);
  });

  testWidgets('HU-182 · Descartar pide confirmación y la saca', (tester) async {
    await _montar(tester, const PantallaRecepcionesPorEnviar(), [porEnviar]);

    await tester.tap(find.byKey(const Key('por-enviar-descartar-id-1')));
    await tester.pumpAndSettle();
    expect(find.text('¿Descartar esta recepción?'), findsOneWidget);
    await tester.tap(find.byKey(const Key('por-enviar-descartar-si')));
    await tester.pumpAndSettle();

    expect(find.byKey(const Key('por-enviar-vacio')), findsOneWidget);
  });

  testWidgets('HU-182 · Compras avisa cuántas recepciones esperan y cuántas tienen error', (tester) async {
    await montarCompras(tester, const [], bandeja: _BandejaFake([porEnviar, conError, _r('id-3')]));

    expect(find.text('2 recepciones por enviar · 1 con error'), findsOneWidget);
    await tester.tap(find.byKey(const Key('compras-por-enviar')));
    await tester.pumpAndSettle();
    expect(find.text('Recepciones por enviar'), findsOneWidget);
  });

  testWidgets('HU-182 · Sin nada por enviar, Compras no muestra el aviso', (tester) async {
    await montarCompras(tester, const [], bandeja: _BandejaFake(const []));

    expect(find.byKey(const Key('compras-por-enviar')), findsNothing);
  });
}
