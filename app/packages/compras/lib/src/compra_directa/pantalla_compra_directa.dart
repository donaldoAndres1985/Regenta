import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/datos/producto_del_proveedor.dart';
import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'controlador.dart';
import 'estado.dart';

/// Compra directa (HU-167): la mercancía llegó con la factura del proveedor y se registra de una,
/// sin orden. Proveedor, bodega y factura arriba; los productos con su costo, descuento, IVA y lote;
/// abajo la base, el IVA, el flete y el total que queda debiéndose.
class PantallaCompraDirecta extends ConsumerStatefulWidget {
  const PantallaCompraDirecta({super.key});

  @override
  ConsumerState<PantallaCompraDirecta> createState() => _PantallaCompraDirectaState();
}

class _PantallaCompraDirectaState extends ConsumerState<PantallaCompraDirecta> {
  final TextEditingController _buscador = TextEditingController();
  final TextEditingController _factura = TextEditingController();
  final TextEditingController _flete = TextEditingController();
  final TextEditingController _descuento = TextEditingController();

  @override
  void dispose() {
    _buscador.dispose();
    _factura.dispose();
    _flete.dispose();
    _descuento.dispose();
    super.dispose();
  }

  Future<void> _registrar() async {
    final ctrl = ref.read(controladorDeCompraDirectaProvider.notifier);
    final hecha = await ctrl.registrar();
    if (hecha == null || !mounted) return;
    _factura.clear();
    _flete.clear();
    _descuento.clear();
    _buscador.clear();
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(
          'Compra ${hecha.numero} registrada por ${formatearPesos(hecha.total)}. '
          'El inventario ya la tiene.${hecha.avisos.isEmpty ? '' : ' ${hecha.avisos.join(' ')}'}',
        ),
      ),
    );
  }

  Future<void> _elegirFecha(DateTime? actual) async {
    final hoy = DateTime.now();
    final elegida = await showDatePicker(
      context: context,
      initialDate: actual ?? hoy,
      firstDate: DateTime(hoy.year - 1),
      lastDate: hoy,
    );
    if (elegida != null) ref.read(controladorDeCompraDirectaProvider.notifier).cambiarFecha(elegida);
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeCompraDirectaProvider);
    final ctrl = ref.read(controladorDeCompraDirectaProvider.notifier);

    return LayoutBuilder(
      builder: (context, afuera) {
        final escritorio = afuera.maxWidth >= kBreakpointEscritorio;
        return Scaffold(
          backgroundColor: RegentaColors.paper,
          appBar: escritorio
              ? const EncabezadoDeSeccion(
                  subtitulo: 'La mercancía llegó con su factura: regístrala sin orden',
                  titulo: 'Compra directa',
                )
              : AppBar(
                  backgroundColor: RegentaColors.surface,
                  surfaceTintColor: RegentaColors.surface,
                  elevation: 0,
                  shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
                  title: Text(
                    'Compra directa',
                    style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink),
                  ),
                ),
          body: estado.cargando && estado.proveedores.isEmpty
              ? const Center(child: CircularProgressIndicator())
              : SingleChildScrollView(
                  key: Key(escritorio ? 'compra-directa-escritorio' : 'compra-directa-movil'),
                  padding: const EdgeInsets.all(14),
                  child: Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 980),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          _Tarjeta(
                            titulo: 'Factura del proveedor',
                            hijo: _Cabecera(
                              estado: estado,
                              factura: _factura,
                              flete: _flete,
                              descuento: _descuento,
                              onProveedor: ctrl.elegirProveedor,
                              onBodega: ctrl.elegirBodega,
                              onFactura: ctrl.cambiarFactura,
                              onFlete: (t) => ctrl.cambiarFlete(num.tryParse(_digitos(t)) ?? 0),
                              onDescuento: (t) => ctrl.cambiarDescuentoGlobal(num.tryParse(_digitos(t)) ?? 0),
                              onFecha: () => unawaited(_elegirFecha(estado.fechaFactura)),
                            ),
                          ),
                          const SizedBox(height: 12),
                          _Tarjeta(
                            titulo: 'Productos',
                            hijo: Column(
                              crossAxisAlignment: CrossAxisAlignment.stretch,
                              children: [
                                _Buscador(
                                  controlador: _buscador,
                                  habilitado: estado.proveedorId != null,
                                  buscando: estado.buscando,
                                  onBuscar: () => unawaited(ctrl.buscar(_buscador.text)),
                                ),
                                if (estado.resultados.isNotEmpty)
                                  _Resultados(
                                    estado: estado,
                                    onElegir: (p) {
                                      _buscador.clear();
                                      unawaited(ctrl.agregar(p));
                                    },
                                  ),
                                const SizedBox(height: 8),
                                if (estado.lineas.isEmpty)
                                  Padding(
                                    padding: const EdgeInsets.symmetric(vertical: 18),
                                    child: Text(
                                      estado.proveedorId == null
                                          ? 'Elige el proveedor para buscar los productos de la factura.'
                                          : 'Busca los productos de la factura y agrégalos.',
                                      textAlign: TextAlign.center,
                                      style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                                    ),
                                  ),
                                for (var i = 0; i < estado.lineas.length; i++)
                                  _FilaLinea(
                                    key: ValueKey(estado.lineas[i].producto.productoId),
                                    linea: estado.lineas[i],
                                    onCambio: (l) => ctrl.editarLinea(i, l),
                                    onQuitar: () => ctrl.quitarLinea(i),
                                  ),
                              ],
                            ),
                          ),
                          const SizedBox(height: 12),
                          _Tarjeta(titulo: 'Pago', hijo: _Pago(estado: estado)),
                          const SizedBox(height: 12),
                          _Totales(estado: estado),
                          const SizedBox(height: 12),
                          if (estado.error != null)
                            AvisoDeError(estado.error!, key: const Key('compra-error')),
                          SizedBox(
                            height: RegentaSpacing.hitTarget,
                            child: FilledButton.icon(
                              key: const Key('compra-registrar'),
                              onPressed: estado.guardando ? null : () => unawaited(_registrar()),
                              style: FilledButton.styleFrom(
                                backgroundColor: RegentaColors.accent,
                                foregroundColor: RegentaColors.surface,
                                shape: RoundedRectangleBorder(
                                  borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                                ),
                              ),
                              icon: const Icon(Icons.inventory_outlined, size: 18),
                              label: Text(
                                estado.guardando
                                    ? 'Registrando…'
                                    : 'Registrar compra por ${formatearPesos(estado.total)}',
                              ),
                            ),
                          ),
                          const SizedBox(height: 24),
                        ],
                      ),
                    ),
                  ),
                ),
        );
      },
    );
  }
}

