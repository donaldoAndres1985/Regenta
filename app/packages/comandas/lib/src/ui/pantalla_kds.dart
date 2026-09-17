import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../cocina/estado_de_cocina.dart';
import '../cocina/proveedores.dart';
import '../datos/ticket_de_cocina.dart';

/// Colores propios del KDS: fondo oscuro a propósito, se mira de lejos en una
/// cocina. No son del tema general (`regenta_theme.dart`), que es siempre
/// claro; son literales del mockup (`design/pantallas/KDSWeb.html`).
abstract final class _Kds {
  static const fondo = Color(0xFF1B1916);
  static const barra = Color(0xFF22201C);
  static const borde = Color(0xFF33302B);
  static const pestanaInactiva = Color(0xFF2C2925);
  static const pestanaInactivaTexto = Color(0xFFB9B3A6);
  static const texto = Color(0xFFF2EFE9);
  static const nota = Color(0xFFE8A08F);
  static const vacioBorde = Color(0xFF3A362F);
  static const divisor = Color(0x14FFFFFF);
  static const botonNeutral = Color(0x17FFFFFF);
}

/// El KDS (HU-088): los tickets de cocina de una estación, en tres columnas
/// (Nuevos, en preparación, listos). **Un solo widget** que se adapta con
/// `LayoutBuilder` en [kBreakpointEscritorio]: en escritorio, tres columnas
/// lado a lado; en móvil, una sola lista con el color de cada tarjeta
/// marcando su columna.
class PantallaKds extends ConsumerWidget {
  const PantallaKds({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(controladorDeCocinaProvider);
    final ctrl = ref.read(controladorDeCocinaProvider.notifier);

    return Scaffold(
      backgroundColor: _Kds.fondo,
      body: SafeArea(
        child: Builder(builder: (context) {
          if (estado.cargando && estado.estaciones.isEmpty) {
            return const Center(
                child: CircularProgressIndicator(color: _Kds.texto));
          }
          if (estado.errorAlCargar && estado.estaciones.isEmpty) {
            return _CentroConReintento(onReintentar: ctrl.cargar);
          }
          return LayoutBuilder(builder: (context, restricciones) {
            final esEscritorio = restricciones.maxWidth >= kBreakpointEscritorio;
            return Column(
              children: [
                _Encabezado(
                  estado: estado,
                  esEscritorio: esEscritorio,
                  onElegirEstacion: ctrl.seleccionarEstacion,
                ),
                if (estado.mensaje != null)
                  _Aviso(mensaje: estado.mensaje!, onCerrar: ctrl.limpiarMensaje),
                Expanded(
                  child: KeyedSubtree(
                    key: Key(esEscritorio ? 'kds-escritorio' : 'kds-movil'),
                    child: esEscritorio
                        ? _ColumnasDeEstado(
                            estado: estado,
                            onEmpezar: ctrl.marcarEnPreparacion,
                            onListo: ctrl.marcarListo,
                            onEntregado: ctrl.marcarEntregado,
                          )
                        : _ListaUnica(
                            estado: estado,
                            onEmpezar: ctrl.marcarEnPreparacion,
                            onListo: ctrl.marcarListo,
                            onEntregado: ctrl.marcarEntregado,
                          ),
                  ),
                ),
                const _PieInformativo(),
              ],
            );
          });
        }),
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
          Text('No se pudo cargar la cocina',
              style: RegentaType.cuerpo.copyWith(color: RegentaColors.faint)),
          const SizedBox(height: 10),
          FilledButton(
            key: const Key('kds-reintentar'),
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
      color: RegentaColors.crit,
      padding: const EdgeInsets.fromLTRB(14, 8, 6, 8),
      child: Row(
        children: [
          Expanded(
            child: Text(mensaje,
                key: const Key('kds-aviso'),
                style: RegentaType.cuerpo.copyWith(fontSize: 12, color: Colors.white)),
          ),
          IconButton(
            onPressed: onCerrar,
            icon: const Icon(Icons.close, size: 16, color: Colors.white),
            visualDensity: VisualDensity.compact,
          ),
        ],
      ),
    );
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({
    required this.estado,
    required this.esEscritorio,
    required this.onElegirEstacion,
  });
  final EstadoDeCocina estado;
  final bool esEscritorio;
  final void Function(String) onElegirEstacion;

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 60,
      padding: const EdgeInsets.symmetric(horizontal: RegentaSpacing.xl),
      decoration: const BoxDecoration(
        color: _Kds.barra,
        border: Border(bottom: BorderSide(color: _Kds.borde)),
      ),
      child: Row(
        children: [
          Text('Cocina', style: RegentaType.seccion.copyWith(fontSize: 16, color: _Kds.texto)),
          const SizedBox(width: RegentaSpacing.lg),
          Expanded(
            child: SingleChildScrollView(
              scrollDirection: Axis.horizontal,
              child: Row(
                children: [
                  for (final e in estado.estaciones)
                    Padding(
                      padding: const EdgeInsets.only(right: 7),
                      child: _PestanaEstacion(
                        estacion: e,
                        activa: e.id == estado.estacionSeleccionadaId,
                        onTap: () => onElegirEstacion(e.id),
                      ),
                    ),
                ],
              ),
            ),
          ),
          // En móvil no caben los tres números; solo el título y las pestañas.
          if (esEscritorio) ...[
            _Stat(etiqueta: 'En cola', valor: '${estado.tickets.length}'),
            const SizedBox(width: RegentaSpacing.lg),
            _Stat(etiqueta: 'Demora media', valor: "${estado.demoraMediaMin}′"),
            const SizedBox(width: RegentaSpacing.lg),
            _Stat(
              etiqueta: 'Más antiguo',
              valor: "${estado.demoraMaximaMin}′",
              destacar: estado.tickets.any((t) => t.demorado),
            ),
          ],
        ],
      ),
    );
  }
}

class _PestanaEstacion extends StatelessWidget {
  const _PestanaEstacion({required this.estacion, required this.activa, required this.onTap});
  final EstacionDeCocina estacion;
  final bool activa;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      key: Key('kds-tab-${estacion.id}'),
      onTap: onTap,
      borderRadius: BorderRadius.circular(3),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
        decoration: BoxDecoration(
          color: activa ? RegentaColors.comanda : _Kds.pestanaInactiva,
          borderRadius: BorderRadius.circular(3),
        ),
        child: Text(estacion.nombre.toUpperCase(),
            style: RegentaType.etiqueta.copyWith(
                fontSize: 9.5,
                color: activa ? Colors.white : _Kds.pestanaInactivaTexto)),
      ),
    );
  }
}

