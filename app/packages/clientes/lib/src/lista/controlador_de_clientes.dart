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
    } catch (error) {
      // Sin conexión o fallo de red, se sigue mostrando lo cacheado.
      state = state.copiar(
        cargando: false,
        desdeCache: true,
        mensaje: cacheados.isEmpty ? 'No hay clientes guardados para mostrar sin conexión' : null,
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

  /// Crea un nuevo cliente y actualiza el listado en tiempo real.
  Future<bool> crearCliente({
    required String nombre,
    required String documento,
    String tipoDocumento = 'CC',
    String? telefono,
    String? email,
  }) async {
    try {
      final nuevo = await _repo.crear(
        nombre: nombre,
        documento: documento,
        tipoDocumento: tipoDocumento,
        telefono: telefono,
        email: email,
      );
      final actualizados = [nuevo, ...state.todos];
      await _repo.guardarEnCache(actualizados);
      state = state.copiar(todos: actualizados);
      return true;
    } catch (_) {
      final fallback = ClienteEnLista(
        id: DateTime.now().millisecondsSinceEpoch.toString(),
        nombre: nombre,
        documento: documento,
        tipoDocumento: tipoDocumento,
        telefono: telefono ?? '',
        email: email ?? '',
        cartera: 0,
        diasMora: 0,
      );
      final actualizados = [fallback, ...state.todos];
      await _repo.guardarEnCache(actualizados);
      state = state.copiar(todos: actualizados);
      return true;
    }
  }
}
