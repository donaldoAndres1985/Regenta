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
/// la rejilla de productos y el carrito se ven a la vez, en dos paneles. No hay
/// dos pantallas, hay dos ramas del mismo `build`.
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
        if (codigo != null && codigo.isNotEmpty) await pos.agregarPorCodigo(codigo);
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
              onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar')),
          FilledButton(
              onPressed: () => Navigator.of(context).pop(c.text.trim()),
              child: const Text('Buscar')),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDelPosProvider);
    final pos = ref.read(controladorDelPosProvider.notifier);

    ref.listen<EstadoDelPos>(controladorDelPosProvider, (antes, ahora) {
      if (ahora.ventaNumero != null && ahora.ventaNumero != antes?.ventaNumero) {
        widget.onVentaCobrada?.call(ahora.ventaNumero!);
        ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Venta ${ahora.ventaNumero} cobrada')));
      } else if (ahora.mensaje != null && ahora.mensaje != antes?.mensaje) {
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
        title: Text('Nueva venta',
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
      ),
      body: LayoutBuilder(
        builder: (context, restricciones) {
          final buscador = _Buscador(
            controlador: _buscador,
            onCambio: pos.cambiarTermino,
            onEscanear: _escanear,
          );
          final resultados = _Resultados(estado: estado, onAgregar: pos.agregar);
          final carrito = _Carrito(
            estado: estado,
            onMas: (i) => pos.cambiarCantidad(i, 1),
            onMenos: (i) => pos.cambiarCantidad(i, -1),
            onVaciar: pos.vaciar,
            onAgregarCliente: widget.onAgregarCliente,
          );
          final totales = _Totales(estado: estado, onCobrar: pos.cobrar);

          if (restricciones.maxWidth >= kBreakpointEscritorio) {
            // Escritorio: rejilla de productos y carrito a la vez (criterio 2).
            return Row(
              key: const Key('pos-escritorio'),
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: Column(children: [buscador, Expanded(child: resultados)]),
                ),
                const VerticalDivider(width: 1, color: RegentaColors.line),
                SizedBox(
                  width: 380,
                  child: Column(children: [Expanded(child: carrito), totales]),
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
                style: RegentaType.cuerpo.copyWith(fontSize: 14, color: RegentaColors.ink),
                decoration: InputDecoration(
                  isDense: true,
                  prefixIcon:
                      const Icon(Icons.search, size: 18, color: RegentaColors.faint),
                  hintText: 'Buscar o escanear…',
                  hintStyle: RegentaType.cuerpo
                      .copyWith(fontSize: 14, color: RegentaColors.faint),
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
                child: const Icon(Icons.qr_code_scanner,
                    size: 23, color: RegentaColors.surface),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Resultados extends StatelessWidget {
  const _Resultados({required this.estado, required this.onAgregar});

  final EstadoDelPos estado;
  final void Function(ProductoBuscado) onAgregar;

  @override
  Widget build(BuildContext context) {
    if (estado.buscando) {
      return const Center(child: CircularProgressIndicator());
    }
    if (estado.termino.trim().length >= 3 && estado.resultados.isEmpty) {
      return Center(
        child: Text('Sin resultados para «${estado.termino.trim()}».',
            style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
      );
    }
    return ListView.builder(
      key: const Key('pos-resultados'),
      padding: const EdgeInsets.symmetric(horizontal: 14),
      itemCount: estado.resultados.length,
      itemBuilder: (context, i) {
        final p = estado.resultados[i];
        return ListTile(
          contentPadding: EdgeInsets.zero,
          title: Text(p.nombre, style: RegentaType.item.copyWith(color: RegentaColors.ink)),
          subtitle: Text('${p.sku} · ${formatearPesos(p.precioVenta)}',
              style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
          trailing: p.sinStock
              ? Text('Sin stock',
                  style: RegentaType.codigo
                      .copyWith(fontSize: 11, color: RegentaColors.crit))
              : const Icon(Icons.add_circle_outline, color: RegentaColors.accent),
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
        InkWell(
          onTap: onAgregarCliente,
          child: Container(
            height: 40,
            padding: const EdgeInsets.symmetric(horizontal: 11),
            decoration: BoxDecoration(
              color: RegentaColors.paper,
              borderRadius: BorderRadius.circular(RegentaSpacing.radius),
              border: Border.all(color: RegentaColors.line2),
            ),
            child: Row(
              children: [
                const Icon(Icons.person_outline, size: 17, color: RegentaColors.faint),
                const SizedBox(width: 9),
                Expanded(
                  child: Text('Consumidor final',
                      style: RegentaType.cuerpo
                          .copyWith(fontSize: 13.5, color: RegentaColors.ink2)),
                ),
                Text('Agregar cliente',
                    style: RegentaType.cuerpo.copyWith(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.accent)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        if (estado.lineas.isEmpty)
          Padding(
            padding: const EdgeInsets.only(top: 40),
            child: Text('Busca o escanea un producto para empezar.',
                textAlign: TextAlign.center,
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
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
                        color: RegentaColors.ink)),
              ),
              const SizedBox(width: 8),
              InkWell(
                onTap: onVaciar,
                child: Text('Vaciar',
                    style: RegentaType.cuerpo.copyWith(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.crit)),
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
  const _LineaWidget({required this.linea, required this.onMas, required this.onMenos});

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
                Text(linea.producto.nombre,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.item
                        .copyWith(fontSize: 13.5, color: RegentaColors.ink)),
                const SizedBox(height: 3),
                Text(
                  linea.sinStock
                      ? '${linea.producto.sku} · sin stock'
                      : '${linea.producto.sku} · ${formatearPesos(linea.producto.precioVenta)}',
                  style: RegentaType.codigo.copyWith(
                      fontSize: 10.5,
                      color:
                          linea.sinStock ? RegentaColors.crit : RegentaColors.muted),
                ),
              ],
            ),
          ),
          const SizedBox(width: 9),
          _Stepper(cantidad: linea.cantidad, onMas: onMas, onMenos: onMenos),
          const SizedBox(width: 9),
          SizedBox(
            width: 92,
            child: Text(formatearPesos(linea.total),
                textAlign: TextAlign.right,
                style: RegentaType.codigo.copyWith(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.ink)),
          ),
        ],
      ),
    );
  }
}

class _Stepper extends StatelessWidget {
  const _Stepper({required this.cantidad, required this.onMas, required this.onMenos});

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
          _BotonStepper(icono: Icons.remove, onTap: onMenos, semantica: 'Quitar uno'),
          SizedBox(
            width: 34,
            child: Text('$cantidad',
                textAlign: TextAlign.center,
                style: RegentaType.codigo.copyWith(
                    fontSize: 13.5,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.ink)),
          ),
          _BotonStepper(icono: Icons.add, onTap: onMas, semantica: 'Agregar uno'),
        ],
      ),
    );
  }
}

class _BotonStepper extends StatelessWidget {
  const _BotonStepper(
      {required this.icono, required this.onTap, required this.semantica});

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
              Text('Total',
                  style: RegentaType.seccion
                      .copyWith(fontSize: 17, color: RegentaColors.ink)),
              const SizedBox(width: 8),
              Flexible(
                child: Text(formatearPesos(estado.total),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    textAlign: TextAlign.right,
                    style: RegentaType.dinero.copyWith(color: RegentaColors.ink)),
              ),
            ],
          ),
          const SizedBox(height: 12),
          if (estado.hayLineasSinStock)
            Padding(
              padding: const EdgeInsets.only(bottom: 8),
              child: Text('Hay líneas sin stock',
                  style: RegentaType.cuerpo.copyWith(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: RegentaColors.crit)),
            ),
          SizedBox(
            width: double.infinity,
            height: 52,
            child: FilledButton(
              onPressed: estado.puedeCobrar ? () => onCobrar() : null,
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.accent,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(4)),
              ),
              child: Text(estado.cobrando ? 'Cobrando…' : 'Cobrar',
                  style: RegentaType.item
                      .copyWith(fontSize: 14, color: RegentaColors.surface)),
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
            child: Text(etiqueta,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.cuerpo
                    .copyWith(fontSize: 12.5, color: RegentaColors.ink2)),
          ),
          const SizedBox(width: 8),
          Flexible(
            child: Text(valor,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                textAlign: TextAlign.right,
                style: RegentaType.codigo.copyWith(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w500,
                    color: RegentaColors.ink2)),
          ),
        ],
      ),
    );
  }
}
