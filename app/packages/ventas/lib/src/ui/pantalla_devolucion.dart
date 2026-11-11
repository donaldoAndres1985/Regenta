import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../datos/repositorio_de_ventas.dart';
import '../pos/proveedores.dart';
import 'formato.dart';

/// Pantalla para registrar devoluciones totales o parciales (HU-042).
class PantallaDevolucion extends ConsumerStatefulWidget {
  const PantallaDevolucion({
    super.key,
    required this.venta,
    this.bodegaId,
    this.onDevolucionExitosa,
  });

  final VentaEnProceso venta;
  final String? bodegaId;
  final void Function(DevolucionRegistrada devolucion)? onDevolucionExitosa;

  @override
  ConsumerState<PantallaDevolucion> createState() => _PantallaDevolucionState();
}

class _PantallaDevolucionState extends ConsumerState<PantallaDevolucion> {
  late VentaEnProceso _venta;
  bool _cargandoLineas = false;
  MotivoDevolucion _motivo = MotivoDevolucion.arrepentimiento;
  bool _reintegraStock = true;
  final _detalleCtrl = TextEditingController();
  final Map<int, num> _cantidadesDevolver = {};
  bool _enviando = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    _venta = widget.venta;
    if (_venta.lineas.isEmpty) {
      _cargarDetalleVenta();
    } else {
      _inicializarCantidades();
    }
  }

  @override
  void dispose() {
    _detalleCtrl.dispose();
    super.dispose();
  }

  void _inicializarCantidades() {
    for (final l in _venta.lineas) {
      _cantidadesDevolver[l.linea] = l.cantidad;
    }
  }

  Future<void> _cargarDetalleVenta() async {
    setState(() => _cargandoLineas = true);
    try {
      final repo = ref.read(repositorioDeVentasProvider);
      final ventaCompleta = await repo.verVenta(_venta.id);
      if (mounted) {
        setState(() {
          _venta = ventaCompleta;
          _cargandoLineas = false;
          _inicializarCantidades();
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Error al cargar líneas de la venta: $e';
          _cargandoLineas = false;
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

  num get _totalDevolucion {
    num total = 0;
    for (final l in _venta.lineas) {
      final cant = _cantidadesDevolver[l.linea] ?? 0;
      if (cant > 0) {
        total += cant * l.precioUnitario;
      }
    }
    return total;
  }

  bool get _tieneCantidadesParaDevolver {
    return _cantidadesDevolver.values.any((c) => c > 0);
  }

  Future<void> _enviarDevolucion() async {
    if (!_tieneCantidadesParaDevolver) return;

    final lineasParaEnviar = <LineaDevueltaParaEnviar>[];
    for (final entry in _cantidadesDevolver.entries) {
      if (entry.value > 0) {
        lineasParaEnviar.add(
          LineaDevueltaParaEnviar(
            linea: entry.key,
            cantidad: entry.value,
          ),
        );
      }
    }

    setState(() {
      _enviando = true;
      _error = null;
    });

    try {
      final repo = ref.read(repositorioDeVentasProvider);
      final solicitud = SolicitudDeDevolucionParaEnviar(
        motivo: _motivo.codigo,
        detalle: _detalleCtrl.text.trim().isNotEmpty ? _detalleCtrl.text.trim() : null,
        reintegraStock: _reintegraStock,
        bodegaDestinoId: _reintegraStock ? widget.bodegaId : null,
        lineas: lineasParaEnviar,
      );

      final resultado = await repo.registrarDevolucion(_venta.id, solicitud);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            backgroundColor: const Color(0xFF2E7D32),
            content: Text(
              'Devolución ${resultado.numero} registrada con éxito. Tipo: ${resultado.tipo}',
            ),
          ),
        );
        widget.onDevolucionExitosa?.call(resultado);
        Navigator.of(context).pop();
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _error = 'Error al registrar devolución: $e';
          _enviando = false;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final tema = Theme.of(context);
    final nombreCliente = _extraerNombreCliente(_venta.clienteSnapshot);

    return Scaffold(
      appBar: AppBar(
        title: Text('Devolución de venta #${_venta.numero}'),
      ),
      body: _cargandoLineas
          ? const Center(child: CircularProgressIndicator())
          : SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Alerta de Facturación DIAN si aplica
                  if (_venta.facturada) ...[
                    Container(
                      key: const Key('alerta-factura-dian'),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: Colors.blue.withValues(alpha: 0.1),
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: Colors.blue.shade300),
                      ),
                      child: Row(
                        children: [
                          const Icon(Icons.info_outline, color: Colors.blue),
                          const SizedBox(width: 12),
                          Expanded(
                            child: Text(
                              'Esta venta fue facturada electrónicamente; al procesar la devolución se generará la respectiva nota crédito ante la DIAN.',
                              style: tema.textTheme.bodyMedium?.copyWith(
                                color: Colors.blue.shade900,
                                fontWeight: FontWeight.w500,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],

                  // Resumen de la venta
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.spaceBetween,
                        children: [
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Cliente: $nombreCliente',
                                style: tema.textTheme.titleSmall?.copyWith(
                                  fontWeight: FontWeight.bold,
                                ),
                              ),
                              const SizedBox(height: 4),
                              Text('Estado actual: ${_venta.estado}'),
                            ],
                          ),
                          Column(
                            crossAxisAlignment: CrossAxisAlignment.end,
                            children: [
                              Text('Total venta: ${formatearPesos(_venta.total)}'),
                              const SizedBox(height: 4),
                              Text(
                                'A devolver: ${formatearPesos(_totalDevolucion)}',
                                style: tema.textTheme.titleMedium?.copyWith(
                                  fontWeight: FontWeight.bold,
                                  color: tema.colorScheme.primary,
                                ),
                              ),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ),

                  const SizedBox(height: 16),

                  // Configuración de la devolución: Motivo y Reintegro
                  Text(
                    'Motivo de la devolución',
                    style: tema.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),
                  DropdownButtonFormField<MotivoDevolucion>(
                    key: const Key('dropdown-motivo-devolucion'),
                    initialValue: _motivo,
                    decoration: const InputDecoration(
                      border: OutlineInputBorder(),
                      contentPadding: EdgeInsets.symmetric(horizontal: 12, vertical: 12),
                    ),
                    items: MotivoDevolucion.values.map((m) {
                      return DropdownMenuItem(
                        value: m,
                        child: Text(m.etiqueta),
                      );
                    }).toList(),
                    onChanged: (nuevo) {
                      if (nuevo == null) return;
                      setState(() {
                        _motivo = nuevo;
                        // Si es defectuoso, por defecto no se reintegra al stock vendible
                        if (nuevo == MotivoDevolucion.defectuoso) {
                          _reintegraStock = false;
                        } else {
                          _reintegraStock = true;
                        }
                      });
                    },
                  ),

                  const SizedBox(height: 12),

                  SwitchListTile(
                    key: const Key('switch-reintegrar-stock'),
                    title: const Text('Reintegrar productos al stock de inventario'),
                    subtitle: Text(
                      _reintegraStock
                          ? 'Los productos devueltos volverán a estar disponibles para la venta.'
                          : 'Los productos NO volverán al stock vendible (útil para mercancía dañada o en garantía).',
                    ),
                    value: _reintegraStock,
                    onChanged: (val) => setState(() => _reintegraStock = val),
                  ),

                  const SizedBox(height: 8),

                  TextField(
                    key: const Key('input-detalle-devolucion'),
                    controller: _detalleCtrl,
                    maxLines: 2,
                    decoration: const InputDecoration(
                      labelText: 'Detalle u observaciones (opcional)',
                      hintText: 'Explica cualquier detalle relevante sobre esta devolución...',
                      border: OutlineInputBorder(),
                    ),
                  ),

                  const SizedBox(height: 24),

                  Text(
                    'Productos a devolver',
                    style: tema.textTheme.titleMedium?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 8),

                  // Listado de líneas
                  if (_venta.lineas.isEmpty)
                    const Padding(
                      padding: EdgeInsets.all(16.0),
                      child: Text('Esta venta no tiene líneas de productos asociadas.'),
                    )
                  else
                    ListView.separated(
                      shrinkWrap: true,
                      physics: const NeverScrollableScrollPhysics(),
                      itemCount: _venta.lineas.length,
                      separatorBuilder: (context, _) => const Divider(),
                      itemBuilder: (context, index) {
                        final linea = _venta.lineas[index];
                        final cantidadDevueltaActual = _cantidadesDevolver[linea.linea] ?? 0;

                        return Padding(
                          padding: const EdgeInsets.symmetric(vertical: 4),
                          child: Row(
                            children: [
                              Expanded(
                                flex: 3,
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      linea.nombre,
                                      style: const TextStyle(fontWeight: FontWeight.w600),
                                    ),
                                    Text(
                                      'SKU: ${linea.sku} · Vendido: ${linea.cantidad} und · Unit: ${formatearPesos(linea.precioUnitario)}',
                                      style: tema.textTheme.bodySmall?.copyWith(
                                        color: tema.colorScheme.onSurfaceVariant,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              const SizedBox(width: 8),
                              // Control de cantidad a devolver
                              Row(
                                mainAxisSize: MainAxisSize.min,
                                children: [
                                  IconButton(
                                    icon: const Icon(Icons.remove_circle_outline),
                                    onPressed: cantidadDevueltaActual > 0
                                        ? () {
                                            setState(() {
                                              _cantidadesDevolver[linea.linea] =
                                                  (cantidadDevueltaActual - 1).clamp(0, linea.cantidad);
                                            });
                                          }
                                        : null,
                                  ),
                                  SizedBox(
                                    width: 70,
                                    child: TextFormField(
                                      key: Key('input-devolver-linea-${linea.linea}'),
                                      initialValue: cantidadDevueltaActual.toString(),
                                      textAlign: TextAlign.center,
                                      keyboardType:
                                          const TextInputType.numberWithOptions(decimal: true),
                                      decoration: const InputDecoration(
                                        isDense: true,
                                        contentPadding:
                                            EdgeInsets.symmetric(horizontal: 4, vertical: 8),
                                        border: OutlineInputBorder(),
                                      ),
                                      onChanged: (val) {
                                        final numParsed = num.tryParse(val) ?? 0;
                                        setState(() {
                                          _cantidadesDevolver[linea.linea] =
                                              numParsed.clamp(0, linea.cantidad);
                                        });
                                      },
                                    ),
                                  ),
                                  IconButton(
                                    icon: const Icon(Icons.add_circle_outline),
                                    onPressed: cantidadDevueltaActual < linea.cantidad
                                        ? () {
                                            setState(() {
                                              _cantidadesDevolver[linea.linea] =
                                                  (cantidadDevueltaActual + 1).clamp(0, linea.cantidad);
                                            });
                                          }
                                        : null,
                                  ),
                                ],
                              ),
                            ],
                          ),
                        );
                      },
                    ),

                  if (_error != null) ...[
                    const SizedBox(height: 16),
                    Text(
                      _error!,
                      style: TextStyle(color: tema.colorScheme.error),
                    ),
                  ],

                  const SizedBox(height: 24),

                  SizedBox(
                    width: double.infinity,
                    height: 48,
                    child: FilledButton.icon(
                      key: const Key('boton-confirmar-devolucion'),
                      icon: _enviando
                          ? const SizedBox(
                              width: 20,
                              height: 20,
                              child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2),
                            )
                          : const Icon(Icons.check_circle_outline),
                      label: Text(
                        _enviando
                            ? 'Procesando devolución...'
                            : 'Registrar devolución (${formatearPesos(_totalDevolucion)})',
                      ),
                      onPressed:
                          _enviando || !_tieneCantidadesParaDevolver ? null : _enviarDevolucion,
                    ),
                  ),
                ],
              ),
            ),
    );
  }
}
