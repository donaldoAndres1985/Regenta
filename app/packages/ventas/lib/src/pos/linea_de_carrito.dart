import 'package:regenta_core/regenta_core.dart';

import '../datos/fraccion_de_venta.dart';
import '../datos/producto_buscado.dart';
import 'iva.dart';

/// Una línea del carrito del POS: un producto y la cantidad que se lleva. El
/// precio y los datos del producto se congelan al agregarlo (snapshot).
///
/// HU-158: con [fraccion], [cantidad] es cuántas de esa fracción y cada una vale el precio de
/// la fracción (no proporcional al del producto).
class LineaDeCarrito {
  const LineaDeCarrito({
    required this.producto,
    required this.cantidad,
    this.descuentoPct = 0,
    this.fraccion,
    this.precioDeLista,
  });

  final ProductoBuscado producto;
  final num cantidad;
  final num descuentoPct;
  final FraccionDeVenta? fraccion;

  /// HU-132 · El precio de la lista de precios del cliente (IVA incluido). Nulo: el del catálogo.
  final num? precioDeLista;

  bool get sinStock =>
      !ConfiguradorDeStock.politicaStockNegativoHabilitada && producto.sinStock;

  /// El precio de una unidad de la línea, con IVA incluido.
  num get precioUnitario => fraccion?.precio ?? precioDeLista ?? producto.precioVenta;

  /// HU-149: el precio del producto es el de la etiqueta, con IVA incluido (I2). El total de
  /// la línea es ese precio menos el descuento; la base y el IVA salen de adentro.
  num get bruto => precioUnitario * cantidad;
  num get descuento => bruto * (descuentoPct / 100);
  num get total => bruto - descuento;
  num get subtotal => baseSinIva(total, producto.impuestoPct);
  num get impuesto => redondear2(total - subtotal);

  LineaDeCarrito conCantidad(num nueva) => LineaDeCarrito(
        producto: producto,
        cantidad: nueva,
        descuentoPct: descuentoPct,
        fraccion: fraccion,
        precioDeLista: precioDeLista,
      );

  LineaDeCarrito conDescuento(num nuevoDescuento) => LineaDeCarrito(
        producto: producto,
        cantidad: cantidad,
        descuentoPct: nuevoDescuento,
        fraccion: fraccion,
        precioDeLista: precioDeLista,
      );

  /// HU-132 · Con el precio de una lista (nulo: vuelve al del catálogo).
  LineaDeCarrito conPrecioDeLista(num? precio) => LineaDeCarrito(
        producto: producto,
        cantidad: cantidad,
        descuentoPct: descuentoPct,
        fraccion: fraccion,
        precioDeLista: precio,
      );
}
