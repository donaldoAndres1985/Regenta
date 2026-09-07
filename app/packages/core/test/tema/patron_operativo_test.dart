import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

/// HU-106 criterio 3: el patron del negocio decide el color secundario.
void main() {
  test('cada patron lleva su color y su suave', () {
    expect(PatronOperativo.ventaDirecta.color, RegentaColors.venta);
    expect(PatronOperativo.ventaDirecta.colorSuave, RegentaColors.ventaSoft);
    expect(PatronOperativo.reserva.color, RegentaColors.reserva);
    expect(PatronOperativo.reserva.colorSuave, RegentaColors.reservaSoft);
    expect(PatronOperativo.comanda.color, RegentaColors.comanda);
    expect(PatronOperativo.comanda.colorSuave, RegentaColors.comandaSoft);
  });

  test('el patron se resuelve desde el claim del JWT', () {
    expect(PatronOperativo.desdeClaim('VENTA_DIRECTA'), PatronOperativo.ventaDirecta);
    expect(PatronOperativo.desdeClaim('RESERVA'), PatronOperativo.reserva);
    expect(PatronOperativo.desdeClaim('COMANDA'), PatronOperativo.comanda);
  });

  test('un claim desconocido no se traga en silencio', () {
    expect(() => PatronOperativo.desdeClaim('FRANQUICIA'), throwsArgumentError);
  });

  test('el tema toma el secundario del patron y el primario siempre es el acento', () {
    for (final patron in PatronOperativo.values) {
      final tema = regentaTheme(patron);
      expect(tema.colorScheme.secondary, patron.color,
          reason: 'el secundario es el color del patron $patron');
      expect(tema.colorScheme.primary, RegentaColors.accent,
          reason: 'el primario es el ocre de Regenta, no cambia con el patron');
      expect(tema.scaffoldBackgroundColor, RegentaColors.paper);
    }
  });
}
