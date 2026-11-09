import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../caja/controlador_de_caja.dart';
import '../caja/datos/sesion_caja_modelos.dart';
import '../caja/estado_de_caja.dart';
import '../caja/proveedores.dart';
import 'formato.dart';

class PantallaCaja extends ConsumerStatefulWidget {
  const PantallaCaja({super.key});

  @override
  ConsumerState<PantallaCaja> createState() => _PantallaCajaState();
}

class _PantallaCajaState extends ConsumerState<PantallaCaja> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(controladorDeCajaProvider.notifier).cargar();
    });
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeCajaProvider);
    final ctrl = ref.read(controladorDeCajaProvider.notifier);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Row(
          children: [
            const Icon(Icons.point_of_sale, color: RegentaColors.ink, size: 22),
            const SizedBox(width: 8),
            Text(
              'Gestión de Caja y Turnos',
              style: RegentaType.seccion.copyWith(
                fontSize: 18,
                color: RegentaColors.ink,
              ),
            ),
          ],
        ),
        actions: [
          if (estado.cajas.isNotEmpty)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8, horizontal: 8),
              child: DropdownButton<String>(
                value: estado.cajaSeleccionada?.id,
                underline: const SizedBox.shrink(),
                items: estado.cajas
                    .map(
                      (c) => DropdownMenuItem(
                        value: c.id,
                        child: Text(
                          c.nombre.isEmpty ? c.codigo : c.nombre,
                          style: RegentaType.cuerpo.copyWith(
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                    )
                    .toList(),
                onChanged: (id) {
                  if (id != null) {
                    final seleccionada = estado.cajas.firstWhere((c) => c.id == id);
                    ctrl.seleccionarCaja(seleccionada);
                  }
                },
              ),
            ),
          IconButton(
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: () {
              ctrl.cargar();
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: estado.cargando && estado.sesionActiva == null
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (estado.error != null)
                    Container(
                      margin: const EdgeInsets.only(bottom: 16),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: RegentaColors.critSoft,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: RegentaColors.crit.withValues(alpha: 0.3)),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.error_outline, color: RegentaColors.crit),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              estado.error!,
                              style: RegentaType.cuerpo.copyWith(color: RegentaColors.crit),
                            ),
                          ),
                        ],
                      ),
                    ),
                  if (estado.sesionActiva == null)
                    _TarjetaSinSesion(
                      cajaNombre: estado.cajaSeleccionada?.nombre ?? 'Caja Principal',
                      onAbrir: (monto) => ctrl.abrirSesion(monto),
                    )
                    else ...[
                    _ResumenDeTurno(estado: estado),
                    const SizedBox(height: 16),
                    _AccionesDeTurno(
                      onIngreso: (monto, concepto) => ctrl.registrarIngreso(monto, concepto),
                      onRetiro: (monto, concepto) => ctrl.registrarRetiro(monto, concepto),
                      onCierre: (monto) => ctrl.cerrarSesion(monto),
                    ),
                    const SizedBox(height: 24),
                    _ListaDeMovimientos(movimientos: estado.movimientos),
                  ],
                ],
              ),
            ),
    );
  }
}

class _TarjetaSinSesion extends StatelessWidget {
  const _TarjetaSinSesion({
    required this.cajaNombre,
    required this.onAbrir,
  });