String _digitos(String texto) => texto.replaceAll(RegExp(r'[^0-9.]'), '');

String _fechaVisible(DateTime d) =>
    '${d.day.toString().padLeft(2, '0')}/${d.month.toString().padLeft(2, '0')}/${d.year}';

class _Tarjeta extends StatelessWidget {
  const _Tarjeta({required this.titulo, required this.hijo});

  final String titulo;
  final Widget hijo;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        border: Border.all(color: RegentaColors.line),
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      // Capa de Material propia: lo que se toca adentro (casillas, listas) muestra su efecto
      // sobre la tarjeta, no debajo de su fondo.
      child: Material(
        type: MaterialType.transparency,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(titulo.toUpperCase(), style: RegentaType.etiqueta.copyWith(fontSize: 10)),
            const SizedBox(height: 10),
            hijo,
          ],
        ),
      ),
    );
  }
}

/// Campos que se acomodan en filas en web y uno por renglón en el celular.
class _Celdas extends StatelessWidget {
  const _Celdas({required this.hijos, this.ancho = 220});

  final List<Widget> hijos;
  final double ancho;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, c) {
        final w = c.maxWidth < 520 ? c.maxWidth : ancho;
        return Wrap(
          spacing: 10,
          children: [for (final h in hijos) SizedBox(width: w, child: h)],
        );
      },
    );
  }
}

