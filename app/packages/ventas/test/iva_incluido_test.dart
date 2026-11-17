import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

/// HU-149 · el POS y el ticket muestran el IVA real, con el precio de la etiqueta como total
/// (I2 de docs/dominio/inventario.md): base = precio / (1 + t), IVA = precio − base.
ProductoBuscado _prod(String id, num precio, num pct) => ProductoBuscado(
      id: id,
      sku: id,
      nombre: id,
      precioVenta: precio,
      nivelStock: NivelStock.normal,
      impuestoPct: pct,
    );

void main() {
  test('HU-149 C1 · la búsqueda trae la tarifa real del producto, no un 0 fijo', () {
    final p = ProductoBuscado.desdeJson({
      'id': 'p1',
      'sku': 'BAR-1',
      'nombre': 'Barniz',
      'precioVenta': 35500,
      'nivelStock': 'NORMAL',
      'tarifaIva': 'IVA_19',
      'impuestoPct': 19,
    });

    expect(p.impuestoPct, 19);
    expect(p.tarifaIva, 'IVA_19');
  });

  test('HU-149 · una línea de \$ 35.500 al 19 % cobra \$ 35.500: base \$ 29.831,93 + IVA \$ 5.668,07', () {
    final l = LineaDeCarrito(producto: _prod('p1', 35500, 19), cantidad: 1);

    expect(l.total, 35500);
    expect(l.subtotal, 29831.93);
    expect(l.impuesto, 5668.07);
  });

  test('HU-149 · con 10 % de descuento la línea cobra \$ 31.950', () {
    final l = LineaDeCarrito(producto: _prod('p1', 35500, 19), cantidad: 1, descuentoPct: 10);

    expect(l.total, 31950);
    expect(l.subtotal + l.impuesto, 31950);
  });

  test('HU-149 · el total del POS es la suma de las etiquetas, sin sumar el IVA encima', () {
    final estado = EstadoDelPos(lineas: [
      LineaDeCarrito(producto: _prod('p1', 35500, 19), cantidad: 1),
      LineaDeCarrito(producto: _prod('p2', 4000, 0), cantidad: 2),
    ]);

    expect(estado.total, 43500);
    expect(estado.impuesto, 5668.07);
    expect(estado.subtotal, 43500 - 5668.07);
  });

  test('HU-149 C2 · el ticket desglosa base e IVA por tarifa', () {
    final desglose = desgloseDeIva(const [
      LineaTicket(nombre: 'Barniz', cantidad: 1, precioUnitario: 35500, total: 35500, impuestoPct: 19),
      LineaTicket(nombre: 'Arroz', cantidad: 2, precioUnitario: 2000, total: 4000),
      LineaTicket(nombre: 'Rodillo', cantidad: 1, precioUnitario: 10500, total: 10500, impuestoPct: 5),
    ]);

    expect(desglose.map((d) => d.porcentaje), [19, 5, 0]);
    expect(desglose[0].base, 29831.93);
    expect(desglose[0].iva, 5668.07);
    expect(desglose[1].base, 10000);
    expect(desglose[1].iva, 500);
    expect(desglose[2].base, 4000);
    expect(desglose[2].iva, 0);
  });
}
