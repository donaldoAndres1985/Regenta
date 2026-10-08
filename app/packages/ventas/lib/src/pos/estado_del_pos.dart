import '../datos/cliente_de_la_venta.dart';
import '../datos/producto_buscado.dart';
import 'linea_de_carrito.dart';

/// El estado de la pantalla de POS: el carrito, la búsqueda y el resultado del
/// cobro.
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
  });

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
      montoParaIdentificar != null && cliente == null && total > montoParaIdentificar!;

  /// Se puede cobrar si hay líneas, ninguna sin stock, el comprador está
  /// identificado cuando hace falta, y no se está cobrando ya.
  bool get puedeCobrar =>
      lineas.isNotEmpty && !hayLineasSinStock && !faltaIdentificarComprador && !cobrando;

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
  }) =>
      EstadoDelPos(
        lineas: lineas ?? this.lineas,
        termino: termino ?? this.termino,
        resultados: resultados ?? this.resultados,
        buscando: buscando ?? this.buscando,
        cobrando: cobrando ?? this.cobrando,
        mensaje: mensaje == _sinCambio ? this.mensaje : mensaje as String?,
        ventaNumero:
            ventaNumero == _sinCambio ? this.ventaNumero : ventaNumero as String?,
        ventaEnCola: ventaEnCola ?? this.ventaEnCola,
        cliente: cliente == _sinCambio ? this.cliente : cliente as ClienteDeLaVenta?,
        montoParaIdentificar: montoParaIdentificar == _sinCambio
            ? this.montoParaIdentificar
            : montoParaIdentificar as num?,
      );

  static const _sinCambio = Object();
}