class _Cabecera extends StatelessWidget {
  const _Cabecera({
    required this.estado,
    required this.factura,
    required this.flete,
    required this.descuento,
    required this.onProveedor,
    required this.onBodega,
    required this.onFactura,
    required this.onFlete,
    required this.onDescuento,
    required this.onFecha,
  });

  final EstadoDeCompraDirecta estado;
  final TextEditingController factura;
  final TextEditingController flete;
  final TextEditingController descuento;
  final ValueChanged<String?> onProveedor;
  final ValueChanged<String?> onBodega;
  final ValueChanged<String> onFactura;
  final ValueChanged<String> onFlete;
  final ValueChanged<String> onDescuento;
  final VoidCallback onFecha;

  @override
  Widget build(BuildContext context) {
    final proveedor = estado.proveedor;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        _Celdas(
          ancho: 300,
          hijos: [
            CampoConEtiqueta(
              etiqueta: 'Proveedor *',
              ayuda: proveedor == null
                  ? null
                  : (proveedor.diasCredito == 0 ? 'De contado' : 'Crédito a ${proveedor.diasCredito} días'),
              hijo: DropdownButtonFormField<String>(
                key: const Key('compra-proveedor'),
                initialValue: estado.proveedorId,
                isExpanded: true,
                decoration: decoracionDeCampo(hint: 'Elige el proveedor'),
                items: [
                  for (final p in estado.proveedores)
                    DropdownMenuItem(
                      value: p.id,
                      child: Text(p.nombre, overflow: TextOverflow.ellipsis),
                    ),
                ],
                onChanged: onProveedor,
              ),
            ),
            CampoConEtiqueta(
              etiqueta: 'Bodega *',
              hijo: DropdownButtonFormField<String>(
                key: const Key('compra-bodega'),
                initialValue: estado.bodegaId,
                isExpanded: true,
                decoration: decoracionDeCampo(),
                items: [
                  for (final b in estado.bodegas)
                    DropdownMenuItem(
                      value: b.id,
                      child: Text(b.nombre, overflow: TextOverflow.ellipsis),
                    ),
                ],
                onChanged: onBodega,
              ),
            ),
          ],
        ),
        _Celdas(
          ancho: 196,
          hijos: [
            CampoConEtiqueta(
              etiqueta: estado.deContado ? 'Nº de factura *' : 'Nº de factura',
              ayuda: estado.deContado ? null : '¿Llegó con remisión? Déjalo vacío',
              hijo: TextField(
                key: const Key('compra-factura'),
                controller: factura,
                onChanged: onFactura,
                style: estiloDeCampo,
                decoration: decoracionDeCampo(hint: 'FV-1234'),
              ),
            ),
            CampoConEtiqueta(
              etiqueta: 'Fecha de la factura',
              hijo: OutlinedButton.icon(
                key: const Key('compra-fecha'),
                onPressed: onFecha,
                style: OutlinedButton.styleFrom(
                  minimumSize: const Size.fromHeight(44),
                  alignment: Alignment.centerLeft,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
                ),
                icon: const Icon(Icons.event, size: 18),
                label: Text(estado.fechaFactura == null ? 'Hoy' : _fechaVisible(estado.fechaFactura!)),
              ),
            ),
            CampoConEtiqueta(
              etiqueta: 'Descuento global',
              ayuda: 'A pie de factura, sin IVA',
              hijo: TextField(
                key: const Key('compra-descuento-global'),
                controller: descuento,
                onChanged: onDescuento,
                keyboardType: TextInputType.number,
                style: estiloDeCampo,
                decoration: decoracionDeCampo(hint: '0'),
              ),
            ),
            CampoConEtiqueta(
              etiqueta: 'Flete',
              hijo: TextField(
                key: const Key('compra-flete'),
                controller: flete,
                onChanged: onFlete,
                keyboardType: TextInputType.number,
                style: estiloDeCampo,
                decoration: decoracionDeCampo(hint: '0'),
              ),
            ),
          ],
        ),
      ],
    );
  }
}

