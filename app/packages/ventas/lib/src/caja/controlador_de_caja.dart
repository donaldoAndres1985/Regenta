import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';
import 'datos/repositorio_de_caja.dart';
import 'datos/sesion_caja_modelos.dart';

final repositorioDeCajaProvider = Provider<RepositorioDeCaja>((ref) {
  final http = ref.watch(clienteHttpProvider);
  return RepositorioDeCaja(http);
});

class EstadoControladorCaja {
  const EstadoControladorCaja({
    this.cargando = false,
    this.cajas = const [],
    this.cajaSeleccionada,
    this.sesionActiva,
    this.movimientos = const [],
    this.error,
  });

  final bool cargando;
  final List<CajaVista> cajas;
  final CajaVista? cajaSeleccionada;
  final SesionCajaActiva? sesionActiva;
  final List<MovimientoCajaVista> movimientos;
  final String? error;

  bool get tieneSesionAbierta => sesionActiva != null && sesionActiva!.estaAbierta;

  num get totalIngresos {
    num total = 0;
    for (final m in movimientos) {
      if (m.signo > 0) total += m.monto;
    }
    return total;
  }

  num get totalRetiros {
    num total = 0;
    for (final m in movimientos) {
      if (m.signo < 0) total += m.monto;
    }
    return total;
  }

  num get efectivoEsperadoCalculado {
    if (sesionActiva == null) return 0;
    num suma = 0;
    for (final m in movimientos) {
      if (m.metodoPago.toUpperCase() == 'EFECTIVO') {
        suma += (m.monto * m.signo);
      }
    }
    return suma;
  }

  EstadoControladorCaja copyWith({
    bool? cargando,
    List<CajaVista>? cajas,
    CajaVista? Function()? cajaSeleccionada,
    SesionCajaActiva? Function()? sesionActiva,
    List<MovimientoCajaVista>? movimientos,
    String? Function()? error,
  }) {
    return EstadoControladorCaja(
      cargando: cargando ?? this.cargando,
      cajas: cajas ?? this.cajas,
      cajaSeleccionada:
          cajaSeleccionada != null ? cajaSeleccionada() : this.cajaSeleccionada,
      sesionActiva: sesionActiva != null ? sesionActiva() : this.sesionActiva,
      movimientos: movimientos ?? this.movimientos,
      error: error != null ? error() : this.error,
    );
  }
}

class ControladorDeCaja extends StateNotifier<EstadoControladorCaja> {
  ControladorDeCaja(this._repo) : super(const EstadoControladorCaja());

  final RepositorioDeCaja _repo;

  Future<void> inicializar() async {
    state = state.copyWith(cargando: true, error: () => null);
    try {
      final cajas = await _repo.listarCajas();
      CajaVista? seleccionada;
      if (cajas.isNotEmpty) {
        seleccionada = cajas.first;
      }
      state = state.copyWith(
        cajas: cajas,
        cajaSeleccionada: () => seleccionada,
      );

      if (seleccionada != null) {
        await cargarSesion(seleccionada.id);
      } else {
        state = state.copyWith(cargando: false);
      }
    } catch (e) {
      state = state.copyWith(
        cargando: false,
        error: () => 'No se pudo cargar la información de caja: $e',
      );
    }
  }

  Future<void> cambiarCaja(CajaVista caja) async {
    state = state.copyWith(cajaSeleccionada: () => caja);
    await cargarSesion(caja.id);
  }

  Future<void> cargarSesion(String cajaId) async {
    state = state.copyWith(cargando: true, error: () => null);
    try {
      final sesion = await _repo.consultarActiva(cajaId);
      List<MovimientoCajaVista> movs = const [];
      if (sesion != null) {
        movs = await _repo.movimientos(sesion.id);
      }
      state = state.copyWith(
        cargando: false,
        sesionActiva: () => sesion,
        movimientos: movs,
      );
    } catch (e) {
      state = state.copyWith(
        cargando: false,
        error: () => 'Error al consultar la sesión: $e',
      );
    }
  }

  Future<bool> abrirTurno(num montoApertura) async {
    final caja = state.cajaSeleccionada;
    if (caja == null) return false;

    state = state.copyWith(cargando: true, error: () => null);
    try {
      final nueva = await _repo.abrirSesion(
        cajaId: caja.id,
        montoApertura: montoApertura,
      );
      final movs = await _repo.movimientos(nueva.id);
      state = state.copyWith(
        cargando: false,
        sesionActiva: () => nueva,
        movimientos: movs,
      );
      return true;
    } catch (e) {
      state = state.copyWith(
        cargando: false,
        error: () => 'Error al abrir caja: $e',
      );
      return false;
    }
  }

  Future<bool> registrarIngreso(num monto, String concepto) async {
    final sesion = state.sesionActiva;
    if (sesion == null) return false;

    try {
      await _repo.registrarIngreso(
        sesionId: sesion.id,
        monto: monto,
        concepto: concepto,
      );
      final movs = await _repo.movimientos(sesion.id);
      state = state.copyWith(movimientos: movs);
      return true;
    } catch (e) {
      state = state.copyWith(error: () => 'Error al registrar ingreso: $e');
      return false;
    }
  }

  Future<bool> registrarRetiro({
    required num monto,
    required String concepto,
    required String tipo,
    String? autorizadoPor,
  }) async {
    final sesion = state.sesionActiva;
    if (sesion == null) return false;

    try {
      await _repo.registrarRetiro(
        sesionId: sesion.id,
        monto: monto,
        concepto: concepto,
        tipo: tipo,
        autorizadoPor: autorizadoPor,
      );
      final movs = await _repo.movimientos(sesion.id);
      state = state.copyWith(movimientos: movs);
      return true;
    } catch (e) {
      state = state.copyWith(error: () => 'Error al registrar retiro: $e');
      return false;
    }
  }

  Future<SesionCajaActiva?> cerrarTurno(num montoDeclarado) async {
    final sesion = state.sesionActiva;
    if (sesion == null) return null;

    state = state.copyWith(cargando: true, error: () => null);
    try {
      final cerrada = await _repo.cerrarSesion(
        sesionId: sesion.id,
        montoDeclarado: montoDeclarado,
      );
      state = state.copyWith(
        cargando: false,
        sesionActiva: () => null,
        movimientos: const [],
      );
      return cerrada;
    } catch (e) {
      state = state.copyWith(
        cargando: false,
        error: () => 'Error al cerrar caja: $e',
      );
      return null;
    }
  }
}

final controladorDeCajaProvider =
    StateNotifierProvider<ControladorDeCaja, EstadoControladorCaja>((ref) {
  final repo = ref.watch(repositorioDeCajaProvider);
  return ControladorDeCaja(repo);
});
