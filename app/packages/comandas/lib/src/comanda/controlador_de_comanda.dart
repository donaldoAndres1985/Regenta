import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_comandas.dart';
import 'estado_de_comanda.dart';

/// Maneja la pantalla de una comanda (HU-085): la carga, agrega líneas —que
/// nacen PENDIENTE aunque la comanda ya esté en cocina— y envía a cocina.
class ControladorDeComanda extends StateNotifier<EstadoDeComanda> {
  ControladorDeComanda(this._repo, this._comandaId) : super(const EstadoDeComanda()) {
    cargar();
  }

  final RepositorioDeComandas _repo;
  final String _comandaId;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, errorAlCargar: false, mensaje: null);
    try {
      state = state.copiar(comanda: await _repo.comanda(_comandaId), cargando: false);
    } on ErrorDeApi catch (e) {
      state = state.copiar(cargando: false, errorAlCargar: true, mensaje: e.mensaje);
    }
  }

  void limpiarMensaje() => state = state.copiar(mensaje: null);

  /// Criterio 1 / 5: agrega una línea. Devuelve `null` si entró, o el mensaje
  /// del error (422 de modificadores obligatorios) para que la hoja no se cierre.
  Future<String?> agregarLinea({
    required String itemMenuId,
    required num cantidad,
    List<String> modificadorIds = const [],
    String? notas,
  }) async {
    try {
      final comanda = await _repo.agregarLinea(
        _comandaId,
        itemMenuId: itemMenuId,
        cantidad: cantidad,
        modificadorIds: modificadorIds,
        notas: notas,
      );
      state = state.copiar(comanda: comanda);
      return null;
    } on ErroresDeValidacion catch (e) {
      final campo = e.deCampo('modificadorIds') ?? e.deCampo('modificadores');
      return campo ?? 'Faltan modificadores obligatorios del ítem';
    } on ErrorDeApi catch (e) {
      return e.mensaje;
    }
  }

  Future<void> enviarACocina() async {
    try {
      state = state.copiar(comanda: await _repo.enviarACocina(_comandaId));
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
    }
  }

  /// HU-086 criterio 1: avanza el estado de una línea.
  Future<void> avanzarLinea(String lineaId) async {
    try {
      state = state.copiar(comanda: await _repo.avanzarLinea(_comandaId, lineaId));
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
    }
  }
}