class _Buscador extends StatelessWidget {
  const _Buscador({
    required this.controlador,
    required this.habilitado,
    required this.buscando,
    required this.onBuscar,
  });

  final TextEditingController controlador;
  final bool habilitado;
  final bool buscando;
  final VoidCallback onBuscar;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: TextField(
            key: const Key('compra-buscar'),
            controller: controlador,
            enabled: habilitado,
            onSubmitted: (_) => onBuscar(),
            style: estiloDeCampo,
            decoration: decoracionDeCampo(
              hint: 'Producto: nombre, SKU o código',
              prefijo: const Icon(Icons.search, size: 18, color: RegentaColors.faint),
            ),
          ),
        ),
        const SizedBox(width: 8),
        SizedBox(
          height: RegentaSpacing.hitTarget,
          child: OutlinedButton(
            key: const Key('compra-buscar-boton'),
            onPressed: habilitado && !buscando ? onBuscar : null,
            child: Text(buscando ? 'Buscando…' : 'Buscar'),
          ),
        ),
      ],
    );
  }
}

class _Resultados extends StatelessWidget {
  const _Resultados({required this.estado, required this.onElegir});

  final EstadoDeCompraDirecta estado;
  final ValueChanged<ProductoEncontrado> onElegir;

  @override
  Widget build(BuildContext context) {
    return Container(
      key: const Key('compra-resultados'),
      margin: const EdgeInsets.only(top: 8),
      constraints: const BoxConstraints(maxHeight: 220),
      decoration: BoxDecoration(
        border: Border.all(color: RegentaColors.line2),
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      // Su propia capa de Material: la tarjeta tiene fondo y taparía el efecto al tocar.
      child: Material(
        type: MaterialType.transparency,
        child: ListView(
          shrinkWrap: true,
          children: [
            for (final r in estado.resultados)
              ListTile(
                dense: true,
                title: Text(r.nombre, style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte)),
                subtitle: r.sku == null
                    ? null
                    : Text(r.sku!, style: RegentaType.codigo.copyWith(fontSize: 11)),
                trailing: const Icon(Icons.add_circle_outline, color: RegentaColors.accent),
                onTap: () => onElegir(r),
              ),
          ],
        ),
      ),
    );
  }
}

/// Una línea de la factura. Guarda sus propios campos y avisa cada cambio al controlador.
class _FilaLinea extends StatefulWidget {
  const _FilaLinea({super.key, required this.linea, required this.onCambio, required this.onQuitar});

  final LineaEnEdicion linea;
  final ValueChanged<LineaEnEdicion> onCambio;
  final VoidCallback onQuitar;

  @override
  State<_FilaLinea> createState() => _FilaLineaState();
}

class _FilaLineaState extends State<_FilaLinea> {
  late final TextEditingController _cantidad;
  late final TextEditingController _costo;
  late final TextEditingController _descuento;
  late final TextEditingController _codigo;
  late final TextEditingController _lote;

  @override
  void initState() {
    super.initState();
    final l = widget.linea;
    _cantidad = TextEditingController(text: _texto(l.cantidad));
    _costo = TextEditingController(text: l.costo == null ? '' : _texto(l.costo!));
    _descuento = TextEditingController(text: l.descuentoPct == 0 ? '' : _texto(l.descuentoPct));
    _codigo = TextEditingController(text: l.codigoProveedor ?? '');
    _lote = TextEditingController(text: l.codigoLote ?? '');
  }

  @override
  void dispose() {
    for (final c in [_cantidad, _costo, _descuento, _codigo, _lote]) {
      c.dispose();
    }
    super.dispose();
  }

  static String _texto(num n) => n == n.roundToDouble() ? '${n.round()}' : '$n';

  Future<void> _elegirVencimiento() async {
    final hoy = DateTime.now();
    final elegida = await showDatePicker(
      context: context,
      initialDate: widget.linea.vencimiento ?? hoy.add(const Duration(days: 365)),
      firstDate: hoy,
      lastDate: DateTime(hoy.year + 10),
    );
    if (elegida != null) widget.onCambio(widget.linea.copiar(vencimiento: elegida));
  }

