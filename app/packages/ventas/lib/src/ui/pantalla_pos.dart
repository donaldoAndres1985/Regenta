import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/producto_buscado.dart';
import '../escaner/escaner_de_codigos.dart';
import '../pos/estado_del_pos.dart';
import '../pos/linea_de_carrito.dart';
import '../pos/proveedores.dart';
import 'formato.dart';

/// La pantalla de POS. HU-045.
///
/// Es **un solo widget** que se adapta con `LayoutBuilder` en
/// [kBreakpointEscritorio] (900 px): en móvil el buscador y el escáner quedan
/// arriba, al alcance del pulgar, y el carrito ocupa la pantalla; en escritorio
/// la factura se lleva el panel principal y los resultados de la búsqueda se
/// despliegan encima, sólo mientras se busca (R10, R10b). No hay dos
/// pantallas, hay dos ramas del mismo `build`. Arriba a la derecha, las tres
/// ventas que se pueden armar a la vez (R12).
class PantallaPos extends ConsumerStatefulWidget {
  const PantallaPos({super.key, this.onAgregarCliente, this.onVentaCobrada});

  final VoidCallback? onAgregarCliente;
  final void Function(String numeroVenta)? onVentaCobrada;

  @override
  ConsumerState<PantallaPos> createState() => _PantallaPosState();
}

class _PantallaPosState extends ConsumerState<PantallaPos> {
  final TextEditingController _buscador = TextEditingController();

  @override
  void dispose() {
    _buscador.dispose();
    super.dispose();
  }

