import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'controlador.dart';
import 'datos.dart';
import 'dialogo_de_pago.dart';
import 'dialogo_de_pagos.dart';

String _fecha(DateTime d) =>
    '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

/// Cuentas por pagar (HU-170): lo que se le debe a cada proveedor, por antigüedad, con las vencidas
/// marcadas y las que llegaron con remisión esperando su factura.
class PantallaCuentasPorPagar extends ConsumerWidget {
  const PantallaCuentasPorPagar({super.key});

  Future<void> _registrarFactura(BuildContext context, WidgetRef ref, CuentaPorPagarVista cuenta) async {
    final ctrl = ref.read(controladorDeCuentasProvider.notifier);
    final hecho = await showDialog<bool>(
      context: context,
      builder: (_) => _DialogoDeFactura(
        cuenta: cuenta,
        onGuardar: (numero, fecha) => ctrl.registrarFactura(cuenta.id, numero, fecha),
      ),
    );
    if (hecho == true && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
        content: Text('Factura registrada. El vencimiento se calculó desde su fecha.'),
      ));
    }
  }

  /// HU-171 · Pagar a un proveedor, repartiendo entre sus facturas.
  Future<void> _pagar(BuildContext context, WidgetRef ref, {String? proveedorId}) async {
    final ctrl = ref.read(controladorDeCuentasProvider.notifier);
    final hecho = await abrirDialogoDePago(
      context,
      estado: ref.read(controladorDeCuentasProvider),
      proveedorId: proveedorId,
      onPagar: ctrl.pagar,
    );
    if (hecho && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Pago registrado.')));
    }
  }

  /// HU-172 · Los pagos de una cuenta, para revisarlos o anular uno.
  Future<void> _verPagos(BuildContext context, WidgetRef ref, CuentaPorPagarVista cuenta) async {
    final ctrl = ref.read(controladorDeCuentasProvider.notifier);
    final anulado = await abrirPagosDeLaCuenta(context, cuenta: cuenta, cargar: ctrl.pagosDe, onAnular: ctrl.anularPago);
    if (anulado && context.mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Pago anulado. Lo que abonó vuelve a deberse.')));
    }
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(controladorDeCuentasProvider);
    final ctrl = ref.read(controladorDeCuentasProvider.notifier);
    return LayoutBuilder(builder: (context, afuera) {
      final escritorio = afuera.maxWidth >= kBreakpointEscritorio;
      final acciones = <Widget>[
        IconButton(
          tooltip: 'Actualizar',
          onPressed: estado.cargando ? null : ctrl.cargar,
          icon: const Icon(Icons.refresh, size: 20, color: RegentaColors.ink2),
        ),
        FilledButton.icon(
          key: const Key('cuentas-pagar'),
          onPressed: estado.proveedoresConDeuda.isEmpty ? null : () => unawaited(_pagar(context, ref)),
          icon: const Icon(Icons.payments_outlined, size: 18),
          label: Text(escritorio ? 'Pagar a proveedor' : 'Pagar'),
        ),
      ];
      final cuerpo = Column(children: [
        Container(
          width: double.infinity,
          padding: const EdgeInsets.fromLTRB(14, 10, 14, 10),
          decoration: const BoxDecoration(
            color: RegentaColors.surface,
            border: Border(bottom: BorderSide(color: RegentaColors.line)),
          ),
          child: SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(children: [
              for (final f in FiltroDeCuentas.values) ...[
                ChoiceChip(
                  key: Key('cuentas-filtro-${f.name}'),
                  label: Text(f == FiltroDeCuentas.enEspera && estado.enEspera > 0
                      ? '${f.etiqueta} (${estado.enEspera})'
                      : f.etiqueta),
                  selected: estado.filtro == f,
                  onSelected: (_) => ctrl.cambiarFiltro(f),
                ),
                const SizedBox(width: 7),
              ],
            ]),
          ),
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 2),
          child: Row(children: [
            Expanded(
              child: Text(
                estado.visibles.length == 1 ? '1 cuenta' : '${estado.visibles.length} cuentas',
                style: RegentaType.cuerpo.copyWith(fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink),
              ),
            ),
            Text('saldo ${formatearPesos(estado.saldoVisible)}',
                key: const Key('cuentas-saldo'),
                style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.ink2)),
          ]),
        ),
        Expanded(
          child: _Lista(
            estado: estado,
            onFactura: (c) => unawaited(_registrarFactura(context, ref, c)),
            onPagar: (c) => unawaited(_pagar(context, ref, proveedorId: c.proveedorId)),
            onVerPagos: (c) => unawaited(_verPagos(context, ref, c)),
          ),
        ),
      ]);
      return Scaffold(
        backgroundColor: RegentaColors.paper,
        appBar: escritorio
            ? EncabezadoDeSeccion(
                subtitulo: 'Lo que le debes a cada proveedor y cuándo vence',
                titulo: 'Cuentas por pagar',
                acciones: acciones,
              )
            : AppBar(
                backgroundColor: RegentaColors.surface,
                surfaceTintColor: RegentaColors.surface,
                elevation: 0,
                shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
                title: Text('Cuentas por pagar',
                    style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
                actions: [...acciones, const SizedBox(width: 8)],
              ),
        body: escritorio
            ? Center(child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 900), child: cuerpo))
            : cuerpo,
      );
    });
  }
}

