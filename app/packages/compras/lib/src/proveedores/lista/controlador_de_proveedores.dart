import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/proveedor_del_negocio.dart';
import '../datos/repositorio_de_proveedores.dart';
import 'estado_de_proveedores.dart';

/// Maneja el listado de proveedores (HU-166). Lo que se ve es siempre lo que el servidor
/// aceptó: crear, editar, desactivar o reactivar devuelven `null` si salió bien, o el motivo por
/// el que no, para mostrarlo sin perder lo escrito.
class ControladorDeProveedores extends StateNotifier<EstadoDeProveedores> {
  ControladorDeProveedores(this._repo) : super(const EstadoDeProveedores()) {
    cargar();
  }

  final RepositorioDeProveedores _repo;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, mensaje: null);
    try {
      final todos = await _repo.listar();
      state = state.copiar(todos: todos, cargando: false);
    } on ErrorDeRed {
      state = state.copiar(
        cargando: false,
        mensaje: 'No hay conexión: no se pudieron cargar los proveedores.',
      );
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(cargando: false, mensaje: fallo.mensaje);
    } catch (_) {
      state = state.copiar(
        cargando: false,
        mensaje: 'No se pudieron leer los proveedores. Intenta de nuevo.',
      );
    }
  }

  void cambiarTermino(String termino) => state = state.copiar(termino: termino);

  void cambiarFiltro(FiltroDeProveedores filtro) => state = state.copiar(filtro: filtro);

  Future<String?> guardar(ProveedorDelNegocio proveedor) =>
      _intentar(() => _repo.guardar(proveedor));

  /// HU-166 C3: la pantalla pide confirmación antes de llamar aquí.
  Future<String?> desactivar(String proveedorId) => _intentar(() => _repo.desactivar(proveedorId));

  Future<String?> reactivar(String proveedorId) => _intentar(() => _repo.reactivar(proveedorId));

  Future<String?> _intentar(Future<ProveedorDelNegocio> Function() operacion) async {
    try {
      _reemplazar(await operacion());
      return null;
    } on ErroresDeValidacion catch (fallo) {
      for (final campo in fallo.porCampo.entries) {
        if (campo.value.isNotEmpty) return '${campo.key}: ${campo.value.first}';
      }
      return fallo.mensaje;
    } on ErrorDeRed {
      return 'No hay conexión: no se guardó nada. Intenta de nuevo.';
    } on ErrorDeApi catch (fallo) {
      return fallo.mensaje;
    }
  }

  void _reemplazar(ProveedorDelNegocio actualizado) {
    final todos = [
      actualizado,
      ...state.todos.where((p) => p.id != actualizado.id),
    ]..sort((a, b) => a.razonSocial.toLowerCase().compareTo(b.razonSocial.toLowerCase()));
    state = state.copiar(todos: todos, mensaje: null);
  }
}
