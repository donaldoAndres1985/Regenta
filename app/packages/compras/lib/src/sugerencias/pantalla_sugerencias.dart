import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';
import 'datos.dart';

/// HU-181 · Qué reponer, agrupado por proveedor (y bodega): cada grupo se vuelve una orden en
/// borrador con un toque (I31). Los productos sin proveedor van al final, con la acción de
/// asignarles uno (C5).
class PantallaSugerencias extends ConsumerStatefulWidget {
  const PantallaSugerencias({super.key, this.onOrdenCreada});

  /// Abre la orden recién creada. Nulo: solo se avisa.
  final ValueChanged<String>? onOrdenCreada;

  @override
  ConsumerState<PantallaSugerencias> createState() => _PantallaSugerenciasState();
}

class _PantallaSugerenciasState extends ConsumerState<PantallaSugerencias> {
  List<GrupoDeSugerenciasVista>? _grupos;
  Map<String, String> _bodegas = const {};
  String? _error;

  /// El grupo o la sugerencia que se está procesando, para no tocar dos veces.
  String? _ocupado;

  RepositorioDeSugerencias get _repo => ref.read(repositorioDeSugerenciasProvider);

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  Future<void> _cargar() async {
    setState(() => _error = null);
    try {
      final grupos = await _repo.listar();
      final bodegas = _bodegas.isEmpty ? await _repo.bodegas() : _bodegas;
      if (mounted) {
        setState(() {
          _grupos = grupos;
          _bodegas = bodegas;
        });
      }
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _grupos ??= const [];
        _error = _motivo(fallo, 'no se pudieron cargar las sugerencias');
      });
    }
  }

  String _motivo(ErrorDeApi fallo, String que) {
    if (fallo is ErrorDeRed) return 'No hay conexión: $que.';
    if (fallo is SinPermiso) return 'No tienes permiso para hacer esto.';
    return fallo.mensaje;
  }

  void _avisar(String texto) =>
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(texto)));

  Future<void> _crearOrden(GrupoDeSugerenciasVista grupo) async {
    setState(() => _ocupado = grupo.clave);
    try {
      final orden = await _repo.aceptar(grupo);
      if (!mounted) return;
      _avisar('Orden ${orden.numero} creada en borrador para ${grupo.proveedorNombre ?? 'el proveedor'}.');
      await _cargar();
      widget.onOrdenCreada?.call(orden.id);
    } on ErrorDeApi catch (fallo) {
      if (mounted) _avisar(_motivo(fallo, 'la orden no se creó'));
    } finally {
      if (mounted) setState(() => _ocupado = null);
    }
  }

  Future<void> _asignar(SugerenciaVista sugerencia) async {
    final List<ProveedorParaAsignar> proveedores;
    try {
      proveedores = await _repo.proveedores();
    } on ErrorDeApi catch (fallo) {
      if (mounted) _avisar(_motivo(fallo, 'no se pudieron traer los proveedores'));
      return;
    }
    if (!mounted) return;
    final elegido = await showDialog<ProveedorParaAsignar>(
      context: context,
      builder: (_) => _DialogoDeProveedor(producto: sugerencia.nombre, proveedores: proveedores),
    );
    if (elegido == null || !mounted) return;
    setState(() => _ocupado = sugerencia.id);
    try {
      await _repo.asignarProveedor(sugerencia.id, elegido.id);
      if (!mounted) return;
      _avisar('${sugerencia.nombre} queda con ${elegido.nombre} como proveedor preferido.');
      await _cargar();
    } on ErrorDeApi catch (fallo) {
      if (mounted) _avisar(_motivo(fallo, 'el proveedor no se asignó'));
    } finally {
      if (mounted) setState(() => _ocupado = null);
    }
  }

  Future<void> _descartar(SugerenciaVista sugerencia) async {
    setState(() => _ocupado = sugerencia.id);
    try {
      await _repo.descartar(sugerencia.id);
      if (!mounted) return;
      _avisar('${sugerencia.nombre} sale de la lista.');
      await _cargar();
    } on ErrorDeApi catch (fallo) {
      if (mounted) _avisar(_motivo(fallo, 'no se descartó'));
    } finally {
      if (mounted) setState(() => _ocupado = null);
    }
  }

  @override
  Widget build(BuildContext context) {
    final grupos = _grupos;
    final variasBodegas = {for (final g in grupos ?? const <GrupoDeSugerenciasVista>[]) ...g.sugerencias.map((s) => s.bodegaId)}
            .whereType<String>()
            .length >
        1;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text('Sugerencias de compra',
            overflow: TextOverflow.ellipsis,
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
        actions: [
          IconButton(
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: () => unawaited(_cargar()),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: Column(children: [
        if (_error != null)
          Padding(padding: const EdgeInsets.fromLTRB(14, 10, 14, 0), child: AvisoDeError(_error!)),
        Expanded(
          child: grupos == null
              ? const Center(child: CircularProgressIndicator())
              : grupos.isEmpty
                  ? Center(
                      child: Padding(
                        padding: const EdgeInsets.all(24),
                        child: Text(
                          'No hay nada por reponer. Cuando un producto baje de su mínimo, aparecerá aquí.',
                          key: const Key('sugerencias-vacio'),
                          textAlign: TextAlign.center,
                          style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                        ),
                      ),
                    )
                  : RefreshIndicator(
                      onRefresh: _cargar,
                      child: ListView(
                        key: const Key('sugerencias-lista'),
                        padding: const EdgeInsets.fromLTRB(14, 12, 14, 24),
                        children: [
                          for (final g in grupos)
                            _TarjetaDeGrupo(
                              grupo: g,
                              bodega: variasBodegas && g.bodegaId != null ? _bodegas[g.bodegaId] : null,
                              ocupado: _ocupado,
                              onCrearOrden: () => unawaited(_crearOrden(g)),
                              onAsignar: (s) => unawaited(_asignar(s)),
                              onDescartar: (s) => unawaited(_descartar(s)),
                            ),
                        ],
                      ),
                    ),
        ),
      ]),
    );
  }
}

class _TarjetaDeGrupo extends StatelessWidget {
  const _TarjetaDeGrupo({
    required this.grupo,
    required this.bodega,
    required this.ocupado,
    required this.onCrearOrden,
    required this.onAsignar,
    required this.onDescartar,
  });

  final GrupoDeSugerenciasVista grupo;
  final String? bodega;
  final String? ocupado;
  final VoidCallback onCrearOrden;
  final ValueChanged<SugerenciaVista> onAsignar;
  final ValueChanged<SugerenciaVista> onDescartar;

  @override
  Widget build(BuildContext context) {
    final g = grupo;
    final titulo = g.sinProveedor ? 'Sin proveedor' : (g.proveedorNombre ?? 'Proveedor');
    final productos = g.sugerencias.length == 1 ? '1 producto' : '${g.sugerencias.length} productos';
    return Container(
      key: Key('sugerencias-grupo-${g.clave}'),
      margin: const EdgeInsets.only(bottom: 12),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: g.sinProveedor ? RegentaColors.warn : RegentaColors.line),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(14, 12, 14, 8),
          child: Row(children: [
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                Text(titulo,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
                const SizedBox(height: 2),
                Text(
                  g.sinProveedor
                      ? 'Asígnales un proveedor para poder pedirlos.'
                      : [productos, ?bodega].join(' · '),
                  style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.muted),
                ),
              ]),
            ),
            if (!g.sinProveedor && g.costoTotalEstimado > 0)
              Text(formatearPesos(g.costoTotalEstimado),
                  key: Key('sugerencias-total-${g.clave}'),
                  style: RegentaType.codigo
                      .copyWith(fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
          ]),
        ),
        for (final s in g.sugerencias)
          _FilaDeSugerencia(
            sugerencia: s,
            ocupada: ocupado == s.id,
            onAsignar: g.sinProveedor ? () => onAsignar(s) : null,
            onDescartar: () => onDescartar(s),
          ),
        if (!g.sinProveedor)
          Padding(
            padding: const EdgeInsets.fromLTRB(14, 10, 14, 12),
            child: Align(
              alignment: Alignment.centerRight,
              child: FilledButton.icon(
                key: Key('sugerencias-crear-orden-${g.clave}'),
                onPressed: ocupado == g.clave ? null : onCrearOrden,
                style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
                icon: ocupado == g.clave
                    ? const SizedBox(
                        width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                    : const Icon(Icons.playlist_add_check, size: 18),
                label: const Text('Crear orden'),
              ),
            ),
          ),
      ]),
    );
  }
}

