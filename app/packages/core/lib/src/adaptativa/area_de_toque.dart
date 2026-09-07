import 'package:flutter/material.dart';

import 'forma_de_pantalla.dart';

/// Envuelve algo tocable y le garantiza el objetivo minimo: 44 px de alto en
/// movil (`RegentaSpacing.hitTarget`), mas compacto en escritorio. Asi ningun
/// control se queda por debajo del minimo sin que haya que acordarse en cada
/// pantalla (HU-108, criterio 4).
class AreaDeToque extends StatelessWidget {
  const AreaDeToque({
    required this.child,
    this.onTap,
    this.padding = const EdgeInsets.symmetric(horizontal: 8),
    super.key,
  });

  final Widget child;
  final VoidCallback? onTap;
  final EdgeInsetsGeometry padding;

  @override
  Widget build(BuildContext context) {
    final minimo = context.alturaDeControl;
    return InkWell(
      onTap: onTap,
      child: ConstrainedBox(
        constraints: BoxConstraints(minHeight: minimo, minWidth: minimo),
        child: Padding(
          padding: padding,
          child: Align(
            alignment: Alignment.center,
            heightFactor: 1,
            widthFactor: 1,
            child: child,
          ),
        ),
      ),
    );
  }
}
