import 'datos/sesion_caja_modelos.dart';

class EstadoDeCaja {
  const EstadoDeCaja({
    this.cargando = false,
    this.cajas = const [],
    this.cajaSeleccionada,
    this.sesionActiva,
    this.movimientos = const [],
    this.error,
  });

  final bool cargando;
  final List<CajaVista> cajas;
  final CajaVista? cajaSeleccionada;
  final SesionCajaActiva? sesionActiva;
  final List<MovimientoCajaVista> movimientos;
  final String? error;

  static const _salidas = {'RETIRO', 'GASTO'};

  /// Una anulación (HU-141) entra con el signo contrario del movimiento que compensa.
  bool _anulaUnaSalida(MovimientoCajaVista anulacion) => movimientos.any(
      (m) => m.id == anulacion.anulaA && _salidas.contains(m.tipo.toUpperCase()));

  num get totalIngresos => movimientos
      .where((m) {
        final tipo = m.tipo.toUpperCase();
        return tipo == 'INGRESO' || tipo == 'VENTA' || (tipo == 'ANULACION' && _anulaUnaSalida(m));
      })
      .fold<num>(0, (sum, m) => sum + m.monto);

  num get totalRetiros => movimientos
      .where((m) {
        final tipo = m.tipo.toUpperCase();
        return _salidas.contains(tipo) || (tipo == 'ANULACION' && !_anulaUnaSalida(m));
      })
      .fold<num>(0, (sum, m) => sum + m.monto);

  num get efectivoEsperado {
    final base = sesionActiva?.montoApertura ?? 0;
    return base + totalIngresos - totalRetiros;
  }

  EstadoDeCaja copyWith({
    bool? cargando,
    List<CajaVista>? cajas,
    CajaVista? cajaSeleccionada,
    SesionCajaActiva? sesionActiva,
    bool clearSesion = false,
    List<MovimientoCajaVista>? movimientos,
    String? error,
    bool clearError = false,
  }) {
    return EstadoDeCaja(
      cargando: cargando ?? this.cargando,
      cajas: cajas ?? this.cajas,
      cajaSeleccionada: cajaSeleccionada ?? this.cajaSeleccionada,
      sesionActiva: clearSesion ? null : (sesionActiva ?? this.sesionActiva),
      movimientos: movimientos ?? this.movimientos,
      error: clearError ? null : (error ?? this.error),
    );
  }
}