class _Lista extends StatelessWidget {
  const _Lista({required this.estado, required this.onFactura, required this.onPagar, required this.onVerPagos});

  final EstadoDeCuentas estado;
  final ValueChanged<CuentaPorPagarVista> onFactura;
  final ValueChanged<CuentaPorPagarVista> onPagar;
  final ValueChanged<CuentaPorPagarVista> onVerPagos;

  @override
  Widget build(BuildContext context) {
    if (estado.cargando && estado.todas.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    final visibles = estado.visibles;
    if (visibles.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text(
            estado.mensaje ??
                (estado.filtro == FiltroDeCuentas.enEspera
                    ? 'No hay compras esperando su factura.'
                    : 'No hay cuentas por pagar con este filtro.'),
            key: const Key('cuentas-vacio'),
            textAlign: TextAlign.center,
            style: RegentaType.cuerpo.copyWith(fontSize: 15, color: RegentaColors.ink2),
          ),
        ),
      );
    }
    return ListView.builder(
      key: const Key('cuentas-lista'),
      padding: const EdgeInsets.symmetric(horizontal: 14),
      itemCount: visibles.length,
      itemBuilder: (context, i) => _FilaCuenta(
        cuenta: visibles[i],
        onFactura: () => onFactura(visibles[i]),
        onPagar: () => onPagar(visibles[i]),
        onVerPagos: () => onVerPagos(visibles[i]),
      ),
    );
  }
}

class _FilaCuenta extends StatelessWidget {
  const _FilaCuenta({
    required this.cuenta,
    required this.onFactura,
    required this.onPagar,
    required this.onVerPagos,
  });

  final CuentaPorPagarVista cuenta;
  final VoidCallback onFactura;
  final VoidCallback onPagar;
  final VoidCallback onVerPagos;

