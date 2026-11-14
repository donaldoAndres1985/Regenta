import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_usuarios/regenta_usuarios.dart';

/// HU-139 · los métodos de pago que no se cuadran al cerrar la caja, desde la app.
class _RepoCajaFake implements RepositorioDeConfigDeCaja {
  _RepoCajaFake(this.metodos);

  List<String> metodos;
  List<String>? enviado;

  @override
  Future<List<String>> verMetodosNoCuadrables() async => metodos;

  @override
  Future<List<String>> fijarMetodosNoCuadrables(List<String> nuevos) async {
    enviado = nuevos;
    metodos = nuevos;
    return nuevos;
  }
}

Future<void> _montar(WidgetTester tester, _RepoCajaFake repo) async {
  await tester.pumpWidget(
    ProviderScope(
      overrides: [repositorioDeConfigDeCajaProvider.overrideWithValue(repo)],
      child: const MaterialApp(
        home: Scaffold(body: SingleChildScrollView(child: TarjetaMetodosNoCuadrables())),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

bool _seCuadra(WidgetTester tester, String metodo) =>
    tester.widget<SwitchListTile>(find.byKey(ValueKey('cuadrar-$metodo'))).value;

void main() {
  testWidgets(
      'HU-139 C1 · Dado un negocio con la configuración por defecto, '
      'débito, crédito, bono y crédito aparecen como no cuadrables', (tester) async {
    await _montar(tester,
        _RepoCajaFake(['BONO', 'CREDITO', 'TARJETA_CREDITO', 'TARJETA_DEBITO']));

    expect(_seCuadra(tester, 'TARJETA_DEBITO'), isFalse);
    expect(_seCuadra(tester, 'TARJETA_CREDITO'), isFalse);
    expect(_seCuadra(tester, 'BONO'), isFalse);
    expect(_seCuadra(tester, 'CREDITO'), isFalse);
    expect(_seCuadra(tester, 'TRANSFERENCIA'), isTrue);
    expect(_seCuadra(tester, 'EFECTIVO'), isTrue);
  });

  testWidgets(
      'HU-139 C2 · Dado un cambio en la lista, cuando guardo, '
      'se envían los métodos que quedaron sin cuadrar', (tester) async {
    final repo = _RepoCajaFake(['BONO']);
    await _montar(tester, repo);

    await tester.tap(find.byKey(const ValueKey('cuadrar-QR')));
    await tester.pumpAndSettle();
    await tester.tap(find.byKey(const ValueKey('cuadrar-BONO')));
    await tester.pumpAndSettle();
    await tester.ensureVisible(find.text('Guardar métodos de cierre'));
    await tester.tap(find.text('Guardar métodos de cierre'));
    await tester.pumpAndSettle();

    expect(repo.enviado, unorderedEquals(['QR']));
  });

  testWidgets(
      'HU-139 C4 · Dado el efectivo, no se puede marcar como no cuadrable: '
      'el efectivo siempre se cuadra', (tester) async {
    await _montar(tester, _RepoCajaFake(['BONO']));

    final efectivo =
        tester.widget<SwitchListTile>(find.byKey(const ValueKey('cuadrar-EFECTIVO')));
    expect(efectivo.value, isTrue);
    expect(efectivo.onChanged, isNull);
    expect(find.text('El efectivo siempre se cuenta al cerrar.'), findsOneWidget);
  });
}
