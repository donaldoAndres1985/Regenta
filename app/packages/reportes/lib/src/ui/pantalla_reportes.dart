import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';
import '../caja/controlador_de_reportes_de_caja.dart';
import '../caja/lista_de_sesiones_de_caja.dart';
import '../caja/repositorio_de_reportes_de_caja.dart';
import '../caja/sesion_de_caja_vista.dart';

final controladorReportesCajaProvider =
    ChangeNotifierProvider<ControladorDeReportesDeCaja>((ref) {
  final http = ref.watch(clienteHttpProvider);
  final repo = RepositorioDeReportesDeCaja(http);
  final hoy = DateTime.now();
  final inicio = DateTime(hoy.year, hoy.month, 1);
  return ControladorDeReportesDeCaja(
    repo,
    desde: inicio,
    hasta: hoy,
  );
});

class PantallaReportes extends ConsumerStatefulWidget {
  const PantallaReportes({super.key});

  @override
  ConsumerState<PantallaReportes> createState() => _PantallaReportesState();
}

class _PantallaReportesState extends ConsumerState<PantallaReportes> {
  int _tabSeleccionada = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(controladorReportesCajaProvider).cargar();
    });
  }

  @override
  Widget build(BuildContext context) {
    final ctrlCaja = ref.watch(controladorReportesCajaProvider);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Row(
          children: [
            const Icon(Icons.bar_chart, color: RegentaColors.ink, size: 22),
            const SizedBox(width: 8),
            Text(
              'Reportes y Análisis',
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
            onPressed: () => ctrlCaja.cargar(),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Column(
        children: [
          Container(
            color: RegentaColors.surface,
            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
            child: Row(
              children: [
                ChoiceChip(
                  label: const Text('Cierres de Caja'),
                  selected: _tabSeleccionada == 0,
                  onSelected: (val) {
                    if (val) setState(() => _tabSeleccionada = 0);
                  },
                ),
                const SizedBox(width: 8),
                ChoiceChip(
                  label: const Text('Resumen General'),
                  selected: _tabSeleccionada == 1,
                  onSelected: (val) {
                    if (val) setState(() => _tabSeleccionada = 1);
                  },
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: RegentaColors.line),
          Expanded(
            child: _tabSeleccionada == 0
                ? SingleChildScrollView(
                    padding: const EdgeInsets.all(16),
                    child: Card(
                      elevation: 0,
                      color: RegentaColors.surface,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                        side: const BorderSide(color: RegentaColors.line),
                      ),
                      child: Padding(
                        padding: const EdgeInsets.all(8),
                        child: ListaDeSesionesDeCaja(
                          controlador: ctrlCaja,
                          onAbrirSesion: (sesion) => _mostrarDetalleSesion(context, sesion),
                        ),
                      ),
                    ),
                  )
                : Center(
                    child: Padding(
                      padding: const EdgeInsets.all(24),
                      child: Column(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.analytics_outlined, size: 48, color: RegentaColors.faint),
                          const SizedBox(height: 16),
                          Text(
                            'Reportes ejecutivos de ventas y rotación de inventario',
                            style: RegentaType.seccion.copyWith(fontSize: 16),
                          ),
                          const SizedBox(height: 8),
                          Text(
                            'Visualiza el consolidado del mes a través de los reportes del panel principal de Inicio.',
                            textAlign: TextAlign.center,
                            style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                          ),
                        ],
                      ),
                    ),
                  ),
          ),
        ],
      ),
    );
  }

  void _mostrarDetalleSesion(BuildContext context, SesionDeCajaVista s) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Turno ${s.numero}'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Estado: ${s.estado.name.toUpperCase()}'),
            const SizedBox(height: 4),
            Text('Apertura: ${s.abiertaEn.toIso8601String().substring(0, 16)}'),
            if (s.cerradaEn != null)
              Text('Cierre: ${s.cerradaEn!.toIso8601String().substring(0, 16)}'),
            const Divider(),
            Text('Monto esperado: \$ ${s.montoEsperado ?? 0}'),
            Text('Monto declarado: \$ ${s.montoDeclarado ?? 0}'),
            Text('Diferencia: \$ ${s.diferencia ?? 0}'),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cerrar')),
        ],
      ),
    );
  }
}
