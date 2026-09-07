import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/estado_de_busqueda.dart';
import '../busqueda/proveedores.dart';
import '../datos/producto_encontrado.dart';
import '../escaner/escaner_de_codigos.dart';
import 'captura_manual_de_codigo.dart';
import 'fila_de_producto.dart';

/// La pantalla de inventario del vendedor. HU-035.
///
/// Buscar por nombre, SKU o código; escanear (en Android) o teclear el código
/// (siempre disponible, y en la web el único camino). Al resolver un código
/// llama a [onProductoSeleccionado] con el id y el factor de conversión — quien
/// monta la pantalla decide si abre la ficha o lo agrega a la venta.
class PantallaInventario extends ConsumerStatefulWidget {
  const PantallaInventario({
    super.key,
    required this.onProductoSeleccionado,
    this.onNavegar,
    this.titulo = 'Inventario',
  });

  final void Function(String productoId, num factor) onProductoSeleccionado;
  final void Function(String destino)? onNavegar;
  final String titulo;

  @override
  ConsumerState<PantallaInventario> createState() => _PantallaInventarioState();
}

class _PantallaInventarioState extends ConsumerState<PantallaInventario> {
  final TextEditingController _buscador = TextEditingController();
  final FocusNode _foco = FocusNode();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _foco.requestFocus());
  }

  @override
  void dispose() {
    _buscador.dispose();
    _foco.dispose();
    super.dispose();
  }

  Future<void> _escanear() async {
    final escaner = ref.read(escanerProvider);
    if (await escaner.disponible) {
      try {
        if (!mounted) return;
        final codigo = await escaner.escanearUnCodigo(context);
        if (codigo != null && codigo.isNotEmpty) await _resolver(codigo);
        return; // escaneó, o canceló la cámara a propósito
      } on EscaneoNoDisponible {
        // la cámara falló: cae al ingreso manual
      }
    }
    if (!mounted) return;
    final codigo = await pedirCodigoAMano(context);
    if (codigo != null && codigo.isNotEmpty) await _resolver(codigo);
  }

  Future<void> _resolver(String codigo) async {
    final repo = ref.read(repositorioDeInventarioProvider);
    try {
      final resuelto = await repo.resolverCodigo(codigo);
      widget.onProductoSeleccionado(resuelto.productoId, resuelto.factor);
    } on ErrorDeApi catch (e) {
      if (!mounted) return;
      final sinCodigo = e is ErrorDesconocido && e.codigo == 404;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(sinCodigo
            ? 'Ningún producto tiene el código «$codigo».'
            : e.mensaje),
      ));
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeBusquedaProvider);
    final controlador = ref.read(controladorDeBusquedaProvider.notifier);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: _AppBar(titulo: widget.titulo),
      bottomNavigationBar: _BottomNav(onNavegar: widget.onNavegar),
      body: Column(
        children: [
          _BarraDeBusqueda(
            controlador: _buscador,
            foco: _foco,
            onCambio: controlador.cambiarTermino,
            onEscanear: _escanear,
          ),
          _Chips(
            soloBajoMinimo: estado.soloBajoMinimo,
            onTodas: () => controlador.elegirCategoria(null),
            onBajoMinimo: controlador.alternarBajoMinimo,
          ),
          Expanded(child: _Cuerpo(estado: estado, controlador: controlador, onTap: _abrir)),
        ],
      ),
    );
  }

  void _abrir(ProductoEncontrado p) => widget.onProductoSeleccionado(p.id, 1);
}

class _AppBar extends StatelessWidget implements PreferredSizeWidget {
  const _AppBar({required this.titulo});
  final String titulo;

  @override
  Size get preferredSize => const Size.fromHeight(56);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      backgroundColor: RegentaColors.surface,
      surfaceTintColor: RegentaColors.surface,
      elevation: 0,
      shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
      titleSpacing: 14,
      title: Row(
        children: [
          Container(
            width: 26,
            height: 26,
            decoration: BoxDecoration(
                color: RegentaColors.accent, borderRadius: BorderRadius.circular(5)),
            alignment: Alignment.center,
            child: Text('R',
                style: RegentaType.codigo.copyWith(
                    fontSize: 13,
                    fontWeight: FontWeight.w700,
                    color: RegentaColors.surface)),
          ),
          const SizedBox(width: 11),
          Text(titulo,
              style: RegentaType.seccion
                  .copyWith(fontSize: 16, color: RegentaColors.ink)),
        ],
      ),
      actions: const [
        Icon(Icons.filter_list, size: 20, color: RegentaColors.ink2),
        SizedBox(width: 14),
        Icon(Icons.add, size: 22, color: RegentaColors.ink2),
        SizedBox(width: 14),
      ],
    );
  }
}

class _BarraDeBusqueda extends StatelessWidget {
  const _BarraDeBusqueda({
    required this.controlador,
    required this.foco,
    required this.onCambio,
    required this.onEscanear,
  });

