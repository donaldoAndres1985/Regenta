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

class PantallaCompras extends ConsumerWidget {
  const PantallaCompras({
    super.key,
    required this.onOrdenSeleccionada,
  });

  final ValueChanged<String> onOrdenSeleccionada;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordenesAsync = ref.watch(listaOrdenesComprasProvider);

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
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: () {
              ref.invalidate(listaOrdenesComprasProvider);
            },
          ),
          const SizedBox(width: 8),
        ],
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
        data: (ordenes) {
          if (ordenes.isEmpty) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.inventory_2_outlined, size: 48, color: RegentaColors.faint),
                  const SizedBox(height: 16),
                  Text(
                    'No hay órdenes de compra registradas.',
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                  ),
                ],
              ),
            );
          }

          return ListView.separated(
            padding: const EdgeInsets.all(16),
            itemCount: ordenes.length,
            separatorBuilder: (_, __) => const SizedBox(height: 8),
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
