/// Un renglón de pago que se está armando en pantalla, antes de mandarlo
/// (HU-039: el cobro puede ser con varios medios a la vez).
class LineaDePagoEnCurso {
  const LineaDePagoEnCurso({
    required this.metodo,
    required this.monto,
    this.montoRecibido,
    this.referencia,
    this.franquicia,
  });

  /// Uno de los valores del `MetodoDePago` del backend: EFECTIVO,
  /// TARJETA_DEBITO, TARJETA_CREDITO, TRANSFERENCIA, QR, BONO, OTRO.
  final String metodo;

  /// Lo que este renglón cubre de la venta.
  final num monto;

  /// Solo EFECTIVO: lo que entregó el cliente. El cambio es la diferencia.
  final num? montoRecibido;
  final String? referencia;
  final String? franquicia;

  num get cambio =>
      metodo == 'EFECTIVO' && montoRecibido != null ? (montoRecibido! - monto).clamp(0, double.infinity) : 0;
}

/// El estado de la pantalla de Cobro: los pagos que se van armando contra el
/// total de la venta, y cómo va el envío al confirmar.
class EstadoDelCobro {
  const EstadoDelCobro({
    required this.total,
    this.clienteAsignado = false,
    this.pagos = const [],
    this.procesando = false,
    this.paso,
    this.error,
    this.ventaNumero,
    this.requiereAutorizacionCredito = false,
    this.sinConexion = false,
  });

  final num total;

  /// R4 de `POS.md`: el crédito exige cliente asignado a la venta.
  final bool clienteAsignado;

  final List<LineaDePagoEnCurso> pagos;
  final bool procesando;

  /// Qué paso del envío está en curso, para mostrarlo ("Confirmando
  /// disponibilidad…", "Registrando pago…"). `null` cuando no se está
  /// procesando.
  final String? paso;

  final String? error;

  /// El número de la venta ya cobrada. Si no es null, el cobro terminó.
  final String? ventaNumero;

  /// HU-040 criterio 4: el cliente tiene cartera vencida; se puede reintentar
  /// autorizando, si quien está cobrando tiene el permiso.
  final bool requiereAutorizacionCredito;

  /// El primer paso (crear el borrador) falló por falta de red: no tiene
  /// sentido pedir método de pago sin señal. Quien abrió esta pantalla debe
  /// caer al camino `/offline` (HU-043) en vez de mostrar este error.
  final bool sinConexion;

  num get pagado => pagos.fold<num>(0, (a, p) => a + p.monto);
  num get saldoPendiente => (total - pagado).clamp(0, double.infinity);
  bool get cubierto => pagos.isNotEmpty && saldoPendiente <= 0;
  bool get terminado => ventaNumero != null;

  EstadoDelCobro copiar({
    List<LineaDePagoEnCurso>? pagos,
    bool? procesando,
    Object? paso = _sinCambio,
    Object? error = _sinCambio,
    Object? ventaNumero = _sinCambio,
    bool? requiereAutorizacionCredito,
    bool? clienteAsignado,
    bool? sinConexion,
  }) =>
      EstadoDelCobro(
        total: total,
        clienteAsignado: clienteAsignado ?? this.clienteAsignado,
        pagos: pagos ?? this.pagos,
        procesando: procesando ?? this.procesando,
        paso: paso == _sinCambio ? this.paso : paso as String?,
        error: error == _sinCambio ? this.error : error as String?,
        ventaNumero: ventaNumero == _sinCambio ? this.ventaNumero : ventaNumero as String?,
        requiereAutorizacionCredito: requiereAutorizacionCredito ?? this.requiereAutorizacionCredito,
        sinConexion: sinConexion ?? this.sinConexion,
      );

  static const _sinCambio = Object();
}
