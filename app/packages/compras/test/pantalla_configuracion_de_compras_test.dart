import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_compras/regenta_compras.dart';
import 'package:regenta_core/regenta_core.dart';

class _RepoFake implements RepositorioDeConfiguracionDeCompras {
  ConfiguracionDeCompras actual = const ConfiguracionDeCompras();
  final List<ConfiguracionDeCompras> guardadas = [];
  ErrorDeApi? falla;

  @override
  Future<ConfiguracionDeCompras> ver() async => actual;

  @override
  Future<ConfiguracionDeCompras> guardar(ConfiguracionDeCompras c) async {
    if (falla != null) throw falla!;
    guardadas.add(c);
    actual = c;
    return c;
  }
}

Future<_RepoFake> _montar(WidgetTester tester, {Size tamano = const Size(1440, 900)}) async {
  await tester.binding.setSurfaceSize(tamano);
  addTearDown(() => tester.binding.setSurfaceSize(null));
  final repo = _RepoFake();
  await tester.pumpWidget(ProviderScope(
    overrides: [repositorioDeConfiguracionDeComprasProvider.overrideWithValue(repo)],
    child: const MaterialApp(home: PantallaConfiguracionDeCompras()),
  ));
  await tester.pumpAndSettle();
  return repo;
}

void main() {
  testWidgets('HU-173 C1 · Por omisión: exige aprobación, sin monto mínimo, tolerancia 5 %', (tester) async {
    await _montar(tester);

    expect(tester.widget<SwitchListTile>(find.byKey(const Key('config-exige-aprobacion'))).value, isTrue);
    expect(tester.widget<TextField>(find.byKey(const Key('config-monto-minimo'))).controller!.text, '');
    expect(tester.widget<TextField>(find.byKey(const Key('config-tolerancia'))).controller!.text, '5');
    expect(find.text('Toda orden necesita que alguien con permiso la apruebe.'), findsOneWidget);
  });

  testWidgets('HU-173 C3 · Con monto mínimo se explica qué órdenes se aprueban solas y se guarda', (tester) async {
    final repo = await _montar(tester);
    await tester.enterText(find.byKey(const Key('config-monto-minimo')), '1000000');
    await tester.enterText(find.byKey(const Key('config-tolerancia')), '0');
    await tester.pumpAndSettle();

    expect(find.text('Las órdenes desde \$1.000.000 necesitan aprobación; las menores se aprueban solas.'),
        findsOneWidget);
    await tester.tap(find.byKey(const Key('config-guardar')));
    await tester.pumpAndSettle();

    final g = repo.guardadas.single;
    expect(g.exigeAprobacion, isTrue);
    expect(g.montoMinimoAprobacion, 1000000);
    expect(g.toleranciaRecepcionPct, 0);
    expect(find.text('Configuración de compras guardada.'), findsOneWidget);
  });

  testWidgets('HU-173 C2 · Sin aprobación: el monto mínimo se deshabilita y no se envía', (tester) async {
    final repo = await _montar(tester);
    await tester.tap(find.byKey(const Key('config-exige-aprobacion')));
    await tester.pumpAndSettle();

    expect(tester.widget<TextField>(find.byKey(const Key('config-monto-minimo'))).enabled, isFalse);
    expect(find.text('Toda orden confirmada queda aprobada sola.'), findsOneWidget);
    await tester.tap(find.byKey(const Key('config-guardar')));
    await tester.pumpAndSettle();

    expect(repo.guardadas.single.aJson()['montoMinimoAprobacion'], isNull);
    expect(repo.guardadas.single.exigeAprobacion, isFalse);
  });

  testWidgets('HU-173 · Una tolerancia por encima de 100 % no se deja guardar', (tester) async {
    final repo = await _montar(tester);
    await tester.enterText(find.byKey(const Key('config-tolerancia')), '150');
    await tester.tap(find.byKey(const Key('config-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('La tolerancia va de 0 a 100 %.'), findsOneWidget);
    expect(repo.guardadas, isEmpty);
  });

  testWidgets('HU-173 · Sin COMPRAS_CONFIGURAR se explica que no hay permiso', (tester) async {
    final repo = await _montar(tester);
    repo.falla = const SinPermiso('Sin permiso');
    await tester.tap(find.byKey(const Key('config-guardar')));
    await tester.pumpAndSettle();

    expect(find.text('No tienes permiso para cambiar la configuración de compras.'), findsOneWidget);
  });

  testWidgets('HU-173 · En el celular la pantalla cabe sin desbordarse', (tester) async {
    await _montar(tester, tamano: const Size(390, 844));

    expect(tester.takeException(), isNull);
  });
}
