import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/cliente_de_la_venta.dart';
import '../datos/producto_buscado.dart';
import '../pos/estado_del_pos.dart';
import '../pos/linea_de_carrito.dart';
import '../pos/proveedores.dart';
import 'formato.dart';
import 'pantalla_cliente_de_venta.dart';

/// La pantalla de Nueva venta (POS). HU-045 y `design/comportamiento/POS.md`.
class PantallaPos extends ConsumerStatefulWidget {
  const PantallaPos({super.key, this.onAgregarCliente, this.onVentaCobrada});

  final VoidCallback? onAgregarCliente;
  final void Function(String numeroVenta)? onVentaCobrada;

  @override
  ConsumerState<PantallaPos> createState() => _PantallaPosState();
}

class _PantallaPosState extends ConsumerState<PantallaPos> {
  final TextEditingController _buscador = TextEditingController();
  final FocusNode _foco = FocusNode(debugLabel: 'buscador-pos');
  int _resaltado = 0;

  @override
  void initState() {
    super.initState();
    HardwareKeyboard.instance.addHandler(_atajoGlobal);
  }

  @override
  void dispose() {
    HardwareKeyboard.instance.removeHandler(_atajoGlobal);
    _buscador.dispose();
    _foco.dispose();
    super.dispose();
  }

  bool _atajoGlobal(KeyEvent evento) {
    if (evento is! KeyDownEvent || !mounted) return false;
    if (!(ModalRoute.of(context)?.isCurrent ?? true)) return false;
    if (evento.logicalKey == LogicalKeyboardKey.f2) {
      _enfocarBuscador();
      return true;
    }
    if (evento.logicalKey == LogicalKeyboardKey.f12) {
      _cobrar();
      return true;
    }
    return false;
  }

  void _enfocarBuscador() => _foco.requestFocus();

  Future<void> _cobrar() async {
    final pos = ref.read(controladorDelPosProvider.notifier);
    if (ref.read(controladorDelPosProvider).puedeCobrar) await pos.cobrar();
  }

