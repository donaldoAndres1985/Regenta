import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/orden_recibible.dart';
import '../compra_directa/estado.dart';
import '../escaner/escaner_de_codigos.dart';
import '../proveedores/ui/piezas.dart';
import '../recepcion/entrada_de_linea.dart';
import '../recepcion/estado_de_recepcion.dart';
import '../recepcion/factura_de_recepcion.dart';
import '../recepcion/precio_actual.dart';
import '../recepcion/proveedores.dart';
import 'formato.dart';

/// La pantalla de recepción de mercancía. HU-051.
///
/// Es **un solo widget** que se adapta con `LayoutBuilder` en
/// [kBreakpointEscritorio] (900 px): en el celular, parado junto a las cajas, se
/// recibe una línea a la vez con campos grandes y teclado numérico (criterios 1
/// y 2) y se salta a la línea del código escaneado (criterio 3); en escritorio
/// se ven todas las líneas de la orden en una tabla. Sin señal, la recepción se
/// guarda y sube después (criterio 4).
class PantallaRecepcion extends ConsumerStatefulWidget {
  const PantallaRecepcion({
    super.key,
    required this.ordenId,
    this.onVolver,
    this.onRegistrada,
  });

  final String ordenId;
  final VoidCallback? onVolver;
  final void Function(String numeroRecepcion)? onRegistrada;

  @override
  ConsumerState<PantallaRecepcion> createState() => _PantallaRecepcionState();
}

class _PantallaRecepcionState extends ConsumerState<PantallaRecepcion> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(controladorDeRecepcionProvider.notifier).cargarOrden(widget.ordenId);
    });
  }

  Future<void> _escanear() async {
    final escaner = ref.read(escanerDeRecepcionProvider);
    final ctrl = ref.read(controladorDeRecepcionProvider.notifier);
    if (await escaner.disponible) {
      try {
        if (!mounted) return;
        final codigo = await escaner.escanearUnCodigo(context);
        if (codigo != null && codigo.isNotEmpty) ctrl.saltarACodigo(codigo);
        return;
      } on EscaneoNoDisponible {
        // sin cámara: ingreso manual
      }
    }
    if (!mounted) return;
    final codigo = await _pedirCodigo();
    if (codigo != null && codigo.isNotEmpty) ctrl.saltarACodigo(codigo);
  }

  Future<String?> _pedirCodigo() {
    final c = TextEditingController();
    return showDialog<String>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text('Ingresar código', style: RegentaType.seccion),
        content: TextField(
          controller: c,
          autofocus: true,
          decoration: const InputDecoration(hintText: 'Código del producto'),
          onSubmitted: (v) => Navigator.of(context).pop(v.trim()),
        ),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(context).pop(),
              child: const Text('Cancelar')),
          FilledButton(
              onPressed: () => Navigator.of(context).pop(c.text.trim()),
              child: const Text('Ir')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeRecepcionProvider);
    final ctrl = ref.read(controladorDeRecepcionProvider.notifier);

    ref.listen<EstadoDeRecepcion>(controladorDeRecepcionProvider, (antes, ahora) {
      if (ahora.numeroRecepcion != null &&
          ahora.numeroRecepcion != antes?.numeroRecepcion) {
        widget.onRegistrada?.call(ahora.numeroRecepcion!);
      }
      if (ahora.mensaje != null && ahora.mensaje != antes?.mensaje) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(ahora.mensaje!)));
      }
    });

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        leading: widget.onVolver == null
            ? null
            : IconButton(
                icon: const Icon(Icons.arrow_back, color: RegentaColors.ink),
                onPressed: widget.onVolver,
              ),
        title: Text(
          estado.orden == null ? 'Recepción' : 'Recepción · ${estado.orden!.numero}',
          style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink),
        ),
        actions: [
          Semantics(
            button: true,
            label: 'Escanear código',
            child: InkWell(
              onTap: estado.orden == null ? null : _escanear,
              child: const SizedBox(
                width: RegentaSpacing.hitTarget,
                height: RegentaSpacing.hitTarget,
                child: Icon(Icons.qr_code_scanner, size: 22, color: RegentaColors.ink2),
              ),
            ),
          ),
          const SizedBox(width: 6),
        ],
      ),
      body: estado.cargando
          ? const Center(child: CircularProgressIndicator())
          : estado.orden == null
              ? Center(
                  child: Text(estado.mensaje ?? 'No se pudo cargar la orden',
                      style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)))
              : LayoutBuilder(
                  builder: (context, restricciones) {
                    if (restricciones.maxWidth >= kBreakpointEscritorio) {
                      return _Tabla(
                          estado: estado, ctrl: ctrl, puedeCambiarPrecios: ref.watch(puedeCambiarPreciosProvider));
                    }
                    return _UnaLinea(estado: estado, ctrl: ctrl);
                  },
                ),
    );
  }
}

