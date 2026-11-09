import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../caja/lista_de_sesiones_de_caja.dart';
import '../caja/proveedores.dart';
import '../caja/sesion_de_caja_vista.dart';

class PantallaReportes extends ConsumerStatefulWidget {
  const PantallaReportes({super.key});

  @override
  ConsumerState<PantallaReportes> createState() => _PantallaReportesState();
}

class _PantallaReportesState extends ConsumerState<PantallaReportes> {
  int _rangoDias = 7;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      _cargar();
    });
  }

  void _cargar() {
    final hasta = DateTime.now();
    final desde = hasta.subtract(Duration(days: _rangoDias));
    unawaited(ref.read(controladorDeReportesDeCajaProvider).cargar(desde: desde, hasta: hasta));
  }

  @override
  Widget build(BuildContext context) {
    final ctrl = ref.watch(controladorDeReportesDeCajaProvider);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Row(
          children: [
            const Icon(Icons.analytics_outlined, color: RegentaColors.ink, size: 22),
            const SizedBox(width: 8),
            Text(
              'Reportes Operativos',
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
            onPressed: _cargar,
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            // Filtros de fecha
            Row(
              children: [
                _filtroChip(etiqueta: 'Hoy', dias: 0),
                const SizedBox(width: 8),
                _filtroChip(etiqueta: 'Últimos 7 días', dias: 7),
                const SizedBox(width: 8),
                _filtroChip(etiqueta: 'Últimos 30 días', dias: 30),
              ],
            ),
            const SizedBox(height: 16),

            // Métricas
            LayoutBuilder(
              builder: (context, constraints) {
                final esEstrecho = constraints.maxWidth < 600;
                final ancho = (constraints.maxWidth - 16) / 3;

                final items = [
                  _MetricaReporte(
                    titulo: 'Turnos Evaluados',
                    valor: '${ctrl.sesiones.length}',
                    color: RegentaColors.ink,
                    icono: Icons.event_note,
                  ),
                  _MetricaReporte(
                    titulo: 'Turnos Cuadrados',
                    valor: '${ctrl.sesiones.length - ctrl.descuadres}',
                    color: RegentaColors.ok,
                    icono: Icons.check_circle_outline,
                  ),
                  _MetricaReporte(
                    titulo: 'Turnos Descuadrados',
                    valor: '${ctrl.descuadres}',
                    color: ctrl.descuadres > 0 ? RegentaColors.crit : RegentaColors.muted,
                    icono: Icons.warning_amber_rounded,
                  ),
                ];

                if (esEstrecho) {
                  return Column(
                    children: items
                        .map((it) => Padding(padding: const EdgeInsets.only(bottom: 8), child: it))
                        .toList(),
                  );
                }

                return Row(
                  children: items
                      .map((it) => SizedBox(width: ancho, child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: it)))
                      .toList(),
                );
              },
            ),
            const SizedBox(height: 24),

            // Listado de sesiones
            Card(
              elevation: 0,
              color: RegentaColors.surface,
              shape: const RoundedRectangleBorder(
                borderRadius: BorderRadius.all(Radius.circular(10)),
                side: BorderSide(color: RegentaColors.line),
              ),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Historial de Sesiones de Caja',
                      style: RegentaType.seccion.copyWith(fontSize: 16),
                    ),
                    const Divider(color: RegentaColors.line, height: 24),
                    ListaDeSesionesDeCaja(
                      controlador: ctrl,
                      onAbrirSesion: (sesion) {
                        _mostrarDetalleSesion(context, sesion);
                      },
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _filtroChip({required String etiqueta, required int dias}) {
    final seleccionado = _rangoDias == dias;
    return ChoiceChip(
      label: Text(etiqueta),
      selected: seleccionado,
      onSelected: (val) {
        if (val) {
          setState(() => _rangoDias = dias);
          _cargar();
        }
      },
    );
  }

  void _mostrarDetalleSesion(BuildContext context, SesionDeCajaVista sesion) {
    unawaited(
      showDialog<void>(
        context: context,
        builder: (dialogCtx) => AlertDialog(
          title: Text('Detalle de Turno ${sesion.numero}'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Fecha apertura: ${sesion.abiertaEn}'),
              if (sesion.cerradaEn != null) Text('Fecha cierre: ${sesion.cerradaEn}'),
              const SizedBox(height: 8),
              Text('Monto esperado: \$ ${sesion.montoEsperado ?? 0}'),
              Text('Monto declarado: \$ ${sesion.montoDeclarado ?? 0}'),
              const SizedBox(height: 8),
              Text(
                'Diferencia: \$ ${sesion.diferencia ?? 0}',
                style: TextStyle(
                  fontWeight: FontWeight.bold,
                  color: sesion.descuadrada ? RegentaColors.crit : RegentaColors.ok,
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogCtx).pop(),
              child: const Text('Cerrar'),
            ),
          ],
        ),
      ),
    );
  }
}

class _MetricaReporte extends StatelessWidget {
  const _MetricaReporte({
    required this.titulo,
    required this.valor,
    required this.color,
    required this.icono,
  });

  final String titulo;
  final String valor;
  final Color color;
  final IconData icono;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: RegentaColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(10)),
        side: BorderSide(color: RegentaColors.line),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                Icon(icono, size: 16, color: color),
                const SizedBox(width: 6),
                Expanded(
                  child: Text(
                    titulo,
                    style: RegentaType.cuerpo.copyWith(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: RegentaColors.muted,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 8),
            Text(
              valor,
              style: RegentaType.dinero.copyWith(fontSize: 20, color: color),
            ),
          ],
        ),
      ),
    );
  }
}
