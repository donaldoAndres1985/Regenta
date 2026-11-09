import '../datos/producto_buscado.dart';

/// Una línea del carrito del POS: un producto y la cantidad que se lleva. El
/// precio y los datos del producto se congelan al agregarlo (snapshot).
class LineaDeCarrito {
  const LineaDeCarrito({
    required this.producto,
    required this.cantidad,
    this.descuentoPct = 0,
  });

  final ProductoBuscado producto;
  final int cantidad;
  final num descuentoPct;

  bool get sinStock => producto.sinStock;

  num get bruto => producto.precioVenta * cantidad;
  num get descuento => bruto * (descuentoPct / 100);
  num get subtotal => bruto - descuento;
  num get impuesto => subtotal * producto.impuestoPct / 100;
  num get total => subtotal + impuesto;

  LineaDeCarrito conCantidad(int nueva) => LineaDeCarrito(
        producto: producto,
        cantidad: nueva,
        descuentoPct: descuentoPct,
      );

  LineaDeCarrito conDescuento(num nuevoDescuento) => LineaDeCarrito(
        producto: producto,
        cantidad: cantidad,
        descuentoPct: nuevoDescuento,
      );
}
