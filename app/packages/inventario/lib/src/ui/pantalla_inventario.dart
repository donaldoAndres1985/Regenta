import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/controlador_de_inventario.dart';
import '../busqueda/proveedores.dart';
import '../datos/bodega_del_negocio.dart';
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
/// En web (R6): encabezado con **Herramientas**, **Cambiar tarifa** y **Nuevo producto**; cuatro
/// tarjetas de resumen (R13); y una tarjeta con buscador, filtros, chips de categoría y la tabla,
/// que se carga de a 50 (R5). En móvil, filas apiladas.
///
/// No hay botón de escanear: el lector de códigos escribe en el mismo buscador y Enter resuelve
/// el código (R2); en móvil la cámara es un ícono dentro del campo (R3). Al resolver un código
/// llama a [onProductoSeleccionado] con el id y el factor de conversión.
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

  /// R13 · las bodegas activas; nulo mientras cargan.
  List<BodegaDelNegocio>? _bodegas;

  @override
  void initState() {
    super.initState();
    _scroll.addListener(_alHacerScroll);
    WidgetsBinding.instance.addPostFrameCallback((_) => _foco.requestFocus());
    final repo = ref.read(repositorioDeInventarioProvider);
    repo.categorias().then((c) {
      if (mounted) setState(() => _categorias = c);
    }).catchError((Object _) {});
    repo.bodegas().then((b) {
      if (mounted) setState(() => _bodegas = b.where((x) => x.activa).toList());
    }).catchError((Object _) {
      if (mounted) setState(() => _bodegas = const []);
    });
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

  /// R3 · la cámara (o el código a mano) escribe en el buscador y se resuelve como R2.
  Future<void> _escanear() async {
    final escaner = ref.read(escanerProvider);
    String? codigo;
    var tieneCamara = await escaner.disponible;
    if (tieneCamara) {
      try {
        if (!mounted) return;
        codigo = await escaner.escanearUnCodigo(context);
      } on EscaneoNoDisponible {
        tieneCamara = false; // la cámara falló: cae al ingreso manual
      }
    }
    if (!tieneCamara) {
      if (!mounted) return;
      codigo = await pedirCodigoAMano(context);
    }
    if (codigo == null || codigo.isEmpty || !mounted) return;
    _buscador.text = codigo;
    await _alEnviar(codigo);
  }

  /// R2 · Enter (o el lector) en el buscador: si es un código, filtra a su SKU y entrega el
  /// producto con su factor; si no, busca el texto de una vez.
  Future<void> _alEnviar(String texto) async {
    final t = texto.trim();
    _controlador.buscarYa(t);
    if (t.isEmpty) return;
    try {
      final resuelto = await ref.read(repositorioDeInventarioProvider).resolverCodigo(t);
      if (!mounted) return;
      _buscador.text = resuelto.sku;
      _controlador.buscarYa(resuelto.sku);
      widget.onProductoSeleccionado(resuelto.productoId, resuelto.factor);
    } on ErrorDeApi {
      // No es un código: queda como búsqueda por texto.
    }
  }

  // ---- acciones sobre productos ----

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

  void _alternarTodas(List<FilaDeInventario> filas) => setState(() {
        final todas = filas.isNotEmpty && filas.every((f) => _seleccion.contains(f.id));
        todas ? _seleccion.clear() : _seleccion.addAll(filas.map((f) => f.id));
      });

  void _herramienta(String opcion) => switch (opcion) {
        'unidades' => _abrir(const PantallaUnidades()),
        'fracciones' => _abrir(const PantallaTablasDeFracciones()),
        'equivalencias' => _abrir(const PantallaEquivalencias()),
        'importar' => _abrir(const PantallaImportacion()),
        _ => null,
      };

  void _abrir(Widget pantalla) =>
      Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => pantalla));

  bool _puedeCambiarTarifa(ConsultaDeInventario c) => _seleccion.isNotEmpty || c.categoriaId != null;

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
        appBar: web
            ? _encabezado(estado, puedeImportar)
            : AppBar(
                backgroundColor: RegentaColors.surface,
                surfaceTintColor: RegentaColors.surface,
                elevation: 0,
                shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
                title: Text(widget.titulo,
                    style: RegentaType.seccion.copyWith(
                        fontSize: 17, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
                actions: [
                  _MenuHerramientas(puedeImportar: puedeImportar, compacto: true, onOpcion: _herramienta),
                  const SizedBox(width: 6),
                ],
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
        body: web ? _web(estado, puedeImportar) : _movil(estado),
      );
    });
  }

  // ---- web ----

  Widget _web(EstadoDeInventario estado, bool puedeImportar) => Padding(
        padding: const EdgeInsets.fromLTRB(24, 0, 24, 22),
        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          _tarjetas(estado),
          const SizedBox(height: 18),
          Expanded(
            child: Container(
              clipBehavior: Clip.antiAlias,
              decoration: BoxDecoration(
                color: RegentaColors.surface,
                border: Border.all(color: RegentaColors.line),
                borderRadius: BorderRadius.circular(12),
              ),
              padding: const EdgeInsets.fromLTRB(14, 14, 14, 0),
              child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                Row(children: [
                  Expanded(child: _campoDeBusqueda(web: true)),
                  const SizedBox(width: 10),
                  _BotonFiltros(
                    consulta: estado.consulta,
                    bajoMinimo: estado.bajoMinimo,
                    soloIcono: false,
                    onCambio: _controlador.filtrar,
                  ),
                ]),
                const SizedBox(height: 12),
                _chips(estado.consulta),
                if (_filtrando(estado)) ...[
                  const SizedBox(height: 10),
                  Text('${estado.totalFilas} de ${estado.resumen.productos} productos',
                      style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted)),
                ],
                const SizedBox(height: 14),
                Expanded(
                  child: Container(
                    clipBehavior: Clip.antiAlias,
                    decoration: const BoxDecoration(
                      border: Border(
                        top: BorderSide(color: RegentaColors.line),
                        left: BorderSide(color: RegentaColors.line),
                        right: BorderSide(color: RegentaColors.line),
                      ),
                      borderRadius: BorderRadius.vertical(top: Radius.circular(8)),
                    ),
                    child: Column(children: [
                      _EncabezadoDeTabla(
                        todas: estado.filas.isNotEmpty &&
                            estado.filas.every((f) => _seleccion.contains(f.id)),
                        onTodas: () => _alternarTodas(estado.filas),
                      ),
                      Expanded(child: _cuerpo(estado, web: true)),
                    ]),
                  ),
                ),
              ]),
            ),
          ),
        ]),
      );

  /// Encabezado común de las secciones (design/comportamiento/Encabezado.md), sin ruta de migas.
  PreferredSizeWidget _encabezado(EstadoDeInventario estado, bool puedeImportar) {
    final consulta = estado.consulta;
    return EncabezadoDeSeccion(icono: Icons.inventory_2_outlined, subtitulo: 'Productos, existencias y valor de tu inventario', titulo: widget.titulo, acciones: [
      _MenuHerramientas(puedeImportar: puedeImportar, compacto: false, onOpcion: _herramienta),
      SizedBox(
        height: 38,
        child: OutlinedButton.icon(
          key: const Key('cambiar-tarifa'),
          onPressed: _puedeCambiarTarifa(consulta) ? _cambiarTarifa : null,
          style: _estiloSecundario,
          icon: const Icon(Icons.percent, size: 16),
          label: Text(_seleccion.isEmpty
              ? 'Cambiar tarifa de la categoría'
              : 'Cambiar tarifa (${_seleccion.length})'),
        ),
      ),
      SizedBox(
        height: 38,
        child: FilledButton.icon(
          key: const Key('inventario-nuevo'),
          onPressed: _nuevoProducto,
          style: FilledButton.styleFrom(
            backgroundColor: RegentaColors.accent,
            padding: const EdgeInsets.symmetric(horizontal: 18),
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            textStyle: RegentaType.cuerpo.copyWith(fontSize: 13, fontWeight: FontWeight.w600),
          ),
          icon: const Icon(Icons.add, size: 16),
          label: const Text('Nuevo producto'),
        ),
      ),
    ]);
  }

  /// R13 · valorizado, productos, bodegas y tarifas por revisar, de todo el inventario activo
  /// (el resumen del backend): los filtros de la lista no los cambian.
  Widget _tarjetas(EstadoDeInventario estado) {
    final cargando = estado.fase == FaseDeInventario.cargando && estado.filas.isEmpty;
    String o(String v) => cargando ? '—' : v;
    final resumen = estado.resumen;
    final alCosto = resumen.valorCosto != null;
    final bodegas = _bodegas;
    final porRevisar = estado.consulta.tarifaPorRevisar;
    return IntrinsicHeight(
      child: Row(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Expanded(
          child: _Tarjeta(
            clave: 'tarjeta-valorizado',
            titulo: 'Inventario valorizado',
            valor: o(formatearPesos(alCosto ? resumen.valorCosto! : resumen.valorPrecio)),
            pie: alCosto ? 'Al costo promedio, sin IVA' : 'A precio de venta, con IVA',
            detalle: alCosto && !cargando
                ? 'A precio de venta con IVA: ${formatearPesos(resumen.valorPrecio)}'
                : null,
            tono: _TonoTarjeta.acento,
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: _Tarjeta(
            clave: 'tarjeta-productos',
            titulo: 'Productos',
            valor: o('${resumen.productos}'),
            pie: 'Activos en el inventario',
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: _Tarjeta(
            clave: 'tarjeta-bodegas',
            titulo: 'Bodegas',
            valor: bodegas == null ? '—' : '${bodegas.length}',
            pie: bodegas == null || bodegas.isEmpty ? ' ' : bodegas.map((b) => b.nombre).join(', '),
          ),
        ),
        const SizedBox(width: 14),
        Expanded(
          child: _Tarjeta(
            clave: 'tarjeta-por-revisar',
            titulo: 'Tarifas por revisar',
            valor: o('${resumen.porRevisar}'),
            pie: porRevisar ? 'Filtrando: toca para quitar' : 'Productos sin tarifa confirmada',
            tono: _TonoTarjeta.aviso,
            activa: porRevisar,
            onTap: () => _controlador.filtrar(estado.consulta.copiar(tarifaPorRevisar: !porRevisar)),
          ),
        ),
      ]),
    );
  }

  // ---- buscador y chips (web y móvil) ----

  Widget _campoDeBusqueda({required bool web}) {
    final borde = OutlineInputBorder(
      borderRadius: BorderRadius.circular(8),
      borderSide: const BorderSide(color: RegentaColors.line),
    );
    return SizedBox(
      height: 40,
      child: TextField(
        key: const Key('inventario-buscar'),
        controller: _buscador,
        focusNode: _foco,
        textInputAction: TextInputAction.search,
        onChanged: _controlador.cambiarTermino,
        onSubmitted: _alEnviar,
        style: RegentaType.cuerpo.copyWith(fontSize: 13.5, color: RegentaColors.ink),
        decoration: InputDecoration(
          isDense: true,
          filled: true,
          fillColor: RegentaColors.sunken,
          prefixIcon: const Icon(Icons.search, size: 18, color: RegentaColors.faint),
          suffixIcon: web
              ? null
              : IconButton(
                  key: const Key('inventario-camara'),
                  tooltip: 'Escanear con la cámara',
                  icon: const Icon(Icons.photo_camera_outlined, size: 20, color: RegentaColors.ink2),
                  onPressed: _escanear,
                ),
          hintText: 'Buscar por nombre, SKU o código de barras',
          hintStyle: RegentaType.cuerpo.copyWith(fontSize: 13.5, color: RegentaColors.faint),
          contentPadding: const EdgeInsets.symmetric(vertical: 11),
          border: borde,
          enabledBorder: borde,
        ),
      ),
    );
  }

  static const _chipsVisibles = 8;

  Widget _chips(ConsultaDeInventario consulta) {
    final elegida = consulta.categoriaId;
    final visibles = _categorias.take(_chipsVisibles).toList();
    final resto = _categorias.skip(_chipsVisibles).toList();
    final elegidaEnResto = resto.where((c) => c.id == elegida).firstOrNull;
    if (elegidaEnResto != null) visibles.add(elegidaEnResto);
    void elegir(String? id) => _controlador.filtrar(consulta.copiar(categoriaId: id));
    return Wrap(spacing: 8, runSpacing: 8, children: [
      _Chip(
        clave: 'chip-todas',
        texto: 'Todas',
        activo: elegida == null,
        onTap: () => elegir(null),
      ),
      for (final c in visibles)
        _Chip(
          clave: 'chip-${c.id}',
          texto: c.nombre,
          activo: elegida == c.id,
          onTap: () => elegir(elegida == c.id ? null : c.id),
        ),
      if (resto.isNotEmpty)
        PopupMenuButton<String>(
          key: const Key('chip-mas'),
          tooltip: 'Más categorías',
          onSelected: elegir,
          itemBuilder: (_) => [
            for (final c in resto) PopupMenuItem(value: c.id, child: Text(c.nombre)),
          ],
          child: const _Chip(texto: 'Más categorías', activo: false),
        ),
    ]);
  }

  // ---- móvil ----

  Widget _movil(EstadoDeInventario estado) => Column(children: [
        Container(
          color: RegentaColors.surface,
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
          child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
            Row(children: [
              Expanded(child: _campoDeBusqueda(web: false)),
              const SizedBox(width: 8),
              _BotonFiltros(
                consulta: estado.consulta,
                bajoMinimo: estado.bajoMinimo,
                soloIcono: true,
                onCambio: _controlador.filtrar,
              ),
            ]),
            const SizedBox(height: 10),
            SingleChildScrollView(scrollDirection: Axis.horizontal, child: _chips(estado.consulta)),
            if (_seleccion.isNotEmpty)
              Align(
                alignment: Alignment.centerLeft,
                child: TextButton.icon(
                  key: const Key('cambiar-tarifa'),
                  onPressed: _cambiarTarifa,
                  icon: const Icon(Icons.percent, size: 16),
                  label: Text('Cambiar tarifa (${_seleccion.length})'),
                ),
              ),
          ]),
        ),
        Expanded(child: _cuerpo(estado, web: false)),
      ]);

  // ---- lista ----

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
        final maximo = estado.filas.fold<num>(0, (m, f) => f.existencia > m ? f.existencia : m);
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
                    maximo: maximo,
                    seleccionada: seleccionada,
                    onTap: () => _abrirFicha(f.id),
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

  /// Con búsqueda o filtros la lista no es todo el inventario: se dice cuántos de cuántos.
  bool _filtrando(EstadoDeInventario e) =>
      e.fase == FaseDeInventario.conDatos &&
      ((e.consulta.q ?? '').isNotEmpty || _hayFiltros(e.consulta));

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
            Text('Existencia: ${_cantidad(f.existencia, f.unidad)} · mínimo ${_cantidad(f.stockMinimo, f.unidad)}',
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

/// «3 und», «2,5 gln»: la cantidad con el código de su unidad.
String _cantidad(num n, String unidad) {
  final v = n == n.roundToDouble() ? n.round() : n;
  return '$v ${unidad.isEmpty ? 'und' : unidad.toLowerCase()}';
}

ButtonStyle get _estiloSecundario => OutlinedButton.styleFrom(
      foregroundColor: RegentaColors.ink,
      backgroundColor: RegentaColors.surface,
      side: const BorderSide(color: RegentaColors.line2),
      padding: const EdgeInsets.symmetric(horizontal: 14),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
      textStyle: RegentaType.cuerpo.copyWith(fontSize: 13, fontWeight: FontWeight.w600),
    );

// ---------------------------------------------------------------------------
// Encabezado: Herramientas y tarjetas
// ---------------------------------------------------------------------------

/// R10 · Configuración (unidades, fracciones, equivalencias) y Datos (importar).
class _MenuHerramientas extends StatelessWidget {
  const _MenuHerramientas({required this.puedeImportar, required this.compacto, required this.onOpcion});

  final bool puedeImportar;
  final bool compacto;
  final void Function(String opcion) onOpcion;

  @override
  Widget build(BuildContext context) {
    return PopupMenuButton<String>(
      key: const Key('inventario-herramientas'),
      tooltip: 'Herramientas',
      position: PopupMenuPosition.under,
      onSelected: onOpcion,
      itemBuilder: (_) => [
        _seccion('Configuración'),
        _opcion('unidades', Icons.straighten, 'Unidades de medida'),
        _opcion('fracciones', Icons.pie_chart_outline, 'Tablas de fracciones'),
        _opcion('equivalencias', Icons.call_split, 'Equivalencias de detallado'),
        if (puedeImportar) ...[
          const PopupMenuDivider(),
          _seccion('Datos'),
          _opcion('importar', Icons.upload_file, 'Importar desde Excel'),
        ],
      ],
      icon: compacto ? const Icon(Icons.more_vert, color: RegentaColors.ink2) : null,
      child: compacto
          ? null
          : Container(
              height: 38,
              padding: const EdgeInsets.symmetric(horizontal: 14),
              decoration: BoxDecoration(
                color: RegentaColors.surface,
                border: Border.all(color: RegentaColors.line2),
                borderRadius: BorderRadius.circular(8),
              ),
              child: Row(mainAxisSize: MainAxisSize.min, children: [
                const Icon(Icons.settings_outlined, size: 16, color: RegentaColors.ink),
                const SizedBox(width: 8),
                Text('Herramientas',
                    style: RegentaType.cuerpo.copyWith(
                        fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
                const SizedBox(width: 6),
                const Icon(Icons.expand_more, size: 18, color: RegentaColors.ink2),
              ]),
            ),
    );
  }

  static PopupMenuItem<String> _seccion(String texto) => PopupMenuItem<String>(
        enabled: false,
        height: 28,
        child: Text(texto.toUpperCase(),
            style: RegentaType.etiqueta.copyWith(
                fontSize: 10, fontWeight: FontWeight.w600, letterSpacing: .8, color: RegentaColors.muted)),
      );

  static PopupMenuItem<String> _opcion(String valor, IconData icono, String texto) =>
      PopupMenuItem<String>(
        value: valor,
        child: Row(children: [
          Container(
            width: 28,
            height: 28,
            decoration: BoxDecoration(color: RegentaColors.sunken, borderRadius: BorderRadius.circular(6)),
            child: Icon(icono, size: 16, color: RegentaColors.accent),
          ),
          const SizedBox(width: 12),
          Flexible(child: Text(texto, overflow: TextOverflow.ellipsis)),
        ]),
      );
}

enum _TonoTarjeta { normal, acento, aviso }

class _Tarjeta extends StatelessWidget {
  const _Tarjeta({
    required this.clave,
    required this.titulo,
    required this.valor,
    required this.pie,
    this.detalle,
    this.tono = _TonoTarjeta.normal,
    this.activa = false,
    this.onTap,
  });

  final String clave;
  final String titulo;
  final String valor;
  final String pie;

  /// Una segunda línea bajo el pie (el valorizado a precio de venta junto al de costo).
  final String? detalle;
  final _TonoTarjeta tono;
  final bool activa;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final (fondo, borde, colorTitulo, colorValor, colorPie) = switch (tono) {
      _TonoTarjeta.acento => (
          RegentaColors.accent,
          RegentaColors.accent,
          RegentaColors.surface,
          RegentaColors.surface,
          RegentaColors.surface,
        ),
      _TonoTarjeta.aviso => (
          RegentaColors.warnSoft,
          activa ? RegentaColors.warn : RegentaColors.warnSoft,
          RegentaColors.warn,
          RegentaColors.warn,
          RegentaColors.ink2,
        ),
      _TonoTarjeta.normal => (
          RegentaColors.surface,
          RegentaColors.line,
          RegentaColors.muted,
          RegentaColors.ink,
          RegentaColors.muted,
        ),
    };
    return Material(
      color: fondo,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: borde, width: activa ? 1.5 : 1),
      ),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        key: Key(clave),
        onTap: onTap,
        child: Padding(
          padding: const EdgeInsets.fromLTRB(16, 14, 16, 14),
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(titulo.toUpperCase(),
                style: RegentaType.etiqueta.copyWith(
                    fontSize: 10, fontWeight: FontWeight.w600, letterSpacing: .8, color: colorTitulo)),
            const SizedBox(height: 8),
            Text(valor,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.dinero.copyWith(
                    fontSize: 24, fontWeight: FontWeight.w700, color: colorValor)),
            const SizedBox(height: 6),
            Text(pie,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.cuerpo.copyWith(fontSize: 11.5, color: colorPie)),
            if (detalle != null) ...[
              const SizedBox(height: 2),
              Text(detalle!,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.cuerpo.copyWith(fontSize: 11.5, color: colorPie)),
            ],
          ]),
        ),
      ),
    );
  }
}

