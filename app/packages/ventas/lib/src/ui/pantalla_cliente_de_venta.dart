import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:uuid/uuid.dart';

import '../cliente/estado_de_cliente.dart';
import '../cliente/proveedores.dart';
import '../datos/cliente_de_la_venta.dart';

/// Quién compra en esta venta. HU-113 y HU-114.
///
/// Un solo widget con dos ramas en [kBreakpointEscritorio]: en móvil es una
/// pantalla completa con vuelta atrás; en escritorio, el mismo contenido en un
/// panel sobre el POS, sin perder de vista el carrito.
///
/// *Consumidor final* queda fijo arriba y marcado cuando la venta no tiene
/// cliente: vender sin cliente es el camino corto y no puede costar un paso
/// extra.
class PantallaClienteDeVenta extends ConsumerStatefulWidget {
  const PantallaClienteDeVenta({
    super.key,
    this.asignado,
    this.onAsignado,
    this.onCerrar,
  });

  /// El cliente que la venta ya traía, si lo tenía.
  final ClienteDeLaVenta? asignado;

  /// Se llama con el cliente elegido, o con `null` si se quitó.
  final void Function(ClienteDeLaVenta?)? onAsignado;

  final VoidCallback? onCerrar;

  @override
  ConsumerState<PantallaClienteDeVenta> createState() => _PantallaClienteDeVentaState();
}

class _PantallaClienteDeVentaState extends ConsumerState<PantallaClienteDeVenta> {
  final TextEditingController _buscador = TextEditingController();

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      ref.read(controladorDeClienteProvider.notifier).sembrar(widget.asignado);
    });
  }

  @override
  void dispose() {
    _buscador.dispose();
    super.dispose();
  }

  Future<void> _asignar(ClienteDeLaVenta cliente) async {
    final elegido =
        await ref.read(controladorDeClienteProvider.notifier).asignar(cliente);
    if (elegido != null) widget.onAsignado?.call(elegido);
  }

  Future<void> _quitar() async {
    await ref.read(controladorDeClienteProvider.notifier).quitar();
    if (!ref.read(controladorDeClienteProvider).esConsumidorFinal) return;
    widget.onAsignado?.call(null);
  }

  Future<void> _crear(ClienteNuevo nuevo) async {
    final creado =
        await ref.read(controladorDeClienteProvider.notifier).crearYAsignar(nuevo);
    if (creado != null) widget.onAsignado?.call(creado);
  }

  @override
  Widget build(BuildContext context) {
    final estado = ref.watch(controladorDeClienteProvider);
    final puedeCrear = ref.watch(puedeCrearClientesProvider);

    ref.listen<EstadoDeCliente>(controladorDeClienteProvider, (antes, ahora) {
      if (ahora.mensaje != null && ahora.mensaje != antes?.mensaje) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(ahora.mensaje!)));
      }
    });

    return LayoutBuilder(
      builder: (context, restricciones) {
        final escritorio = restricciones.maxWidth >= kBreakpointEscritorio;
        final lista = _Lista(
          estado: estado,
          onElegir: _asignar,
          onConsumidorFinal: estado.esConsumidorFinal ? null : _quitar,
        );
        final buscador = _Buscador(
          controlador: _buscador,
          onCambio: ref.read(controladorDeClienteProvider.notifier).cambiarTermino,
        );
        final alta = estado.creando
            ? _FormularioDeAlta(
                onCrear: _crear,
                onCancelar: ref.read(controladorDeClienteProvider.notifier).cerrarAlta,
                trabajando: estado.trabajando,
              )
            : null;

        if (escritorio) {
          return Scaffold(
            key: const Key('cliente-venta-escritorio'),
            backgroundColor: RegentaColors.paper,
            body: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Expanded(
                  child: Column(children: [
                    _Encabezado(onCerrar: widget.onCerrar, mostrarVolver: false),
                    buscador,
                    Expanded(child: lista),
                    if (puedeCrear && alta == null)
                      _BotonCrear(
                          onCrear: ref.read(controladorDeClienteProvider.notifier).abrirAlta),
                  ]),
                ),
                if (alta != null) ...[
                  const VerticalDivider(width: 1, color: RegentaColors.line),
                  SizedBox(width: 380, child: alta),
                ],
              ],
            ),
          );
        }

        return Scaffold(
          key: const Key('cliente-venta-movil'),
          backgroundColor: RegentaColors.paper,
          body: SafeArea(
            child: Column(children: [
              _Encabezado(onCerrar: widget.onCerrar, mostrarVolver: true),
              if (alta != null)
                Expanded(child: alta)
              else ...[
                buscador,
                Expanded(child: lista),
                if (puedeCrear)
                  _BotonCrear(
                      onCrear: ref.read(controladorDeClienteProvider.notifier).abrirAlta),
              ],
            ]),
          ),
        );
      },
    );
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({required this.onCerrar, required this.mostrarVolver});

  final VoidCallback? onCerrar;
  final bool mostrarVolver;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 56,
      padding: const EdgeInsets.symmetric(horizontal: 14),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(children: [
        if (mostrarVolver)
          Semantics(
            button: true,
            label: 'Volver a la venta',
            child: InkWell(
              onTap: onCerrar,
              child: const SizedBox(
                width: RegentaSpacing.hitTarget,
                height: RegentaSpacing.hitTarget,
                child: Icon(Icons.arrow_back, size: 22, color: RegentaColors.ink),
              ),
            ),
          ),
        Expanded(
          child: Text('Cliente de la venta',
              style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
        ),
      ]),
    );
  }
}

