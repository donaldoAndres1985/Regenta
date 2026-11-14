import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'datos/repositorio_de_caja.dart';
import 'datos/sesion_caja_modelos.dart';
import 'estado_de_caja.dart';

class ControladorDeCaja extends StateNotifier<EstadoDeCaja> {
  ControladorDeCaja(this._repo) : super(const EstadoDeCaja());

  final RepositorioDeCaja _repo;

  Future<void> cargar() async {
    state = state.copyWith(cargando: true, clearError: true);
    try {
      final cajas = await _repo.listarCajas();
      final caja = state.cajaSeleccionada ?? (cajas.isNotEmpty ? cajas.first : null);
      if (caja != null) {
        final sesion = await _repo.consultarActiva(caja.id);
        List<MovimientoCajaVista> movs = const [];
        if (sesion != null) {
          movs = await _repo.movimientos(sesion.id);
        }
        state = state.copyWith(
          cargando: false,
          cajas: cajas,
          cajaSeleccionada: caja,
          sesionActiva: sesion,
          clearSesion: sesion == null,
          movimientos: movs,
        );
      } else {
        state = state.copyWith(
          cargando: false,
          cajas: cajas,
          clearSesion: true,
          movimientos: const [],
        );
      }
    } catch (e) {
      state = state.copyWith(cargando: false, error: e.toString());
    }
  }

  Future<void> seleccionarCaja(CajaVista caja) async {
    state = state.copyWith(cargando: true, cajaSeleccionada: caja, clearError: true);
    try {
      final sesion = await _repo.consultarActiva(caja.id);
      List<MovimientoCajaVista> movs = const [];
      if (sesion != null) {
        movs = await _repo.movimientos(sesion.id);
      }
      state = state.copyWith(
        cargando: false,
        sesionActiva: sesion,
        clearSesion: sesion == null,
        movimientos: movs,
      );
    } catch (e) {
      state = state.copyWith(cargando: false, error: e.toString());
    }
  }

  Future<void> abrirSesion(num montoApertura) async {
    final caja = state.cajaSeleccionada;
    if (caja == null) return;
    state = state.copyWith(cargando: true, clearError: true);
    try {
      final sesion = await _repo.abrirSesion(
        cajaId: caja.id,
        montoApertura: montoApertura,
      );
      final movs = await _repo.movimientos(sesion.id);
      state = state.copyWith(
        cargando: false,
        sesionActiva: sesion,
        movimientos: movs,
      );
    } catch (e) {
      state = state.copyWith(cargando: false, error: e.toString());
    }
  }

  Future<void> registrarIngreso(num monto, String concepto) async {
    final sesion = state.sesionActiva;
    if (sesion == null) return;
    state = state.copyWith(cargando: true, clearError: true);
    try {
      await _repo.registrarIngreso(
        sesionId: sesion.id,
        monto: monto,
        concepto: concepto,
      );
      final movs = await _repo.movimientos(sesion.id);
      state = state.copyWith(cargando: false, movimientos: movs);
    } catch (e) {
      state = state.copyWith(cargando: false, error: e.toString());
    }
  }

  Future<void> registrarRetiro(num monto, String concepto) async {
    final sesion = state.sesionActiva;
    if (sesion == null) return;
    state = state.copyWith(cargando: true, clearError: true);
    try {
      await _repo.registrarRetiro(
        sesionId: sesion.id,
        monto: monto,
        concepto: concepto,
      );
      final movs = await _repo.movimientos(sesion.id);
      state = state.copyWith(cargando: false, movimientos: movs);
    } catch (e) {
      state = state.copyWith(cargando: false, error: e.toString());
    }
  }

  /// HU-140 · el arqueo por método de la sesión abierta, para la pantalla de cierre.
  Future<List<LineaDeArqueoVista>> arqueoPorMetodo() async {
    final sesion = state.sesionActiva;
    if (sesion == null) return const [];
    try {
      return await _repo.arqueoPorMetodo(sesion.id);
    } catch (e) {
      state = state.copyWith(error: e.toString());
      return const [];
    }
  }

  Future<void> cerrarSesion(Map<String, num> contados) async {
    final sesion = state.sesionActiva;
    if (sesion == null) return;
    state = state.copyWith(cargando: true, clearError: true);
    try {
      await _repo.cerrarSesion(
        sesionId: sesion.id,
        contados: contados,
      );
      state = state.copyWith(
        cargando: false,
        clearSesion: true,
        movimientos: const [],
      );
    } catch (e) {
      state = state.copyWith(cargando: false, error: e.toString());
    }
  }
}
