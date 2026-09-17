import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_comandas.dart';
import '../datos/repositorio_de_cuentas.dart';
import 'estado_de_division.dart';

/// Maneja «Dividir cuenta» (HU-089): crea cuentas, marca o desmarca líneas en
/// ellas —el reparto de una compartida lo calcula el backend, criterio 2—, y
/// cobra cada una. Al pagarse la última, la comanda vuelve `CERRADA`.
class ControladorDeDivision extends StateNotifier<EstadoDeDivision> {
  ControladorDeDivision(this._comandas, this._cuentas, this._comandaId) : super(const EstadoDeDivision()) {
    cargar();
  }

  final RepositorioDeComandas _comandas;
  final RepositorioDeCuentas _cuentas;
  final String _comandaId;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, errorAlCargar: false, mensaje: null);
    try {
      final comanda = await _comandas.comanda(_comandaId);
      final cuentas = await _cuentas.listar(_comandaId);
      state = state.copiar(comanda: comanda, cuentas: cuentas, cargando: false);
    } on ErrorDeApi catch (e) {
      state = state.copiar(cargando: false, errorAlCargar: true, mensaje: e.mensaje);
    }
  }

  void limpiarMensaje() => state = state.copiar(mensaje: null);

  Future<void> crearCuenta({String? etiqueta}) async {
    try {
      await _cuentas.crear(_comandaId, etiqueta: etiqueta);
      state = state.copiar(cuentas: await _cuentas.listar(_comandaId));
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
    }
  }

  /// Criterios 1 y 2: si [lineaId] ya está en [cuentaId] la quita; si no, la
  /// marca (y si estaba en otras cuentas, el reparto se recalcula entre todas).
  Future<void> alternarLinea(String cuentaId, String lineaId) async {
    final cuenta = state.cuentas.where((c) => c.id == cuentaId).firstOrDefault;
    if (cuenta == null) return;
    try {
      final tras = cuenta.proporcionDe(lineaId) != null
          ? await _cuentas.desmarcarLinea(_comandaId, cuentaId, lineaId)
          : await _cuentas.marcarLinea(_comandaId, cuentaId, lineaId);
      state = state.copiar(cuentas: tras);
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
    }
  }

  /// Criterio 3: reparte el total en [numeroPartes] cuentas iguales.
  Future<void> dividirEnPartesIguales(int numeroPartes) async {
    try {
      state = state.copiar(cuentas: await _cuentas.dividirEnPartesIguales(_comandaId, numeroPartes));
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
    }
  }

  /// Criterios 4 y 5: cobra la cuenta; si era la última abierta, la comanda se
  /// cierra sola (se refleja recargando la comanda).
  Future<void> cobrar(String cuentaId) async {
    try {
      final actualizada = await _cuentas.marcarPagada(_comandaId, cuentaId);
      state = state.copiar(
        cuentas: [for (final c in state.cuentas) c.id == cuentaId ? actualizada : c],
      );
      if (state.todasPagadas) {
        state = state.copiar(comanda: await _comandas.comanda(_comandaId));
      }
    } on ErrorDeApi catch (e) {
      state = state.copiar(mensaje: e.mensaje);
    }
  }
}

extension _PrimeroONulo<T> on Iterable<T> {
  T? get firstOrDefault {
    final it = iterator;
    return it.moveNext() ? it.current : null;
  }
}
