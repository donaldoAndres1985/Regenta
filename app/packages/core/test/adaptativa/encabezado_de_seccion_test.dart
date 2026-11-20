import 'package:regenta_core/regenta_core.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Future<void> _montar(WidgetTester tester, Widget hijo) => tester.pumpWidget(
      MaterialApp(home: Scaffold(body: Column(children: [hijo]))),
    );

void main() {
  group('Encabezado de sección (design/comportamiento/Encabezado.md)', () {
    testWidgets('E1 y E2 · ícono, título de 25 px negrita y subtítulo', (tester) async {
      await _montar(
        tester,
        const EncabezadoDeSeccion(
          icono: Icons.inventory_2_outlined,
          titulo: 'Inventario',
          subtitulo: '2721 productos',
        ),
      );

      expect(find.byIcon(Icons.inventory_2_outlined), findsOneWidget);
      final titulo = tester.widget<Text>(find.text('Inventario'));
      expect(titulo.style?.fontSize, 25);
      expect(titulo.style?.fontWeight, FontWeight.bold);
      expect(find.text('2721 productos'), findsOneWidget);
    });

    testWidgets('E1 · las acciones van a la derecha, en la misma fila', (tester) async {
      await _montar(
        tester,
        EncabezadoDeSeccion(
          icono: Icons.people_outline,
          titulo: 'Clientes', subtitulo: 'x',
          acciones: [FilledButton(key: const Key('accion'), onPressed: () {}, child: const Text('Nuevo'))],
        ),
      );

      final titulo = tester.getCenter(find.text('Clientes'));
      final accion = tester.getCenter(find.byKey(const Key('accion')));
      expect(accion.dx, greaterThan(titulo.dx));
      expect((accion.dy - titulo.dy).abs(), lessThan(20));
    });

    testWidgets('E3 · mismo alto y márgenes con o sin subtítulo y acciones', (tester) async {
      await _montar(tester,
          const EncabezadoDeSeccion(key: Key('a'), icono: Icons.payments_outlined, titulo: 'Caja', subtitulo: 'x'));
      final solo = tester.getSize(find.byKey(const Key('a')));
      final tituloSolo = tester.getTopLeft(find.byIcon(Icons.payments_outlined));

      await _montar(
        tester,
        EncabezadoDeSeccion(
          key: const Key('b'),
          icono: Icons.payments_outlined,
          titulo: 'Caja',
          subtitulo: 'Caja Principal',
          acciones: [IconButton(onPressed: () {}, icon: const Icon(Icons.refresh))],
        ),
      );
      final completo = tester.getSize(find.byKey(const Key('b')));
      final tituloCompleto = tester.getTopLeft(find.byIcon(Icons.payments_outlined));

      expect(solo.height, 86);
      expect(const EncabezadoDeSeccion(icono: Icons.payments_outlined, titulo: 'Caja', subtitulo: 'x').preferredSize.height, 86);
      expect(completo.height, solo.height);
      expect(tituloCompleto, tituloSolo);
      expect(tituloSolo.dx, 24);
    });

    testWidgets('E4 · no muestra ruta de migas', (tester) async {
      await _montar(tester, const EncabezadoDeSeccion(icono: Icons.inventory_2_outlined, titulo: 'Inventario', subtitulo: 'x'));
      expect(find.textContaining('Regenta /'), findsNothing);
    });
  });
}