/// Móvil: una línea a la vez, campos grandes, teclado numérico.
class _UnaLinea extends StatelessWidget {
  const _UnaLinea({required this.estado, required this.ctrl});

  final EstadoDeRecepcion estado;
  final dynamic ctrl;

  @override
  Widget build(BuildContext context) {
    final linea = estado.lineaActual;
    return Column(
      key: const Key('recepcion-movil'),
      children: [
        _Progreso(estado: estado, ctrl: ctrl),
        Expanded(
          child: linea == null
              ? const SizedBox.shrink()
              : SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
                  child: _CapturaDeLinea(
                    linea: linea,
                    entrada: estado.entradaDe(linea.id),
                    ctrl: ctrl,
                  ),
                ),
        ),
        _BarraConfirmar(estado: estado, ctrl: ctrl),
      ],
    );
  }
}

class _Progreso extends StatelessWidget {
  const _Progreso({required this.estado, required this.ctrl});

  final EstadoDeRecepcion estado;
  final dynamic ctrl;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 10, 6, 10),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Text(
              'Línea ${estado.indiceActual + 1} de ${estado.lineas.length}',
              style: RegentaType.codigo
                  .copyWith(fontSize: 11, color: RegentaColors.muted),
            ),
          ),
          Semantics(
            button: true,
            label: 'Línea anterior',
            child: InkWell(
              onTap: estado.indiceActual > 0 ? () => ctrl.anterior() : null,
              child: SizedBox(
                width: RegentaSpacing.hitTarget,
                height: RegentaSpacing.hitTarget,
                child: Icon(Icons.chevron_left,
                    color: estado.indiceActual > 0
                        ? RegentaColors.ink2
                        : RegentaColors.faint),
              ),
            ),
          ),
          Semantics(
            button: true,
            label: 'Línea siguiente',
            child: InkWell(
              onTap: estado.indiceActual < estado.lineas.length - 1
                  ? () => ctrl.siguiente()
                  : null,
              child: SizedBox(
                width: RegentaSpacing.hitTarget,
                height: RegentaSpacing.hitTarget,
                child: Icon(Icons.chevron_right,
                    color: estado.indiceActual < estado.lineas.length - 1
                        ? RegentaColors.ink2
                        : RegentaColors.faint),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _CapturaDeLinea extends StatelessWidget {
  const _CapturaDeLinea({
    required this.linea,
    required this.entrada,
    required this.ctrl,
  });

  final LineaRecibible linea;
  final EntradaDeLinea entrada;
  final dynamic ctrl;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(RegentaSpacing.radiusCard),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(linea.nombre,
                    style: RegentaType.item.copyWith(color: RegentaColors.ink)),
              ),
              const SizedBox(width: 10),
              Text('${_entero(linea.cantidadPedida)} ped.',
                  style: RegentaType.codigo
                      .copyWith(fontSize: 11, color: RegentaColors.muted)),
            ],
          ),
          if ((linea.codigo ?? '').isNotEmpty) ...[
            const SizedBox(height: 3),
            Text(linea.codigo!,
                style: RegentaType.codigo
                    .copyWith(fontSize: 10.5, color: RegentaColors.muted)),
          ],
          const SizedBox(height: 12),
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              SizedBox(
                width: 110,
                child: _Campo(
                  etiqueta: 'Recibido',
                  child: SizedBox(
                    height: 48,
                    child: TextField(
                      key: const Key('campo-recibido'),
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      style: RegentaType.codigo
                          .copyWith(fontSize: 15, color: RegentaColors.ink),
                      decoration: _decoracion(hint: linea.faltante.toString()),
                      onChanged: (v) =>
                          ctrl.fijarRecibido(linea.id, num.tryParse(v.trim())),
                    ),
                  ),
                ),
              ),
              const SizedBox(width: 9),
              Expanded(
                child: _Campo(
                  etiqueta: 'Costo',
                  child: _SoloLectura(
                    texto: formatearPesos(entrada.costo ?? linea.costoUnitario),
                  ),
                ),
              ),
            ],
          ),
          if (linea.exigeLote) ...[
            const SizedBox(height: 9),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Expanded(
                  child: _Campo(
                    etiqueta: 'Lote *',
                    child: SizedBox(
                      height: 48,
                      child: TextField(
                        key: const Key('campo-lote'),
                        style: RegentaType.codigo
                            .copyWith(fontSize: 15, color: RegentaColors.ink),
                        decoration: _decoracion(hint: 'L-0000'),
                        onChanged: (v) => ctrl.fijarLote(linea.id, v),
                      ),
                    ),
                  ),
                ),
                const SizedBox(width: 9),
                Expanded(
                  child: _Campo(
                    etiqueta: 'Vence *',
                    child: _CampoFecha(
                      valor: entrada.vencimiento,
                      onElegir: (d) => ctrl.fijarVencimiento(linea.id, d),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Container(
              padding: const EdgeInsets.all(11),
              decoration: BoxDecoration(
                color: RegentaColors.accentSoft,
                borderRadius: BorderRadius.circular(RegentaSpacing.radiusCard),
              ),
              child: Text(
                'El lote se captura aquí, no en el producto: el mismo entra con '
                'lotes distintos cada semana.',
                style: RegentaType.cuerpo
                    .copyWith(fontSize: 11.5, color: RegentaColors.ink2),
              ),
            ),
          ],
          if (entrada.problema(linea) != null) ...[
            const SizedBox(height: 10),
            Text(entrada.problema(linea)!,
                style: RegentaType.cuerpo.copyWith(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.crit)),
          ],
        ],
      ),
    );
  }
}

