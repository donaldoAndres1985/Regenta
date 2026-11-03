import '../datos/cliente_de_la_venta.dart';
import '../datos/producto_buscado.dart';
import 'linea_de_carrito.dart';

/// Ventas que el POS ofrece al abrir (R12).
const int ventasAlAbrir = 3;

/// Tope de ventas abiertas a la vez: el *+* no pasa de aquí (R12).
const int ventasMaximas = 9;

/// El estado de la pantalla de POS: el carrito, la búsqueda y el resultado del
/// cobro de la venta activa, más cuántos productos lleva cada una de las otras.
class EstadoDelPos {
  const EstadoDelPos({
    this.lineas = const [],
    this.termino = '',
    this.resultados = const [],
    this.buscando = false,
    this.cobrando = false,
    this.mensaje,
    this.ventaNumero,
    this.ventaEnCola = false,
    this.cliente,
    this.montoParaIdentificar,
    this.ventaActiva = 0,
    this.productosPorVenta = const [0, 0, 0],
    this.desplegableCerrado = false,
  });

  /// R7c: `Esc` cerró la lista de resultados sin borrar lo escrito.
  final bool desplegableCerrado;

  /// R12: cuántas ventas hay abiertas.
  int get ventasAbiertas => productosPorVenta.length;

  /// R12: cuál de las ventas abiertas se está mostrando (desde 0).
  final int ventaActiva;

  /// R12: productos que lleva cada venta guardada. La activa se cuenta de
  /// [lineas]; usa [productosEnVenta].
  final List<int> productosPorVenta;

  int productosEnVenta(int venta) =>
      venta == ventaActiva ? lineas.length : productosPorVenta[venta];

  final List<LineaDeCarrito> lineas;
  final String termino;
  final List<ProductoBuscado> resultados;
  final bool buscando;
  final bool cobrando;
  final String? mensaje;

  /// Número de la venta creada tras un cobro exitoso.
  final String? ventaNumero;

  /// HU-043 criterio 1: se cobró sin señal. La venta quedó guardada en el
  /// celular y sube sola; todavía no tiene número, que lo asigna el servidor.
  final bool ventaEnCola;

  /// HU-113: quien compra. `null` es consumidor final, que es como nace toda
  /// venta y es el camino que más se usa en mostrador.
  final ClienteDeLaVenta? cliente;

  /// HU-137: sobre este total la venta no se cobra a consumidor final. `null`
  /// es que el negocio no lo configuró, y entonces no se exige nada.
  final num? montoParaIdentificar;

  int get unidades => lineas.fold(0, (a, l) => a + l.cantidad);
  num get subtotal => lineas.fold<num>(0, (a, l) => a + l.subtotal);
  num get impuesto => lineas.fold<num>(0, (a, l) => a + l.impuesto);
  num get total => subtotal + impuesto;

  bool get hayLineasSinStock => lineas.any((l) => l.sinStock);

  /// HU-137 criterio 2: la venta supera el monto y nadie dijo quién compra.
  bool get faltaIdentificarComprador =>
      montoParaIdentificar != null &&
      cliente == null &&
      total > montoParaIdentificar!;

  /// Se puede cobrar si hay líneas, ninguna sin stock, el comprador está
  /// identificado cuando hace falta, y no se está cobrando ya.
  bool get puedeCobrar =>
      lineas.isNotEmpty &&
      !hayLineasSinStock &&
      !faltaIdentificarComprador &&
      !cobrando;

  EstadoDelPos copiar({
    List<LineaDeCarrito>? lineas,
    String? termino,
    List<ProductoBuscado>? resultados,
    bool? buscando,
    bool? cobrando,
    Object? mensaje = _sinCambio,
    Object? ventaNumero = _sinCambio,
    bool? ventaEnCola,
    Object? cliente = _sinCambio,
    Object? montoParaIdentificar = _sinCambio,
    int? ventaActiva,
    List<int>? productosPorVenta,
    bool? desplegableCerrado,
  }) => EstadoDelPos(
    lineas: lineas ?? this.lineas,
    termino: termino ?? this.termino,
    resultados: resultados ?? this.resultados,
    buscando: buscando ?? this.buscando,
    cobrando: cobrando ?? this.cobrando,
    mensaje: mensaje == _sinCambio ? this.mensaje : mensaje as String?,
    ventaNumero: ventaNumero == _sinCambio
        ? this.ventaNumero
        : ventaNumero as String?,
    ventaEnCola: ventaEnCola ?? this.ventaEnCola,
    cliente: cliente == _sinCambio
        ? this.cliente
        : cliente as ClienteDeLaVenta?,
    montoParaIdentificar: montoParaIdentificar == _sinCambio
        ? this.montoParaIdentificar
        : montoParaIdentificar as num?,
    ventaActiva: ventaActiva ?? this.ventaActiva,
    productosPorVenta: productosPorVenta ?? this.productosPorVenta,
    desplegableCerrado: desplegableCerrado ?? this.desplegableCerrado,
  );

  static const _sinCambio = Object();
}
