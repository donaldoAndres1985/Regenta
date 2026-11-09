import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';
import '../datos/orden_compra_resumen.dart';
import '../recepcion/proveedores.dart';
import '../ui/formato.dart';

final listaOrdenesFutureProvider =
    FutureProvider.autoDispose<List<OrdenCompraResumen>>((ref) async {
  final repo = ref.watch(repositorioDeRecepcionesProvider);
  return repo.listarOrdenes();
});

class PantallaCompras extends ConsumerWidget {
  const PantallaCompras({
    super.key,
    required this.onRecibirOrden,
  });

  final ValueChanged<String> onRecibirOrden;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ordenesAsync = ref.watch(listaOrdenesFutureProvider);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Row(
          children: [
            const Icon(Icons.shopping_bag_outlined, color: RegentaColors.ink, size: 22),
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
            onPressed: () => ref.refresh(listaOrdenesFutureProvider),
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
                  'No se pudieron cargar las órdenes de compra:\n$err',
                  textAlign: TextAlign.center,
                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                ),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: () => ref.refresh(listaOrdenesFutureProvider),
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
              final ord = ordenes[i];
              final colorBadge = _colorEstado(ord.estado);

              return Card(
                elevation: 0,
                color: RegentaColors.surface,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(8),
                  side: const BorderSide(color: RegentaColors.line),
                ),
                child: ListTile(
                  contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                  leading: CircleAvatar(
                    backgroundColor: colorBadge.withOpacity(0.15),
                    child: Icon(Icons.receipt, color: colorBadge, size: 20),
                  ),
                  title: Row(
                    children: [
                      Text(
                        ord.numero.isEmpty ? 'Orden sin número' : ord.numero,
                        style: RegentaType.cuerpo.copyWith(
                          fontWeight: FontWeight.bold,
                          fontSize: 15,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: colorBadge.withOpacity(0.12),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: colorBadge.withOpacity(0.4)),
                        ),
                        child: Text(
                          ord.estado.toUpperCase(),
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w600,
                            color: colorBadge,
                          ),
                        ),
                      ),
                    ],
                  ),
                  subtitle: Text(
                    'Creada: ${_fecha(ord.creadaEn)}',
                    style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted),
                  ),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        formatearPesos(ord.total),
                        style: RegentaType.cuerpo.copyWith(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                          color: RegentaColors.ink,
                        ),
                      ),
                      const SizedBox(width: 12),
                      FilledButton.tonal(
                        onPressed: () => onRecibirOrden(ord.id),
                        child: const Text('Recibir'),
                      ),
                    ],
                  ),
                ),
              );
            },
          );
        },
      ),
    );
  }

  String _fecha(DateTime d) =>
      '${d.year}-${d.month.toString().padLeft(2, '0')}-${d.day.toString().padLeft(2, '0')}';

  Color _colorEstado(String estado) {
    switch (estado.toUpperCase()) {
      case 'APROBADA':
      case 'ENVIADA':
        return RegentaColors.info;
      case 'RECIBIDA':
        return RegentaColors.ok;
      case 'CANCELADA':
        return RegentaColors.crit;
      default:
        return RegentaColors.warn;
    }
  }
}
