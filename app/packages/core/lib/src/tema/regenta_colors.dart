import 'package:flutter/painting.dart';

/// Paleta de Regenta. Neutros calidos y un solo acento ocre.
///
/// Los hex son los mismos que usan los mockups de `design/pantallas/`. Si
/// cambia uno aqui, cambia tambien alla o dejaran de coincidir. Ningun widget
/// escribe un `Color(0x...)` a mano: sale de aqui.
abstract final class RegentaColors {
  static const paper = Color(0xFFFBFAF7);
  static const surface = Color(0xFFFFFFFF);
  static const sunken = Color(0xFFF3F1EB);
  static const ink = Color(0xFF1A1815);
  static const ink2 = Color(0xFF4A4740);
  static const muted = Color(0xFF7C776C);
  static const faint = Color(0xFFA39D91);
  static const line = Color(0xFFE4E0D8);
  static const line2 = Color(0xFFD2CCC0);
  static const accent = Color(0xFF9A5709);
  static const accentSoft = Color(0xFFF6EBD9);

  // Un color por patron operativo: permite cruzar cualquier pantalla con su
  // parte del modelo.
  static const core = Color(0xFF4A5C6D);
  static const coreSoft = Color(0xFFE7ECF0);
  static const venta = Color(0xFF9A5709);
  static const ventaSoft = Color(0xFFF6EBD9);
  static const reserva = Color(0xFF0C6473);
  static const reservaSoft = Color(0xFFDFEFF2);
  static const comanda = Color(0xFFA03325);
  static const comandaSoft = Color(0xFFF8E6E3);

  // Semanticos: separados del acento a proposito.
  static const ok = Color(0xFF1E6B45);
  static const okSoft = Color(0xFFE4F0E9);
  static const warn = Color(0xFF8A5B06);
  static const warnSoft = Color(0xFFFAF0DC);
  static const crit = Color(0xFFA0271B);
  static const critSoft = Color(0xFFF9E7E4);
  static const info = Color(0xFF0C6473);
  static const infoSoft = Color(0xFFDFEFF2);

  // KDS (HU-088): la unica pantalla de fondo oscuro a proposito, se mira de
  // lejos en una cocina. No son parte del tema claro general.
  static const kdsFondo = Color(0xFF1B1916);
  static const kdsBarra = Color(0xFF22201C);
  static const kdsBorde = Color(0xFF33302B);
  static const kdsPestanaInactiva = Color(0xFF2C2925);
  static const kdsPestanaInactivaTexto = Color(0xFFB9B3A6);
  static const kdsTexto = Color(0xFFF2EFE9);
  static const kdsNota = Color(0xFFE8A08F);
  static const kdsVacioBorde = Color(0xFF3A362F);
  static const kdsDivisor = Color(0x14FFFFFF);
  static const kdsBotonNeutral = Color(0x17FFFFFF);
}
