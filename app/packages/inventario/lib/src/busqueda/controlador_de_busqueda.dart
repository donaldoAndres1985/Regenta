import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_inventario.dart';
import 'estado_de_busqueda.dart';

/// Maneja la búsqueda de inventario: término (con rebote), filtro de categoría y
/// «bajo mínimo». Menos de tres caracteres no llama al backend: muestra todo
/// (HU-035, criterio 1).
class ControladorDeBusqueda extends StateNotifier<EstadoDeBusqueda> {
  ControladorDeBusqueda(this._repo, {Duration? rebote})
      : _rebote = rebote ?? const Duration(milliseconds: 300),
        super(const EstadoDeBusqueda()) {
    _buscar();
  }

  final RepositorioDeInventario _repo;
  final Duration _rebote;
  Timer? _temporizador;
  int _generacion = 0;

  static const minimoParaFiltrar = 3;

  void cambiarTermino(String termino) {
    state = state.copiar(termino: termino);
    _temporizador?.cancel();
    _temporizador = Timer(_rebote, _buscar);
  }

  void elegirCategoria(String? categoriaId) {
    state = state.copiar(categoriaId: categoriaId, soloBajoMinimo: false);
    _buscar();
  }

  void alternarBajoMinimo() {
    state = state.copiar(soloBajoMinimo: !state.soloBajoMinimo, categoriaId: null);
    _buscar();
  }

  void reintentar() => _buscar();

  Future<void> _buscar() async {
    final gen = ++_generacion;
    state = state.copiar(fase: FaseBusqueda.cargando, mensajeError: null);
    final termino = state.termino.trim();
    try {
      final resultado = await _repo.buscar(
        termino: termino.length >= minimoParaFiltrar ? termino : null,
        categoriaId: state.categoriaId,
        soloBajoMinimo: state.soloBajoMinimo,
      );
      if (gen != _generacion) return; // llegó una búsqueda más nueva
      state = state.copiar(
        resultados: resultado.productos,
        total: resultado.total,
        fase: resultado.productos.isEmpty
            ? FaseBusqueda.sinResultados
            : FaseBusqueda.conDatos,
      );
    } on ErrorDeApi catch (e) {
      if (gen != _generacion) return;
      state = state.copiar(fase: FaseBusqueda.error, mensajeError: _mensajeDe(e));
    }
  }

  String _mensajeDe(ErrorDeApi e) => switch (e) {
        ErrorDeRed() => 'Sin conexión. Revisa tu red e intenta de nuevo.',
        SinPermiso() => 'No tienes permiso para ver el inventario.',
        ErrorDelServidor() => 'El servidor tuvo un problema. Intenta en un momento.',
        _ => e.mensaje,
      };

  @override
  void dispose() {
    _temporizador?.cancel();
    super.dispose();
  }
}
