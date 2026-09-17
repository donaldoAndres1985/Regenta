import '../datos/comanda_vista.dart';
import '../datos/cuenta_vista.dart';

/// El estado de la pantalla «Dividir cuenta» (HU-089). Inmutable.
class EstadoDeDivision {
  const EstadoDeDivision({
    this.comanda,
    this.cuentas = const [],
    this.cargando = true,
    this.mensaje,
    this.errorAlCargar = false,
  });

  final ComandaVista? comanda;
  final List<CuentaVista> cuentas;
  final bool cargando;
  final String? mensaje;
  final bool errorAlCargar;

  /// Lo que suman las cuentas que todavía no se pagaron.
  num get faltaPorCobrar => cuentas.where((c) => !c.pagada).fold<num>(0, (s, c) => s + c.total);

  /// Criterio 5: cuando todas quedan pagadas, la comanda se cierra sola.
  bool get todasPagadas => cuentas.isNotEmpty && cuentas.every((c) => c.pagada);

  EstadoDeDivision copiar({
    ComandaVista? comanda,
    List<CuentaVista>? cuentas,
    bool? cargando,
    Object? mensaje = _sinCambio,
    bool? errorAlCargar,
  }) {
    return EstadoDeDivision(
      comanda: comanda ?? this.comanda,
      cuentas: cuentas ?? this.cuentas,
      cargando: cargando ?? this.cargando,
      mensaje: identical(mensaje, _sinCambio) ? this.mensaje : mensaje as String?,
      errorAlCargar: errorAlCargar ?? this.errorAlCargar,
    );
  }

  static const _sinCambio = Object();
}
