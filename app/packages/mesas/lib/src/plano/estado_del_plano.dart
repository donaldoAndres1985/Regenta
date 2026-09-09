import '../datos/mesa_en_plano.dart';
import '../datos/plano_del_salon.dart';

/// El estado del plano del salón (HU-081). Inmutable: cada cambio hace uno nuevo.
class EstadoDelPlano {
  const EstadoDelPlano({
    this.plano = PlanoDelSalon.vacio,
    this.zonaFiltro,
    this.cargando = false,
    this.modoEdicion = false,
    this.mensaje,
    this.errorAlCargar = false,
  });

  final PlanoDelSalon plano;

  /// `null` = «Todas». Si no, el id de la zona elegida (o `''` para «Sin zona»).
  final String? zonaFiltro;
  final bool cargando;
  final bool modoEdicion;

  /// Aviso puntual (un 409, por ejemplo). Lo pinta la pantalla y se limpia.
  final String? mensaje;
  final bool errorAlCargar;

  /// Las mesas que se ven con el filtro de zona puesto (R3).
  List<MesaEnPlano> get mesasVisibles {
    if (zonaFiltro == null) return plano.todas;
    if (zonaFiltro!.isEmpty) return plano.sinZona;
    return plano.todas.where((m) => m.zonaId == zonaFiltro).toList();
  }

  /// Los ids de zona presentes, en orden, para los chips.
  List<({String id, String nombre})> get zonasParaChips =>
      [for (final z in plano.zonas) (id: z.id, nombre: z.nombre)];

  EstadoDelPlano copiar({
    PlanoDelSalon? plano,
    Object? zonaFiltro = _sinCambio,
    bool? cargando,
    bool? modoEdicion,
    Object? mensaje = _sinCambio,
    bool? errorAlCargar,
  }) {
    return EstadoDelPlano(
      plano: plano ?? this.plano,
      zonaFiltro:
          identical(zonaFiltro, _sinCambio) ? this.zonaFiltro : zonaFiltro as String?,
      cargando: cargando ?? this.cargando,
      modoEdicion: modoEdicion ?? this.modoEdicion,
      mensaje: identical(mensaje, _sinCambio) ? this.mensaje : mensaje as String?,
      errorAlCargar: errorAlCargar ?? this.errorAlCargar,
    );
  }

  static const _sinCambio = Object();
}
