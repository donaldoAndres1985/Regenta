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
  });

  final List<LineaDeCarrito> lineas;
  final String termino;
  final List<ProductoBuscado> resultados;
  final bool buscando;
  final bool cobrando;
  final String? mensaje;

  /// Número de la venta creada tras un cobro exitoso.
  final String? ventaNumero;

  int get unidades => lineas.fold(0, (a, l) => a + l.cantidad);
  num get subtotal => lineas.fold<num>(0, (a, l) => a + l.subtotal);
  num get impuesto => lineas.fold<num>(0, (a, l) => a + l.impuesto);
  num get total => subtotal + impuesto;

  bool get hayLineasSinStock => lineas.any((l) => l.sinStock);

  /// Se puede cobrar si hay líneas, ninguna sin stock, y no se está cobrando ya.
  bool get puedeCobrar => lineas.isNotEmpty && !hayLineasSinStock && !cobrando;

  EstadoDelPos copiar({
    List<LineaDeCarrito>? lineas,
    String? termino,
    List<ProductoBuscado>? resultados,
    bool? buscando,
    bool? cobrando,
    Object? mensaje = _sinCambio,
    Object? ventaNumero = _sinCambio,
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
      );

  static const _sinCambio = Object();
}
