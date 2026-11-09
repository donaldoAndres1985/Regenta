import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';
import '../caja/controlador_de_caja.dart';
import '../caja/datos/sesion_caja_modelos.dart';
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
      ref.read(controladorDeCajaProvider.notifier).inicializar();
    });
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeCajaProvider);

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
              'Gestión de Caja',
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
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: DropdownButton<CajaVista>(
                value: estado.cajaSeleccionada,
                underline: const SizedBox(),
                items: estado.cajas.map((c) {
                  return DropdownMenuItem<CajaVista>(
                    value: c,
                    child: Text(
                      '${c.nombre} (${c.codigo})',
                      style: RegentaType.cuerpo.copyWith(
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.ink,
                      ),
                    ),
                  );
                }).toList(),
                onChanged: (nueva) {
                  if (nueva != null) {
                    ref.read(controladorDeCajaProvider.notifier).cambiarCaja(nueva);
                  }
                },
              ),
            ),
          IconButton(
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: () {
              ref.read(controladorDeCajaProvider.notifier).inicializar();
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Builder(
        builder: (context) {
          if (estado.cargando && estado.cajas.isEmpty) {
            return const Center(child: CircularProgressIndicator());
          }

          if (estado.error != null && estado.cajas.isEmpty) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const Icon(Icons.error_outline, size: 48, color: RegentaColors.crit),
                    const SizedBox(height: 16),
                    Text(
                      estado.error!,
                      textAlign: TextAlign.center,
                      style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                    ),
                    const SizedBox(height: 16),
                    ElevatedButton(
                      onPressed: () =>
                          ref.read(controladorDeCajaProvider.notifier).inicializar(),
                      child: const Text('Reintentar'),
                    ),
                  ],
                ),
              ),
            );
          }

          if (estado.cajas.isEmpty) {
            return Center(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.inbox_outlined, size: 48, color: RegentaColors.faint),
                  const SizedBox(height: 16),
                  Text(
                    'No hay puntos de cobro (cajas) configurados en el negocio.',
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                  ),
                ],
              ),
            );
          }

          if (!estado.tieneSesionAbierta) {
            return _VistaCajaCerrada(
              caja: estado.cajaSeleccionada!,
              onAbrir: (monto) async {
                final exito = await ref
                    .read(controladorDeCajaProvider.notifier)
                    .abrirTurno(monto);
                if (context.mounted && !exito) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text(estado.error ?? 'Error al abrir caja'),
                      backgroundColor: RegentaColors.crit,
                    ),
                  );
                }
              },
            );
          }

          return _VistaCajaAbierta(
            sesion: estado.sesionActiva!,
            movimientos: estado.movimientos,
            totalIngresos: estado.totalIngresos,
            totalRetiros: estado.totalRetiros,
            efectivoEsperado: estado.efectivoEsperadoCalculado,
            onNuevoIngreso: (monto, concepto) => ref
                .read(controladorDeCajaProvider.notifier)
                .registrarIngreso(monto, concepto),
            onNuevoRetiro: (monto, concepto, tipo) => ref
                .read(controladorDeCajaProvider.notifier)
                .registrarRetiro(
                  monto: monto,
                  concepto: concepto,
                  tipo: tipo,
                ),
            onCerrarCaja: (montoDeclarado) async {
              final cerrada = await ref
                  .read(controladorDeCajaProvider.notifier)
                  .cerrarTurno(montoDeclarado);
              if (context.mounted && cerrada != null) {
                final descuadre = cerrada.diferencia ?? 0;
                showDialog(
                  context: context,
                  builder: (ctx) => AlertDialog(
                    title: const Text('Caja cerrada con éxito'),
                    content: Text(
                      'Número de turno: ${cerrada.numero}\n'
                      'Monto esperado: ${formatearPesos(cerrada.montoEsperado ?? 0)}\n'
                      'Monto contado: ${formatearPesos(cerrada.montoDeclarado ?? 0)}\n'
                      'Diferencia: ${formatearPesos(descuadre)} '
                      '(${descuadre == 0 ? "CUADRADA" : "DESCUADRADA"})',
                    ),
                    actions: [
                      TextButton(
                        onPressed: () => Navigator.pop(ctx),
                        child: const Text('Aceptar'),
                      ),
                    ],
                  ),
                );
              }
            },
          );
        },
      ),
    );
  }
}