class _Buscador extends StatelessWidget {
  const _Buscador({required this.controlador, required this.onCambio});

  final TextEditingController controlador;
  final ValueChanged<String> onCambio;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(14, 12, 14, 12),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: SizedBox(
        height: 46,
        child: TextField(
          controller: controlador,
          autofocus: true,
          onChanged: onCambio,
          style: RegentaType.cuerpo.copyWith(fontSize: 14, color: RegentaColors.ink),
          decoration: InputDecoration(
            isDense: true,
            prefixIcon: const Icon(Icons.search, size: 18, color: RegentaColors.faint),
            hintText: 'Buscar por nombre, NIT o cédula…',
            hintStyle:
                RegentaType.cuerpo.copyWith(fontSize: 14, color: RegentaColors.faint),
            contentPadding: const EdgeInsets.symmetric(vertical: 13),
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
    );
  }
}

class _Lista extends StatelessWidget {
  const _Lista({
    required this.estado,
    required this.onElegir,
    required this.onConsumidorFinal,
  });

  final EstadoDeCliente estado;
  final void Function(ClienteDeLaVenta) onElegir;

  /// `null` cuando ya está en consumidor final: no hay nada que quitar.
  final Future<void> Function()? onConsumidorFinal;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.fromLTRB(14, 14, 14, 14),
      children: [
        _FilaConsumidorFinal(
          seleccionado: estado.esConsumidorFinal,
          onQuitar: onConsumidorFinal,
        ),
        Padding(
          padding: const EdgeInsets.fromLTRB(0, 18, 0, 2),
          child: Text('CLIENTES DEL NEGOCIO',
              style: RegentaType.etiqueta.copyWith(fontSize: 9.5)),
        ),
        if (estado.resultados.isEmpty && !estado.buscando)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 16),
            child: Text(
              estado.termino.isEmpty
                  ? 'Este negocio todavía no tiene clientes. Se puede vender así, a consumidor final.'
                  : 'No hay ningún cliente con ese nombre o documento.',
              style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink2),
            ),
          ),
        for (final cliente in estado.resultados)
          _FilaDeCliente(
            cliente: cliente,
            seleccionado: estado.asignado?.id == cliente.id,
            onElegir: () => onElegir(cliente),
          ),
        const SizedBox(height: 12),
        const _Nota(),
      ],
    );
  }
}

class _FilaConsumidorFinal extends StatelessWidget {
  const _FilaConsumidorFinal({required this.seleccionado, required this.onQuitar});

  final bool seleccionado;
  final Future<void> Function()? onQuitar;

