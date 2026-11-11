import '../datos/cliente_de_la_venta.dart';
import '../datos/producto_buscado.dart';
import '../datos/repositorio_de_ventas.dart';
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
    this.totalesPorVenta = const [0, 0, 0],
    this.clientesPorVenta = const [null, null, null],
    this.desplegableCerrado = false,
    this.nota,
    this.descuentoGeneralPct = 0,
    this.descuentoGeneralValor = 0,
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

  /// Totales de las ventas abiertas para mostrar en las pestañas.
  final List<num> totalesPorVenta;

  /// Nombre de cliente o mostrador de cada venta abierta.
  final List<String?> clientesPorVenta;

  int productosEnVenta(int venta) =>
      venta == ventaActiva ? lineas.length : (venta < productosPorVenta.length ? productosPorVenta[venta] : 0);

  num totalEnVenta(int venta) =>
      venta == ventaActiva ? total : (venta < totalesPorVenta.length ? totalesPorVenta[venta] : 0);

  String? clienteEnVenta(int venta) =>
      venta == ventaActiva ? cliente?.nombre : (venta < clientesPorVenta.length ? clientesPorVenta[venta] : null);

  final List<LineaDeCarrito> lineas;
  final String termino;
  final List<ProductoBuscado> resultados;
  final bool buscando;
  final bool cobrando;
  final String? mensaje;

  /// Nota opcional de la venta (ej. "entregar en obra...").
  final String? nota;

  /// Descuento global aplicado a la venta.
  final num descuentoGeneralPct;
  final num descuentoGeneralValor;

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

  num get unidades => lineas.fold<num>(0, (a, l) => a + l.cantidad);
  num get subtotalBruto => lineas.fold<num>(0, (a, l) => a + l.bruto);
  num get descuentoLineas => lineas.fold<num>(0, (a, l) => a + l.descuento);
  num get descuentoTotal => descuentoLineas + descuentoGeneralValor;
  num get subtotal => (subtotalBruto - descuentoTotal).clamp(0, double.infinity);
  num get impuesto => lineas.fold<num>(0, (a, l) => a + l.impuesto);
  num get total => subtotal + impuesto;

  bool get hayLineasSinStock => lineas.any((l) => l.sinStock);

  /// Las líneas listas para mandar al backend, con el descuento global
  /// prorrateado por línea: el contrato de ventas solo tiene descuento por
  /// línea, no uno a nivel de venta, así que acá se funden los dos.
  List<LineaParaEnviar> lineasParaEnviar() {
    final brutoTotal = subtotalBruto;
    return [
      for (final l in lineas)
        LineaParaEnviar(
          productoId: l.producto.id,
          sku: l.producto.sku,
          nombre: l.producto.nombre,
          cantidad: l.cantidad,
          precioUnitario: l.producto.precioVenta,
          impuestoPct: l.producto.impuestoPct,
          costoUnitario: l.producto.costoUnitario,
          descuentoPct: _descuentoEfectivoPct(l, brutoTotal),
        ),
    ];
  }

  num _descuentoEfectivoPct(LineaDeCarrito l, num brutoTotal) {
    if (l.bruto <= 0) return 0;
    final prorrateoGlobal = brutoTotal > 0 ? (l.bruto / brutoTotal) * descuentoGeneralValor : 0;
    final totalDescuentoValor = l.descuento + prorrateoGlobal;
    return (totalDescuentoValor / l.bruto * 100).clamp(0, 100);
  }

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
    List<num>? totalesPorVenta,
    List<String?>? clientesPorVenta,
    bool? desplegableCerrado,
    Object? nota = _sinCambio,
    num? descuentoGeneralPct,
    num? descuentoGeneralValor,
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
    totalesPorVenta: totalesPorVenta ?? this.totalesPorVenta,
    clientesPorVenta: clientesPorVenta ?? this.clientesPorVenta,
    desplegableCerrado: desplegableCerrado ?? this.desplegableCerrado,
    nota: nota == _sinCambio ? this.nota : nota as String?,
    descuentoGeneralPct: descuentoGeneralPct ?? this.descuentoGeneralPct,
    descuentoGeneralValor: descuentoGeneralValor ?? this.descuentoGeneralValor,
  );

  static const _sinCambio = Object();
}
