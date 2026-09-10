import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../comanda/proveedores.dart';
import '../datos/comanda_vista.dart';
import 'formato.dart';

/// La toma de comanda en móvil y en web (HU-085). **Un solo widget** que se
/// adapta con `LayoutBuilder` en [kBreakpointEscritorio]. Se van agregando
/// líneas por rondas; cada una tiene su propio estado; los totales se
/// recalculan en cada adición.
class PantallaComanda extends ConsumerWidget {
  const PantallaComanda({super.key, required this.comandaId, this.cartaId});

  final String comandaId;

  /// La carta del menú de la que se eligen los ítems al «Añadir». Sin ella, el
  /// botón queda deshabilitado.
  final String? cartaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(controladorDeComandaProvider(comandaId));
    final ctrl = ref.read(controladorDeComandaProvider(comandaId).notifier);
    final comanda = estado.comanda;

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text(comanda?.numero ?? 'Comanda',
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
      ),
      body: Builder(builder: (context) {
        if (estado.cargando && comanda == null) {
          return const Center(child: CircularProgressIndicator());
        }
        if (estado.errorAlCargar && comanda == null) {
          return _CentroConReintento(onReintentar: ctrl.cargar);
        }
        if (comanda == null) return const SizedBox.shrink();

        return LayoutBuilder(builder: (context, restricciones) {
          final ancho = restricciones.maxWidth >= kBreakpointEscritorio ? 720.0 : double.infinity;
          return Center(
            child: ConstrainedBox(
              constraints: BoxConstraints(maxWidth: ancho),
              child: KeyedSubtree(
                key: Key(restricciones.maxWidth >= kBreakpointEscritorio
                    ? 'comanda-escritorio'
                    : 'comanda-movil'),
                child: Column(
                  children: [
                    _Encabezado(comanda: comanda),
                    if (estado.mensaje != null)
                      _Aviso(mensaje: estado.mensaje!, onCerrar: ctrl.limpiarMensaje),
                    Expanded(child: _Lineas(comanda: comanda)),
                    _BarraInferior(
                      comanda: comanda,
                      puedeAnadir: cartaId != null,
                      onAnadir: () => _abrirAnadir(context, ref),
                      onEnviar: ctrl.enviarACocina,
                    ),
                  ],
                ),
              ),
            ),
          );
        });
      }),
    );
  }

  Future<void> _abrirAnadir(BuildContext context, WidgetRef ref) async {
    final repo = ref.read(repositorioDeComandasProvider);
    final ctrl = ref.read(controladorDeComandaProvider(comandaId).notifier);
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: RegentaColors.surface,
      builder: (_) => _HojaAnadir(
        cargarItems: () => repo.itemsDeCarta(cartaId!),
        cargarGrupos: repo.gruposDeItem,
        onAgregar: ({
          required String itemMenuId,
          required num cantidad,
          required List<String> modificadorIds,
          String? notas,
        }) =>
            ctrl.agregarLinea(
              itemMenuId: itemMenuId,
              cantidad: cantidad,
              modificadorIds: modificadorIds,
              notas: notas,
            ),
      ),
    );
  }
}

class _CentroConReintento extends StatelessWidget {
  const _CentroConReintento({required this.onReintentar});
  final VoidCallback onReintentar;

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Text('No se pudo cargar la comanda',
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
          const SizedBox(height: 10),
          FilledButton(
            key: const Key('comanda-reintentar'),
            onPressed: onReintentar,
            child: const Text('Reintentar'),
          ),
        ],
      ),
    );
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({required this.comanda});
  final ComandaVista comanda;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(14, 11, 14, 11),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('${comanda.numComensales} comensales',
                    style: RegentaType.item.copyWith(color: RegentaColors.ink)),
                const SizedBox(height: 2),
                Text('${comanda.itemsCount} ítems · cada línea tiene su propio estado',
                    style: RegentaType.codigo
                        .copyWith(fontSize: 10.5, color: RegentaColors.muted)),
              ],
            ),
          ),
          _ChipEstadoComanda(estado: comanda.estado),
        ],
      ),
    );
  }
}

class _ChipEstadoComanda extends StatelessWidget {
  const _ChipEstadoComanda({required this.estado});
  final String estado;

  @override
  Widget build(BuildContext context) {
    final (texto, fondo, tinta) = switch (estado) {
      'ABIERTA' => ('Abierta', RegentaColors.okSoft, RegentaColors.ok),
      'EN_COCINA' => ('En cocina', RegentaColors.warnSoft, RegentaColors.warn),
      'SERVIDA' => ('Servida', RegentaColors.infoSoft, RegentaColors.info),
      'CUENTA_PEDIDA' => ('Cuenta pedida', RegentaColors.warnSoft, RegentaColors.warn),
      'CERRADA' => ('Cerrada', RegentaColors.sunken, RegentaColors.muted),
      _ => ('Anulada', RegentaColors.critSoft, RegentaColors.crit),
    };
    return Container(
      key: const Key('comanda-estado'),
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(3)),
      child: Text(texto.toUpperCase(),
          style: RegentaType.etiqueta.copyWith(fontSize: 9.5, color: tinta)),
    );
  }
}