  @override
  Widget build(BuildContext context) {
    final fila = Container(
      constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: seleccionado ? RegentaColors.accentSoft : RegentaColors.surface,
        border: Border.all(
            color: seleccionado ? RegentaColors.accent : RegentaColors.line2),
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      child: Row(children: [
        Icon(Icons.people_outline,
            size: 19,
            color: seleccionado ? RegentaColors.accent : RegentaColors.muted),
        const SizedBox(width: 11),
        Expanded(
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            mainAxisSize: MainAxisSize.min,
            children: [
              Text('Consumidor final',
                  style: RegentaType.item
                      .copyWith(fontSize: 14, color: RegentaColors.ink)),
              const SizedBox(height: 2),
              Text('sin identificar · por defecto',
                  style: RegentaType.codigo
                      .copyWith(fontSize: 10, color: RegentaColors.muted)),
            ],
          ),
        ),
        if (seleccionado)
          const Icon(Icons.check, size: 19, color: RegentaColors.accent),
      ]),
    );

    if (seleccionado) {
      return Semantics(
        label: 'Consumidor final, seleccionado',
        container: true,
        excludeSemantics: true,
        child: fila,
      );
    }
    return Semantics(
      button: true,
      label: 'Quitar el cliente de la venta',
      container: true,
      excludeSemantics: true,
      child: InkWell(onTap: () => onQuitar?.call(), child: fila),
    );
  }
}

class _FilaDeCliente extends StatelessWidget {
  const _FilaDeCliente({
    required this.cliente,
    required this.seleccionado,
    required this.onElegir,
  });

  final ClienteDeLaVenta cliente;
  final bool seleccionado;
  final VoidCallback onElegir;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onElegir,
      child: Container(
        constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
        padding: const EdgeInsets.symmetric(vertical: 11),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: RegentaColors.line)),
        ),
        child: Row(children: [
          Container(
            width: 34,
            height: 34,
            alignment: Alignment.center,
            decoration: const BoxDecoration(
                color: RegentaColors.paper, shape: BoxShape.circle),
            child: Text(cliente.iniciales,
                style: RegentaType.codigo
                    .copyWith(fontSize: 12, color: RegentaColors.ink2)),
          ),
          const SizedBox(width: 11),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(cliente.nombre,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.item
                        .copyWith(fontSize: 13.5, color: RegentaColors.ink)),
                const SizedBox(height: 3),
                Row(children: [
                  Flexible(
                    child: Text(cliente.documento,
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: RegentaType.codigo
                            .copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                  ),
                  if (cliente.condicion != null) ...[
                    const SizedBox(width: 8),
                    _Chip(texto: cliente.condicion!, alerta: cliente.tieneCarteraVencida),
                  ],
                ]),
              ],
            ),
          ),
          Icon(seleccionado ? Icons.check : Icons.chevron_right,
              size: 16,
              color: seleccionado ? RegentaColors.accent : RegentaColors.faint),
        ]),
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({required this.texto, required this.alerta});

  final String texto;
  final bool alerta;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: alerta ? RegentaColors.critSoft : RegentaColors.paper,
        borderRadius: BorderRadius.circular(3),
      ),
      child: Text(texto.toUpperCase(),
          style: RegentaType.etiqueta.copyWith(
              fontSize: 9.5, color: alerta ? RegentaColors.crit : RegentaColors.muted)),
    );
  }
}

class _Nota extends StatelessWidget {
  const _Nota();

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(11),
      decoration: BoxDecoration(
        color: RegentaColors.paper,
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text('Para facturar con datos',
            style: RegentaType.item.copyWith(fontSize: 12, color: RegentaColors.ink)),
        const SizedBox(height: 4),
        Text(
          'Basta tipo y número de documento, nombre o razón social y el correo '
          'donde llega la factura. Lo demás se completa después en la ficha del cliente.',
          style: RegentaType.cuerpo.copyWith(fontSize: 11, color: RegentaColors.ink2),
        ),
      ]),
    );
  }
}

class _BotonCrear extends StatelessWidget {
  const _BotonCrear({required this.onCrear});

