import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

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
    } on ErrorDeApi catch (error) {
      // Criterio 2: sin conexión, se sigue mostrando lo cacheado.
      state = state.copiar(
        cargando: false,
        desdeCache: true,
        mensaje: cacheados.isEmpty ? 'No hay clientes guardados para mostrar sin conexión' : null,
      );
      if (cacheados.isEmpty && error is! ErrorDeRed) rethrow;
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
}
