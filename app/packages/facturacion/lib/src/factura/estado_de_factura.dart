import '../datos/factura_vista.dart';

/// El estado de la pantalla de una factura.
class EstadoDeFactura {
  const EstadoDeFactura({
    this.factura,
    this.cargando = true,
    this.error,
    this.enviando = false,
    this.avisoEnvio,
  });

  final FacturaVista? factura;
  final bool cargando;
  final String? error;
  final bool enviando;

  /// Mensaje tras enviar (éxito o fallo), para el snackbar.
  final String? avisoEnvio;

  bool get puedeEnviar => (factura?.aceptada ?? false) && !enviando;

  EstadoDeFactura copiar({
    FacturaVista? factura,
    bool? cargando,
    Object? error = _sinCambio,
    bool? enviando,
    Object? avisoEnvio = _sinCambio,
  }) {
    return EstadoDeFactura(
      factura: factura ?? this.factura,
      cargando: cargando ?? this.cargando,
      error: identical(error, _sinCambio) ? this.error : error as String?,
      enviando: enviando ?? this.enviando,
      avisoEnvio: identical(avisoEnvio, _sinCambio) ? this.avisoEnvio : avisoEnvio as String?,
    );
  }

  static const _sinCambio = Object();
}