class _FilaDeSugerencia extends StatelessWidget {
  const _FilaDeSugerencia({
    required this.sugerencia,
    required this.ocupada,
    required this.onAsignar,
    required this.onDescartar,
  });

  final SugerenciaVista sugerencia;
  final bool ocupada;
  final VoidCallback? onAsignar;
  final VoidCallback onDescartar;

  @override
  Widget build(BuildContext context) {
    final s = sugerencia;
    return Container(
      key: Key('sugerencia-${s.id}'),
      padding: const EdgeInsets.fromLTRB(14, 10, 4, 10),
      decoration: const BoxDecoration(border: Border(top: BorderSide(color: RegentaColors.line))),
      child: Row(children: [
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(s.nombre,
                maxLines: 2,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink, fontWeight: FontWeight.w600)),
            const SizedBox(height: 2),
            Text(
              'Hay ${_numero(s.existencia)} · mínimo ${_numero(s.stockMinimo)} · '
              'objetivo ${_numero(s.stockObjetivo)}',
              style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted),
            ),
            if (onAsignar != null) ...[
              const SizedBox(height: 6),
              OutlinedButton.icon(
                key: Key('sugerencia-asignar-${s.id}'),
                onPressed: ocupada ? null : onAsignar,
                icon: const Icon(Icons.store_mall_directory_outlined, size: 16),
                label: const Text('Asignar proveedor'),
              ),
            ],
          ]),
        ),
        const SizedBox(width: 8),
        Text(formatearCantidad(s.cantidadSugerida),
            key: Key('sugerencia-cantidad-${s.id}'),
            style: RegentaType.codigo.copyWith(fontSize: 13, fontWeight: FontWeight.w700, color: RegentaColors.ink)),
        IconButton(
          key: Key('sugerencia-descartar-${s.id}'),
          tooltip: 'Descartar',
          onPressed: ocupada ? null : onDescartar,
          icon: const Icon(Icons.close, size: 18, color: RegentaColors.muted),
        ),
      ]),
    );
  }
}

