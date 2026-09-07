import '../datos/producto_buscado.dart';

/// Una línea del carrito del POS: un producto y la cantidad que se lleva. El
/// precio y los datos del producto se congelan al agregarlo (snapshot).
class LineaDeCarrito {
  const LineaDeCarrito({required this.producto, required this.cantidad});

  final ProductoBuscado producto;
  final int cantidad;

  bool get sinStock => producto.sinStock;

  num get subtotal => producto.precioVenta * cantidad;
  num get impuesto => subtotal * producto.impuestoPct / 100;
  num get total => subtotal + impuesto;

  LineaDeCarrito conCantidad(int nueva) =>
      LineaDeCarrito(producto: producto, cantidad: nueva);
}
