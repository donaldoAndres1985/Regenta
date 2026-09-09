import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import 'controlador_de_calendario.dart';
import 'ocupacion_vista.dart';

const double _altoFila = 54;
const double _altoBarra = 44;
const double _altoEncabezado = 40;

/// El calendario de ocupación (HU-075): una fila por recurso, una columna por
/// día, las reservas como barras coloreadas según su estado y los bloqueos como
/// barras rojas aparte. En móvil se ven tres días y se desplaza en horizontal.
class CalendarioDeOcupacion extends StatelessWidget {
  const CalendarioDeOcupacion({
    super.key,
    required this.controlador,
    required this.onAbrirReserva,
  });

  final ControladorDeCalendario controlador;

  /// Tocar una barra de reserva abre esa reserva (criterio 5). Lo resuelve el shell.
  final void Function(String reservaId) onAbrirReserva;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controlador,
      builder: (context, _) {
        if (controlador.cargando && controlador.ocupacion == null) {
          return const Padding(
            padding: EdgeInsets.all(24),
            child: Center(child: CircularProgressIndicator()),
          );
        }
        if (controlador.error != null) {
          return Padding(
            padding: const EdgeInsets.all(16),
            child: Text(controlador.error!,
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.crit)),
          );
        }
        final ocupacion = controlador.ocupacion;
        if (ocupacion == null || ocupacion.recursos.isEmpty) {
          return Padding(
            padding: const EdgeInsets.all(20),
            child: Text('No hay recursos que mostrar en esta semana.',
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
          );
        }
        return LayoutBuilder(
          builder: (context, restricciones) {
            final esMovil = restricciones.maxWidth < kBreakpointEscritorio;
            final dias = ocupacion.dias;
            final anchoEtiqueta = esMovil ? 92.0 : 176.0;
            // Móvil: se ven tres días; el resto se alcanza desplazándose.
            final visibles = esMovil ? 3 : dias.length;
            final anchoDia =
                ((restricciones.maxWidth - anchoEtiqueta) / visibles).clamp(84.0, 220.0);
            final anchoRejilla = anchoDia * dias.length;

            final tabla = Column(
              key: const Key('calendario-ocupacion'),
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                _Encabezado(
                    dias: dias, anchoEtiqueta: anchoEtiqueta, anchoDia: anchoDia),
                for (final recurso in ocupacion.recursos)
                  _FilaDeRecurso(
                    recurso: recurso,
                    dias: dias,
                    anchoEtiqueta: anchoEtiqueta,
                    anchoDia: anchoDia,
                    reservas: ocupacion.reservasDe(recurso.id),
                    bloqueos: ocupacion.bloqueosDe(recurso.id),
                    onAbrirReserva: onAbrirReserva,
                  ),
              ],
            );

            if (!esMovil) return tabla;
            return SingleChildScrollView(
              key: const Key('calendario-scroll-horizontal'),
              scrollDirection: Axis.horizontal,
              child: SizedBox(width: anchoEtiqueta + anchoRejilla, child: tabla),
            );
          },
        );
      },
    );
  }
}

class _Encabezado extends StatelessWidget {
  const _Encabezado({
    required this.dias,
    required this.anchoEtiqueta,
    required this.anchoDia,
  });

  final List<DateTime> dias;
  final double anchoEtiqueta;
  final double anchoDia;

  static const _diaSemana = ['LUN', 'MAR', 'MIÉ', 'JUE', 'VIE', 'SÁB', 'DOM'];

  @override
  Widget build(BuildContext context) {
    return Container(
      height: _altoEncabezado,
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        children: [
          SizedBox(width: anchoEtiqueta),
          for (final dia in dias)
            SizedBox(
              key: Key('dia-${_iso(dia)}'),
              width: anchoDia,
              child: Column(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  Text(_diaSemana[(dia.weekday - 1) % 7],
                      style: RegentaType.etiqueta),
                  Text('${dia.day}',
                      style: RegentaType.codigo
                          .copyWith(fontSize: 12, color: RegentaColors.ink)),
                ],
              ),
            ),
        ],
      ),
    );
  }
}

class _FilaDeRecurso extends StatelessWidget {
  const _FilaDeRecurso({
    required this.recurso,
    required this.dias,
    required this.anchoEtiqueta,
    required this.anchoDia,
    required this.reservas,
    required this.bloqueos,
    required this.onAbrirReserva,
  });

  final RecursoDelCalendario recurso;
  final List<DateTime> dias;
  final double anchoEtiqueta;
  final double anchoDia;
  final List<BarraDeReserva> reservas;
  final List<BarraDeBloqueo> bloqueos;
  final void Function(String reservaId) onAbrirReserva;

