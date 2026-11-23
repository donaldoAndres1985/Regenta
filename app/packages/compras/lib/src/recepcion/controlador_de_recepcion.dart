import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_recepciones.dart';
import 'entrada_de_linea.dart';
import 'estado_de_recepcion.dart';
import 'factura_de_recepcion.dart';
import 'recepciones_por_enviar.dart';

/// La recepción móvil de una orden (HU-051). Carga la orden, deja capturar por
/// línea lo recibido —y el lote donde la categoría lo exige (criterio 2)—,
/// salta a la línea de un código escaneado (criterio 3) y registra la
/// recepción; sin señal, la deja encolada para subir después (criterio 4).
class ControladorDeRecepcion extends StateNotifier<EstadoDeRecepcion> {
  ControladorDeRecepcion(this._repo) : super(const EstadoDeRecepcion());

  final RepositorioDeRecepciones _repo;

  /// Abre la orden para recibir. HU-182 C3: con [correccion], vuelve lo que se había guardado en
  /// el celular (cantidades, lotes, factura, precios) y el motivo del rechazo, con el mismo
  /// identificador para que al reenviarla no quede doble.
  Future<void> cargarOrden(String ordenId, {RecepcionPorEnviar? correccion}) async {
    state = state.copiar(cargando: true, mensaje: null);
    try {
      final orden = await _repo.verOrden(ordenId);
      final guardadas = correccion?.lineasRecibidas ?? const <String, Map<String, dynamic>>{};
      state = EstadoDeRecepcion(
        orden: orden,
        entradas: {
          for (final l in orden.lineas)
            l.id: guardadas[l.id] == null
                ? EntradaDeLinea(costo: l.costoUnitario)
                : EntradaDeLinea(
                    recibido: guardadas[l.id]!['cantidad'] as num?,
                    costo: (guardadas[l.id]!['costoUnitario'] as num?) ?? l.costoUnitario,
                    lote: (guardadas[l.id]!['codigoLote'] ?? '') as String,
                    vencimiento: DateTime.tryParse((guardadas[l.id]!['fechaVencimiento'] ?? '') as String),
                  ),
        },
        factura: correccion?.factura ?? const FacturaDeRecepcion(),
        preciosNuevos: correccion?.preciosNuevos ?? const {},
        idDispositivo: correccion?.id,
        errorPrevio: correccion == null ? null : (correccion.error ?? 'El servidor la rechazó.'),
      );
      // HU-179 · El margen es una ayuda: si no se pueden traer los precios, se recibe igual.
      try {
        final precios = await _repo.preciosActuales(orden.bodegaId, orden.lineas.map((l) => l.productoId));
        state = state.copiar(precios: precios);
      } on ErrorDeApi {
        // sin margen
      }
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

  /// HU-179 · El precio de venta nuevo de un producto; nulo o cero, no cambia.
  void fijarPrecioNuevo(String productoId, num? precio) {
    final nuevos = {...state.preciosNuevos};
    if (precio == null || precio <= 0) {
      nuevos.remove(productoId);
    } else {
      nuevos[productoId] = precio;
    }
    state = state.copiar(preciosNuevos: nuevos, mensaje: null);
  }

  /// HU-175 · Los datos de la factura del proveedor.
  void fijarFactura(FacturaDeRecepcion factura) =>
      state = state.copiar(factura: factura, mensaje: null);

  Future<void> confirmar() async {
    final orden = state.orden;
    if (orden == null || !state.puedeConfirmar) {
      final problema = state.primerProblema;
      if (problema != null) state = state.copiar(mensaje: problema);
      return;
    }
    // HU-182 · I32: un solo identificador por recepción, aunque se confirme dos veces.
    final idDispositivo = state.idDispositivo ?? nuevoIdDeDispositivo();
    state = state.copiar(guardando: true, mensaje: null, idDispositivo: idDispositivo);
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
        factura: state.factura,
        lineas: lineas,
        preciosNuevos: {
          for (final e in state.preciosNuevos.entries)
            if (state.lineasConEntrada.any((l) => l.productoId == e.key)) e.key: e.value,
        },
        idDispositivo: idDispositivo,
        ordenNumero: orden.numero,
      );
      if (resultado.encolada) {
        state = state.copiar(
          guardando: false,
          encolada: true,
          mensaje: _guardadaSinSenal,
        );
      } else {
        state = state.copiar(
          guardando: false,
          numeroRecepcion: resultado.numero,
          errorPrevio: null,
          mensaje: 'Recepción ${resultado.numero} registrada.',
        );
      }
    } on ErrorDeRed {
      state = state.copiar(guardando: false, encolada: true, mensaje: _guardadaSinSenal);
    } on ErrorDeApi catch (e) {
      state = state.copiar(guardando: false, mensaje: motivoDelRechazo(e));
    }
  }

  static const _guardadaSinSenal =
      'Sin señal: la recepción se guardó y subirá sola cuando vuelva. Queda «por enviar» en Compras.';
}
