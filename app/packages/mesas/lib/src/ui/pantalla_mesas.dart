import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/mesa_en_plano.dart';
import '../datos/plano_del_salon.dart';
import '../plano/estado_del_plano.dart';
import '../plano/proveedores.dart';
import 'formato.dart';

/// El plano del salón en móvil y en web (HU-081). **Un solo widget** que se
/// adapta con `LayoutBuilder` en [kBreakpointEscritorio]. En modo lectura solo
/// se ve; en modo edición se crean, mueven y borran mesas.
class PantallaMesas extends ConsumerStatefulWidget {
  const PantallaMesas({super.key, this.puedeEditar = true});

  /// Si `false`, no se ve el botón de editar (equivale a no tener
  /// `MESAS_MESA_EDITAR`).
  final bool puedeEditar;

  @override
  ConsumerState<PantallaMesas> createState() => _PantallaMesasState();
}

class _PantallaMesasState extends ConsumerState<PantallaMesas> {
  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDelPlanoProvider);
    final ctrl = ref.read(controladorDelPlanoProvider.notifier);

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text('Mesas',
            style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
        actions: [
          if (widget.puedeEditar)
            TextButton(
              key: const Key('mesas-toggle-edicion'),
              onPressed: ctrl.alternarEdicion,
              child: Text(estado.modoEdicion ? 'Listo' : 'Editar plano',
                  style: RegentaType.item.copyWith(color: RegentaColors.accent)),
            ),
        ],
      ),
      floatingActionButton: estado.modoEdicion
          ? FloatingActionButton.extended(
              key: const Key('mesas-anadir'),
              backgroundColor: RegentaColors.accent,
              onPressed: () => _abrirFormularioMesa(context, estado),
              icon: const Icon(Icons.add, color: Colors.white),
              label: Text('Añadir mesa',
                  style: RegentaType.item.copyWith(color: Colors.white)),
            )
          : null,
      body: LayoutBuilder(
        builder: (context, restricciones) {
          final escritorio = restricciones.maxWidth >= kBreakpointEscritorio;
          final cuerpo = _Cuerpo(
            estado: estado,
            anchoTarjeta: escritorio ? 150 : (restricciones.maxWidth - 28 - 18) / 3,
            onMover: ctrl.moverMesa,
            onEliminar: (mesa) => _confirmarEliminar(context, ctrl, mesa),
            onCerrarMensaje: ctrl.limpiarMensaje,
          );
          return KeyedSubtree(
            key: Key(escritorio ? 'mesas-escritorio' : 'mesas-movil'),
            child: escritorio
                ? Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 900),
                      child: cuerpo,
                    ),
                  )
                : cuerpo,
          );
        },
      ),
    );
  }

  Future<void> _abrirFormularioMesa(BuildContext context, EstadoDelPlano estado) async {
    await showDialog<void>(
      context: context,
      builder: (_) => _FormularioMesa(
        zonas: estado.plano.zonas,
        onCrear: ({
          required String codigo,
          String? zonaId,
          String? nombre,
          int? capacidad,
          String? forma,
        }) =>
            ref.read(controladorDelPlanoProvider.notifier).crearMesa(
                  codigo: codigo,
                  zonaId: zonaId,
                  nombre: nombre,
                  capacidad: capacidad,
                  forma: forma,
                ),
      ),
    );
  }

  Future<void> _confirmarEliminar(
      BuildContext context, dynamic ctrl, MesaEnPlano mesa) async {
    final ok = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: Text('Borrar ${mesa.codigo}',
            style: RegentaType.seccion.copyWith(fontSize: 16)),
        content: Text('La mesa sale del plano. Se puede volver a crear con otro código.',
            style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
        actions: [
          TextButton(
              onPressed: () => Navigator.of(context).pop(false),
              child: const Text('Cancelar')),
          FilledButton(
              key: const Key('mesas-confirmar-borrado'),
              onPressed: () => Navigator.of(context).pop(true),
              child: const Text('Borrar')),
        ],
      ),
    );
    if (ok == true) {
      await ctrl.eliminarMesa(mesa.id);
    }
  }
}

