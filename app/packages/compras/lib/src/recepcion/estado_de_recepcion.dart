import '../datos/orden_recibible.dart';
import 'entrada_de_linea.dart';
import 'factura_de_recepcion.dart';
import 'precio_actual.dart';

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
    this.factura = const FacturaDeRecepcion(),
    this.precios = const {},
    this.preciosNuevos = const {},
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

  /// HU-175 · La factura con que llega (número, flete, contado o crédito).
  final FacturaDeRecepcion factura;

  /// HU-179 · Costo y precio actuales por producto (productoId → precio actual).
  final Map<String, PrecioActual> precios;

  /// HU-179 · El precio de venta nuevo que se escribió (productoId → precio con IVA).
  final Map<String, num> preciosNuevos;

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
    return factura.problema;
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
    FacturaDeRecepcion? factura,
    Map<String, PrecioActual>? precios,
    Map<String, num>? preciosNuevos,
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
        factura: factura ?? this.factura,
        precios: precios ?? this.precios,
        preciosNuevos: preciosNuevos ?? this.preciosNuevos,
      );

  static const _sinCambio = Object();
}
