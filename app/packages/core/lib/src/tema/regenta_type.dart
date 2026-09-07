import 'package:flutter/painting.dart';

import 'regenta_colors.dart';

/// Tipografia: Archivo para la interfaz, IBM Plex Mono para todo lo que es
/// codigo, dinero o identificador (SKU, NIT, CUFE, totales). Las dos familias
/// van empaquetadas con el nucleo, asi cargan igual en Android y en Web.
abstract final class RegentaType {
  static const ui = 'Archivo';
  static const mono = 'IBMPlexMono';

  static const tituloPantalla = TextStyle(
      fontFamily: ui, fontSize: 30, fontWeight: FontWeight.w700, letterSpacing: -0.6);
  static const seccion =
      TextStyle(fontFamily: ui, fontSize: 17, fontWeight: FontWeight.w600);
  static const item =
      TextStyle(fontFamily: ui, fontSize: 14, fontWeight: FontWeight.w500);
  static const cuerpo = TextStyle(fontFamily: ui, fontSize: 13);

  /// Cifras y codigos. tabular-nums para que las columnas cuadren.
  static const dinero = TextStyle(
      fontFamily: mono,
      fontSize: 22,
      fontWeight: FontWeight.w600,
      fontFeatures: [FontFeature.tabularFigures()]);
  static const codigo = TextStyle(fontFamily: mono, fontSize: 12);

  /// Etiqueta de campo: versalita monoespaciada.
  static const etiqueta = TextStyle(
      fontFamily: mono,
      fontSize: 9.5,
      fontWeight: FontWeight.w600,
      letterSpacing: 0.76,
      color: RegentaColors.muted);
}