  @override
  Widget build(BuildContext context) {
    return Container(
      key: Key('fila-recurso-${recurso.id}'),
      height: _altoFila,
      decoration: const BoxDecoration(
        border: Border(bottom: BorderSide(color: RegentaColors.line)),
      ),
      child: Row(
        children: [
          Container(
            width: anchoEtiqueta,
            padding: const EdgeInsets.symmetric(horizontal: 10),
            alignment: Alignment.centerLeft,
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(recurso.codigo,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.codigo
                        .copyWith(fontSize: 12, color: RegentaColors.ink)),
                Text(recurso.nombre,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: RegentaType.etiqueta),
              ],
            ),
          ),
          SizedBox(
            width: anchoDia * dias.length,
            child: Stack(
              children: [
                Row(
                  children: [
                    for (var i = 0; i < dias.length; i++)
                      Container(
                        width: anchoDia,
                        decoration: BoxDecoration(
                          border: Border(
                            right: BorderSide(
                                color: RegentaColors.line.withValues(alpha: 0.6)),
                          ),
                          color: i.isOdd
                              ? RegentaColors.sunken.withValues(alpha: 0.35)
                              : null,
                        ),
                      ),
                  ],
                ),
                for (final b in bloqueos)
                  ..._posicionar(
                    b.desde,
                    b.hasta,
                    (izq, ancho) => Positioned(
                      left: izq,
                      top: (_altoFila - _altoBarra) / 2,
                      width: ancho,
                      height: _altoBarra,
                      child: _BarraBloqueo(
                        key: Key(
                            'barra-bloqueo-${recurso.id}-${bloqueos.indexOf(b)}'),
                        motivo: b.motivo,
                      ),
                    ),
                  ),
                for (final r in reservas)
                  ..._posicionar(
                    r.desde,
                    r.hasta,
                    (izq, ancho) => Positioned(
                      left: izq,
                      top: (_altoFila - _altoBarra) / 2,
                      width: ancho,
                      height: _altoBarra,
                      child: _BarraReserva(
                        key: Key('barra-reserva-${r.id}'),
                        reserva: r,
                        onTap: () => onAbrirReserva(r.id),
                      ),
                    ),
                  ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  /// Traduce `[desde, hasta)` a un `left`/`width` en píxeles dentro de la
  /// rejilla, recortado a la ventana visible. Devuelve vacío si cae fuera.
  Iterable<Widget> _posicionar(
    DateTime desde,
    DateTime hasta,
    Widget Function(double left, double width) construir,
  ) {
    final base = DateTime.utc(dias.first.year, dias.first.month, dias.first.day);
    int idx(DateTime d) =>
        DateTime.utc(d.year, d.month, d.day).difference(base).inDays;

    final iDesde = idx(desde).clamp(0, dias.length);
    final iHasta = idx(hasta).clamp(0, dias.length);
    if (iHasta <= iDesde) return const [];
    final left = iDesde * anchoDia;
    final width = (iHasta - iDesde) * anchoDia - 2;
    return [construir(left + 1, width)];
  }
}

class _BarraReserva extends StatelessWidget {
  const _BarraReserva({super.key, required this.reserva, required this.onTap});

  final BarraDeReserva reserva;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8),
        alignment: Alignment.centerLeft,
        decoration: BoxDecoration(
          color: reserva.estado.fondo,
          border: Border(left: BorderSide(color: reserva.estado.filo, width: 3)),
          borderRadius: BorderRadius.circular(3),
        ),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(reserva.numero,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.codigo
                    .copyWith(fontSize: 11, color: RegentaColors.ink)),
            Text(reserva.estado.codigo,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.etiqueta.copyWith(fontSize: 8.5)),
          ],
        ),
      ),
    );
  }
}

class _BarraBloqueo extends StatelessWidget {
  const _BarraBloqueo({super.key, required this.motivo});

  final String motivo;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8),
      alignment: Alignment.centerLeft,
      decoration: BoxDecoration(
        color: RegentaColors.critSoft,
        border: const Border(left: BorderSide(color: RegentaColors.crit, width: 3)),
        borderRadius: BorderRadius.circular(3),
      ),
      child: Text('Bloqueo · $motivo',
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          style: RegentaType.cuerpo.copyWith(
              fontSize: 11,
              fontWeight: FontWeight.w600,
              color: RegentaColors.crit)),
    );
  }
}

String _iso(DateTime d) =>
    '${d.year.toString().padLeft(4, '0')}-'
    '${d.month.toString().padLeft(2, '0')}-'
    '${d.day.toString().padLeft(2, '0')}';
