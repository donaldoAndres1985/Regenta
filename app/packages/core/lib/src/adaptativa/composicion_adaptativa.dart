import 'package:flutter/material.dart';

import '../tema/regenta_colors.dart';
import '../tema/regenta_spacing.dart';
import 'forma_de_pantalla.dart';

/// Una sola pantalla que se adapta: por debajo de [kBreakpointEscritorio] es la
/// composicion movil (una columna); por encima, la de escritorio con panel
/// lateral. Un unico `LayoutBuilder`, no dos widgets separados: `cuerpo` es el
/// mismo objeto en las dos ramas.
class ComposicionAdaptativa extends StatelessWidget {
  const ComposicionAdaptativa({
    required this.cuerpo,
    this.panelLateral,
    this.anchoPanel = 360,
    super.key,
  });

  /// Lo principal. En movil ocupa todo; en escritorio, la columna izquierda.
  final Widget cuerpo;

  /// El detalle a la derecha en escritorio. En movil no se muestra (va a otra
  /// ruta). Si es null, escritorio tambien es una sola columna.
  final Widget? panelLateral;

  final double anchoPanel;

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final forma = constraints.maxWidth < kBreakpointEscritorio
            ? FormaDePantalla.movil
            : FormaDePantalla.escritorio;

        final Widget contenido;
        if (forma == FormaDePantalla.escritorio && panelLateral != null) {
          contenido = Row(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Expanded(child: cuerpo),
              const VerticalDivider(width: 1, color: RegentaColors.line),
              SizedBox(
                width: anchoPanel,
                child: Padding(
                  padding: const EdgeInsets.all(RegentaSpacing.lg),
                  child: panelLateral,
                ),
              ),
            ],
          );
        } else {
          contenido = cuerpo;
        }

        return FormaDePantallaScope(forma: forma, child: contenido);
      },
    );
  }
}
