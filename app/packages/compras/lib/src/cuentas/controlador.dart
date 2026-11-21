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
  });

  final List<CuentaPorPagarVista> todas;
  final FiltroDeCuentas filtro;
  final bool cargando;
  final String? mensaje;

  List<CuentaPorPagarVista> get visibles => todas.where(filtro.admite).toList();
  num get saldoVisible => visibles.fold<num>(0, (s, c) => s + c.saldo);
  int get enEspera => todas.where((c) => c.enEsperaDeFactura).length;

  EstadoDeCuentas copiar({
    List<CuentaPorPagarVista>? todas,
    FiltroDeCuentas? filtro,
    bool? cargando,
    Object? mensaje = _sin,
  }) =>
      EstadoDeCuentas(
        todas: todas ?? this.todas,
        filtro: filtro ?? this.filtro,
        cargando: cargando ?? this.cargando,
        mensaje: identical(mensaje, _sin) ? this.mensaje : mensaje as String?,
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
      state = state.copiar(todas: await _repo.listar(), cargando: false);
    } on ErrorDeRed {
      state = state.copiar(cargando: false, mensaje: 'No hay conexión: no se pudieron cargar las cuentas.');
    } on ErrorDeApi catch (fallo) {
      state = state.copiar(cargando: false, mensaje: fallo.mensaje);
    }
  }

  void cambiarFiltro(FiltroDeCuentas filtro) => state = state.copiar(filtro: filtro);

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
