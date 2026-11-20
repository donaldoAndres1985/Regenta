import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../../ui/formato.dart';
import '../datos/proveedor_del_negocio.dart';
import '../lista/estado_de_proveedores.dart';
import '../lista/proveedores.dart';
import 'catalogo_del_proveedor.dart';
import 'formulario_de_proveedor.dart';
import 'piezas.dart';

/// El listado de proveedores en móvil y en web (HU-166). Un solo widget que se adapta con
/// `LayoutBuilder` en [kBreakpointEscritorio]. El buscador y los filtros trabajan sobre lo cargado.
class PantallaProveedores extends ConsumerStatefulWidget {
  const PantallaProveedores({super.key});

  @override
  ConsumerState<PantallaProveedores> createState() => _PantallaProveedoresState();
}

class _PantallaProveedoresState extends ConsumerState<PantallaProveedores> {
  final TextEditingController _buscador = TextEditingController();
  final FocusNode _focoBuscador = FocusNode(debugLabel: 'buscador-proveedores');

  @override
  void dispose() {
    _buscador.dispose();
    _focoBuscador.dispose();
    super.dispose();
  }

  void _avisar(String texto) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(texto)));
  }

  Future<void> _crear() async {
    final ctrl = ref.read(controladorDeProveedoresProvider.notifier);
    final r = await abrirFormularioDeProveedor(context, onGuardar: ctrl.guardar);
    if (r == ResultadoDeFicha.guardado) _avisar('Proveedor agregado.');
  }

  Future<void> _abrir(ProveedorDelNegocio proveedor) async {
    final ctrl = ref.read(controladorDeProveedoresProvider.notifier);
    final r = await abrirFormularioDeProveedor(
      context,
      proveedor: proveedor,
      onGuardar: ctrl.guardar,
      onDesactivar: ctrl.desactivar,
      onReactivar: ctrl.reactivar,
    );
    switch (r) {
      case ResultadoDeFicha.guardado:
        _avisar('Proveedor modificado.');
      case ResultadoDeFicha.desactivado:
        _avisar('${proveedor.nombre} quedó desactivado. Lo encuentras en «Inactivos».');
      case ResultadoDeFicha.reactivado:
        _avisar('${proveedor.nombre} está activo otra vez.');
      case ResultadoDeFicha.nada:
        break;
    }
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeProveedoresProvider);
    final ctrl = ref.read(controladorDeProveedoresProvider.notifier);

    return CallbackShortcuts(
      bindings: {const SingleActivator(LogicalKeyboardKey.f5): _focoBuscador.requestFocus},
      child: LayoutBuilder(builder: (context, afuera) {
        final acciones = <Widget>[
          IconButton(
            tooltip: 'Actualizar',
            onPressed: estado.cargando ? null : ctrl.cargar,
            icon: const Icon(Icons.refresh, size: 20, color: RegentaColors.ink2),
          ),
          FilledButton.icon(
            key: const Key('proveedores-nuevo'),
            onPressed: () => unawaited(_crear()),
            style: FilledButton.styleFrom(
              backgroundColor: RegentaColors.accent,
              foregroundColor: RegentaColors.surface,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
            ),
            icon: const Icon(Icons.add_business_outlined, size: 18),
            label: const Text('Nuevo proveedor'),
          ),
        ];
        final escritorio = afuera.maxWidth >= kBreakpointEscritorio;
        final cuerpo = Column(children: [
          _Controles(
            controlador: _buscador,
            foco: _focoBuscador,
            filtro: estado.filtro,
            onTermino: ctrl.cambiarTermino,
            onFiltro: ctrl.cambiarFiltro,
          ),
          if (estado.mensaje != null && estado.todos.isNotEmpty) _Aviso(texto: estado.mensaje!),
          _Resumen(estado: estado),
          Expanded(
            child: _Lista(
              estado: estado,
              onAbrir: (p) => unawaited(_abrir(p)),
              onCatalogo: (p) => unawaited(abrirCatalogoDelProveedor(context, p)),
              onCrear: () => unawaited(_crear()),
            ),
          ),
        ]);
        return Scaffold(
          backgroundColor: RegentaColors.paper,
          appBar: escritorio
              ? EncabezadoDeSeccion(
                  subtitulo: 'A quién le compras, con qué plazo y cuánto le debes',
                  titulo: 'Proveedores',
                  acciones: acciones,
                )
              : AppBar(
                  backgroundColor: RegentaColors.surface,
                  surfaceTintColor: RegentaColors.surface,
                  elevation: 0,
                  shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
                  title: Text('Proveedores',
                      style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
                  actions: [...acciones, const SizedBox(width: 12)],
                ),
          body: escritorio
              ? Center(
                  key: const Key('proveedores-escritorio'),
                  child: ConstrainedBox(constraints: const BoxConstraints(maxWidth: 860), child: cuerpo),
                )
              : KeyedSubtree(key: const Key('proveedores-movil'), child: cuerpo),
        );
      }),
    );
  }
}

