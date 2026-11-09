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
///
/// De arriba hacia abajo: la franja de cliente, el buscador a todo el ancho y
/// la venta a todo el ancho, con los totales y *Cobrar* debajo del listado
/// (R10). Los resultados de la búsqueda se despliegan encima de la venta sólo
/// mientras se busca (R10b), y Enter agrega sin tocar el mouse (R7, R7b).
///
/// Es **un solo widget** que se adapta con `LayoutBuilder` en
/// [kBreakpointEscritorio]: en escritorio la venta es una tabla; en móvil, una
/// tarjeta por línea con el total y *Cobrar* fijos abajo. Arriba a la derecha,
/// las ventas abiertas a la vez (R12).
class PantallaPos extends ConsumerStatefulWidget {
  const PantallaPos({super.key, this.onAgregarCliente, this.onVentaCobrada});

  /// Si se da, reemplaza al selector de cliente propio del POS.
  final VoidCallback? onAgregarCliente;
  final void Function(String numeroVenta)? onVentaCobrada;

  @override
  ConsumerState<PantallaPos> createState() => _PantallaPosState();
}

class _PantallaPosState extends ConsumerState<PantallaPos> {
  final TextEditingController _buscador = TextEditingController();
  final FocusNode _foco = FocusNode(debugLabel: 'buscador-pos');

  /// R7b: el resultado que Enter agregaría. Se mueve con ↑ ↓.
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

  /// R7c: `F2` y `F12` funcionan aunque el foco no esté en el buscador,
  /// mientras el POS sea la pantalla de arriba (no con un diálogo encima).
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

  /// ↑ ↓ mueven el resaltado y `Esc` cierra la lista (R7b, R7c).
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
          shape: RoundedRectangleBorder(
            borderRadius: BorderRadius.circular(16),
          ),
          child: SizedBox(width: 560, height: 640, child: selector(contexto)),
        ),
      );
    } else {
      await Navigator.of(
        context,
      ).push(MaterialPageRoute<void>(builder: selector));
    }
    if (mounted) _enfocarBuscador();
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDelPosProvider);
    final pos = ref.read(controladorDelPosProvider.notifier);

    ref.listen<EstadoDelPos>(controladorDelPosProvider, (antes, ahora) {
      if (!identical(antes?.resultados, ahora.resultados)) _resaltado = 0;
      // R6: al agregar, el término se limpia y el foco se queda en el buscador.
      if (ahora.termino.isEmpty && _buscador.text.isNotEmpty) {
        _buscador.clear();
        _enfocarBuscador();
      }
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
          style: PastelTheme.titulo(
            fontSize: 18,
            fontWeight: FontWeight.w700,
            color: RegentaColors.ink,
          ),
        ),
        actions: [
          for (var v = 0; v < estado.ventasAbiertas; v++)
            Padding(
              padding: const EdgeInsets.only(left: 8),
              child: _BotonDeVenta(
                numero: v + 1,
                activa: estado.ventaActiva == v,
                productos: estado.productosEnVenta(v),
                onTap: () => _cambiarDeVenta(() => pos.cambiarDeVenta(v)),
              ),
            ),
          Padding(
            padding: const EdgeInsets.only(left: 8, right: 14),
            child: _BotonNuevaVenta(
              onTap: estado.ventasAbiertas >= ventasMaximas
                  ? null
                  : () => _cambiarDeVenta(pos.nuevaVenta),
            ),
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
                elevation: 10,
                shadowColor: RegentaColors.ink.withValues(alpha: 0.25),
                borderRadius: const BorderRadius.vertical(
                  bottom: Radius.circular(12),
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
                          onQuitar: pos.quitar,
                          onVaciar: pos.vaciar,
                          onEspera: () => _cambiarDeVenta(pos.ponerEnEspera),
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
// Encabezado: ventas abiertas
// ---------------------------------------------------------------------------

/// R12: la pestaña de una de las ventas abiertas. Si la venta lleva
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
                    style: PastelTheme.cuerpo(
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
                    style: PastelTheme.cuerpo(
                      fontSize: 10,
                      fontWeight: FontWeight.w700,
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
      excludeSemantics: true,
      child: Material(
        color: RegentaColors.accentSoft,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
        child: InkWell(
          key: const Key('venta-nueva'),
          onTap: onTap,
          borderRadius: BorderRadius.circular(10),
          child: SizedBox(
            width: 40,
            height: 40,
            child: Icon(
              Icons.add,
              size: 20,
              color: onTap == null ? RegentaColors.faint : RegentaColors.accent,
            ),
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
      width: 40,
      height: 40,
      decoration: BoxDecoration(
        color: c == null ? RegentaColors.sunken : RegentaColors.moduloClientes,
        shape: BoxShape.circle,
      ),
      alignment: Alignment.center,
      child: c == null
          ? const Icon(
              Icons.person_outline,
              size: 20,
              color: RegentaColors.ink2,
            )
          : Text(
              c.iniciales,
              style: PastelTheme.cuerpo(
                fontSize: 13,
                fontWeight: FontWeight.w800,
                color: RegentaColors.comanda,
              ),
            ),
    );
    final datos = Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(
          'CLIENTE',
          style: PastelTheme.cuerpo(
            fontSize: 10.5,
            fontWeight: FontWeight.w700,
            letterSpacing: 0.8,
            color: RegentaColors.ink2,
          ),
        ),
        const SizedBox(height: 2),
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
        if (c != null)
          Text(
            [c.documento, if (c.condicion != null) c.condicion!].join(' · '),
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: PastelTheme.cuerpo(
              fontSize: 12.5,
              color: RegentaColors.ink2,
            ),
          ),
      ],
    );
    final botones = Wrap(
      spacing: 8,
      runSpacing: 8,
      children: [
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
          ),
        ),
        if (c != null)
          TextButton(
            onPressed: onQuitar,
            style: TextButton.styleFrom(
              minimumSize: const Size(44, 44),
              foregroundColor: RegentaColors.crit,
            ),
            child: const Text('Quitar'),
          ),
      ],
    );

    return Container(
      key: const Key('pos-cliente'),
      padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
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
                const SizedBox(width: 12),
                Expanded(child: datos),
                const SizedBox(width: 12),
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
          // Sin esto, Enter soltaría el foco y el siguiente código del lector
          // se perdería.
          onEditingComplete: () {},
          onSubmitted: (_) => onEnter(),
          cursorColor: RegentaColors.surface,
          style: PastelTheme.cuerpo(fontSize: 15, color: RegentaColors.surface),
          decoration: InputDecoration(
            filled: true,
            fillColor: RegentaColors.ink,
            isDense: false,
            contentPadding: const EdgeInsets.symmetric(
              vertical: 16,
              horizontal: 12,
            ),
            prefixIcon: const Icon(
              Icons.search,
              size: 20,
              color: RegentaColors.faint,
            ),
            suffixIcon: const Padding(
              padding: EdgeInsets.only(right: 12),
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
              borderSide: const BorderSide(
                color: RegentaColors.accent,
                width: 2,
              ),
            ),
          ),
        ),
      ),
    );
  }
}

