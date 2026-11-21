import 'dart:async';

import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'controlador.dart';
import 'datos.dart';
import 'reparto.dart';

/// Lo que el diálogo le pide al controlador para registrar el pago.
typedef AlPagar = Future<String?> Function({
  required String proveedorId,
  required num monto,
  required String metodo,
  String? referencia,
  bool desdeLaCaja,
  Map<String, num>? aplicaciones,
});

/// HU-171 · Pagar a un proveedor: un monto que se reparte entre sus facturas abiertas. Por omisión
/// de la más antigua a la más nueva; con «Repartir a mano», como lo escriba el usuario. Devuelve
/// `true` si el pago quedó registrado.
Future<bool> abrirDialogoDePago(
  BuildContext context, {
  required EstadoDeCuentas estado,
  String? proveedorId,
  required AlPagar onPagar,
}) async {
  final hecho = await showDialog<bool>(
    context: context,
    barrierDismissible: false,
    builder: (_) => Dialog(
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 560, maxHeight: 760),
        child: DialogoDePago(estado: estado, proveedorInicial: proveedorId, onPagar: onPagar),
      ),
    ),
  );
  return hecho ?? false;
}

class DialogoDePago extends StatefulWidget {
  const DialogoDePago({super.key, required this.estado, this.proveedorInicial, required this.onPagar});

  final EstadoDeCuentas estado;
  final String? proveedorInicial;
  final AlPagar onPagar;

  @override
  State<DialogoDePago> createState() => _DialogoDePagoState();
}

class _DialogoDePagoState extends State<DialogoDePago> {
  static const _metodos = {
    'TRANSFERENCIA': 'Transferencia',
    'EFECTIVO': 'Efectivo',
    'CHEQUE': 'Cheque',
    'TARJETA': 'Tarjeta',
  };