  Widget _campo(
    String clave,
    String etiqueta,
    TextEditingController c,
    ValueChanged<String> alCambiar, {
    bool numero = true,
  }) {
    final id = widget.linea.producto.productoId;
    return CampoConEtiqueta(
      etiqueta: etiqueta,
      hijo: TextField(
        key: Key('linea-$clave-$id'),
        controller: c,
        onChanged: alCambiar,
        keyboardType: numero ? const TextInputType.numberWithOptions(decimal: true) : null,
        style: estiloDeCampo,
        decoration: decoracionDeCampo(),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final l = widget.linea;
    final p = l.producto;
    final iva = p.impuestoPct == 0 ? p.tarifaIva.replaceAll('_', ' ') : 'IVA ${_texto(p.impuestoPct)} %';
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 10),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      p.nombre,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.item.copyWith(
                        color: RegentaColors.inkFuerte,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                    Text(
                      [if (p.sku != null) p.sku!, iva].join(' · '),
                      style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted),
                    ),
                  ],
                ),
              ),
              Text(
                formatearPesos(l.base),
                key: Key('linea-base-${p.productoId}'),
                style: RegentaType.codigo.copyWith(
                  fontSize: 13,
                  fontWeight: FontWeight.w600,
                  color: RegentaColors.ink,
                ),
              ),
              IconButton(
                tooltip: 'Quitar de la factura',
                onPressed: widget.onQuitar,
                icon: const Icon(Icons.delete_outline, size: 18, color: RegentaColors.faint),
              ),
            ],
          ),
          const SizedBox(height: 8),
          _Celdas(
            ancho: 150,
            hijos: [
              _campo(
                'cantidad',
                'Cantidad',
                _cantidad,
                (t) => widget.onCambio(l.copiar(cantidad: num.tryParse(t) ?? 0)),
              ),
              _campo(
                'costo',
                'Costo unitario (sin IVA)',
                _costo,
                (t) => widget.onCambio(l.copiar(costo: num.tryParse(_digitos(t)))),
              ),
              _campo(
                'descuento',
                'Descuento %',
                _descuento,
                (t) => widget.onCambio(l.copiar(descuentoPct: num.tryParse(t) ?? 0)),
              ),
              _campo(
                'codigo',
                'Código proveedor',
                _codigo,
                (t) => widget.onCambio(l.copiar(codigoProveedor: t)),
                numero: false,
              ),
            ],
          ),
          if (p.exigeLote)
            _Celdas(
              ancho: 230,
              hijos: [
                _campo(
                  'lote',
                  'Lote *',
                  _lote,
                  (t) => widget.onCambio(l.copiar(codigoLote: t)),
                  numero: false,
                ),
                CampoConEtiqueta(
                  etiqueta: p.perecedero ? 'Vencimiento *' : 'Vencimiento',
                  hijo: OutlinedButton.icon(
                    key: Key('linea-vencimiento-${p.productoId}'),
                    onPressed: () => unawaited(_elegirVencimiento()),
                    style: OutlinedButton.styleFrom(
                      minimumSize: const Size.fromHeight(44),
                      alignment: Alignment.centerLeft,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                      ),
                    ),
                    icon: const Icon(Icons.event, size: 18),
                    label: Text(l.vencimiento == null ? 'Elegir fecha' : _fechaVisible(l.vencimiento!)),
                  ),
                ),
              ],
            ),
        ],
      ),
    );
  }
}

class _Totales extends StatelessWidget {
  const _Totales({required this.estado});

  final EstadoDeCompraDirecta estado;

