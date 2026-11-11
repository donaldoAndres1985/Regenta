import 'package:flutter_test/flutter_test.dart';
import 'package:regenta_ventas/regenta_ventas.dart';

ProductoBuscado _prod({required String id, required num precio}) => ProductoBuscado(
      id: id,
      sku: id,
      nombre: id,
      precioVenta: precio,
      nivelStock: NivelStock.normal,
      impuestoPct: 19,
    );

void main() {
  test('lineasParaEnviar manda el descuento propio de la línea', () {
    const estado = EstadoDelPos(
      lineas: [
        LineaDeCarrito(producto: ProductoBuscado(
          id: 'p1',
          sku: 'p1',
          nombre: 'p1',
          precioVenta: 1000,
          nivelStock: NivelStock.normal,
        ), cantidad: 1, descuentoPct: 10),
      ],
    );

    final enviadas = estado.lineasParaEnviar();

    expect(enviadas.single.descuentoPct, 10);
  });

  test('lineasParaEnviar prorratea el descuento global entre las líneas según su bruto', () {
    final estado = EstadoDelPos(
      lineas: [
        LineaDeCarrito(producto: _prod(id: 'p1', precio: 3000), cantidad: 1), // bruto 3000
        LineaDeCarrito(producto: _prod(id: 'p2', precio: 1000), cantidad: 1), // bruto 1000
      ],
      descuentoGeneralValor: 400, // 10% de 4000, repartido 300/100
    );

    final enviadas = estado.lineasParaEnviar();

    expect(enviadas[0].descuentoPct, closeTo(10, 0.001)); // 300/3000
    expect(enviadas[1].descuentoPct, closeTo(10, 0.001)); // 100/1000
  });

  test('lineasParaEnviar combina el descuento propio con el prorrateo global', () {
    final estado = EstadoDelPos(
      lineas: [
        LineaDeCarrito(producto: _prod(id: 'p1', precio: 2000), cantidad: 1, descuentoPct: 5),
        LineaDeCarrito(producto: _prod(id: 'p2', precio: 2000), cantidad: 1),
      ],
      descuentoGeneralValor: 200, // 100 por línea (bruto igual en ambas)
    );

    final enviadas = estado.lineasParaEnviar();

    // p1: 5% propio (100) + 100 global = 200 de 2000 = 10%
    expect(enviadas[0].descuentoPct, closeTo(10, 0.001));
    // p2: 0% propio + 100 global = 100 de 2000 = 5%
    expect(enviadas[1].descuentoPct, closeTo(5, 0.001));
  });

  test('lineasParaEnviar nunca pasa de 100% aunque el descuento global sea mayor que el bruto', () {
    final estado = EstadoDelPos(
      lineas: [LineaDeCarrito(producto: _prod(id: 'p1', precio: 1000), cantidad: 1)],
      descuentoGeneralValor: 5000,
    );

    expect(estado.lineasParaEnviar().single.descuentoPct, 100);
  });
}