class _Tabla extends StatelessWidget {
  const _Tabla({required this.estado, required this.ctrl, this.puedeCambiarPrecios = false});

  final EstadoDeRecepcion estado;
  final dynamic ctrl;
  final bool puedeCambiarPrecios;

  @override
  Widget build(BuildContext context) {
    return Column(
      key: const Key('recepcion-escritorio'),
      children: [
        Expanded(
          child: ListView(
            padding: const EdgeInsets.all(18),
            children: [
              // HU-175 C4 · En la web, la factura con que llega: como en la compra directa.
              _PanelDeFactura(factura: estado.factura, alCambiar: (f) => ctrl.fijarFactura(f)),
              const SizedBox(height: 16),
              for (final linea in estado.lineas)
                Padding(
                  padding: const EdgeInsets.only(bottom: 14),
                  child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                    _CapturaDeLinea(
                      linea: linea,
                      entrada: estado.entradaDe(linea.id),
                      ctrl: ctrl,
                    ),
                    // HU-179 · cómo cambia el margen y, con permiso, el precio nuevo.
                    if (estado.precios[linea.productoId] != null)
                      _MargenDeLinea(
                        linea: linea,
                        estado: estado,
                        puedeCambiarPrecio: puedeCambiarPrecios,
                        alCambiarPrecio: (v) => ctrl.fijarPrecioNuevo(linea.productoId, v),
                      ),
                  ]),
                ),
              _ResumenDeFactura(estado: estado),
            ],
          ),
        ),
        _BarraConfirmar(estado: estado, ctrl: ctrl),
      ],
    );
  }
}

class _BarraConfirmar extends StatelessWidget {
  const _BarraConfirmar({required this.estado, required this.ctrl});