class _Controles extends StatelessWidget {
  const _Controles({
    required this.controlador,
    required this.foco,
    required this.filtro,
    required this.onTermino,
    required this.onFiltro,
  });

  final TextEditingController controlador;
  final FocusNode foco;
  final FiltroDeProveedores filtro;
  final ValueChanged<String> onTermino;
  final ValueChanged<FiltroDeProveedores> onFiltro;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
        SizedBox(
          height: 42,
          child: TextField(
            key: const Key('proveedores-buscar'),
            controller: controlador,
            focusNode: foco,
            onChanged: onTermino,
            style: estiloDeCampo,
            decoration: decoracionDeCampo(
              hint: 'Nombre o NIT…  (F5)',
              prefijo: const Icon(Icons.search, size: 18, color: RegentaColors.faint),
            ),
          ),
        ),
        const SizedBox(height: 8),
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          child: Row(children: [
            for (final f in FiltroDeProveedores.values) ...[
              _ChipFiltro(filtro: f, seleccionado: f == filtro, onTap: () => onFiltro(f)),
              const SizedBox(width: 7),
            ],
          ]),
        ),
      ]),
    );
  }
}

class _ChipFiltro extends StatelessWidget {
  const _ChipFiltro({required this.filtro, required this.seleccionado, required this.onTap});

  final FiltroDeProveedores filtro;
  final bool seleccionado;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      button: true,
      selected: seleccionado,
      label: filtro.etiqueta,
      excludeSemantics: true,
      child: InkWell(
        key: Key('proveedores-filtro-${filtro.name}'),
        onTap: onTap,
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
          child: Align(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
              decoration: BoxDecoration(
                color: seleccionado ? RegentaColors.accent : RegentaColors.sunken,
                borderRadius: BorderRadius.circular(3),
              ),
              child: Text(
                filtro.etiqueta.toUpperCase(),
                style: RegentaType.etiqueta.copyWith(
                  fontSize: 9.5,
                  color: seleccionado ? RegentaColors.surface : RegentaColors.ink2,
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Aviso extends StatelessWidget {
  const _Aviso({required this.texto});

  final String texto;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
      color: RegentaColors.warnSoft,
      child: Text(texto, style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.warn)),
    );
  }
}

class _Resumen extends StatelessWidget {
  const _Resumen({required this.estado});

  final EstadoDeProveedores estado;

  @override
  Widget build(BuildContext context) {
    final n = estado.visibles.length;
    return Padding(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 2),
      child: Row(children: [
        Expanded(
          child: Text(n == 1 ? '1 proveedor' : '$n proveedores',
              style: RegentaType.cuerpo.copyWith(fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
        ),
        Text('debes ${formatearPesos(estado.deudaVisible)}',
            key: const Key('proveedores-deuda-total'),
            style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.ink2)),
      ]),
    );
  }
}

class _Lista extends StatelessWidget {
  const _Lista({required this.estado, required this.onAbrir, required this.onCatalogo, required this.onCrear});

  final EstadoDeProveedores estado;
  final ValueChanged<ProveedorDelNegocio> onAbrir;
  final ValueChanged<ProveedorDelNegocio> onCatalogo;
  final VoidCallback onCrear;

  @override
  Widget build(BuildContext context) {
    if (estado.cargando && estado.todos.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    final visibles = estado.visibles;
    if (visibles.isEmpty) {
      final texto = estado.mensaje ??
          (estado.todos.isEmpty
              ? 'Todavía no hay proveedores registrados.'
              : 'Ningún proveedor coincide con la búsqueda.');
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(mainAxisSize: MainAxisSize.min, children: [
            const Icon(Icons.local_shipping_outlined, size: 54, color: RegentaColors.ink2),
            const SizedBox(height: 14),
            Text(texto,
                key: const Key('proveedores-vacio'),
                textAlign: TextAlign.center,
                style: RegentaType.cuerpo.copyWith(fontSize: 15, color: RegentaColors.ink2)),
            if (estado.todos.isEmpty && estado.mensaje == null) ...[
              const SizedBox(height: 18),
              FilledButton.icon(
                icon: const Icon(Icons.add_business_outlined),
                label: const Text('Registrar primer proveedor'),
                onPressed: onCrear,
              ),
            ],
          ]),
        ),
      );
    }
    return ListView.builder(
      key: const Key('proveedores-lista'),
      padding: const EdgeInsets.symmetric(horizontal: 14),
      itemCount: visibles.length,
      itemBuilder: (context, i) => _FilaProveedor(
        proveedor: visibles[i],
        onAbrir: () => onAbrir(visibles[i]),
        onCatalogo: () => onCatalogo(visibles[i]),
      ),
    );
  }
}

class _FilaProveedor extends StatelessWidget {
  const _FilaProveedor({required this.proveedor, required this.onAbrir, required this.onCatalogo});