class _VistaCajaCerrada extends StatefulWidget {
  const _VistaCajaCerrada({
    required this.caja,
    required this.onAbrir,
  });

  final CajaVista caja;
  final ValueChanged<num> onAbrir;

  @override
  State<_VistaCajaCerrada> createState() => _VistaCajaCerradaState();
}

class _VistaCajaCerradaState extends State<_VistaCajaCerrada> {
  final _baseController = TextEditingController(text: '0');

  @override
  void dispose() {
    _baseController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 420),
        child: Card(
          elevation: 0,
          color: RegentaColors.surface,
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(12),
            side: const BorderSide(color: RegentaColors.line),
          ),
          child: Padding(
            padding: const EdgeInsets.all(28),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Center(
                  child: CircleAvatar(
                    radius: 32,
                    backgroundColor: RegentaColors.moduloCaja,
                    child: Icon(Icons.lock_clock, size: 36, color: RegentaColors.accent),
                  ),
                ),
                const SizedBox(height: 20),
                Text(
                  'Turno de Caja Cerrado',
                  textAlign: TextAlign.center,
                  style: RegentaType.titulo.copyWith(fontSize: 20),
                ),
                const SizedBox(height: 8),
                Text(
                  'El punto "${widget.caja.nombre}" no tiene ningún turno activo. Ingresa el monto base de apertura en efectivo para comenzar.',
                  textAlign: TextAlign.center,
                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                ),
                const SizedBox(height: 24),
                TextField(
                  controller: _baseController,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                    labelText: 'Base de apertura (COP)',
                    prefixText: '\$ ',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 24),
                FilledButton.icon(
                  style: FilledButton.styleFrom(
                    backgroundColor: RegentaColors.ok,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  icon: const Icon(Icons.play_arrow),
                  label: const Text(
                    'Abrir Turno de Caja',
                    style: TextStyle(fontWeight: FontWeight.bold),
                  ),
                  onPressed: () {
                    final valor = num.tryParse(_baseController.text) ?? 0;
                    widget.onAbrir(valor);
                  },
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}

class _VistaCajaAbierta extends StatelessWidget {
  const _VistaCajaAbierta({
    required this.sesion,
    required this.movimientos,
    required this.totalIngresos,
    required this.totalRetiros,
    required this.efectivoEsperado,
    required this.onNuevoIngreso,
    required this.onNuevoRetiro,
    required this.onCerrarCaja,
  });

  final SesionCajaActiva sesion;
  final List<MovimientoCajaVista> movimientos;
  final num totalIngresos;
  final num totalRetiros;
  final num efectivoEsperado;
  final Future<bool> Function(num monto, String concepto) onNuevoIngreso;
  final Future<bool> Function(num monto, String concepto, String tipo) onNuevoRetiro;
  final ValueChanged<num> onCerrarCaja;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Resumen superior
          Wrap(
            spacing: 16,
            runSpacing: 16,
            children: [
              _TarjetonMetrica(
                titulo: 'Turno Activo',
                valor: sesion.numero,
                subtitulo: 'Apertura: ${formatearPesos(sesion.montoApertura)}',
                colorFondo: RegentaColors.moduloCaja,
                icono: Icons.verified_user_outlined,
              ),
              _TarjetonMetrica(
                titulo: 'Efectivo en Caja',
                valor: formatearPesos(efectivoEsperado),
                subtitulo: 'Calculado en tiempo real',
                colorFondo: RegentaColors.moduloInventario,
                icono: Icons.payments_outlined,
              ),
              _TarjetonMetrica(
                titulo: 'Movimientos',
                valor: '${movimientos.length}',
                subtitulo: 'Entradas: ${formatearPesos(totalIngresos)} | Retiros: ${formatearPesos(totalRetiros)}',
                colorFondo: RegentaColors.paper,
                icono: Icons.swap_horiz,
              ),
            ],
          ),
          const SizedBox(height: 20),

          // Barra de acciones
          Row(
            children: [
              FilledButton.tonalIcon(
                icon: const Icon(Icons.arrow_downward, color: RegentaColors.ok),
                label: const Text('Ingreso manual'),
                onPressed: () => _mostrarDialogoIngreso(context),
              ),
              const SizedBox(width: 12),
              FilledButton.tonalIcon(
                icon: const Icon(Icons.arrow_upward, color: RegentaColors.crit),
                label: const Text('Retiro / Gasto'),
                onPressed: () => _mostrarDialogoRetiro(context),
              ),
              const Spacer(),
              FilledButton.icon(
                style: FilledButton.styleFrom(
                  backgroundColor: RegentaColors.crit,
                ),
                icon: const Icon(Icons.lock),
                label: const Text('Arqueo y Cierre'),
                onPressed: () => _mostrarDialogoCierre(context),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Listado de movimientos
          Expanded(
            child: Card(
              elevation: 0,
              color: RegentaColors.surface,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(8),
                side: const BorderSide(color: RegentaColors.line),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                    decoration: const BoxDecoration(
                      border: Border(bottom: BorderSide(color: RegentaColors.line)),
                    ),
                    child: Text(
                      'Movimientos del Turno',
                      style: RegentaType.seccion.copyWith(fontSize: 15),
                    ),
                  ),
                  Expanded(
                    child: movimientos.isEmpty
                        ? Center(
                            child: Text(
                              'Aún no hay movimientos registrados en este turno.',
                              style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted),
                            ),
                          )
                        : ListView.separated(
                            itemCount: movimientos.length,
                            separatorBuilder: (_, __) =>
                                const Divider(height: 1, color: RegentaColors.line),
                            itemBuilder: (context, i) {
                              final mov = movimientos[i];
                              final esPositivo = mov.signo > 0;
                              return ListTile(
                                dense: true,
                                leading: Icon(
                                  esPositivo ? Icons.add_circle_outline : Icons.remove_circle_outline,
                                  color: esPositivo ? RegentaColors.ok : RegentaColors.crit,
                                ),
                                title: Text(
                                  mov.concepto.isEmpty ? mov.tipo : mov.concepto,
                                  style: RegentaType.cuerpo.copyWith(fontWeight: FontWeight.w600),
                                ),
                                subtitle: Text(
                                  '${mov.tipo} · ${mov.metodoPago} · ${_hora(mov.creadoEn)}',
                                  style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted),
                                ),
                                trailing: Text(
                                  '${esPositivo ? '+' : '-'}${formatearPesos(mov.monto)}',
                                  style: RegentaType.cuerpo.copyWith(
                                    fontWeight: FontWeight.bold,
                                    color: esPositivo ? RegentaColors.ok : RegentaColors.crit,
                                    fontSize: 15,
                                  ),
                                ),
                              );
                            },
                          ),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  String _hora(DateTime d) =>
      '${d.hour.toString().padLeft(2, '0')}:${d.minute.toString().padLeft(2, '0')}';

  void _mostrarDialogoIngreso(BuildContext context) {
    final montoCtrl = TextEditingController();
    final conceptoCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Registrar Ingreso Manual'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            TextField(
              controller: montoCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Monto en efectivo', prefixText: '\$ '),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: conceptoCtrl,
              decoration: const InputDecoration(labelText: 'Concepto / Motivo'),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () async {
              final monto = num.tryParse(montoCtrl.text) ?? 0;
              final concepto = conceptoCtrl.text.trim();
              if (monto > 0 && concepto.isNotEmpty) {
                Navigator.pop(ctx);
                await onNuevoIngreso(monto, concepto);
              }
            },
            child: const Text('Registrar'),
          ),
        ],
      ),
    );
  }

  void _mostrarDialogoRetiro(BuildContext context) {
    final montoCtrl = TextEditingController();
    final conceptoCtrl = TextEditingController();
    String tipo = 'RETIRO';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDlgState) => AlertDialog(
          title: const Text('Registrar Salida de Efectivo'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              DropdownButtonFormField<String>(
                value: tipo,
                decoration: const InputDecoration(labelText: 'Tipo de salida'),
                items: const [
                  DropdownMenuItem(value: 'RETIRO', child: Text('Retiro a caja general')),
                  DropdownMenuItem(value: 'GASTO', child: Text('Gasto menor del turno')),
                ],
                onChanged: (val) {
                  if (val != null) setDlgState(() => tipo = val);
                },
              ),
              const SizedBox(height: 12),
              TextField(
                controller: montoCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(labelText: 'Monto a retirar', prefixText: '\$ '),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: conceptoCtrl,
                decoration: const InputDecoration(labelText: 'Concepto obligatorio'),
              ),
            ],
          ),
          actions: [
            TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
            FilledButton(
              onPressed: () async {
                final monto = num.tryParse(montoCtrl.text) ?? 0;
                final concepto = conceptoCtrl.text.trim();
                if (monto > 0 && concepto.isNotEmpty) {
                  Navigator.pop(ctx);
                  await onNuevoRetiro(monto, concepto, tipo);
                }
              },
              child: const Text('Confirmar Retiro'),
            ),
          ],
        ),
      ),
    );
  }

  void _mostrarDialogoCierre(BuildContext context) {
    final conteoCtrl = TextEditingController(text: efectivoEsperado.round().toString());

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Arqueo y Cierre de Turno'),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Efectivo esperado según el sistema: ${formatearPesos(efectivoEsperado)}',
              style: const TextStyle(fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 12),
            TextField(
              controller: conteoCtrl,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(
                labelText: 'Monto en efectivo contado (declarado)',
                prefixText: '\$ ',
                helperText: 'Cualquier diferencia generará un turno DESCUADRADO',
              ),
            ),
          ],
        ),
        actions: [
          TextButton(onPressed: () => Navigator.pop(ctx), child: const Text('Cancelar')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: RegentaColors.crit),
            onPressed: () {
              final monto = num.tryParse(conteoCtrl.text) ?? 0;
              Navigator.pop(ctx);
              onCerrarCaja(monto);
            },
            child: const Text('Cerrar Turno Definitivamente'),
          ),
        ],
      ),
    );
  }
}

class _TarjetonMetrica extends StatelessWidget {
  const _TarjetonMetrica({
    required this.titulo,
    required this.valor,
    required this.subtitulo,
    required this.colorFondo,
    required this.icono,
  });

  final String titulo;
  final String valor;
  final String subtitulo;
  final Color colorFondo;
  final IconData icono;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: 240,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: colorFondo,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Icon(icono, size: 20, color: RegentaColors.ink2),
              const SizedBox(width: 8),
              Text(
                titulo,
                style: RegentaType.cuerpo.copyWith(
                  fontWeight: FontWeight.w600,
                  fontSize: 13,
                  color: RegentaColors.ink2,
                ),
              ),
            ],
          ),
          const SizedBox(height: 8),
          Text(
            valor,
            style: RegentaType.titulo.copyWith(fontSize: 22, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          Text(
            subtitulo,
            style: RegentaType.cuerpo.copyWith(fontSize: 11, color: RegentaColors.muted),
          ),
        ],
      ),
    );
  }
}
