import 'package:flutter/material.dart';

import '../../tema/regenta_colors.dart';
import '../../tema/regenta_type.dart';
import 'registro_de_push.dart';
import 'servicio_de_push.dart';

/// La invitación a recibir las alertas en el celular, dentro del centro de
/// alertas (HU-123). Es el único lugar desde el que se pide el permiso: el
/// usuario está mirando sus alertas y entiende para qué sirve.
///
/// No se muestra en Web, ni si ya está activo. Si el usuario lo negó, solo
/// recuerda dónde se cambia.
class AvisoDePush extends StatelessWidget {
  const AvisoDePush({super.key, required this.registro});

  final RegistroDePush registro;

  @override
  Widget build(BuildContext context) {
    return AnimatedBuilder(
      animation: registro,
      builder: (context, _) {
        if (registro.permiso == PermisoDePush.denegado) {
          return Padding(
            key: const Key('aviso-push-denegado'),
            padding: const EdgeInsets.fromLTRB(14, 0, 14, 10),
            child: Text(
              'Las notificaciones están apagadas para Regenta. Puedes '
              'encenderlas en los ajustes del teléfono.',
              style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted),
            ),
          );
        }
        if (!registro.invitarAActivar) return const SizedBox.shrink();
        return Container(
          key: const Key('aviso-push'),
          margin: const EdgeInsets.fromLTRB(14, 0, 14, 10),
          padding: const EdgeInsets.fromLTRB(12, 10, 8, 10),
          decoration: BoxDecoration(
            color: RegentaColors.accentSoft,
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: RegentaColors.line),
          ),
          child: Row(
            children: [
              const Icon(Icons.notifications_active_outlined, size: 20, color: RegentaColors.accent),
              const SizedBox(width: 10),
              Expanded(
                child: Text(
                  'Recibe estas alertas en el celular, aunque no tengas la app abierta.',
                  style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink),
                ),
              ),
              TextButton(
                key: const Key('activar-push'),
                onPressed: registro.ocupado ? null : registro.activar,
                child: const Text('Activar'),
              ),
            ],
          ),
        );
      },
    );
  }
}