  final String cajaNombre;
  final ValueChanged<num> onAbrir;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: RegentaColors.surface,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.all(Radius.circular(12)),
        side: BorderSide(color: RegentaColors.line),
      ),
      child: Padding(
        padding: const EdgeInsets.all(32),
        child: Column(
          children: [
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: RegentaColors.moduloCaja,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.lock_clock,
                size: 48,
                color: RegentaColors.accent,
              ),
            ),
            const SizedBox(height: 16),
            Text(
              'Turno Cerrado en $cajaNombre',
              style: RegentaType.seccion.copyWith(fontSize: 18),
            ),
            const SizedBox(height: 8),
            Text(
              'Abre una nueva sesión indicando la base de efectivo inicial para empezar a operar.',
              textAlign: TextAlign.center,
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: RegentaColors.accent,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              ),
              icon: const Icon(Icons.key),
              label: const Text('Abrir Turno de Caja'),
              onPressed: () => _mostrarDialogoApertura(context),
            ),
          ],
        ),
      ),
    );
  }

  void _mostrarDialogoApertura(BuildContext context) {
    final ctrl = TextEditingController(text: '100000');
    unawaited(
      showDialog<void>(
        context: context,
        builder: (dialogCtx) => AlertDialog(
          title: const Text('Apertura de Turno'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Ingrese la base de efectivo inicial en caja:'),
              const SizedBox(height: 12),
              TextField(
                controller: ctrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Base Inicial (\$)',
                  border: OutlineInputBorder(),
                  prefixText: '\$ ',
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(dialogCtx).pop(),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: () {
                final monto = num.tryParse(ctrl.text.trim()) ?? 0;
                Navigator.of(dialogCtx).pop();
                onAbrir(monto);
              },
              child: const Text('Confirmar Apertura'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ResumenDeTurno extends StatelessWidget {
  const _ResumenDeTurno({required this.estado});

  final EstadoDeCaja estado;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final ancho = (constraints.maxWidth - 24) / 4;
        final esEstrecho = constraints.maxWidth < 650;

        final items = [
          _TarjetaMetrica(
            titulo: 'Base Inicial',
            valor: formatearPesos(estado.sesionActiva?.montoApertura ?? 0),
            color: RegentaColors.ink,
            icono: Icons.account_balance_wallet_outlined,
          ),
          _TarjetaMetrica(
            titulo: 'Ingresos (+)',
            valor: formatearPesos(estado.totalIngresos),
            color: RegentaColors.ok,
            icono: Icons.arrow_downward,
          ),
          _TarjetaMetrica(
            titulo: 'Retiros (-)',
            valor: formatearPesos(estado.totalRetiros),
            color: RegentaColors.crit,
            icono: Icons.arrow_upward,
          ),
          _TarjetaMetrica(
            titulo: 'Efectivo Esperado',
            valor: formatearPesos(estado.efectivoEsperado),
            color: RegentaColors.accent,
            icono: Icons.monetization_on_outlined,
            destacado: true,
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
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: items.map((it) => SizedBox(width: ancho, child: it)).toList(),
        );
      },
    );
  }
}

class _TarjetaMetrica extends StatelessWidget {
  const _TarjetaMetrica({
    required this.titulo,
    required this.valor,
    required this.color,
    required this.icono,
    this.destacado = false,
  });

  final String titulo;
  final String valor;
  final Color color;
  final IconData icono;
  final bool destacado;

  @override
  Widget build(BuildContext context) {
    return Card(
      elevation: 0,
      color: destacado ? RegentaColors.moduloCaja : RegentaColors.surface,
      shape: RoundedRectangleBorder(
        borderRadius: const BorderRadius.all(Radius.circular(10)),
        side: BorderSide(
          color: destacado ? RegentaColors.accent.withValues(alpha: 0.3) : RegentaColors.line,
        ),
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
              style: RegentaType.dinero.copyWith(
                fontSize: 18,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AccionesDeTurno extends StatelessWidget {
  const _AccionesDeTurno({
    required this.onIngreso,
    required this.onRetiro,
    required this.onCierre,
  });

  final void Function(num monto, String concepto) onIngreso;
  final void Function(num monto, String concepto) onRetiro;
  final ValueChanged<num> onCierre;

  @override
  Widget build(BuildContext context) {
    return Wrap(
      spacing: 12,
      runSpacing: 8,
      children: [
        OutlinedButton.icon(
          icon: const Icon(Icons.add_circle_outline, color: RegentaColors.ok),
          label: const Text('Registrar Ingreso'),
          onPressed: () => _mostrarDialogoMovimiento(context, esIngreso: true),
        ),
        OutlinedButton.icon(
          icon: const Icon(Icons.remove_circle_outline, color: RegentaColors.crit),
          label: const Text('Registrar Retiro / Gasto'),
          onPressed: () => _mostrarDialogoMovimiento(context, esIngreso: false),
        ),
        FilledButton.tonalIcon(
          icon: const Icon(Icons.lock_outline),
          label: const Text('Cerrar Turno (Arqueo)'),
          onPressed: () => _mostrarDialogoCierre(context),
        ),
      ],
    );
  }

  void _mostrarDialogoMovimiento(BuildContext context, {required bool esIngreso}) {
    final montoCtrl = TextEditingController();
    final conceptoCtrl = TextEditingController();

    unawaited(
      showDialog<void>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: Text(esIngreso ? 'Registrar Ingreso' : 'Registrar Retiro / Gasto'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: montoCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Monto (\$)',
                  border: OutlineInputBorder(),
                  prefixText: '\$ ',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: conceptoCtrl,
                decoration: const InputDecoration(
                  labelText: 'Concepto / Motivo',
                  border: OutlineInputBorder(),
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: () {
                final monto = num.tryParse(montoCtrl.text.trim()) ?? 0;
                final concepto = conceptoCtrl.text.trim();
                Navigator.of(ctx).pop();
                if (esIngreso) {
                  onIngreso(monto, concepto);
                } else {
                  onRetiro(monto, concepto);
                }
              },
              child: const Text('Guardar'),
            ),
          ],
        ),
      ),
    );
  }

  void _mostrarDialogoCierre(BuildContext context) {
    final declaradoCtrl = TextEditingController();

    unawaited(
      showDialog<void>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('Cierre y Arqueo de Caja'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text('Ingrese el monto físico total contado en la caja:'),
              const SizedBox(height: 12),
              TextField(
                controller: declaradoCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Efectivo Contado (\$)',
                  border: OutlineInputBorder(),
                  prefixText: '\$ ',
                ),
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: () {
                final monto = num.tryParse(declaradoCtrl.text.trim()) ?? 0;
                Navigator.of(ctx).pop();
                onCierre(monto);
              },
              child: const Text('Confirmar Cierre'),
            ),
          ],
        ),
      ),
    );
  }
}

class _ListaDeMovimientos extends StatelessWidget {
  const _ListaDeMovimientos({required this.movimientos});

  final List<MovimientoCajaVista> movimientos;

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
                const Icon(Icons.swap_vert, size: 20, color: RegentaColors.ink),
                const SizedBox(width: 8),
                Text(
                  'Movimientos del Turno (${movimientos.length})',
                  style: RegentaType.seccion.copyWith(fontSize: 16),
                ),
              ],
            ),
            const Divider(color: RegentaColors.line, height: 24),
            if (movimientos.isEmpty)
              Padding(
                padding: const EdgeInsets.all(24),
                child: Center(
                  child: Text(
                    'No hay movimientos registrados en este turno todavía.',
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted),
                  ),
                ),
              )
            else
              ListView.separated(
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                itemCount: movimientos.length,
                separatorBuilder: (_, __) => const Divider(color: RegentaColors.line, height: 1),
                itemBuilder: (context, i) {
                  final mov = movimientos[i];
                  final esIngreso = mov.tipo.toUpperCase() == 'INGRESO' ||
                      mov.tipo.toUpperCase() == 'VENTA' ||
                      mov.tipo.toUpperCase() == 'APERTURA';
                  final color = esIngreso ? RegentaColors.ok : RegentaColors.crit;

                  return ListTile(
                    contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                    leading: CircleAvatar(
                      radius: 16,
                      backgroundColor: color.withValues(alpha: 0.12),
                      child: Icon(
                        esIngreso ? Icons.arrow_downward : Icons.arrow_upward,
                        size: 16,
                        color: color,
                      ),
                    ),
                    title: Text(
                      mov.concepto.isEmpty ? mov.tipo : mov.concepto,
                      style: RegentaType.cuerpo.copyWith(fontWeight: FontWeight.w600),
                    ),
                    subtitle: Text(
                      'Tipo: ${mov.tipo}',
                      style: RegentaType.etiqueta,
                    ),
                    trailing: Text(
                      '${esIngreso ? '+' : '-'} ${formatearPesos(mov.monto)}',
                      style: RegentaType.dinero.copyWith(
                        fontSize: 15,
                        color: color,
                      ),
                    ),
                  );
                },
              ),
          ],
        ),
      ),
    );
  }
}
