import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/factura_resumen_vista.dart';
import '../factura/proveedores.dart';
import 'formato.dart';

final listaFacturasFutureProvider =
    FutureProvider.autoDispose<List<FacturaResumenVista>>((ref) {
  final repo = ref.watch(repositorioDeFacturasProvider);
  return repo.listar();
});

final facturasCreadasLocalProvider =
    StateProvider<List<FacturaResumenVista>>((ref) => []);

class PantallaListaFacturas extends ConsumerWidget {
  const PantallaListaFacturas({
    super.key,
    required this.onFacturaSeleccionada,
  });

  final ValueChanged<String> onFacturaSeleccionada;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final facturasAsync = ref.watch(listaFacturasFutureProvider);
    final locales = ref.watch(facturasCreadasLocalProvider);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Row(
          children: [
            const Icon(Icons.receipt_long, color: RegentaColors.ink, size: 22),
            const SizedBox(width: 8),
            Text(
              'Facturas Electrónicas',
              style: RegentaType.seccion.copyWith(
                fontSize: 18,
                color: RegentaColors.ink,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Factura Demo',
            icon: const Icon(Icons.post_add, color: RegentaColors.accent),
            onPressed: () => _generarFacturaDemo(ref),
          ),
          IconButton(
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: () {
              ref.invalidate(listaFacturasFutureProvider);
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: facturasAsync.when(
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
                  'No se pudieron cargar las facturas:\n$err',
                  textAlign: TextAlign.center,
                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                ),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: () {
                    ref.invalidate(listaFacturasFutureProvider);
                  },
                  child: const Text('Reintentar'),
                ),
              ],
            ),
          ),
        ),
        data: (remotas) {
          final facturas = [...locales, ...remotas];
          if (facturas.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(28),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Container(
                      padding: const EdgeInsets.all(18),
                      decoration: const BoxDecoration(
                        color: Color(0xFFF1F5F9),
                        shape: BoxShape.circle,
                      ),
                      child: const Icon(Icons.receipt_long_outlined, size: 48, color: RegentaColors.accent),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      'No hay facturas emitidas todavía.',
                      style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink),
                    ),
                    const SizedBox(height: 8),
                    Text(
                      'Módulo de Facturación Electrónica DIAN. Emite facturas fiscales válidas con CUFE y código QR desde las ventas.',
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
                          onPressed: () => _generarFacturaDemo(ref),
                          icon: const Icon(Icons.bolt, size: 18),
                          label: const Text('Generar Factura Demo'),
                        ),
                        OutlinedButton.icon(
                          onPressed: () {
                            Navigator.of(context).pushNamed('/pos');
                          },
                          icon: const Icon(Icons.point_of_sale, size: 18),
                          label: const Text('Ir al POS'),
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
            itemCount: facturas.length,
            separatorBuilder: (context, index) => const SizedBox(height: 8),
            itemBuilder: (context, i) {
              final fac = facturas[i];
              final badgeColor = _colorEstado(fac.estado);

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
                    child: Icon(Icons.description, color: badgeColor, size: 20),
                  ),
                  title: Row(
                    children: [
                      Text(
                        fac.numeroCompleto.isEmpty ? 'Factura sin número' : fac.numeroCompleto,
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
                          fac.estado.toUpperCase(),
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
                    'Origen: ${fac.origenTipo}',
                    style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted),
                  ),
                  trailing: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        formatearPesos(fac.total),
                        style: RegentaType.cuerpo.copyWith(
                          fontWeight: FontWeight.bold,
                          fontSize: 16,
                          color: RegentaColors.ink,
                        ),
                      ),
                      const SizedBox(width: 8),
                      const Icon(Icons.chevron_right, color: RegentaColors.muted),
                    ],
                  ),
                  onTap: () => onFacturaSeleccionada(fac.id),
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
      case 'ACEPTADA':
        return RegentaColors.ok;
      case 'RECHAZADA':
      case 'ANULADA':
        return RegentaColors.crit;
      case 'ENVIADA':
      case 'FIRMADA':
        return RegentaColors.info;
      default:
        return RegentaColors.warn;
    }
  }
}

  void _generarFacturaDemo(WidgetRef ref) {
    final sec = (DateTime.now().millisecondsSinceEpoch % 9000) + 1000;
    final nueva = FacturaResumenVista(
      id: 'demo-fac-${DateTime.now().millisecondsSinceEpoch}',
      numeroCompleto: 'SETP-$sec',
      origenTipo: 'VENTA_POS',
      estado: 'AUTORIZADA',
      total: 128500,
    );
    ref.read(facturasCreadasLocalProvider.notifier).update((prev) => [nueva, ...prev]);
  }