class _Cuerpo extends StatelessWidget {
  const _Cuerpo({
    required this.estado,
    required this.anchoTarjeta,
    required this.onMover,
    required this.onEliminar,
    required this.onCerrarMensaje,
  });

  final EstadoDelPlano estado;
  final double anchoTarjeta;
  final void Function(String mesaId, int posX, int posY) onMover;
  final void Function(MesaEnPlano mesa) onEliminar;
  final VoidCallback onCerrarMensaje;

  @override
  Widget build(BuildContext context) {
    if (estado.cargando && estado.plano.estaVacio) {
      return const Center(child: CircularProgressIndicator());
    }
    if (estado.errorAlCargar && estado.plano.estaVacio) {
      return Center(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('No se pudo cargar el plano',
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
            const SizedBox(height: 10),
            FilledButton(
              key: const Key('mesas-reintentar'),
              onPressed: () => ProviderScope.containerOf(context)
                  .read(controladorDelPlanoProvider.notifier)
                  .cargar(),
              child: const Text('Reintentar'),
            ),
          ],
        ),
      );
    }
    if (estado.plano.estaVacio) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Text('Todavía no hay mesas en el salón',
              textAlign: TextAlign.center,
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
        ),
      );
    }

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        if (estado.mensaje != null && !estado.errorAlCargar)
          _AvisoMensaje(mensaje: estado.mensaje!, onCerrar: onCerrarMensaje),
        _ChipsDeZona(estado: estado),
        Expanded(
          child: ListView(
            key: const Key('mesas-plano'),
            padding: const EdgeInsets.fromLTRB(14, 4, 14, 90),
            children: _secciones(),
          ),
        ),
      ],
    );
  }

  List<Widget> _secciones() {
    final filtro = estado.zonaFiltro;
    final bloques = <Widget>[];

    void seccion(String titulo, List<MesaEnPlano> mesas) {
      if (filtro != null) {
        // Con filtro puesto, solo la sección elegida.
      }
      bloques.add(_Seccion(
        titulo: titulo,
        mesas: mesas,
        anchoTarjeta: anchoTarjeta,
        modoEdicion: estado.modoEdicion,
        onMover: onMover,
        onEliminar: onEliminar,
      ));
    }

    for (final z in estado.plano.zonas) {
      if (filtro != null && filtro.isNotEmpty && filtro != z.id) continue;
      if (filtro != null && filtro.isEmpty) continue;
      seccion(z.nombre, z.mesas);
    }
    final sinZona = estado.plano.sinZona;
    if (sinZona.isNotEmpty && (filtro == null || filtro.isEmpty)) {
      seccion('Sin zona', sinZona);
    }
    if (bloques.isEmpty) {
      bloques.add(Padding(
        padding: const EdgeInsets.all(24),
        child: Text('Ninguna mesa en esta zona',
            textAlign: TextAlign.center,
            style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
      ));
    }
    return bloques;
  }
}

class _ChipsDeZona extends ConsumerWidget {
  const _ChipsDeZona({required this.estado});

  final EstadoDelPlano estado;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final ctrl = ref.read(controladorDelPlanoProvider.notifier);
    final chips = <({String? id, String nombre})>[
      (id: null, nombre: 'Todas'),
      for (final z in estado.zonasParaChips) (id: z.id, nombre: z.nombre),
      if (estado.plano.sinZona.isNotEmpty) (id: '', nombre: 'Sin zona'),
    ];
    return Container(
      color: RegentaColors.surface,
      padding: const EdgeInsets.fromLTRB(14, 11, 14, 11),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            for (final c in chips) ...[
              _ChipZona(
                nombre: c.nombre,
                seleccionado: estado.zonaFiltro == c.id,
                onTap: () => ctrl.filtrarPorZona(c.id),
              ),
              const SizedBox(width: 7),
            ],
          ],
        ),
      ),
    );
  }
}

class _ChipZona extends StatelessWidget {
  const _ChipZona({
    required this.nombre,
    required this.seleccionado,
    required this.onTap,
  });

