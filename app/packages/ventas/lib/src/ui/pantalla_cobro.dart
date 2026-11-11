import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../cobro/estado_del_cobro.dart';
import '../cobro/proveedores.dart';
import '../datos/repositorio_de_ventas.dart';
import 'formato.dart';

/// Los métodos que ofrece la pantalla, en el orden en que se muestran.
const _metodos = <(String valor, String etiqueta, IconData icono)>[
  ('EFECTIVO', 'Efectivo', Icons.payments_outlined),
  ('TARJETA_DEBITO', 'Tarjeta débito', Icons.credit_card),
  ('TARJETA_CREDITO', 'Tarjeta crédito', Icons.credit_card),
  ('TRANSFERENCIA', 'Transferencia', Icons.account_balance_outlined),
  ('QR', 'QR', Icons.qr_code),
  ('BONO', 'Bono', Icons.card_giftcard_outlined),
];

String _etiquetaMetodo(String valor) =>
    _metodos.firstWhere((m) => m.$1 == valor, orElse: () => (valor, valor, Icons.payments)).$2;

/// La pantalla de Cobro (HU-039/040, `design/comportamiento/Cobro.md`): se
/// abre con el carrito ya congelado y arma el pago -único o mixto- o la
/// venta a crédito, mostrando cada paso real contra el backend (crear venta,
/// confirmar disponibilidad, registrar pago, cerrar).
class PantallaCobro extends ConsumerStatefulWidget {
  /// Lo que devuelve el `Navigator.pop` cuando no hay conexión: quien abrió
  /// esta pantalla debe caer al camino `/offline` de `ControladorDelPos`.
  static const String sinConexion = 'SIN_CONEXION';

  const PantallaCobro({
    super.key,
    required this.total,
    required this.clienteAsignado,
    required this.bodegaId,
    required this.lineas,
    this.clienteId,
    this.notaCliente,
  });

  final num total;
  final bool clienteAsignado;
  final String bodegaId;
  final List<LineaParaEnviar> lineas;
  final String? clienteId;
  final String? notaCliente;

  @override
  ConsumerState<PantallaCobro> createState() => _PantallaCobroState();
}

class _PantallaCobroState extends ConsumerState<PantallaCobro> {
  late final ParametrosDeCobro _parametros;
  final TextEditingController _montoRecibido = TextEditingController();
  bool _pidiendoEfectivo = false;

  @override
  void initState() {
    super.initState();
    _parametros = ParametrosDeCobro(
      total: widget.total,
      clienteAsignado: widget.clienteAsignado,
      bodegaId: widget.bodegaId,
      lineas: widget.lineas,
      clienteId: widget.clienteId,
      notaCliente: widget.notaCliente,
    );
  }

  @override
  void dispose() {
    _montoRecibido.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeCobroProvider(_parametros));
    final controlador = ref.read(controladorDeCobroProvider(_parametros).notifier);

