import 'package:flutter/material.dart';

import '../tema/pastel_theme.dart';
import '../tema/regenta_colors.dart';

/// El encabezado común de las secciones del menú en web (design/comportamiento/Encabezado.md).
///
/// Estándar de Inicio: ícono de la sección, título de 26 px en negrita, subtítulo opcional y las
/// acciones a la derecha. Siempre mide [alto], así que va en el `appBar` del `Scaffold` y el
/// contenido de todas las pantallas empieza a la misma altura. En móvil cada pantalla conserva
/// su `AppBar`.
class EncabezadoDeSeccion extends StatelessWidget implements PreferredSizeWidget {
  const EncabezadoDeSeccion({
    super.key,
    required this.icono,
    required this.titulo,
    this.subtitulo,
    this.acciones = const [],
  });

  /// 24 arriba + 56 de fila + 20 abajo (E3).
  static const double alto = 100;

  /// Márgenes de todas las secciones (E3).
  static const EdgeInsets margen = EdgeInsets.fromLTRB(24, 24, 24, 20);

  final IconData icono;
  final String titulo;
  final String? subtitulo;
  final List<Widget> acciones;

  @override
  Size get preferredSize => const Size.fromHeight(alto);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: alto,
      child: Padding(
        padding: margen,
        child: Row(children: [
          Icon(icono, size: 26, color: RegentaColors.ink),
          const SizedBox(width: 12),
          Expanded(
            child: Column(
              mainAxisAlignment: MainAxisAlignment.center,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  titulo,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: PastelTheme.titulo(
                    fontSize: 26,
                    fontWeight: FontWeight.bold,
                    color: RegentaColors.ink,
                    height: 1.2,
                  ),
                ),
                if (subtitulo != null) ...[
                  const SizedBox(height: 2),
                  Text(
                    subtitulo!,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: PastelTheme.cuerpo(fontSize: 13, color: RegentaColors.ink2),
                  ),
                ],
              ],
            ),
          ),
          for (final (i, a) in acciones.indexed) ...[
            if (i > 0) const SizedBox(width: 10),
            a,
          ],
        ]),
      ),
    );
  }
}
