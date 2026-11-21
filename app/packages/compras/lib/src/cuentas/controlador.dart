import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import 'datos.dart';

/// Los filtros del listado de cuentas por pagar. Se aplican en memoria.
enum FiltroDeCuentas {
  abiertas('Abiertas'),
  enEspera('En espera de factura'),
  vencidas('Vencidas'),
  todas('Todas');

  const FiltroDeCuentas(this.etiqueta);

  final String etiqueta;

  bool admite(CuentaPorPagarVista c) => switch (this) {
        FiltroDeCuentas.abiertas => !c.pagada && c.estado != 'ANULADA',
        FiltroDeCuentas.enEspera => c.enEsperaDeFactura,
        FiltroDeCuentas.vencidas => c.vencida,
        FiltroDeCuentas.todas => true,
      };
}

class EstadoDeCuentas {
  const EstadoDeCuentas({
    this.todas = const [],
    this.filtro = FiltroDeCuentas.abiertas,
    this.cargando = false,
    this.mensaje,
    this.cajaAbierta = false,
    this.saldosAFavor = const {},
  });

  final List<CuentaPorPagarVista> todas;
  final FiltroDeCuentas filtro;
  final bool cargando;
  final String? mensaje;
  final bool cajaAbierta;

  /// HU-171 C5 · Lo que cada proveedor le debe al negocio por notas crédito: id → saldo.
  final Map<String, num> saldosAFavor;

  /// HU-171 · Las cuentas abiertas de un proveedor, de la que vence primero a la última.
  List<CuentaPorPagarVista> abiertasDe(String proveedorId) =>
      todas.where((c) => c.proveedorId == proveedorId && c.abierta).toList()
        ..sort((a, b) => a.fechaVencimiento.compareTo(b.fechaVencimiento));

  /// Los proveedores a los que se les debe algo: id → nombre.
  Map<String, String> get proveedoresConDeuda => {
        for (final c in todas)
          if (c.abierta && c.proveedorId != null) c.proveedorId!: c.proveedorNombre ?? 'Proveedor',
      };

  List<CuentaPorPagarVista> get visibles => todas.where(filtro.admite).toList();
  num get saldoVisible => visibles.fold<num>(0, (s, c) => s + c.saldo);
  int get enEspera => todas.where((c) => c.enEsperaDeFactura).length;

  EstadoDeCuentas copiar({
    List<CuentaPorPagarVista>? todas,
    FiltroDeCuentas? filtro,
    bool? cargando,
    Object? mensaje = _sin,
    bool? cajaAbierta,
    Map<String, num>? saldosAFavor,
  }) =>
      EstadoDeCuentas(
        todas: todas ?? this.todas,
        filtro: filtro ?? this.filtro,
        cargando: cargando ?? this.cargando,
        mensaje: identical(mensaje, _sin) ? this.mensaje : mensaje as String?,
        cajaAbierta: cajaAbierta ?? this.cajaAbierta,
        saldosAFavor: saldosAFavor ?? this.saldosAFavor,
      );

  static const _sin = Object();
}

final repositorioDeCuentasPorPagarProvider = Provider<RepositorioDeCuentasPorPagar>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeCuentasPorPagarProvider al montar el ProviderScope.',
  );
});

final controladorDeCuentasProvider =
    StateNotifierProvider.autoDispose<ControladorDeCuentas, EstadoDeCuentas>(
  (ref) => ControladorDeCuentas(ref.watch(repositorioDeCuentasPorPagarProvider)),
);

/// Las cuentas por pagar del negocio (HU-170): lo que se debe, cuándo vence y cuáles esperan factura.
class ControladorDeCuentas extends StateNotifier<EstadoDeCuentas> {
  ControladorDeCuentas(this._repo) : super(const EstadoDeCuentas()) {
    cargar();
  }

  final RepositorioDeCuentasPorPagar _repo;