  final TextEditingController controlador;
  final FocusNode foco;
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
              height: 42,
              child: TextField(
                controller: controlador,
                focusNode: foco,
                onChanged: onCambio,
                style: RegentaType.cuerpo.copyWith(fontSize: 14, color: RegentaColors.ink),
                decoration: InputDecoration(
                  isDense: true,
                  prefixIcon:
                      const Icon(Icons.search, size: 18, color: RegentaColors.faint),
                  hintText: 'Nombre, SKU o código…',
                  hintStyle:
                      RegentaType.cuerpo.copyWith(fontSize: 14, color: RegentaColors.faint),
                  contentPadding: const EdgeInsets.symmetric(vertical: 11),
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
                width: 42,
                height: 42,
                decoration: BoxDecoration(
                  color: RegentaColors.accent,
                  borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                ),
                child: const Icon(Icons.qr_code_scanner,
                    size: 21, color: RegentaColors.surface),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _Chips extends StatelessWidget {
  const _Chips({
    required this.soloBajoMinimo,
    required this.onTodas,
    required this.onBajoMinimo,
  });

  final bool soloBajoMinimo;
  final VoidCallback onTodas;
  final VoidCallback onBajoMinimo;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: RegentaColors.surface,
      padding: const EdgeInsets.fromLTRB(14, 0, 14, 12),
      child: Row(
        children: [
          _Chip(texto: 'Todas', activo: !soloBajoMinimo, onTap: onTodas),
          const SizedBox(width: 7),
          _Chip(
            texto: 'Bajo mínimo',
            activo: soloBajoMinimo,
            critico: true,
            onTap: onBajoMinimo,
          ),
        ],
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({
    required this.texto,
    required this.activo,
    required this.onTap,
    this.critico = false,
  });

  final String texto;
  final bool activo;
  final bool critico;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final fondo = activo
        ? (critico ? RegentaColors.crit : RegentaColors.accent)
        : (critico ? RegentaColors.critSoft : RegentaColors.sunken);
    final color = activo
        ? RegentaColors.surface
        : (critico ? RegentaColors.crit : RegentaColors.ink2);
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration:
            BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(3)),
        child: Text(
          texto.toUpperCase(),
          style: RegentaType.etiqueta.copyWith(fontSize: 9.5, color: color),
        ),
      ),
    );
  }
}

class _Cuerpo extends StatelessWidget {
  const _Cuerpo({required this.estado, required this.controlador, required this.onTap});

  final EstadoDeBusqueda estado;
  final dynamic controlador;
  final void Function(ProductoEncontrado) onTap;

  @override
  Widget build(BuildContext context) {
    switch (estado.fase) {
      case FaseBusqueda.cargando:
        return const Center(child: CircularProgressIndicator());
      case FaseBusqueda.error:
        return _Mensaje(
          texto: estado.mensajeError ?? 'Algo salió mal.',
          accion: 'Reintentar',
          onAccion: controlador.reintentar,
        );
      case FaseBusqueda.sinResultados:
        return _Mensaje(
          texto: estado.termino.trim().isEmpty
              ? 'No hay productos todavía.'
              : 'No hay productos que coincidan con «${estado.termino.trim()}».',
        );
      case FaseBusqueda.conDatos:
        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 12, 14, 2),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text('${estado.total} productos',
                      style: RegentaType.cuerpo.copyWith(
                          fontWeight: FontWeight.w600, color: RegentaColors.ink)),
                ],
              ),
            ),
            Expanded(
              child: ListView.builder(
                padding: const EdgeInsets.symmetric(horizontal: 14),
                itemCount: estado.resultados.length,
                itemBuilder: (context, i) => FilaDeProducto(
                  producto: estado.resultados[i],
                  onTap: () => onTap(estado.resultados[i]),
                ),
              ),
            ),
          ],
        );
    }
  }
}

class _Mensaje extends StatelessWidget {
  const _Mensaje({required this.texto, this.accion, this.onAccion});
  final String texto;
  final String? accion;
  final VoidCallback? onAccion;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(28),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text(texto,
                textAlign: TextAlign.center,
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
            if (accion != null) ...[
              const SizedBox(height: 12),
              OutlinedButton(onPressed: onAccion, child: Text(accion!)),
            ],
          ],
        ),
      ),
    );
  }
}

class _BottomNav extends StatelessWidget {
  const _BottomNav({this.onNavegar});
  final void Function(String destino)? onNavegar;

  static const _items = <(String, IconData)>[
    ('inicio', Icons.bar_chart),
    ('inventario', Icons.inventory_2_outlined),
    ('vender', Icons.shopping_cart_outlined),
    ('clientes', Icons.people_outline),
    ('mas', Icons.more_horiz),
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(top: BorderSide(color: RegentaColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(4, 6, 4, 10),
      child: Row(
        children: [
          for (final (destino, icono) in _items)
            Expanded(
              child: InkWell(
                onTap: () => onNavegar?.call(destino),
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(icono,
                        size: 21,
                        color: destino == 'inventario'
                            ? RegentaColors.accent
                            : RegentaColors.faint),
                    const SizedBox(height: 4),
                    Text(
                      destino[0].toUpperCase() + destino.substring(1),
                      style: RegentaType.cuerpo.copyWith(
                        fontSize: 10,
                        fontWeight: FontWeight.w600,
                        color: destino == 'inventario'
                            ? RegentaColors.accent
                            : RegentaColors.faint,
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
}
