import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_ventas.dart';
import 'estado_del_cobro.dart';

/// Arma y confirma el cobro real de una venta (HU-037/038/039/040): crea el
/// borrador, le agrega las líneas que ya estaban en el carrito del POS, lo
/// confirma (lo que arranca la saga de stock de HU-038) y, según el método
/// elegido, registra el pago (o lo manda a crédito) y cierra la venta.
///
/// Solo se usa con conexión: si el primer paso falla por falta de red, quien
/// abre esta pantalla debe caer al camino `/offline` existente
/// (`ControladorDelPos.cobrar`), que es el seguro para vender sin señal.
class ControladorDeCobro extends StateNotifier<EstadoDelCobro> {
  ControladorDeCobro(
    this._repo, {
    required num total,
    required bool clienteAsignado,
    required String bodegaId,
    String? clienteId,
    String? notaCliente,
    required List<LineaParaEnviar> lineas,
  })  : _bodegaId = bodegaId,
        _clienteId = clienteId,
        _notaCliente = notaCliente,
        _lineas = lineas,
        super(EstadoDelCobro(total: total, clienteAsignado: clienteAsignado));

  final RepositorioDeVentas _repo;
  final String _bodegaId;
  final String? _clienteId;
  final String? _notaCliente;
  final List<LineaParaEnviar> _lineas;

  void agregarPago(LineaDePagoEnCurso pago) {
    state = state.copiar(pagos: [...state.pagos, pago], error: null);
  }

  void quitarPago(int indice) {
    if (indice < 0 || indice >= state.pagos.length) return;
    final pagos = [...state.pagos]..removeAt(indice);
    state = state.copiar(pagos: pagos, error: null);
  }

  /// Agrega un pago que cubre exactamente el saldo pendiente: el atajo de
  /// "pagar todo con este método" cuando no es un pago mixto.
  void pagarSaldoCon(String metodo, {num? montoRecibido, String? referencia, String? franquicia}) {
    final saldo = state.saldoPendiente;
    if (saldo <= 0) return;
    agregarPago(LineaDePagoEnCurso(
      metodo: metodo,
      monto: saldo,
      montoRecibido: metodo == 'EFECTIVO' ? (montoRecibido ?? saldo) : null,
      referencia: referencia,
      franquicia: franquicia,
    ));
  }

  /// HU-040: vende a crédito en vez de registrar pago. [autorizado] es el
  /// reintento deliberado cuando el cliente tiene cartera vencida
  /// (criterio 4) — nunca `true` en el primer intento.
  Future<void> confirmarACredito({bool autorizado = false}) async {
    if (!state.clienteAsignado) {
      state = state.copiar(error: 'Asigna un cliente para vender a crédito');
      return;
    }
    state = state.copiar(
      procesando: true,
      error: null,
      paso: 'Creando venta…',
      requiereAutorizacionCredito: false,
    );
    try {
      final ventaId = await _crearBorradorConLineas();
      state = state.copiar(paso: 'Confirmando crédito…');
      await _repo.confirmarACredito(ventaId, autorizado: autorizado);
      state = state.copiar(paso: 'Confirmando disponibilidad…');
      final venta = await _repo.esperarConfirmacion(ventaId);
      if (venta.confirmada) {
        state = state.copiar(procesando: false, paso: null, ventaNumero: venta.numero);
      } else {
        state = state.copiar(
          procesando: false,
          paso: null,
          error: 'No hay stock suficiente para completar la venta',
        );
      }
    } on RecursoDuplicado catch (e) {
      // 409: cartera vencida sin autorizar todavía (criterio 4).
      state = state.copiar(
        procesando: false,
        paso: null,
        error: e.mensaje,
        requiereAutorizacionCredito: true,
      );
    } on ErrorDeRed {
      state = state.copiar(procesando: false, paso: null, sinConexion: true);
    } on ErrorDeApi catch (e) {
      state = state.copiar(procesando: false, paso: null, error: e.mensaje);
    }
  }

  /// Pago normal (uno o varios medios). Exige que los pagos ya cubran el
  /// total antes de mandar nada (igual que exige el backend al cerrar).
  Future<void> confirmarCobro() async {
    if (!state.cubierto) {
      state = state.copiar(error: 'Completa el pago antes de confirmar');
      return;
    }
    state = state.copiar(procesando: true, error: null, paso: 'Creando venta…');
    try {
      final ventaId = await _crearBorradorConLineas();
      state = state.copiar(paso: 'Confirmando disponibilidad…');
      await _repo.confirmarBorrador(ventaId);
      final venta = await _repo.esperarConfirmacion(ventaId);
      if (!venta.confirmada) {
        state = state.copiar(
          procesando: false,
          paso: null,
          error: 'No hay stock suficiente para completar la venta',
        );
        return;
      }
      state = state.copiar(paso: 'Registrando pago…');
      for (final pago in state.pagos) {
        await _repo.registrarPago(
          ventaId,
          PagoParaEnviar(
            metodo: pago.metodo,
            monto: pago.metodo == 'EFECTIVO' ? null : pago.monto,
            montoRecibido: pago.metodo == 'EFECTIVO' ? pago.montoRecibido : null,
            referencia: pago.referencia,
            franquicia: pago.franquicia,
          ),
        );
      }
      state = state.copiar(paso: 'Cerrando venta…');
      await _repo.cerrarVenta(ventaId);
      state = state.copiar(procesando: false, paso: null, ventaNumero: venta.numero);
    } on ErrorDeRed {
      state = state.copiar(procesando: false, paso: null, sinConexion: true);
    } on ErrorDeApi catch (e) {
      state = state.copiar(procesando: false, paso: null, error: e.mensaje);
    }
  }

  Future<String> _crearBorradorConLineas() async {
    final venta = await _repo.crearBorrador(
      bodegaId: _bodegaId,
      clienteId: _clienteId,
      notaCliente: _notaCliente,
    );
    for (final linea in _lineas) {
      await _repo.agregarLineaAlBorrador(venta.id, linea);
    }
    return venta.id;
  }
}
