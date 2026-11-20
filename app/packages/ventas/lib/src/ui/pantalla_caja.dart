import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../caja/datos/sesion_caja_modelos.dart';
import '../caja/estado_de_caja.dart';
import '../caja/proveedores.dart';
import 'dialogo_de_cierre_por_metodo.dart';
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

  /// HU-140 + HU-142: cierra y pregunta si se imprime la tirilla de cierre.
  Future<void> _cerrarEImprimir(Map<String, num> contados) async {
    final ctrl = ref.read(controladorDeCajaProvider.notifier);
    final sesionId = await ctrl.cerrarSesion(contados);
    if (sesionId == null || !mounted) return;
    final imprimir = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('¿Imprimir tirilla de cierre?'),
        content: const Text('Sale por la impresora térmica configurada.'),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('No')),
          FilledButton(onPressed: () => Navigator.of(ctx).pop(true), child: const Text('Imprimir')),
        ],
      ),
    );
    if (imprimir != true) return;
    try {
      final tirilla = await ctrl.tirillaDeCierre(sesionId);
      await ref.read(impresoraDeTirillaProvider)(tirilla);
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('No se pudo imprimir la tirilla: $e'),
          backgroundColor: RegentaColors.crit,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeCajaProvider);
    final ctrl = ref.read(controladorDeCajaProvider.notifier);

    final acciones = <Widget>[
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
                      style: RegentaType.cuerpo.copyWith(fontWeight: FontWeight.w600),
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
    ];
    return LayoutBuilder(
      builder: (context, afuera) => Scaffold(
        backgroundColor: RegentaColors.paper,
        // Encabezado común en web (design/comportamiento/Encabezado.md); en móvil, la barra.
        appBar: afuera.maxWidth >= kBreakpointEscritorio
            ? EncabezadoDeSeccion(
                icono: Icons.payments_outlined,
                subtitulo: 'Abre el turno, registra movimientos y cierra con arqueo',
                titulo: 'Gestión de Caja y Turnos',
                acciones: acciones,
              )
            : AppBar(
                backgroundColor: RegentaColors.surface,
                surfaceTintColor: RegentaColors.surface,
                elevation: 0,
                shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
                title: Row(
                  children: [
                    const Icon(Icons.point_of_sale, color: RegentaColors.ink, size: 22),
                    const SizedBox(width: 8),
                    Flexible(
                      child: Text(
                        'Gestión de Caja y Turnos',
                        overflow: TextOverflow.ellipsis,
                        style: RegentaType.seccion.copyWith(fontSize: 18, color: RegentaColors.ink),
                      ),
                    ),
                  ],
                ),
                actions: [...acciones, const SizedBox(width: 8)],
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
                        cargarArqueo: ctrl.arqueoPorMetodo,
                        onCierre: (contados) => _cerrarEImprimir(contados),
                      ),
                      const SizedBox(height: 24),
                      _ListaDeMovimientos(
                        movimientos: estado.movimientos,
                        puedeAnular: ref.watch(puedeAnularMovimientosDeCajaProvider),
                        onAnular: (id, motivo) => ctrl.anularMovimiento(id, motivo),
                      ),
                    ],
                  ],
                ),
              ),
      ),
    );
  }
}

class _TarjetaSinSesion extends StatelessWidget {
  const _TarjetaSinSesion({required this.cajaNombre, required this.onAbrir});

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
              decoration: BoxDecoration(color: RegentaColors.moduloCaja, shape: BoxShape.circle),
              child: const Icon(Icons.lock_clock, size: 48, color: RegentaColors.accent),
            ),
            const SizedBox(height: 16),
            Text('Turno Cerrado en $cajaNombre', style: RegentaType.seccion.copyWith(fontSize: 18)),
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
            Text(valor, style: RegentaType.dinero.copyWith(fontSize: 18, color: color)),
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
    required this.cargarArqueo,
    required this.onCierre,
  });

  final void Function(num monto, String concepto) onIngreso;
  final void Function(num monto, String concepto) onRetiro;
  final Future<List<LineaDeArqueoVista>> Function() cargarArqueo;
  final ValueChanged<Map<String, num>> onCierre;

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
          onPressed: () => unawaited(_mostrarDialogoCierre(context)),
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
            TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancelar')),
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

  /// HU-140 · el cierre se hace por método de pago (R22 y R23 de SesionDeCaja.md).
  Future<void> _mostrarDialogoCierre(BuildContext context) async {
    final lineas = await cargarArqueo();
    if (!context.mounted || lineas.isEmpty) return;
    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Cierre y Arqueo de Caja'),
        content: SizedBox(
          width: 560,
          child: SingleChildScrollView(
            child: DialogoDeCierrePorMetodo(
              lineas: lineas,
              onConfirmar: (contados) {
                Navigator.of(ctx).pop();
                onCierre(contados);
              },
            ),
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancelar')),
        ],
      ),
    );
  }
}