  Future<void> cargar() async {
    state = state.copiar(cargando: true, mensaje: null);
    try {
      final todas = await _repo.listar();
      final caja = await _repo.tengoCajaAbierta();
      final favor = await _repo.saldosAFavor();
      state = state.copiar(todas: todas, cajaAbierta: caja, saldosAFavor: favor, cargando: false);
    } on ErrorDeRed {
      state = state.copiar(cargando: false, mensaje: 'No hay conexión: no se pudieron cargar las cuentas.');
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(cargando: false, mensaje: fallo.mensaje);
    }
  }

  void cambiarFiltro(FiltroDeCuentas filtro) => state = state.copiar(filtro: filtro);

  /// HU-171 · Registra el pago y recarga las cuentas para ver cómo quedaron. Devuelve `null` si
  /// quedó registrado, o el motivo del rechazo.
  Future<String?> pagar({
    required String proveedorId,
    required num monto,
    required String metodo,
    String? referencia,
    bool desdeLaCaja = false,
    Map<String, num>? aplicaciones,
    bool usarSaldoAFavor = false,
  }) async {
    try {
      await _repo.pagar(
        usarSaldoAFavor: usarSaldoAFavor,
        proveedorId: proveedorId,
        monto: monto,
        metodo: metodo,
        referencia: referencia,
        desdeLaCaja: desdeLaCaja,
        aplicaciones: aplicaciones,
      );
    } on ErrorDeRed {
      return 'No hay conexión: el pago no se registró. Intenta de nuevo.';
    } on ErroresDeValidacion catch (fallo) {
      for (final campo in fallo.porCampo.entries) {
        if (campo.value.isNotEmpty) return '${campo.key}: ${campo.value.first}';
      }
      return fallo.mensaje;
    } on ErrorDeApi catch (fallo) {
      return fallo.mensaje;
    }
    await cargar();
    return null;
  }

  /// HU-172 · Los pagos de una cuenta, o `null` si no se pudieron traer.
  Future<List<PagoDeLaCuenta>?> pagosDe(String cuentaId) async {
    try {
      return await _repo.pagosDe(cuentaId);
    } on ErrorDeApi {
      return null;
    }
  }

  /// HU-172 · Anula el pago y recarga las cuentas. Devuelve `null` si quedó anulado, o el motivo.
  Future<String?> anularPago(String pagoId, String motivo) async {
    try {
      await _repo.anularPago(pagoId, motivo);
    } on ErrorDeRed {
      return 'No hay conexión: el pago no se anuló. Intenta de nuevo.';
    } on SinPermiso {
      return 'No tienes permiso para anular pagos a proveedores.';
    } on ErroresDeValidacion catch (fallo) {
      for (final campo in fallo.porCampo.entries) {
        if (campo.value.isNotEmpty) return '${campo.key}: ${campo.value.first}';
      }
      return fallo.mensaje;
    } on ErrorDeApi catch (fallo) {
      return fallo.mensaje;
    }
    await cargar();
    return null;
  }

  /// HU-170 C3 · Devuelve `null` si quedó registrada, o el motivo del rechazo.
  Future<String?> registrarFactura(String cuentaId, String numero, DateTime fecha) async {
    try {
      final actualizada = await _repo.registrarFactura(cuentaId, numero, fecha);
      state = state.copiar(
        todas: [for (final c in state.todas) c.id == cuentaId ? actualizada : c],
        mensaje: null,
      );
      return null;
    } on ErrorDeRed {
      return 'No hay conexión: la factura no se registró. Intenta de nuevo.';
    } on ErroresDeValidacion catch (fallo) {
      for (final campo in fallo.porCampo.entries) {
        if (campo.value.isNotEmpty) return '${campo.key}: ${campo.value.first}';
      }
      return fallo.mensaje;
    } on ErrorDeApi catch (fallo) {
      return fallo.mensaje;
    }
  }
}
