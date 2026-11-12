import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/ficha_de_cliente.dart';
import '../datos/repositorio_de_clientes.dart';
import 'estado_de_clientes.dart';
import 'filtro_de_clientes.dart';

/// Maneja el listado de clientes: primero la copia local (para que la pantalla
/// tenga algo al instante y funcione sin señal), y en cuanto llega la red, la
/// versión fresca. El texto y los filtros nunca vuelven a pedir nada.
class ControladorDeClientes extends StateNotifier<EstadoDeClientes> {
  ControladorDeClientes(this._repo) : super(const EstadoDeClientes()) {
    cargar();
  }

  final RepositorioDeClientes _repo;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, mensaje: null);

    final cacheados = await _repo.enCache();
    if (cacheados.isNotEmpty) {
      state = state.copiar(todos: cacheados, desdeCache: true, cargando: false);
    }

    try {
      final frescos = await _repo.enRed();
      await _repo.guardarEnCache(frescos);
      state = state.copiar(todos: frescos, desdeCache: false, cargando: false);
    } on ErrorDeRed {
      // Criterio 2: sin conexión, se sigue mostrando lo cacheado con aviso.
      state = state.copiar(
        cargando: false,
        desdeCache: true,
        mensaje: 'Estás trabajando sin conexión. Se muestran los clientes guardados en este equipo.',
      );
    } on ErrorDeApi catch (fallo) {
      // Cualquier otro fallo (permiso, sesión, servidor) no es "sin conexión":
      // decirlo así ocultaba el motivo real y parecía que nunca había clientes.
      state = state.copiar(cargando: false, desdeCache: true, mensaje: fallo.mensaje);
    } catch (_) {
      // Una respuesta que no se pudo leer no puede dejar la pantalla cargando
      // para siempre.
      state = state.copiar(
        cargando: false,
        mensaje: 'No se pudieron leer los clientes. Intenta de nuevo.',
      );
    }
  }

  /// Criterio 1: filtra sin recargar.
  void cambiarTermino(String termino) {
    state = state.copiar(termino: termino);
  }

  /// Criterio 3: aplica el filtro en memoria.
  void cambiarFiltro(FiltroDeClientes filtro) {
    state = state.copiar(filtro: filtro);
  }

  /// La ficha completa para editar. Devuelve la ficha, o el motivo por el que
  /// no se pudo traer.
  Future<(FichaDeCliente?, String?)> abrir(String clienteId) async {
    try {
      return (await _repo.ver(clienteId), null);
    } on ErrorDeRed {
      return (null, 'No hay conexión para abrir la ficha del cliente.');
    } on ErrorDeApi catch (fallo) {
      return (null, fallo.mensaje);
    }
  }

  /// Crea o edita un cliente. Devuelve `null` si quedó guardado, o el motivo
  /// por el que el servidor no lo aceptó: lo que se ve en el listado es siempre
  /// lo que de verdad existe, nunca un cliente que solo vive en esta pantalla.
  Future<String?> guardar(FichaDeCliente ficha) async {
    try {
      final guardado = await _repo.guardar(ficha);
      final actualizados = [
        guardado,
        ...state.todos.where((c) => c.id != guardado.id),
      ]..sort((a, b) => a.nombre.toLowerCase().compareTo(b.nombre.toLowerCase()));
      state = state.copiar(todos: actualizados, desdeCache: false, mensaje: null);
      return null;
    } on ErroresDeValidacion catch (fallo) {
      for (final campo in fallo.porCampo.entries) {
        if (campo.value.isNotEmpty) return '${campo.key}: ${campo.value.first}';
      }
      return fallo.mensaje;
    } on ErrorDeRed {
      return 'No hay conexión: el cliente no se guardó. Intenta de nuevo.';
    } on ErrorDeApi catch (fallo) {
      return fallo.mensaje;
    }
  }
}
