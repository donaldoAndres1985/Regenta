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
    this.icono,
    required this.titulo,
    required this.subtitulo,
    this.acciones = const [],
    this.margen = EncabezadoDeSeccion.margenEstandar,
  });

  /// 8 arriba + 56 de fila + 17 abajo (E3).
  static const double alto = 81;

  /// Márgenes de todas las secciones (E3).
  static const EdgeInsets margenEstandar = EdgeInsets.fromLTRB(24, 8, 24, 17);

  /// Igual al estándar: todas las secciones usan los márgenes de Inicio (E3).
  static const EdgeInsets margenInicio = margenEstandar;

  /// Alto de la fila del título y las acciones.
  static const double altoFila = 56;

  /// Sin ícono solo en Inicio, que saluda en vez de nombrar la sección.
  final IconData? icono;
  final String titulo;

  /// Qué se hace en la sección, debajo del título (E1).
  final String subtitulo;
  final List<Widget> acciones;
  final EdgeInsets margen;

  @override
  Size get preferredSize => Size.fromHeight(margen.top + altoFila + margen.bottom);

  @override
  Widget build(BuildContext context) {
    return SizedBox(
      height: preferredSize.height,
      child: Padding(
        padding: margen,
        child: Row(children: [
          if (icono != null) ...[
            Icon(icono, size: 26, color: RegentaColors.ink),
            const SizedBox(width: 12),
          ],
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
                    fontSize: 25,
                    fontWeight: FontWeight.bold,
                    color: RegentaColors.ink,
                    height: 1.2,
                  ),
                ),
                ...[
                  const SizedBox(height: 2),
                  Text(
                    subtitulo,
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
