import 'package:flutter/painting.dart';
import 'package:regenta_core/regenta_core.dart';

/// El estado de una reserva en el calendario (HU-075 criterio 2). Cada uno trae
/// el par de colores del mockup: fondo suave y filo a la izquierda.
enum EstadoDeReserva {
  pendiente('PENDIENTE', RegentaColors.warnSoft, RegentaColors.warn),
  confirmada('CONFIRMADA', RegentaColors.reservaSoft, RegentaColors.reserva),
  checkIn('CHECK_IN', RegentaColors.okSoft, RegentaColors.ok),
  checkOut('CHECK_OUT', RegentaColors.sunken, RegentaColors.muted),
  otro('OTRO', RegentaColors.sunken, RegentaColors.faint);

  const EstadoDeReserva(this.codigo, this.fondo, this.filo);

  final String codigo;
  final Color fondo;
  final Color filo;

  static EstadoDeReserva desde(String? s) => switch ((s ?? '').toUpperCase()) {
        'PENDIENTE' => EstadoDeReserva.pendiente,
        'CONFIRMADA' => EstadoDeReserva.confirmada,
        'CHECK_IN' => EstadoDeReserva.checkIn,
        'CHECK_OUT' => EstadoDeReserva.checkOut,
        _ => EstadoDeReserva.otro,
      };
}

/// Un recurso: una fila del calendario.
class RecursoDelCalendario {
  const RecursoDelCalendario({
    required this.id,
    required this.codigo,
    required this.nombre,
  });

  final String id;
  final String codigo;
  final String nombre;

  factory RecursoDelCalendario.desdeJson(Map<String, dynamic> j) => RecursoDelCalendario(
        id: j['id'] as String,
        codigo: (j['codigo'] ?? '') as String,
        nombre: (j['nombre'] ?? '') as String,
      );
}

/// Una reserva: una barra sobre la fila de su recurso.
class BarraDeReserva {
  const BarraDeReserva({
    required this.id,
    required this.recursoId,
    required this.numero,
    required this.estado,
    required this.desde,
    required this.hasta,
  });

  final String id;
  final String recursoId;
  final String numero;
  final EstadoDeReserva estado;
  final DateTime desde;
  final DateTime hasta;

  factory BarraDeReserva.desdeJson(Map<String, dynamic> j) => BarraDeReserva(
        id: j['id'] as String,
        recursoId: j['recursoId'] as String,
        numero: (j['numero'] ?? '') as String,
        estado: EstadoDeReserva.desde(j['estado'] as String?),
        desde: DateTime.parse(j['desde'] as String).toUtc(),
        hasta: DateTime.parse(j['hasta'] as String).toUtc(),
      );
}

/// Un bloqueo: una barra distinta (roja) sobre la fila de su recurso (criterio 4).
class BarraDeBloqueo {
  const BarraDeBloqueo({
    required this.recursoId,
    required this.motivo,
    required this.desde,
    required this.hasta,
  });

  final String recursoId;
  final String motivo;
  final DateTime desde;
  final DateTime hasta;

  factory BarraDeBloqueo.desdeJson(Map<String, dynamic> j) => BarraDeBloqueo(
        recursoId: j['recursoId'] as String,
        motivo: (j['motivo'] ?? 'OTRO') as String,
        desde: DateTime.parse(j['desde'] as String).toUtc(),
        hasta: DateTime.parse(j['hasta'] as String).toUtc(),
      );
}

/// La ocupación de todos los recursos en una franja de días (HU-075).
class OcupacionDelCalendario {
  const OcupacionDelCalendario({
    required this.desde,
    required this.hasta,
    required this.dias,
    required this.recursos,
    required this.reservas,
    required this.bloqueos,
  });

  final DateTime desde;
  final DateTime hasta;
  final List<DateTime> dias;
  final List<RecursoDelCalendario> recursos;
  final List<BarraDeReserva> reservas;
  final List<BarraDeBloqueo> bloqueos;

  List<BarraDeReserva> reservasDe(String recursoId) =>
      reservas.where((b) => b.recursoId == recursoId).toList(growable: false);

  List<BarraDeBloqueo> bloqueosDe(String recursoId) =>
      bloqueos.where((b) => b.recursoId == recursoId).toList(growable: false);

  factory OcupacionDelCalendario.desdeJson(Map<String, dynamic> j) {
    List<T> lista<T>(String clave, T Function(Map<String, dynamic>) f) =>
        ((j[clave] ?? const []) as List)
            .map((e) => f((e as Map).cast<String, dynamic>()))
            .toList(growable: false);
    return OcupacionDelCalendario(
      desde: DateTime.parse(j['desde'] as String),
      hasta: DateTime.parse(j['hasta'] as String),
      dias: ((j['dias'] ?? const []) as List)
          .map((e) => DateTime.parse(e as String))
          .toList(growable: false),
      recursos: lista('recursos', RecursoDelCalendario.desdeJson),
      reservas: lista('reservas', BarraDeReserva.desdeJson),
      bloqueos: lista('bloqueos', BarraDeBloqueo.desdeJson),
    );
  }
}
