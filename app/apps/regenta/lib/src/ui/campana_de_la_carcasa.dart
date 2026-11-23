import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../arranque/proveedores.dart';

/// La campana de alertas de la carcasa (HU-095, HU-123). Abre el centro de
/// alertas, y ahí, y solo ahí, se invita a recibirlas en el celular.
class CampanaDeLaCarcasa extends ConsumerWidget {
  const CampanaDeLaCarcasa({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final controlador = ref.watch(controladorDeAlertasProvider);
    final registro = ref.watch(registroDePushProvider);
    return CampanaDeAlertas(
      key: const Key('campana-de-la-carcasa'),
      controlador: controlador,
      onAbrir: () {
        // Al abrir se trae lo último: lo que llegó por push con la app cerrada.
        controlador.cargar();
        showModalBottomSheet<void>(
        context: context,
        isScrollControlled: true,
        showDragHandle: true,
        builder: (hoja) => SafeArea(
          child: ConstrainedBox(
            constraints: BoxConstraints(maxHeight: MediaQuery.sizeOf(hoja).height * 0.75),
            child: CentroDeAlertas(
              controlador: controlador,
              registroDePush: registro,
              onAbrirEntidad: (alerta) {
                Navigator.of(hoja).pop();
                final ruta = alerta.rutaApp;
                if (ruta != null && ruta.isNotEmpty) GoRouter.of(context).go(ruta);
              },
            ),
          ),
        ),
        );
      },
    );
  }
}