class _ListaDeMovimientos extends StatelessWidget {
  const _ListaDeMovimientos({
    required this.movimientos,
    required this.puedeAnular,
    required this.onAnular,
  });

  final List<MovimientoCajaVista> movimientos;
  final bool puedeAnular;
  final void Function(String movimientoId, String motivo) onAnular;

  /// HU-141: pide el motivo; sin motivo no se confirma (I6).
  Future<void> _pedirMotivo(BuildContext context, MovimientoCajaVista mov) async {
    final motivo = await showDialog<String>(
      context: context,
      builder: (ctx) => _DialogoDeAnulacion(movimiento: mov),
    );
    if (motivo != null && motivo.trim().isNotEmpty) onAnular(mov.id, motivo.trim());
  }

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
                separatorBuilder: (context, index) =>
                    const Divider(color: RegentaColors.line, height: 1),
                itemBuilder: (context, i) {
                  final mov = movimientos[i];
                  final anulados = {
                    for (final m in movimientos)
                      if (m.anulaA != null) m.anulaA!,
                  };
                  final anulado = anulados.contains(mov.id);
                  final esAnulacion = mov.tipo.toUpperCase() == 'ANULACION';
                  final esIngreso =
                      mov.tipo.toUpperCase() == 'INGRESO' ||
                      mov.tipo.toUpperCase() == 'VENTA' ||
                      mov.tipo.toUpperCase() == 'APERTURA' ||
                      (esAnulacion &&
                          movimientos.any(
                            (m) => m.id == mov.anulaA && m.tipo.toUpperCase() != 'INGRESO',
                          ));
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
                    subtitle: Row(
                      children: [
                        Text('Tipo: ${mov.tipo}', style: RegentaType.etiqueta),
                        if (anulado) ...[
                          const SizedBox(width: 8),
                          Container(
                            key: ValueKey('anulado-${mov.id}'),
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: RegentaColors.critSoft,
                              borderRadius: BorderRadius.circular(4),
                            ),
                            child: Text(
                              'ANULADO',
                              style: RegentaType.etiqueta.copyWith(color: RegentaColors.crit),
                            ),
                          ),
                        ],
                      ],
                    ),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text(
                          '${esIngreso ? '+' : '-'} ${formatearPesos(mov.monto)}',
                          style: RegentaType.dinero.copyWith(
                            fontSize: 15,
                            color: color,
                            decoration: anulado ? TextDecoration.lineThrough : null,
                          ),
                        ),
                        if (puedeAnular && mov.esManual && !anulado)
                          IconButton(
                            key: ValueKey('anular-${mov.id}'),
                            tooltip: 'Anular',
                            icon: const Icon(Icons.undo, color: RegentaColors.crit),
                            onPressed: () => _pedirMotivo(context, mov),
                          ),
                      ],
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

/// HU-141 · pide el motivo de la anulación. Sin motivo no se puede confirmar.
class _DialogoDeAnulacion extends StatefulWidget {
  const _DialogoDeAnulacion({required this.movimiento});

  final MovimientoCajaVista movimiento;

  @override
  State<_DialogoDeAnulacion> createState() => _DialogoDeAnulacionState();
}

class _DialogoDeAnulacionState extends State<_DialogoDeAnulacion> {
  final _motivo = TextEditingController();

  @override
  void dispose() {
    _motivo.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final mov = widget.movimiento;
    return AlertDialog(
      title: const Text('Anular movimiento'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(
            '${mov.tipo} · ${formatearPesos(mov.monto)} · ${mov.concepto}',
            style: RegentaType.cuerpo,
          ),
          const SizedBox(height: 4),
          Text(
            'Se registra un movimiento que lo compensa; el original no se borra.',
            style: RegentaType.etiqueta,
          ),
          const SizedBox(height: 12),
          TextField(
            key: const ValueKey('motivo-anulacion'),
            controller: _motivo,
            decoration: const InputDecoration(labelText: 'Motivo', border: OutlineInputBorder()),
            onChanged: (_) => setState(() {}),
          ),
        ],
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar')),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: RegentaColors.crit),
          onPressed: _motivo.text.trim().isEmpty
              ? null
              : () => Navigator.of(context).pop(_motivo.text.trim()),
          child: const Text('Confirmar anulación'),
        ),
      ],
    );
  }
}
