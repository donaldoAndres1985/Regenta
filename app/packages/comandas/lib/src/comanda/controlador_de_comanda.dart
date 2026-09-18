import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/comanda_vista.dart';
import '../datos/repositorio_de_comandas.dart';
import 'estado_de_comanda.dart';

const _avisoSinSenal = 'Sin señal: la línea se guardó y subirá sola cuando vuelva.';
const _avisoEnvioSinSenal = 'Sin señal: se envió a cocina localmente y subirá sola cuando vuelva.';

/// Maneja la pantalla de una comanda (HU-085): la carga, agrega líneas —que
/// nacen PENDIENTE aunque la comanda ya esté en cocina— y envía a cocina. Sin
/// señal, agregar y enviar quedan encolados y la pantalla sigue mostrando lo
/// que se tomó, con un aviso (HU-091 criterio 4).
class ControladorDeComanda extends StateNotifier<EstadoDeComanda> {
  ControladorDeComanda(this._repo, this._comandaId) : super(const EstadoDeComanda()) {
    cargar();
  }

  final RepositorioDeComandas _repo;
  final String _comandaId;
  int _correlativoLocal = 0;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, errorAlCargar: false, mensaje: null);
    try {
      state = state.copiar(comanda: await _repo.comanda(_comandaId), cargando: false);
    } on ErrorDeApi catch (e) {
      state = state.copiar(cargando: false, errorAlCargar: true, mensaje: e.mensaje);
    }
  }

  void limpiarMensaje() => state = state.copiar(mensaje: null);

  /// Criterio 1 / 5: agrega una línea. Devuelve `null` si entró (o quedó
  /// encolada), o el mensaje del error (422 de modificadores obligatorios)
  /// para que la hoja no se cierre. [nombreItem] y [precioItem] arman la
  /// línea local si toca encolar (HU-091 criterio 4).
  Future<String?> agregarLinea({
    required String itemMenuId,
    required num cantidad,
    List<String> modificadorIds = const [],
    String? notas,
    String? nombreItem,
    num? precioItem,
  }) async {
    final comandaActual = state.comanda;
    try {
      final resultado = await _repo.agregarLinea(
        _comandaId,
        itemMenuId: itemMenuId,
        cantidad: cantidad,
        modificadorIds: modificadorIds,
        notas: notas,
      );
      if (resultado.encolado && comandaActual != null) {
        state = state.copiar(
          comanda: _conLineaLocal(comandaActual, nombreItem ?? 'Ítem', precioItem ?? 0, cantidad, notas),
          mensaje: _avisoSinSenal,
        );
      } else if (resultado.comanda != null) {
        state = state.copiar(comanda: resultado.comanda);
      }
      return null;
    } on ErroresDeValidacion catch (e) {
      final campo = e.deCampo('modificadorIds') ?? e.deCampo('modificadores');
      return campo ?? 'Faltan modificadores obligatorios del ítem';
    } on ErrorDeRed {
      if (comandaActual == null) return 'Sin señal';
      state = state.copiar(
        comanda: _conLineaLocal(comandaActual, nombreItem ?? 'Ítem', precioItem ?? 0, cantidad, notas),
        mensaje: _avisoSinSenal,
      );
      return null;
    } on ErrorDeApi catch (e) {
      return e.mensaje;
    }
  }

  /// Criterio 5: envía a cocina, con confirmación previa ya resuelta por la
  /// pantalla. Sin señal, queda encolado (criterio 4) y las líneas pendientes
  /// se ven `ENVIADA` localmente.
  Future<void> enviarACocina() async {
    final comandaActual = state.comanda;
    try {
      final resultado = await _repo.enviarACocina(_comandaId);
      if (resultado.encolado && comandaActual != null) {
        state = state.copiar(comanda: _conEnvioLocal(comandaActual), mensaje: _avisoEnvioSinSenal);
      } else if (resultado.comanda != null) {
        state = state.copiar(comanda: resultado.comanda);
      }
    } on ErrorDeRed {
      if (comandaActual != null) {
        state = state.copiar(comanda: _conEnvioLocal(comandaActual), mensaje: _avisoEnvioSinSenal);
      }
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

  ComandaVista _conLineaLocal(
      ComandaVista comanda, String nombre, num precio, num cantidad, String? notas) {
    _correlativoLocal++;
    final nueva = LineaVista(
      id: 'local-$_correlativoLocal',
      linea: comanda.lineas.length + 1,
      nombre: nombre,
      estado: 'PENDIENTE',
      curso: 'FUERTE',
      secuenciaEnvio: 1,
      cantidad: cantidad,
      total: precio * cantidad,
      notas: notas,
      modificadores: const [],
    );
    final lineas = [...comanda.lineas, nueva];
    final total = lineas.fold<num>(0, (s, l) => s + l.total);
    return ComandaVista(
      id: comanda.id,
      numero: comanda.numero,
      estado: comanda.estado,
      numComensales: comanda.numComensales,
      subtotal: total,
      impuestoTotal: comanda.impuestoTotal,
      propinaSugerida: total * 0.1,
      total: total,
      mesaId: comanda.mesaId,
      lineas: lineas,
    );
  }

  ComandaVista _conEnvioLocal(ComandaVista comanda) {
    final lineas = [
      for (final l in comanda.lineas)
        l.estado == 'PENDIENTE'
            ? LineaVista(
                id: l.id,
                linea: l.linea,
                nombre: l.nombre,
                estado: 'ENVIADA',
                curso: l.curso,
                secuenciaEnvio: l.secuenciaEnvio,
                cantidad: l.cantidad,
                total: l.total,
                notas: l.notas,
                demoraMin: l.demoraMin,
                modificadores: l.modificadores,
              )
            : l,
    ];
    return ComandaVista(
      id: comanda.id,
      numero: comanda.numero,
      estado: 'EN_COCINA',
      numComensales: comanda.numComensales,
      subtotal: comanda.subtotal,
      impuestoTotal: comanda.impuestoTotal,
      propinaSugerida: comanda.propinaSugerida,
      total: comanda.total,
      mesaId: comanda.mesaId,
      lineas: lineas,
    );
  }
}