  final String nombre;
  final bool seleccionado;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      button: true,
      selected: seleccionado,
      label: nombre,
      excludeSemantics: true,
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(3),
        child: ConstrainedBox(
          constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
          child: Align(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
              decoration: BoxDecoration(
                color: seleccionado ? RegentaColors.comanda : RegentaColors.sunken,
                borderRadius: BorderRadius.circular(3),
              ),
              child: Text(
                nombre.toUpperCase(),
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

class _Seccion extends StatelessWidget {
  const _Seccion({
    required this.titulo,
    required this.mesas,
    required this.anchoTarjeta,
    required this.modoEdicion,
    required this.onMover,
    required this.onEliminar,
  });

  final String titulo;
  final List<MesaEnPlano> mesas;
  final double anchoTarjeta;
  final bool modoEdicion;
  final void Function(String mesaId, int posX, int posY) onMover;
  final void Function(MesaEnPlano mesa) onEliminar;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(0, 16, 0, 9),
          child: Text(titulo.toUpperCase(),
              style: RegentaType.etiqueta.copyWith(fontSize: 9.5, letterSpacing: 0.8)),
        ),
        Wrap(
          spacing: 9,
          runSpacing: 9,
          children: [
            for (final m in mesas)
              SizedBox(
                width: anchoTarjeta.clamp(96, 220),
                child: _TarjetaMesa(
                  mesa: m,
                  modoEdicion: modoEdicion,
                  onMover: onMover,
                  onEliminar: () => onEliminar(m),
                ),
              ),
          ],
        ),
      ],
    );
  }
}

class _TarjetaMesa extends StatefulWidget {
  const _TarjetaMesa({
    required this.mesa,
    required this.modoEdicion,
    required this.onMover,
    required this.onEliminar,
  });

  final MesaEnPlano mesa;
  final bool modoEdicion;
  final void Function(String mesaId, int posX, int posY) onMover;
  final VoidCallback onEliminar;

  @override
  State<_TarjetaMesa> createState() => _TarjetaMesaState();
}

class _TarjetaMesaState extends State<_TarjetaMesa> {
  Offset _arrastre = Offset.zero;

  @override
  Widget build(BuildContext context) {
    final estilo = estiloDeEstado(widget.mesa.estado);
    final tarjeta = Container(
      key: Key('mesa-${widget.mesa.id}'),
      height: 92,
      decoration: BoxDecoration(
        color: estilo.fondo,
        border: Border.all(color: estilo.borde, width: 1.5),
        borderRadius: BorderRadius.circular(7),
      ),
      padding: const EdgeInsets.all(6),
      child: Stack(
        children: [
          Center(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(widget.mesa.codigo,
                    style: const TextStyle(
                        fontFamily: RegentaType.mono,
                        fontSize: 16,
                        fontWeight: FontWeight.w700,
                        color: RegentaColors.ink)),
                const SizedBox(height: 1),
                Text('${widget.mesa.capacidad} pax',
                    style: RegentaType.codigo
                        .copyWith(fontSize: 9.5, color: RegentaColors.muted)),
                const SizedBox(height: 5),
                Text(estilo.etiqueta,
                    style: RegentaType.codigo.copyWith(fontSize: 10, color: estilo.borde)),
              ],
            ),
          ),
          if (widget.modoEdicion)
            Positioned(
              top: -2,
              right: -2,
              child: InkWell(
                key: Key('mesa-borrar-${widget.mesa.id}'),
                onTap: widget.onEliminar,
                child: Container(
                  width: 22,
                  height: 22,
                  decoration: const BoxDecoration(
                      color: RegentaColors.surface, shape: BoxShape.circle),
                  child: const Icon(Icons.close, size: 14, color: RegentaColors.comanda),
                ),
              ),
            ),
        ],
      ),
    );

    if (!widget.modoEdicion) return tarjeta;

    return GestureDetector(
      onPanStart: (_) => _arrastre = Offset.zero,
      onPanUpdate: (d) => _arrastre += d.delta,
      onPanEnd: (_) {
        final nx = (widget.mesa.posX + _arrastre.dx).round();
        final ny = (widget.mesa.posY + _arrastre.dy).round();
        widget.onMover(widget.mesa.id, nx, ny);
      },
      child: tarjeta,
    );
  }
}

