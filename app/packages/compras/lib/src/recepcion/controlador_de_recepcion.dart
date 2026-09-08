import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_recepciones.dart';
import 'entrada_de_linea.dart';
import 'estado_de_recepcion.dart';

/// La recepción móvil de una orden (HU-051). Carga la orden, deja capturar por
/// línea lo recibido —y el lote donde la categoría lo exige (criterio 2)—,
/// salta a la línea de un código escaneado (criterio 3) y registra la
/// recepción; sin señal, la deja encolada para subir después (criterio 4).
class ControladorDeRecepcion extends StateNotifier<EstadoDeRecepcion> {
  ControladorDeRecepcion(this._repo) : super(const EstadoDeRecepcion());

  final RepositorioDeRecepciones _repo;

  Future<void> cargarOrden(String ordenId) async {
    state = state.copiar(cargando: true, mensaje: null);
    try {
      final orden = await _repo.verOrden(ordenId);
      state = EstadoDeRecepcion(
        orden: orden,
        entradas: {
          for (final l in orden.lineas)
            l.id: EntradaDeLinea(costo: l.costoUnitario),
        },
      );
    } on ErrorDeApi catch (e) {
      state = state.copiar(cargando: false, mensaje: e.mensaje);
    }
  }

  void irALinea(int indice) {
    if (indice < 0 || indice >= state.lineas.length) return;
    state = state.copiar(indiceActual: indice, mensaje: null);
  }

  void siguiente() => irALinea(state.indiceActual + 1);
  void anterior() => irALinea(state.indiceActual - 1);

  /// Criterio 3: salta a la línea cuyo código (o nombre) coincide con lo leído.
  void saltarACodigo(String codigo) {
    final buscado = codigo.trim().toLowerCase();
    if (buscado.isEmpty) return;
    final indice = state.lineas.indexWhere((l) =>
        (l.codigo ?? '').toLowerCase() == buscado ||
        l.productoId.toLowerCase() == buscado ||
        l.nombre.toLowerCase().contains(buscado));
    if (indice < 0) {
      state = state.copiar(mensaje: 'Ningún renglón tiene el código «${codigo.trim()}»');
      return;
    }
    state = state.copiar(indiceActual: indice, mensaje: null);
  }

  void fijarRecibido(String lineaId, num? cantidad) {
    _actualizar(lineaId, (e) => e.copiar(recibido: cantidad));
  }

  void fijarLote(String lineaId, String lote) {
    _actualizar(lineaId, (e) => e.copiar(lote: lote));
  }

  void fijarVencimiento(String lineaId, DateTime? fecha) {
    _actualizar(lineaId, (e) => e.copiar(vencimiento: fecha));
  }

  void _actualizar(String lineaId, EntradaDeLinea Function(EntradaDeLinea) cambio) {
    final entradas = {...state.entradas};
    entradas[lineaId] = cambio(state.entradaDe(lineaId));
    state = state.copiar(entradas: entradas, mensaje: null);
  }

  Future<void> confirmar() async {
    final orden = state.orden;
    if (orden == null || !state.puedeConfirmar) {
      final problema = state.primerProblema;
      if (problema != null) state = state.copiar(mensaje: problema);
      return;
    }
    state = state.copiar(guardando: true, mensaje: null);
    final lineas = [
      for (final l in state.lineasConEntrada)
        LineaParaRecibir(
          ordenLineaId: l.id,
          cantidad: state.entradaDe(l.id).recibido!,
          exigeLote: l.exigeLote,
          costoUnitario: state.entradaDe(l.id).costo ?? l.costoUnitario,
          codigoLote: l.exigeLote ? state.entradaDe(l.id).lote.trim() : null,
          fechaVencimiento: l.exigeLote ? state.entradaDe(l.id).vencimiento : null,
        ),
    ];
    try {
      final resultado = await _repo.registrar(
        ordenId: orden.id,
        bodegaId: orden.bodegaId,
        lineas: lineas,
      );
      if (resultado.encolada) {
        state = state.copiar(
          guardando: false,
          encolada: true,
          mensaje: 'Sin señal: la recepción se guardó y subirá sola cuando vuelva.',
        );
      } else {
        state = state.copiar(
          guardando: false,
          numeroRecepcion: resultado.numero,
          mensaje: 'Recepción ${resultado.numero} registrada.',
        );
      }
    } on ErrorDeRed {
      state = state.copiar(
        guardando: false,
        encolada: true,
        mensaje: 'Sin señal: la recepción se guardó y subirá sola cuando vuelva.',
      );
    } on ErrorDeApi catch (e) {
      state = state.copiar(guardando: false, mensaje: e.mensaje);
    }
  }
}