  KeyEventResult _teclaEnBuscador(FocusNode _, KeyEvent evento) {
    if (evento is! KeyDownEvent && evento is! KeyRepeatEvent) {
      return KeyEventResult.ignored;
    }
    final estado = ref.read(controladorDelPosProvider);
    final hay = estado.resultados.length;
    final tecla = evento.logicalKey;
    if (tecla == LogicalKeyboardKey.arrowDown && hay > 0) {
      setState(() => _resaltado = (_resaltado + 1).clamp(0, hay - 1));
      return KeyEventResult.handled;
    }
    if (tecla == LogicalKeyboardKey.arrowUp && hay > 0) {
      setState(() => _resaltado = (_resaltado - 1).clamp(0, hay - 1));
      return KeyEventResult.handled;
    }
    if (tecla == LogicalKeyboardKey.escape) {
      ref.read(controladorDelPosProvider.notifier).cerrarDesplegable();
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  void _cambiarDeVenta(void Function() accion) {
    _buscador.clear();
    setState(() => _resaltado = 0);
    accion();
  }

  Future<void> _confirmarCerrarVenta(int indice) async {
    final estado = ref.read(controladorDelPosProvider);
    final pos = ref.read(controladorDelPosProvider.notifier);
    if (estado.productosEnVenta(indice) > 0) {
      final confirmar = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          title: const Text('¿Cerrar venta?'),
          content: Text(
            'La venta ${indice + 1} tiene ${estado.productosEnVenta(indice)} productos. '
            'Si la cierras, se perderán estos productos.',
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              style: FilledButton.styleFrom(backgroundColor: RegentaColors.crit),
              child: const Text('Cerrar venta'),
            ),
          ],
        ),
      );
      if (confirmar != true) return;
    }
    _cambiarDeVenta(() => pos.cerrarVenta(indice));
  }

  Future<void> _abrirSelectorDeCliente({required bool escritorio}) async {
    if (widget.onAgregarCliente != null) {
      widget.onAgregarCliente!();
      return;
    }
    final pos = ref.read(controladorDelPosProvider.notifier);
    final asignado = ref.read(controladorDelPosProvider).cliente;
    Widget selector(BuildContext contexto) => PantallaClienteDeVenta(
      asignado: asignado,
      onAsignado: (cliente) {
        pos.fijarCliente(cliente);
        Navigator.of(contexto).pop();
      },
      onCerrar: () => Navigator.of(contexto).pop(),
    );
    if (escritorio) {
      await showDialog<void>(
        context: context,
        builder: (contexto) => Dialog(
          clipBehavior: Clip.antiAlias,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          child: SizedBox(width: 560, height: 640, child: selector(contexto)),
        ),
      );
    } else {
      await Navigator.of(context).push(MaterialPageRoute<void>(builder: selector));
    }
    if (mounted) _enfocarBuscador();
  }

  Future<void> _abrirDialogoDescuento() async {
    final estado = ref.read(controladorDelPosProvider);
    final pos = ref.read(controladorDelPosProvider.notifier);

    String tipo = 'pct'; // 'pct' o 'val'
    int objetivo = -1; // -1: Toda la venta, >= 0: índice de línea
    final controller = TextEditingController();

    await showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDialogState) {
          final valor = num.tryParse(controller.text.trim()) ?? 0;
          num descCalc = 0;
          if (objetivo == -1) {
            descCalc = tipo == 'pct' ? (estado.subtotalBruto * (valor / 100)) : valor;
          } else if (objetivo < estado.lineas.length) {
            final l = estado.lineas[objetivo];
            descCalc = tipo == 'pct' ? (l.bruto * (valor / 100)) : valor;
          }
          final nuevoTotal = (estado.subtotalBruto - descCalc + estado.impuesto).clamp(0, double.infinity);

          return AlertDialog(
            title: const Text('Aplicar descuento'),
            content: SizedBox(
              width: 400,
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Aplicar a:', style: TextStyle(fontWeight: FontWeight.w600)),
                  const SizedBox(height: 6),
                  DropdownButtonFormField<int>(
                    initialValue: objetivo,
                    isExpanded: true,
                    decoration: const InputDecoration(border: OutlineInputBorder()),
                    items: [
                      const DropdownMenuItem(value: -1, child: Text('Toda la venta')),
                      for (var i = 0; i < estado.lineas.length; i++)
                        DropdownMenuItem(
                          value: i,
                          child: Text(
                            '${estado.lineas[i].producto.nombre} (${formatearPesos(estado.lineas[i].bruto)})',
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                    ],
                    onChanged: (v) => setDialogState(() => objetivo = v ?? -1),
                  ),
                  const SizedBox(height: 14),
                  const Text('Tipo de descuento:', style: TextStyle(fontWeight: FontWeight.w600)),
                  const SizedBox(height: 6),
                  SegmentedButton<String>(
                    segments: const [
                      ButtonSegment(value: 'pct', label: Text('Porcentaje (%)')),
                      ButtonSegment(value: 'val', label: Text('Valor (\$)')),
                    ],
                    selected: {tipo},
                    onSelectionChanged: (s) => setDialogState(() => tipo = s.first),
                  ),
                  const SizedBox(height: 14),
                  TextField(
                    controller: controller,
                    keyboardType: const TextInputType.numberWithOptions(decimal: true),
                    decoration: InputDecoration(
                      labelText: tipo == 'pct' ? 'Porcentaje de descuento' : 'Valor en pesos',
                      prefixText: tipo == 'pct' ? '% ' : '\$ ',
                      border: const OutlineInputBorder(),
                    ),
                    onChanged: (_) => setDialogState(() {}),
                  ),
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: RegentaColors.paper,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: RegentaColors.line),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text('Nuevo total estimado:'),
                        Text(
                          formatearPesos(nuevoTotal),
                          style: RegentaType.codigo.copyWith(fontWeight: FontWeight.w700),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            actions: [
              TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancelar')),
              FilledButton(
                onPressed: () {
                  final val = num.tryParse(controller.text.trim()) ?? 0;
                  if (objetivo == -1) {
                    if (tipo == 'pct') {
                      final valPesos = estado.subtotalBruto * (val / 100);
                      pos.aplicarDescuentoGlobal(porcentaje: val, valor: valPesos);
                    } else {
                      pos.aplicarDescuentoGlobal(valor: val);
                    }
                  } else {
                    final pct = tipo == 'pct' ? val : (estado.lineas[objetivo].bruto > 0 ? (val / estado.lineas[objetivo].bruto * 100) : 0);
                    pos.aplicarDescuentoLinea(objetivo, pct);
                  }
                  Navigator.of(ctx).pop();
                },
                child: const Text('Aplicar'),
              ),
            ],
          );
        },
      ),
    );
  }

  Future<void> _abrirDialogoNota() async {
    final estado = ref.read(controladorDelPosProvider);
    final pos = ref.read(controladorDelPosProvider.notifier);
    final controller = TextEditingController(text: estado.nota ?? '');

    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Nota de la venta'),
        content: SizedBox(
          width: 440,
          child: TextField(
            controller: controller,
            maxLines: 3,
            decoration: const InputDecoration(
              hintText: 'Ej: entregar en obra, calle 25 # 18-40',
              border: OutlineInputBorder(),
            ),
          ),
        ),
        actions: [
          if (estado.nota != null)
            TextButton(
              onPressed: () {
                pos.fijarNota(null);
                Navigator.of(ctx).pop();
              },
              child: const Text('Borrar nota', style: TextStyle(color: RegentaColors.crit)),
            ),
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () {
              final texto = controller.text.trim();
              pos.fijarNota(texto.isEmpty ? null : texto);
              Navigator.of(ctx).pop();
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDelPosProvider);
    final pos = ref.read(controladorDelPosProvider.notifier);

    ref.listen<EstadoDelPos>(controladorDelPosProvider, (antes, ahora) {
      if (!identical(antes?.resultados, ahora.resultados)) _resaltado = 0;
      if (ahora.termino.isEmpty && _buscador.text.isNotEmpty) {
        _buscador.clear();
        _enfocarBuscador();
      }
      if (ahora.ventaNumero != null && ahora.ventaNumero != antes?.ventaNumero) {
        widget.onVentaCobrada?.call(ahora.ventaNumero!);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Venta ${ahora.ventaNumero} cobrada')),
        );
      } else if (ahora.ventaEnCola && !(antes?.ventaEnCola ?? false)) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Venta guardada sin señal: sube sola cuando vuelva la conexión'),
          ),
        );
      } else if (ahora.mensaje != null && ahora.mensaje != antes?.mensaje) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(ahora.mensaje!)));
      }
    });

    final esEscritorio = MediaQuery.sizeOf(context).width >= kBreakpointEscritorio;

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text(
          'Nueva venta',
          style: PastelTheme.titulo(
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: RegentaColors.ink,
          ),
        ),
        actions: [
          if (esEscritorio)
            _PestanasHeader(
              estado: estado,
              onSeleccionar: (v) => _cambiarDeVenta(() => pos.cambiarDeVenta(v)),
              onCerrar: _confirmarCerrarVenta,
              onNueva: estado.ventasAbiertas >= ventasMaximas
                  ? null
                  : () => _cambiarDeVenta(pos.nuevaVenta),
            )
          else
            _SelectorVentasMovil(
              estado: estado,
              onSeleccionar: (v) => _cambiarDeVenta(() => pos.cambiarDeVenta(v)),
              onNueva: estado.ventasAbiertas >= ventasMaximas
                  ? null
                  : () => _cambiarDeVenta(pos.nuevaVenta),
            ),
        ],
      ),
      body: LayoutBuilder(
        builder: (context, restricciones) {
          final escritorio = restricciones.maxWidth >= kBreakpointEscritorio;
          final desplegable =
              !estado.desplegableCerrado &&
              (estado.buscando || estado.termino.trim().length >= 3);

          final cliente = _FranjaCliente(
            cliente: estado.cliente,
            compacta: !escritorio,
            onAgregar: () => _abrirSelectorDeCliente(escritorio: escritorio),
            onQuitar: () => pos.fijarCliente(null),
          );

          final buscador = _Buscador(
            controlador: _buscador,
            foco: _foco,
            autofocus: escritorio,
            onCambio: pos.cambiarTermino,
            onEnter: () => pos.confirmarBusqueda(resaltado: _resaltado),
            onTecla: _teclaEnBuscador,
          );

          final resultados = Positioned(
            top: 0,
            left: 16,
            right: 16,
            child: ConstrainedBox(
              constraints: BoxConstraints(
                maxHeight: (restricciones.maxHeight * 0.55).clamp(160, 420),
              ),
              child: Material(
                color: RegentaColors.surface,
                elevation: 12,
                shadowColor: RegentaColors.ink.withValues(alpha: 0.25),
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(12),
                  side: const BorderSide(color: RegentaColors.line),
                ),
                clipBehavior: Clip.antiAlias,
                child: _Resultados(
                  estado: estado,
                  resaltado: _resaltado,
                  onAgregar: pos.agregar,
                ),
              ),
            ),
          );

          if (escritorio) {
            return Column(
              key: const Key('pos-escritorio'),
              children: [
                cliente,
                buscador,
                Expanded(
                  child: Stack(
                    children: [
                      Positioned.fill(
                        child: _VentaEscritorio(
                          estado: estado,
                          onMas: (i) => pos.cambiarCantidad(i, 1),
                          onMenos: (i) => pos.cambiarCantidad(i, -1),
                          onFijarCantidad: pos.fijarCantidad,
                          onQuitar: pos.quitar,
                          onVaciar: pos.vaciar,
                          onEspera: () => _cambiarDeVenta(pos.ponerEnEspera),
                          onDescuento: _abrirDialogoDescuento,
                          onNota: _abrirDialogoNota,
                          onCobrar: _cobrar,
                        ),
                      ),
                      if (desplegable) resultados,
                    ],
                  ),
                ),
              ],
            );
          }
          return Column(
            key: const Key('pos-movil'),
            children: [
              cliente,
              buscador,
              Expanded(
                child: Stack(
                  children: [
                    Positioned.fill(
                      child: _VentaMovil(
                        estado: estado,
                        onMas: (i) => pos.cambiarCantidad(i, 1),
                        onMenos: (i) => pos.cambiarCantidad(i, -1),
                        onFijarCantidad: pos.fijarCantidad,
                        onQuitar: pos.quitar,
                        onVaciar: pos.vaciar,
                      ),
                    ),
                    if (desplegable) resultados,
                  ],
                ),
              ),
              _PieMovil(
                estado: estado,
                onEspera: () => _cambiarDeVenta(pos.ponerEnEspera),
                onDescuento: _abrirDialogoDescuento,
                onNota: _abrirDialogoNota,
                onCobrar: _cobrar,
              ),
            ],
          );
        },
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Encabezado: Pestañas de ventas abiertas
// ---------------------------------------------------------------------------

