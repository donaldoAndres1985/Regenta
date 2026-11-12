import 'dart:async';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/cliente_en_lista.dart';

import '../lista/estado_de_clientes.dart';
import '../lista/filtro_de_clientes.dart';
import '../lista/proveedores.dart';
import 'formato.dart';
import 'formulario_de_cliente.dart';

/// El listado de clientes en móvil y en web. HU-025.
///
/// Es **un solo widget** que se adapta con `LayoutBuilder` en
/// [kBreakpointEscritorio]. El buscador y los filtros trabajan sobre lo que ya
/// está cargado: nunca vuelven a pedir nada. Si no hay señal, se muestra la
/// copia local.
class PantallaClientes extends ConsumerStatefulWidget {
  const PantallaClientes({super.key, this.onAbrirCliente});

  final void Function(String clienteId)? onAbrirCliente;

  @override
  ConsumerState<PantallaClientes> createState() => _PantallaClientesState();
}

class _PantallaClientesState extends ConsumerState<PantallaClientes> {
  final TextEditingController _buscador = TextEditingController();
  final FocusNode _focoBuscador = FocusNode(debugLabel: 'buscador-clientes');
  bool _abriendo = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _focoBuscador.requestFocus();
    });
  }

  @override
  void dispose() {
    _buscador.dispose();
    _focoBuscador.dispose();
    super.dispose();
  }

  Future<void> _crear() async {
    final ctrl = ref.read(controladorDeClientesProvider.notifier);
    final guardado = await abrirFormularioDeCliente(context, onGuardar: ctrl.guardar);
    if (guardado) _avisar('Cliente agregado correctamente.');
  }

  /// Tocar una fila abre la ficha para editarla, salvo que quien montó la
  /// pantalla haya pedido otra cosa con `onAbrirCliente`.
  Future<void> _abrir(String clienteId) async {
    if (widget.onAbrirCliente != null) {
      widget.onAbrirCliente!(clienteId);
      return;
    }
    if (_abriendo) return;
    _abriendo = true;
    final ctrl = ref.read(controladorDeClientesProvider.notifier);
    final (ficha, motivo) = await ctrl.abrir(clienteId);
    _abriendo = false;
    if (!mounted) return;
    if (ficha == null) {
      _avisar(motivo ?? 'No se pudo abrir la ficha del cliente.');
      return;
    }
    final guardado =
        await abrirFormularioDeCliente(context, ficha: ficha, onGuardar: ctrl.guardar);
    if (guardado) _avisar('Cliente modificado correctamente.');
  }

  void _avisar(String texto) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(texto)));
  }

  /// Enter en el buscador con un solo resultado abre ese cliente.
  void _enterEnBuscador() {
    final visibles = ref.read(controladorDeClientesProvider).visibles;
    if (visibles.length == 1) unawaited(_abrir(visibles.first.id));
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeClientesProvider);
    final ctrl = ref.read(controladorDeClientesProvider.notifier);

    // F5 lleva al buscador, como en el escritorio de siempre.
    return CallbackShortcuts(
      bindings: {
        const SingleActivator(LogicalKeyboardKey.f5): _focoBuscador.requestFocus,
      },
      child: Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text('Clientes',
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
        actions: [
          IconButton(
            tooltip: 'Actualizar',
            onPressed: estado.cargando ? null : ctrl.cargar,
            icon: const Icon(Icons.refresh, size: 20, color: RegentaColors.ink2),
          ),
          Padding(
            padding: const EdgeInsets.only(right: 12, left: 4),
            child: FilledButton.icon(
              key: const Key('clientes-nuevo'),
              onPressed: _crear,
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.accent,
                foregroundColor: RegentaColors.surface,
                shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
              ),
              icon: const Icon(Icons.person_add_alt_1, size: 18),
              label: const Text('Nuevo cliente'),
            ),
          ),
        ],
      ),
      body: LayoutBuilder(
        builder: (context, restricciones) {
          final controles = _Controles(
            controlador: _buscador,
            foco: _focoBuscador,
            filtro: estado.filtro,
            onTermino: ctrl.cambiarTermino,
            onFiltro: ctrl.cambiarFiltro,
            onEnter: _enterEnBuscador,
          );
          final encabezado = _Encabezado(estado: estado);
          final lista = _Lista(
            estado: estado,
            onAbrir: (id) => unawaited(_abrir(id)),
            onCrear: _crear,
          );
          final aviso = estado.desdeCache || (estado.mensaje != null && estado.todos.isNotEmpty)
              ? _Aviso(texto: estado.mensaje)
              : null;

          if (restricciones.maxWidth >= kBreakpointEscritorio) {
            return Column(
              key: const Key('clientes-escritorio'),
              children: [
                controles,
                ?aviso,
                Expanded(
                  child: Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 760),
                      child: Column(children: [encabezado, Expanded(child: lista)]),
                    ),
                  ),
                ),
              ],
            );
          }
          return Column(
            key: const Key('clientes-movil'),
            children: [
              controles,
              ?aviso,
              encabezado,
              Expanded(child: lista),
            ],
          );
        },
      ),
      ),
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
    required this.onEnter,
  });

  final TextEditingController controlador;
  final FocusNode foco;
  final FiltroDeClientes filtro;
  final ValueChanged<String> onTermino;
  final ValueChanged<FiltroDeClientes> onFiltro;
  final VoidCallback onEnter;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          SizedBox(
            height: 42,
            child: TextField(
              controller: controlador,
              focusNode: foco,
              autofocus: true,
              onChanged: onTermino,
              onSubmitted: (_) => onEnter(),
              style: const TextStyle(
                fontSize: 14,
                fontWeight: FontWeight.w600,
                color: RegentaColors.inkFuerte,
              ),
              decoration: InputDecoration(
                isDense: true,
                filled: true,
                fillColor: RegentaColors.surface,
                prefixIcon:
                    const Icon(Icons.search, size: 18, color: RegentaColors.faint),
                hintText: 'Nombre, NIT o cédula…  (F5)',
                hintStyle:
                    const TextStyle(fontSize: 14, color: RegentaColors.faint),
                contentPadding: const EdgeInsets.symmetric(vertical: 11),
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                  borderSide: const BorderSide(color: RegentaColors.line2),
                ),
                enabledBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                  borderSide: const BorderSide(color: RegentaColors.line2),
                ),
                focusedBorder: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                  borderSide: const BorderSide(color: RegentaColors.accent, width: 1.5),
                ),
              ),
            ),
          ),
          const SizedBox(height: 8),
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              children: [
                for (final f in FiltroDeClientes.values) ...[
                  _ChipFiltro(
                    filtro: f,
                    seleccionado: f == filtro,
                    onTap: () => onFiltro(f),
                  ),
                  if (f != FiltroDeClientes.values.last) const SizedBox(width: 7),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _ChipFiltro extends StatelessWidget {
  const _ChipFiltro({
    required this.filtro,
    required this.seleccionado,
    required this.onTap,
  });

  final FiltroDeClientes filtro;
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
        onTap: onTap,
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
          child: Align(
            alignment: Alignment.center,
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

/// La franja de aviso: el motivo real si el servidor lo dio, o el de siempre
/// cuando lo que se ve es la copia guardada.
class _Aviso extends StatelessWidget {
  const _Aviso({this.texto});

  final String? texto;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
      color: RegentaColors.warnSoft,
      child: Text(texto ?? 'Mostrando la copia guardada — sin conexión',
          style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.warn)),
    );
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({required this.estado});

  final EstadoDeClientes estado;

  @override
  Widget build(BuildContext context) {
    final cartera = estado.visibles.fold<num>(0, (suma, c) => suma + c.saldo);
    return Padding(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 2),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.baseline,
        textBaseline: TextBaseline.alphabetic,
        children: [
          Expanded(
            child: Text(
                estado.totalVisible == 1 ? '1 cliente' : '${estado.totalVisible} clientes',
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.cuerpo.copyWith(
                    fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
          ),
          const SizedBox(width: 8),
          Text('cartera ${formatearPesos(cartera)}',
              style: RegentaType.codigo.copyWith(fontSize: 11, color: RegentaColors.ink2)),
        ],
      ),
    );
  }
}

class _Lista extends StatelessWidget {
  const _Lista({required this.estado, this.onAbrir, this.onCrear});

  final EstadoDeClientes estado;
  final void Function(String clienteId)? onAbrir;
  final VoidCallback? onCrear;

  @override
  Widget build(BuildContext context) {
    if (estado.cargando && estado.todos.isEmpty) {
      return const Center(child: CircularProgressIndicator());
    }
    final visibles = estado.visibles;
    if (visibles.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.people_outline, size: 54, color: RegentaColors.ink2),
              const SizedBox(height: 14),
              Text(
                estado.mensaje ??
                    (estado.todos.isEmpty
                        ? 'Todavía no hay clientes registrados.'
                        : 'Ningún cliente coincide con la búsqueda.'),
                textAlign: TextAlign.center,
                style: RegentaType.cuerpo.copyWith(fontSize: 15, color: RegentaColors.ink2),
              ),
              const SizedBox(height: 18),
              if (onCrear != null)
                FilledButton.icon(
                  icon: const Icon(Icons.person_add),
                  label: Text(estado.todos.isEmpty ? 'Registrar primer cliente' : 'Nuevo cliente'),
                  onPressed: onCrear,
                ),
            ],
          ),
        ),
      );
    }
    return ListView.builder(
      key: const Key('clientes-lista'),
      padding: const EdgeInsets.symmetric(horizontal: 14),
      itemCount: visibles.length,
      itemBuilder: (context, i) => _FilaCliente(cliente: visibles[i], onAbrir: onAbrir),
    );
  }
}

