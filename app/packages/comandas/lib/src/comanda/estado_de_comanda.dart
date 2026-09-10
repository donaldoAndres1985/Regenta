import '../datos/comanda_vista.dart';

/// El estado de la pantalla de comanda (HU-085). Inmutable.
class EstadoDeComanda {
  const EstadoDeComanda({
    this.comanda,
    this.cargando = true,
    this.mensaje,
    this.errorAlCargar = false,
  });

  final ComandaVista? comanda;
  final bool cargando;

  /// Aviso puntual (un 422 de modificadores, un fallo de envío). Lo pinta la
  /// pantalla y se limpia.
  final String? mensaje;
  final bool errorAlCargar;

  EstadoDeComanda copiar({
    ComandaVista? comanda,
    bool? cargando,
    Object? mensaje = _sinCambio,
    bool? errorAlCargar,
  }) {
    return EstadoDeComanda(
      comanda: comanda ?? this.comanda,
      cargando: cargando ?? this.cargando,
      mensaje: identical(mensaje, _sinCambio) ? this.mensaje : mensaje as String?,
      errorAlCargar: errorAlCargar ?? this.errorAlCargar,
    );
  }

  static const _sinCambio = Object();
}