  final EstadoDeRecepcion estado;
  final dynamic ctrl;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 13, 14, 13),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(top: BorderSide(color: RegentaColors.line)),
      ),
      child: SizedBox(
        width: double.infinity,
        height: 52,
        child: FilledButton(
          onPressed: estado.puedeConfirmar ? () => ctrl.confirmar() : null,
          style: FilledButton.styleFrom(
            backgroundColor: RegentaColors.accent,
            shape:
                RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Flexible(
                child: Text(
                  estado.guardando
                      ? 'Guardando…'
                      : estado.encolada
                          ? 'Guardada sin señal'
                          : 'Confirmar recepción',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.item
                      .copyWith(fontSize: 14, color: RegentaColors.surface),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Campo extends StatelessWidget {
  const _Campo({required this.etiqueta, required this.child});

  final String etiqueta;
  final Widget child;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(etiqueta.toUpperCase(), style: RegentaType.etiqueta),
        const SizedBox(height: 5),
        child,
      ],
    );
  }
}

class _SoloLectura extends StatelessWidget {
  const _SoloLectura({required this.texto});
  final String texto;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 48,
      alignment: Alignment.centerLeft,
      padding: const EdgeInsets.symmetric(horizontal: 11),
      decoration: BoxDecoration(
        color: RegentaColors.sunken,
        borderRadius: BorderRadius.circular(4),
        border: Border.all(color: RegentaColors.line2),
      ),
      child: Text(texto,
          style: RegentaType.codigo.copyWith(fontSize: 14, color: RegentaColors.ink2)),
    );
  }
}

class _CampoFecha extends StatelessWidget {
  const _CampoFecha({required this.valor, required this.onElegir});

  final DateTime? valor;
  final ValueChanged<DateTime?> onElegir;

  @override
  Widget build(BuildContext context) {
    final texto = valor == null
        ? 'dd/mm/aaaa'
        : '${valor!.day.toString().padLeft(2, '0')}/'
            '${valor!.month.toString().padLeft(2, '0')}/${valor!.year}';
    return Semantics(
      button: true,
      label: 'Elegir vencimiento',
      child: InkWell(
        key: const Key('campo-vence'),
        onTap: () async {
          final hoy = DateTime.now();
          final elegida = await showDatePicker(
            context: context,
            initialDate: valor ?? hoy,
            firstDate: hoy.subtract(const Duration(days: 1)),
            lastDate: DateTime(hoy.year + 10),
          );
          if (elegida != null) onElegir(elegida);
        },
        child: Container(
          height: 48,
          alignment: Alignment.centerLeft,
          padding: const EdgeInsets.symmetric(horizontal: 11),
          decoration: BoxDecoration(
            color: RegentaColors.surface,
            borderRadius: BorderRadius.circular(4),
            border: Border.all(color: RegentaColors.line2),
          ),
          child: Text(texto,
              style: RegentaType.codigo.copyWith(
                  fontSize: 14,
                  color: valor == null ? RegentaColors.faint : RegentaColors.ink)),
        ),
      ),
    );
  }
}

String _entero(num n) => n == n.roundToDouble() ? n.round().toString() : n.toString();

InputDecoration _decoracion({required String hint}) => InputDecoration(
      isDense: true,
      hintText: hint,
      hintStyle: RegentaType.codigo
          .copyWith(fontSize: 14, color: RegentaColors.faint),
      contentPadding: const EdgeInsets.symmetric(horizontal: 11, vertical: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(4),
        borderSide: const BorderSide(color: RegentaColors.line2),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(4),
        borderSide: const BorderSide(color: RegentaColors.line2),
      ),
    );

/// HU-175 C4 · Número, fecha, flete y descuento de la factura, y si se paga de contado o a crédito.
class _PanelDeFactura extends StatefulWidget {
  const _PanelDeFactura({required this.factura, required this.alCambiar});

  final FacturaDeRecepcion factura;
  final ValueChanged<FacturaDeRecepcion> alCambiar;

  @override
  State<_PanelDeFactura> createState() => _PanelDeFacturaState();
}

