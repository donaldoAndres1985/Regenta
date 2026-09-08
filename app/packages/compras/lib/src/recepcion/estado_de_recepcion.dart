import '../datos/orden_recibible.dart';
import 'entrada_de_linea.dart';

/// El estado de la pantalla de recepción móvil: la orden, lo capturado por
/// línea, en qué línea está el foco, y el resultado del registro.
class EstadoDeRecepcion {
  const EstadoDeRecepcion({
    this.orden,
    this.entradas = const {},
    this.indiceActual = 0,
    this.cargando = false,
    this.guardando = false,
    this.mensaje,
    this.numeroRecepcion,
    this.encolada = false,
  });

  final OrdenRecibible? orden;

  /// Lo capturado por línea, indexado por `LineaRecibible.id`.
  final Map<String, EntradaDeLinea> entradas;
  final int indiceActual;
  final bool cargando;
  final bool guardando;
  final String? mensaje;

  /// Número de la recepción, cuando el registro salió bien.
  final String? numeroRecepcion;

  /// La recepción se guardó sin señal y subirá después (criterio 4).
  final bool encolada;

  List<LineaRecibible> get lineas => orden?.lineas ?? const [];

  LineaRecibible? get lineaActual =>
      (indiceActual >= 0 && indiceActual < lineas.length)
          ? lineas[indiceActual]
          : null;

  EntradaDeLinea entradaDe(String lineaId) =>
      entradas[lineaId] ?? const EntradaDeLinea();

  /// Las líneas con algo recibido, listas para mandar.
  Iterable<LineaRecibible> get lineasConEntrada => lineas.where((l) {
        final e = entradas[l.id];
        return e != null && e.recibido != null && e.recibido! > 0;
      });

  bool get hayAlgoQueRecibir => lineasConEntrada.isNotEmpty;

  /// El primer problema de captura, o null si todo está bien.
  String? get primerProblema {
    for (final l in lineas) {
      final p = entradaDe(l.id).problema(l);
      if (p != null) return p;
    }
    return null;
  }

  bool get puedeConfirmar =>
      hayAlgoQueRecibir && primerProblema == null && !guardando;

  EstadoDeRecepcion copiar({
    Object? orden = _sinCambio,
    Map<String, EntradaDeLinea>? entradas,
    int? indiceActual,
    bool? cargando,
    bool? guardando,
    Object? mensaje = _sinCambio,
    Object? numeroRecepcion = _sinCambio,
    bool? encolada,
  }) =>
      EstadoDeRecepcion(
        orden: orden == _sinCambio ? this.orden : orden as OrdenRecibible?,
        entradas: entradas ?? this.entradas,
        indiceActual: indiceActual ?? this.indiceActual,
        cargando: cargando ?? this.cargando,
        guardando: guardando ?? this.guardando,
        mensaje: mensaje == _sinCambio ? this.mensaje : mensaje as String?,
        numeroRecepcion: numeroRecepcion == _sinCambio
            ? this.numeroRecepcion
            : numeroRecepcion as String?,
        encolada: encolada ?? this.encolada,
      );

  static const _sinCambio = Object();
}
