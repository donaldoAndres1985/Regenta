import '../datos/ticket_de_cocina.dart';

/// El estado de la pantalla KDS (HU-088). Inmutable.
class EstadoDeCocina {
  const EstadoDeCocina({
    this.estaciones = const [],
    this.estacionSeleccionadaId,
    this.tickets = const [],
    this.cargando = true,
    this.mensaje,
    this.errorAlCargar = false,
  });

  final List<EstacionDeCocina> estaciones;
  final String? estacionSeleccionadaId;
  final List<TicketDeCocina> tickets;
  final bool cargando;

  /// Aviso puntual (un ticket que ya no existe, un fallo al avanzar).
  final String? mensaje;
  final bool errorAlCargar;

  List<TicketDeCocina> get nuevos =>
      tickets.where((t) => t.estado == 'NUEVO').toList(growable: false);
  List<TicketDeCocina> get enPreparacion =>
      tickets.where((t) => t.estado == 'EN_PREPARACION').toList(growable: false);
  List<TicketDeCocina> get listos =>
      tickets.where((t) => t.estado == 'LISTO').toList(growable: false);

  /// R3 (KDS.md): promedio y máximo de lo que llevan esperando los tickets vivos.
  int get demoraMediaMin => tickets.isEmpty
      ? 0
      : (tickets.fold<int>(0, (s, t) => s + t.minutosTranscurridos) / tickets.length).round();
  int get demoraMaximaMin =>
      tickets.isEmpty ? 0 : tickets.map((t) => t.minutosTranscurridos).reduce((a, b) => a > b ? a : b);

  EstadoDeCocina copiar({
    List<EstacionDeCocina>? estaciones,
    Object? estacionSeleccionadaId = _sinCambio,
    List<TicketDeCocina>? tickets,
    bool? cargando,
    Object? mensaje = _sinCambio,
    bool? errorAlCargar,
  }) {
    return EstadoDeCocina(
      estaciones: estaciones ?? this.estaciones,
      estacionSeleccionadaId: identical(estacionSeleccionadaId, _sinCambio)
          ? this.estacionSeleccionadaId
          : estacionSeleccionadaId as String?,
      tickets: tickets ?? this.tickets,
      cargando: cargando ?? this.cargando,
      mensaje: identical(mensaje, _sinCambio) ? this.mensaje : mensaje as String?,
      errorAlCargar: errorAlCargar ?? this.errorAlCargar,
    );
  }

  static const _sinCambio = Object();
}
