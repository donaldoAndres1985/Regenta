import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// HU-158 · vender fracciones con los precios del catálogo (I13-I15).
const _rojo = ProductoBuscado(
  id: 'p1',
  sku: 'PIN-R',
  nombre: 'Pintulux rojo galón',
  precioVenta: 60000,
  nivelStock: NivelStock.normal,
  impuestoPct: 19,
  unidad: 'GLN',
);

const _cuarto = FraccionDeVenta(medida: '¼ galón', factor: 0.25, precio: 18000);

void main() {
  test('HU-158 C3 · 3 de ¼ de galón cobran 3 × \$ 18.000 = \$ 54.000, no la cuarta parte del galón', () {
    const l = LineaDeCarrito(producto: _rojo, cantidad: 3, fraccion: _cuarto);

    expect(l.total, 54000);
    expect(l.impuesto, 8621.85);
  });

  test('HU-158 C4 · al backend se manda la medida y cuántas fracciones; el precio lo pone él', () {
    const estado = EstadoDelPos(lineas: [LineaDeCarrito(producto: _rojo, cantidad: 3, fraccion: _cuarto)]);

    final l = estado.lineasParaEnviar().single;

    expect(l.fraccion, '¼ galón');
    expect(l.cantidad, 3);
    expect(l.aJson()['fraccion'], '¼ galón');
  });

  test('HU-158 · la búsqueda trae la unidad real del producto', () {
    final p = ProductoBuscado.desdeJson({
      'id': 'p1', 'sku': 'X', 'nombre': 'Cable', 'precioVenta': 1000, 'nivelStock': 'NORMAL',
      'unidad': 'MTS',
    });

    expect(p.unidad, 'MTS');
  });
}
