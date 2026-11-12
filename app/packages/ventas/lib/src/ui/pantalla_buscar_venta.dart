import 'dart:async';
import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_ventas.dart';
import '../pos/proveedores.dart';
import 'formato.dart';
import 'pantalla_devolucion.dart';
import 'ticket_pos.dart';

/// Pantalla para buscar ventas previas, consultar su estado, anularlas
/// (HU-041) o iniciar una devolución (HU-042).
class PantallaBuscarVenta extends ConsumerStatefulWidget {
  const PantallaBuscarVenta({
    super.key,
    this.onVentaAnulada,
    this.onSeleccionarParaDevolucion,
  });

  final void Function(VentaEnProceso venta)? onVentaAnulada;
  final void Function(VentaEnProceso venta)? onSeleccionarParaDevolucion;

  @override
  ConsumerState<PantallaBuscarVenta> createState() => _PantallaBuscarVentaState();
}

class _PantallaBuscarVentaState extends ConsumerState<PantallaBuscarVenta> {
  final _controladorBusqueda = TextEditingController();
  final _focoBusqueda = FocusNode();
  Timer? _temporizadorDeBusqueda;
  List<VentaEnProceso> _ventas = [];
  bool _cargando = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _focoBusqueda.onKeyEvent = _manejarTeclasBusqueda;
    _cargarVentas();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        _focoBusqueda.requestFocus();
      }
    });
  }

  @override
  void dispose() {
    _temporizadorDeBusqueda?.cancel();
    _focoBusqueda.dispose();
    _controladorBusqueda.dispose();
    super.dispose();
  }

  KeyEventResult _manejarTeclasBusqueda(FocusNode node, KeyEvent event) {
    if (event is! KeyDownEvent) return KeyEventResult.ignored;
    if (event.logicalKey == LogicalKeyboardKey.escape) {
      _cerrar();
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  void _cerrar() {
    if (Navigator.of(context).canPop()) {
      Navigator.of(context).pop();
    }
  }

  Future<void> _cargarVentas([String? termino]) async {
    setState(() {
      _cargando = true;
      _error = null;
    });
    try {
      final repo = ref.read(repositorioDeVentasProvider);
      final resultado = await repo.buscarVentas(q: termino);
      if (mounted) {
        setState(() {
          _ventas = resultado;
          _cargando = false;
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Error al cargar ventas: $e';
          _cargando = false;
        });
      }
    }
  }

  String _extraerNombreCliente(String? snapshot) {
    if (snapshot == null || snapshot.trim().isEmpty) return 'Consumidor final';
    try {
      final mapa = jsonDecode(snapshot);
      if (mapa is Map) {
        final nombre = mapa['nombreCompleto'] ?? mapa['razonSocial'] ?? mapa['nombre'];
        if (nombre != null && nombre.toString().trim().isNotEmpty) {
          return nombre.toString();
        }
      }
    } catch (_) {}
    return 'Consumidor final';
  }

  Future<void> _abrirDialogoAnulacion(VentaEnProceso venta) async {
    if (venta.anulada) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Esta venta ya se encuentra anulada')),
      );
      return;
    }
    if (!venta.confirmada) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Solo se pueden anular ventas confirmadas (estado: ${venta.estado})')),
      );
      return;
    }
    if (venta.facturada) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          backgroundColor: Theme.of(context).colorScheme.error,
          content: Text(
            'La venta ${venta.numero} ya fue facturada electrónicamente: emite una nota crédito para revertirla',
          ),
        ),
      );
      return;
    }

    final motivoCtrl = TextEditingController();
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (ctx) {
        String? errorMotivo;
        return StatefulBuilder(
          builder: (ctx, setDialogState) {
            final tema = Theme.of(ctx);
            return AlertDialog(
              key: const Key('dialogo-anular-venta'),
              title: Text('Anular venta ${venta.numero}'),
              content: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Al anular la venta, los productos salientes volverán automáticamente a estar disponibles en el stock.',
                    style: tema.textTheme.bodyMedium?.copyWith(
                      color: tema.colorScheme.onSurfaceVariant,
                    ),
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    key: const Key('input-motivo-anulacion'),
                    controller: motivoCtrl,
                    autofocus: true,
                    maxLines: 2,
                    decoration: InputDecoration(
                      labelText: 'Motivo de anulación (obligatorio)',
                      hintText: 'Ej: Error de digitación, cliente canceló el pedido...',
                      errorText: errorMotivo,
                      border: const OutlineInputBorder(),
                    ),
                    onChanged: (_) {
                      if (errorMotivo != null) {
                        setDialogState(() => errorMotivo = null);
                      }
                    },
                  ),
                ],
              ),
              actions: [
                TextButton(
                  onPressed: () => Navigator.of(ctx).pop(false),
                  child: const Text('Cancelar'),
                ),
                FilledButton(
                  key: const Key('boton-confirmar-anulacion'),
                  style: FilledButton.styleFrom(
                    backgroundColor: tema.colorScheme.error,
                    foregroundColor: tema.colorScheme.onError,
                  ),
                  onPressed: () {
                    if (motivoCtrl.text.trim().isEmpty) {
                      setDialogState(() {
                        errorMotivo = 'Indica el motivo de la anulación';
                      });
                      return;
                    }
                    Navigator.of(ctx).pop(true);
                  },
                  child: const Text('Confirmar anulación'),
                ),
              ],
            );
          },
        );
      },
    );

    if (confirmado == true) {
      try {
        final repo = ref.read(repositorioDeVentasProvider);
        await repo.anularVenta(venta.id, motivoCtrl.text.trim());
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text(
                'Venta ${venta.numero} anulada. El stock fue reintegrado al inventario.',
              ),
            ),
          );
          // Actualizar estado local
          setState(() {
            _ventas = _ventas.map((v) {
              if (v.id == venta.id) {
                return VentaEnProceso(
                  id: v.id,
                  numero: v.numero,
                  estado: 'ANULADA',
                  total: v.total,
                  notaCliente: v.notaCliente,
                  facturada: v.facturada,
                  clienteSnapshot: v.clienteSnapshot,
                  lineas: v.lineas,
                );
              }
              return v;
            }).toList();
          });
          widget.onVentaAnulada?.call(venta);
        }
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              backgroundColor: Theme.of(context).colorScheme.error,
              content: Text('Error al anular la venta: $e'),
            ),
          );
        }
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final ventasFiltradas = _controladorBusqueda.text.trim().isEmpty
        ? _ventas
        : _ventas.where((v) => v.coincideCon(_controladorBusqueda.text)).toList();

    return CallbackShortcuts(
      bindings: {
        const SingleActivator(LogicalKeyboardKey.escape): _cerrar,
      },
      child: Scaffold(
      appBar: AppBar(
        title: const Text('Historial y anulación de ventas'),
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            tooltip: 'Actualizar lista',
            onPressed: () => _cargarVentas(_controladorBusqueda.text),
          ),
        ],
      ),
      body: Column(
        children: [
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: TextField(
              key: const Key('buscar-venta-input'),
              controller: _controladorBusqueda,
              focusNode: _focoBusqueda,
              autofocus: true,
              decoration: InputDecoration(
                prefixIcon: const Icon(Icons.search),
                hintText: 'Buscar por consecutivo o cliente...',
                border: const OutlineInputBorder(),
                suffixIcon: _controladorBusqueda.text.isNotEmpty
                    ? IconButton(
                        icon: const Icon(Icons.clear),
                        onPressed: () {
                          _controladorBusqueda.clear();
                          _cargarVentas();
                          _focoBusqueda.requestFocus();
                        },
                      )
                    : null,
              ),
              onSubmitted: (texto) {
                _temporizadorDeBusqueda?.cancel();
                _cargarVentas(texto);
              },
              onChanged: (texto) {
                _temporizadorDeBusqueda?.cancel();
                _temporizadorDeBusqueda = Timer(const Duration(milliseconds: 200), () {
                  _cargarVentas(texto);
                });
              },
            ),
          ),
          if (_error != null)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16.0),
              child: Text(
                _error!,
                style: TextStyle(color: tema.colorScheme.error),
              ),
            ),
          if (_cargando)
            const Expanded(child: Center(child: CircularProgressIndicator()))
          else if (ventasFiltradas.isEmpty)
            Expanded(
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(Icons.receipt_long, size: 64, color: tema.colorScheme.outline),
                    const SizedBox(height: 12),
                    Text(
                      'No se encontraron ventas',
                      style: tema.textTheme.titleMedium?.copyWith(
                        color: tema.colorScheme.onSurfaceVariant,
                      ),
                    ),
                  ],
                ),
              ),
            )
          else
            Expanded(
              child: ListView.separated(
                key: const Key('lista-de-ventas'),
                padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                itemCount: ventasFiltradas.length,
                separatorBuilder: (context, _) => const SizedBox(height: 8),
                itemBuilder: (context, index) {
                  final v = ventasFiltradas[index];
                  final nombreCliente = _extraerNombreCliente(v.clienteSnapshot);
                  Color estadoColor;
                  switch (v.estado) {
                    case 'CONFIRMADA':
                      estadoColor = RegentaColors.ok;
                      break;
                    case 'ANULADA':
                      estadoColor = RegentaColors.crit;
                      break;
                    case 'DEVUELTA':
                    case 'DEVUELTA_PARCIAL':
                      estadoColor = RegentaColors.warn;
                      break;
                    default:
                      estadoColor = RegentaColors.muted;
                  }

                  return Card(
                    key: Key('venta-tarjeta-${v.id}'),
                    elevation: 1,
                    child: Padding(
                      padding: const EdgeInsets.all(12),
                      child: Row(
                        children: [
                          Icon(
                            Icons.receipt,
                            size: 36,
                            color: tema.colorScheme.primary,
                          ),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Wrap(
                                  spacing: 6,
                                  runSpacing: 4,
                                  crossAxisAlignment: WrapCrossAlignment.center,
                                  children: [
                                    Text(
                                      'Venta #${v.numero}',
                                      style: tema.textTheme.titleMedium?.copyWith(
                                        fontWeight: FontWeight.bold,
                                      ),
                                    ),
                                    Container(
                                      padding: const EdgeInsets.symmetric(
                                        horizontal: 8,
                                        vertical: 2,
                                      ),
                                      decoration: BoxDecoration(
                                        color: estadoColor.withValues(alpha: 0.15),
                                        borderRadius: BorderRadius.circular(4),
                                        border: Border.all(color: estadoColor),
                                      ),
                                      child: Text(
                                        v.estado,
                                        style: TextStyle(
                                          color: estadoColor,
                                          fontWeight: FontWeight.w600,
                                          fontSize: 12,
                                        ),
                                      ),
                                    ),
                                    if (v.facturada) ...[
                                      const SizedBox(width: 6),
                                      Container(
                                        padding: const EdgeInsets.symmetric(
                                          horizontal: 6,
                                          vertical: 2,
                                        ),
                                        decoration: BoxDecoration(
                                          color: Colors.blue.withValues(alpha: 0.15),
                                          borderRadius: BorderRadius.circular(4),
                                          border: Border.all(color: Colors.blue),
                                        ),
                                        child: const Text(
                                          'DIAN',
                                          style: TextStyle(
                                            color: Colors.blue,
                                            fontWeight: FontWeight.bold,
                                            fontSize: 11,
                                          ),
                                        ),
                                      ),
                                    ],
                                  ],
                                ),
                                const SizedBox(height: 4),
                                Text(
                                  'Cliente: $nombreCliente',
                                  style: tema.textTheme.bodyMedium?.copyWith(
                                    color: tema.colorScheme.onSurfaceVariant,
                                  ),
                                ),
                                if (v.notaCliente != null && v.notaCliente!.isNotEmpty)
                                  Text(
                                    'Nota: ${v.notaCliente}',
                                    style: tema.textTheme.bodySmall?.copyWith(
                                      fontStyle: FontStyle.italic,
                                    ),
                                  ),
                              ],
                            ),
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text(
                                formatearPesos(v.total),
                                style: tema.textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: tema.colorScheme.primary,
                                ),
                              ),
                              const SizedBox(height: 6),
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  IconButton(
                                    key: Key('boton-ticket-venta-${v.id}'),
                                    tooltip: 'Imprimir / Ver ticket',
                                    icon: const Icon(Icons.print_outlined),
                                    onPressed: () async {
                                      final repo = ref.read(repositorioDeVentasProvider);
                                      final ventaCompleta =
                                          v.lineas.isEmpty ? await repo.verVenta(v.id) : v;
                                      if (context.mounted) {
                                        await GeneradorTicketPos.imprimirOPrevisualizar(
                                          context,
                                          nombreNegocio: 'Regenta',
                                          venta: ventaCompleta,
                                        );
                                      }
                                    },
                                  ),
                                  const SizedBox(width: 4),
                                  if (v.confirmada) ...[
                                    OutlinedButton.icon(
                                      key: Key('boton-devolver-venta-${v.id}'),
                                      icon: const Icon(Icons.assignment_return, size: 16),
                                      label: const Text('Devolver'),
                                      onPressed: () {
                                        if (widget.onSeleccionarParaDevolucion != null) {
                                          widget.onSeleccionarParaDevolucion!(v);
                                        } else {
                                          Navigator.of(context).push(
                                            MaterialPageRoute(
                                              builder: (_) => PantallaDevolucion(
                                                venta: v,
                                                onDevolucionExitosa: (_) => _cargarVentas(),
                                              ),
                                            ),
                                          );
                                        }
                                      },
                                    ),
                                    const SizedBox(width: 8),
                                  ],
                                  if (v.confirmada)
                                    FilledButton.tonalIcon(
                                      key: Key('boton-anular-venta-${v.id}'),
                                      style: FilledButton.styleFrom(
                                        foregroundColor: tema.colorScheme.error,
                                      ),
                                      icon: const Icon(Icons.cancel_outlined, size: 16),
                                      label: const Text('Anular'),
                                      onPressed: () => _abrirDialogoAnulacion(v),
                                    ),
                                ],
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  );
                },
              ),
            ),
        ],
      ),
    ),
    );
  }
}
