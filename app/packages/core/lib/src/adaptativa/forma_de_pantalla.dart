import 'package:flutter/widgets.dart';

import '../tema/regenta_spacing.dart';

/// La forma de la pantalla: no es un tipo de dispositivo, es en que lado del
/// corte cae el ancho disponible.
enum FormaDePantalla { movil, escritorio }

/// Deja la [FormaDePantalla] a mano de los descendientes sin que cada uno
/// vuelva a medir.
class FormaDePantallaScope extends InheritedWidget {
  const FormaDePantallaScope({
    required this.forma,
    required super.child,
    super.key,
  });

  final FormaDePantalla forma;

  static FormaDePantalla of(BuildContext context) {
    final scope =
        context.dependOnInheritedWidgetOfExactType<FormaDePantallaScope>();
    return scope?.forma ?? FormaDePantalla.movil;
  }

  @override
  bool updateShouldNotify(FormaDePantallaScope oldWidget) =>
      oldWidget.forma != forma;
}

extension FormaDePantallaContext on BuildContext {
  /// La forma vigente. Fuera de una [ComposicionAdaptativa] asume movil, que es
  /// lo prudente (objetivos de toque grandes, una columna).
  FormaDePantalla get forma => FormaDePantallaScope.of(this);

  bool get esMovil => forma == FormaDePantalla.movil;

  bool get esEscritorio => forma == FormaDePantalla.escritorio;

  /// Altura minima de un control tocable: 44 en movil (se usa de pie, con una
  /// mano), mas compacto en escritorio (raton, sentado).
  double get alturaDeControl =>
      esMovil ? RegentaSpacing.hitTarget : RegentaSpacing.controlWeb;
}
