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

  /// HU-141 · anula un movimiento manual y recarga la lista del turno.
  Future<void> anularMovimiento(String movimientoId, String motivo) async {
    final sesion = state.sesionActiva;
    if (sesion == null) return;
    state = state.copyWith(cargando: true, clearError: true);
    try {
      await _repo.anularMovimiento(
        sesionId: sesion.id,
        movimientoId: movimientoId,
        motivo: motivo,
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

  /// Cierra la sesión abierta. Devuelve el id de la sesión cerrada, o `null` si no se cerró.
  Future<String?> cerrarSesion(Map<String, num> contados) async {
    final sesion = state.sesionActiva;
    if (sesion == null) return null;
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
      return sesion.id;
    } catch (e) {
      state = state.copyWith(cargando: false, error: e.toString());
      return null;
    }
  }

  /// HU-142 · los datos de la tirilla de una sesión cerrada.
  Future<TirillaDeCierreVista> tirillaDeCierre(String sesionId) =>
      _repo.tirillaDeCierre(sesionId);
}
