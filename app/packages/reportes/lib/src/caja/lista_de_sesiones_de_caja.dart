import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

import 'controlador_de_reportes_de_caja.dart';
import 'sesion_de_caja_vista.dart';

/// El listado de turnos de caja de un rango (HU-063). Las sesiones descuadradas
/// destacan sobre las cuadradas (criterio 3); tocar una lleva a su reporte
/// (criterio 1, resuelto por el shell).
class ListaDeSesionesDeCaja extends StatelessWidget {
  const ListaDeSesionesDeCaja({
    super.key,
    required this.controlador,
    required this.onAbrirSesion,
  });

  final ControladorDeReportesDeCaja controlador;
  final void Function(SesionDeCajaVista sesion) onAbrirSesion;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controlador,
      builder: (context, _) {
        if (controlador.cargando) {
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
        final sesiones = controlador.sesiones;
        if (sesiones.isEmpty) {
          return Padding(
            padding: const EdgeInsets.all(20),
            child: Text('No hay turnos de caja en el rango.',
                style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
          );
        }
        return Column(
          key: const Key('lista-sesiones-caja'),
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            if (controlador.descuadres > 0)
              Padding(
                padding: const EdgeInsets.fromLTRB(14, 12, 14, 4),
                child: Text('${controlador.descuadres} turno(s) descuadrado(s)',
                    style: RegentaType.codigo
                        .copyWith(fontSize: 11, color: RegentaColors.crit)),
              ),
            for (final s in sesiones)
              _FilaDeSesion(sesion: s, onAbrir: () => onAbrirSesion(s)),
          ],
        );
      },
    );
  }
}

class _FilaDeSesion extends StatelessWidget {
  const _FilaDeSesion({required this.sesion, required this.onAbrir});

  final SesionDeCajaVista sesion;
  final VoidCallback onAbrir;

  @override
  Widget build(BuildContext context) {
    final descuadrada = sesion.descuadrada;
    return InkWell(
      key: Key('sesion-${sesion.id}'),
      onTap: onAbrir,
      child: Container(
        padding: const EdgeInsets.fromLTRB(14, 11, 14, 11),
        decoration: BoxDecoration(
          color: descuadrada ? RegentaColors.critSoft : null,
          border: const Border(bottom: BorderSide(color: RegentaColors.line)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(sesion.numero,
                      style: RegentaType.codigo
                          .copyWith(fontSize: 13, color: RegentaColors.ink)),
                  const SizedBox(height: 2),
                  Text(sesion.estado.name.toUpperCase(),
                      style: RegentaType.etiqueta.copyWith(
                          color: descuadrada
                              ? RegentaColors.crit
                              : RegentaColors.muted)),
                ],
              ),
            ),
            const SizedBox(width: 8),
            if (descuadrada)
              Container(
                key: Key('sesion-descuadrada-${sesion.id}'),
                padding: const EdgeInsets.symmetric(horizontal: 7, vertical: 3),
                decoration: BoxDecoration(
                  color: RegentaColors.crit,
                  borderRadius: BorderRadius.circular(3),
                ),
                child: Text('Descuadre',
                    style: RegentaType.etiqueta.copyWith(color: RegentaColors.surface)),
              ),
            if (sesion.diferencia != null) ...[
              const SizedBox(width: 8),
              Text(_pesos(sesion.diferencia!),
                  style: RegentaType.codigo.copyWith(
                      fontSize: 12,
                      fontWeight: FontWeight.w600,
                      color: descuadrada ? RegentaColors.crit : RegentaColors.ink2)),
            ],
          ],
        ),
      ),
    );
  }

  static String _pesos(num v) {
    final s = v.round().abs().toString();
    final b = StringBuffer();
    for (var i = 0; i < s.length; i++) {
      if (i != 0 && (s.length - i) % 3 == 0) b.write('.');
      b.write(s[i]);
    }
    return '${v < 0 ? '-' : ''}\$$b';
  }
}