  Future<void> _escanear() async {
    final escaner = ref.read(escanerProvider);
    final pos = ref.read(controladorDelPosProvider.notifier);
    if (await escaner.disponible) {
      try {
        if (!mounted) return;
        final codigo = await escaner.escanearUnCodigo(context);
        if (codigo != null && codigo.isNotEmpty) {
          await pos.agregarPorCodigo(codigo);
        }
        return;
      } on EscaneoNoDisponible {
        // sin cámara: cae al ingreso manual
      }
    }
    if (!mounted) return;
    final codigo = await _pedirCodigo();
    if (codigo != null && codigo.isNotEmpty) await pos.agregarPorCodigo(codigo);
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
          decoration: const InputDecoration(hintText: 'Código de barras'),
          onSubmitted: (v) => Navigator.of(context).pop(v.trim()),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(context).pop(),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(c.text.trim()),
            child: const Text('Buscar'),
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
      if (ahora.ventaNumero != null &&
          ahora.ventaNumero != antes?.ventaNumero) {
        widget.onVentaCobrada?.call(ahora.ventaNumero!);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Venta ${ahora.ventaNumero} cobrada')),
        );
      } else if (ahora.ventaEnCola && !(antes?.ventaEnCola ?? false)) {
        // HU-043 criterio 1: sin señal la venta igual se cobró; está guardada
        // en el celular y sube sola. Decirlo importa: el vendedor tiene que
        // saber que no la perdió, pero también que todavía no tiene número.
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text(
              'Venta guardada sin señal: sube sola cuando vuelva la conexión',
            ),
          ),
        );
      } else if (ahora.mensaje != null && ahora.mensaje != antes?.mensaje) {
        ScaffoldMessenger.of(
          context,
        ).showSnackBar(SnackBar(content: Text(ahora.mensaje!)));
      }
    });

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text(
          'Nueva venta',
          style: RegentaType.seccion.copyWith(
            fontSize: 16,
            color: RegentaColors.ink,
          ),
        ),
        actions: [
          for (var v = 0; v < ventasEnParalelo; v++)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: _BotonDeVenta(
                numero: v + 1,
                activa: estado.ventaActiva == v,
                productos: estado.productosEnVenta(v),
                onTap: () {
                  _buscador.clear();
                  pos.cambiarDeVenta(v);
                },
              ),
            ),
          const SizedBox(width: 14),
        ],
      ),
      body: LayoutBuilder(
        builder: (context, restricciones) {
          final buscador = _Buscador(
            controlador: _buscador,
            onCambio: pos.cambiarTermino,
            onEscanear: _escanear,
          );
          final resultados = _Resultados(
            estado: estado,
            onAgregar: pos.agregar,
          );
          final carrito = _Carrito(
            estado: estado,
            onMas: (i) => pos.cambiarCantidad(i, 1),
            onMenos: (i) => pos.cambiarCantidad(i, -1),
            onVaciar: pos.vaciar,
            onAgregarCliente: widget.onAgregarCliente,
          );
          final totales = _Totales(estado: estado, onCobrar: pos.cobrar);

          if (restricciones.maxWidth >= kBreakpointEscritorio) {
            // Escritorio: la factura se lleva el panel principal (R10) y los
            // resultados flotan encima sólo mientras se busca (R10b).
            final buscando =
                estado.buscando ||
                estado.resultados.isNotEmpty ||
                estado.termino.trim().length >= 3;
            return Row(
              key: const Key('pos-escritorio'),
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: Column(
                    children: [
                      buscador,
                      Expanded(
                        child: Stack(
                          children: [
                            Positioned.fill(
                              child: _Factura(
                                estado: estado,
                                onMas: (i) => pos.cambiarCantidad(i, 1),
                                onMenos: (i) => pos.cambiarCantidad(i, -1),
                                onQuitar: pos.quitar,
                              ),
                            ),
                            if (buscando)
                              Positioned(
                                top: 0,
                                left: 14,
                                right: 14,
                                child: ConstrainedBox(
                                  constraints: const BoxConstraints(
                                    maxHeight: 380,
                                  ),
                                  child: Material(
                                    color: RegentaColors.surface,
                                    elevation: 8,
                                    shadowColor: RegentaColors.ink.withValues(
                                      alpha: 0.25,
                                    ),
                                    borderRadius: const BorderRadius.vertical(
                                      bottom: Radius.circular(12),
                                    ),
                                    clipBehavior: Clip.antiAlias,
                                    child: _Resultados(
                                      estado: estado,
                                      onAgregar: pos.agregar,
                                      flotante: true,
                                    ),
                                  ),
                                ),
                              ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                const VerticalDivider(width: 1, color: RegentaColors.line),
                SizedBox(
                  width: 380,
                  child: Column(
                    children: [
                      Expanded(
                        child: _Resumen(
                          estado: estado,
                          onVaciar: pos.vaciar,
                          onAgregarCliente: widget.onAgregarCliente,
                        ),
                      ),
                      totales,
                    ],
                  ),
                ),
              ],
            );
          }
          // Móvil: buscador y escáner arriba, carrito debajo (criterio 1).
          return Column(
            key: const Key('pos-movil'),
            children: [
              buscador,
              Expanded(
                child: estado.resultados.isNotEmpty ? resultados : carrito,
              ),
              totales,
            ],
          );
        },
      ),
    );
  }
}

class _Buscador extends StatelessWidget {
  const _Buscador({
    required this.controlador,
    required this.onCambio,
    required this.onEscanear,
  });

  final TextEditingController controlador;
  final ValueChanged<String> onCambio;
  final Future<void> Function() onEscanear;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        children: [
          Expanded(
            child: SizedBox(
              height: 46,
              child: TextField(
                controller: controlador,
                onChanged: onCambio,
                style: RegentaType.cuerpo.copyWith(
                  fontSize: 14,
                  color: RegentaColors.ink,
                ),
                decoration: InputDecoration(
                  isDense: true,
                  prefixIcon: const Icon(
                    Icons.search,
                    size: 18,
                    color: RegentaColors.faint,
                  ),
                  hintText: 'Buscar o escanear…',
                  hintStyle: RegentaType.cuerpo.copyWith(
                    fontSize: 14,
                    color: RegentaColors.faint,
                  ),
                  contentPadding: const EdgeInsets.symmetric(vertical: 13),
                  border: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                    borderSide: const BorderSide(color: RegentaColors.line2),
                  ),
                  enabledBorder: OutlineInputBorder(
                    borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                    borderSide: const BorderSide(color: RegentaColors.line2),
                  ),
                ),
              ),
            ),
          ),
          const SizedBox(width: 9),
          Semantics(
            button: true,
            label: 'Escanear código',
            child: InkWell(
              onTap: onEscanear,
              borderRadius: BorderRadius.circular(RegentaSpacing.radius),
              child: Container(
                width: 46,
                height: 46,
                decoration: BoxDecoration(
                  color: RegentaColors.accent,
                  borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                ),
                child: const Icon(
                  Icons.qr_code_scanner,
                  size: 23,
                  color: RegentaColors.surface,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Resultados extends StatelessWidget {
  const _Resultados({
    required this.estado,
    required this.onAgregar,
    this.flotante = false,
  });

  final EstadoDelPos estado;
  final void Function(ProductoBuscado) onAgregar;

  /// En escritorio la lista se despliega sobre la factura y mide lo que
  /// necesita, hasta el alto que le deje quien la contiene.
  final bool flotante;

  @override
  Widget build(BuildContext context) {
    if (estado.buscando) {
      return const Padding(
        padding: EdgeInsets.all(20),
        child: Center(child: CircularProgressIndicator()),
      );
    }
    if (estado.termino.trim().length >= 3 && estado.resultados.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(20),
        child: Center(
          child: Text(
            'Sin resultados para «${estado.termino.trim()}».',
            style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted),
          ),
        ),
      );
    }
    return ListView.builder(
      key: const Key('pos-resultados'),
      shrinkWrap: flotante,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      itemCount: estado.resultados.length,
      itemBuilder: (context, i) {
        final p = estado.resultados[i];
        return ListTile(
          contentPadding: EdgeInsets.zero,
          title: Text(
            p.nombre,
            style: RegentaType.item.copyWith(color: RegentaColors.ink),
          ),
          subtitle: Text(
            '${p.sku} · ${formatearPesos(p.precioVenta)}',
            style: RegentaType.codigo.copyWith(
              fontSize: 10.5,
              color: RegentaColors.muted,
            ),
          ),
          trailing: p.sinStock
              ? Text(
                  'Sin stock',
                  style: RegentaType.codigo.copyWith(
                    fontSize: 11,
                    color: RegentaColors.crit,
                  ),
                )
              : const Icon(
                  Icons.add_circle_outline,
                  color: RegentaColors.accent,
                ),
          onTap: () => onAgregar(p),
        );
      },
    );
  }
}

class _Carrito extends StatelessWidget {
  const _Carrito({
    required this.estado,
    required this.onMas,
    required this.onMenos,
    required this.onVaciar,
    this.onAgregarCliente,
  });

  final EstadoDelPos estado;
  final void Function(int) onMas;
  final void Function(int) onMenos;
  final VoidCallback onVaciar;
  final VoidCallback? onAgregarCliente;

  @override
  Widget build(BuildContext context) {
    return ListView(
      key: const Key('pos-carrito'),
      padding: const EdgeInsets.symmetric(horizontal: 14),
      children: [
        const SizedBox(height: 10),
        _FilaCliente(estado: estado, onAgregarCliente: onAgregarCliente),
        const SizedBox(height: 12),
        if (estado.lineas.isEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 40),
            child: Text(
              'Busca o escanea un producto para empezar.',
              textAlign: TextAlign.center,
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted),
            ),
          )
        else ...[
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Text(
                  '${estado.lineas.length} productos · ${estado.unidades} unidades',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.cuerpo.copyWith(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.ink,
                  ),
                ),
              ),
              const SizedBox(width: 8),
              InkWell(
                onTap: onVaciar,
                child: Text(
                  'Vaciar',
                  style: RegentaType.cuerpo.copyWith(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.crit,
                  ),
                ),
              ),
            ],
          ),
          for (var i = 0; i < estado.lineas.length; i++)
            _LineaWidget(
              linea: estado.lineas[i],
              onMas: () => onMas(i),
              onMenos: () => onMenos(i),
            ),
        ],
      ],
    );
  }
}

