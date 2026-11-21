import '../compra_directa/estado.dart';

/// HU-175 C4 · La factura con la que llega lo de una orden: lo mismo que en la compra directa —
/// número, fecha, flete, descuento global y si se paga de contado o queda a crédito.
class FacturaDeRecepcion {
  const FacturaDeRecepcion({
    this.numero = '',
    this.fecha,
    this.flete = 0,
    this.descuentoGlobal = 0,
    this.condicion = CondicionDePago.credito,
    this.metodo = MetodoDePagoDeCompra.efectivo,
    this.desdeLaCaja = false,
    this.referencia = '',
  });

  final String numero;
  final DateTime? fecha;
  final num flete;
  final num descuentoGlobal;
  final CondicionDePago condicion;
  final MetodoDePagoDeCompra metodo;
  final bool desdeLaCaja;
  final String referencia;

  bool get deContado => condicion == CondicionDePago.contado;

  /// Lo que impide registrarla, o null.
  String? get problema {
    if (deContado && numero.trim().isEmpty) {
      return 'Una compra de contado necesita el número de la factura del proveedor.';
    }
    if (flete < 0 || descuentoGlobal < 0) return 'El flete y el descuento no pueden ser negativos.';
    return null;
  }

  Map<String, dynamic> aJson() => {
        if (numero.trim().isNotEmpty) 'facturaProveedor': numero.trim(),
        if (fecha != null) 'fechaFactura': fecha!.toIso8601String().substring(0, 10),
        if (flete > 0) 'flete': flete,
        if (descuentoGlobal > 0) 'descuentoGlobal': descuentoGlobal,
        'condicionPago': deContado ? 'CONTADO' : 'CREDITO',
        if (deContado) 'metodoPago': metodo.codigo,
        if (deContado && metodo == MetodoDePagoDeCompra.efectivo) 'pagarDesdeLaCaja': desdeLaCaja,
        if (deContado && referencia.trim().isNotEmpty) 'referenciaPago': referencia.trim(),
      };

  FacturaDeRecepcion copiar({
    String? numero,
    Object? fecha = _sinCambio,
    num? flete,
    num? descuentoGlobal,
    CondicionDePago? condicion,
    MetodoDePagoDeCompra? metodo,
    bool? desdeLaCaja,
    String? referencia,
  }) =>
      FacturaDeRecepcion(
        numero: numero ?? this.numero,
        fecha: fecha == _sinCambio ? this.fecha : fecha as DateTime?,
        flete: flete ?? this.flete,
        descuentoGlobal: descuentoGlobal ?? this.descuentoGlobal,
        condicion: condicion ?? this.condicion,
        metodo: metodo ?? this.metodo,
        desdeLaCaja: desdeLaCaja ?? this.desdeLaCaja,
        referencia: referencia ?? this.referencia,
      );

  static const _sinCambio = Object();
}
