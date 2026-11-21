import 'dart:async';

import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'datos.dart';

String _fecha(DateTime d) =>
    '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

const _metodos = {
  'EFECTIVO': 'Efectivo',
  'TRANSFERENCIA': 'Transferencia',
  'CHEQUE': 'Cheque',
  'TARJETA': 'Tarjeta',
  'OTRO': 'Otro',
};

/// HU-172 · Los pagos de una cuenta: lo que cada uno le abonó y, los anulados, con su motivo. Desde
/// aquí se anula un pago registrado por error. Devuelve `true` si se anuló alguno.
Future<bool> abrirPagosDeLaCuenta(
  BuildContext context, {
  required CuentaPorPagarVista cuenta,
  required Future<List<PagoDeLaCuenta>?> Function(String cuentaId) cargar,
  required Future<String?> Function(String pagoId, String motivo) onAnular,
}) async {
  final cambio = await showDialog<bool>(
    context: context,
    builder: (_) => Dialog(
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 540, maxHeight: 640),
        child: PagosDeLaCuenta(cuenta: cuenta, cargar: cargar, onAnular: onAnular),
      ),
    ),
  );
  return cambio ?? false;
}

class PagosDeLaCuenta extends StatefulWidget {
  const PagosDeLaCuenta({super.key, required this.cuenta, required this.cargar, required this.onAnular});

  final CuentaPorPagarVista cuenta;
  final Future<List<PagoDeLaCuenta>?> Function(String cuentaId) cargar;
  final Future<String?> Function(String pagoId, String motivo) onAnular;

  @override
  State<PagosDeLaCuenta> createState() => _PagosDeLaCuentaState();
}

class _PagosDeLaCuentaState extends State<PagosDeLaCuenta> {
  List<PagoDeLaCuenta>? _pagos;
  bool _cargando = true;
  bool _huboCambio = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    unawaited(_traer());
  }

  Future<void> _traer() async {
    final pagos = await widget.cargar(widget.cuenta.id);
    if (!mounted) return;
    setState(() {
      _pagos = pagos;
      _cargando = false;
      _error = pagos == null ? 'No se pudieron traer los pagos de esta cuenta.' : null;
    });
  }

  Future<void> _anular(PagoDeLaCuenta pago) async {
    final motivo = await _pedirMotivo(context, pago);
    if (motivo == null || !mounted) return;
    final rechazo = await widget.onAnular(pago.id, motivo);
    if (!mounted) return;
    if (rechazo != null) {
      setState(() => _error = rechazo);
      return;
    }
    _huboCambio = true;
    setState(() {
      _error = null;
      _cargando = true;
    });
    await _traer();
  }

  @override
  Widget build(BuildContext context) {
    final pagos = _pagos ?? const <PagoDeLaCuenta>[];
    return Material(
      color: RegentaColors.surface,
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        CabeceraDeDialogo(
          titulo: 'Pagos · ${widget.cuenta.documento}',
          subtitulo: widget.cuenta.proveedorNombre,
          onCerrar: () => Navigator.of(context).pop(_huboCambio),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 10, 18, 0),
            child: AvisoDeError(_error!, key: const Key('pagos-error')),
          ),
        Flexible(
          child: _cargando
              ? const Padding(padding: EdgeInsets.all(32), child: Center(child: CircularProgressIndicator()))
              : pagos.isEmpty
                  ? Padding(
                      padding: const EdgeInsets.all(28),
                      child: Text('Esta cuenta todavía no tiene pagos.',
                          textAlign: TextAlign.center,
                          style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
                    )
                  : ListView(
                      key: const Key('pagos-lista'),
                      shrinkWrap: true,
                      padding: const EdgeInsets.fromLTRB(18, 8, 18, 14),
                      children: [for (final p in pagos) _FilaPago(pago: p, onAnular: () => unawaited(_anular(p)))],
                    ),
        ),
      ]),
    );
  }
}

class _FilaPago extends StatelessWidget {
  const _FilaPago({required this.pago, required this.onAnular});

  final PagoDeLaCuenta pago;
  final VoidCallback onAnular;

  @override
  Widget build(BuildContext context) {
    final p = pago;
    final metodo = _metodos[p.metodo] ?? p.metodo;
    return Container(
      key: Key('pago-${p.id}'),
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
      child: Row(children: [
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(formatearPesos(p.monto),
                style: RegentaType.codigo.copyWith(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: p.anulado ? RegentaColors.muted : RegentaColors.ink,
                  decoration: p.anulado ? TextDecoration.lineThrough : null,
                )),
            Text(
              [_fecha(p.fecha), metodo, if (p.desdeLaCaja) 'de la caja', if (p.referencia != null) p.referencia!]
                  .join(' · '),
              style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted),
            ),
            if (p.anulado) ...[
              const SizedBox(height: 4),
              const ChipDeEstado(texto: 'anulado', fondo: RegentaColors.critSoft, tinta: RegentaColors.crit),
              if (p.motivoAnulacion != null)
                Padding(
                  padding: const EdgeInsets.only(top: 3),
                  child: Text('Motivo: ${p.motivoAnulacion}',
                      style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.ink2)),
                ),
            ],
          ]),
        ),
        if (!p.anulado)
          TextButton(
            key: Key('pago-anular-${p.id}'),
            onPressed: onAnular,
            style: TextButton.styleFrom(foregroundColor: RegentaColors.crit),
            child: const Text('Anular'),
          ),
      ]),
    );
  }
}

/// El motivo es obligatorio (I28). Si el pago salió de la caja, se avisa que el efectivo vuelve a ella.
Future<String?> _pedirMotivo(BuildContext context, PagoDeLaCuenta pago) {
  final motivo = TextEditingController();
  String? error;
  return showDialog<String>(
    context: context,
    builder: (dialogo) => StatefulBuilder(
      builder: (context, setState) => AlertDialog(
        title: Text('¿Anular el pago de ${formatearPesos(pago.monto)}?'),
        content: SizedBox(
          width: 380,
          child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Text(
              pago.desdeLaCaja
                  ? 'Lo que abonó vuelve a deberse y el efectivo regresa a la caja: a la misma si sigue '
                      'abierta, o a la tuya si ya cerró.'
                  : 'Lo que abonó vuelve a deberse. El pago queda visible como anulado.',
              style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink2),
            ),
            const SizedBox(height: 12),
            CampoConEtiqueta(
              etiqueta: 'Motivo *',
              hijo: TextField(
                key: const Key('anular-motivo'),
                controller: motivo,
                autofocus: true,
                maxLength: 200,
                style: estiloDeCampo,
                decoration: decoracionDeCampo(hint: 'Se registró dos veces').copyWith(counterText: ''),
              ),
            ),
            if (error != null) AvisoDeError(error!),
          ]),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogo).pop(), child: const Text('Cancelar')),
          FilledButton(
            key: const Key('anular-confirmar'),
            style: FilledButton.styleFrom(backgroundColor: RegentaColors.crit),
            onPressed: () {
              if (motivo.text.trim().isEmpty) {
                setState(() => error = 'Escribe por qué se anula.');
                return;
              }
              Navigator.of(dialogo).pop(motivo.text.trim());
            },
            child: const Text('Anular pago'),
          ),
        ],
      ),
    ),
  );
}