  final TextEditingController _monto = TextEditingController();
  final TextEditingController _referencia = TextEditingController();
  final Map<String, TextEditingController> _aMano = {};
  String? _proveedorId;
  String _metodo = 'TRANSFERENCIA';
  bool _repartirAMano = false;
  late bool _desdeLaCaja = widget.estado.cajaAbierta;
  String? _error;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    _proveedorId = widget.proveedorInicial ??
        (widget.estado.proveedoresConDeuda.length == 1 ? widget.estado.proveedoresConDeuda.keys.first : null);
  }

  @override
  void dispose() {
    _monto.dispose();
    _referencia.dispose();
    for (final c in _aMano.values) {
      c.dispose();
    }
    super.dispose();
  }

  List<CuentaPorPagarVista> get _abiertas =>
      _proveedorId == null ? const [] : widget.estado.abiertasDe(_proveedorId!);

  num get _deuda => _abiertas.fold<num>(0, (s, c) => s + c.saldo);

  num get _montoEscrito => num.tryParse(_monto.text.replaceAll(RegExp(r'[^0-9.]'), '')) ?? 0;

  Map<String, num> get _reparto => _repartirAMano
      ? {
          for (final c in _abiertas)
            c.id: num.tryParse((_aMano[c.id]?.text ?? '').replaceAll(RegExp(r'[^0-9.]'), '')) ?? 0,
        }
      : repartirPorAntiguedad(_montoEscrito, _abiertas);

  TextEditingController _campoDe(CuentaPorPagarVista c) =>
      _aMano.putIfAbsent(c.id, () => TextEditingController());

  void _cambiarAMano(bool aMano) {
    setState(() {
      if (aMano) {
        // Arranca desde el reparto automático, para corregir solo lo que haga falta.
        final auto = repartirPorAntiguedad(_montoEscrito, _abiertas);
        for (final c in _abiertas) {
          final v = auto[c.id] ?? 0;
          _campoDe(c).text = v == 0 ? '' : '${v.round()}';
        }
      }
      _repartirAMano = aMano;
    });
  }

  String? _validar() {
    if (_proveedorId == null) return 'Elige el proveedor.';
    final monto = _montoEscrito;
    if (monto <= 0) return 'Escribe cuánto vas a pagar.';
    if (monto > _deuda) return 'No se paga más de lo que se debe: le debes ${formatearPesos(_deuda)}.';
    if (_repartirAMano) {
      final suma = _reparto.values.fold<num>(0, (s, v) => s + v);
      if (suma != monto) {
        return 'El reparto suma ${formatearPesos(suma)} y el pago es de ${formatearPesos(monto)}.';
      }
      for (final c in _abiertas) {
        if ((_reparto[c.id] ?? 0) > c.saldo) {
          return 'A la ${c.documento} se le deben ${formatearPesos(c.saldo)}: no se le abona más.';
        }
      }
    }
    return null;
  }

  Future<void> _pagar() async {
    final problema = _validar();
    if (problema != null) {
      setState(() => _error = problema);
      return;
    }
    setState(() {
      _guardando = true;
      _error = null;
    });
    final rechazo = await widget.onPagar(
      proveedorId: _proveedorId!,
      monto: _montoEscrito,
      metodo: _metodo,
      referencia: _referencia.text,
      desdeLaCaja: _metodo == 'EFECTIVO' && widget.estado.cajaAbierta && _desdeLaCaja,
      aplicaciones: _repartirAMano ? _reparto : null,
    );
    if (!mounted) return;
    if (rechazo == null) {
      Navigator.of(context).pop(true);
    } else {
      setState(() {
        _error = rechazo;
        _guardando = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    final proveedores = widget.estado.proveedoresConDeuda;
    final reparto = _reparto;
    return Material(
      color: RegentaColors.surface,
      child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        CabeceraDeDialogo(
          titulo: 'Pagar a proveedor',
          subtitulo: _proveedorId == null ? null : 'Le debes ${formatearPesos(_deuda)}',
          onCerrar: _guardando ? null : () => Navigator.of(context).pop(false),
        ),
        Flexible(
          child: SingleChildScrollView(
            padding: const EdgeInsets.fromLTRB(18, 14, 18, 8),
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              CampoConEtiqueta(
                etiqueta: 'Proveedor',
                hijo: DropdownButtonFormField<String>(
                  key: const Key('pago-proveedor'),
                  initialValue: _proveedorId,
                  isExpanded: true,
                  decoration: decoracionDeCampo(hint: 'Elige a quién le pagas'),
                  items: [
                    for (final e in proveedores.entries)
                      DropdownMenuItem(value: e.key, child: Text(e.value, overflow: TextOverflow.ellipsis)),
                  ],
                  onChanged: (v) => setState(() {
                    _proveedorId = v;
                    _repartirAMano = false;
                  }),
                ),
              ),
              Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Expanded(
                  child: CampoConEtiqueta(
                    etiqueta: 'Monto *',
                    hijo: TextField(
                      key: const Key('pago-monto'),
                      controller: _monto,
                      keyboardType: TextInputType.number,
                      onChanged: (_) => setState(() {}),
                      style: estiloDeCampo,
                      decoration: decoracionDeCampo(hint: '0'),
                    ),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: CampoConEtiqueta(
                    etiqueta: 'Pagado con',
                    hijo: DropdownButtonFormField<String>(
                      key: const Key('pago-metodo'),
                      initialValue: _metodo,
                      isExpanded: true,
                      decoration: decoracionDeCampo(),
                      items: [
                        for (final m in _metodos.entries) DropdownMenuItem(value: m.key, child: Text(m.value)),
                      ],
                      onChanged: (v) => setState(() => _metodo = v ?? 'TRANSFERENCIA'),
                    ),
                  ),
                ),
              ]),
              if (_metodo == 'EFECTIVO')
                CheckboxListTile(
                  key: const Key('pago-desde-la-caja'),
                  contentPadding: EdgeInsets.zero,
                  controlAffinity: ListTileControlAffinity.leading,
                  value: widget.estado.cajaAbierta && _desdeLaCaja,
                  onChanged: widget.estado.cajaAbierta ? (v) => setState(() => _desdeLaCaja = v ?? false) : null,
                  title: const Text('Pagar con dinero de la caja'),
                  subtitle: Text(
                    widget.estado.cajaAbierta
                        ? 'La salida queda registrada sola en tu caja abierta.'
                        : 'No tienes una caja abierta: se registra como pagado con dinero de fuera de la caja.',
                    style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted),
                  ),
                )
              else
                CampoConEtiqueta(
                  etiqueta: 'Referencia',
                  hijo: TextField(
                    key: const Key('pago-referencia'),
                    controller: _referencia,
                    style: estiloDeCampo,
                    decoration: decoracionDeCampo(hint: 'Nº de transferencia o cheque'),
                  ),
                ),
              if (_abiertas.isNotEmpty) ...[
                Row(children: [
                  Expanded(
                    child: Text('A QUÉ FACTURAS VA', style: RegentaType.etiqueta.copyWith(fontSize: 9.5)),
                  ),
                  Text('Repartir a mano', style: RegentaType.cuerpo.copyWith(fontSize: 12.5)),
                  Switch(
                    key: const Key('pago-a-mano'),
                    value: _repartirAMano,
                    onChanged: _cambiarAMano,
                  ),
                ]),
                for (final c in _abiertas)
                  Container(
                    key: Key('pago-cuenta-${c.id}'),
                    padding: const EdgeInsets.symmetric(vertical: 6),
                    decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
                    child: Row(children: [
                      Expanded(
                        child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                          Text(c.documento, style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte)),
                          Text('debe ${formatearPesos(c.saldo)}',
                              style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                        ]),
                      ),
                      SizedBox(
                        width: 130,
                        child: _repartirAMano
                            ? TextField(
                                key: Key('pago-aplicar-${c.id}'),
                                controller: _campoDe(c),
                                keyboardType: TextInputType.number,
                                onChanged: (_) => setState(() {}),
                                style: estiloDeCampo,
                                textAlign: TextAlign.right,
                                decoration: decoracionDeCampo(hint: '0'),
                              )
                            : Text(formatearPesos(reparto[c.id] ?? 0),
                                key: Key('pago-reparto-${c.id}'),
                                textAlign: TextAlign.right,
                                style: RegentaType.codigo.copyWith(fontSize: 13, color: RegentaColors.ink)),
                      ),
                    ]),
                  ),
              ],
              const SizedBox(height: 10),
              if (_error != null) AvisoDeError(_error!, key: const Key('pago-error')),
            ]),
          ),
        ),
        Container(
          padding: const EdgeInsets.fromLTRB(18, 12, 18, 14),
          decoration: const BoxDecoration(border: Border(top: BorderSide(color: RegentaColors.line))),
          child: Row(children: [
            Expanded(
              child: SizedBox(
                height: RegentaSpacing.hitTarget,
                child: OutlinedButton(
                  onPressed: _guardando ? null : () => Navigator.of(context).pop(false),
                  child: const Text('Cancelar'),
                ),
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: SizedBox(
                height: RegentaSpacing.hitTarget,
                child: FilledButton(
                  key: const Key('pago-registrar'),
                  onPressed: _guardando ? null : () => unawaited(_pagar()),
                  child: Text(_guardando ? 'Registrando…' : 'Registrar pago'),
                ),
              ),
            ),
          ]),
        ),
      ]),
    );
  }
}
