import 'dart:ui' show lerpDouble;

import 'package:flutter/material.dart';

/// Estados de documento que se pintan como chip (ventas, facturas, compras).
enum EstadoDocumento { pagado, pendiente, anulado }

/// Tokens propios de Regenta que Material 3 no cubre:
/// colores de estado, degradados, color por módulo y forma.
/// Uso: `final t = Theme.of(context).extension<RegentaTokens>()!;`
@immutable
class RegentaTokens extends ThemeExtension<RegentaTokens> {
  const RegentaTokens({
    required this.success,
    required this.successContainer,
    required this.warning,
    required this.warningContainer,
    required this.info,
    required this.infoContainer,
    required this.anulado,
    required this.anuladoContainer,
    required this.fondo,
    required this.superficieElevada,
    required this.heroGradient,
    required this.modulos,
    required this.radioTarjeta,
    required this.radioControl,
    required this.sombraTarjeta,
    required this.vidrio,
  });

  final Color success;
  final Color successContainer;
  final Color warning;
  final Color warningContainer;
  final Color info;
  final Color infoContainer;
  final Color anulado;
  final Color anuladoContainer;
  final Color fondo;
  final Color superficieElevada;
  final LinearGradient heroGradient;

  /// Color por módulo: ventas, inventario, caja, clientes, compras, reportes.
  final Map<String, Color> modulos;
  final double radioTarjeta;
  final double radioControl;
  final List<BoxShadow> sombraTarjeta;

  /// Si la dirección usa superficies tipo vidrio (BackdropFilter).
  /// En web, úsalo solo en barras de navegación, nunca en listas.
  final bool vidrio;

  /// Colores (fondo, texto) del chip para un estado de documento.
  /// El estado anulado además debe tachar el monto: no depende solo del color.
  ({Color fondo, Color texto}) chip(EstadoDocumento e) => switch (e) {
        EstadoDocumento.pagado => (fondo: successContainer, texto: success),
        EstadoDocumento.pendiente => (fondo: warningContainer, texto: warning),
        EstadoDocumento.anulado => (fondo: anuladoContainer, texto: anulado),
      };

  Color modulo(String nombre) => modulos[nombre] ?? success;

  @override
  RegentaTokens copyWith({
    Color? success,
    Color? successContainer,
    Color? warning,
    Color? warningContainer,
    Color? info,
    Color? infoContainer,
    Color? anulado,
    Color? anuladoContainer,
    Color? fondo,
    Color? superficieElevada,
    LinearGradient? heroGradient,
    Map<String, Color>? modulos,
    double? radioTarjeta,
    double? radioControl,
    List<BoxShadow>? sombraTarjeta,
    bool? vidrio,
  }) {
    return RegentaTokens(
      success: success ?? this.success,
      successContainer: successContainer ?? this.successContainer,
      warning: warning ?? this.warning,
      warningContainer: warningContainer ?? this.warningContainer,
      info: info ?? this.info,
      infoContainer: infoContainer ?? this.infoContainer,
      anulado: anulado ?? this.anulado,
      anuladoContainer: anuladoContainer ?? this.anuladoContainer,
      fondo: fondo ?? this.fondo,
      superficieElevada: superficieElevada ?? this.superficieElevada,
      heroGradient: heroGradient ?? this.heroGradient,
      modulos: modulos ?? this.modulos,
      radioTarjeta: radioTarjeta ?? this.radioTarjeta,
      radioControl: radioControl ?? this.radioControl,
      sombraTarjeta: sombraTarjeta ?? this.sombraTarjeta,
      vidrio: vidrio ?? this.vidrio,
    );
  }

  @override
  RegentaTokens lerp(ThemeExtension<RegentaTokens>? other, double t) {
    if (other is! RegentaTokens) return this;
    Color l(Color a, Color b) => Color.lerp(a, b, t)!;
    return RegentaTokens(
      success: l(success, other.success),
      successContainer: l(successContainer, other.successContainer),
      warning: l(warning, other.warning),
      warningContainer: l(warningContainer, other.warningContainer),
      info: l(info, other.info),
      infoContainer: l(infoContainer, other.infoContainer),
      anulado: l(anulado, other.anulado),
      anuladoContainer: l(anuladoContainer, other.anuladoContainer),
      fondo: l(fondo, other.fondo),
      superficieElevada: l(superficieElevada, other.superficieElevada),
      heroGradient: LinearGradient.lerp(heroGradient, other.heroGradient, t)!,
      modulos: t < 0.5 ? modulos : other.modulos,
      radioTarjeta: lerpDouble(radioTarjeta, other.radioTarjeta, t)!,
      radioControl: lerpDouble(radioControl, other.radioControl, t)!,
      sombraTarjeta: BoxShadow.lerpList(sombraTarjeta, other.sombraTarjeta, t)!,
      vidrio: t < 0.5 ? vidrio : other.vidrio,
    );
  }
}
