import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../caja/proveedores.dart';
import '../datos/cliente_de_la_venta.dart';
import '../datos/producto_buscado.dart';
import '../datos/repositorio_de_ventas.dart';
import '../pos/estado_del_pos.dart';
import '../pos/linea_de_carrito.dart';
import '../pos/proveedores.dart';
import 'formato.dart';
import 'pantalla_buscar_venta.dart';
import 'pantalla_cliente_de_venta.dart';
import 'pantalla_cobro.dart';

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

    final tecla = evento.logicalKey;
    final esCtrl = HardwareKeyboard.instance.isControlPressed ||
        HardwareKeyboard.instance.isMetaPressed;
    final esShift = HardwareKeyboard.instance.isShiftPressed;
    final esAlt = HardwareKeyboard.instance.isAltPressed;

    // F2 o Ctrl+F: Enfocar buscador
    if (tecla == LogicalKeyboardKey.f2 || (esCtrl && tecla == LogicalKeyboardKey.keyF)) {
      _enfocarBuscador();
      return true;
    }

    // F12 o Ctrl+Enter: Cobrar (con Shift: venta rápida)
    if (tecla == LogicalKeyboardKey.f12) {
      if (esShift) {
        _ventaRapidaEfectivo();
      } else {
        _cobrar();
      }
      return true;
    }
    if (esCtrl && (tecla == LogicalKeyboardKey.enter || tecla == LogicalKeyboardKey.numpadEnter)) {
      _cobrar();
      return true;
    }

    // Ctrl+P: Venta rápida en efectivo
    if (esCtrl && tecla == LogicalKeyboardKey.keyP) {
      _ventaRapidaEfectivo();
      return true;
    }

    // Alt+C: Abrir selector de clientes
    if (esAlt && tecla == LogicalKeyboardKey.keyC) {
      final escritorio = MediaQuery.of(context).size.width >= kBreakpointEscritorio;
      _abrirSelectorDeCliente(escritorio: escritorio);
      return true;
    }

    // Alt+M: Abrir diálogo movimiento de caja
    if (esAlt && tecla == LogicalKeyboardKey.keyM) {
      _mostrarDialogoMovimientoCajaRapido();
      return true;
    }

    // Ctrl+M: Abrir monedero / cajón
    if (esCtrl && tecla == LogicalKeyboardKey.keyM) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Comando apertura de cajón monedero enviado')),
      );
      return true;
    }

    // Ctrl+C: Modificar cantidad de línea
    if (esCtrl && tecla == LogicalKeyboardKey.keyC) {
      _abrirDialogoCantidadRapida();
      return true;
    }

    // Ctrl+B: Descuento
    if (esCtrl && tecla == LogicalKeyboardKey.keyB) {
      _abrirDialogoDescuento();
      return true;
    }

    // Ctrl+E: Eliminar producto del carrito (última línea)
    if (esCtrl && tecla == LogicalKeyboardKey.keyE) {
      final estado = ref.read(controladorDelPosProvider);
      if (estado.lineas.isNotEmpty) {
        ref.read(controladorDelPosProvider.notifier).quitar(estado.lineas.length - 1);
        _enfocarBuscador();
      }
      return true;
    }

    // Ctrl+L: Limpiar / nueva venta
    if (esCtrl && tecla == LogicalKeyboardKey.keyL) {
      _confirmarLimpiarVenta();
      return true;
    }

    // Ctrl+S: Cotizar carrito
    if (esCtrl && tecla == LogicalKeyboardKey.keyS) {
      _cotizarCarrito();
      return true;
    }

    // Ctrl+D: Fracciones / detalle de producto
    if (esCtrl && tecla == LogicalKeyboardKey.keyD) {
      final estado = ref.read(controladorDelPosProvider);
      if (estado.lineas.isNotEmpty) {
        _mostrarDetalleProducto(context, estado.lineas.last.producto);
      }
      return true;
    }

    // Esc: Poner en espera si hay líneas
    if (tecla == LogicalKeyboardKey.escape) {
      final estado = ref.read(controladorDelPosProvider);
      if (estado.lineas.isNotEmpty) {
        _cambiarDeVenta(ref.read(controladorDelPosProvider.notifier).ponerEnEspera);
        return true;
      }
    }

    return false;
  }

  Future<void> _abrirDialogoCantidadRapida() async {
    final estado = ref.read(controladorDelPosProvider);
    if (estado.lineas.isEmpty) return;
    final pos = ref.read(controladorDelPosProvider.notifier);
    final indice = estado.lineas.length - 1;
    final linea = estado.lineas[indice];
    final ctrl = TextEditingController(text: formatearCantidadExacta(linea.cantidad));

    await showDialog<void>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text('Modificar cantidad: ${linea.producto.nombre}'),
        content: SizedBox(
          width: 320,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('Unidad: ${unidadParaProducto(linea.producto.nombre)}',
                  style: const TextStyle(fontWeight: FontWeight.w600, color: RegentaColors.muted)),
              const SizedBox(height: 12),
              TextField(
                controller: ctrl,
                autofocus: true,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Nueva cantidad',
                  border: OutlineInputBorder(),
                ),
                onSubmitted: (_) {
                  final val = num.tryParse(ctrl.text.trim().replaceAll(',', '.')) ?? 0;
                  if (val > 0) pos.fijarCantidad(indice, val);
                  Navigator.of(ctx).pop();
                },
              ),
            ],
          ),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cancelar')),
          FilledButton(
            onPressed: () {
              final val = num.tryParse(ctrl.text.trim().replaceAll(',', '.')) ?? 0;
              if (val > 0) pos.fijarCantidad(indice, val);
              Navigator.of(ctx).pop();
            },
            child: const Text('Guardar'),
          ),
        ],
      ),
    );
    if (mounted) _enfocarBuscador();
  }

  Future<void> _confirmarLimpiarVenta() async {
    final estado = ref.read(controladorDelPosProvider);
    if (estado.lineas.isEmpty) return;
    final confirmar = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('¿Limpiar venta?'),
        content: const Text('¿Desea limpiar el carrito actual e iniciar una nueva venta?'),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Cancelar')),
          FilledButton(
            style: FilledButton.styleFrom(backgroundColor: RegentaColors.crit),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Limpiar'),
          ),
        ],
      ),
    );
    if (confirmar == true) {
      ref.read(controladorDelPosProvider.notifier).vaciar();
    }
    if (mounted) _enfocarBuscador();
  }

  Future<void> _mostrarDialogoMovimientoCajaRapido() async {
    bool esIngreso = true;
    final montoCtrl = TextEditingController();
    final conceptoCtrl = TextEditingController();
    final focoMonto = FocusNode();

    await showDialog<void>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setDlgState) => AlertDialog(
          title: const Text('Movimiento de Caja'),
          content: SizedBox(
            width: 380,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                SegmentedButton<bool>(
                  segments: const [
                    ButtonSegment(
                      value: true,
                      label: Text('Entrada'),
                      icon: Icon(Icons.arrow_downward, color: RegentaColors.ok),
                    ),
                    ButtonSegment(
                      value: false,
                      label: Text('Salida'),
                      icon: Icon(Icons.arrow_upward, color: RegentaColors.crit),
                    ),
                  ],
                  selected: {esIngreso},
                  onSelectionChanged: (s) {
                    setDlgState(() => esIngreso = s.first);
                    focoMonto.requestFocus();
                  },
                ),
                const SizedBox(height: 14),
                TextField(
                  key: const Key('movimiento-caja-monto'),
                  controller: montoCtrl,
                  focusNode: focoMonto,
                  autofocus: true,
                  keyboardType: const TextInputType.numberWithOptions(decimal: true),
                  decoration: const InputDecoration(
                    labelText: 'Total del movimiento (\$)',
                    prefixText: '\$ ',
                    border: OutlineInputBorder(),
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  key: const Key('movimiento-caja-descripcion'),
                  controller: conceptoCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Descripción / Motivo *',
                    border: OutlineInputBorder(),
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            FilledButton(
              key: const Key('movimiento-caja-guardar'),
              onPressed: () async {
                final monto = num.tryParse(montoCtrl.text.trim().replaceAll(',', '.')) ?? 0;
                final concepto = conceptoCtrl.text.trim();
                final messenger = ScaffoldMessenger.of(context);
                final errorColor = Theme.of(context).colorScheme.error;

                if (monto <= 0) {
                  messenger.showSnackBar(
                    const SnackBar(content: Text('Falta total del movimiento de caja.')),
                  );
                  focoMonto.requestFocus();
                  return;
                }
                if (concepto.isEmpty) {
                  messenger.showSnackBar(
                    const SnackBar(content: Text('Falta descripción del movimiento de caja.')),
                  );
                  return;
                }
                Navigator.of(ctx).pop();
                try {
                  final ctrlCaja = ref.read(controladorDeCajaProvider.notifier);
                  if (esIngreso) {
                    await ctrlCaja.registrarIngreso(monto, concepto);
                  } else {
                    await ctrlCaja.registrarRetiro(monto, concepto);
                  }
                  if (!mounted) return;
                  messenger.showSnackBar(
                    SnackBar(
                      content: Text(
                        'Movimiento de ${esIngreso ? "Entrada" : "Salida"} por ${formatearPesos(monto)} registrado correctamente.',
                      ),
                    ),
                  );
                } catch (e) {
                  if (!mounted) return;
                  messenger.showSnackBar(
                    SnackBar(
                      backgroundColor: errorColor,
                      content: Text('Error al registrar movimiento: $e'),
                    ),
                  );
                }
              },
              child: const Text('Guardar'),
            ),
          ],
        ),
      ),
    );
    if (mounted) _enfocarBuscador();
  }

  void _enfocarBuscador() {
    if (_foco.canRequestFocus) {
      _foco.requestFocus();
    }
  }

  void _mostrarDetalleProducto(BuildContext ctx, ProductoBuscado p) {
    Color stockBadgeBg;
    Color stockBadgeBorde;
    Color stockBadgeTexto;
    String nivelTexto;

    if (p.sinStock) {
      stockBadgeBg = RegentaColors.posStockCeroFondo;
      stockBadgeBorde = RegentaColors.posStockCeroBorde;
      stockBadgeTexto = RegentaColors.posStockCeroTexto;
      nivelTexto = 'Sin stock';
    } else if (p.nivelStock == NivelStock.bajo) {
      stockBadgeBg = RegentaColors.posStockBajoFondo;
      stockBadgeBorde = RegentaColors.posStockBajoBorde;
      stockBadgeTexto = RegentaColors.posStockBajoTexto;
      nivelTexto = 'Stock bajo';
    } else {
      stockBadgeBg = RegentaColors.posStockOkFondo;
      stockBadgeBorde = RegentaColors.posStockOkBorde;
      stockBadgeTexto = RegentaColors.posStockOkTexto;
      nivelTexto = 'Disponible';
    }

    final unidad = unidadParaProducto(p.nombre);

    showDialog<void>(
      context: ctx,
      builder: (dialogCtx) {
        return AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
          backgroundColor: RegentaColors.posModalFondo,
          title: Row(
            children: [
              const Icon(Icons.inventory_2_outlined, size: 24, color: RegentaColors.posModalAcento),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  p.nombre,
                  style: const TextStyle(
                    fontSize: 17,
                    fontWeight: FontWeight.w700,
                    color: Colors.white,
                  ),
                ),
              ),
            ],
          ),
          content: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: stockBadgeBg,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: stockBadgeBorde, width: 1.2),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      const Text(
                        'Estado de stock',
                        style: TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w700,
                          color: RegentaColors.posModalTitulo,
                        ),
                      ),
                      Text(
                        nivelTexto,
                        style: TextStyle(
                          fontSize: 13.5,
                          fontWeight: FontWeight.w800,
                          color: stockBadgeTexto,
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 14),
                _FilaDetalle('SKU / Referencia', p.sku.isNotEmpty ? p.sku : '—'),
                const Divider(height: 1, color: RegentaColors.posModalBorde),
                _FilaDetalle(
                  'Existencia total / Stock',
                  p.existencia != null
                      ? '${formatearCantidadExacta(p.existencia!)} $unidad'
                      : 'No especificado',
                  valorColor: stockBadgeTexto,
                ),
                const Divider(height: 1, color: RegentaColors.posModalBorde),
                const _FilaDetalle('Ubicación / Sección', 'Bodega Principal · Pasillo'),
                const Divider(height: 1, color: RegentaColors.posModalBorde),
                _FilaDetalle('Precio de venta', formatearPesos(p.precioVenta)),
                const Divider(height: 1, color: RegentaColors.posModalBorde),
                _FilaDetalle('IVA', '${p.impuestoPct}%'),
                if (p.costoUnitario > 0) ...[
                  const Divider(height: 1, color: RegentaColors.posModalBorde),
                  _FilaDetalle('Costo unitario', formatearPesos(p.costoUnitario)),
                ],
                if (p.codigoBarras != null && p.codigoBarras!.isNotEmpty) ...[
                  const Divider(height: 1, color: RegentaColors.posModalBorde),
                  _FilaDetalle('Código de barras', p.codigoBarras!),
                ],
              ],
            ),
          ),
          actions: [
            FilledButton(
              onPressed: () => Navigator.of(dialogCtx).pop(),
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.posAcentoFuerte,
                foregroundColor: Colors.white,
                shape: const StadiumBorder(),
              ),
              child: const Text('Cerrar', style: TextStyle(fontWeight: FontWeight.w700)),
            ),
          ],
        );
      },
    ).then((_) {
      if (mounted) _enfocarBuscador();
    });
  }

  Future<void> _cobrar() async {
    final estado = ref.read(controladorDelPosProvider);
    if (!estado.puedeCobrar) return;
    final pos = ref.read(controladorDelPosProvider.notifier);

    final resultado = await Navigator.of(context).push<String>(
      MaterialPageRoute<String>(
        builder: (_) => PantallaCobro(
          total: estado.total,
          clienteAsignado: estado.cliente != null,
          bodegaId: ref.read(bodegaDeVentaProvider),
          lineas: estado.lineasParaEnviar(),
          clienteId: estado.cliente?.id,
          clienteNombre: estado.cliente?.nombre,
          clienteDoc: estado.cliente?.documento,
          notaCliente: estado.nota,
        ),
      ),
    );

    if (resultado == PantallaCobro.sinConexion) {
      // Sin señal: HU-043 cobra directo, sin pedir metodo de pago.
      await pos.cobrar();
    } else if (resultado != null) {
      pos.vaciarTrasCobro(resultado);
    }
    if (mounted) _enfocarBuscador();
  }

  Future<void> _ventaRapidaEfectivo() async {
    final estado = ref.read(controladorDelPosProvider);
    final pos = ref.read(controladorDelPosProvider.notifier);
    if (!estado.puedeCobrar) {
      if (estado.lineas.isEmpty) return;
      if (estado.hayLineasSinStock) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Hay líneas sin stock: quítalas antes de cobrar')),
        );
        return;
      }
      if (estado.faltaIdentificarComprador) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'La venta supera \$ ${estado.montoParaIdentificar}: identifica al comprador antes de cobrar',
            ),
          ),
        );
        return;
      }
      return;
    }

    final repo = ref.read(repositorioDeVentasProvider);
    final bodegaId = ref.read(bodegaDeVentaProvider);

    try {
      final borrador = await repo.crearBorrador(
        bodegaId: bodegaId,
        clienteId: estado.cliente?.id,
        notaCliente: estado.nota,
      );

      for (final l in estado.lineasParaEnviar()) {
        await repo.agregarLineaAlBorrador(borrador.id, l);
      }

      await repo.confirmarBorrador(borrador.id);
      final ventaConfirmada = await repo.esperarConfirmacion(borrador.id);

      if (!ventaConfirmada.confirmada) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('No hay stock suficiente para completar la venta')),
          );
        }
        return;
      }

      await repo.registrarPago(
        ventaConfirmada.id,
        PagoParaEnviar(
          metodo: 'EFECTIVO',
          montoRecibido: ventaConfirmada.total,
        ),
      );

      await repo.cerrarVenta(ventaConfirmada.id);

      if (mounted) {
        pos.vaciarTrasCobro(ventaConfirmada.numero);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('⚡ Venta ${ventaConfirmada.numero} cobrada en efectivo exacto'),
          ),
        );
      }
    } on ErrorDeRed {
      // Sin conexión: contingencia a offline
      await pos.cobrar();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: Theme.of(context).colorScheme.error,
            content: Text('Error al cobrar venta rápida: $e'),
          ),
        );
      }
    }
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
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _enfocarBuscador();
    });
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
          backgroundColor: RegentaColors.paper,
          surfaceTintColor: Colors.transparent,
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
    if (mounted) _enfocarBuscador();
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
    if (mounted) _enfocarBuscador();
  }

  Future<void> _cotizarCarrito() async {
    final estado = ref.read(controladorDelPosProvider);
    if (estado.lineas.isEmpty) return;

    try {
      final repo = ref.read(repositorioDeVentasProvider);
      final lineas = estado.lineas
          .map((l) => LineaDeCotizacionParaEnviar(
                productoId: l.producto.id,
                sku: l.producto.sku,
                nombre: l.producto.nombre,
                cantidad: l.cantidad,
                precioUnitario: l.producto.precioVenta,
                descuentoPct: l.descuentoPct,
                impuestoPct: l.producto.impuestoPct,
              ))
          .toList();

      final solicitud = SolicitudDeCotizacionParaEnviar(
        clienteId: estado.cliente?.id,
        lineas: lineas,
      );

      final cot = await repo.crearCotizacion(solicitud);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Cotización #${cot.numero} creada exitosamente'),
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: Theme.of(context).colorScheme.error,
            content: Text('Error al crear cotización: $e'),
          ),
        );
      }
    }
  }

  Future<void> _abrirCargarCotizacion() async {
    final repo = ref.read(repositorioDeVentasProvider);
    final bodegaId = ref.read(bodegaDeVentaProvider);

    await showDialog<void>(
      context: context,
      builder: (ctx) => _DialogoCargarCotizacion(
        repo: repo,
        bodegaId: bodegaId,
        onCotizacionConvertida: (resultado, cotizacion) async {
          final venta = await repo.verVenta(resultado.ventaId);
          final pos = ref.read(controladorDelPosProvider.notifier);
          pos.cargarLineasDesdeVenta(venta.lineas);
          if (mounted) {
            ScaffoldMessenger.of(context).hideCurrentSnackBar();
            if (resultado.preciosDesactualizados) {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  backgroundColor: Theme.of(context).colorScheme.error,
                  content: Text(
                    'Cotización vencida convertida en venta #${resultado.numeroVenta}. Verifica los precios antes de cobrar.',
                  ),
                ),
              );
            } else {
              ScaffoldMessenger.of(context).showSnackBar(
                SnackBar(
                  content: Text(
                    'Cotización #${cotizacion.numero} convertida en venta #${resultado.numeroVenta}',
                  ),
                ),
              );
            }
          }
        },
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

    return LayoutBuilder(
      builder: (context, restricciones) {
        final escritorio = restricciones.maxWidth >= kBreakpointEscritorio;
        final esMovil = restricciones.maxWidth < 600;

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
              IconButton(
                key: const Key('pos-boton-cargar-cotizacion'),
                tooltip: 'Cargar cotización',
                icon: const Icon(Icons.request_quote_outlined, color: RegentaColors.ink),
                onPressed: _abrirCargarCotizacion,
              ),
              IconButton(
                key: const Key('boton-historial-ventas'),
                tooltip: 'Historial y anulación de ventas',
                icon: const Icon(Icons.history, color: RegentaColors.ink),
                onPressed: () {
                  Navigator.of(context).push(
                    MaterialPageRoute(
                      builder: (_) => const PantallaBuscarVenta(),
                    ),
                  );
                },
              ),
              if (!esMovil)
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
          body: Builder(
            builder: (context) {
              final desplegable =
              !estado.desplegableCerrado &&
              (estado.buscando || estado.termino.trim().length >= 3);

          final cliente = _FranjaCliente(
            cliente: estado.cliente,
            compacta: !escritorio,
            onAgregar: () => _abrirSelectorDeCliente(escritorio: escritorio),
            onQuitar: () {
              pos.fijarCliente(null);
              _enfocarBuscador();
            },
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
                child: KeyedSubtree(
                  key: const Key('pos-resultados'),
                  child: _Resultados(
                    estado: estado,
                    resaltado: _resaltado,
                    onAgregar: (p) {
                      pos.agregar(p);
                      _enfocarBuscador();
                    },
                    onDetalle: (p) => _mostrarDetalleProducto(context, p),
                  ),
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
                          onMas: (i) {
                            pos.cambiarCantidad(i, 1);
                            _enfocarBuscador();
                          },
                          onMenos: (i) {
                            pos.cambiarCantidad(i, -1);
                            _enfocarBuscador();
                          },
                          onFijarCantidad: (i, c) {
                            pos.fijarCantidad(i, c);
                            _enfocarBuscador();
                          },
                          onQuitar: (i) {
                            pos.quitar(i);
                            _enfocarBuscador();
                          },
                          onVaciar: () {
                            pos.vaciar();
                            _enfocarBuscador();
                          },
                          onEspera: () => _cambiarDeVenta(pos.ponerEnEspera),
                          onDescuento: _abrirDialogoDescuento,
                          onNota: _abrirDialogoNota,
                          onCotizar: _cotizarCarrito,
                          onCobrar: _cobrar,
                          onVentaRapida: _ventaRapidaEfectivo,
                          onDetalle: (p) => _mostrarDetalleProducto(context, p),
                          onMovimientoCaja: _mostrarDialogoMovimientoCajaRapido,
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
                        onMas: (i) {
                          pos.cambiarCantidad(i, 1);
                          _enfocarBuscador();
                        },
                        onMenos: (i) {
                          pos.cambiarCantidad(i, -1);
                          _enfocarBuscador();
                        },
                        onFijarCantidad: (i, c) {
                          pos.fijarCantidad(i, c);
                          _enfocarBuscador();
                        },
                        onQuitar: (i) {
                          pos.quitar(i);
                          _enfocarBuscador();
                        },
                        onVaciar: () {
                          pos.vaciar();
                          _enfocarBuscador();
                        },
                        onDetalle: (p) => _mostrarDetalleProducto(context, p),
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
                onCotizar: _cotizarCarrito,
                onCobrar: _cobrar,
                onVentaRapida: _ventaRapidaEfectivo,
                onMovimientoCaja: _mostrarDialogoMovimientoCajaRapido,
              ),
            ],
          );
        },
      ),
    );
      },
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
        : (cliente != null && cliente!.trim().isNotEmpty
            ? (cliente!.length > 12 ? '${cliente!.substring(0, 10)}…' : cliente!)
            : 'Mostrador');

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
    required this.onDetalle,
  });

  final EstadoDelPos estado;
  final int resaltado;
  final void Function(ProductoBuscado) onAgregar;
  final void Function(ProductoBuscado) onDetalle;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          color: RegentaColors.sunken,
          padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
          child: LayoutBuilder(
            builder: (context, box) {
              final estrecho = box.maxWidth < 450;
              return Row(
                children: [
                  Expanded(
                    child: Text(
                      '${estado.resultados.length} resultados · Enter agrega el resaltado',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: PastelTheme.cuerpo(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.ink2,
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Text(
                    estrecho ? '↑ ↓ · Esc' : '↑ ↓ para moverte · Esc cierra',
                    style: PastelTheme.cuerpo(
                      fontSize: 12,
                      color: RegentaColors.ink2,
                    ),
                  ),
                ],
              );
            },
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
                      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                      child: Row(
                        children: [
                          SizedBox(
                            width: 80,
                            child: Text(
                              p.sku,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: RegentaType.codigo.copyWith(
                                fontSize: 12.5,
                                color: RegentaColors.ink2,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Text(
                              p.nombre,
                              maxLines: 1,
                              overflow: TextOverflow.ellipsis,
                              style: PastelTheme.cuerpo(
                                fontSize: 14,
                                fontWeight: FontWeight.w600,
                                color: RegentaColors.ink,
                              ),
                            ),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            formatearPesos(p.precioVenta),
                            style: RegentaType.codigo.copyWith(
                              fontSize: 13.5,
                              fontWeight: FontWeight.w700,
                              color: RegentaColors.ink,
                            ),
                          ),
                          const SizedBox(width: 8),
                          Container(
                            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                            decoration: BoxDecoration(
                              color: chipBg,
                              borderRadius: BorderRadius.circular(6),
                            ),
                            child: Text(
                              existStr,
                              style: RegentaType.codigo.copyWith(
                                fontSize: 11,
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
    required this.onCotizar,
    required this.onCobrar,
    required this.onVentaRapida,
    required this.onDetalle,
    required this.onMovimientoCaja,
  });

  final EstadoDelPos estado;
  final void Function(int) onMas;
  final void Function(int) onMenos;
  final void Function(int, num) onFijarCantidad;
  final void Function(int) onQuitar;
  final VoidCallback onVaciar;
  final VoidCallback onEspera;
  final VoidCallback onDescuento;
  final VoidCallback onNota;
  final VoidCallback onCotizar;
  final Future<void> Function() onCobrar;
  final VoidCallback onVentaRapida;
  final void Function(ProductoBuscado) onDetalle;
  final VoidCallback onMovimientoCaja;

  static const anchoCodigo = 120.0;
  static const anchoCantidad = 180.0;
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
        key: const Key('pos-carrito'),
        children: [
          Expanded(
            child: Container(
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
                              onDetalle: () => onDetalle(estado.lineas[i].producto),
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
            onCotizar: onCotizar,
            onEspera: onEspera,
            onCobrar: onCobrar,
            onVentaRapida: onVentaRapida,
            onMovimientoCaja: onMovimientoCaja,
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
    required this.onDetalle,
  });

  final LineaDeCarrito linea;
  final VoidCallback onMas;
  final VoidCallback onMenos;
  final ValueChanged<num> onFijarCantidad;
  final VoidCallback onQuitar;
  final VoidCallback onDetalle;

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
            child: GestureDetector(
              onDoubleTap: onDetalle,
              behavior: HitTestBehavior.opaque,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          linea.producto.nombre,
                          maxLines: 1,
                          overflow: TextOverflow.ellipsis,
                          style: TextStyle(
                            fontSize: 15,
                            fontWeight: FontWeight.w700,
                            color: Theme.of(context).brightness == Brightness.dark
                                ? RegentaColors.kdsTexto
                                : RegentaColors.inkFuerte,
                          ),
                        ),
                      ),
                      Tooltip(
                        message: 'Ver stock por bodega y detalle',
                        child: InkWell(
                          onTap: onDetalle,
                          borderRadius: BorderRadius.circular(12),
                          child: const Padding(
                            padding: EdgeInsets.all(4),
                            child: Icon(Icons.info_outline, size: 18, color: RegentaColors.accent),
                          ),
                        ),
                      ),
                    ],
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
          ),
          SizedBox(
            width: _VentaEscritorio.anchoCantidad,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                _StepperEditable(
                  cantidad: linea.cantidad,
                  nombreProducto: linea.producto.nombre,
                  onMas: onMas,
                  onMenos: onMenos,
                  onFijar: onFijarCantidad,
                ),
                const SizedBox(height: 3),
                Text(
                  unidadParaProducto(linea.producto.nombre),
                  key: Key('unidad-producto-${linea.producto.id}'),
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w800,
                    color: RegentaColors.muted,
                    letterSpacing: 0.5,
                  ),
                ),
              ],
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
    required this.onCotizar,
    required this.onEspera,
    required this.onCobrar,
    required this.onVentaRapida,
    required this.onMovimientoCaja,
  });

  final EstadoDelPos estado;
  final VoidCallback onDescuento;
  final VoidCallback onNota;
  final VoidCallback onCotizar;
  final VoidCallback onEspera;
  final Future<void> Function() onCobrar;
  final VoidCallback onVentaRapida;
  final VoidCallback onMovimientoCaja;

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
                      key: const Key('pos-boton-cotizar'),
                      icono: Icons.request_quote_outlined,
                      texto: 'Cotizar',
                      onTap: estado.lineas.isEmpty ? null : onCotizar,
                    ),
                    _BotonPildora(
                      icono: Icons.pause_circle_outline,
                      texto: 'Poner en espera',
                      onTap: estado.lineas.isEmpty ? null : onEspera,
                    ),
                    _BotonPildora(
                      key: const Key('pos-boton-venta-rapida'),
                      icono: Icons.bolt,
                      texto: 'Efectivo exacto',
                      onTap: estado.puedeCobrar ? onVentaRapida : null,
                    ),
                    _BotonPildora(
                      key: const Key('pos-boton-movimiento-caja'),
                      icono: Icons.point_of_sale_outlined,
                      texto: 'Movi. Caja',
                      onTap: onMovimientoCaja,
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
                    key: const Key('aviso-identificar-comprador'),
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
    super.key,
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
    required this.onDetalle,
  });

  final EstadoDelPos estado;
  final void Function(int) onMas;
  final void Function(int) onMenos;
  final void Function(int, num) onFijarCantidad;
  final void Function(int) onQuitar;
  final VoidCallback onVaciar;
  final void Function(ProductoBuscado) onDetalle;

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
            onDetalle: () => onDetalle(estado.lineas[i].producto),
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
    required this.onDetalle,
  });

  final LineaDeCarrito linea;
  final VoidCallback onMas;
  final VoidCallback onMenos;
  final ValueChanged<num> onFijarCantidad;
  final VoidCallback onQuitar;
  final VoidCallback onDetalle;

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
                    GestureDetector(
                      onDoubleTap: onDetalle,
                      behavior: HitTestBehavior.opaque,
                      child: Row(
                        children: [
                          Expanded(
                            child: Text(
                              linea.producto.nombre,
                              maxLines: 2,
                              overflow: TextOverflow.ellipsis,
                              style: TextStyle(
                                fontSize: 15,
                                fontWeight: FontWeight.w700,
                                color: Theme.of(context).brightness == Brightness.dark
                                    ? RegentaColors.kdsTexto
                                    : RegentaColors.inkFuerte,
                              ),
                            ),
                          ),
                          Tooltip(
                            message: 'Ver stock y detalle',
                            child: InkWell(
                              onTap: onDetalle,
                              child: const Padding(
                                padding: EdgeInsets.all(4),
                                child: Icon(Icons.info_outline, size: 18, color: RegentaColors.accent),
                              ),
                            ),
                          ),
                        ],
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
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _StepperEditable(
                    cantidad: linea.cantidad,
                    nombreProducto: linea.producto.nombre,
                    onMas: onMas,
                    onMenos: onMenos,
                    onFijar: onFijarCantidad,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    unidadParaProducto(linea.producto.nombre),
                    style: const TextStyle(
                      fontSize: 10.5,
                      fontWeight: FontWeight.w800,
                      color: RegentaColors.muted,
                    ),
                  ),
                ],
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
    required this.onCotizar,
    required this.onCobrar,
    required this.onVentaRapida,
    required this.onMovimientoCaja,
  });

  final EstadoDelPos estado;
  final VoidCallback onEspera;
  final VoidCallback onDescuento;
  final VoidCallback onNota;
  final VoidCallback onCotizar;
  final Future<void> Function() onCobrar;
  final VoidCallback onVentaRapida;
  final VoidCallback onMovimientoCaja;

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
                  key: const Key('pos-boton-cotizar'),
                  icono: Icons.request_quote_outlined,
                  texto: 'Cotizar',
                  onTap: estado.lineas.isEmpty ? null : onCotizar,
                ),
                const SizedBox(width: 8),
                _BotonPildora(
                  icono: Icons.pause_circle_outline,
                  texto: 'Espera',
                  onTap: estado.lineas.isEmpty ? null : onEspera,
                ),
                const SizedBox(width: 8),
                _BotonPildora(
                  key: const Key('pos-boton-venta-rapida-movil'),
                  icono: Icons.bolt,
                  texto: 'Efectivo',
                  onTap: estado.puedeCobrar ? onVentaRapida : null,
                ),
                const SizedBox(width: 8),
                _BotonPildora(
                  key: const Key('pos-boton-movimiento-caja-movil'),
                  icono: Icons.point_of_sale_outlined,
                  texto: 'Caja',
                  onTap: onMovimientoCaja,
                ),
              ],
            ),
          ),
          if (estado.hayLineasSinStock)
            Padding(
              padding: const EdgeInsets.only(bottom: 6),
              child: Text(
                'Hay líneas sin stock',
                style: PastelTheme.cuerpo(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w700,
                  color: RegentaColors.crit,
                ),
              ),
            ),
          if (estado.faltaIdentificarComprador)
            Padding(
              key: const Key('aviso-identificar-comprador'),
              padding: const EdgeInsets.only(bottom: 6),
              child: Text(
                'Supera ${formatearPesos(estado.montoParaIdentificar!)}: '
                'identifica al comprador para cobrar',
                style: PastelTheme.cuerpo(
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: RegentaColors.warn,
                ),
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
    this.nombreProducto,
  });

  final num cantidad;
  final VoidCallback onMas;
  final VoidCallback onMenos;
  final ValueChanged<num> onFijar;
  final String? nombreProducto;

  @override
  State<_StepperEditable> createState() => _StepperEditableState();
}