    ref.listen(controladorDeCobroProvider(_parametros), (previo, actual) {
      if (actual.sinConexion && !(previo?.sinConexion ?? false)) {
        Navigator.of(context).pop(PantallaCobro.sinConexion);
      }
    });

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        title: const Text('Cobrar'),
        leading: IconButton(
          icon: const Icon(Icons.close),
          onPressed: estado.procesando ? null : () => Navigator.of(context).pop(),
        ),
      ),
      body: estado.terminado
          ? _VistaDeExito(numero: estado.ventaNumero!)
          : SafeArea(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _TarjetaTotal(total: estado.total, saldoPendiente: estado.saldoPendiente),
                    const SizedBox(height: 16),
                    if (estado.error != null) _BannerDeError(mensaje: estado.error!),
                    if (estado.requiereAutorizacionCredito)
                      Padding(
                        padding: const EdgeInsets.only(top: 8),
                        child: FilledButton.tonal(
                          onPressed: estado.procesando
                              ? null
                              : () => controlador.confirmarACredito(autorizado: true),
                          child: const Text('Autorizar y vender a crédito igual'),
                        ),
                      ),
                    if (estado.pagos.isNotEmpty) ...[
                      const SizedBox(height: 8),
                      ..._listaDePagos(estado, controlador),
                    ],
                    const SizedBox(height: 16),
                    if (!estado.cubierto) ...[
                      const Text('Método de pago', style: TextStyle(fontWeight: FontWeight.w700)),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        runSpacing: 8,
                        children: [
                          for (final m in _metodos)
                            _BotonDeMetodo(
                              etiqueta: m.$2,
                              icono: m.$3,
                              onTap: estado.procesando
                                  ? null
                                  : () => _elegirMetodo(m.$1, controlador),
                            ),
                          _BotonDeMetodo(
                            etiqueta: 'Crédito',
                            icono: Icons.schedule,
                            resaltado: false,
                            deshabilitado: !widget.clienteAsignado,
                            onTap: estado.procesando || !widget.clienteAsignado
                                ? null
                                : () => controlador.confirmarACredito(),
                          ),
                        ],
                      ),
                      if (!widget.clienteAsignado)
                        const Padding(
                          padding: EdgeInsets.only(top: 6),
                          child: Text(
                            'Asigna un cliente a la venta para poder vender a crédito.',
                            style: TextStyle(fontSize: 12.5, color: RegentaColors.ink),
                          ),
                        ),
                      if (_pidiendoEfectivo) _campoDeEfectivo(estado, controlador),
                    ],
                    const Spacer(),
                    if (estado.procesando)
                      Padding(
                        padding: const EdgeInsets.symmetric(vertical: 12),
                        child: Row(
                          children: [
                            const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2.4),
                            ),
                            const SizedBox(width: 10),
                            Text(estado.paso ?? 'Procesando…'),
                          ],
                        ),
                      ),
                    FilledButton(
                      onPressed: estado.cubierto && !estado.procesando
                          ? controlador.confirmarCobro
                          : null,
                      style: FilledButton.styleFrom(
                        backgroundColor: RegentaColors.accent,
                        foregroundColor: Colors.white,
                        minimumSize: const Size.fromHeight(RegentaSpacing.hitTarget),
                        shape: const StadiumBorder(),
                      ),
                      child: Text(
                        estado.procesando ? 'Cobrando…' : 'Confirmar cobro',
                        style: const TextStyle(fontWeight: FontWeight.w700),
                      ),
                    ),
                  ],
                ),
              ),
            ),
    );
  }

  void _elegirMetodo(String metodo, dynamic controlador) {
    if (metodo == 'EFECTIVO') {
      setState(() => _pidiendoEfectivo = true);
      return;
    }
    controlador.pagarSaldoCon(metodo);
  }

  Widget _campoDeEfectivo(EstadoDelCobro estado, dynamic controlador) {
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          TextField(
            controller: _montoRecibido,
            autofocus: true,
            keyboardType: const TextInputType.numberWithOptions(decimal: true),
            decoration: InputDecoration(
              labelText: 'Monto recibido en efectivo',
              prefixText: '\$ ',
              border: const OutlineInputBorder(),
              suffixIcon: TextButton(
                onPressed: () => _montoRecibido.text = estado.saldoPendiente.round().toString(),
                child: const Text('Exacto'),
              ),
            ),
          ),
          const SizedBox(height: 10),
          FilledButton.tonal(
            key: const Key('cobro-confirmar-efectivo'),
            onPressed: () {
              final recibido = num.tryParse(_montoRecibido.text.trim());
              if (recibido == null || recibido <= 0) return;
              controlador.pagarSaldoCon('EFECTIVO', montoRecibido: recibido);
              _montoRecibido.clear();
              setState(() => _pidiendoEfectivo = false);
            },
            child: const Text('Agregar pago en efectivo'),
          ),
        ],
      ),
    );
  }

  List<Widget> _listaDePagos(EstadoDelCobro estado, dynamic controlador) {
    return [
      const Text('Pagos registrados', style: TextStyle(fontWeight: FontWeight.w700)),
      const SizedBox(height: 6),
      for (var i = 0; i < estado.pagos.length; i++)
        Card(
          margin: const EdgeInsets.only(bottom: 6),
          child: ListTile(
            dense: true,
            title: Text(_etiquetaMetodo(estado.pagos[i].metodo)),
            subtitle: estado.pagos[i].cambio > 0
                ? Text('Cambio: ${formatearPesos(estado.pagos[i].cambio)}')
                : null,
            trailing: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(formatearPesos(estado.pagos[i].monto)),
                IconButton(
                  icon: const Icon(Icons.close, size: 18),
                  onPressed: estado.procesando ? null : () => controlador.quitarPago(i),
                ),
              ],
            ),
          ),
        ),
    ];
  }
}

class _TarjetaTotal extends StatelessWidget {
  const _TarjetaTotal({required this.total, required this.saldoPendiente});
  final num total;
  final num saldoPendiente;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text('Total a cobrar', style: TextStyle(fontSize: 13, color: RegentaColors.ink)),
          Text(
            formatearPesos(total),
            style: RegentaType.codigo.copyWith(fontSize: 28, fontWeight: FontWeight.w800),
          ),
          if (saldoPendiente > 0 && saldoPendiente < total)
            Padding(
              padding: const EdgeInsets.only(top: 4),
              child: Text(
                'Saldo pendiente: ${formatearPesos(saldoPendiente)}',
                style: const TextStyle(color: RegentaColors.warn, fontWeight: FontWeight.w700),
              ),
            ),
        ],
      ),
    );
  }
}

class _BannerDeError extends StatelessWidget {
  const _BannerDeError({required this.mensaje});
  final String mensaje;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: RegentaColors.critSoft,
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: RegentaColors.crit, size: 20),
          const SizedBox(width: 8),
          Expanded(child: Text(mensaje, style: const TextStyle(color: RegentaColors.crit))),
        ],
      ),
    );
  }
}

class _BotonDeMetodo extends StatelessWidget {
  const _BotonDeMetodo({
    required this.etiqueta,
    required this.icono,
    required this.onTap,
    this.resaltado = true,
    this.deshabilitado = false,
  });

  final String etiqueta;
  final IconData icono;
  final VoidCallback? onTap;
  final bool resaltado;
  final bool deshabilitado;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onTap,
      icon: Icon(icono, size: 18, color: deshabilitado ? RegentaColors.line : null),
      label: Text(etiqueta),
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(0, RegentaSpacing.hitTarget),
        padding: const EdgeInsets.symmetric(horizontal: 14),
        shape: const StadiumBorder(),
      ),
    );
  }
}

class _VistaDeExito extends StatelessWidget {
  const _VistaDeExito({required this.numero});
  final String numero;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          const Icon(Icons.check_circle, color: RegentaColors.ok, size: 64),
          const SizedBox(height: 12),
          Text('Venta $numero cobrada', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.w700)),
          const SizedBox(height: 20),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(numero),
            style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent, foregroundColor: Colors.white),
            child: const Text('Listo'),
          ),
        ],
      ),
    );
  }
}
