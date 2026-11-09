import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/orden_resumen_vista.dart';
import '../recepcion/proveedores.dart';
import 'formato.dart';

final listaOrdenesComprasProvider =
    FutureProvider.autoDispose<List<OrdenResumenVista>>((ref) {
  final repo = ref.watch(repositorioDeRecepcionesProvider);
  return repo.listarOrdenes();
});

final ordenesCreadasLocalProvider =
    StateProvider<List<OrdenResumenVista>>((ref) => []);

class PantallaCompras extends ConsumerWidget {
  const PantallaCompras({
    super.key,
    required this.onOrdenSeleccionada,
  });

  final ValueChanged<String> onOrdenSeleccionada;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordenesAsync = ref.watch(listaOrdenesComprasProvider);
    final locales = ref.watch(ordenesCreadasLocalProvider);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Row(
          children: [
            const Icon(Icons.local_shipping, color: RegentaColors.ink, size: 22),
            const SizedBox(width: 8),
            Text(
              'Órdenes de Compra',
              style: RegentaType.seccion.copyWith(
                fontSize: 18,
                color: RegentaColors.ink,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Nueva Orden',
            icon: const Icon(Icons.add, color: RegentaColors.accent),
            onPressed: () => _abrirDialogoNuevaOrden(context, ref),
          ),
          IconButton(
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: () {
              ref.invalidate(listaOrdenesComprasProvider);
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      floatingActionButton: FloatingActionButton.extended(
        onPressed: () => _abrirDialogoNuevaOrden(context, ref),
        backgroundColor: RegentaColors.accent,
        foregroundColor: Colors.white,
        icon: const Icon(Icons.add),
        label: const Text('Nueva Orden'),
      ),
      body: ordenesAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline, size: 48, color: RegentaColors.crit),
                const SizedBox(height: 16),
                Text(
                  'No se pudieron cargar las órdenes de compra: $err',
                  textAlign: TextAlign.center,
                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                ),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: () {
                    ref.invalidate(listaOrdenesComprasProvider);
                  },
                  child: const Text('Reintentar'),
                ),
              ],
            ),
          ),
        ),
        data: (remotas) {
          final ordenes = [...locales, ...remotas];
          if (ordenes.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(28),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(18),
                      decoration: const BoxDecoration(
                        color: RegentaColors.sunken,
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.local_shipping_outlined, size: 48, color: RegentaColors.accent),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'No hay órdenes de compra registradas.',
                      style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Gestiona los pedidos de abastecimiento a tus proveedores y recibe mercancía en bodega.',
                      textAlign: TextAlign.center,
                      style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                    ),
                    const SizedBox(height: 20),
                    Wrap(
                      spacing: 12,
                      runSpacing: 8,
                      alignment: WrapAlignment.center,
                      children: [
                        FilledButton.icon(
                          style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
                          onPressed: () => _abrirDialogoNuevaOrden(context, ref),
                          icon: const Icon(Icons.add, size: 18),
                          label: const Text('Crear Orden de Compra'),
                        ),
                        OutlinedButton.icon(
                          onPressed: () => _crearOrdenDemo(ref),
                          icon: const Icon(Icons.bolt, size: 18),
                          label: const Text('Generar Orden Demo'),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
            );
          }

          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: ordenes.length,
            separatorBuilder: (context, index) => const SizedBox(height: 8),
            itemBuilder: (context, i) {
              final oc = ordenes[i];
              final badgeColor = _colorEstado(oc.estado);

              return Card(
                elevation: 0,
                color: RegentaColors.surface,
                shape: const RoundedRectangleBorder(
                  borderRadius: BorderRadius.all(Radius.circular(8)),
                  side: BorderSide(color: RegentaColors.line),
                ),
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  leading: CircleAvatar(
                    backgroundColor: badgeColor.withValues(alpha: 0.15),
                    child: Icon(Icons.receipt, color: badgeColor, size: 20),
                  ),
                  title: Row(
                    children: [
                      Text(
                        oc.numero.isEmpty ? 'Orden sin número' : oc.numero,
                        style: RegentaType.cuerpo.copyWith(
                          fontWeight: FontWeight.bold,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: badgeColor.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: badgeColor.withValues(alpha: 0.4)),
                        ),
                        child: Text(
                          oc.estado.toUpperCase(),
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: badgeColor,
                          ),
                        ),
                      ),
                    ],
                  ),
                  subtitle: Text(
                    oc.observaciones ?? (oc.fechaEmision != null ? 'Fecha: ${oc.fechaEmision}' : 'Sin observaciones'),
                    style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted),
                  ),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        formatearPesos(oc.total),
                        style: RegentaType.cuerpo.copyWith(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                          color: RegentaColors.ink,
                        ),
                      ),
                      const SizedBox(width: 12),
                      FilledButton.tonal(
                        style: FilledButton.styleFrom(
                          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                          visualDensity: VisualDensity.compact,
                        ),
                        onPressed: () => onOrdenSeleccionada(oc.id),
                        child: const Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            Icon(Icons.move_to_inbox, size: 16),
                            SizedBox(width: 4),
                            Text('Recibir'),
                          ],
                        ),
                      ),
                    ],
                  ),
                  onTap: () => onOrdenSeleccionada(oc.id),
                ),
              );
            },
          );
        },
      ),
    );
  }

  Color _colorEstado(String estado) {
    switch (estado.toUpperCase()) {
      case 'RECIBIDA':
        return RegentaColors.ok;
      case 'CANCELADA':
        return RegentaColors.crit;
      case 'APROBADA':
      case 'PARCIAL':
        return RegentaColors.accent;
      default:
        return RegentaColors.warn;
    }
  }
}

  void _crearOrdenDemo(WidgetRef ref) {
    final nueva = OrdenResumenVista(
      id: 'demo-oc-${DateTime.now().millisecondsSinceEpoch}',
      numero: 'OC-2026-${(DateTime.now().millisecondsSinceEpoch % 900) + 100}',
      estado: 'APROBADA',
      total: 850000,
      fechaEmision: DateTime.now().toIso8601String().substring(0, 10),
      observaciones: 'Distribuidora Central S.A.S. - 15 items recibibles',
    );
    ref.read(ordenesCreadasLocalProvider.notifier).update((prev) => [nueva, ...prev]);
  }

  Future<void> _abrirDialogoNuevaOrden(BuildContext context, WidgetRef ref) async {
    final numCtrl = TextEditingController(text: 'OC-2026-${(DateTime.now().millisecondsSinceEpoch % 900) + 100}');
    final provCtrl = TextEditingController(text: 'Distribuidora Andina S.A.');
    final totalCtrl = TextEditingController(text: '650000');

    await showDialog<void>(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        title: Text('Nueva Orden de Compra', style: RegentaType.seccion.copyWith(color: RegentaColors.ink)),
        content: SingleChildScrollView(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: provCtrl,
                style: const TextStyle(color: RegentaColors.ink),
                decoration: const InputDecoration(labelText: 'Proveedor *'),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: numCtrl,
                style: const TextStyle(color: RegentaColors.ink),
                decoration: const InputDecoration(labelText: 'Número de orden *'),
              ),
              const SizedBox(height: 10),
              TextField(
                controller: totalCtrl,
                keyboardType: TextInputType.number,
                style: const TextStyle(color: RegentaColors.ink),
                decoration: const InputDecoration(labelText: 'Total estimado (\$ COP) *'),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(dialogCtx).pop(),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
            onPressed: () {
              final prov = provCtrl.text.trim();
              final num = numCtrl.text.trim();
              final total = double.tryParse(totalCtrl.text.trim()) ?? 0;
              if (prov.isEmpty || num.isEmpty) return;
              Navigator.of(dialogCtx).pop();

              final nueva = OrdenResumenVista(
                id: 'local-${DateTime.now().millisecondsSinceEpoch}',
                numero: num,
                estado: 'APROBADA',
                total: total,
                fechaEmision: DateTime.now().toIso8601String().substring(0, 10),
                observaciones: prov,
              );
              ref.read(ordenesCreadasLocalProvider.notifier).update((prev) => [nueva, ...prev]);
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(content: Text('Orden de compra $num registrada con éxito.')),
              );
            },
            child: const Text('Crear Orden'),
          ),
        ],
      ),
    );
  }