/// La etiqueta de un atajo de teclado: `F2`, `F12`.
class _Atajo extends StatelessWidget {
  const _Atajo(this.tecla, {this.oscuro = false});

  final String tecla;
  final bool oscuro;

  @override
  Widget build(BuildContext context) {
    final color = oscuro ? RegentaColors.faint : RegentaColors.surface;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
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

/// R10b: la lista que se despliega bajo el buscador, encima de la venta.
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
    if (estado.buscando && estado.resultados.isEmpty) {
      return const Padding(
        padding: EdgeInsets.all(20),
        child: Center(
          child: SizedBox(
            width: 24,
            height: 24,
            child: CircularProgressIndicator(),
          ),
        ),
      );
    }
    if (estado.resultados.isEmpty) {
      return Padding(
        padding: const EdgeInsets.all(20),
        child: Text(
          'No se encontró «${estado.termino.trim()}»',
          style: PastelTheme.cuerpo(fontSize: 14, color: RegentaColors.ink2),
        ),
      );
    }
    return ListView.builder(
      key: const Key('pos-resultados'),
      shrinkWrap: true,
      padding: const EdgeInsets.symmetric(vertical: 6),
      itemCount: estado.resultados.length,
      itemBuilder: (context, i) {
        final p = estado.resultados[i];
        final existencia = p.sinStock
            ? 'Sin stock'
            : p.existencia != null
            ? formatearCantidad(p.existencia!)
            : (p.nivelStock == NivelStock.bajo ? 'Stock bajo' : 'Disponible');
        return Material(
          color: i == resaltado
              ? RegentaColors.accentSoft
              : RegentaColors.surface,
          child: InkWell(
            onTap: () => onAgregar(p),
            child: ConstrainedBox(
              constraints: const BoxConstraints(
                minHeight: RegentaSpacing.hitTarget,
              ),
              child: Padding(
                padding: const EdgeInsets.symmetric(
                  horizontal: 14,
                  vertical: 8,
                ),
                child: Row(
                  children: [
                    SizedBox(
                      width: 110,
                      child: Text(
                        p.sku,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: RegentaType.codigo.copyWith(
                          fontSize: 12,
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
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: RegentaColors.ink,
                        ),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Text(
                      formatearPesos(p.precioVenta),
                      style: RegentaType.codigo.copyWith(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.ink,
                      ),
                    ),
                    const SizedBox(width: 14),
                    SizedBox(
                      width: 84,
                      child: Text(
                        existencia,
                        textAlign: TextAlign.right,
                        style: RegentaType.codigo.copyWith(
                          fontSize: 11.5,
                          color: p.sinStock
                              ? RegentaColors.crit
                              : RegentaColors.ink2,
                        ),
                      ),
                    ),
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

// ---------------------------------------------------------------------------
// Venta en escritorio: tabla + pie con totales (R10)
// ---------------------------------------------------------------------------

class _VentaEscritorio extends StatelessWidget {
  const _VentaEscritorio({
    required this.estado,
    required this.onMas,
    required this.onMenos,
    required this.onQuitar,
    required this.onVaciar,
    required this.onEspera,
    required this.onCobrar,
  });

  final EstadoDelPos estado;
  final void Function(int) onMas;
  final void Function(int) onMenos;
  final void Function(int) onQuitar;
  final VoidCallback onVaciar;
  final VoidCallback onEspera;
  final Future<void> Function() onCobrar;

  static const anchoCodigo = 130.0;
  static const anchoCantidad = 150.0;
  static const anchoPrecio = 120.0;
  static const anchoIva = 70.0;
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
    Widget celda(
      String texto,
      double ancho, {
      TextAlign alinear = TextAlign.left,
    }) => SizedBox(
      width: ancho,
      child: Text(texto, textAlign: alinear, style: cabecera),
    );

    return Padding(
      padding: const EdgeInsets.fromLTRB(16, 0, 16, 16),
      child: Container(
        key: const Key('pos-carrito'),
        decoration: BoxDecoration(
          color: RegentaColors.surface,
          borderRadius: BorderRadius.circular(16),
          border: Border.all(color: RegentaColors.line),
        ),
        clipBehavior: Clip.antiAlias,
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            _CabeceraDeVenta(estado: estado, onVaciar: onVaciar),
            Container(
              color: RegentaColors.sunken,
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 11),
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
                        onQuitar: () => onQuitar(i),
                      ),
                    ),
            ),
            Container(
              decoration: const BoxDecoration(
                color: RegentaColors.surface,
                border: Border(top: BorderSide(color: RegentaColors.line)),
              ),
              padding: const EdgeInsets.all(16),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.end,
                children: [
                  Expanded(
                    child: Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: [
                        const _BotonSecundario(
                          icono: Icons.percent,
                          texto: 'Descuento',
                          onTap: null,
                          proximamente: true,
                        ),
                        const _BotonSecundario(
                          icono: Icons.sticky_note_2_outlined,
                          texto: 'Nota',
                          onTap: null,
                          proximamente: true,
                        ),
                        _BotonSecundario(
                          icono: Icons.pause_circle_outline,
                          texto: 'Poner en espera',
                          onTap: estado.lineas.isEmpty ? null : onEspera,
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 24),
                  SizedBox(
                    width: 380,
                    child: _Totales(
                      estado: estado,
                      onCobrar: onCobrar,
                      grande: true,
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
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
      padding: const EdgeInsets.fromLTRB(16, 8, 8, 8),
      child: Row(
        children: [
          Expanded(
            child: Text(
              '${estado.lineas.length} ${estado.lineas.length == 1 ? 'producto' : 'productos'}'
              ' · ${estado.unidades} ${estado.unidades == 1 ? 'unidad' : 'unidades'}',
              style: PastelTheme.cuerpo(
                fontSize: 14,
                fontWeight: FontWeight.w700,
                color: RegentaColors.ink,
              ),
            ),
          ),
          TextButton.icon(
            onPressed: estado.lineas.isEmpty ? null : onVaciar,
            icon: const Icon(Icons.delete_sweep_outlined, size: 18),
            label: const Text('Vaciar venta'),
            style: TextButton.styleFrom(
              foregroundColor: RegentaColors.crit,
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
              width: 64,
              height: 64,
              decoration: const BoxDecoration(
                color: RegentaColors.moduloVentas,
                shape: BoxShape.circle,
              ),
              child: const Icon(
                Icons.qr_code_scanner,
                size: 28,
                color: RegentaColors.accent,
              ),
            ),
            const SizedBox(height: 14),
            Text(
              'Busca o escanea un producto para empezar la venta.',
              textAlign: TextAlign.center,
              style: PastelTheme.cuerpo(
                fontSize: 14,
                color: RegentaColors.ink2,
              ),
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
    required this.onQuitar,
  });

  final LineaDeCarrito linea;
  final VoidCallback onMas;
  final VoidCallback onMenos;
  final VoidCallback onQuitar;

  @override
  Widget build(BuildContext context) {
    final dinero = RegentaType.codigo.copyWith(
      fontSize: 13.5,
      color: RegentaColors.ink2,
    );
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
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
              style: RegentaType.codigo.copyWith(
                fontSize: 12.5,
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
                  style: PastelTheme.cuerpo(
                    fontSize: 14.5,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.ink,
                  ),
                ),
                if (linea.sinStock)
                  Text(
                    'Sin stock',
                    style: RegentaType.codigo.copyWith(
                      fontSize: 11,
                      color: RegentaColors.crit,
                    ),
                  ),
              ],
            ),
          ),
          SizedBox(
            width: _VentaEscritorio.anchoCantidad,
            child: Center(
              child: _Stepper(
                cantidad: linea.cantidad,
                onMas: onMas,
                onMenos: onMenos,
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
            child: Text(
              formatearPesos(linea.total),
              textAlign: TextAlign.right,
              style: RegentaType.codigo.copyWith(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: RegentaColors.ink,
              ),
            ),
          ),
          SizedBox(
            width: _VentaEscritorio.anchoAccion,
            child: IconButton(
              tooltip: 'Quitar línea',
              onPressed: onQuitar,
              icon: const Icon(
                Icons.delete_outline,
                size: 20,
                color: RegentaColors.faint,
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _BotonSecundario extends StatelessWidget {
  const _BotonSecundario({
    required this.icono,
    required this.texto,
    required this.onTap,
    this.proximamente = false,
  });

  final IconData icono;
  final String texto;
  final VoidCallback? onTap;

  /// Descuento y Nota: se ven, pero esperan contrato del backend.
  final bool proximamente;

  @override
  Widget build(BuildContext context) {
    final boton = OutlinedButton.icon(
      onPressed: proximamente ? null : onTap,
      icon: Icon(icono, size: 18),
      label: Text(texto),
      style: OutlinedButton.styleFrom(
        minimumSize: const Size(44, 44),
        foregroundColor: RegentaColors.ink,
        side: const BorderSide(color: RegentaColors.line2),
        padding: const EdgeInsets.symmetric(horizontal: 14),
      ),
    );
    return proximamente
        ? Tooltip(message: 'Próximamente', child: boton)
        : boton;
  }
}

// ---------------------------------------------------------------------------
// Venta en móvil: tarjetas + pie fijo
// ---------------------------------------------------------------------------

class _VentaMovil extends StatelessWidget {
  const _VentaMovil({
    required this.estado,
    required this.onMas,
    required this.onMenos,
    required this.onQuitar,
    required this.onVaciar,
  });

  final EstadoDelPos estado;
  final void Function(int) onMas;
  final void Function(int) onMenos;
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
          _TarjetaDeLinea(
            linea: estado.lineas[i],
            onMas: () => onMas(i),
            onMenos: () => onMenos(i),
            onQuitar: () => onQuitar(i),
          ),
      ],
    );
  }
}

class _TarjetaDeLinea extends StatelessWidget {
  const _TarjetaDeLinea({
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
    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.fromLTRB(14, 10, 4, 10),
      decoration: BoxDecoration(
        color: linea.sinStock ? RegentaColors.critSoft : RegentaColors.surface,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: RegentaColors.line),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
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
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.ink,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      linea.sinStock
                          ? '${linea.producto.sku} · sin stock'
                          : '${linea.producto.sku} · ${formatearPesos(linea.producto.precioVenta)}',
                      style: RegentaType.codigo.copyWith(
                        fontSize: 11,
                        color: linea.sinStock
                            ? RegentaColors.crit
                            : RegentaColors.ink2,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton(
                tooltip: 'Quitar línea',
                onPressed: onQuitar,
                icon: const Icon(
                  Icons.delete_outline,
                  size: 20,
                  color: RegentaColors.faint,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Row(
            children: [
              _Stepper(
                cantidad: linea.cantidad,
                onMas: onMas,
                onMenos: onMenos,
              ),
              const Spacer(),
              Padding(
                padding: const EdgeInsets.only(right: 10),
                child: Text(
                  formatearPesos(linea.total),
                  style: RegentaType.codigo.copyWith(
                    fontSize: 15,
                    fontWeight: FontWeight.w600,
                    color: RegentaColors.ink,
                  ),
                ),
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
    required this.onCobrar,
  });

  final EstadoDelPos estado;
  final VoidCallback onEspera;
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
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                const _BotonSecundario(
                  icono: Icons.percent,
                  texto: 'Descuento',
                  onTap: null,
                  proximamente: true,
                ),
                const SizedBox(width: 8),
                const _BotonSecundario(
                  icono: Icons.sticky_note_2_outlined,
                  texto: 'Nota',
                  onTap: null,
                  proximamente: true,
                ),
                const SizedBox(width: 8),
                _BotonSecundario(
                  icono: Icons.pause_circle_outline,
                  texto: 'En espera',
                  onTap: estado.lineas.isEmpty ? null : onEspera,
                ),
              ],
            ),
          ),
          const SizedBox(height: 10),
          _Totales(estado: estado, onCobrar: onCobrar, grande: false),
        ],
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Piezas compartidas
// ---------------------------------------------------------------------------

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
        borderRadius: BorderRadius.circular(12),
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
            width: 36,
            child: Text(
              '$cantidad',
              textAlign: TextAlign.center,
              style: RegentaType.codigo.copyWith(
                fontSize: 14,
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
        borderRadius: BorderRadius.circular(12),
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
  const _Totales({
    required this.estado,
    required this.onCobrar,
    required this.grande,
  });

  final EstadoDelPos estado;
  final Future<void> Function() onCobrar;
  final bool grande;

  @override
  Widget build(BuildContext context) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (grande) ...[
          _Fila('Subtotal', formatearPesos(estado.subtotal)),
          _Fila('IVA', formatearPesos(estado.impuesto)),
          const SizedBox(height: 6),
        ],
        Row(
          crossAxisAlignment: CrossAxisAlignment.baseline,
          textBaseline: TextBaseline.alphabetic,
          children: [
            Text(
              'Total',
              style: PastelTheme.titulo(
                fontSize: grande ? 18 : 16,
                fontWeight: FontWeight.w700,
                color: RegentaColors.ink,
              ),
            ),
            if (!grande) ...[
              const SizedBox(width: 8),
              Flexible(
                child: Text(
                  'IVA ${formatearPesos(estado.impuesto)}',
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.codigo.copyWith(
                    fontSize: 11.5,
                    color: RegentaColors.ink2,
                  ),
                ),
              ),
            ],
            const Spacer(),
            Text(
              formatearPesos(estado.total),
              style: RegentaType.dinero.copyWith(
                fontSize: grande ? 30 : 24,
                fontWeight: FontWeight.w700,
                color: RegentaColors.ink,
              ),
            ),
          ],
        ),
        const SizedBox(height: 10),
        if (estado.hayLineasSinStock)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Text(
              'Hay líneas sin stock',
              style: PastelTheme.cuerpo(
                fontSize: 12.5,
                fontWeight: FontWeight.w700,
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
              style: PastelTheme.cuerpo(
                fontSize: 12.5,
                fontWeight: FontWeight.w700,
                color: RegentaColors.warn,
              ),
            ),
          ),
        SizedBox(
          height: 54,
          child: FilledButton(
            onPressed: estado.puedeCobrar ? () => onCobrar() : null,
            style: FilledButton.styleFrom(
              backgroundColor: RegentaColors.accent,
            ),
            child: Row(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  estado.cobrando ? 'Cobrando…' : 'Cobrar',
                  style: PastelTheme.cuerpo(
                    fontSize: 16,
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
      ],
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
        children: [
          Text(
            etiqueta,
            style: PastelTheme.cuerpo(
              fontSize: 13.5,
              color: RegentaColors.ink2,
            ),
          ),
          const Spacer(),
          Text(
            valor,
            style: RegentaType.codigo.copyWith(
              fontSize: 13.5,
              fontWeight: FontWeight.w500,
              color: RegentaColors.ink2,
            ),
          ),
        ],
      ),
    );
  }
}
