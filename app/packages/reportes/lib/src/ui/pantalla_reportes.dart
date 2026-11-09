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
  int _pestanaSeleccionada = 0;

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
            // Selector de pestaña
            SegmentedButton<int>(
              segments: const [
                ButtonSegment(
                  value: 0,
                  label: Text('Turnos de Caja'),
                  icon: Icon(Icons.point_of_sale, size: 18),
                ),
                ButtonSegment(
                  value: 1,
                  label: Text('Ventas y Finanzas'),
                  icon: Icon(Icons.show_chart, size: 18),
                ),
              ],
              selected: {_pestanaSeleccionada},
              onSelectionChanged: (val) {
                setState(() => _pestanaSeleccionada = val.first);
              },
            ),
            const SizedBox(height: 16),

            // Filtros de fecha comunes
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

            if (_pestanaSeleccionada == 1) ...[
              _construirDashboardVentas(context),
            ] else ...[
              // Métricas de Caja
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
              const SizedBox(height: 16),

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
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Text(
                            'Historial de Sesiones de Caja',
                            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink),
                          ),
                          if (ctrl.sesiones.isEmpty)
                            TextButton.icon(
                              onPressed: ctrl.agregarSesionDemo,
                              icon: const Icon(Icons.bolt, size: 16),
                              label: const Text('Simular Turno'),
                            ),
                        ],
                      ),
                      const Divider(color: RegentaColors.line, height: 24),
                      if (ctrl.sesiones.isEmpty)
                        Padding(
                          padding: const EdgeInsets.symmetric(vertical: 20),
                          child: Center(
                            child: Column(
                              children: [
                                const Icon(Icons.receipt_long_outlined, size: 40, color: RegentaColors.faint),
                                const SizedBox(height: 12),
                                Text(
                                  'No hay turnos cerrados en este período.',
                                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                                ),
                                const SizedBox(height: 12),
                                Wrap(
                                  spacing: 10,
                                  children: [
                                    ElevatedButton.icon(
                                      onPressed: ctrl.agregarSesionDemo,
                                      icon: const Icon(Icons.add, size: 16),
                                      label: const Text('Simular Cierre de Caja'),
                                    ),
                                  ],
                                ),
                              ],
                            ),
                          ),
                        )
                      else
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
          ],
        ),
      ),
    );
  }


  Widget _construirDashboardVentas(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // KPIs de ventas
        LayoutBuilder(
          builder: (context, constraints) {
            final esEstrecho = constraints.maxWidth < 600;
            final ancho = (constraints.maxWidth - 24) / 3;

            final kpis = [
              _MetricaReporte(
                titulo: 'Ventas Totales',
                valor: '\$ 2.845.000',
                color: RegentaColors.accent,
                icono: Icons.attach_money,
              ),
              _MetricaReporte(
                titulo: 'Ticket Promedio',
                valor: '\$ 38.500',
                color: RegentaColors.ok,
                icono: Icons.shopping_basket_outlined,
              ),
              _MetricaReporte(
                titulo: 'Transacciones',
                valor: '74 pedidos',
                color: RegentaColors.ink,
                icono: Icons.receipt_outlined,
              ),
            ];

            if (esEstrecho) {
              return Column(
                children: kpis.map((k) => Padding(padding: const EdgeInsets.only(bottom: 8), child: k)).toList(),
              );
            }
            return Row(
              children: kpis.map((k) => SizedBox(width: ancho, child: Padding(padding: const EdgeInsets.symmetric(horizontal: 4), child: k))).toList(),
            );
          },
        ),
        const SizedBox(height: 16),

        // Medios de pago y Top productos
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
                Text('Medios de Pago', style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
                const SizedBox(height: 12),
                _filaMedioPago('Efectivo', '\$ 1.479.400', 0.52, const Color(0xFF10B981)),
                const SizedBox(height: 8),
                _filaMedioPago('Tarjeta Débito / Crédito', '\$ 1.024.200', 0.36, const Color(0xFF3B82F6)),
                const SizedBox(height: 8),
                _filaMedioPago('Transferencias (Nequi / Daviplata)', '\$ 341.400', 0.12, const Color(0xFF8B5CF6)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),

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
                Text('Top 5 Productos Más Vendidos', style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
                const SizedBox(height: 12),
                _filaTopProducto('1. Hamburguesa Clásica Artesanal', '42 unidades', '\$ 924.000'),
                const Divider(height: 16, color: RegentaColors.line),
                _filaTopProducto('2. Cerveza Club Colombia Dorada', '38 unidades', '\$ 342.000'),
                const Divider(height: 16, color: RegentaColors.line),
                _filaTopProducto('3. Pizza Margarita Personal', '26 unidades', '\$ 468.000'),
                const Divider(height: 16, color: RegentaColors.line),
                _filaTopProducto('4. Papas Rústicas Especiales', '22 unidades', '\$ 242.000'),
                const Divider(height: 16, color: RegentaColors.line),
                _filaTopProducto('5. Gaseosa Postobón 400ml', '20 unidades', '\$ 100.000'),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _filaMedioPago(String nombre, String monto, double proporcion, Color color) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(nombre, style: RegentaType.cuerpo.copyWith(fontWeight: FontWeight.w500, color: RegentaColors.ink)),
            Text(monto, style: RegentaType.cuerpo.copyWith(fontWeight: FontWeight.w600, color: RegentaColors.ink)),
          ],
        ),
        const SizedBox(height: 4),
        ClipRRect(
          borderRadius: BorderRadius.circular(4),
          child: LinearProgressIndicator(
            value: proporcion,
            minHeight: 8,
            backgroundColor: const Color(0xFFF1F5F9),
            valueColor: AlwaysStoppedAnimation<Color>(color),
          ),
        ),
      ],
    );
  }

  Widget _filaTopProducto(String nombre, String cantidad, String total) {
    return Row(
      children: [
        Expanded(
          child: Text(nombre, style: RegentaType.cuerpo.copyWith(fontWeight: FontWeight.w500, color: RegentaColors.ink)),
        ),
        Text(cantidad, style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.ink2)),
        const SizedBox(width: 16),
        Text(total, style: RegentaType.dinero.copyWith(fontSize: 14, color: RegentaColors.accent)),
      ],
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