class _PestanasHeader extends StatelessWidget {
  const _PestanasHeader({
    required this.estado,
    required this.onSeleccionar,
    required this.onCerrar,
    required this.onNueva,
  });

  final EstadoDelPos estado;
  final ValueChanged<int> onSeleccionar;
  final ValueChanged<int> onCerrar;
  final VoidCallback? onNueva;

  static const maxTabsVisibles = 5;

  @override
  Widget build(BuildContext context) {
    final totalVentas = estado.ventasAbiertas;
    final visibles = totalVentas > maxTabsVisibles ? maxTabsVisibles : totalVentas;
    final sobrantes = totalVentas > maxTabsVisibles ? totalVentas - maxTabsVisibles : 0;

    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 14),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          for (var v = 0; v < visibles; v++)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: _PestanaVenta(
                numero: v + 1,
                activa: estado.ventaActiva == v,
                total: estado.totalEnVenta(v),
                cliente: estado.clienteEnVenta(v),
                productos: estado.productosEnVenta(v),
                puedeCerrar: totalVentas > 1 && estado.ventaActiva == v,
                onTap: () => onSeleccionar(v),
                onCerrar: () => onCerrar(v),
              ),
            ),
          if (sobrantes > 0)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: PopupMenuButton<int>(
                tooltip: 'Más ventas',
                onSelected: onSeleccionar,
                itemBuilder: (ctx) => [
                  for (var v = visibles; v < totalVentas; v++)
                    PopupMenuItem(
                      value: v,
                      child: Text(
                        'Venta ${v + 1} · ${formatearPesos(estado.totalEnVenta(v))}',
                      ),
                    ),
                ],
                child: Container(
                  height: 44,
                  padding: const EdgeInsets.symmetric(horizontal: 12),
                  decoration: BoxDecoration(
                    color: RegentaColors.surface,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: RegentaColors.line),
                  ),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '+$sobrantes más',
                        style: PastelTheme.cuerpo(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: RegentaColors.ink,
                        ),
                      ),
                      const Icon(Icons.arrow_drop_down, size: 18, color: RegentaColors.ink2),
                    ],
                  ),
                ),
              ),
            ),
          Padding(
            padding: const EdgeInsets.only(left: 8),
            child: _BotonNuevaVenta(onTap: onNueva),
          ),
        ],
      ),
    );
  }
}

class _SelectorVentasMovil extends StatelessWidget {
  const _SelectorVentasMovil({
    required this.estado,
    required this.onSeleccionar,
    required this.onNueva,
  });

  final EstadoDelPos estado;
  final ValueChanged<int> onSeleccionar;
  final VoidCallback? onNueva;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(right: 12),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          PopupMenuButton<int>(
            tooltip: 'Cambiar venta',
            onSelected: onSeleccionar,
            itemBuilder: (ctx) => [
              for (var v = 0; v < estado.ventasAbiertas; v++)
                PopupMenuItem(
                  value: v,
                  child: Text(
                    'Venta ${v + 1}${estado.productosEnVenta(v) > 0 ? ' · ${formatearPesos(estado.totalEnVenta(v))}' : ''}',
                  ),
                ),
            ],
            child: Container(
              height: 44,
              padding: const EdgeInsets.symmetric(horizontal: 10),
              decoration: BoxDecoration(
                color: RegentaColors.accent,
                borderRadius: BorderRadius.circular(10),
              ),
              child: Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(
                    'Venta ${estado.ventaActiva + 1}',
                    style: PastelTheme.cuerpo(
                      fontSize: 13,
                      fontWeight: FontWeight.w700,
                      color: RegentaColors.surface,
                    ),
                  ),
                  const SizedBox(width: 4),
                  const Icon(Icons.arrow_drop_down, size: 18, color: RegentaColors.surface),
                ],
              ),
            ),
          ),
          const SizedBox(width: 8),
          _BotonNuevaVenta(onTap: onNueva),
        ],
      ),
    );
  }
}

class _PestanaVenta extends StatelessWidget {
  const _PestanaVenta({
    required this.numero,
    required this.activa,
    required this.total,
    required this.cliente,
    required this.productos,
    required this.puedeCerrar,
    required this.onTap,
    required this.onCerrar,
  });

  final int numero;
  final bool activa;
  final num total;
  final String? cliente;
  final int productos;
  final bool puedeCerrar;
  final VoidCallback onTap;
  final VoidCallback onCerrar;