class _Stat extends StatelessWidget {
  const _Stat({required this.etiqueta, required this.valor, this.destacar = false});
  final String etiqueta;
  final String valor;
  final bool destacar;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.end,
      mainAxisSize: MainAxisSize.min,
      children: [
        Text(etiqueta.toUpperCase(),
            style: RegentaType.etiqueta.copyWith(fontSize: 9.5, color: RegentaColors.muted)),
        const SizedBox(height: 2),
        Text(valor,
            style: TextStyle(
                fontFamily: RegentaType.mono,
                fontSize: 17,
                fontWeight: FontWeight.w600,
                color: destacar ? RegentaColors.crit : _Kds.texto)),
      ],
    );
  }
}

typedef _Accion = void Function(String ticketId);

class _ColumnasDeEstado extends StatelessWidget {
  const _ColumnasDeEstado({
    required this.estado,
    required this.onEmpezar,
    required this.onListo,
    required this.onEntregado,
  });

  final EstadoDeCocina estado;
  final _Accion onEmpezar;
  final _Accion onListo;
  final _Accion onEntregado;

  @override
  Widget build(BuildContext context) {
    return Row(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Expanded(
          child: _Columna(
            titulo: 'Nuevos',
            colorPunto: RegentaColors.comanda,
            tickets: estado.nuevos,
            vacioTexto: 'Nada nuevo',
            onEmpezar: onEmpezar,
            onListo: onListo,
            onEntregado: onEntregado,
          ),
        ),
        Expanded(
          child: _Columna(
            titulo: 'En preparación',
            colorPunto: RegentaColors.warn,
            tickets: estado.enPreparacion,
            vacioTexto: 'Nada en preparación',
            onEmpezar: onEmpezar,
            onListo: onListo,
            onEntregado: onEntregado,
          ),
        ),
        Expanded(
          child: _Columna(
            titulo: 'Listos para servir',
            colorPunto: RegentaColors.ok,
            tickets: estado.listos,
            vacioTexto: 'Nada listo por ahora',
            onEmpezar: onEmpezar,
            onListo: onListo,
            onEntregado: onEntregado,
          ),
        ),
      ],
    );
  }
}