class _Lineas extends StatelessWidget {
  const _Lineas({required this.comanda});
  final ComandaVista comanda;

  @override
  Widget build(BuildContext context) {
    if (comanda.lineas.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text('Todavía no hay líneas. Pulsa «Añadir».',
              textAlign: TextAlign.center,
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
        ),
      );
    }
    return ListView.builder(
      key: const Key('comanda-lineas'),
      padding: const EdgeInsets.symmetric(horizontal: 14),
      itemCount: comanda.lineas.length,
      itemBuilder: (context, i) => _FilaLinea(linea: comanda.lineas[i]),
    );
  }
}

class _FilaLinea extends StatelessWidget {
  const _FilaLinea({required this.linea});
  final LineaVista linea;

  @override
  Widget build(BuildContext context) {
    final estilo = estiloDeLinea(linea.estado);
    return Container(
      key: Key('linea-${linea.id}'),
      constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
      padding: const EdgeInsets.symmetric(vertical: 12),
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Container(
            width: 26,
            height: 26,
            alignment: Alignment.center,
            decoration: BoxDecoration(
              color: RegentaColors.sunken,
              borderRadius: BorderRadius.circular(5),
            ),
            child: Text('${linea.cantidad.round()}',
                style: RegentaType.codigo.copyWith(
                    fontSize: 12, fontWeight: FontWeight.w700, color: RegentaColors.ink2)),
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(linea.nombre,
                    style: RegentaType.item.copyWith(fontSize: 13.5, color: RegentaColors.ink)),
                const SizedBox(height: 4),
                Wrap(
                  spacing: 6,
                  runSpacing: 4,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                          color: estilo.fondo, borderRadius: BorderRadius.circular(3)),
                      child: Text(estilo.etiqueta.toUpperCase(),
                          style: RegentaType.etiqueta.copyWith(fontSize: 9.5, color: estilo.tinta)),
                    ),
                    for (final m in linea.modificadores)
                      Text(m,
                          style: RegentaType.codigo
                              .copyWith(fontSize: 10, color: RegentaColors.muted)),
                    if (linea.notas != null)
                      Text(linea.notas!,
                          style: RegentaType.codigo
                              .copyWith(fontSize: 10, color: RegentaColors.crit)),
                  ],
                ),
              ],
            ),
          ),
          const SizedBox(width: 10),
          Text(pesos(linea.total),
              style: RegentaType.codigo.copyWith(
                  fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
        ],
      ),
    );
  }
}

class _Aviso extends StatelessWidget {
  const _Aviso({required this.mensaje, required this.onCerrar});
  final String mensaje;
  final VoidCallback onCerrar;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      color: RegentaColors.critSoft,
      padding: const EdgeInsets.fromLTRB(14, 8, 6, 8),
      child: Row(
        children: [
          Expanded(
            child: Text(mensaje,
                key: const Key('comanda-aviso'),
                style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.crit)),
          ),
          IconButton(
            onPressed: onCerrar,
            icon: const Icon(Icons.close, size: 16, color: RegentaColors.crit),
            visualDensity: VisualDensity.compact,
          ),
        ],
      ),
    );
  }
}

class _BarraInferior extends StatelessWidget {
  const _BarraInferior({
    required this.comanda,
    required this.puedeAnadir,
    required this.onAnadir,
    required this.onEnviar,
  });

  final ComandaVista comanda;
  final bool puedeAnadir;
  final VoidCallback onAnadir;
  final VoidCallback onEnviar;