  @override
  Widget build(BuildContext context) {
    final info = productos > 0
        ? formatearPesos(total)
        : (cliente != null && cliente!.trim().isNotEmpty ? cliente! : 'Mostrador');

    final etiqueta = productos == 0
        ? 'Venta $numero · $info'
        : 'Venta $numero · $productos ${productos == 1 ? 'producto' : 'productos'} · $info';

    return Semantics(
      button: true,
      selected: activa,
      label: etiqueta,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Material(
            color: activa ? RegentaColors.accent : RegentaColors.surface,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(10),
              side: BorderSide(
                color: activa ? RegentaColors.accent : RegentaColors.line,
              ),
            ),
            child: InkWell(
              key: Key('venta-$numero'),
              onTap: onTap,
              borderRadius: BorderRadius.circular(10),
              child: ConstrainedBox(
                constraints: const BoxConstraints(minHeight: 44, minWidth: 80),
                child: Padding(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  child: Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Text(
                        '$numero',
                        style: PastelTheme.cuerpo(
                          fontSize: 13,
                          fontWeight: FontWeight.w700,
                          color: activa ? RegentaColors.surface : RegentaColors.ink,
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        info,
                        style: (productos > 0 ? RegentaType.codigo : PastelTheme.cuerpo()).copyWith(
                          fontSize: 13,
                          fontWeight: FontWeight.w600,
                          color: activa ? RegentaColors.surface : RegentaColors.ink2,
                        ),
                      ),
                      if (puedeCerrar) ...[
                        const SizedBox(width: 8),
                        InkWell(
                          onTap: onCerrar,
                          borderRadius: BorderRadius.circular(12),
                          child: Padding(
                            padding: const EdgeInsets.all(2),
                            child: Icon(
                              Icons.close,
                              size: 16,
                              color: activa ? RegentaColors.surface : RegentaColors.ink2,
                            ),
                          ),
                        ),
                      ],
                    ],
                  ),
                ),
              ),
            ),
          ),
          if (activa && productos > 0)
            Positioned(
              top: -6,
              right: -6,
              child: Container(
                constraints: const BoxConstraints(minWidth: 20, minHeight: 20),
                padding: const EdgeInsets.symmetric(horizontal: 6),
                decoration: BoxDecoration(
                  color: RegentaColors.warn,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: RegentaColors.surface, width: 2),
                ),
                child: Center(
                  child: Text(
                    '$productos',
                    style: PastelTheme.cuerpo(
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: RegentaColors.surface,
                    ),
                  ),
                ),
              ),
            ),
        ],
      ),
    );
  }
}

class _BotonNuevaVenta extends StatelessWidget {
  const _BotonNuevaVenta({required this.onTap});

  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: 'Abrir otra venta',
      child: Material(
        color: RegentaColors.accentSoft,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        child: InkWell(
          key: const Key('venta-nueva'),
          onTap: onTap,
          borderRadius: BorderRadius.circular(10),
          child: const SizedBox(
            width: 44,
            height: 44,
            child: Icon(Icons.add, size: 22, color: RegentaColors.accent),
          ),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Cliente (R10a)
// ---------------------------------------------------------------------------

class _FranjaCliente extends StatelessWidget {
  const _FranjaCliente({
    required this.cliente,
    required this.compacta,
    required this.onAgregar,
    required this.onQuitar,
  });

  final ClienteDeLaVenta? cliente;
  final bool compacta;
  final VoidCallback onAgregar;
  final VoidCallback onQuitar;

  @override
  Widget build(BuildContext context) {
    final c = cliente;
    final avatar = Container(
      width: 42,
      height: 42,
      decoration: BoxDecoration(
        color: c == null ? RegentaColors.sunken : RegentaColors.accentSoft,
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: c == null
          ? const Icon(Icons.person_outline, size: 22, color: RegentaColors.ink2)
          : Text(
              c.iniciales,
              style: PastelTheme.cuerpo(
                fontSize: 14,
                fontWeight: FontWeight.w800,
                color: RegentaColors.accent,
              ),
            ),
    );

    final datos = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        if (c == null)
          Text(
            'CLIENTE',
            style: PastelTheme.cuerpo(
              fontSize: 10.5,
              fontWeight: FontWeight.w700,
              letterSpacing: 0.8,
              color: RegentaColors.ink2,
            ),
          ),
        Text(
          c?.nombre ?? 'Consumidor final',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: PastelTheme.cuerpo(
            fontSize: 15,
            fontWeight: FontWeight.w700,
            color: RegentaColors.ink,
          ),
        ),
        if (c != null) ...[
          const SizedBox(height: 3),
          Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 8,
            runSpacing: 4,
            children: [
              Text(
                c.documento,
                style: PastelTheme.cuerpo(fontSize: 13, color: RegentaColors.ink2),
              ),
              if (c.condicion != null && c.condicion!.trim().isNotEmpty)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                  decoration: BoxDecoration(
                    color: RegentaColors.okSoft,
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    c.condicion!.startsWith('Crédito') ? c.condicion! : 'Crédito disp. ${c.condicion!}',
                    style: PastelTheme.cuerpo(
                      fontSize: 11.5,
                      fontWeight: FontWeight.w700,
                      color: RegentaColors.ok,
                    ),
                  ),
                ),
            ],
          ),
        ],
      ],
    );

    final botones = Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
        if (c != null)
          OutlinedButton(
            onPressed: onQuitar,
            style: OutlinedButton.styleFrom(
              minimumSize: const Size(44, 44),
              foregroundColor: RegentaColors.ink,
              backgroundColor: RegentaColors.surface,
              side: const BorderSide(color: RegentaColors.line),
              shape: const StadiumBorder(),
            ),
            child: const Text('Quitar'),
          ),
        OutlinedButton.icon(
          onPressed: onAgregar,
          icon: Icon(
            c == null ? Icons.person_add_alt_1_outlined : Icons.swap_horiz,
            size: 18,
          ),
          label: Text(c == null ? 'Agregar cliente' : 'Cambiar cliente'),
          style: OutlinedButton.styleFrom(
            minimumSize: const Size(44, 44),
            backgroundColor: RegentaColors.accentSoft,
            foregroundColor: RegentaColors.accent,
            side: const BorderSide(color: RegentaColors.accent),
            shape: const StadiumBorder(),
          ),
        ),
      ],
    );

    return Container(
      key: const Key('pos-cliente'),
      margin: const EdgeInsets.fromLTRB(16, 12, 16, 0),
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: RegentaColors.line),
      ),
      child: compacta
          ? Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    avatar,
                    const SizedBox(width: 12),
                    Expanded(child: datos),
                  ],
                ),
                const SizedBox(height: 10),
                botones,
              ],
            )
          : Row(
              children: [
                avatar,
                const SizedBox(width: 14),
                Expanded(child: datos),
                const SizedBox(width: 14),
                botones,
              ],
            ),
    );
  }
}

// ---------------------------------------------------------------------------
// Buscador (R5, R7, R7b, R7c)
// ---------------------------------------------------------------------------

class _Buscador extends StatelessWidget {
  const _Buscador({
    required this.controlador,
    required this.foco,
    required this.autofocus,
    required this.onCambio,
    required this.onEnter,
    required this.onTecla,
  });

  final TextEditingController controlador;
  final FocusNode foco;
  final bool autofocus;
  final ValueChanged<String> onCambio;
  final VoidCallback onEnter;
  final FocusOnKeyEventCallback onTecla;