class _Columna extends StatelessWidget {
  const _Columna({
    required this.titulo,
    required this.colorPunto,
    required this.tickets,
    required this.vacioTexto,
    required this.onEmpezar,
    required this.onListo,
    required this.onEntregado,
  });

  final String titulo;
  final Color colorPunto;
  final List<TicketDeCocina> tickets;
  final String vacioTexto;
  final _Accion onEmpezar;
  final _Accion onListo;
  final _Accion onEntregado;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(RegentaSpacing.lg),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Container(
                width: 9,
                height: 9,
                decoration: BoxDecoration(color: colorPunto, shape: BoxShape.circle),
              ),
              const SizedBox(width: 9),
              Text(titulo,
                  style: RegentaType.item.copyWith(fontSize: 14.5, color: _Kds.texto)),
              const SizedBox(width: 6),
              Text('${tickets.length}',
                  style: TextStyle(
                      fontFamily: RegentaType.mono, fontSize: 12, color: RegentaColors.muted)),
            ],
          ),
          const SizedBox(height: 12),
          Expanded(
            child: tickets.isEmpty
                ? _ColumnaVacia(texto: vacioTexto)
                : ListView.builder(
                    itemCount: tickets.length,
                    itemBuilder: (context, i) => _TarjetaTicket(
                      ticket: tickets[i],
                      onEmpezar: onEmpezar,
                      onListo: onListo,
                      onEntregado: onEntregado,
                    ),
                  ),
          ),
        ],
      ),
    );
  }
}

class _ListaUnica extends StatelessWidget {
  const _ListaUnica({
    required this.estado,
    required this.onEmpezar,
    required this.onListo,
    required this.onEntregado,
  });

  final EstadoDeCocina estado;
  final _Accion onEmpezar;
  final _Accion onListo;
  final _Accion onEntregado;

  @override
  Widget build(BuildContext context) {
    if (estado.tickets.isEmpty) {
      return const _ColumnaVacia(texto: 'Nada pendiente en esta estación');
    }
    return ListView.builder(
      padding: const EdgeInsets.all(RegentaSpacing.md),
      itemCount: estado.tickets.length,
      itemBuilder: (context, i) => _TarjetaTicket(
        ticket: estado.tickets[i],
        onEmpezar: onEmpezar,
        onListo: onListo,
        onEntregado: onEntregado,
      ),
    );
  }
}

class _ColumnaVacia extends StatelessWidget {
  const _ColumnaVacia({required this.texto});
  final String texto;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(26),
      decoration: BoxDecoration(
        border: Border.all(color: _Kds.vacioBorde, width: 1.5),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(texto,
          textAlign: TextAlign.center,
          style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.muted)),
    );
  }
}

class _TarjetaTicket extends StatelessWidget {
  const _TarjetaTicket({
    required this.ticket,
    required this.onEmpezar,
    required this.onListo,
    required this.onEntregado,
  });

  final TicketDeCocina ticket;
  final _Accion onEmpezar;
  final _Accion onListo;
  final _Accion onEntregado;

  Color get _color {
    if (ticket.demorado) return RegentaColors.crit;
    return switch (ticket.estado) {
      'NUEVO' => RegentaColors.comanda,
      'EN_PREPARACION' => RegentaColors.warn,
      'LISTO' => RegentaColors.ok,
      _ => RegentaColors.muted,
    };
  }