class _AvisoMensaje extends StatelessWidget {
  const _AvisoMensaje({required this.mensaje, required this.onCerrar});

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
                key: const Key('mesas-aviso'),
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

typedef _AltaDeMesa = Future<String?> Function({
  required String codigo,
  String? zonaId,
  String? nombre,
  int? capacidad,
  String? forma,
});

class _FormularioMesa extends StatefulWidget {
  const _FormularioMesa({
    required this.zonas,
    required this.onCrear,
  });

  final List<ZonaConMesas> zonas;
  final _AltaDeMesa onCrear;

  @override
  State<_FormularioMesa> createState() => _FormularioMesaState();
}

class _FormularioMesaState extends State<_FormularioMesa> {
  final _codigo = TextEditingController();
  final _nombre = TextEditingController();
  final _capacidad = TextEditingController(text: '4');
  String? _zonaId;
  String _forma = 'CUADRADA';
  String? _error;
  bool _enviando = false;

  @override
  void dispose() {
    _codigo.dispose();
    _nombre.dispose();
    _capacidad.dispose();
    super.dispose();
  }

  Future<void> _enviar() async {
    final codigo = _codigo.text.trim();
    if (codigo.isEmpty) {
      setState(() => _error = 'La mesa necesita un código');
      return;
    }
    setState(() {
      _enviando = true;
      _error = null;
    });
    final error = await widget.onCrear(
      codigo: codigo,
      zonaId: _zonaId,
      nombre: _nombre.text.trim().isEmpty ? null : _nombre.text.trim(),
      capacidad: int.tryParse(_capacidad.text.trim()),
      forma: _forma,
    );
    if (!mounted) return;
    if (error == null) {
      Navigator.of(context).pop();
    } else {
      setState(() {
        _enviando = false;
        _error = error;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      title: Text('Nueva mesa', style: RegentaType.seccion.copyWith(fontSize: 16)),
      content: SingleChildScrollView(
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            TextField(
              key: const Key('mesa-form-codigo'),
              controller: _codigo,
              textCapitalization: TextCapitalization.characters,
              decoration: const InputDecoration(labelText: 'Código'),
            ),
            const SizedBox(height: 8),
            TextField(
              key: const Key('mesa-form-nombre'),
              controller: _nombre,
              decoration: const InputDecoration(labelText: 'Nombre (opcional)'),
            ),
            const SizedBox(height: 8),
            TextField(
              key: const Key('mesa-form-capacidad'),
              controller: _capacidad,
              keyboardType: TextInputType.number,
              decoration: const InputDecoration(labelText: 'Capacidad'),
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<String?>(
              key: const Key('mesa-form-zona'),
              initialValue: _zonaId,
              decoration: const InputDecoration(labelText: 'Zona'),
              items: [
                const DropdownMenuItem(value: null, child: Text('Sin zona')),
                for (final z in widget.zonas)
                  DropdownMenuItem(value: z.id, child: Text(z.nombre)),
              ],
              onChanged: (v) => setState(() => _zonaId = v),
            ),
            const SizedBox(height: 8),
            DropdownButtonFormField<String>(
              key: const Key('mesa-form-forma'),
              initialValue: _forma,
              decoration: const InputDecoration(labelText: 'Forma'),
              items: const [
                DropdownMenuItem(value: 'CUADRADA', child: Text('Cuadrada')),
                DropdownMenuItem(value: 'REDONDA', child: Text('Redonda')),
                DropdownMenuItem(value: 'RECTANGULAR', child: Text('Rectangular')),
                DropdownMenuItem(value: 'BARRA', child: Text('Barra')),
              ],
              onChanged: (v) => setState(() => _forma = v ?? 'CUADRADA'),
            ),
            if (_error != null) ...[
              const SizedBox(height: 10),
              Text(_error!,
                  key: const Key('mesa-form-error'),
                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.crit)),
            ],
          ],
        ),
      ),
      actions: [
        TextButton(
            onPressed: _enviando ? null : () => Navigator.of(context).pop(),
            child: const Text('Cancelar')),
        FilledButton(
          key: const Key('mesa-form-guardar'),
          onPressed: _enviando ? null : _enviar,
          child: const Text('Crear'),
        ),
      ],
    );
  }
}
