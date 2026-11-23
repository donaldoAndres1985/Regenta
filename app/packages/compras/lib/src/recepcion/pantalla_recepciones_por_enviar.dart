import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/pantalla_recepcion.dart';
import 'recepciones_por_enviar.dart';

/// HU-182 · Las recepciones hechas en el celular que todavía no entraron: «por enviar» (suben solas
/// cuando vuelve la señal, C1/C2) o «con error» (el backend las rechazó: se corrigen y se reenvían,
/// C3).
class PantallaRecepcionesPorEnviar extends ConsumerStatefulWidget {
  const PantallaRecepcionesPorEnviar({super.key});

  @override
  ConsumerState<PantallaRecepcionesPorEnviar> createState() => _PantallaRecepcionesPorEnviarState();
}

class _PantallaRecepcionesPorEnviarState extends ConsumerState<PantallaRecepcionesPorEnviar> {
  String? _ocupada;

  void _avisar(String texto) => ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(texto)));

  Future<void> _enviar(RecepcionPorEnviar r) async {
    final bandeja = ref.read(bandejaDeRecepcionesProvider);
    if (bandeja == null) return;
    setState(() => _ocupada = r.id);
    final motivo = await bandeja.enviarAhora(r);
    if (!mounted) return;
    setState(() => _ocupada = null);
    _avisar(motivo ?? 'La recepción de ${r.ordenNumero ?? 'la orden'} entró.');
  }

  Future<void> _corregir(RecepcionPorEnviar r) async {
    await Navigator.of(context).push<void>(MaterialPageRoute(
      builder: (ruta) => PantallaRecepcion(
        ordenId: r.ordenId,
        correccion: r,
        onVolver: () => Navigator.of(ruta).pop(),
        onRegistrada: (_) => Navigator.of(ruta).pop(),
      ),
    ));
  }

  Future<void> _descartar(RecepcionPorEnviar r) async {
    final seguro = await showDialog<bool>(
      context: context,
      builder: (dialogo) => AlertDialog(
        title: const Text('¿Descartar esta recepción?'),
        content: Text('Lo que se anotó de ${r.ordenNumero ?? 'la orden'} se pierde y no llega al sistema.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogo).pop(false), child: const Text('Cancelar')),
          FilledButton(
            key: const Key('por-enviar-descartar-si'),
            style: FilledButton.styleFrom(backgroundColor: RegentaColors.crit),
            onPressed: () => Navigator.of(dialogo).pop(true),
            child: const Text('Descartar'),
          ),
        ],
      ),
    );
    if (seguro != true || !mounted) return;
    await ref.read(bandejaDeRecepcionesProvider)?.descartar(r.id);
    if (mounted) _avisar('Recepción descartada.');
  }

  @override
  Widget build(BuildContext context) {
    final lista = ref.watch(recepcionesPorEnviarProvider);
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text('Recepciones por enviar',
            overflow: TextOverflow.ellipsis,
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
      ),
      body: lista.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (e, _) => Center(child: Text('$e')),
        data: (recepciones) => recepciones.isEmpty
            ? Center(
                child: Text('Todo lo recibido en el celular ya entró.',
                    key: const Key('por-enviar-vacio'),
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
              )
            : ListView(
                key: const Key('por-enviar-lista'),
                padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
                children: [
                  for (final r in recepciones)
                    _Tarjeta(
                      recepcion: r,
                      ocupada: _ocupada == r.id,
                      onEnviar: () => unawaited(_enviar(r)),
                      onCorregir: () => unawaited(_corregir(r)),
                      onDescartar: () => unawaited(_descartar(r)),
                    ),
                ],
              ),
      ),
    );
  }
}

class _Tarjeta extends StatelessWidget {
  const _Tarjeta({
    required this.recepcion,
    required this.ocupada,
    required this.onEnviar,
    required this.onCorregir,
    required this.onDescartar,
  });

  final RecepcionPorEnviar recepcion;
  final bool ocupada;
  final VoidCallback onEnviar;
  final VoidCallback onCorregir;
  final VoidCallback onDescartar;

  @override
  Widget build(BuildContext context) {
    final r = recepcion;
    final g = r.guardadaEn;
    final hora = '${g.day.toString().padLeft(2, '0')}/${g.month.toString().padLeft(2, '0')} '
        '${g.hour.toString().padLeft(2, '0')}:${g.minute.toString().padLeft(2, '0')}';
    final productos = r.lineas == 1 ? '1 producto' : '${r.lineas} productos';
    return Container(
      key: Key('por-enviar-${r.id}'),
      margin: const EdgeInsets.only(bottom: 12),
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 10),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: r.conError ? RegentaColors.crit : RegentaColors.line),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          Expanded(
            child: Text(r.ordenNumero ?? 'Recepción',
                style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
          ),
          ChipDeEstado(
            texto: r.conError ? 'CON ERROR' : 'POR ENVIAR',
            fondo: r.conError ? RegentaColors.critSoft : RegentaColors.warnSoft,
            tinta: r.conError ? RegentaColors.crit : RegentaColors.warn,
          ),
        ]),
        const SizedBox(height: 4),
        Text('$productos · guardada el $hora',
            style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
        if (r.conError && r.error != null) ...[
          const SizedBox(height: 8),
          Text(r.error!,
              key: Key('por-enviar-error-${r.id}'),
              style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.crit)),
        ],
        const SizedBox(height: 8),
        Wrap(spacing: 8, runSpacing: 4, alignment: WrapAlignment.end, children: [
          TextButton(
            key: Key('por-enviar-descartar-${r.id}'),
            onPressed: ocupada ? null : onDescartar,
            child: const Text('Descartar'),
          ),
          if (r.conError)
            FilledButton.icon(
              key: Key('por-enviar-corregir-${r.id}'),
              style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
              onPressed: ocupada ? null : onCorregir,
              icon: const Icon(Icons.edit_outlined, size: 18),
              label: const Text('Corregir'),
            )
          else
            FilledButton.icon(
              key: Key('por-enviar-enviar-${r.id}'),
              style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
              onPressed: ocupada ? null : onEnviar,
              icon: const Icon(Icons.cloud_upload_outlined, size: 18),
              label: const Text('Enviar ahora'),
            ),
        ]),
      ]),
    );
  }
}