class _PanelDeFacturaState extends State<_PanelDeFactura> {
  late final TextEditingController _numero = TextEditingController(text: widget.factura.numero);
  late final TextEditingController _flete =
      TextEditingController(text: widget.factura.flete == 0 ? '' : '${widget.factura.flete}');
  late final TextEditingController _descuento = TextEditingController(
      text: widget.factura.descuentoGlobal == 0 ? '' : '${widget.factura.descuentoGlobal}');

  @override
  void dispose() {
    _numero.dispose();
    _flete.dispose();
    _descuento.dispose();
    super.dispose();
  }

  static num _valor(String texto) => num.tryParse(texto.replaceAll(RegExp(r'[^0-9.]'), '')) ?? 0;

  Future<void> _elegirFecha() async {
    final hoy = DateTime.now();
    final f = await showDatePicker(
      context: context,
      initialDate: widget.factura.fecha ?? hoy,
      firstDate: DateTime(hoy.year - 1),
      lastDate: hoy,
    );
    if (f != null) widget.alCambiar(widget.factura.copiar(fecha: f));
  }

  @override
  Widget build(BuildContext context) {
    final f = widget.factura;
    Widget celda(double ancho, Widget hijo) => SizedBox(width: ancho, child: hijo);
    return Container(
      key: const Key('recepcion-factura'),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        border: Border.all(color: RegentaColors.line),
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      child: Material(
        type: MaterialType.transparency,
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          Text('Factura del proveedor', style: RegentaType.item.copyWith(fontWeight: FontWeight.w700)),
          const SizedBox(height: 8),
          Wrap(spacing: 12, runSpacing: 4, children: [
            celda(
                220,
                CampoConEtiqueta(
                  etiqueta: f.deContado ? 'N.º factura *' : 'N.º factura (vacío si llegó con remisión)',
                  hijo: TextField(
                    key: const Key('recepcion-numero-factura'),
                    controller: _numero,
                    onChanged: (v) => widget.alCambiar(f.copiar(numero: v)),
                    style: estiloDeCampo,
                    decoration: decoracionDeCampo(hint: 'FE-88213'),
                  ),
                )),
            celda(
                160,
                CampoConEtiqueta(
                  etiqueta: 'Fecha',
                  hijo: OutlinedButton.icon(
                    key: const Key('recepcion-fecha-factura'),
                    onPressed: _elegirFecha,
                    style: OutlinedButton.styleFrom(
                        minimumSize: const Size.fromHeight(44), alignment: Alignment.centerLeft),
                    icon: const Icon(Icons.event, size: 18),
                    label: Text(f.fecha == null ? 'Hoy' : f.fecha!.toIso8601String().substring(0, 10)),
                  ),
                )),
            celda(
                140,
                CampoConEtiqueta(
                  etiqueta: 'Flete',
                  hijo: TextField(
                    key: const Key('recepcion-flete'),
                    controller: _flete,
                    keyboardType: TextInputType.number,
                    onChanged: (v) => widget.alCambiar(f.copiar(flete: _valor(v))),
                    style: estiloDeCampo,
                    decoration: decoracionDeCampo(hint: '0'),
                  ),
                )),
            celda(
                160,
                CampoConEtiqueta(
                  etiqueta: 'Descuento global',
                  hijo: TextField(
                    key: const Key('recepcion-descuento-global'),
                    controller: _descuento,
                    keyboardType: TextInputType.number,
                    onChanged: (v) => widget.alCambiar(f.copiar(descuentoGlobal: _valor(v))),
                    style: estiloDeCampo,
                    decoration: decoracionDeCampo(hint: '0'),
                  ),
                )),
          ]),
          const SizedBox(height: 8),
          Align(
            alignment: Alignment.centerLeft,
            child: SegmentedButton<CondicionDePago>(
              key: const Key('recepcion-condicion'),
              showSelectedIcon: false,
              segments: const [
                ButtonSegment(value: CondicionDePago.credito, label: Text('A crédito'), icon: Icon(Icons.schedule)),
                ButtonSegment(
                    value: CondicionDePago.contado, label: Text('De contado'), icon: Icon(Icons.payments_outlined)),
              ],
              selected: {f.condicion},
              onSelectionChanged: (s) => widget.alCambiar(f.copiar(condicion: s.first)),
            ),
          ),
          if (f.deContado) ...[
            const SizedBox(height: 8),
            Wrap(spacing: 12, crossAxisAlignment: WrapCrossAlignment.end, children: [
              celda(
                  200,
                  CampoConEtiqueta(
                    etiqueta: 'Pagado con',
                    hijo: DropdownButtonFormField<MetodoDePagoDeCompra>(
                      key: const Key('recepcion-metodo'),
                      initialValue: f.metodo,
                      isExpanded: true,
                      decoration: decoracionDeCampo(),
                      items: [
                        for (final m in MetodoDePagoDeCompra.values)
                          DropdownMenuItem(value: m, child: Text(m.etiqueta)),
                      ],
                      onChanged: (m) {
                        if (m != null) widget.alCambiar(f.copiar(metodo: m));
                      },
                    ),
                  )),
              if (f.metodo != MetodoDePagoDeCompra.efectivo)
                celda(
                    220,
                    CampoConEtiqueta(
                      etiqueta: 'Referencia',
                      hijo: TextField(
                        key: const Key('recepcion-referencia-pago'),
                        onChanged: (v) => widget.alCambiar(f.copiar(referencia: v)),
                        style: estiloDeCampo,
                        decoration: decoracionDeCampo(hint: 'N.º de transferencia o cheque'),
                      ),
                    ))
              else
                celda(
                    300,
                    CheckboxListTile(
                      key: const Key('recepcion-desde-la-caja'),
                      contentPadding: EdgeInsets.zero,
                      controlAffinity: ListTileControlAffinity.leading,
                      value: f.desdeLaCaja,
                      onChanged: (v) => widget.alCambiar(f.copiar(desdeLaCaja: v ?? false)),
                      title: const Text('Pagar con dinero de la caja'),
                    )),
            ]),
          ] else
            Padding(
              padding: const EdgeInsets.only(top: 6),
              child: Text('Nace una cuenta por pagar con el plazo del proveedor.',
                  style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2)),
            ),
          if (f.problema != null)
            Padding(
              padding: const EdgeInsets.only(top: 8),
              child: Text(f.problema!,
                  key: const Key('recepcion-factura-problema'),
                  style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.warn)),
            ),
        ]),
      ),
    );
  }
}