class _FilaCliente extends StatelessWidget {
  const _FilaCliente({required this.cliente, this.onAbrir});

  final ClienteEnLista cliente;
  final void Function(String clienteId)? onAbrir;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onAbrir == null ? null : () => onAbrir!(cliente.id),
      child: Container(
        constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
        padding: const EdgeInsets.symmetric(vertical: 12),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: RegentaColors.line)),
        ),
        child: Row(
          children: [
            _Avatar(nombre: cliente.nombre),
            const SizedBox(width: 12),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(cliente.nombre.toUpperCase(),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.item.copyWith(
                          color: RegentaColors.inkFuerte,
                          fontWeight: FontWeight.w700)),
                  const SizedBox(height: 3),
                  Text(cliente.documentoLabel.toUpperCase(),
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.codigo.copyWith(
                          fontSize: 10.5,
                          fontWeight: FontWeight.w600,
                          color: RegentaColors.muted)),
                ],
              ),
            ),
            const SizedBox(width: 12),
            Column(
              crossAxisAlignment: CrossAxisAlignment.end,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(formatearPesos(cliente.saldo),
                    style: RegentaType.codigo.copyWith(
                        fontSize: 13,
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.ink)),
                const SizedBox(height: 4),
                _ChipEstado(estado: cliente.estadoDeCartera),
              ],
            ),
            if (onAbrir != null) ...[
              const SizedBox(width: 8),
              const Icon(Icons.edit_outlined, size: 16, color: RegentaColors.faint),
            ],
          ],
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
    return Container(
      width: 38,
      height: 38,
      alignment: Alignment.center,
      decoration: const BoxDecoration(
        color: RegentaColors.sunken,
        shape: BoxShape.circle,
      ),
      child: Text(iniciales(nombre),
          style: RegentaType.codigo.copyWith(
              fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink2)),
    );
  }
}

class _ChipEstado extends StatelessWidget {
  const _ChipEstado({required this.estado});

  final EstadoDeCartera estado;

  @override
  Widget build(BuildContext context) {
    final (texto, fondo, tinta) = switch (estado) {
      EstadoDeCartera.alDia => ('al día', RegentaColors.okSoft, RegentaColors.ok),
      EstadoDeCartera.excedido => ('excedido', RegentaColors.critSoft, RegentaColors.crit),
      EstadoDeCartera.sinSaldo => ('sin saldo', RegentaColors.sunken, RegentaColors.muted),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(3)),
      child: Text(texto.toUpperCase(),
          style: RegentaType.etiqueta.copyWith(fontSize: 9.5, color: tinta)),
    );
  }
}
