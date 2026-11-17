import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/controlador_de_inventario.dart';
import '../busqueda/proveedores.dart';
import '../datos/ficha_de_producto.dart';
import '../datos/importacion.dart';
import '../datos/inventario_general.dart';
import '../escaner/escaner_de_codigos.dart';
import 'captura_manual_de_codigo.dart';
import 'formato.dart';
import 'pantalla_equivalencias.dart';
import 'pantalla_ficha_de_producto.dart';
import 'pantalla_importacion.dart';
import 'pantalla_tablas_de_fracciones.dart';
import 'pantalla_unidades.dart';

/// La pantalla de Inventario, en una sola ventana (`design/comportamiento/Inventario.md`).
///
/// La lista sale del inventario general, de a 50 (R5). En web es la tabla del mockup con el
/// panel de valorizado, existencias por bodega y bajo mínimo (R6); en móvil, filas apiladas.
/// Filtros, categorías y bajo mínimo (R8), tarifa en bloque (HU-154), y unidades, fracciones,
/// equivalencias e importar en el menú «Más opciones» (R10).
///
/// Buscar por nombre, SKU o código; escanear (en Android) o teclear el código (siempre
/// disponible, y en la web el único camino). Al resolver un código llama a
/// [onProductoSeleccionado] con el id y el factor de conversión.
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
  final ScrollController _scroll = ScrollController();

  /// HU-154: filas marcadas para cambiarles la tarifa en bloque.
  final Set<String> _seleccion = {};
  List<OpcionDeFicha> _categorias = const [];

  /// R9 · el producto elegido y su existencia en cada bodega (panel web).
  FilaDeInventario? _elegida;
  List<(String, num)>? _existencias;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_alHacerScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) => _foco.requestFocus());
    ref.read(repositorioDeInventarioProvider).categorias().then((c) {
      if (mounted) setState(() => _categorias = c);
    }).catchError((Object _) {});
  }

  @override
  void dispose() {
    _buscador.dispose();
    _foco.dispose();
    _scroll.dispose();
    super.dispose();
  }

  ControladorDeInventario get _controlador => ref.read(controladorDeInventarioProvider.notifier);

  /// R5 · a 600 px del final se pide la página siguiente.
  void _alHacerScroll() {
    if (_scroll.position.pixels >= _scroll.position.maxScrollExtent - 600) {
      _controlador.cargarMas();
    }
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
    try {
      final resuelto = await ref.read(repositorioDeInventarioProvider).resolverCodigo(codigo);
      widget.onProductoSeleccionado(resuelto.productoId, resuelto.factor);
    } on ErrorDeApi catch (e) {
      if (!mounted) return;
      final sinCodigo = e is ErrorDesconocido && e.codigo == 404;
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(
        content: Text(sinCodigo ? 'Ningún producto tiene el código «$codigo».' : e.mensaje),
      ));
    }
  }

  // ---- acciones sobre productos ----

  Future<void> _elegir(FilaDeInventario f) async {
    setState(() {
      _elegida = f;
      _existencias = null;
    });
    try {
      final e = await ref.read(repositorioDeInventarioProvider).existenciasDe(f.id);
      if (mounted && _elegida?.id == f.id) setState(() => _existencias = e);
    } on ErrorDeApi {
      if (mounted && _elegida?.id == f.id) setState(() => _existencias = const []);
    }
  }

  Future<void> _abrirFicha(String productoId) async {
    await Navigator.of(context).push(MaterialPageRoute<void>(
      builder: (_) => PantallaFichaDeProducto(productoId: productoId),
    ));
    if (mounted) _controlador.recargar();
  }

  void _agregarAVenta(FilaDeInventario f) {
    widget.onProductoSeleccionado(f.id, 1);
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text('«${f.nombre}» agregado a la venta actual.')));
  }

  void _alternarSeleccion(String id) =>
      setState(() => _seleccion.contains(id) ? _seleccion.remove(id) : _seleccion.add(id));

  void _abrir(Widget pantalla) =>
      Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => pantalla));

  /// HU-154 C1 · elegir la tarifa, confirmar con cuántos productos y aplicar.
  Future<void> _cambiarTarifa() async {
    final consulta = ref.read(controladorDeInventarioProvider).consulta;
    final tarifa = await showDialog<(String, String, num)>(
      context: context,
      builder: (ctx) => SimpleDialog(
        title: const Text('Nueva tarifa de IVA'),
        children: [
          for (final t in tarifasDeIva)
            SimpleDialogOption(onPressed: () => Navigator.of(ctx).pop(t), child: Text(t.$2)),
        ],
      ),
    );
    if (tarifa == null || !mounted) return;
    final aSeleccion = _seleccion.isNotEmpty;
    final categoria =
        _categorias.where((c) => c.id == consulta.categoriaId).map((c) => c.nombre).firstOrNull ?? '';
    final pregunta = aSeleccion
        ? '¿Cambiar la tarifa de ${_seleccion.length} productos a ${tarifa.$2}? El precio no cambia.'
        : '¿Cambiar la tarifa de toda la categoría $categoria a ${tarifa.$2}? El precio no cambia.';
    final ok = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        content: Text(pregunta),
        actions: [
          TextButton(onPressed: () => Navigator.of(ctx).pop(false), child: const Text('Cancelar')),
          FilledButton(onPressed: () => Navigator.of(ctx).pop(true), child: const Text('Cambiar')),
        ],
      ),
    );
    if (ok != true || !mounted) return;
    final messenger = ScaffoldMessenger.of(context);
    try {
      final n = await ref.read(repositorioDeInventarioProvider).cambiarTarifaEnBloque(
            productoIds: aSeleccion ? _seleccion.toList() : const [],
            categoriaId: aSeleccion ? null : consulta.categoriaId,
            tarifaIva: tarifa.$1,
          );
      if (!mounted) return;
      setState(_seleccion.clear);
      messenger.showSnackBar(SnackBar(content: Text('Tarifa cambiada a $n productos')));
      _controlador.recargar();
    } on ErrorDeApi catch (e) {
      messenger.showSnackBar(SnackBar(content: Text(e.mensaje)));
    }
  }

  /// R12 · crea el producto en el backend; si lo rechaza, el mensaje queda en el diálogo.
  Future<void> _nuevoProducto() async {
    final repo = ref.read(repositorioDeInventarioProvider);
    final unidades = await repo.unidades().catchError((Object _) => const <OpcionDeFicha>[]);
    if (!mounted) return;
    final creado = await showDialog<(String, String)>(
      context: context,
      builder: (_) => _DialogoNuevoProducto(
          categorias: _categorias, unidades: unidades, crear: repo.crearProducto),
    );
    if (creado == null || !mounted) return;
    _controlador.recargar();
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(
      content: Text('Producto «${creado.$2}» creado.'),
      action: SnackBarAction(label: 'Abrir ficha', onPressed: () => _abrirFicha(creado.$1)),
    ));
  }

  // ---- composición ----

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeInventarioProvider);
    final puedeImportar = ref.watch(puedeImportarProvider);
    return LayoutBuilder(builder: (context, c) {
      final web = c.maxWidth >= kBreakpointEscritorio;
      return Scaffold(
        backgroundColor: RegentaColors.paper,
        appBar: _Barra(
          titulo: widget.titulo,
          puedeImportar: puedeImportar,
          onOpcion: (o) => switch (o) {
            'unidades' => _abrir(const PantallaUnidades()),
            'fracciones' => _abrir(const PantallaTablasDeFracciones()),
            'equivalencias' => _abrir(const PantallaEquivalencias()),
            'importar' => _abrir(const PantallaImportacion()),
            _ => null,
          },
        ),
        floatingActionButton: web
            ? null
            : FloatingActionButton.extended(
                key: const Key('inventario-nuevo'),
                onPressed: _nuevoProducto,
                backgroundColor: RegentaColors.accent,
                foregroundColor: RegentaColors.surface,
                icon: const Icon(Icons.add),
                label: const Text('Nuevo producto'),
              ),
        bottomNavigationBar: web ? null : _BottomNav(onNavegar: widget.onNavegar),
        body: web ? _web(estado) : _movil(estado),
      );
    });
  }

  Widget _barraDeHerramientas(EstadoDeInventario estado, {required bool web}) => Row(children: [
        Expanded(
          child: SizedBox(
            height: 36,
            child: TextField(
              key: const Key('inventario-buscar'),
              controller: _buscador,
              focusNode: _foco,
              onChanged: _controlador.cambiarTermino,
              style: RegentaType.cuerpo.copyWith(fontSize: 13.5, color: RegentaColors.ink),
              decoration: InputDecoration(
                isDense: true,
                filled: true,
                fillColor: RegentaColors.surface,
                prefixIcon: const Icon(Icons.search, size: 16, color: RegentaColors.faint),
                hintText: 'Nombre, SKU o código de barras…',
                hintStyle: RegentaType.cuerpo.copyWith(fontSize: 13.5, color: RegentaColors.faint),
                contentPadding: const EdgeInsets.symmetric(vertical: 9),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(5),
                  borderSide: const BorderSide(color: RegentaColors.line2),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(5),
                  borderSide: const BorderSide(color: RegentaColors.line2),
                ),
              ),
            ),
          ),
        ),
        const SizedBox(width: 10),
        Semantics(
          button: true,
          label: 'Escanear código',
          child: InkWell(
            key: const Key('inventario-escanear'),
            onTap: _escanear,
            borderRadius: BorderRadius.circular(4),
            child: Container(
              width: 36,
              height: 36,
              decoration: BoxDecoration(
                color: RegentaColors.surface,
                border: Border.all(color: RegentaColors.line2),
                borderRadius: BorderRadius.circular(4),
              ),
              child: const Icon(Icons.qr_code_scanner, size: 18, color: RegentaColors.ink),
            ),
          ),
        ),
        const SizedBox(width: 10),
        _BotonFiltros(
          consulta: estado.consulta,
          soloIcono: !web,
          onCambio: _controlador.filtrar,
        ),
        if (web) ...[
          const SizedBox(width: 10),
          SizedBox(
            height: 36,
            child: FilledButton.icon(
              key: const Key('inventario-nuevo'),
              onPressed: _nuevoProducto,
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.accent,
                padding: const EdgeInsets.symmetric(horizontal: 16),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
                textStyle: RegentaType.cuerpo.copyWith(fontSize: 13, fontWeight: FontWeight.w600),
              ),
              icon: const Icon(Icons.add, size: 16),
              label: const Text('Nuevo producto'),
            ),
          ),
        ],
      ]);

  Widget _chips(EstadoDeInventario estado) {
    final consulta = estado.consulta;
    final puedeCambiarTarifa = _seleccion.isNotEmpty || consulta.categoriaId != null;
    return Row(children: [
      Expanded(
        child: SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(children: [
            _Chip(
              texto: 'Todas las categorías',
              activo: consulta.categoriaId == null && !consulta.soloBajoMinimo,
              onTap: () => _controlador
                  .filtrar(consulta.copiar(categoriaId: null, soloBajoMinimo: false)),
            ),
            for (final c in _categorias) ...[
              const SizedBox(width: 7),
              _Chip(
                texto: c.nombre,
                activo: consulta.categoriaId == c.id,
                onTap: () => _controlador.filtrar(consulta.copiar(
                    categoriaId: consulta.categoriaId == c.id ? null : c.id)),
              ),
            ],
            const SizedBox(width: 7),
            _Chip(
              texto: 'Bajo mínimo · ${estado.bajoMinimo}',
              activo: consulta.soloBajoMinimo,
              tono: _TonoChip.critico,
              onTap: () => _controlador
                  .filtrar(consulta.copiar(soloBajoMinimo: !consulta.soloBajoMinimo)),
            ),
          ]),
        ),
      ),
      if (puedeCambiarTarifa) ...[
        const SizedBox(width: 10),
        TextButton.icon(
          key: const Key('cambiar-tarifa'),
          onPressed: _cambiarTarifa,
          icon: const Icon(Icons.percent, size: 16),
          label: Text(_seleccion.isEmpty
              ? 'Cambiar tarifa de la categoría'
              : 'Cambiar tarifa (${_seleccion.length})'),
        ),
      ],
    ]);
  }

  Widget _web(EstadoDeInventario estado) => Padding(
        padding: const EdgeInsets.symmetric(horizontal: 22, vertical: 20),
        child: Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
              _barraDeHerramientas(estado, web: true),
              const SizedBox(height: 12),
              _chips(estado),
              const SizedBox(height: 14),
              Expanded(
                child: Container(
                  clipBehavior: Clip.antiAlias,
                  decoration: BoxDecoration(
                    color: RegentaColors.surface,
                    border: Border.all(color: RegentaColors.line),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Column(children: [
                    const _EncabezadoDeTabla(),
                    Expanded(child: _cuerpo(estado, web: true)),
                  ]),
                ),
              ),
            ]),
          ),
          const SizedBox(width: 16),
          SizedBox(
            width: 308,
            child: SingleChildScrollView(
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                _PanelValorizado(estado: estado),
                const SizedBox(height: 14),
                _PanelBodegas(
                  elegida: _elegida,
                  existencias: _existencias,
                  onFicha: _elegida == null ? null : () => _abrirFicha(_elegida!.id),
                ),
                if (estado.bajoMinimo > 0) ...[
                  const SizedBox(height: 14),
                  _PanelBajoMinimo(
                    cuantos: estado.bajoMinimo,
                    onVer: () => _controlador
                        .filtrar(estado.consulta.copiar(soloBajoMinimo: true)),
                  ),
                ],
              ]),
            ),
          ),
        ]),
      );

  Widget _movil(EstadoDeInventario estado) => Column(children: [
        Container(
          color: RegentaColors.surface,
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          child: Column(children: [
            _barraDeHerramientas(estado, web: false),
            const SizedBox(height: 10),
            _chips(estado),
          ]),
        ),
        Expanded(child: _cuerpo(estado, web: false)),
      ]);

  Widget _cuerpo(EstadoDeInventario estado, {required bool web}) {
    switch (estado.fase) {
      case FaseDeInventario.cargando:
        return const Center(child: CircularProgressIndicator());
      case FaseDeInventario.error:
        return _Mensaje(
          texto: estado.mensajeError ?? 'Algo salió mal.',
          accion: 'Reintentar',
          onAccion: _controlador.recargar,
        );
      case FaseDeInventario.sinResultados:
        final q = estado.consulta.q;
        return _Mensaje(
          texto: q == null || q.isEmpty
              ? (_hayFiltros(estado.consulta)
                  ? 'No hay productos con estos filtros.'
                  : 'No hay productos todavía.')
              : 'No hay productos que coincidan con «$q».',
        );
      case FaseDeInventario.conDatos:
        final n = estado.filas.length + (estado.hayMas ? 1 : 0);
        return ListView.builder(
          key: const Key('inventario-lista'),
          controller: _scroll,
          padding: web ? EdgeInsets.zero : const EdgeInsets.symmetric(horizontal: 14),
          itemCount: n,
          itemBuilder: (context, i) {
            if (i >= estado.filas.length) return const _PieCargando();
            final f = estado.filas[i];
            final seleccionada = _seleccion.contains(f.id);
            return web
                ? _FilaWeb(
                    fila: f,
                    elegida: _elegida?.id == f.id,
                    seleccionada: seleccionada,
                    onTap: () => _elegir(f),
                    onSeleccion: () => _alternarSeleccion(f.id),
                    onFicha: () => _abrirFicha(f.id),
                    onVender: () => _agregarAVenta(f),
                  )
                : _FilaMovil(
                    fila: f,
                    seleccionada: seleccionada,
                    onTap: () => _detalleMovil(f),
                    onSeleccion: () => _alternarSeleccion(f.id),
                  );
          },
        );
    }
  }

  bool _hayFiltros(ConsultaDeInventario c) =>
      c.categoriaId != null ||
      c.soloBajoMinimo ||
      c.tarifaPorRevisar ||
      c.existenciaNegativa ||
      c.estado != 'ACTIVOS';

  /// R9 · en móvil, tocar un producto abre su detalle.
  void _detalleMovil(FilaDeInventario f) {
    showModalBottomSheet<void>(
      context: context,
      shape: const RoundedRectangleBorder(
          borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (ctx) => Padding(
        padding: const EdgeInsets.all(20),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(children: [
              Expanded(
                child: Text(f.nombre,
                    style: RegentaType.seccion.copyWith(fontSize: 18, color: RegentaColors.ink)),
              ),
              _Estado(nivel: f.nivel),
            ]),
            const SizedBox(height: 12),
            Text('SKU: ${f.sku}', style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
            const SizedBox(height: 4),
            Text('Categoría: ${f.categoria.isEmpty ? 'General' : f.categoria}',
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
            const SizedBox(height: 4),
            Text('Existencia: ${formatearCantidad(f.existencia)} · mínimo ${formatearCantidad(f.stockMinimo)}',
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
            const SizedBox(height: 16),
            Text(formatearPesos(f.precio),
                style: RegentaType.dinero.copyWith(fontSize: 22, color: RegentaColors.accent)),
            const SizedBox(height: 20),
            OutlinedButton.icon(
              key: const Key('inventario-editar-ficha'),
              icon: const Icon(Icons.edit_outlined, size: 18),
              label: const Text('Ver o editar ficha'),
              onPressed: () {
                Navigator.of(ctx).pop();
                _abrirFicha(f.id);
              },
            ),
            const SizedBox(height: 8),
            Row(children: [
              Expanded(
                child: OutlinedButton(
                    onPressed: () => Navigator.of(ctx).pop(), child: const Text('Cerrar')),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: FilledButton.icon(
                  style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
                  icon: const Icon(Icons.shopping_cart, size: 18),
                  label: const Text('Agregar a venta'),
                  onPressed: () {
                    Navigator.of(ctx).pop();
                    _agregarAVenta(f);
                  },
                ),
              ),
            ]),
          ],
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Barra superior y menús
// ---------------------------------------------------------------------------

class _Barra extends StatelessWidget implements PreferredSizeWidget {
  const _Barra({required this.titulo, required this.puedeImportar, required this.onOpcion});

  final String titulo;
  final bool puedeImportar;
  final void Function(String opcion) onOpcion;

  @override
  Size get preferredSize => const Size.fromHeight(56);

  @override
  Widget build(BuildContext context) {
    return AppBar(
      backgroundColor: RegentaColors.surface,
      surfaceTintColor: RegentaColors.surface,
      elevation: 0,
      shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
      titleSpacing: 22,
      title: Row(children: [
        Text('Regenta', style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
        const Padding(
          padding: EdgeInsets.symmetric(horizontal: 10),
          child: Icon(Icons.chevron_right, size: 14, color: RegentaColors.faint),
        ),
        Text(titulo,
            style: RegentaType.seccion.copyWith(
                fontSize: 17, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
      ]),
      actions: [
        // R10 · las demás opciones del inventario, en un solo menú.
        PopupMenuButton<String>(
          key: const Key('inventario-mas'),
          tooltip: 'Más opciones',
          icon: const Icon(Icons.more_vert, color: RegentaColors.ink2),
          onSelected: onOpcion,
          itemBuilder: (_) => [
            _opcion('unidades', Icons.straighten, 'Unidades de medida'),
            _opcion('fracciones', Icons.pie_chart_outline, 'Tablas de fracciones'),
            _opcion('equivalencias', Icons.call_split, 'Equivalencias de detallado'),
            if (puedeImportar) _opcion('importar', Icons.upload_file, 'Importar desde Excel'),
          ],
        ),
        const SizedBox(width: 14),
      ],
    );
  }

  static PopupMenuItem<String> _opcion(String valor, IconData icono, String texto) =>
      PopupMenuItem<String>(
        value: valor,
        child: Row(children: [
          Icon(icono, size: 18, color: RegentaColors.ink2),
          const SizedBox(width: 10),
          Flexible(child: Text(texto, overflow: TextOverflow.ellipsis)),
        ]),
      );
}

/// R8 · tarifa por revisar, existencia negativa e inactivos.
class _BotonFiltros extends StatelessWidget {
  const _BotonFiltros({required this.consulta, required this.soloIcono, required this.onCambio});

  final ConsultaDeInventario consulta;
  final bool soloIcono;
  final ValueChanged<ConsultaDeInventario> onCambio;

  int get _activos => [
        consulta.tarifaPorRevisar,
        consulta.existenciaNegativa,
        consulta.estado == 'INACTIVOS',
      ].where((a) => a).length;

  @override
  Widget build(BuildContext context) {
    final etiqueta = _activos == 0 ? 'Filtros' : 'Filtros · $_activos';
    return PopupMenuButton<String>(
      key: const Key('inventario-filtros'),
      tooltip: 'Filtros',
      onSelected: (f) => onCambio(switch (f) {
        'revisar' => consulta.copiar(tarifaPorRevisar: !consulta.tarifaPorRevisar),
        'negativa' => consulta.copiar(existenciaNegativa: !consulta.existenciaNegativa),
        _ => consulta.copiar(estado: consulta.estado == 'INACTIVOS' ? 'ACTIVOS' : 'INACTIVOS'),
      }),
      itemBuilder: (_) => [
        CheckedPopupMenuItem<String>(
          key: const Key('filtro-por-revisar'),
          value: 'revisar',
          checked: consulta.tarifaPorRevisar,
          child: const Text('Tarifa por revisar'),
        ),
        CheckedPopupMenuItem<String>(
          key: const Key('filtro-negativa'),
          value: 'negativa',
          checked: consulta.existenciaNegativa,
          child: const Text('Existencia negativa'),
        ),
        CheckedPopupMenuItem<String>(
          key: const Key('filtro-inactivos'),
          value: 'inactivos',
          checked: consulta.estado == 'INACTIVOS',
          child: const Text('Inactivos'),
        ),
      ],
      child: Container(
        height: 36,
        padding: EdgeInsets.symmetric(horizontal: soloIcono ? 9 : 16),
        decoration: BoxDecoration(
          color: _activos > 0 ? RegentaColors.accentSoft : RegentaColors.surface,
          border: Border.all(color: RegentaColors.line2),
          borderRadius: BorderRadius.circular(4),
        ),
        child: Row(mainAxisSize: MainAxisSize.min, children: [
          const Icon(Icons.filter_alt_outlined, size: 16, color: RegentaColors.ink),
          if (!soloIcono) ...[
            const SizedBox(width: 8),
            Text(etiqueta,
                style: RegentaType.cuerpo.copyWith(
                    fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
          ],
        ]),
      ),
    );
  }
}

enum _TonoChip { normal, critico }

class _Chip extends StatelessWidget {
  const _Chip({
    required this.texto,
    required this.activo,
    required this.onTap,
    this.tono = _TonoChip.normal,
  });

  final String texto;
  final bool activo;
  final VoidCallback onTap;
  final _TonoChip tono;

  @override
  Widget build(BuildContext context) {
    final critico = tono == _TonoChip.critico;
    final fondo = activo
        ? (critico ? RegentaColors.crit : RegentaColors.accent)
        : (critico ? RegentaColors.critSoft : RegentaColors.sunken);
    final color = activo
        ? RegentaColors.surface
        : (critico ? RegentaColors.crit : RegentaColors.ink2);
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(3),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(3)),
        child: Text(
          texto.toUpperCase(),
          style: RegentaType.etiqueta.copyWith(
              fontSize: 9.5, fontWeight: FontWeight.w600, letterSpacing: .5, color: color),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Tabla (web) y filas (móvil)
// ---------------------------------------------------------------------------

/// Anchos de columna del mockup (`InventarioWeb.html`).
abstract final class _Col {
  static const casilla = 30.0;
  static const sku = 110.0;
  static const categoria = 120.0;
  static const stock = 64.0;
  static const minimo = 52.0;
  static const precio = 100.0;
  static const estado = 96.0;
  static const menu = 24.0;
  static const espacio = 12.0;
}

TextStyle get _estiloEncabezado => RegentaType.etiqueta.copyWith(
    fontSize: 9.5, fontWeight: FontWeight.w600, letterSpacing: .76, color: RegentaColors.muted);

class _EncabezadoDeTabla extends StatelessWidget {
  const _EncabezadoDeTabla();

  @override
  Widget build(BuildContext context) {
    Widget celda(String t, double? ancho, {bool derecha = false}) {
      final texto = Text(t.toUpperCase(),
          textAlign: derecha ? TextAlign.right : TextAlign.left, style: _estiloEncabezado);
      return ancho == null ? Expanded(flex: 3, child: texto) : SizedBox(width: ancho, child: texto);
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: const BoxDecoration(
        color: RegentaColors.sunken,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(children: [
        const SizedBox(width: _Col.casilla),
        const SizedBox(width: _Col.espacio),
        celda('SKU', _Col.sku),
        const SizedBox(width: _Col.espacio),
        celda('Producto', null),
        const SizedBox(width: _Col.espacio),
        celda('Categoría', _Col.categoria),
        const SizedBox(width: _Col.espacio),
        celda('Stock', _Col.stock, derecha: true),
        const SizedBox(width: _Col.espacio),
        celda('Mínimo', _Col.minimo, derecha: true),
        const SizedBox(width: _Col.espacio),
        celda('Precio', _Col.precio, derecha: true),
        const SizedBox(width: _Col.espacio),
        celda('Estado', _Col.estado, derecha: true),
        const SizedBox(width: _Col.espacio),
        const SizedBox(width: _Col.menu),
      ]),
    );
  }
}

Color _colorDeStock(String nivel) => switch (nivel) {
      'CERO' => RegentaColors.crit,
      'BAJO' => RegentaColors.warn,
      _ => RegentaColors.ok,
    };

class _FilaWeb extends StatelessWidget {
  const _FilaWeb({
    required this.fila,
    required this.elegida,
    required this.seleccionada,
    required this.onTap,
    required this.onSeleccion,
    required this.onFicha,
    required this.onVender,
  });

  final FilaDeInventario fila;
  final bool elegida;
  final bool seleccionada;
  final VoidCallback onTap;
  final VoidCallback onSeleccion;
  final VoidCallback onFicha;
  final VoidCallback onVender;

  @override
  Widget build(BuildContext context) {
    const sep = SizedBox(width: _Col.espacio);
    return Material(
      color: elegida ? RegentaColors.accentSoft : RegentaColors.surface,
      child: InkWell(
        key: Key('fila-${fila.id}'),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 9),
          decoration: const BoxDecoration(
              border: Border(bottom: BorderSide(color: RegentaColors.line))),
          child: Row(children: [
            SizedBox(
              width: _Col.casilla,
              child: Checkbox(
                key: Key('seleccionar-${fila.id}'),
                value: seleccionada,
                visualDensity: VisualDensity.compact,
                onChanged: (_) => onSeleccion(),
              ),
            ),
            sep,
            SizedBox(
              width: _Col.sku,
              child: Text(fila.sku,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.codigo.copyWith(fontSize: 11.5, color: RegentaColors.ink)),
            ),
            sep,
            Expanded(
              flex: 3,
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(fila.nombre,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.cuerpo.copyWith(
                        fontSize: 13.5, fontWeight: FontWeight.w500, color: RegentaColors.ink)),
                if (fila.tarifaPorRevisar || !fila.activo)
                  Text(
                    [
                      if (fila.tarifaPorRevisar) 'Tarifa por revisar',
                      if (!fila.activo) 'Inactivo',
                    ].join(' · '),
                    style: RegentaType.etiqueta.copyWith(
                        fontSize: 9.5,
                        color: fila.activo ? RegentaColors.warn : RegentaColors.crit),
                  ),
              ]),
            ),
            sep,
            SizedBox(
              width: _Col.categoria,
              child: Text(fila.categoria,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2)),
            ),
            sep,
            SizedBox(
              width: _Col.stock,
              child: Text(formatearCantidad(fila.existencia),
                  textAlign: TextAlign.right,
                  style: RegentaType.codigo.copyWith(
                      fontSize: 12.5, fontWeight: FontWeight.w600, color: _colorDeStock(fila.nivel))),
            ),
            sep,
            SizedBox(
              width: _Col.minimo,
              child: Text(formatearCantidad(fila.stockMinimo),
                  textAlign: TextAlign.right,
                  style: RegentaType.codigo.copyWith(fontSize: 12, color: RegentaColors.faint)),
            ),
            sep,
            SizedBox(
              width: _Col.precio,
              child: Text(formatearPesos(fila.precio),
                  textAlign: TextAlign.right,
                  style: RegentaType.dinero.copyWith(
                      fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
            ),
            sep,
            SizedBox(
              width: _Col.estado,
              child: Align(alignment: Alignment.centerRight, child: _Estado(nivel: fila.nivel)),
            ),
            sep,
            SizedBox(
              width: _Col.menu,
              child: PopupMenuButton<String>(
                key: Key('fila-menu-${fila.id}'),
                tooltip: 'Opciones',
                padding: EdgeInsets.zero,
                iconSize: 18,
                icon: const Icon(Icons.more_vert, color: RegentaColors.faint),
                onSelected: (o) => o == 'ficha' ? onFicha() : onVender(),
                itemBuilder: (_) => const [
                  PopupMenuItem(value: 'ficha', child: Text('Ver o editar ficha')),
                  PopupMenuItem(value: 'vender', child: Text('Agregar a venta')),
                ],
              ),
            ),
          ]),
        ),
      ),
    );
  }
}

class _FilaMovil extends StatelessWidget {
  const _FilaMovil({
    required this.fila,
    required this.seleccionada,
    required this.onTap,
    required this.onSeleccion,
  });

  final FilaDeInventario fila;
  final bool seleccionada;
  final VoidCallback onTap;
  final VoidCallback onSeleccion;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      key: Key('fila-${fila.id}'),
      onTap: onTap,
      onLongPress: onSeleccion,
      child: Container(
        constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
        padding: const EdgeInsets.symmetric(vertical: 11),
        decoration: BoxDecoration(
          color: seleccionada ? RegentaColors.accentSoft : null,
          border: const Border(bottom: BorderSide(color: RegentaColors.line)),
        ),
        child: Row(children: [
          Expanded(
            child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
              Text(fila.nombre,
                  style: RegentaType.cuerpo.copyWith(
                      fontSize: 14, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
              const SizedBox(height: 2),
              Text('${fila.sku} · ${fila.categoria}',
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
            ]),
          ),
          const SizedBox(width: 10),
          Column(crossAxisAlignment: CrossAxisAlignment.end, children: [
            Text(formatearPesos(fila.precio),
                style: RegentaType.dinero.copyWith(
                    fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
            const SizedBox(height: 3),
            Text(formatearCantidad(fila.existencia),
                style: RegentaType.codigo.copyWith(
                    fontSize: 12.5, fontWeight: FontWeight.w600, color: _colorDeStock(fila.nivel))),
          ]),
        ]),
      ),
    );
  }
}

/// R7 · EN STOCK, BAJO MÍNIMO o AGOTADO.
class _Estado extends StatelessWidget {
  const _Estado({required this.nivel});

  final String nivel;

  @override
  Widget build(BuildContext context) {
    final (texto, color, fondo) = switch (nivel) {
      'CERO' => ('Agotado', RegentaColors.crit, RegentaColors.critSoft),
      'BAJO' => ('Bajo mínimo', RegentaColors.warn, RegentaColors.warnSoft),
      _ => ('En stock', RegentaColors.ok, RegentaColors.okSoft),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(3)),
      child: Text(texto.toUpperCase(),
          style: RegentaType.etiqueta.copyWith(
              fontSize: 9.5, fontWeight: FontWeight.w600, letterSpacing: .5, color: color)),
    );
  }
}

class _PieCargando extends StatelessWidget {
  const _PieCargando();

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.symmetric(vertical: 14),
        child: Row(mainAxisAlignment: MainAxisAlignment.center, children: [
          const SizedBox(
              width: 14, height: 14, child: CircularProgressIndicator(strokeWidth: 2)),
          const SizedBox(width: 10),
          Text('Cargando más…',
              style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.muted)),
        ]),
      );
}

// ---------------------------------------------------------------------------
// Panel lateral (web)
// ---------------------------------------------------------------------------

BoxDecoration get _tarjeta => BoxDecoration(
      color: RegentaColors.surface,
      border: Border.all(color: RegentaColors.line),
      borderRadius: BorderRadius.circular(6),
    );

class _PanelValorizado extends StatelessWidget {
  const _PanelValorizado({required this.estado});

  final EstadoDeInventario estado;

  @override
  Widget build(BuildContext context) {
    // R11 · sin permiso de costo, el valor a precio de venta.
    final alCosto = estado.valorCosto != null;
    return Container(
      key: const Key('panel-valorizado'),
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 13),
      decoration: _tarjeta,
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('INVENTARIO VALORIZADO',
            style: RegentaType.etiqueta.copyWith(
                fontSize: 9.5, fontWeight: FontWeight.w600, letterSpacing: .86, color: RegentaColors.muted)),
        const SizedBox(height: 6),
        Text(formatearPesos(alCosto ? estado.valorCosto! : estado.valorPrecio),
            style: RegentaType.dinero.copyWith(
                fontSize: 23, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
        const SizedBox(height: 3),
        Text(alCosto ? 'al costo promedio' : 'a precio de venta',
            style: RegentaType.cuerpo.copyWith(fontSize: 11, color: RegentaColors.muted)),
        const SizedBox(height: 8),
        Text('${estado.totalFilas} productos',
            style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
      ]),
    );
  }
}

class _PanelBodegas extends StatelessWidget {
  const _PanelBodegas({required this.elegida, required this.existencias, required this.onFicha});

  final FilaDeInventario? elegida;
  final List<(String, num)>? existencias;
  final VoidCallback? onFicha;

  @override
  Widget build(BuildContext context) {
    final e = elegida;
    return Container(
      key: const Key('panel-bodegas'),
      padding: const EdgeInsets.all(16),
      decoration: _tarjeta,
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Text('Existencias por bodega',
            style: RegentaType.cuerpo.copyWith(
                fontSize: 14.5, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
        const SizedBox(height: 4),
        if (e == null)
          Text('Elige un producto para ver su existencia en cada bodega.',
              style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.muted))
        else ...[
          Text(e.nombre,
              overflow: TextOverflow.ellipsis,
              style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
          const SizedBox(height: 8),
          if (existencias == null)
            const Padding(
              padding: EdgeInsets.symmetric(vertical: 12),
              child: Center(
                  child: SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))),
            )
          else if (existencias!.isEmpty)
            Text('Sin existencias registradas.',
                style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.muted))
          else
            for (final (bodega, cantidad) in existencias!)
              Container(
                padding: const EdgeInsets.symmetric(vertical: 9),
                decoration: const BoxDecoration(
                    border: Border(bottom: BorderSide(color: RegentaColors.line))),
                child: Row(children: [
                  Expanded(
                    child: Text(bodega,
                        style: RegentaType.cuerpo.copyWith(
                            fontSize: 13, fontWeight: FontWeight.w500, color: RegentaColors.ink)),
                  ),
                  Text(formatearCantidad(cantidad),
                      style: RegentaType.codigo.copyWith(
                          fontSize: 12.5, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
                ]),
              ),
          const SizedBox(height: 11),
          OutlinedButton.icon(
            key: const Key('panel-ficha'),
            onPressed: onFicha,
            icon: const Icon(Icons.edit_outlined, size: 16),
            label: const Text('Ver o editar ficha'),
          ),
        ],
      ]),
    );
  }
}

class _PanelBajoMinimo extends StatelessWidget {
  const _PanelBajoMinimo({required this.cuantos, required this.onVer});

  final int cuantos;
  final VoidCallback onVer;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: RegentaColors.critSoft, borderRadius: BorderRadius.circular(6)),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Row(children: [
          const Icon(Icons.warning_amber_rounded, size: 18, color: RegentaColors.crit),
          const SizedBox(width: 9),
          Text('$cuantos bajo mínimo',
              style: RegentaType.cuerpo.copyWith(
                  fontSize: 14, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
        ]),
        const SizedBox(height: 8),
        Text('Productos en su stock mínimo o por debajo.',
            style: RegentaType.cuerpo.copyWith(fontSize: 12.5, height: 1.5, color: RegentaColors.ink2)),
        const SizedBox(height: 11),
        SizedBox(
          height: 34,
          child: OutlinedButton(
            onPressed: onVer,
            style: OutlinedButton.styleFrom(
              backgroundColor: RegentaColors.surface,
              side: const BorderSide(color: RegentaColors.line2),
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
            ),
            child: Text('Ver los de bajo mínimo',
                style: RegentaType.cuerpo.copyWith(
                    fontSize: 12.5, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
          ),
        ),
      ]),
    );
  }
}

// ---------------------------------------------------------------------------
// Nuevo producto (R12)
// ---------------------------------------------------------------------------

class _DialogoNuevoProducto extends StatefulWidget {
  const _DialogoNuevoProducto({required this.categorias, required this.unidades, required this.crear});

  final List<OpcionDeFicha> categorias;
  final List<OpcionDeFicha> unidades;
  final Future<String> Function(NuevoProducto) crear;

  @override
  State<_DialogoNuevoProducto> createState() => _DialogoNuevoProductoState();
}

class _DialogoNuevoProductoState extends State<_DialogoNuevoProducto> {
  final _sku = TextEditingController();
  final _codigo = TextEditingController();
  final _nombre = TextEditingController();
  final _precio = TextEditingController();
  final _minimo = TextEditingController(text: '0');
  String? _categoriaId;
  String? _unidadId;
  String _tarifa = 'IVA_19';
  String? _error;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    _unidadId = widget.unidades.firstOrNull?.id;
  }

  @override
  void dispose() {
    for (final c in [_sku, _codigo, _nombre, _precio, _minimo]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _crear() async {
    final precio = num.tryParse(_precio.text.trim().replaceAll('.', '').replaceAll(',', '.'));
    final minimo = num.tryParse(_minimo.text.trim().replaceAll(',', '.')) ?? 0;
    if (_sku.text.trim().isEmpty ||
        _nombre.text.trim().isEmpty ||
        _categoriaId == null ||
        _unidadId == null ||
        precio == null ||
        precio <= 0) {
      setState(() => _error = 'Completa SKU, nombre, categoría, unidad y un precio mayor que cero.');
      return;
    }
    setState(() {
      _guardando = true;
      _error = null;
    });
    final nombre = _nombre.text.trim();
    try {
      final id = await widget.crear(NuevoProducto(
        sku: _sku.text.trim(),
        codigoBarras: _codigo.text.trim().isEmpty ? null : _codigo.text.trim(),
        nombre: nombre,
        categoriaId: _categoriaId!,
        unidadMedidaId: _unidadId!,
        tarifaIva: _tarifa,
        precioVenta: precio,
        stockMinimo: minimo,
      ));
      if (mounted) Navigator.of(context).pop((id, nombre));
    } on ErrorDeApi catch (e) {
      if (mounted) {
        setState(() {
          _guardando = false;
          _error = e.mensaje;
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    InputDecoration deco(String etiqueta) => InputDecoration(labelText: etiqueta, isDense: true);
    const estilo = TextStyle(color: RegentaColors.ink);
    return AlertDialog(
      title: Text('Nuevo producto', style: RegentaType.seccion.copyWith(color: RegentaColors.ink)),
      content: SizedBox(
        width: 460,
        child: SingleChildScrollView(
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            Row(children: [
              Expanded(
                  child: TextField(
                      key: const Key('nuevo-sku'), controller: _sku, style: estilo, decoration: deco('SKU *'))),
              const SizedBox(width: 12),
              Expanded(
                  child: TextField(
                      key: const Key('nuevo-codigo'),
                      controller: _codigo,
                      style: estilo,
                      decoration: deco('Código de barras'))),
            ]),
            const SizedBox(height: 10),
            TextField(
                key: const Key('nuevo-nombre'), controller: _nombre, style: estilo, decoration: deco('Nombre *')),
            const SizedBox(height: 10),
            DropdownButtonFormField<String>(
              key: const Key('nuevo-categoria'),
              initialValue: _categoriaId,
              isExpanded: true,
              decoration: deco('Categoría *'),
              items: [
                for (final c in widget.categorias) DropdownMenuItem(value: c.id, child: Text(c.nombre)),
              ],
              onChanged: (v) => setState(() => _categoriaId = v),
            ),
            const SizedBox(height: 10),
            Row(children: [
              Expanded(
                child: DropdownButtonFormField<String>(
                  key: const Key('nuevo-unidad'),
                  initialValue: _unidadId,
                  isExpanded: true,
                  decoration: deco('Unidad *'),
                  items: [
                    for (final u in widget.unidades) DropdownMenuItem(value: u.id, child: Text(u.nombre)),
                  ],
                  onChanged: (v) => setState(() => _unidadId = v),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: DropdownButtonFormField<String>(
                  key: const Key('nuevo-tarifa'),
                  initialValue: _tarifa,
                  isExpanded: true,
                  decoration: deco('Tarifa de IVA *'),
                  items: [
                    for (final t in tarifasDeIva) DropdownMenuItem(value: t.$1, child: Text(t.$2)),
                  ],
                  onChanged: (v) => setState(() => _tarifa = v ?? _tarifa),
                ),
              ),
            ]),
            const SizedBox(height: 10),
            Row(children: [
              Expanded(
                child: TextField(
                  key: const Key('nuevo-precio'),
                  controller: _precio,
                  keyboardType: TextInputType.number,
                  style: estilo,
                  decoration: deco('Precio con IVA *'),
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: TextField(
                  key: const Key('nuevo-minimo'),
                  controller: _minimo,
                  keyboardType: TextInputType.number,
                  style: estilo,
                  decoration: deco('Stock mínimo'),
                ),
              ),
            ]),
            const SizedBox(height: 8),
            Text('La existencia inicial se carga después, en la ficha, con «Ajustar existencia».',
                style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted)),
            if (_error != null) ...[
              const SizedBox(height: 10),
              Text(_error!, style: RegentaType.cuerpo.copyWith(color: RegentaColors.crit)),
            ],
          ]),
        ),
      ),
      actions: [
        TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar')),
        FilledButton(
          style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
          onPressed: _guardando ? null : _crear,
          child: const Text('Crear'),
        ),
      ],
    );
  }
}

// ---------------------------------------------------------------------------
// Mensajes y navegación inferior (móvil)
// ---------------------------------------------------------------------------

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
        child: Column(mainAxisSize: MainAxisSize.min, children: [
          Text(texto,
              textAlign: TextAlign.center,
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
          if (accion != null) ...[
            const SizedBox(height: 12),
            OutlinedButton(onPressed: onAccion, child: Text(accion!)),
          ],
        ]),
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
      child: Row(children: [
        for (final (destino, icono) in _items)
          Expanded(
            child: InkWell(
              onTap: () => onNavegar?.call(destino),
              child: Column(mainAxisSize: MainAxisSize.min, children: [
                Icon(icono,
                    size: 21,
                    color: destino == 'inventario' ? RegentaColors.accent : RegentaColors.faint),
                const SizedBox(height: 4),
                Text(
                  destino[0].toUpperCase() + destino.substring(1),
                  style: RegentaType.cuerpo.copyWith(
                    fontSize: 10,
                    fontWeight: FontWeight.w600,
                    color: destino == 'inventario' ? RegentaColors.accent : RegentaColors.faint,
                  ),
                ),
              ]),
            ),
          ),
      ]),
    );
  }
}