/// HU-175 · Lo que suma lo recibido, con el descuento global repartido, el IVA y el flete.
class _ResumenDeFactura extends StatelessWidget {
  const _ResumenDeFactura({required this.estado});

  final EstadoDeRecepcion estado;

  @override
  Widget build(BuildContext context) {
    num base = 0;
    num iva = 0;
    for (final l in estado.lineasConEntrada) {
      final e = estado.entradaDe(l.id);
      final b = e.recibido! * (e.costo ?? l.costoUnitario) * (1 - l.descuentoPct / 100);
      base += b;
      iva += b * l.impuestoPct / 100;
    }
    final f = estado.factura;
    final proporcion = base == 0 ? 0 : (base - f.descuentoGlobal) / base;
    final baseNeta = base - f.descuentoGlobal;
    final ivaNeto = iva * proporcion;
    Widget fila(String etiqueta, num valor, String clave, {bool fuerte = false}) => Padding(
          padding: const EdgeInsets.symmetric(vertical: 2),
          child: Row(children: [
            Expanded(
                child: Text(etiqueta,
                    style: RegentaType.cuerpo.copyWith(fontWeight: fuerte ? FontWeight.w800 : FontWeight.w500))),
            Text(formatearPesos(valor),
                key: Key('recepcion-$clave'),
                style: RegentaType.codigo.copyWith(fontWeight: fuerte ? FontWeight.w800 : FontWeight.w600)),
          ]),
        );
    return Align(
      alignment: Alignment.centerRight,
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 340),
        child: Column(children: [
          fila('Subtotal', base, 'subtotal'),
          if (f.descuentoGlobal > 0) fila('Descuento global', -f.descuentoGlobal, 'descuento'),
          fila('IVA', ivaNeto, 'iva'),
          if (f.flete > 0) fila('Flete', f.flete, 'flete-total'),
          fila('Total', baseNeta + ivaNeto + f.flete, 'total', fuerte: true),
        ]),
      ),
    );
  }
}

