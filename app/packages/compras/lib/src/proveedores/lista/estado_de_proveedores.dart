import '../datos/proveedor_del_negocio.dart';

/// Los filtros del listado de proveedores. Se aplican en memoria: la lista responde al instante.
enum FiltroDeProveedores {
  activos('Activos'),
  conDeuda('Con deuda'),
  inactivos('Inactivos'),
  todos('Todos');

  const FiltroDeProveedores(this.etiqueta);

  final String etiqueta;

  bool admite(ProveedorDelNegocio p) => switch (this) {
        FiltroDeProveedores.activos => p.activo,
        FiltroDeProveedores.conDeuda => p.tieneDeuda,
        FiltroDeProveedores.inactivos => !p.activo,
        FiltroDeProveedores.todos => true,
      };
}

/// El estado del listado de proveedores. Inmutable: cada cambio produce uno nuevo.
class EstadoDeProveedores {
  const EstadoDeProveedores({
    this.todos = const [],
    this.termino = '',
    this.filtro = FiltroDeProveedores.activos,
    this.cargando = false,
    this.mensaje,
  });

  final List<ProveedorDelNegocio> todos;
  final String termino;
  final FiltroDeProveedores filtro;
  final bool cargando;

  /// El motivo real si no se pudo cargar (sin conexión, sin permiso…).
  final String? mensaje;

  List<ProveedorDelNegocio> get visibles =>
      todos.where((p) => p.coincideCon(termino) && filtro.admite(p)).toList();

  /// Lo que se le debe a los proveedores que se están viendo.
  num get deudaVisible => visibles.fold<num>(0, (suma, p) => suma + p.deuda);

  EstadoDeProveedores copiar({
    List<ProveedorDelNegocio>? todos,
    String? termino,
    FiltroDeProveedores? filtro,
    bool? cargando,
    Object? mensaje = _sinCambio,
  }) {
    return EstadoDeProveedores(
      todos: todos ?? this.todos,
      termino: termino ?? this.termino,
      filtro: filtro ?? this.filtro,
      cargando: cargando ?? this.cargando,
      mensaje: identical(mensaje, _sinCambio) ? this.mensaje : mensaje as String?,
    );
  }

  static const _sinCambio = Object();
}