  final VoidCallback onCrear;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.fromLTRB(14, 13, 14, 13),
      decoration: const BoxDecoration(
        color: RegentaColors.surface,
        border: Border(top: BorderSide(color: RegentaColors.line)),
      ),
      child: Semantics(
        button: true,
        label: 'Crear cliente nuevo',
        child: SizedBox(
          height: 52,
          child: FilledButton.icon(
            onPressed: onCrear,
            style: FilledButton.styleFrom(
              backgroundColor: RegentaColors.accent,
              foregroundColor: RegentaColors.surface,
              shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
            ),
            icon: const Icon(Icons.add, size: 18),
            label: Text('Crear cliente nuevo',
                style: RegentaType.item
                    .copyWith(fontSize: 14, color: RegentaColors.surface)),
          ),
        ),
      ),
    );
  }
}

/// Lo mínimo para facturar, y nada más (HU-114). Lo que sobra se completa
/// después en la ficha: aquí hay alguien esperando en el mostrador.
class _FormularioDeAlta extends StatefulWidget {
  const _FormularioDeAlta({
    required this.onCrear,
    required this.onCancelar,
    required this.trabajando,
  });

  final void Function(ClienteNuevo) onCrear;
  final VoidCallback onCancelar;
  final bool trabajando;

  @override
  State<_FormularioDeAlta> createState() => _FormularioDeAltaState();
}

class _FormularioDeAltaState extends State<_FormularioDeAlta> {
  final _numero = TextEditingController();
  final _dv = TextEditingController();
  final _nombre = TextEditingController();
  final _email = TextEditingController();
  final _telefono = TextEditingController();

  String _tipoDocumento = 'NIT';
  String? _error;

  /// El DV solo lo toca la fórmula mientras nadie lo haya escrito a mano.
  bool _dvAMano = false;

  @override
  void initState() {
    super.initState();
    _numero.addListener(_recalcularDv);
    _dv.addListener(() {
      // Si el cambio no vino de la fórmula, manda la persona.
      if (_dv.text != (digitoDeVerificacion(_numero.text) ?? '')) _dvAMano = true;
    });
  }

  @override
  void dispose() {
    _numero.dispose();
    _dv.dispose();
    _nombre.dispose();
    _email.dispose();
    _telefono.dispose();
    super.dispose();
  }

  /// HU-114 criterio 3.
  void _recalcularDv() {
    if (_tipoDocumento != 'NIT' || _dvAMano) return;
    final calculado = digitoDeVerificacion(_numero.text) ?? '';
    if (_dv.text != calculado) _dv.text = calculado;
  }