/// HU-179 C1 / I20 · El costo anterior (promedio) y el nuevo de esta compra, y el margen sobre la base sin
/// IVA antes y después. El costo nuevo es una estimación de la pantalla con el descuento global y el flete
/// repartidos, como lo hace el backend al confirmar. Con `INVENTARIO_PRODUCTO_EDITAR` deja escribir el
/// precio nuevo con IVA incluido (C2); sin él, solo se ve el margen (C4).
class _MargenDeLinea extends StatefulWidget {
  const _MargenDeLinea({
    required this.linea,
    required this.estado,
    required this.puedeCambiarPrecio,
    required this.alCambiarPrecio,
  });

  final LineaRecibible linea;
  final EstadoDeRecepcion estado;
  final bool puedeCambiarPrecio;
  final ValueChanged<num?> alCambiarPrecio;

  @override
  State<_MargenDeLinea> createState() => _MargenDeLineaState();
}

class _MargenDeLineaState extends State<_MargenDeLinea> {
  late final TextEditingController _precio = TextEditingController(
      text: widget.estado.preciosNuevos[widget.linea.productoId]?.round().toString() ?? '');

  @override
  void dispose() {
    _precio.dispose();
    super.dispose();
  }

  num _netoUnitario(LineaRecibible l) {
    final e = widget.estado.entradaDe(l.id);
    return (e.costo ?? l.costoUnitario) * (1 - l.descuentoPct / 100);
  }

  /// El costo de entrada que va a quedar, repartiendo el descuento global y el flete por valor.
  num get _costoNuevo {
    final estado = widget.estado;
    num base = 0;
    for (final l in estado.lineasConEntrada) {
      base += estado.entradaDe(l.id).recibido! * _netoUnitario(l);
    }
    final neto = _netoUnitario(widget.linea);
    if (base <= 0) return neto;
    final f = estado.factura;
    final conDescuento = neto * (base - f.descuentoGlobal) / base;
    return conDescuento + f.flete * neto / base;
  }

  @override
  Widget build(BuildContext context) {
    final actual = widget.estado.precios[widget.linea.productoId]!;
    final nuevo = _costoNuevo;
    final antes = margenSobreBase(actual.base, actual.costoPromedio);
    final despues = margenSobreBase(actual.base, nuevo);
    final baja = antes != null && despues != null && despues < antes;
    final id = widget.linea.productoId;
    return Container(
      key: Key('margen-$id'),
      margin: const EdgeInsets.only(top: 6),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: baja ? RegentaColors.warnSoft : RegentaColors.sunken,
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      child: Wrap(spacing: 16, runSpacing: 8, crossAxisAlignment: WrapCrossAlignment.center, children: [
        Text('Costo ${formatearPesos(actual.costoPromedio)} → ${formatearPesos(nuevo)}',
            key: Key('margen-costo-$id'), style: RegentaType.codigo.copyWith(fontSize: 12)),
        Text('Margen ${formatearMargen(antes)} → ${formatearMargen(despues)}',
            key: Key('margen-valor-$id'),
            style: RegentaType.codigo.copyWith(
                fontSize: 12, fontWeight: FontWeight.w700, color: baja ? RegentaColors.warn : RegentaColors.ink)),
        Text('Precio ${formatearPesos(actual.precio)} (base ${formatearPesos(actual.base)})',
            style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
        if (widget.puedeCambiarPrecio)
          SizedBox(
            width: 190,
            child: TextField(
              key: Key('precio-nuevo-$id'),
              controller: _precio,
              keyboardType: TextInputType.number,
              onChanged: (v) => widget.alCambiarPrecio(num.tryParse(v.replaceAll(RegExp(r'[^0-9.]'), ''))),
              style: RegentaType.codigo.copyWith(fontSize: 13),
              decoration: const InputDecoration(
                isDense: true,
                labelText: 'Precio nuevo (IVA incl.)',
                border: OutlineInputBorder(),
              ),
            ),
          ),
      ]),
    );
  }
}
