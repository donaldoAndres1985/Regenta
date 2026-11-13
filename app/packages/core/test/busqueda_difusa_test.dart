import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_core/regenta_core.dart';

void main() {
  group('busqueda_difusa', () {
    test('coincideConTokens encuentra palabras en cualquier orden con texto intermedio cuando está activa', () {
      ConfiguradorDeBusqueda.busquedaDifusaHabilitada = true;
      const consulta = 'esmal negr';

      expect(coincideConTokens('Esmalte pintulan negro', consulta), isTrue);
      expect(coincideConTokens('esmalte pinte galon negro', consulta), isTrue);
      expect(coincideConTokens('esmalte negro', consulta), isTrue);
      expect(coincideConTokens('negro esmalte', consulta), isTrue);
      expect(coincideConTokens('Esmalte poliuretano color NEGRO brillante', consulta), isTrue);
      expect(coincideConTokens('Esmalte blanco', consulta), isFalse);
      expect(coincideConTokens('Pintura negra al agua', consulta), isFalse);
    });

    test('coincideConTokens en modo estándar busca como antes (subcadena directa rápida)', () {
      ConfiguradorDeBusqueda.busquedaDifusaHabilitada = false;

      // Subcadena corrida exacta coincide
      expect(coincideConTokens('Esmalte negro brillante', 'esmalte neg'), isTrue);

      // Si las palabras están separadas por otra ("pintulan"), en modo estándar no coincide
      expect(coincideConTokens('Esmalte pintulan negro', 'esmal negr'), isFalse);

      // Restaurar valor por defecto
      ConfiguradorDeBusqueda.busquedaDifusaHabilitada = true;
    });

    test('coincideConTokens ignora tildes y mayúsculas', () {
      expect(coincideConTokens('Pantalón Cómodo Azul', 'pantalon comodo'), isTrue);
      expect(coincideConTokens('Camión de Carga', 'camion carga'), isTrue);
    });

    test('coincideConTokens funciona con campos adicionales (SKU, código, documento)', () {
      expect(
        coincideConTokens(
          'Esmalte Sintético',
          'esmal 770123',
          camposAdicionales: ['PIN-01', '770123456789'],
        ),
        isTrue,
      );

      expect(
        coincideConTokens(
          'ADELMO CARRASCAL',
          'adelmo 9314',
          camposAdicionales: ['CC 9.314.711', '9314711'],
        ),
        isTrue,
      );
    });

    test('consulta vacía coincide siempre con cualquier texto', () {
      expect(coincideConTokens('Cualquier cosa', ''), isTrue);
      expect(coincideConTokens('Cualquier cosa', '   '), isTrue);
    });
  });
}