/// «8», «2,5»: la cantidad sin la unidad.
String _numero(num n) => formatearCantidad(n).replaceAll(' und', '');

/// C5 · Elegir el proveedor de un producto que no tiene.
class _DialogoDeProveedor extends StatefulWidget {
  const _DialogoDeProveedor({required this.producto, required this.proveedores});

  final String producto;
  final List<ProveedorParaAsignar> proveedores;

  @override
  State<_DialogoDeProveedor> createState() => _DialogoDeProveedorState();
}

class _DialogoDeProveedorState extends State<_DialogoDeProveedor> {
  ProveedorParaAsignar? _elegido;

  @override
  Widget build(BuildContext context) {
    return Dialog(
      backgroundColor: RegentaColors.surface,
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 440),
        child: Column(mainAxisSize: MainAxisSize.min, crossAxisAlignment: CrossAxisAlignment.stretch, children: [
          CabeceraDeDialogo(
            titulo: 'Asignar proveedor',
            subtitulo: widget.producto,
            onCerrar: () => Navigator.of(context).pop(),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 16, 18, 8),
            child: widget.proveedores.isEmpty
                ? Text('Todavía no hay proveedores activos. Créalo primero en Proveedores.',
                    key: const Key('asignar-sin-proveedores'),
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2))
                : Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    DropdownButtonFormField<ProveedorParaAsignar>(
                      key: const Key('asignar-proveedor'),
                      initialValue: _elegido,
                      isExpanded: true,
                      decoration: decoracionDeCampo(),
                      hint: const Text('Elige el proveedor'),
                      items: [
                        for (final p in widget.proveedores)
                          DropdownMenuItem(value: p, child: Text(p.nombre, overflow: TextOverflow.ellipsis)),
                      ],
                      onChanged: (p) => setState(() => _elegido = p),
                    ),
                    const SizedBox(height: 8),
                    Text('Quedará como su proveedor preferido.',
                        style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.muted)),
                  ]),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(18, 8, 18, 16),
            child: Row(mainAxisAlignment: MainAxisAlignment.end, children: [
              TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar')),
              const SizedBox(width: 8),
              FilledButton(
                key: const Key('asignar-guardar'),
                style: FilledButton.styleFrom(backgroundColor: RegentaColors.accent),
                onPressed: _elegido == null ? null : () => Navigator.of(context).pop(_elegido),
                child: const Text('Asignar'),
              ),
            ]),
          ),
        ]),
      ),
    );
  }
}