  @override
  Widget build(BuildContext context) {
    final borde = OutlineInputBorder(
      borderRadius: BorderRadius.circular(12),
      borderSide: BorderSide.none,
    );
    return Container(
      key: const Key('pos-buscador'),
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      child: Focus(
        onKeyEvent: onTecla,
        skipTraversal: true,
        child: TextField(
          controller: controlador,
          focusNode: foco,
          autofocus: autofocus,
          onChanged: onCambio,
          textInputAction: TextInputAction.done,
          onEditingComplete: () {},
          onSubmitted: (_) => onEnter(),
          cursorColor: RegentaColors.surface,
          style: PastelTheme.cuerpo(fontSize: 15, color: RegentaColors.surface),
          decoration: InputDecoration(
            filled: true,
            fillColor: RegentaColors.ink,
            isDense: false,
            contentPadding: const EdgeInsets.symmetric(vertical: 16, horizontal: 14),
            prefixIcon: const Icon(
              Icons.search,
              size: 22,
              color: RegentaColors.faint,
            ),
            suffixIcon: const Padding(
              padding: EdgeInsets.only(right: 14),
              child: Center(widthFactor: 1, child: _Atajo('F2', oscuro: true)),
            ),
            hintText: 'Buscar producto, referencia o código de barras…',
            hintStyle: PastelTheme.cuerpo(
              fontSize: 15,
              color: RegentaColors.faint,
            ),
            border: borde,
            enabledBorder: borde,
            focusedBorder: OutlineInputBorder(
              borderRadius: BorderRadius.circular(12),
              borderSide: const BorderSide(color: RegentaColors.accent, width: 2),
            ),
          ),
        ),
      ),
    );
  }
}

class _Atajo extends StatelessWidget {
  const _Atajo(this.tecla, {this.oscuro = false});

  final String tecla;
  final bool oscuro;

  @override
  Widget build(BuildContext context) {
    final color = oscuro ? RegentaColors.faint : RegentaColors.surface;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: color.withValues(alpha: 0.7)),
      ),
      child: Text(
        tecla,
        style: RegentaType.codigo.copyWith(
          fontSize: 11,
          fontWeight: FontWeight.w600,
          color: color,
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Desplegable de Resultados
// ---------------------------------------------------------------------------

class _Resultados extends StatelessWidget {
  const _Resultados({
    required this.estado,
    required this.resaltado,
    required this.onAgregar,
  });

  final EstadoDelPos estado;
  final int resaltado;
  final void Function(ProductoBuscado) onAgregar;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          color: RegentaColors.sunken,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 9),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                '${estado.resultados.length} resultados · Enter agrega el resaltado',
                style: PastelTheme.cuerpo(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: RegentaColors.ink2,
                ),
              ),
              Text(
                '↑ ↓ para moverte · Esc cierra',
                style: PastelTheme.cuerpo(
                  fontSize: 12,
                  color: RegentaColors.ink2,
                ),
              ),
            ],
          ),
        ),
        if (estado.buscando && estado.resultados.isEmpty)
          const Padding(
            padding: EdgeInsets.all(24),
            child: Center(
              child: SizedBox(width: 24, height: 24, child: CircularProgressIndicator()),
            ),
          )
        else if (estado.resultados.isEmpty)
          Padding(
            padding: const EdgeInsets.all(24),
            child: Center(
              child: Text(
                'No se encontró «${estado.termino.trim()}»',
                style: PastelTheme.cuerpo(fontSize: 14, color: RegentaColors.ink2),
              ),
            ),
          )
        else
          Flexible(
            child: ListView.builder(
              key: const Key('pos-resultados'),
              shrinkWrap: true,
              padding: EdgeInsets.zero,
              itemCount: estado.resultados.length,
              itemBuilder: (context, i) {
                final p = estado.resultados[i];
                final esSeleccionado = i == resaltado;
                final chipColor = p.sinStock
                    ? RegentaColors.crit
                    : (p.nivelStock == NivelStock.bajo ? RegentaColors.warn : RegentaColors.ok);
                final chipBg = p.sinStock
                    ? RegentaColors.critSoft
                    : (p.nivelStock == NivelStock.bajo ? RegentaColors.warnSoft : RegentaColors.okSoft);
                final existStr = p.sinStock
                    ? '0 und'
                    : (p.existencia != null ? '${p.existencia} und' : 'Disponible');

                return Material(
                  color: esSeleccionado ? RegentaColors.accentSoft : RegentaColors.surface,
                  child: InkWell(
                    onTap: () => onAgregar(p),
                    child: Container(
                      decoration: BoxDecoration(
                        border: Border(
                          left: BorderSide(
                            color: esSeleccionado ? RegentaColors.accent : Colors.transparent,
                            width: 4,
                          ),
                          bottom: const BorderSide(color: RegentaColors.line),
                        ),
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                      child: Row(
                        children: [
                          SizedBox(
                            width: 100,
                            child: Text(
                              p.sku,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: RegentaType.codigo.copyWith(
                                fontSize: 13,
                                color: RegentaColors.ink2,
                              ),
                            ),
                          ),
                          Expanded(
                            child: Text(
                              p.nombre,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: PastelTheme.cuerpo(
                                fontSize: 14.5,
                                fontWeight: FontWeight.w600,
                                color: RegentaColors.ink,
                              ),
                            ),
                          ),
                          const SizedBox(width: 14),
                          Text(
                            formatearPesos(p.precioVenta),
                            style: RegentaType.codigo.copyWith(
                              fontSize: 14,
                              fontWeight: FontWeight.w700,
                              color: RegentaColors.ink,
                            ),
                          ),
                          const SizedBox(width: 14),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                            decoration: BoxDecoration(
                              color: chipBg,
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              existStr,
                              style: RegentaType.codigo.copyWith(
                                fontSize: 11.5,
                                fontWeight: FontWeight.w700,
                                color: chipColor,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ),
                );
              },
            ),
          ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// Venta en escritorio: Tabla + Pie (R10)
// ---------------------------------------------------------------------------

class _VentaEscritorio extends StatelessWidget {
  const _VentaEscritorio({
    required this.estado,
    required this.onMas,
    required this.onMenos,
    required this.onFijarCantidad,
    required this.onQuitar,
    required this.onVaciar,
    required this.onEspera,
    required this.onDescuento,
    required this.onNota,
    required this.onCobrar,
  });

  final EstadoDelPos estado;
  final void Function(int) onMas;
  final void Function(int) onMenos;
  final void Function(int, int) onFijarCantidad;
  final void Function(int) onQuitar;
  final VoidCallback onVaciar;
  final VoidCallback onEspera;
  final VoidCallback onDescuento;
  final VoidCallback onNota;
  final Future<void> Function() onCobrar;

  static const anchoCodigo = 120.0;
  static const anchoCantidad = 150.0;
  static const anchoPrecio = 120.0;
  static const anchoIva = 75.0;
  static const anchoTotal = 140.0;
  static const anchoAccion = 52.0;

  @override
  Widget build(BuildContext context) {
    final cabecera = PastelTheme.cuerpo(
      fontSize: 11.5,
      fontWeight: FontWeight.w700,
      letterSpacing: 0.6,
      color: RegentaColors.ink2,
    );
    Widget celda(String texto, double ancho, {TextAlign alinear = TextAlign.left}) =>
        SizedBox(width: ancho, child: Text(texto, textAlign: alinear, style: cabecera));

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      child: Column(
        children: [
          Expanded(
            child: Container(
              key: const Key('pos-carrito'),
              decoration: BoxDecoration(
                color: RegentaColors.surface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: RegentaColors.line),
              ),
              clipBehavior: Clip.antiAlias,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  _CabeceraDeVenta(estado: estado, onVaciar: onVaciar),
                  Container(
                    color: RegentaColors.paper,
                    padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 10),
                    child: Row(
                      children: [
                        celda('CÓDIGO', anchoCodigo),
                        Expanded(child: Text('DESCRIPCIÓN', style: cabecera)),
                        celda('CANT.', anchoCantidad, alinear: TextAlign.center),
                        celda('PRECIO', anchoPrecio, alinear: TextAlign.right),
                        celda('% IVA', anchoIva, alinear: TextAlign.right),
                        celda('TOTAL', anchoTotal, alinear: TextAlign.right),
                        const SizedBox(width: anchoAccion),
                      ],
                    ),
                  ),
                  Expanded(
                    child: estado.lineas.isEmpty
                        ? const _VentaVacia()
                        : ListView.builder(
                            itemCount: estado.lineas.length,
                            itemBuilder: (context, i) => _FilaDeVenta(
                              linea: estado.lineas[i],
                              onMas: () => onMas(i),
                              onMenos: () => onMenos(i),
                              onFijarCantidad: (c) => onFijarCantidad(i, c),
                              onQuitar: () => onQuitar(i),
                            ),
                          ),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          _PieEscritorio(
            estado: estado,
            onDescuento: onDescuento,
            onNota: onNota,
            onEspera: onEspera,
            onCobrar: onCobrar,
          ),
        ],
      ),
    );
  }
}

class _CabeceraDeVenta extends StatelessWidget {
  const _CabeceraDeVenta({required this.estado, required this.onVaciar});

  final EstadoDelPos estado;
  final VoidCallback onVaciar;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 10, 10, 10),
      child: Row(
        children: [
          Expanded(
            child: Text(
              '${estado.lineas.length} ${estado.lineas.length == 1 ? 'producto' : 'productos'}'
              ' · ${estado.unidades} ${estado.unidades == 1 ? 'unidad' : 'unidades'}',
              style: PastelTheme.cuerpo(
                fontSize: 14.5,
                fontWeight: FontWeight.w700,
                color: RegentaColors.ink,
              ),
            ),
          ),
          TextButton.icon(
            onPressed: estado.lineas.isEmpty ? null : onVaciar,
            icon: const Icon(Icons.delete_outline, size: 18),
            label: const Text('Vaciar venta'),
            style: TextButton.styleFrom(
              foregroundColor: RegentaColors.crit,
              disabledForegroundColor: RegentaColors.faint,
              minimumSize: const Size(44, 44),
            ),
          ),
        ],
      ),
    );
  }
}

class _VentaVacia extends StatelessWidget {
  const _VentaVacia();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Container(
              width: 68,
              height: 68,
              decoration: const BoxDecoration(
                color: RegentaColors.paper,
                shape: BoxShape.circle,
              ),
              child: const Icon(Icons.search, size: 32, color: RegentaColors.faint),
            ),
            const SizedBox(height: 14),
            Text(
              'Busca o escanea un producto para empezar la venta.',
              textAlign: TextAlign.center,
              style: PastelTheme.cuerpo(fontSize: 14, color: RegentaColors.ink2),
            ),
          ],
        ),
      ),
    );
  }
}