  final ProveedorDelNegocio proveedor;
  final VoidCallback onAbrir;
  final VoidCallback onCatalogo;

  @override
  Widget build(BuildContext context) {
    final p = proveedor;
    final plazo = p.diasCredito == 0 ? 'contado' : '${p.diasCredito} días';
    return InkWell(
      key: Key('proveedor-fila-${p.id}'),
      onTap: onAbrir,
      child: Opacity(
        opacity: p.activo ? 1 : 0.6,
        child: Container(
          constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
          padding: const EdgeInsets.symmetric(vertical: 12),
          decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
          child: Row(children: [
            _Avatar(nombre: p.nombre),
            const SizedBox(width: 12),
            Expanded(
              child: Column(crossAxisAlignment: CrossAxisAlignment.start, mainAxisSize: MainAxisSize.min, children: [
                Text(p.nombre.toUpperCase(),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.item.copyWith(color: RegentaColors.inkFuerte, fontWeight: FontWeight.w700)),
                const SizedBox(height: 3),
                Text('${p.documentoLabel} · $plazo'.toUpperCase(),
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.codigo.copyWith(
                        fontSize: 10.5, fontWeight: FontWeight.w600, color: RegentaColors.muted)),
              ]),
            ),
            const SizedBox(width: 12),
            Column(crossAxisAlignment: CrossAxisAlignment.end, mainAxisSize: MainAxisSize.min, children: [
              Text(formatearPesos(p.deuda),
                  style: RegentaType.codigo.copyWith(fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
              const SizedBox(height: 4),
              if (!p.activo)
                const ChipDeEstado(texto: 'inactivo', fondo: RegentaColors.sunken, tinta: RegentaColors.muted)
              else if (p.superaCupo)
                const ChipDeEstado(texto: 'supera cupo', fondo: RegentaColors.critSoft, tinta: RegentaColors.crit)
              else
                Text(p.cupoCredito > 0 ? 'cupo ${formatearPesos(p.cupoCredito)}' : 'sin cupo',
                    style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
            ]),
            const SizedBox(width: 4),
            IconButton(
              key: Key('proveedor-catalogo-${p.id}'),
              tooltip: 'Productos que vende',
              onPressed: onCatalogo,
              icon: const Icon(Icons.inventory_2_outlined, size: 18, color: RegentaColors.ink2),
            ),
          ]),
        ),
      ),
    );
  }
}

class _Avatar extends StatelessWidget {
  const _Avatar({required this.nombre});

  final String nombre;

  @override
  Widget build(BuildContext context) {
    final partes = nombre.trim().split(RegExp(r'\s+')).where((p) => p.isNotEmpty).toList();
    final iniciales = partes.take(2).map((p) => p[0].toUpperCase()).join();
    return Container(
      width: 38,
      height: 38,
      alignment: Alignment.center,
      decoration: const BoxDecoration(color: RegentaColors.sunken, shape: BoxShape.circle),
      child: Text(iniciales,
          style: RegentaType.codigo.copyWith(fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink2)),
    );
  }
}
