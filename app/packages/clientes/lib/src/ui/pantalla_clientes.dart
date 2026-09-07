import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/cliente_en_lista.dart';
import '../lista/estado_de_clientes.dart';
import '../lista/filtro_de_clientes.dart';
import '../lista/proveedores.dart';
import 'formato.dart';

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

  @override
  void dispose() {
    _buscador.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeClientesProvider);
    final ctrl = ref.read(controladorDeClientesProvider.notifier);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text('Clientes',
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
      ),
      body: LayoutBuilder(
        builder: (context, restricciones) {
          final controles = _Controles(
            controlador: _buscador,
            filtro: estado.filtro,
            onTermino: ctrl.cambiarTermino,
            onFiltro: ctrl.cambiarFiltro,
          );
          final encabezado = _Encabezado(estado: estado);
          final lista = _Lista(
            estado: estado,
            onAbrir: widget.onAbrirCliente,
          );

          if (restricciones.maxWidth >= kBreakpointEscritorio) {
            return Column(
              key: const Key('clientes-escritorio'),
              children: [
                controles,
                if (estado.desdeCache) const _AvisoSinConexion(),
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
              if (estado.desdeCache) const _AvisoSinConexion(),
              encabezado,
              Expanded(child: lista),
            ],
          );
        },
      ),
    );
  }
}

class _Controles extends StatelessWidget {
  const _Controles({
    required this.controlador,
    required this.filtro,
    required this.onTermino,
    required this.onFiltro,
  });

  final TextEditingController controlador;
  final FiltroDeClientes filtro;
  final ValueChanged<String> onTermino;
  final ValueChanged<FiltroDeClientes> onFiltro;

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
              onChanged: onTermino,
              style: RegentaType.cuerpo.copyWith(fontSize: 14, color: RegentaColors.ink),
              decoration: InputDecoration(
                isDense: true,
                prefixIcon:
                    const Icon(Icons.search, size: 18, color: RegentaColors.faint),
                hintText: 'Nombre, NIT o cédula…',
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

class _AvisoSinConexion extends StatelessWidget {
  const _AvisoSinConexion();

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 7),
      color: RegentaColors.warnSoft,
      child: Text('Mostrando la copia guardada — sin conexión',
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
            child: Text('${estado.totalVisible} clientes',
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
  const _Lista({required this.estado, this.onAbrir});

  final EstadoDeClientes estado;
  final void Function(String clienteId)? onAbrir;

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
          child: Text(
            estado.mensaje ??
                (estado.todos.isEmpty
                    ? 'Todavía no hay clientes.'
                    : 'Ningún cliente coincide con la búsqueda.'),
            textAlign: TextAlign.center,
            style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted),
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
                  Text(cliente.nombre,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.item.copyWith(color: RegentaColors.ink)),
                  const SizedBox(height: 3),
                  Text(cliente.documentoLabel,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.codigo
                          .copyWith(fontSize: 10.5, color: RegentaColors.muted)),
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
