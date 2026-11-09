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

  num get totalIngresos => movimientos
      .where((m) => m.tipo.toUpperCase() == 'INGRESO' || m.tipo.toUpperCase() == 'VENTA')
      .fold<num>(0, (sum, m) => sum + m.monto);

  num get totalRetiros => movimientos
      .where((m) => m.tipo.toUpperCase() == 'RETIRO')
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