  @override
  Widget build(BuildContext context) {
    return Container(
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(top: BorderSide(color: RegentaColors.line)),
      ),
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      child: Column(
        mainAxisSize: MainAxisSize.min,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text('Total',
                  style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
              Text(pesos(comanda.total),
                  key: const Key('comanda-total'),
                  style: const TextStyle(
                      fontFamily: RegentaType.mono,
                      fontSize: 21,
                      fontWeight: FontWeight.w700,
                      color: RegentaColors.ink)),
            ],
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  key: const Key('comanda-anadir'),
                  onPressed: puedeAnadir ? onAnadir : null,
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('Añadir'),
                ),
              ),
              const SizedBox(width: 9),
              Expanded(
                child: FilledButton(
                  key: const Key('comanda-enviar'),
                  onPressed: onEnviar,
                  child: const Text('Enviar a cocina'),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}

typedef _AltaDeLinea = Future<String?> Function({
  required String itemMenuId,
  required num cantidad,
  required List<String> modificadorIds,
  String? notas,
});

class _HojaAnadir extends StatefulWidget {
  const _HojaAnadir({
    required this.cargarItems,
    required this.cargarGrupos,
    required this.onAgregar,
  });

  final Future<List<ItemDeCarta>> Function() cargarItems;
  final Future<List<GrupoModificadores>> Function(String itemId) cargarGrupos;
  final _AltaDeLinea onAgregar;

  @override
  State<_HojaAnadir> createState() => _HojaAnadirState();
}

class _HojaAnadirState extends State<_HojaAnadir> {
  final _cantidad = TextEditingController(text: '1');
  final _notas = TextEditingController();
  List<ItemDeCarta> _items = const [];
  ItemDeCarta? _elegido;
  List<GrupoModificadores> _grupos = const [];
  final Set<String> _mods = {};
  bool _cargando = true;
  bool _enviando = false;
  String? _error;

  @override
  void initState() {
    super.initState();
    widget.cargarItems().then((items) {
      if (!mounted) return;
      setState(() {
        _items = items;
        _cargando = false;
      });
    });
  }

  @override
  void dispose() {
    _cantidad.dispose();
    _notas.dispose();
    super.dispose();
  }

  Future<void> _elegir(ItemDeCarta item) async {
    setState(() {
      _elegido = item;
      _grupos = const [];
      _mods.clear();
    });
    final grupos = await widget.cargarGrupos(item.id);
    if (mounted && _elegido?.id == item.id) setState(() => _grupos = grupos);
  }

  Future<void> _agregar() async {
    final item = _elegido;
    final cant = num.tryParse(_cantidad.text.trim());
    if (item == null || cant == null || cant <= 0) {
      setState(() => _error = 'Elige un ítem y una cantidad válida');
      return;
    }
    setState(() {
      _enviando = true;
      _error = null;
    });
    final err = await widget.onAgregar(
      itemMenuId: item.id,
      cantidad: cant,
      modificadorIds: _mods.toList(),
      notas: _notas.text.trim().isEmpty ? null : _notas.text.trim(),
    );
    if (!mounted) return;
    if (err == null) {
      Navigator.of(context).pop();
    } else {
      setState(() {
        _enviando = false;
        _error = err;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return SafeArea(
      child: Padding(
        padding: EdgeInsets.fromLTRB(
            16, 14, 16, MediaQuery.of(context).viewInsets.bottom + 16),
        child: SingleChildScrollView(
          child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text('Añadir a la comanda',
                style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
            const SizedBox(height: 12),
            if (_cargando)
              const Center(child: Padding(
                  padding: EdgeInsets.all(12), child: CircularProgressIndicator()))
            else
              DropdownButtonFormField<ItemDeCarta>(
                key: const Key('anadir-item'),
                initialValue: _elegido,
                isExpanded: true,
                decoration: const InputDecoration(labelText: 'Ítem'),
                items: [
                  for (final it in _items)
                    DropdownMenuItem(
                      value: it,
                      enabled: it.disponible,
                      child: Text('${it.nombre} · ${pesos(it.precio)}',
                          maxLines: 1, overflow: TextOverflow.ellipsis),
                    ),
                ],
                onChanged: (it) {
                  if (it != null) _elegir(it);
                },
              ),
            for (final g in _grupos) ...[
              const SizedBox(height: 10),
              Text(g.obligatorio ? '${g.nombre} · obligatorio' : g.nombre,
                  style: RegentaType.etiqueta.copyWith(fontSize: 9.5)),
              for (final o in g.opciones)
                CheckboxListTile(
                  key: Key('anadir-mod-${o.id}'),
                  dense: true,
                  contentPadding: EdgeInsets.zero,
                  controlAffinity: ListTileControlAffinity.leading,
                  value: _mods.contains(o.id),
                  title: Text(o.precioExtra > 0
                      ? '${o.nombre}  +${pesos(o.precioExtra)}'
                      : o.nombre),
                  onChanged: (v) => setState(() {
                    if (v == true) {
                      _mods.add(o.id);
                    } else {
                      _mods.remove(o.id);
                    }
                  }),
                ),
            ],
            const SizedBox(height: 10),
            TextField(
              key: const Key('anadir-cantidad'),
              controller: _cantidad,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Cantidad'),
            ),
            const SizedBox(height: 8),
            TextField(
              key: const Key('anadir-notas'),
              controller: _notas,
              decoration: const InputDecoration(labelText: 'Notas (ej. sin cebolla)'),
            ),
            if (_error != null) ...[
              const SizedBox(height: 10),
              Text(_error!,
                  key: const Key('anadir-error'),
                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.crit)),
            ],
            const SizedBox(height: 14),
            FilledButton(
              key: const Key('anadir-guardar'),
              onPressed: _enviando ? null : _agregar,
              child: const Text('Agregar línea'),
            ),
          ],
          ),
        ),
      ),
    );
  }
}
