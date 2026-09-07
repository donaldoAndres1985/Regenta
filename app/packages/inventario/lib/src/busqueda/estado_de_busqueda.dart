import '../datos/producto_encontrado.dart';

/// En qué está la búsqueda: los cuatro estados son distintos y se pintan
/// distinto (`design/comportamiento/Inventario.md`).
enum FaseBusqueda { cargando, conDatos, sinResultados, error }

/// El estado completo de la pantalla de inventario.
class EstadoDeBusqueda {
  const EstadoDeBusqueda({
    this.termino = '',
    this.categoriaId,
    this.soloBajoMinimo = false,
    this.fase = FaseBusqueda.cargando,
    this.resultados = const [],
    this.total = 0,
    this.mensajeError,
  });

  final String termino;
  final String? categoriaId;
  final bool soloBajoMinimo;
  final FaseBusqueda fase;
  final List<ProductoEncontrado> resultados;
  final int total;
  final String? mensajeError;

  EstadoDeBusqueda copiar({
    String? termino,
    Object? categoriaId = _sinCambio,
    bool? soloBajoMinimo,
    FaseBusqueda? fase,
    List<ProductoEncontrado>? resultados,
    int? total,
    Object? mensajeError = _sinCambio,
  }) =>
      EstadoDeBusqueda(
        termino: termino ?? this.termino,
        categoriaId:
            categoriaId == _sinCambio ? this.categoriaId : categoriaId as String?,
        soloBajoMinimo: soloBajoMinimo ?? this.soloBajoMinimo,
        fase: fase ?? this.fase,
        resultados: resultados ?? this.resultados,
        total: total ?? this.total,
        mensajeError:
            mensajeError == _sinCambio ? this.mensajeError : mensajeError as String?,
      );

  static const _sinCambio = Object();
}