class _LineaWidget extends StatelessWidget {
  const _LineaWidget({
    required this.linea,
    required this.onMas,
    required this.onMenos,
  });

  final LineaDeCarrito linea;
  final VoidCallback onMas;
  final VoidCallback onMenos;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 12),
      decoration: BoxDecoration(
        color: linea.sinStock ? RegentaColors.critSoft : null,
        border: const Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  linea.producto.nombre,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.item.copyWith(
                    fontSize: 13.5,
                    color: RegentaColors.ink,
                  ),
                ),
                const SizedBox(height: 3),
                Text(
                  linea.sinStock
                      ? '${linea.producto.sku} · sin stock'
                      : '${linea.producto.sku} · ${formatearPesos(linea.producto.precioVenta)}',
                  style: RegentaType.codigo.copyWith(
                    fontSize: 10.5,
                    color: linea.sinStock
                        ? RegentaColors.crit
                        : RegentaColors.muted,
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(width: 9),
          _Stepper(cantidad: linea.cantidad, onMas: onMas, onMenos: onMenos),
          const SizedBox(width: 9),
          SizedBox(
            width: 92,
            child: Text(
              formatearPesos(linea.total),
              textAlign: TextAlign.right,
              style: RegentaType.codigo.copyWith(
                fontSize: 13.5,
                fontWeight: FontWeight.w600,
                color: RegentaColors.ink,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Stepper extends StatelessWidget {
  const _Stepper({
    required this.cantidad,
    required this.onMas,
    required this.onMenos,
  });

  final int cantidad;
  final VoidCallback onMas;
  final VoidCallback onMenos;

  @override
  Widget build(BuildContext context) {
    // Sin alto fijo: los botones de 44 px (RegentaSpacing.hitTarget) marcan la
    // altura. Fijarla aquí les restaría los 2 px del borde y bajarían de 44.
    return Container(
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        border: Border.all(color: RegentaColors.line2),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          _BotonStepper(
            icono: Icons.remove,
            onTap: onMenos,
            semantica: 'Quitar uno',
          ),
          SizedBox(
            width: 34,
            child: Text(
              '$cantidad',
              textAlign: TextAlign.center,
              style: RegentaType.codigo.copyWith(
                fontSize: 13.5,
                fontWeight: FontWeight.w600,
                color: RegentaColors.ink,
              ),
            ),
          ),
          _BotonStepper(
            icono: Icons.add,
            onTap: onMas,
            semantica: 'Agregar uno',
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
        child: SizedBox(
          width: RegentaSpacing.hitTarget,
          height: RegentaSpacing.hitTarget,
          child: Icon(icono, size: 18, color: RegentaColors.ink2),
        ),
      ),
    );
  }
}

class _Totales extends StatelessWidget {
  const _Totales({required this.estado, required this.onCobrar});

  final EstadoDelPos estado;
  final Future<void> Function() onCobrar;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 13, 14, 13),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(top: BorderSide(color: RegentaColors.line)),
      ),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          _Fila('Subtotal', formatearPesos(estado.subtotal)),
          _Fila('IVA', formatearPesos(estado.impuesto)),
          const SizedBox(height: 6),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text(
                'Total',
                style: RegentaType.seccion.copyWith(
                  fontSize: 17,
                  color: RegentaColors.ink,
                ),
              ),
              const SizedBox(width: 8),
              Flexible(
                child: Text(
                  formatearPesos(estado.total),
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  textAlign: TextAlign.right,
                  style: RegentaType.dinero.copyWith(color: RegentaColors.ink),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (estado.hayLineasSinStock)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Text(
                'Hay líneas sin stock',
                style: RegentaType.cuerpo.copyWith(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: RegentaColors.crit,
                ),
              ),
            ),
          // HU-137 criterio 4: se avisa antes de llegar al botón, no después
          // de tocarlo.
          if (estado.faltaIdentificarComprador)
            Padding(
              key: const Key('aviso-identificar-comprador'),
              padding: const EdgeInsets.only(bottom: 8),
              child: Text(
                'Supera ${formatearPesos(estado.montoParaIdentificar!)}: '
                'identifica al comprador para cobrar',
                style: RegentaType.cuerpo.copyWith(
                  fontSize: 12,
                  fontWeight: FontWeight.w600,
                  color: RegentaColors.warn,
                ),
              ),
            ),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: estado.puedeCobrar ? () => onCobrar() : null,
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.accent,
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
              child: Text(
                estado.cobrando ? 'Cobrando…' : 'Cobrar',
                style: RegentaType.item.copyWith(
                  fontSize: 14,
                  color: RegentaColors.surface,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Fila extends StatelessWidget {
  const _Fila(this.etiqueta, this.valor);
  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 2),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Flexible(
            child: Text(
              etiqueta,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: RegentaType.cuerpo.copyWith(
                fontSize: 12.5,
                color: RegentaColors.ink2,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: Text(
              valor,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              textAlign: TextAlign.right,
              style: RegentaType.codigo.copyWith(
                fontSize: 12.5,
                fontWeight: FontWeight.w500,
                color: RegentaColors.ink2,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// R12: el botón de una de las ventas en paralelo. Si la venta lleva
/// productos, lo dice con un globito para que no se olvide a medias.
class _BotonDeVenta extends StatelessWidget {
  const _BotonDeVenta({
    required this.numero,
    required this.activa,
    required this.productos,
    required this.onTap,
  });

  final int numero;
  final bool activa;
  final int productos;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final etiqueta = productos == 0
        ? 'Venta $numero'
        : 'Venta $numero · $productos ${productos == 1 ? 'producto' : 'productos'}';
    return Semantics(
      button: true,
      selected: activa,
      label: etiqueta,
      excludeSemantics: true,
      child: Stack(
        clipBehavior: Clip.none,
        children: [
          Material(
            color: activa ? RegentaColors.accent : RegentaColors.surface,
            shape: RoundedRectangleBorder(
              borderRadius: BorderRadius.circular(10),
              side: BorderSide(
                color: activa ? RegentaColors.accent : RegentaColors.line2,
              ),
            ),
            child: InkWell(
              key: Key('venta-$numero'),
              onTap: onTap,
              borderRadius: BorderRadius.circular(10),
              child: SizedBox(
                width: 40,
                height: 40,
                child: Center(
                  child: Text(
                    '$numero',
                    style: RegentaType.item.copyWith(
                      fontSize: 14,
                      fontWeight: FontWeight.w700,
                      color: activa ? RegentaColors.surface : RegentaColors.ink,
                    ),
                  ),
                ),
              ),
            ),
          ),
          if (productos > 0)
            Positioned(
              top: -5,
              right: -5,
              child: Container(
                constraints: const BoxConstraints(minWidth: 18),
                height: 18,
                padding: const EdgeInsets.symmetric(horizontal: 5),
                decoration: BoxDecoration(
                  color: RegentaColors.warn,
                  borderRadius: BorderRadius.circular(9),
                  border: Border.all(color: RegentaColors.surface, width: 1.5),
                ),
                child: Center(
                  child: Text(
                    '$productos',
                    style: RegentaType.codigo.copyWith(
                      fontSize: 10,
                      fontWeight: FontWeight.w600,
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

/// R10: la factura de escritorio, una tabla que se lleva el panel principal.
class _Factura extends StatelessWidget {
  const _Factura({
    required this.estado,
    required this.onMas,
    required this.onMenos,
    required this.onQuitar,
  });

  final EstadoDelPos estado;
  final void Function(int) onMas;
  final void Function(int) onMenos;
  final void Function(int) onQuitar;

  static const anchoCodigo = 120.0;
  static const anchoCantidad = 140.0;
  static const anchoPrecio = 110.0;
  static const anchoIva = 60.0;
  static const anchoTotal = 120.0;
  static const anchoAccion = 48.0;

  @override
  Widget build(BuildContext context) {
    final cabecera = RegentaType.etiqueta.copyWith(color: RegentaColors.ink2);
    Widget celda(
      String texto,
      double ancho, {
      TextAlign alinear = TextAlign.left,
    }) => SizedBox(
      width: ancho,
      child: Text(texto, textAlign: alinear, style: cabecera),
    );

    return Container(
      key: const Key('pos-carrito'),
      color: RegentaColors.paper,
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
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
            Container(
              color: RegentaColors.sunken,
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 11),
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
                  ? Center(
                      child: Text(
                        'Busca o escanea un producto para empezar.',
                        textAlign: TextAlign.center,
                        style: RegentaType.cuerpo.copyWith(
                          color: RegentaColors.muted,
                        ),
                      ),
                    )
                  : ListView.builder(
                      itemCount: estado.lineas.length,
                      itemBuilder: (context, i) => _FilaDeFactura(
                        linea: estado.lineas[i],
                        onMas: () => onMas(i),
                        onMenos: () => onMenos(i),
                        onQuitar: () => onQuitar(i),
                      ),
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

class _FilaDeFactura extends StatelessWidget {
  const _FilaDeFactura({
    required this.linea,
    required this.onMas,
    required this.onMenos,
    required this.onQuitar,
  });

  final LineaDeCarrito linea;
  final VoidCallback onMas;
  final VoidCallback onMenos;
  final VoidCallback onQuitar;

  @override
  Widget build(BuildContext context) {
    final dinero = RegentaType.codigo.copyWith(
      fontSize: 13,
      color: RegentaColors.ink2,
    );
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 6),
      decoration: BoxDecoration(
        color: linea.sinStock ? RegentaColors.critSoft : null,
        border: const Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        children: [
          SizedBox(
            width: _Factura.anchoCodigo,
            child: Text(
              linea.producto.sku,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: RegentaType.codigo.copyWith(
                fontSize: 12,
                color: RegentaColors.ink2,
              ),
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
                  style: RegentaType.item.copyWith(
                    fontSize: 13.5,
                    color: RegentaColors.ink,
                  ),
                ),
                if (linea.sinStock)
                  Text(
                    'sin stock',
                    style: RegentaType.codigo.copyWith(
                      fontSize: 10.5,
                      color: RegentaColors.crit,
                    ),
                  ),
              ],
            ),
          ),
          SizedBox(
            width: _Factura.anchoCantidad,
            child: Center(
              child: _Stepper(
                cantidad: linea.cantidad,
                onMas: onMas,
                onMenos: onMenos,
              ),
            ),
          ),
          SizedBox(
            width: _Factura.anchoPrecio,
            child: Text(
              formatearPesos(linea.producto.precioVenta),
              textAlign: TextAlign.right,
              style: dinero,
            ),
          ),
          SizedBox(
            width: _Factura.anchoIva,
            child: Text(
              '${linea.producto.impuestoPct}',
              textAlign: TextAlign.right,
              style: dinero,
            ),
          ),
          SizedBox(
            width: _Factura.anchoTotal,
            child: Text(
              formatearPesos(linea.total),
              textAlign: TextAlign.right,
              style: RegentaType.codigo.copyWith(
                fontSize: 13.5,
                fontWeight: FontWeight.w600,
                color: RegentaColors.ink,
              ),
            ),
          ),
          SizedBox(
            width: _Factura.anchoAccion,
            child: IconButton(
              tooltip: 'Quitar línea',
              onPressed: onQuitar,
              icon: const Icon(
                Icons.delete_outline,
                size: 19,
                color: RegentaColors.faint,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

/// El panel derecho de escritorio: quién compra y cuánto lleva la venta.
class _Resumen extends StatelessWidget {
  const _Resumen({
    required this.estado,
    required this.onVaciar,
    this.onAgregarCliente,
  });

  final EstadoDelPos estado;
  final VoidCallback onVaciar;
  final VoidCallback? onAgregarCliente;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.symmetric(horizontal: 14),
      children: [
        const SizedBox(height: 10),
        _FilaCliente(estado: estado, onAgregarCliente: onAgregarCliente),
        const SizedBox(height: 14),
        Row(
          children: [
            Expanded(
              child: Text(
                '${estado.lineas.length} productos · ${estado.unidades} unidades',
                style: RegentaType.cuerpo.copyWith(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: RegentaColors.ink,
                ),
              ),
            ),
            if (estado.lineas.isNotEmpty)
              InkWell(
                onTap: onVaciar,
                child: Text(
                  'Vaciar',
                  style: RegentaType.cuerpo.copyWith(
                    fontSize: 12,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.crit,
                  ),
                ),
              ),
          ],
        ),
      ],
    );
  }
}

class _FilaCliente extends StatelessWidget {
  const _FilaCliente({required this.estado, this.onAgregarCliente});

  final EstadoDelPos estado;
  final VoidCallback? onAgregarCliente;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      button: true,
      label: estado.cliente == null
          ? 'Agregar cliente a la venta'
          : 'Cambiar el cliente de la venta',
      container: true,
      excludeSemantics: true,
      child: InkWell(
        onTap: onAgregarCliente,
        child: Container(
          // HU-113: se toca de pie y con una mano, como todo lo demás.
          constraints: const BoxConstraints(
            minHeight: RegentaSpacing.hitTarget,
          ),
          padding: const EdgeInsets.symmetric(horizontal: 11, vertical: 8),
          decoration: BoxDecoration(
            color: RegentaColors.paper,
            borderRadius: BorderRadius.circular(RegentaSpacing.radius),
            border: Border.all(color: RegentaColors.line2),
          ),
          child: Row(
            children: [
              const Icon(
                Icons.person_outline,
                size: 17,
                color: RegentaColors.faint,
              ),
              const SizedBox(width: 9),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(
                      estado.cliente?.nombre ?? 'Consumidor final',
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.cuerpo.copyWith(
                        fontSize: 13.5,
                        color: RegentaColors.ink2,
                      ),
                    ),
                    if (estado.cliente != null)
                      Text(
                        estado.cliente!.documento,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: RegentaType.codigo.copyWith(
                          fontSize: 10.5,
                          color: RegentaColors.muted,
                        ),
                      ),
                  ],
                ),
              ),
              Text(
                estado.cliente == null ? 'Agregar cliente' : 'Cambiar',
                style: RegentaType.cuerpo.copyWith(
                  fontSize: 12.5,
                  fontWeight: FontWeight.w600,
                  color: RegentaColors.accent,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