class _StepperEditableState extends State<_StepperEditable> {
  bool _editando = false;
  late final TextEditingController _ctrl;
  final FocusNode _foco = FocusNode();

  static String _formatearCantidad(num c) => formatearCantidadExacta(c);

  @override
  void initState() {
    super.initState();
    _ctrl = TextEditingController(text: _formatearCantidad(widget.cantidad));
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
      _ctrl.text = _formatearCantidad(widget.cantidad);
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
    final texto = _ctrl.text.trim().replaceAll(',', '.');
    final nueva = num.tryParse(texto);
    if (nueva != null && nueva > 0) {
      widget.onFijar(nueva);
    } else if (nueva != null && nueva <= 0) {
      widget.onFijar(0);
    } else {
      _ctrl.text = _formatearCantidad(widget.cantidad);
    }
  }

  void _abrirSelectorFracciones() {
    final unidad = unidadParaProducto(widget.nombreProducto);

    showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: RegentaColors.posModalFondo,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(20)),
      ),
      builder: (ctx) => SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 18),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Seleccionar fracción / medida',
                    style: TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w700,
                      color: Colors.white,
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, color: RegentaColors.posModalTextoTenue),
                    onPressed: () => Navigator.of(ctx).pop(),
                  ),
                ],
              ),
              const SizedBox(height: 14),
              if (unidad == 'GL') ...[
                const Text(
                  'Fracciones de galón y cuñete (5 galones):',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.posModalTextoSecundario,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _chipFraccion(ctx, '1 Cuñete · Balde (5.0 gal)', 5.0),
                    _chipFraccion(ctx, '1/2 Cuñete (2.5 gal)', 2.5),
                    _chipFraccion(ctx, '1 Galón (1.0)', 1.0),
                    _chipFraccion(ctx, '1/2 Galón (0.5)', 0.5),
                    _chipFraccion(ctx, '1/4 Galón (0.25)', 0.25),
                    _chipFraccion(ctx, '1/8 Galón (0.125)', 0.125),
                    _chipFraccion(ctx, '1/16 Galón (0.0625)', 0.0625),
                    _chipFraccion(ctx, '1/32 Galón (0.03125)', 0.03125),
                  ],
                ),
              ] else if (unidad == 'MTS') ...[
                const Text(
                  'Medidas de longitud / metros (manto, cable, tubo...):',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.posModalTextoSecundario,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _chipFraccion(ctx, '1 metro (1.0 m)', 1.0),
                    _chipFraccion(ctx, '1/2 m · 50 cm (0.5 m)', 0.5),
                    _chipFraccion(ctx, '1/4 m · 25 cm (0.25 m)', 0.25),
                    _chipFraccion(ctx, '3/4 m · 75 cm (0.75 m)', 0.75),
                    _chipFraccion(ctx, '10 cm (0.1 m)', 0.1),
                    _chipFraccion(ctx, '20 cm (0.2 m)', 0.2),
                    _chipFraccion(ctx, '1.5 metros (1.5 m)', 1.5),
                    _chipFraccion(ctx, '2 metros (2.0 m)', 2.0),
                    _chipFraccion(ctx, '2.5 metros (2.5 m)', 2.5),
                    _chipFraccion(ctx, '3 metros (3.0 m)', 3.0),
                    _chipFraccion(ctx, '5 metros (5.0 m)', 5.0),
                    _chipFraccion(ctx, '10 metros (10.0 m)', 10.0),
                  ],
                ),
              ] else if (unidad == 'KG') ...[
                const Text(
                  'Fracciones de peso (kilos / gramos):',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.posModalTextoSecundario,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _chipFraccion(ctx, '1 Kilo (1.0 kg)', 1.0),
                    _chipFraccion(ctx, '1/2 Kilo · 500 g (0.5 kg)', 0.5),
                    _chipFraccion(ctx, '1/4 Kilo · 250 g (0.25 kg)', 0.25),
                    _chipFraccion(ctx, '100 gramos (0.1 kg)', 0.1),
                    _chipFraccion(ctx, '2 Kilos (2.0 kg)', 2.0),
                    _chipFraccion(ctx, '5 Kilos (5.0 kg)', 5.0),
                    _chipFraccion(ctx, '25 Kilos · Bulto (25 kg)', 25.0),
                    _chipFraccion(ctx, '50 Kilos · Bulto (50 kg)', 50.0),
                  ],
                ),
              ] else ...[
                const Text(
                  'Cantidades y unidades:',
                  style: TextStyle(
                    fontSize: 14,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.posModalTextoSecundario,
                  ),
                ),
                const SizedBox(height: 10),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: [
                    _chipFraccion(ctx, '1 unidad', 1.0),
                    _chipFraccion(ctx, '2 unidades', 2.0),
                    _chipFraccion(ctx, '3 unidades', 3.0),
                    _chipFraccion(ctx, '5 unidades', 5.0),
                    _chipFraccion(ctx, '6 unidades (1/2 docena)', 6.0),
                    _chipFraccion(ctx, '10 unidades', 10.0),
                    _chipFraccion(ctx, '12 unidades (docena)', 12.0),
                    _chipFraccion(ctx, '24 unidades (2 docenas)', 24.0),
                    _chipFraccion(ctx, '1/2 unidad (0.5)', 0.5),
                    _chipFraccion(ctx, '1/4 unidad (0.25)', 0.25),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }

  Widget _chipFraccion(BuildContext ctx, String etiqueta, num valor) {
    final seleccionada = (widget.cantidad - valor).abs() < 0.0001;
    return InkWell(
      onTap: () {
        widget.onFijar(valor);
        Navigator.of(ctx).pop();
      },
      borderRadius: BorderRadius.circular(20),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
        decoration: BoxDecoration(
          color: seleccionada ? RegentaColors.posAcentoFuerte : RegentaColors.posChipFondo,
          borderRadius: BorderRadius.circular(20),
          border: Border.all(
            color: seleccionada ? RegentaColors.posChipBordeSeleccionado : RegentaColors.posChipBorde,
            width: seleccionada ? 1.8 : 1.2,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            if (seleccionada) ...[
              const Icon(Icons.check, size: 15, color: Colors.white),
              const SizedBox(width: 6),
            ],
            Text(
              etiqueta,
              style: TextStyle(
                fontSize: 13,
                fontWeight: seleccionada ? FontWeight.w800 : FontWeight.w600,
                color: seleccionada ? Colors.white : RegentaColors.posModalTitulo,
              ),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final esOscuro = Theme.of(context).brightness == Brightness.dark;
    final colorFondo = esOscuro ? RegentaColors.stepperFondoOscuro : RegentaColors.surface;
    final colorTexto = esOscuro ? RegentaColors.kdsTexto : RegentaColors.inkFuerte;
    final colorBorde = _editando
        ? RegentaColors.accent
        : (esOscuro ? RegentaColors.stepperBordeOscuro : RegentaColors.line);

    return Container(
      constraints: const BoxConstraints(minHeight: 46),
      decoration: BoxDecoration(
        color: colorFondo,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(
          color: colorBorde,
          width: _editando ? 1.8 : 1.0,
        ),
        boxShadow: _editando
            ? [
                BoxShadow(
                  color: RegentaColors.accent.withValues(alpha: 0.35),
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
            width: 58,
            child: _editando
                ? Container(
                    decoration: BoxDecoration(
                      color: esOscuro ? RegentaColors.stepperCampoOscuro : Colors.white,
                      borderRadius: BorderRadius.circular(6),
                      border: Border.all(
                        color: RegentaColors.posAcentoFuerte,
                        width: 2.0,
                      ),
                    ),
                    alignment: Alignment.center,
                    child: TextField(
                      controller: _ctrl,
                      focusNode: _foco,
                      keyboardType: const TextInputType.numberWithOptions(decimal: true),
                      textAlign: TextAlign.center,
                      cursorColor: RegentaColors.posAcentoFuerte,
                      style: TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 15,
                        fontWeight: FontWeight.w900,
                        color: esOscuro ? Colors.white : RegentaColors.stepperCampoOscuro,
                      ),
                      decoration: const InputDecoration(
                        border: InputBorder.none,
                        contentPadding: EdgeInsets.zero,
                        isDense: true,
                      ),
                      onSubmitted: (_) => _guardar(),
                    ),
                  )
                : InkWell(
                    key: const Key('stepper-cantidad-tap'),
                    onTap: () {
                      setState(() => _editando = true);
                      _ctrl.text = _formatearCantidad(widget.cantidad);
                      _foco.requestFocus();
                      _ctrl.selection = TextSelection(
                        baseOffset: 0,
                        extentOffset: _ctrl.text.length,
                      );
                    },
                    child: Center(
                      child: Text(
                        _formatearCantidad(widget.cantidad),
                        textAlign: TextAlign.center,
                        style: RegentaType.codigo.copyWith(
                          fontSize: 14,
                          fontWeight: FontWeight.w800,
                          color: colorTexto,
                        ),
                      ),
                    ),
                  ),
          ),
          _BotonStepper(icono: Icons.add, onTap: widget.onMas, semantica: 'Agregar uno'),
          Tooltip(
            message: 'Fraccionar (galón, metros...)',
            child: InkWell(
              onTap: _abrirSelectorFracciones,
              borderRadius: BorderRadius.circular(8),
              child: const Padding(
                padding: EdgeInsets.symmetric(horizontal: 5, vertical: 8),
                child: Icon(
                  Icons.pie_chart_outline,
                  size: 17,
                  color: RegentaColors.accent,
                ),
              ),
            ),
          ),
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

/// Fila dentro del diálogo de detalle de producto con alto contraste.
class _FilaDetalle extends StatelessWidget {
  const _FilaDetalle(this.etiqueta, this.valor, {this.valorColor});

  final String etiqueta;
  final String valor;
  final Color? valorColor;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 10, horizontal: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Expanded(
            child: Text(
              etiqueta,
              style: const TextStyle(
                fontSize: 13.5,
                color: RegentaColors.posModalTextoTenue,
                fontWeight: FontWeight.w600,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Text(
            valor,
            style: TextStyle(
              fontFamily: 'monospace',
              fontSize: 14,
              fontWeight: FontWeight.w700,
              color: valorColor ?? Colors.white,
            ),
          ),
        ],
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

class _DialogoCargarCotizacion extends StatefulWidget {
  const _DialogoCargarCotizacion({
    required this.repo,
    required this.bodegaId,
    required this.onCotizacionConvertida,
  });

  final RepositorioDeVentas repo;
  final String bodegaId;
  final Future<void> Function(ResultadoDeConversion resultado, CotizacionDelNegocio cotizacion)
      onCotizacionConvertida;

  @override
  State<_DialogoCargarCotizacion> createState() => _DialogoCargarCotizacionState();
}

class _DialogoCargarCotizacionState extends State<_DialogoCargarCotizacion> {
  List<CotizacionDelNegocio> _cotizaciones = [];
  bool _cargando = true;
  String? _error;
  String? _convirtiendoId;

  @override
  void initState() {
    super.initState();
    _cargar();
  }

  Future<void> _cargar() async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final lista = await widget.repo.listarCotizaciones();
      if (mounted) {
        setState(() {
          _cotizaciones = lista;
          _cargando = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Error al cargar cotizaciones: $e';
          _cargando = false;
        });
      }
    }
  }

  Future<void> _convertir(CotizacionDelNegocio c) async {
    setState(() => _convirtiendoId = c.id);
    try {
      final res = await widget.repo.convertirCotizacion(c.id, widget.bodegaId);
      if (mounted) {
        Navigator.of(context).pop();
      }
      await widget.onCotizacionConvertida(res, c);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: Theme.of(context).colorScheme.error,
            content: Text('Error al convertir cotización: $e'),
          ),
        );
        setState(() => _convirtiendoId = null);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);

    return AlertDialog(
      key: const Key('dialogo-cargar-cotizacion'),
      title: const Row(
        children: [
          Icon(Icons.request_quote_outlined, color: RegentaColors.accent),
          SizedBox(width: 10),
          Text('Cotizaciones registradas'),
        ],
      ),
      content: SizedBox(
        width: 550,
        height: 400,
        child: _cargando
            ? const Center(child: CircularProgressIndicator())
            : _error != null
                ? Center(
                    child: Text(_error!, style: TextStyle(color: tema.colorScheme.error)),
                  )
                : _cotizaciones.isEmpty
                    ? Center(
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.inbox_outlined, size: 48, color: RegentaColors.faint),
                            const SizedBox(height: 8),
                            Text(
                              'No hay cotizaciones registradas',
                              style: PastelTheme.cuerpo(color: RegentaColors.ink2),
                            ),
                          ],
                        ),
                      )
                    : ListView.separated(
                        itemCount: _cotizaciones.length,
                        separatorBuilder: (context, _) => const Divider(height: 1),
                        itemBuilder: (context, index) {
                          final c = _cotizaciones[index];
                          final yaConvertida = c.convertida;
                          final convirtiendo = _convirtiendoId == c.id;

                          return ListTile(
                            key: Key('cotizacion-item-${c.id}'),
                            leading: CircleAvatar(
                              backgroundColor: yaConvertida
                                  ? Colors.grey.shade200
                                  : RegentaColors.accent.withValues(alpha: 0.1),
                              child: Icon(
                                Icons.description_outlined,
                                color: yaConvertida ? Colors.grey : RegentaColors.accent,
                              ),
                            ),
                            title: Wrap(
                              crossAxisAlignment: WrapCrossAlignment.center,
                              children: [
                                Text(
                                  'Cotización #${c.numero}',
                                  style: const TextStyle(fontWeight: FontWeight.bold),
                                ),
                                const SizedBox(width: 8),
                                Container(
                                  padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                  decoration: BoxDecoration(
                                    color: yaConvertida
                                        ? Colors.grey.shade100
                                        : Colors.green.shade50,
                                    borderRadius: BorderRadius.circular(4),
                                    border: Border.all(
                                      color: yaConvertida ? Colors.grey : Colors.green,
                                    ),
                                  ),
                                  child: Text(
                                    c.estado,
                                    style: TextStyle(
                                      fontSize: 11,
                                      fontWeight: FontWeight.w600,
                                      color: yaConvertida ? Colors.grey : Colors.green.shade800,
                                    ),
                                  ),
                                ),
                              ],
                            ),
                            subtitle: Text(
                              'Total: ${formatearPesos(c.total)}${c.validaHasta != null ? ' · Vence: ${c.validaHasta}' : ''}',
                            ),
                            trailing: yaConvertida
                                ? const Chip(
                                    label: Text('Convertida', style: TextStyle(fontSize: 11)),
                                  )
                                : FilledButton.tonal(
                                    key: Key('boton-convertir-cotizacion-${c.id}'),
                                    onPressed: convirtiendo ? null : () => _convertir(c),
                                    child: convirtiendo
                                        ? const SizedBox(
                                            width: 16,
                                            height: 16,
                                            child: CircularProgressIndicator(strokeWidth: 2),
                                          )
                                        : const Text('Convertir'),
                                  ),
                          );
                        },
                      ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cerrar'),
        ),
      ],
    );
  }
}