  @override
  Widget build(BuildContext context) {
    final c = cuenta;
    final documento = c.enEsperaDeFactura
        ? 'Remisión ${c.recepcionNumero ?? ''}'.trim()
        : 'Factura ${c.numeroFactura}';
    return Container(
      key: Key('cuenta-${c.id}'),
      padding: const EdgeInsets.symmetric(vertical: 12),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
      child: Row(children: [
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text((c.proveedorNombre ?? 'Proveedor').toUpperCase(),
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
            const SizedBox(height: 3),
            Text('$documento · vence ${_fecha(c.fechaVencimiento)}',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
            const SizedBox(height: 4),
            Wrap(spacing: 6, runSpacing: 4, children: [
              if (c.enEsperaDeFactura)
                const ChipDeEstado(texto: 'en espera de factura', fondo: RegentaColors.warnSoft, tinta: RegentaColors.warn),
              if (c.vencida)
                ChipDeEstado(
                    texto: 'vencida ${c.diasDeMora} d', fondo: RegentaColors.critSoft, tinta: RegentaColors.crit),
              if (c.pagada) const ChipDeEstado(texto: 'pagada', fondo: RegentaColors.okSoft, tinta: RegentaColors.ok),
            ]),
          ]),
        ),
        const SizedBox(width: 10),
        Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
          Text(formatearPesos(c.saldo),
              style: RegentaType.codigo.copyWith(fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
          Text('de ${formatearPesos(c.monto)}',
              style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
          if (c.enEsperaDeFactura)
            TextButton(
              key: Key('cuenta-registrar-factura-${c.id}'),
              onPressed: onFactura,
              child: const Text('Registrar factura'),
            ),
          if (c.abierta && c.proveedorId != null)
            TextButton(
              key: Key('cuenta-pagar-${c.id}'),
              onPressed: onPagar,
              child: const Text('Pagar'),
            ),
          // HU-172 · si ya se le abonó algo (o está pagada), se ven sus pagos y se puede anular uno.
          if (c.saldo < c.monto)
            TextButton(
              key: Key('cuenta-ver-pagos-${c.id}'),
              onPressed: onVerPagos,
              child: const Text('Ver pagos'),
            ),
        ]),
      ]),
    );
  }
}

/// HU-170 C3 · Número y fecha de la factura que llegó después de la mercancía.
class _DialogoDeFactura extends StatefulWidget {
  const _DialogoDeFactura({required this.cuenta, required this.onGuardar});

  final CuentaPorPagarVista cuenta;
  final Future<String?> Function(String numero, DateTime fecha) onGuardar;

  @override
  State<_DialogoDeFactura> createState() => _DialogoDeFacturaState();
}

class _DialogoDeFacturaState extends State<_DialogoDeFactura> {
  final TextEditingController _numero = TextEditingController();
  DateTime _fecha = DateTime.now();
  String? _error;
  bool _guardando = false;

  @override
  void dispose() {
    _numero.dispose();
    super.dispose();
  }

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    final elegida = await showDatePicker(
      context: context,
      initialDate: _fecha,
      firstDate: DateTime(hoy.year - 1),
      lastDate: hoy,
    );
    if (elegida != null) setState(() => _fecha = elegida);
  }

  Future<void> _guardar() async {
    if (_numero.text.trim().isEmpty) {
      setState(() => _error = 'Escribe el número de la factura.');
      return;
    }
    setState(() {
      _guardando = true;
      _error = null;
    });
    final rechazo = await widget.onGuardar(_numero.text, _fecha);
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
    return AlertDialog(
      title: const Text('Registrar factura'),
      content: SizedBox(
        width: 360,
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text(
            '${widget.cuenta.proveedorNombre ?? 'Proveedor'} · remisión ${widget.cuenta.recepcionNumero ?? ''}',
            style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2),
          ),
          const SizedBox(height: 12),
          CampoConEtiqueta(
            etiqueta: 'Nº de factura *',
            hijo: TextField(
              key: const Key('factura-numero'),
              controller: _numero,
              autofocus: true,
              style: estiloDeCampo,
              decoration: decoracionDeCampo(hint: 'FV-1234'),
            ),
          ),
          CampoConEtiqueta(
            etiqueta: 'Fecha de la factura',
            ayuda: 'El vencimiento se calcula desde esta fecha',
            hijo: OutlinedButton.icon(
              key: const Key('factura-fecha'),
              onPressed: () => unawaited(_elegirFecha()),
              style: OutlinedButton.styleFrom(minimumSize: const Size.fromHeight(44), alignment: Alignment.centerLeft),
              icon: const Icon(Icons.event, size: 18),
              label: Text(_fecha.toIso8601String().substring(0, 10)),
            ),
          ),
          if (_error != null) AvisoDeError(_error!, key: const Key('factura-error')),
        ]),
      ),
      actions: [
        TextButton(
          onPressed: _guardando ? null : () => Navigator.of(context).pop(false),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          key: const Key('factura-guardar'),
          onPressed: _guardando ? null : () => unawaited(_guardar()),
          child: Text(_guardando ? 'Guardando…' : 'Registrar'),
        ),
      ],
    );
  }
}
