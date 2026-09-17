import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../cuenta/controlador_de_division.dart';
import '../cuenta/estado_de_division.dart';
import '../cuenta/proveedores.dart';
import '../datos/comanda_vista.dart';
import '../datos/cuenta_vista.dart';
import 'formato.dart';

/// Dividir la cuenta entre comensales (HU-089): marca cada línea en una
/// cuenta —o en varias, si se comparte—, o repártela en partes iguales. **Un
/// solo widget** que se adapta con `LayoutBuilder` en [kBreakpointEscritorio]:
/// en escritorio, la rejilla de líneas por cuenta; en móvil, solo las
/// tarjetas de cuenta y una barra fija para cobrar.
class PantallaDividirCuenta extends ConsumerWidget {
  const PantallaDividirCuenta({super.key, required this.comandaId});

  final String comandaId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(controladorDeDivisionProvider(comandaId));
    final ctrl = ref.read(controladorDeDivisionProvider(comandaId).notifier);
    final comanda = estado.comanda;

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Text('Dividir cuenta',
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
          final esEscritorio = restricciones.maxWidth >= kBreakpointEscritorio;
          return Column(
            children: [
              if (estado.mensaje != null)
                _Aviso(mensaje: estado.mensaje!, onCerrar: ctrl.limpiarMensaje),
              _ChipsDeModo(onPartesIguales: () => _pedirPartesIguales(context, ctrl)),
              Expanded(
                child: KeyedSubtree(
                  key: Key(esEscritorio ? 'dividir-escritorio' : 'dividir-movil'),
                  child: esEscritorio
                      ? _LayoutEscritorio(
                          comanda: comanda,
                          estado: estado,
                          ctrl: ctrl,
                          onAnadirCuenta: () => _anadirCuenta(context, ctrl),
                        )
                      : _LayoutMovil(estado: estado, ctrl: ctrl),
                ),
              ),
            ],
          );
        });
      }),
    );
  }

  Future<void> _pedirPartesIguales(BuildContext context, ControladorDeDivision ctrl) async {
    final controlador = TextEditingController(text: '2');
    final n = await showDialog<int>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Dividir en partes iguales'),
        content: TextField(
          key: const Key('dividir-partes-input'),
          controller: controlador,
          keyboardType: TextInputType.number,
          decoration: const InputDecoration(labelText: 'Número de cuentas'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(), child: const Text('Cancelar')),
          FilledButton(
            key: const Key('dividir-partes-confirmar'),
            onPressed: () => Navigator.of(context).pop(int.tryParse(controlador.text)),
            child: const Text('Dividir'),
          ),
        ],
      ),
    );
    if (n != null && n >= 2) {
      await ctrl.dividirEnPartesIguales(n);
    }
  }

  Future<void> _anadirCuenta(BuildContext context, ControladorDeDivision ctrl) async {
    final controlador = TextEditingController();
    final confirmo = await showDialog<bool>(
      context: context,
      builder: (_) => AlertDialog(
        title: const Text('Añadir cuenta'),
        content: TextField(
          key: const Key('anadir-cuenta-etiqueta'),
          controller: controlador,
          decoration: const InputDecoration(labelText: 'Etiqueta (opcional, ej. Camilo)'),
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text('Cancelar')),
          FilledButton(
            key: const Key('anadir-cuenta-confirmar'),
            onPressed: () => Navigator.of(context).pop(true),
            child: const Text('Añadir'),
          ),
        ],
      ),
    );
    if (confirmo == true) {
      final etiqueta = controlador.text.trim();
      await ctrl.crearCuenta(etiqueta: etiqueta.isEmpty ? null : etiqueta);
    }
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
            key: const Key('dividir-reintentar'),
            onPressed: onReintentar,
            child: const Text('Reintentar'),
          ),
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
                key: const Key('dividir-aviso'),
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

class _ChipsDeModo extends StatelessWidget {
  const _ChipsDeModo({required this.onPartesIguales});
  final VoidCallback onPartesIguales;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(RegentaSpacing.lg, RegentaSpacing.md, RegentaSpacing.lg, 0),
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            const _Chip(texto: 'Por ítem', activo: true),
            const SizedBox(width: 7),
            _Chip(
                texto: 'Partes iguales',
                activo: false,
                onTap: onPartesIguales,
                key: const Key('dividir-partes-iguales')),
            const SizedBox(width: 7),
            const _Chip(texto: 'Monto fijo', activo: false),
          ],
        ),
      ),
    );
  }
}

class _Chip extends StatelessWidget {
  const _Chip({super.key, required this.texto, required this.activo, this.onTap});
  final String texto;
  final bool activo;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(3),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(
          color: activo ? RegentaColors.comanda : RegentaColors.sunken,
          borderRadius: BorderRadius.circular(3),
        ),
        child: Text(texto.toUpperCase(),
            style: RegentaType.etiqueta.copyWith(
                fontSize: 9.5, color: activo ? Colors.white : RegentaColors.ink2)),
      ),
    );
  }
}