class _FilaDeVenta extends StatelessWidget {
  const _FilaDeVenta({
    required this.linea,
    required this.onMas,
    required this.onMenos,
    required this.onFijarCantidad,
    required this.onQuitar,
  });

  final LineaDeCarrito linea;
  final VoidCallback onMas;
  final VoidCallback onMenos;
  final ValueChanged<int> onFijarCantidad;
  final VoidCallback onQuitar;

  @override
  Widget build(BuildContext context) {
    final dinero = RegentaType.codigo.copyWith(fontSize: 13.5, color: RegentaColors.ink2);
    final esBajoStock = linea.producto.nivelStock == NivelStock.bajo ||
        (linea.producto.existencia != null && linea.producto.existencia! <= 5);

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      decoration: BoxDecoration(
        color: linea.sinStock ? RegentaColors.critSoft : null,
        border: const Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        children: [
          SizedBox(
            width: _VentaEscritorio.anchoCodigo,
            child: Text(
              linea.producto.sku,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: RegentaType.codigo.copyWith(fontSize: 12.5, color: RegentaColors.ink2),
            ),
          ),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  linea.producto.nombre,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: PastelTheme.cuerpo(
                    fontSize: 14.5,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.ink,
                  ),
                ),
                if (linea.sinStock)
                  Text(
                    'Sin stock',
                    style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.crit),
                  )
                else if (esBajoStock)
                  Text(
                    linea.producto.existencia != null
                        ? 'Quedan ${linea.producto.existencia} en bodega'
                        : 'Stock bajo',
                    style: PastelTheme.cuerpo(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: RegentaColors.warn,
                    ),
                  ),
              ],
            ),
          ),
          SizedBox(
            width: _VentaEscritorio.anchoCantidad,
            child: Center(
              child: _StepperEditable(
                cantidad: linea.cantidad,
                onMas: onMas,
                onMenos: onMenos,
                onFijar: onFijarCantidad,
              ),
            ),
          ),
          SizedBox(
            width: _VentaEscritorio.anchoPrecio,
            child: Text(
              formatearPesos(linea.producto.precioVenta),
              textAlign: TextAlign.right,
              style: dinero,
            ),
          ),
          SizedBox(
            width: _VentaEscritorio.anchoIva,
            child: Text(
              '${linea.producto.impuestoPct}',
              textAlign: TextAlign.right,
              style: dinero,
            ),
          ),
          SizedBox(
            width: _VentaEscritorio.anchoTotal,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  formatearPesos(linea.total),
                  textAlign: TextAlign.right,
                  style: RegentaType.codigo.copyWith(
                    fontSize: 14,
                    fontWeight: FontWeight.w700,
                    color: RegentaColors.ink,
                  ),
                ),
                if (linea.descuentoPct > 0)
                  Text(
                    '−${linea.descuentoPct}% desc.',
                    style: PastelTheme.cuerpo(
                      fontSize: 11,
                      fontWeight: FontWeight.w600,
                      color: RegentaColors.ok,
                    ),
                  ),
              ],
            ),
          ),
          SizedBox(
            width: _VentaEscritorio.anchoAccion,
            child: IconButton(
              tooltip: 'Quitar línea',
              onPressed: onQuitar,
              icon: const Icon(Icons.delete_outline, size: 20, color: RegentaColors.faint),
            ),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Pie de la venta en Escritorio (Imagen 1)
// ---------------------------------------------------------------------------

class _PieEscritorio extends StatelessWidget {
  const _PieEscritorio({
    required this.estado,
    required this.onDescuento,
    required this.onNota,
    required this.onEspera,
    required this.onCobrar,
  });

  final EstadoDelPos estado;
  final VoidCallback onDescuento;
  final VoidCallback onNota;
  final VoidCallback onEspera;
  final Future<void> Function() onCobrar;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: RegentaColors.line),
      ),
      padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 14),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // Izquierda: Nota y Botones píldora
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                if (estado.nota != null && estado.nota!.isNotEmpty)
                  Padding(
                    padding: const EdgeInsets.only(bottom: 8),
                    child: Row(
                      children: [
                        const Icon(Icons.description_outlined, size: 16, color: RegentaColors.ink2),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            'Nota: ${estado.nota}',
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                            style: PastelTheme.cuerpo(fontSize: 12.5, color: RegentaColors.ink2),
                          ),
                        ),
                      ],
                    ),
                  ),
                Wrap(
                  spacing: 10,
                  runSpacing: 8,
                  children: [
                    _BotonPildora(
                      icono: Icons.percent,
                      texto: 'Descuento',
                      onTap: estado.lineas.isEmpty ? null : onDescuento,
                    ),
                    _BotonPildora(
                      icono: Icons.sticky_note_2_outlined,
                      texto: 'Nota',
                      onTap: onNota,
                    ),
                    _BotonPildora(
                      icono: Icons.pause_circle_outline,
                      texto: 'Poner en espera',
                      onTap: estado.lineas.isEmpty ? null : onEspera,
                    ),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 16),
          // Centro-Derecha: Botón grande Cobrar
          SizedBox(
            width: 220,
            height: 62,
            child: FilledButton(
              key: const Key('pos-cobrar'),
              onPressed: estado.puedeCobrar ? () => onCobrar() : null,
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.accent,
                disabledBackgroundColor: RegentaColors.accent.withValues(alpha: 0.35),
                shape: const StadiumBorder(),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    estado.cobrando ? 'Cobrando…' : 'Cobrar',
                    style: PastelTheme.cuerpo(
                      fontSize: 17,
                      fontWeight: FontWeight.w800,
                      color: RegentaColors.surface,
                    ),
                  ),
                  const SizedBox(width: 10),
                  const _Atajo('F12'),
                ],
              ),
            ),
          ),
          const SizedBox(width: 28),
          // Derecha: Resumen de totales
          SizedBox(
            width: 260,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                _Fila('Subtotal', formatearPesos(estado.subtotalBruto)),
                if (estado.descuentoTotal > 0)
                  _Fila(
                    'Descuentos',
                    '− ${formatearPesos(estado.descuentoTotal)}',
                    colorValor: RegentaColors.ok,
                  ),
                _Fila('IVA', formatearPesos(estado.impuesto)),
                const SizedBox(height: 4),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  crossAxisAlignment: CrossAxisAlignment.baseline,
                  textBaseline: TextBaseline.alphabetic,
                  children: [
                    Text(
                      'Total',
                      style: PastelTheme.titulo(
                        fontSize: 18,
                        fontWeight: FontWeight.w700,
                        color: RegentaColors.ink,
                      ),
                    ),
                    Text(
                      formatearPesos(estado.total),
                      style: RegentaType.dinero.copyWith(
                        fontSize: 28,
                        fontWeight: FontWeight.w800,
                        color: RegentaColors.ink,
                      ),
                    ),
                  ],
                ),
                if (estado.hayLineasSinStock)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      'Hay líneas sin stock',
                      textAlign: TextAlign.right,
                      style: PastelTheme.cuerpo(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w700,
                        color: RegentaColors.crit,
                      ),
                    ),
                  ),
                if (estado.faltaIdentificarComprador)
                  Padding(
                    padding: const EdgeInsets.only(top: 4),
                    child: Text(
                      'Supera ${formatearPesos(estado.montoParaIdentificar!)}: identifica al comprador',
                      textAlign: TextAlign.right,
                      style: PastelTheme.cuerpo(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w700,
                        color: RegentaColors.warn,
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _BotonPildora extends StatelessWidget {
  const _BotonPildora({
    required this.icono,
    required this.texto,
    required this.onTap,
  });

  final IconData icono;
  final String texto;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return OutlinedButton.icon(
      onPressed: onTap,
      icon: Icon(icono, size: 17),
      label: Text(texto),
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(44, 44),
        foregroundColor: RegentaColors.ink,
        backgroundColor: RegentaColors.surface,
        side: const BorderSide(color: RegentaColors.line),
        shape: const StadiumBorder(),
        padding: const EdgeInsets.symmetric(horizontal: 14),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Venta en móvil (390 px)
// ---------------------------------------------------------------------------

class _VentaMovil extends StatelessWidget {
  const _VentaMovil({
    required this.estado,
    required this.onMas,
    required this.onMenos,
    required this.onFijarCantidad,
    required this.onQuitar,
    required this.onVaciar,
  });

  final EstadoDelPos estado;
  final void Function(int) onMas;
  final void Function(int) onMenos;
  final void Function(int, int) onFijarCantidad;
  final void Function(int) onQuitar;
  final VoidCallback onVaciar;

  @override
  Widget build(BuildContext context) {
    if (estado.lineas.isEmpty) {
      return const KeyedSubtree(key: Key('pos-carrito'), child: _VentaVacia());
    }
    return ListView(
      key: const Key('pos-carrito'),
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      children: [
        _CabeceraDeVenta(estado: estado, onVaciar: onVaciar),
        for (var i = 0; i < estado.lineas.length; i++)
          _TarjetaDeLineaMovil(
            linea: estado.lineas[i],
            onMas: () => onMas(i),
            onMenos: () => onMenos(i),
            onFijarCantidad: (c) => onFijarCantidad(i, c),
            onQuitar: () => onQuitar(i),
          ),
      ],
    );
  }
}

class _TarjetaDeLineaMovil extends StatelessWidget {
  const _TarjetaDeLineaMovil({
    required this.linea,
    required this.onMas,
    required this.onMenos,
    required this.onFijarCantidad,
    required this.onQuitar,
  });

  final LineaDeCarrito linea;
  final VoidCallback onMas;
  final VoidCallback onMenos;
  final ValueChanged<int> onFijarCantidad;
  final VoidCallback onQuitar;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: linea.sinStock ? RegentaColors.critSoft : RegentaColors.surface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      linea.producto.nombre,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: PastelTheme.cuerpo(
                        fontSize: 14.5,
                        fontWeight: FontWeight.w700,
                        color: RegentaColors.ink,
                      ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      '${linea.producto.sku} · ${formatearPesos(linea.producto.precioVenta)} · ${linea.producto.impuestoPct}% IVA',
                      style: RegentaType.codigo.copyWith(fontSize: 11.5, color: RegentaColors.ink2),
                    ),
                  ],
                ),
              ),
              IconButton(
                tooltip: 'Quitar línea',
                onPressed: onQuitar,
                icon: const Icon(Icons.delete_outline, size: 20, color: RegentaColors.faint),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              _StepperEditable(
                cantidad: linea.cantidad,
                onMas: onMas,
                onMenos: onMenos,
                onFijar: onFijarCantidad,
              ),
              const Spacer(),
              Column(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Text(
                    formatearPesos(linea.total),
                    style: RegentaType.codigo.copyWith(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: RegentaColors.ink,
                    ),
                  ),
                  if (linea.descuentoPct > 0)
                    Text(
                      '−${linea.descuentoPct}% desc.',
                      style: PastelTheme.cuerpo(fontSize: 11, color: RegentaColors.ok),
                    ),
                ],
              ),
            ],
          ),
        ],
      ),
    );
  }
}

