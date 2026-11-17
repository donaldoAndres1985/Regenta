import 'package:regenta_core/regenta_core.dart';

import '../datos/producto_buscado.dart';
import 'iva.dart';

/// Una línea del carrito del POS: un producto y la cantidad que se lleva. El
/// precio y los datos del producto se congelan al agregarlo (snapshot).
class LineaDeCarrito {
  const LineaDeCarrito({
    required this.producto,
    required this.cantidad,
    this.descuentoPct = 0,
  });

  final ProductoBuscado producto;
  final num cantidad;
  final num descuentoPct;

  bool get sinStock =>
      !ConfiguradorDeStock.politicaStockNegativoHabilitada && producto.sinStock;

  /// HU-149: el precio del producto es el de la etiqueta, con IVA incluido (I2). El total de
  /// la línea es ese precio menos el descuento; la base y el IVA salen de adentro.
  num get bruto => producto.precioVenta * cantidad;
  num get descuento => bruto * (descuentoPct / 100);
  num get total => bruto - descuento;
  num get subtotal => baseSinIva(total, producto.impuestoPct);
  num get impuesto => redondear2(total - subtotal);

  LineaDeCarrito conCantidad(num nueva) => LineaDeCarrito(
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