  @override
  Widget build(BuildContext context) {
    final color = _color;
    return Container(
      key: Key('ticket-${ticket.id}'),
      margin: const EdgeInsets.only(bottom: 12),
      clipBehavior: Clip.antiAlias,
      decoration: BoxDecoration(
        color: _Kds.barra,
        border: Border.all(color: color, width: 1.5),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Container(
            color: color,
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
            child: Row(
              children: [
                Text('#${ticket.secuencia}',
                    style: const TextStyle(
                        fontFamily: RegentaType.mono,
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                        color: Colors.white)),
                const SizedBox(width: 9),
                Text(ticket.comandaNumero,
                    style: TextStyle(
                        fontFamily: RegentaType.mono,
                        fontSize: 11,
                        color: Colors.white.withValues(alpha: .82))),
                const Spacer(),
                Text("${ticket.minutosTranscurridos}′",
                    key: Key('ticket-demora-${ticket.id}'),
                    style: const TextStyle(
                        fontFamily: RegentaType.mono,
                        fontSize: 14,
                        fontWeight: FontWeight.w700,
                        color: Colors.white)),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 4, 12, 10),
            child: Column(
              children: [
                for (final l in ticket.lineas)
                  Container(
                    padding: const EdgeInsets.symmetric(vertical: 8),
                    decoration:
                        const BoxDecoration(border: Border(bottom: BorderSide(color: _Kds.divisor))),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        SizedBox(
                          width: 20,
                          child: Text('${l.cantidad.round()}',
                              style: const TextStyle(
                                  fontFamily: RegentaType.mono,
                                  fontSize: 15,
                                  fontWeight: FontWeight.w700,
                                  color: RegentaColors.accent)),
                        ),
                        const SizedBox(width: 9),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(l.nombre,
                                  style: const TextStyle(
                                      fontFamily: RegentaType.ui,
                                      fontSize: 14,
                                      fontWeight: FontWeight.w600,
                                      color: _Kds.texto,
                                      height: 1.25)),
                              if (l.notas != null)
                                Padding(
                                  padding: const EdgeInsets.only(top: 3),
                                  child: Text(l.notas!,
                                      style: const TextStyle(
                                          fontFamily: RegentaType.mono,
                                          fontSize: 11,
                                          color: _Kds.nota)),
                                ),
                            ],
                          ),
                        ),
                      ],
                    ),
                  ),
              ],
            ),
          ),
          Padding(
            padding: const EdgeInsets.fromLTRB(12, 0, 12, 12),
            child: Row(children: _botones()),
          ),
        ],
      ),
    );
  }

  List<Widget> _botones() {
    switch (ticket.estado) {
      case 'NUEVO':
        return [
          Expanded(
            child: _Boton(
              key: Key('ticket-empezar-${ticket.id}'),
              texto: 'Empezar',
              bg: _Kds.botonNeutral,
              fg: _Kds.texto,
              onTap: () => onEmpezar(ticket.id),
            ),
          ),
          const SizedBox(width: 8),
          Expanded(
            child: _Boton(
              key: Key('ticket-listo-${ticket.id}'),
              texto: 'Listo',
              bg: RegentaColors.ok,
              fg: Colors.white,
              onTap: () => onListo(ticket.id),
            ),
          ),
        ];
      case 'EN_PREPARACION':
        return [
          Expanded(
            child: _Boton(
              key: Key('ticket-listo-${ticket.id}'),
              texto: 'Listo',
              bg: RegentaColors.ok,
              fg: Colors.white,
              onTap: () => onListo(ticket.id),
            ),
          ),
        ];
      case 'LISTO':
        return [
          Expanded(
            child: _Boton(
              key: Key('ticket-entregado-${ticket.id}'),
              texto: 'Entregado',
              bg: RegentaColors.ok,
              fg: Colors.white,
              onTap: () => onEntregado(ticket.id),
            ),
          ),
        ];
      default:
        return const [];
    }
  }
}

class _Boton extends StatelessWidget {
  const _Boton({super.key, required this.texto, required this.bg, required this.fg, required this.onTap});
  final String texto;
  final Color bg;
  final Color fg;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(5),
      child: Container(
        constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
        alignment: Alignment.center,
        decoration: BoxDecoration(color: bg, borderRadius: BorderRadius.circular(5)),
        child: Text(texto,
            style: TextStyle(
                fontFamily: RegentaType.ui, fontSize: 12.5, fontWeight: FontWeight.w600, color: fg)),
      ),
    );
  }
}

class _PieInformativo extends StatelessWidget {
  const _PieInformativo();

  @override
  Widget build(BuildContext context) {
    return Container(
      height: 44,
      alignment: Alignment.centerLeft,
      padding: const EdgeInsets.symmetric(horizontal: RegentaSpacing.xl),
      decoration: const BoxDecoration(
        color: _Kds.barra,
        border: Border(top: BorderSide(color: _Kds.borde)),
      ),
      child: Row(
        children: [
          const Icon(Icons.info_outline, size: 15, color: RegentaColors.muted),
          const SizedBox(width: 9),
          Expanded(
            child: Text.rich(
              TextSpan(
                style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted),
                children: const [
                  TextSpan(text: 'Cada línea avanza por su cuenta. Al marcar «Listo» se publica '),
                  TextSpan(text: 'linea_lista', style: TextStyle(fontFamily: RegentaType.mono)),
                  TextSpan(text: ' y al mesero le llega el aviso.'),
                ],
              ),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ),
        ],
      ),
    );
  }
}