class _PieMovil extends StatelessWidget {
  const _PieMovil({
    required this.estado,
    required this.onEspera,
    required this.onDescuento,
    required this.onNota,
    required this.onCobrar,
  });

  final EstadoDelPos estado;
  final VoidCallback onEspera;
  final VoidCallback onDescuento;
  final VoidCallback onNota;
  final Future<void> Function() onCobrar;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 14),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(top: BorderSide(color: RegentaColors.line)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(
                'Total · ${estado.unidades} und',
                style: PastelTheme.cuerpo(fontSize: 14, color: RegentaColors.ink2),
              ),
              Text(
                formatearPesos(estado.total),
                style: RegentaType.dinero.copyWith(fontSize: 22, fontWeight: FontWeight.w800),
              ),
            ],
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                _BotonPildora(
                  icono: Icons.percent,
                  texto: 'Desc.',
                  onTap: estado.lineas.isEmpty ? null : onDescuento,
                ),
                const SizedBox(width: 8),
                _BotonPildora(
                  icono: Icons.sticky_note_2_outlined,
                  texto: 'Nota',
                  onTap: onNota,
                ),
                const SizedBox(width: 8),
                _BotonPildora(
                  icono: Icons.pause_circle_outline,
                  texto: 'Espera',
                  onTap: estado.lineas.isEmpty ? null : onEspera,
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          SizedBox(
            height: 52,
            child: FilledButton(
              key: const Key('pos-cobrar'),
              onPressed: estado.puedeCobrar ? () => onCobrar() : null,
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.accent,
                shape: const StadiumBorder(),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(
                    estado.cobrando ? 'Cobrando…' : 'Cobrar',
                    style: PastelTheme.cuerpo(
                      fontSize: 16,
                      fontWeight: FontWeight.w700,
                      color: RegentaColors.surface,
                    ),
                  ),
                  const SizedBox(width: 8),
                  const _Atajo('F12'),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Stepper con cantidad editable por teclado y halo azul
// ---------------------------------------------------------------------------

class _StepperEditable extends StatefulWidget {
  const _StepperEditable({
    required this.cantidad,
    required this.onMas,
    required this.onMenos,
    required this.onFijar,
  });

  final int cantidad;
  final VoidCallback onMas;
  final VoidCallback onMenos;
  final ValueChanged<int> onFijar;

  @override
  State<_StepperEditable> createState() => _StepperEditableState();
}

class _StepperEditableState extends State<_StepperEditable> {
  bool _editando = false;
  late final TextEditingController _ctrl;
  final FocusNode _foco = FocusNode();

  @override
  void initState() {
    super.initState();
    _ctrl = TextEditingController(text: '${widget.cantidad}');
    _foco.addListener(() {
      if (!_foco.hasFocus && _editando) {
        _guardar();
      }
    });
  }

  @override
  void didUpdateWidget(covariant _StepperEditable oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (!_editando && oldWidget.cantidad != widget.cantidad) {
      _ctrl.text = '${widget.cantidad}';
    }
  }

  @override
  void dispose() {
    _ctrl.dispose();
    _foco.dispose();
    super.dispose();
  }

  void _guardar() {
    setState(() => _editando = false);
    final nueva = int.tryParse(_ctrl.text.trim());
    if (nueva != null) {
      widget.onFijar(nueva);
    } else {
      _ctrl.text = '${widget.cantidad}';
    }
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 44,
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: _editando ? RegentaColors.accent : RegentaColors.line,
          width: _editando ? 1.8 : 1.0,
        ),
        boxShadow: _editando
            ? [
                BoxShadow(
                  color: RegentaColors.accent.withValues(alpha: 0.25),
                  blurRadius: 4,
                  spreadRadius: 1,
                ),
              ]
            : null,
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _BotonStepper(icono: Icons.remove, onTap: widget.onMenos, semantica: 'Quitar uno'),
          SizedBox(
            width: 44,
            child: _editando
                ? TextField(
                    controller: _ctrl,
                    focusNode: _foco,
                    keyboardType: TextInputType.number,
                    textAlign: TextAlign.center,
                    style: RegentaType.codigo.copyWith(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: RegentaColors.ink,
                    ),
                    decoration: const InputDecoration(
                      border: InputBorder.none,
                      contentPadding: EdgeInsets.zero,
                      isDense: true,
                    ),
                    onSubmitted: (_) => _guardar(),
                  )
                : InkWell(
                    onTap: () {
                      setState(() => _editando = true);
                      _ctrl.text = '${widget.cantidad}';
                      _foco.requestFocus();
                      _ctrl.selection = TextSelection(
                        baseOffset: 0,
                        extentOffset: _ctrl.text.length,
                      );
                    },
                    child: Center(
                      child: Text(
                        '${widget.cantidad}',
                        textAlign: TextAlign.center,
                        style: RegentaType.codigo.copyWith(
                          fontSize: 14,
                          fontWeight: FontWeight.w700,
                          color: RegentaColors.ink,
                        ),
                      ),
                    ),
                  ),
          ),
          _BotonStepper(icono: Icons.add, onTap: widget.onMas, semantica: 'Agregar uno'),
        ],
      ),
    );
  }
}

class _BotonStepper extends StatelessWidget {
  const _BotonStepper({
    required this.icono,
    required this.onTap,
    required this.semantica,
  });

  final IconData icono;
  final VoidCallback onTap;
  final String semantica;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: semantica,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(10),
        child: SizedBox(
          width: RegentaSpacing.hitTarget,
          height: RegentaSpacing.hitTarget,
          child: Icon(icono, size: 18, color: RegentaColors.ink2),
        ),
      ),
    );
  }
}

class _Fila extends StatelessWidget {
  const _Fila(this.etiqueta, this.valor, {this.colorValor});

  final String etiqueta;
  final String valor;
  final Color? colorValor;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            etiqueta,
            style: PastelTheme.cuerpo(fontSize: 13, color: RegentaColors.ink2),
          ),
          Text(
            valor,
            style: RegentaType.codigo.copyWith(
              fontSize: 13.5,
              fontWeight: FontWeight.w600,
              color: colorValor ?? RegentaColors.ink2,
            ),
          ),
        ],
      ),
    );
  }
}
