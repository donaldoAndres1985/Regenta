import 'package:flutter/material.dart';

import '../tema/regenta_colors.dart';
import '../tema/regenta_spacing.dart';
import '../tema/regenta_type.dart';
import 'alerta_vista.dart';
import 'controlador_de_alertas.dart';

/// La campana de la barra superior (HU-095 criterio 4). Muestra un punto cuando
/// hay alertas nuevas; al tocarla se abre el centro y las nuevas pasan a vistas.
class CampanaDeAlertas extends StatelessWidget {
  const CampanaDeAlertas({
    super.key,
    required this.controlador,
    required this.onAbrir,
  });

  final ControladorDeAlertas controlador;
  final VoidCallback onAbrir;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controlador,
      builder: (context, _) {
        return Semantics(
          button: true,
          label: controlador.hayNuevas
              ? 'Alertas, ${controlador.cuentaNuevas} nuevas'
              : 'Alertas',
          child: InkWell(
            onTap: () {
              controlador.abrir();
              onAbrir();
            },
            borderRadius: BorderRadius.circular(RegentaSpacing.radius),
            child: SizedBox(
              width: RegentaSpacing.hitTarget,
              height: RegentaSpacing.hitTarget,
              child: Stack(
                alignment: Alignment.center,
                children: [
                  const Icon(Icons.notifications_none,
                      size: 22, color: RegentaColors.ink2),
                  if (controlador.hayNuevas)
                    Positioned(
                      top: 10,
                      right: 10,
                      child: Container(
                        key: const Key('campana-indicador'),
                        width: 9,
                        height: 9,
                        decoration: const BoxDecoration(
                          color: RegentaColors.crit,
                          shape: BoxShape.circle,
                        ),
                      ),
                    ),
                ],
              ),
            ),
          ),
        );
      },
    );
  }
}

/// El centro de alertas: todas las pendientes en un lugar (HU-095). Las nuevas
/// primero y por severidad (criterio 1); tocar una lleva a su entidad (criterio
/// 3); *Resolver* la saca de la lista (criterio 2).
class CentroDeAlertas extends StatelessWidget {
  const CentroDeAlertas({
    super.key,
    required this.controlador,
    required this.onAbrirEntidad,
  });

  final ControladorDeAlertas controlador;
  final void Function(AlertaVista alerta) onAbrirEntidad;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: controlador,
      builder: (context, _) {
        final pendientes = controlador.pendientes;
        return Column(
          key: const Key('centro-de-alertas'),
          crossAxisAlignment: CrossAxisAlignment.stretch,
          mainAxisSize: MainAxisSize.min,
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(14, 13, 14, 9),
              child: Row(
                children: [
                  Text('Alertas',
                      style: RegentaType.seccion
                          .copyWith(fontSize: 15, color: RegentaColors.ink)),
                  const SizedBox(width: 8),
                  if (controlador.cuentaNuevas > 0)
                    Text('${controlador.cuentaNuevas} nuevas',
                        style: RegentaType.codigo
                            .copyWith(fontSize: 11, color: RegentaColors.accent)),
                ],
              ),
            ),
            if (controlador.cargando)
              const Padding(
                padding: EdgeInsets.all(24),
                child: Center(child: CircularProgressIndicator()),
              )
            else if (controlador.error != null)
              Padding(
                padding: const EdgeInsets.fromLTRB(14, 4, 14, 16),
                child: Text(controlador.error!,
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.crit)),
              )
            else if (pendientes.isEmpty)
              Padding(
                padding: const EdgeInsets.fromLTRB(14, 4, 14, 20),
                child: Text('Sin alertas pendientes.',
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
              )
            else
              Flexible(
                child: ListView.builder(
                  key: const Key('lista-de-alertas'),
                  shrinkWrap: true,
                  itemCount: pendientes.length,
                  itemBuilder: (context, i) => _FilaDeAlerta(
                    alerta: pendientes[i],
                    onAbrir: () => onAbrirEntidad(pendientes[i]),
                    onResolver: () => controlador.resolver(pendientes[i].id),
                  ),
                ),
              ),
          ],
        );
      },
    );
  }
}

class _FilaDeAlerta extends StatelessWidget {
  const _FilaDeAlerta({
    required this.alerta,
    required this.onAbrir,
    required this.onResolver,
  });

  final AlertaVista alerta;
  final VoidCallback onAbrir;
  final VoidCallback onResolver;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      key: Key('alerta-${alerta.id}'),
      onTap: onAbrir,
      child: Container(
        padding: const EdgeInsets.fromLTRB(14, 11, 8, 11),
        decoration: const BoxDecoration(
          border: Border(bottom: BorderSide(color: RegentaColors.line)),
        ),
        child: Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Container(
              margin: const EdgeInsets.only(top: 4),
              width: 8,
              height: 8,
              decoration: BoxDecoration(
                color: _colorSeveridad(alerta.severidad),
                shape: BoxShape.circle,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(alerta.titulo,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.item
                          .copyWith(fontSize: 13, color: RegentaColors.ink)),
                  const SizedBox(height: 2),
                  Text(alerta.mensaje,
                      maxLines: 2,
                      overflow: TextOverflow.ellipsis,
                      style: RegentaType.cuerpo
                          .copyWith(fontSize: 11.5, color: RegentaColors.ink2)),
                  const SizedBox(height: 3),
                  Text(_hace(alerta.generadaEn),
                      style: RegentaType.codigo
                          .copyWith(fontSize: 10, color: RegentaColors.faint)),
                ],
              ),
            ),
            const SizedBox(width: 6),
            Semantics(
              button: true,
              label: 'Resolver',
              child: InkWell(
                onTap: onResolver,
                borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                child: const SizedBox(
                  width: RegentaSpacing.hitTarget,
                  height: RegentaSpacing.hitTarget,
                  child: Icon(Icons.check, size: 18, color: RegentaColors.ok),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  static Color _colorSeveridad(SeveridadAlerta s) => switch (s) {
        SeveridadAlerta.critica => RegentaColors.crit,
        SeveridadAlerta.alta => RegentaColors.warn,
        SeveridadAlerta.media => RegentaColors.accent,
        SeveridadAlerta.baja => RegentaColors.faint,
      };

  static String _hace(DateTime cuando) {
    final d = DateTime.now().difference(cuando);
    if (d.inMinutes < 1) return 'hace un momento';
    if (d.inMinutes < 60) return 'hace ${d.inMinutes} min';
    if (d.inHours < 24) return 'hace ${d.inHours} h';
    return 'hace ${d.inDays} d';
  }
}