class _LayoutEscritorio extends StatelessWidget {
  const _LayoutEscritorio({
    required this.comanda,
    required this.estado,
    required this.ctrl,
    required this.onAnadirCuenta,
  });

  final ComandaVista comanda;
  final EstadoDeDivision estado;
  final ControladorDeDivision ctrl;
  final VoidCallback onAnadirCuenta;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(RegentaSpacing.lg),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Align(
                  alignment: Alignment.centerRight,
                  child: OutlinedButton.icon(
                    key: const Key('dividir-anadir-cuenta'),
                    onPressed: onAnadirCuenta,
                    icon: const Icon(Icons.add, size: 18),
                    label: const Text('Añadir cuenta'),
                  ),
                ),
                const SizedBox(height: RegentaSpacing.md),
                Expanded(
                  child: estado.cuentas.isEmpty
                      ? const _SinCuentas()
                      : SingleChildScrollView(
                          child: _MatrizDeLineas(
                              comanda: comanda, cuentas: estado.cuentas, onAlternar: ctrl.alternarLinea),
                        ),
                ),
              ],
            ),
          ),
          const SizedBox(width: RegentaSpacing.lg),
          SizedBox(
            width: 320,
            child: SingleChildScrollView(
              child: Column(
                children: [
                  for (final c in estado.cuentas)
                    _TarjetaCuenta(cuenta: c, mostrarBotonCobrar: true, onCobrar: () => ctrl.cobrar(c.id)),
                  _ResumenFaltaPorCobrar(estado: estado),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _LayoutMovil extends StatelessWidget {
  const _LayoutMovil({required this.estado, required this.ctrl});
  final EstadoDeDivision estado;
  final ControladorDeDivision ctrl;

  @override
  Widget build(BuildContext context) {
    final pendientes = estado.cuentas.where((c) => !c.pagada).toList();
    return Column(
      children: [
        Expanded(
          child: estado.cuentas.isEmpty
              ? const _SinCuentas()
              : ListView(
                  padding: const EdgeInsets.all(RegentaSpacing.md),
                  children: [
                    for (final c in estado.cuentas)
                      _TarjetaCuenta(cuenta: c, mostrarBotonCobrar: false, onCobrar: () => ctrl.cobrar(c.id)),
                  ],
                ),
        ),
        if (pendientes.isNotEmpty)
          Container(
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
                    Expanded(
                      child: Text('Falta por cobrar',
                          style: RegentaType.seccion.copyWith(fontSize: 14, color: RegentaColors.ink)),
                    ),
                    Text(pesos(estado.faltaPorCobrar),
                        key: const Key('dividir-falta-por-cobrar'),
                        style: const TextStyle(
                            fontFamily: RegentaType.mono,
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                            color: RegentaColors.ink)),
                  ],
                ),
                const SizedBox(height: 10),
                FilledButton(
                  key: Key('dividir-cobrar-movil-${pendientes.first.id}'),
                  onPressed: () => ctrl.cobrar(pendientes.first.id),
                  child: Text('Cobrar ${pendientes.first.nombre.toLowerCase()}'),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _SinCuentas extends StatelessWidget {
  const _SinCuentas();

  @override
  Widget build(BuildContext context) {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Text('Todavía no hay cuentas. Añade una o divide en partes iguales.',
            textAlign: TextAlign.center,
            style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
      ),
    );
  }
}

class _MatrizDeLineas extends StatelessWidget {
  const _MatrizDeLineas({required this.comanda, required this.cuentas, required this.onAlternar});

  final ComandaVista comanda;
  final List<CuentaVista> cuentas;
  final void Function(String cuentaId, String lineaId) onAlternar;

  @override
  Widget build(BuildContext context) {
    final vivas = comanda.lineas.where((l) => l.estado != 'ANULADA').toList();
    return Container(
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        border: Border.all(color: RegentaColors.line),
        borderRadius: BorderRadius.circular(6),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          _fila(context, cabecera: true, children: [
            for (final c in cuentas) _celda(width: 32, texto: 'C${c.numeroDivision}', cabecera: true),
            _celda(width: 30, texto: 'Cant', cabecera: true),
            _celda(flex: 3, texto: 'Ítem', alinear: TextAlign.left, cabecera: true),
            _celda(width: 70, texto: 'Reparto', alinear: TextAlign.right, cabecera: true),
            _celda(width: 90, texto: 'Total', alinear: TextAlign.right, cabecera: true),
          ]),
          for (final l in vivas)
            _fila(context, children: [
              for (final c in cuentas)
                SizedBox(
                  width: 32,
                  child: Center(
                    child: Checkbox(
                      key: Key('dividir-check-${c.id}-${l.id}'),
                      value: c.proporcionDe(l.id) != null,
                      onChanged: c.pagada ? null : (_) => onAlternar(c.id, l.id),
                    ),
                  ),
                ),
              SizedBox(width: 30, child: Center(child: Text('${l.cantidad.round()}'))),
              Expanded(
                flex: 3,
                child: Text(l.nombre, overflow: TextOverflow.ellipsis, style: RegentaType.cuerpo),
              ),
              SizedBox(
                width: 70,
                child: Text(_repartoTexto(l.id), textAlign: TextAlign.right,
                    style: RegentaType.codigo.copyWith(color: RegentaColors.muted)),
              ),
              SizedBox(
                width: 90,
                child: Text(pesos(l.total), textAlign: TextAlign.right,
                    style: RegentaType.codigo.copyWith(fontWeight: FontWeight.w600, color: RegentaColors.ink)),
              ),
            ]),
        ],
      ),
    );
  }

  String _repartoTexto(String lineaId) {
    final n = cuentas.where((c) => c.proporcionDe(lineaId) != null).length;
    return n > 1 ? '1 / $n' : '—';
  }

  Widget _fila(BuildContext context, {required List<Widget> children, bool cabecera = false}) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
      decoration: BoxDecoration(
        color: cabecera ? RegentaColors.sunken : null,
        border: cabecera ? const Border(bottom: BorderSide(color: RegentaColors.line)) : null,
      ),
      child: Row(children: children),
    );
  }

  Widget _celda(
      {double? width, int? flex, required String texto, TextAlign alinear = TextAlign.center, bool cabecera = false}) {
    final texto2 = Text(texto.toUpperCase(),
        textAlign: alinear, style: RegentaType.etiqueta.copyWith(fontSize: 9.5));
    if (flex != null) return Expanded(flex: flex, child: texto2);
    return SizedBox(width: width, child: texto2);
  }
}

class _ChipEstadoCuenta extends StatelessWidget {
  const _ChipEstadoCuenta({required this.pagada});
  final bool pagada;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(
        color: pagada ? RegentaColors.okSoft : RegentaColors.warnSoft,
        borderRadius: BorderRadius.circular(3),
      ),
      child: Text((pagada ? 'Pagada' : 'Pendiente').toUpperCase(),
          style: RegentaType.etiqueta
              .copyWith(fontSize: 9.5, color: pagada ? RegentaColors.ok : RegentaColors.warn)),
    );
  }
}

class _TarjetaCuenta extends StatelessWidget {
  const _TarjetaCuenta({required this.cuenta, required this.mostrarBotonCobrar, required this.onCobrar});

  final CuentaVista cuenta;
  final bool mostrarBotonCobrar;
  final VoidCallback onCobrar;

  @override
  Widget build(BuildContext context) {
    return Container(
      key: Key('dividir-cuenta-${cuenta.id}'),
      margin: const EdgeInsets.only(bottom: 11),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        border: Border.all(color: cuenta.pagada ? RegentaColors.line : RegentaColors.comanda),
        borderRadius: BorderRadius.circular(7),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Text(cuenta.nombre,
                        style: RegentaType.item.copyWith(fontSize: 14.5, color: RegentaColors.ink)),
                    const SizedBox(height: 2),
                    Text('${cuenta.lineas.length} ítems',
                        style: RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                  ],
                ),
              ),
              _ChipEstadoCuenta(pagada: cuenta.pagada),
            ],
          ),
          const SizedBox(height: 10),
          const Divider(height: 1, color: RegentaColors.line2),
          const SizedBox(height: 10),
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            crossAxisAlignment: CrossAxisAlignment.baseline,
            textBaseline: TextBaseline.alphabetic,
            children: [
              Text('Total', style: RegentaType.item.copyWith(fontSize: 14, color: RegentaColors.ink)),
              Text(pesos(cuenta.total),
                  key: Key('dividir-total-${cuenta.id}'),
                  style: const TextStyle(
                      fontFamily: RegentaType.mono,
                      fontSize: 17,
                      fontWeight: FontWeight.w700,
                      color: RegentaColors.ink)),
            ],
          ),
          if (mostrarBotonCobrar && !cuenta.pagada) ...[
            const SizedBox(height: 12),
            FilledButton(
              key: Key('dividir-cobrar-${cuenta.id}'),
              onPressed: onCobrar,
              child: Text('Cobrar'),
            ),
          ],
        ],
      ),
    );
  }
}

class _ResumenFaltaPorCobrar extends StatelessWidget {
  const _ResumenFaltaPorCobrar({required this.estado});
  final EstadoDeDivision estado;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(color: RegentaColors.sunken, borderRadius: BorderRadius.circular(6)),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        crossAxisAlignment: CrossAxisAlignment.baseline,
        textBaseline: TextBaseline.alphabetic,
        children: [
          Expanded(
            child: Text('Falta por cobrar',
                style: RegentaType.item.copyWith(fontSize: 13.5, color: RegentaColors.ink)),
          ),
          Text(pesos(estado.faltaPorCobrar),
              key: const Key('dividir-falta-por-cobrar'),
              style: const TextStyle(
                  fontFamily: RegentaType.mono,
                  fontSize: 18,
                  fontWeight: FontWeight.w700,
                  color: RegentaColors.warn)),
        ],
      ),
    );
  }
}