// ---------------------------------------------------------------------------
// Filtros y chips
// ---------------------------------------------------------------------------

/// R8 · bajo mínimo, tarifa por revisar, existencia negativa e inactivos.
class _BotonFiltros extends StatelessWidget {
  const _BotonFiltros({
    required this.consulta,
    required this.bajoMinimo,
    required this.soloIcono,
    required this.onCambio,
  });

  final ConsultaDeInventario consulta;
  final int bajoMinimo;
  final bool soloIcono;
  final ValueChanged<ConsultaDeInventario> onCambio;

  int get _activos => [
        consulta.soloBajoMinimo,
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
      position: PopupMenuPosition.under,
      onSelected: (f) => onCambio(switch (f) {
        'bajo' => consulta.copiar(soloBajoMinimo: !consulta.soloBajoMinimo),
        'revisar' => consulta.copiar(tarifaPorRevisar: !consulta.tarifaPorRevisar),
        'negativa' => consulta.copiar(existenciaNegativa: !consulta.existenciaNegativa),
        _ => consulta.copiar(estado: consulta.estado == 'INACTIVOS' ? 'ACTIVOS' : 'INACTIVOS'),
      }),
      itemBuilder: (_) => [
        CheckedPopupMenuItem<String>(
          key: const Key('filtro-bajo-minimo'),
          value: 'bajo',
          checked: consulta.soloBajoMinimo,
          child: Text('Bajo mínimo · $bajoMinimo'),
        ),
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
        height: 40,
        padding: EdgeInsets.symmetric(horizontal: soloIcono ? 10 : 16),
        decoration: BoxDecoration(
          color: _activos > 0 ? RegentaColors.accentSoft : RegentaColors.surface,
          border: Border.all(color: RegentaColors.line2),
          borderRadius: BorderRadius.circular(8),
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

class _Chip extends StatelessWidget {
  const _Chip({this.clave, required this.texto, required this.activo, this.onTap});

  final String? clave;
  final String texto;
  final bool activo;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final cuerpo = Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      decoration: BoxDecoration(
        color: activo ? RegentaColors.accent : RegentaColors.surface,
        border: Border.all(color: activo ? RegentaColors.accent : RegentaColors.line2),
        borderRadius: BorderRadius.circular(999),
      ),
      child: Text(texto,
          style: RegentaType.cuerpo.copyWith(
              fontSize: 12.5,
              fontWeight: activo ? FontWeight.w600 : FontWeight.w500,
              color: activo ? RegentaColors.surface : RegentaColors.ink2)),
    );
    if (onTap == null) return cuerpo;
    return InkWell(
      key: clave == null ? null : Key(clave!),
      onTap: onTap,
      borderRadius: BorderRadius.circular(999),
      child: cuerpo,
    );
  }
}

// ---------------------------------------------------------------------------
// Tabla (web) y filas (móvil)
// ---------------------------------------------------------------------------

abstract final class _Col {
  static const casilla = 36.0;
  static const categoria = 150.0;
  static const stock = 120.0;
  static const minimo = 80.0;
  static const precio = 100.0;
  static const estado = 110.0;
  static const menu = 36.0;
  static const espacio = 12.0;
}

TextStyle get _estiloEncabezado => RegentaType.etiqueta.copyWith(
    fontSize: 10, fontWeight: FontWeight.w600, letterSpacing: .76, color: RegentaColors.muted);

class _EncabezadoDeTabla extends StatelessWidget {
  const _EncabezadoDeTabla({required this.todas, required this.onTodas});

  final bool todas;
  final VoidCallback onTodas;

  @override
  Widget build(BuildContext context) {
    Widget celda(String t, double? ancho, {bool derecha = false}) {
      final texto = Text(t.toUpperCase(),
          textAlign: derecha ? TextAlign.right : TextAlign.left, style: _estiloEncabezado);
      return ancho == null ? Expanded(child: texto) : SizedBox(width: ancho, child: texto);
    }

    const sep = SizedBox(width: _Col.espacio);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 4),
      decoration: const BoxDecoration(
        color: RegentaColors.sunken,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(children: [
        SizedBox(
          width: _Col.casilla,
          child: Checkbox(
            key: const Key('seleccionar-todas'),
            value: todas,
            visualDensity: VisualDensity.compact,
            onChanged: (_) => onTodas(),
          ),
        ),
        sep,
        celda('Producto', null),
        sep,
        celda('Categoría', _Col.categoria),
        sep,
        celda('Stock', _Col.stock),
        sep,
        celda('Mínimo', _Col.minimo, derecha: true),
        sep,
        celda('Precio', _Col.precio, derecha: true),
        sep,
        celda('Estado', _Col.estado),
        sep,
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

class _Etiqueta extends StatelessWidget {
  const _Etiqueta(this.texto, {required this.color, required this.fondo});

  final String texto;
  final Color color;
  final Color fondo;

  @override
  Widget build(BuildContext context) => Container(
        padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 2),
        decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(999)),
        child: Text(texto,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: RegentaType.cuerpo.copyWith(fontSize: 10.5, fontWeight: FontWeight.w600, color: color)),
      );
}

class _FilaWeb extends StatelessWidget {
  const _FilaWeb({
    required this.fila,
    required this.maximo,
    required this.seleccionada,
    required this.onTap,
    required this.onSeleccion,
    required this.onFicha,
    required this.onVender,
  });

  final FilaDeInventario fila;
  final num maximo;
  final bool seleccionada;
  final VoidCallback onTap;
  final VoidCallback onSeleccion;
  final VoidCallback onFicha;
  final VoidCallback onVender;

  @override
  Widget build(BuildContext context) {
    const sep = SizedBox(width: _Col.espacio);
    final color = _colorDeStock(fila.nivel);
    final proporcion = maximo <= 0 ? 0.0 : (fila.existencia / maximo).clamp(0, 1).toDouble();
    return Material(
      color: seleccionada ? RegentaColors.accentSoft : RegentaColors.surface,
      child: InkWell(
        key: Key('fila-${fila.id}'),
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
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
            Expanded(
              child: Row(children: [
                Container(
                  width: 34,
                  height: 34,
                  decoration: BoxDecoration(
                      color: RegentaColors.accentSoft, borderRadius: BorderRadius.circular(8)),
                  child: const Icon(Icons.inventory_2_outlined, size: 17, color: RegentaColors.accent),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Text(fila.nombre,
                        overflow: TextOverflow.ellipsis,
                        style: RegentaType.cuerpo.copyWith(
                            fontSize: 13.5, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
                    const SizedBox(height: 2),
                    Row(children: [
                      Flexible(
                        child: Text(fila.sku,
                            overflow: TextOverflow.ellipsis,
                            style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.muted)),
                      ),
                      if (fila.tarifaPorRevisar) ...[
                        const SizedBox(width: 6),
                        const _Etiqueta('Tarifa por revisar',
                            color: RegentaColors.warn, fondo: RegentaColors.warnSoft),
                      ],
                      if (!fila.activo) ...[
                        const SizedBox(width: 6),
                        const _Etiqueta('Inactivo', color: RegentaColors.crit, fondo: RegentaColors.critSoft),
                      ],
                    ]),
                  ]),
                ),
              ]),
            ),
            sep,
            SizedBox(
              width: _Col.categoria,
              child: Align(
                alignment: Alignment.centerLeft,
                child: fila.categoria.isEmpty
                    ? const SizedBox.shrink()
                    : _Etiqueta(fila.categoria, color: RegentaColors.ink2, fondo: RegentaColors.sunken),
              ),
            ),
            sep,
            SizedBox(
              width: _Col.stock,
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(_cantidad(fila.existencia, fila.unidad),
                    style: RegentaType.cuerpo.copyWith(
                        fontSize: 12.5, fontWeight: FontWeight.w600, color: color)),
                const SizedBox(height: 4),
                ClipRRect(
                  borderRadius: BorderRadius.circular(2),
                  child: SizedBox(
                    width: 80,
                    height: 4,
                    child: LinearProgressIndicator(
                      value: proporcion < .03 && fila.existencia > 0 ? .03 : proporcion,
                      backgroundColor: RegentaColors.line,
                      valueColor: AlwaysStoppedAnimation(color),
                    ),
                  ),
                ),
              ]),
            ),
            sep,
            SizedBox(
              width: _Col.minimo,
              child: Text(_cantidad(fila.stockMinimo, fila.unidad),
                  textAlign: TextAlign.right,
                  style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2)),
            ),
            sep,
            SizedBox(
              width: _Col.precio,
              child: Text(formatearPesos(fila.precio),
                  textAlign: TextAlign.right,
                  style: RegentaType.dinero.copyWith(
                      fontSize: 13, fontWeight: FontWeight.w700, color: RegentaColors.ink)),
            ),
            sep,
            SizedBox(
              width: _Col.estado,
              child: Align(alignment: Alignment.centerLeft, child: _Estado(nivel: fila.nivel)),
            ),
            sep,
            SizedBox(
              width: _Col.menu,
              child: PopupMenuButton<String>(
                key: Key('fila-menu-${fila.id}'),
                tooltip: 'Opciones',
                padding: EdgeInsets.zero,
                iconSize: 18,
                icon: const Icon(Icons.more_vert, color: RegentaColors.ink2),
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
            Text(_cantidad(fila.existencia, fila.unidad),
                style: RegentaType.codigo.copyWith(
                    fontSize: 12.5, fontWeight: FontWeight.w600, color: _colorDeStock(fila.nivel))),
          ]),
        ]),
      ),
    );
  }
}

/// R7 · En stock, Bajo mínimo o Agotado.
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
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(999)),
      child: Row(mainAxisSize: MainAxisSize.min, children: [
        Container(width: 5, height: 5, decoration: BoxDecoration(color: color, shape: BoxShape.circle)),
        const SizedBox(width: 5),
        Flexible(
          child: Text(texto,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: RegentaType.cuerpo.copyWith(fontSize: 11, fontWeight: FontWeight.w600, color: color)),
        ),
      ]),
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