  void _enviar() {
    final numero = _numero.text.trim();
    final nombre = _nombre.text.trim();
    final email = _email.text.trim();
    if (numero.isEmpty) return setState(() => _error = 'Falta el número de documento');
    if (nombre.isEmpty) return setState(() => _error = 'Falta el nombre o la razón social');
    if (!email.contains('@')) {
      return setState(() => _error = 'El correo no parece válido: ahí llega la factura');
    }
    setState(() => _error = null);
    widget.onCrear(ClienteNuevo(
      id: const Uuid().v4(),
      tipoPersona: _tipoDocumento == 'NIT' ? 'JURIDICA' : 'NATURAL',
      tipoDocumento: _tipoDocumento,
      numeroDocumento: numero,
      nombre: nombre,
      email: email,
      digitoVerificacion: _tipoDocumento == 'NIT' && _dv.text.isNotEmpty ? _dv.text : null,
      telefono: _telefono.text.trim().isEmpty ? null : _telefono.text.trim(),
    ));
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      color: RegentaColors.surface,
      padding: const EdgeInsets.all(14),
      child: ListView(children: [
        Text('Crear cliente',
            style: RegentaType.seccion.copyWith(fontSize: 15, color: RegentaColors.ink)),
        const SizedBox(height: 2),
        Text('crm.clientes · queda disponible para las próximas ventas',
            style: RegentaType.codigo.copyWith(fontSize: 10, color: RegentaColors.muted)),
        const SizedBox(height: 14),
        _Campo(
          etiqueta: 'Documento',
          hijo: DropdownButtonFormField<String>(
            initialValue: _tipoDocumento,
            decoration: _decoracion(),
            // Sin esto, «Cédula de extranjería» no cabe en el panel de 380 px
            // y el campo se desborda en vez de recortar el texto.
            isExpanded: true,
            items: const [
              DropdownMenuItem(value: 'NIT', child: Text('NIT')),
              DropdownMenuItem(value: 'CC', child: Text('Cédula')),
              DropdownMenuItem(
                  value: 'CE',
                  child: Text('Cédula de extranjería', overflow: TextOverflow.ellipsis)),
            ],
            onChanged: (v) => setState(() {
              _tipoDocumento = v ?? 'NIT';
              _dvAMano = false;
              if (_tipoDocumento != 'NIT') {
                _dv.text = '';
              } else {
                _recalcularDv();
              }
            }),
          ),
        ),
        Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
          Expanded(
            flex: 3,
            child: _Campo(
              etiqueta: 'Número *',
              hijo: Semantics(
                textField: true,
                label: 'Número de documento',
                child: TextField(
                    key: const Key('campo-numero-documento'),
                    controller: _numero,
                    keyboardType: TextInputType.number,
                    decoration: _decoracion()),
              ),
            ),
          ),
          if (_tipoDocumento == 'NIT') ...[
            const SizedBox(width: 10),
            Expanded(
              child: _Campo(
                etiqueta: 'DV',
                hijo: Semantics(
                  textField: true,
                  label: 'Dígito de verificación',
                  child: TextField(
                      key: const Key('campo-dv'),
                      controller: _dv,
                      decoration: _decoracion()),
                ),
              ),
            ),
          ],
        ]),
        _Campo(
          etiqueta: _tipoDocumento == 'NIT' ? 'Razón social *' : 'Nombre *',
          hijo: Semantics(
            textField: true,
            label: 'Nombre o razón social',
            child: TextField(
                key: const Key('campo-nombre'),
                controller: _nombre,
                decoration: _decoracion()),
          ),
        ),
        _Campo(
          etiqueta: 'Correo *',
          ayuda: 'Ahí llega la factura electrónica',
          hijo: Semantics(
            textField: true,
            label: 'Correo',
            child: TextField(
                key: const Key('campo-correo'),
                controller: _email,
                keyboardType: TextInputType.emailAddress,
                decoration: _decoracion()),
          ),
        ),
        _Campo(
          etiqueta: 'Teléfono',
          hijo: Semantics(
            textField: true,
            label: 'Teléfono',
            child: TextField(
                key: const Key('campo-telefono'),
                controller: _telefono,
                keyboardType: TextInputType.phone,
                decoration: _decoracion()),
          ),
        ),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(bottom: 8),
            child: Text(_error!,
                style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.crit)),
          ),
        Row(children: [
          Expanded(
            child: OutlinedButton(
              onPressed: widget.trabajando ? null : widget.onCancelar,
              child: const Text('Cancelar'),
            ),
          ),
          const SizedBox(width: 10),
          Expanded(
            child: SizedBox(
              height: RegentaSpacing.hitTarget,
              child: FilledButton(
                onPressed: widget.trabajando ? null : _enviar,
                style: FilledButton.styleFrom(
                  backgroundColor: RegentaColors.accent,
                  foregroundColor: RegentaColors.surface,
                  shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
                ),
                child: const Text('Crear y asignar'),
              ),
            ),
          ),
        ]),
      ]),
    );
  }

  InputDecoration _decoracion() => InputDecoration(
        isDense: true,
        contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RegentaSpacing.radius),
          borderSide: const BorderSide(color: RegentaColors.line2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RegentaSpacing.radius),
          borderSide: const BorderSide(color: RegentaColors.line2),
        ),
      );
}

class _Campo extends StatelessWidget {
  const _Campo({required this.etiqueta, required this.hijo, this.ayuda});

  final String etiqueta;
  final Widget hijo;
  final String? ayuda;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Text(etiqueta.toUpperCase(), style: RegentaType.etiqueta.copyWith(fontSize: 9.5)),
        const SizedBox(height: 4),
        hijo,
        if (ayuda != null) ...[
          const SizedBox(height: 3),
          Text(ayuda!,
              style: RegentaType.cuerpo.copyWith(fontSize: 11, color: RegentaColors.muted)),
        ],
      ]),
    );
  }
}
