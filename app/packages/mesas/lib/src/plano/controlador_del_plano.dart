import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/mesa_en_plano.dart';
import '../datos/plano_del_salon.dart';
import '../datos/repositorio_de_mesas.dart';
import 'estado_del_plano.dart';

/// Maneja el plano del salón (HU-081): lo carga, filtra por zona sin volver a
/// pedir, y en modo edición crea, mueve y borra mesas.
class ControladorDelPlano extends StateNotifier<EstadoDelPlano> {
  ControladorDelPlano(this._repo) : super(const EstadoDelPlano()) {
    cargar();
  }

  final RepositorioDeMesas _repo;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, errorAlCargar: false, mensaje: null);
    try {
      final plano = await _repo.plano();
      state = state.copiar(plano: plano, cargando: false);
    } on ErrorDeApi catch (e) {
      state = state.copiar(cargando: false, errorAlCargar: true, mensaje: e.mensaje);
    }
  }

  /// R3: filtra en memoria. `null` = todas, `''` = sin zona.
  void filtrarPorZona(String? zonaId) {
    state = state.copiar(zonaFiltro: zonaId);
  }

  void alternarEdicion() {
    state = state.copiar(modoEdicion: !state.modoEdicion, mensaje: null);
  }

  void limpiarMensaje() {
    state = state.copiar(mensaje: null);
  }

  /// R4 / criterio 1: crea una mesa. Devuelve `null` si salió bien, o el mensaje
  /// del error (409 de código repetido, criterio 2) para que el formulario lo
  /// muestre sin cerrarse.
  Future<String?> crearMesa({
    required String codigo,
    String? zonaId,
    String? nombre,
    int? capacidad,
    String? forma,
  }) async {
    try {
      await _repo.crearMesa(
        codigo: codigo,
        zonaId: zonaId,
        nombre: nombre,
        capacidad: capacidad,
        forma: forma,
      );
      await cargar();
      return null;
    } on ErrorDeApi catch (e) {
      return e.mensaje;
    }
  }

  /// R5 / criterio 3: guarda la nueva posición. Aplica el cambio en local al
  /// vuelo y confirma contra el backend.
  Future<void> moverMesa(String mesaId, int posX, int posY) async {
    _aplicarPosicionLocal(mesaId, posX, posY);
    try {
      await _repo.moverMesa(mesaId, posX < 0 ? 0 : posX, posY < 0 ? 0 : posY);
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
      await cargar();
    }
  }

  /// Criterio 4: borra una mesa. En 409 (sesión abierta) deja el mensaje.
  Future<bool> eliminarMesa(String mesaId) async {
    try {
      await _repo.eliminarMesa(mesaId);
      await cargar();
      return true;
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
      return false;
    }
  }

  void _aplicarPosicionLocal(String mesaId, int posX, int posY) {
    MesaEnPlano mapear(MesaEnPlano m) =>
        m.id == mesaId ? m.conPosicion(posX, posY) : m;
    final plano = state.plano;
    state = state.copiar(
      plano: PlanoDelSalon(
        zonas: [
          for (final z in plano.zonas)
            ZonaConMesas(
              id: z.id,
              nombre: z.nombre,
              orden: z.orden,
              color: z.color,
              mesas: z.mesas.map(mapear).toList(),
            ),
        ],
        sinZona: plano.sinZona.map(mapear).toList(),
      ),
    );
  }
}
