/// Severidad de una alerta. El orden es el de prioridad en el centro (HU-095).
enum SeveridadAlerta { critica, alta, media, baja }

/// Estado de una alerta en el centro.
enum EstadoAlerta { nueva, vista, enCurso, resuelta, descartada }

/// Una alerta como la muestra el centro de la app (HU-095). Cuelga del núcleo
/// para que cualquier módulo pueda mostrar la campana y el panel.
class AlertaVista {
  const AlertaVista({
    required this.id,
    required this.tipoCodigo,
    required this.severidad,
    required this.estado,
    required this.titulo,
    required this.mensaje,
    required this.generadaEn,
    this.rutaApp,
    this.entidadTipo,
    this.entidadId,
    this.resueltaPor,
  });

  final String id;
  final String tipoCodigo;
  final SeveridadAlerta severidad;
  final EstadoAlerta estado;
  final String titulo;
  final String mensaje;
  final DateTime generadaEn;
  final String? rutaApp;
  final String? entidadTipo;
  final String? entidadId;
  final String? resueltaPor;

  bool get esNueva => estado == EstadoAlerta.nueva;

  bool get estaPendiente =>
      estado == EstadoAlerta.nueva ||
      estado == EstadoAlerta.vista ||
      estado == EstadoAlerta.enCurso;

  /// Orden del criterio 1: nuevas primero, luego por severidad, luego recientes.
  int compararCon(AlertaVista otra) {
    final pend = _rangoEstado(estado).compareTo(_rangoEstado(otra.estado));
    if (pend != 0) return pend;
    final sev = severidad.index.compareTo(otra.severidad.index);
    if (sev != 0) return sev;
    return otra.generadaEn.compareTo(generadaEn);
  }

  static int _rangoEstado(EstadoAlerta e) => switch (e) {
        EstadoAlerta.nueva => 0,
        EstadoAlerta.vista => 1,
        EstadoAlerta.enCurso => 2,
        EstadoAlerta.resuelta => 3,
        EstadoAlerta.descartada => 4,
      };

  factory AlertaVista.desdeJson(Map<String, dynamic> json) => AlertaVista(
        id: json['id'] as String,
        tipoCodigo: (json['tipoCodigo'] ?? '') as String,
        severidad: _severidad((json['severidad'] ?? 'MEDIA') as String),
        estado: _estado((json['estado'] ?? 'NUEVA') as String),
        titulo: (json['titulo'] ?? '') as String,
        mensaje: (json['mensaje'] ?? '') as String,
        generadaEn: DateTime.tryParse((json['generadaEn'] ?? '') as String)?.toLocal() ??
            DateTime.now(),
        rutaApp: json['rutaApp'] as String?,
        entidadTipo: json['entidadTipo'] as String?,
        entidadId: json['entidadId'] as String?,
        resueltaPor: json['resueltaPor'] as String?,
      );

  AlertaVista comoResuelta(String porUsuario) => AlertaVista(
        id: id,
        tipoCodigo: tipoCodigo,
        severidad: severidad,
        estado: EstadoAlerta.resuelta,
        titulo: titulo,
        mensaje: mensaje,
        generadaEn: generadaEn,
        rutaApp: rutaApp,
        entidadTipo: entidadTipo,
        entidadId: entidadId,
        resueltaPor: porUsuario,
      );

  static SeveridadAlerta _severidad(String s) => switch (s.toUpperCase()) {
        'CRITICA' => SeveridadAlerta.critica,
        'ALTA' => SeveridadAlerta.alta,
        'BAJA' => SeveridadAlerta.baja,
        _ => SeveridadAlerta.media,
      };

  static EstadoAlerta _estado(String s) => switch (s.toUpperCase()) {
        'VISTA' => EstadoAlerta.vista,
        'EN_CURSO' => EstadoAlerta.enCurso,
        'RESUELTA' => EstadoAlerta.resuelta,
        'DESCARTADA' => EstadoAlerta.descartada,
        _ => EstadoAlerta.nueva,
      };
}
