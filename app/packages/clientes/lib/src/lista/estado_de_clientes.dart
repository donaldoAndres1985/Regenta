import '../datos/cliente_en_lista.dart';
import 'filtro_de_clientes.dart';

/// El estado del listado de clientes. Inmutable: cada cambio produce uno nuevo.
class EstadoDeClientes {
  const EstadoDeClientes({
    this.todos = const [],
    this.termino = '',
    this.filtro = FiltroDeClientes.todos,
    this.cargando = false,
    this.desdeCache = false,
    this.mensaje,
  });

  /// Todo lo cargado (de red o de la copia local). El filtrado es en memoria.
  final List<ClienteEnLista> todos;
  final String termino;
  final FiltroDeClientes filtro;
  final bool cargando;

  /// Lo que se ve viene de la copia local: no se pudo llegar al servidor.
  final bool desdeCache;
  final String? mensaje;

  /// Lo que la pantalla pinta: texto (criterio 1) y filtro (criterio 3) a la vez.
  List<ClienteEnLista> get visibles => todos
      .where((c) => c.coincideCon(termino) && filtro.admite(c))
      .toList();

  int get totalVisible => visibles.length;

  EstadoDeClientes copiar({
    List<ClienteEnLista>? todos,
    String? termino,
    FiltroDeClientes? filtro,
    bool? cargando,
    bool? desdeCache,
    Object? mensaje = _sinCambio,
  }) {
    return EstadoDeClientes(
      todos: todos ?? this.todos,
      termino: termino ?? this.termino,
      filtro: filtro ?? this.filtro,
      cargando: cargando ?? this.cargando,
      desdeCache: desdeCache ?? this.desdeCache,
      mensaje: identical(mensaje, _sinCambio) ? this.mensaje : mensaje as String?,
    );
  }

  static const _sinCambio = Object();
}