  @override
  Widget build(BuildContext context) {
    Widget fila(String etiqueta, num valor, String clave, {bool fuerte = false}) => Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        children: [
          Expanded(
            child: Text(
              etiqueta,
              style: RegentaType.cuerpo.copyWith(
                fontSize: fuerte ? 15 : 13,
                fontWeight: fuerte ? FontWeight.w800 : FontWeight.w500,
                color: RegentaColors.ink,
              ),
            ),
          ),
          Text(
            formatearPesos(valor),
            key: Key('compra-$clave'),
            style: RegentaType.codigo.copyWith(
              fontSize: fuerte ? 16 : 13,
              fontWeight: fuerte ? FontWeight.w800 : FontWeight.w600,
              color: RegentaColors.inkFuerte,
            ),
          ),
        ],
      ),
    );
    return _Tarjeta(
      titulo: 'Totales',
      hijo: Column(
        children: [
          fila('Base (sin IVA)', estado.base, 'base'),
          if (estado.descuentoGlobal > 0) fila('Descuento global', -estado.descuentoGlobal, 'descuento'),
          fila('IVA', estado.iva, 'iva'),
          fila('Flete', estado.flete, 'flete'),
          const Divider(height: 14, color: RegentaColors.line),
          fila('Total de la factura', estado.total, 'total', fuerte: true),
        ],
      ),
    );
  }
}

/// HU-169 · Crédito o contado. De contado: con qué se pagó y, si es efectivo, si sale de la caja
/// abierta del usuario (HU-145 C8).
class _Pago extends ConsumerWidget {
  const _Pago({required this.estado});

  final EstadoDeCompraDirecta estado;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ctrl = ref.read(controladorDeCompraDirectaProvider.notifier);
    final proveedor = estado.proveedor;
    final plazo = proveedor == null || proveedor.diasCredito == 0
        ? 'Vence el mismo día de la factura'
        : 'Vence a ${proveedor.diasCredito} días de la factura';
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      SegmentedButton<CondicionDePago>(
        key: const Key('compra-condicion'),
        showSelectedIcon: false,
        segments: const [
          ButtonSegment(value: CondicionDePago.credito, label: Text('A crédito'), icon: Icon(Icons.schedule)),
          ButtonSegment(value: CondicionDePago.contado, label: Text('De contado'), icon: Icon(Icons.payments_outlined)),
        ],
        selected: {estado.condicion},
        onSelectionChanged: (s) => ctrl.cambiarCondicion(s.first),
      ),
      const SizedBox(height: 10),
      if (!estado.deContado)
        Text('Nace una cuenta por pagar. $plazo.',
            key: const Key('compra-plazo'),
            style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2))
      else ...[
        _Celdas(ancho: 230, hijos: [
          CampoConEtiqueta(
            etiqueta: 'Pagado con',
            hijo: DropdownButtonFormField<MetodoDePagoDeCompra>(
              key: const Key('compra-metodo'),
              initialValue: estado.metodo,
              isExpanded: true,
              decoration: decoracionDeCampo(),
              items: [
                for (final m in MetodoDePagoDeCompra.values) DropdownMenuItem(value: m, child: Text(m.etiqueta)),
              ],
              onChanged: (m) {
                if (m != null) ctrl.cambiarMetodo(m);
              },
            ),
          ),
          if (estado.metodo != MetodoDePagoDeCompra.efectivo)
            CampoConEtiqueta(
              etiqueta: 'Referencia',
              hijo: TextField(
                key: const Key('compra-referencia-pago'),
                onChanged: ctrl.cambiarReferenciaPago,
                style: estiloDeCampo,
                decoration: decoracionDeCampo(hint: 'Nº de transferencia o cheque'),
              ),
            ),
        ]),
        if (estado.puedePagarDesdeLaCaja)
          CheckboxListTile(
            key: const Key('compra-desde-la-caja'),
            contentPadding: EdgeInsets.zero,
            controlAffinity: ListTileControlAffinity.leading,
            value: estado.cajaAbierta && estado.desdeLaCaja,
            onChanged: estado.cajaAbierta ? (v) => ctrl.cambiarDesdeLaCaja(v ?? false) : null,
            title: const Text('Pagar con dinero de la caja'),
            subtitle: Text(
              estado.cajaAbierta
                  ? 'La salida queda registrada sola en tu caja abierta.'
                  : 'No tienes una caja abierta: se registra como pagado con dinero de fuera de la caja.',
              style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted),
            ),
          ),
      ],
    ]);
  }
}
