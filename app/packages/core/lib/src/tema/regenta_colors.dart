import 'package:flutter/painting.dart';

/// Paleta de Regenta: Pastel Profesional (modo claro).
///
/// Mismos valores que el ColorScheme y los RegentaTokens de PastelTheme. Los
/// pasteles son fondos, nunca texto.
abstract final class RegentaColors {
  static const paper = Color(0xFFF8F8FD);
  static const surface = Color(0xFFFFFFFF);
  static const sunken = Color(0xFFF1F1F5);
  static const ink = Color(0xFF25253A);
  static const ink2 = Color(0xFF5D5D78);
  /// Texto terciario: etiquetas de campo, códigos secundarios.
  static const muted = Color(0xFF717183);

  /// Placeholders e íconos de apoyo. No es un borde: para eso, [line].
  static const faint = Color(0xFFA6A6B5);
  static const line = Color(0xFFE7E7F2);
  static const line2 = Color(0xFFE9E9EE);
  static const accent = Color(0xFF2E62C0);
  static const accentSoft = Color(0xFFE2E9F6);

  // Un color por patrón operativo (es el secundario del tema):
  static const core = Color(0xFF2E62C0);
  static const coreSoft = Color(0xFFE2E9F6);
  static const venta = Color(0xFF257A6A);
  static const ventaSoft = Color(0xFFE0ECEA);
  static const reserva = Color(0xFF5B57C2);
  static const reservaSoft = Color(0xFFECEBFF);
  static const comanda = Color(0xFFC2416F);
  static const comandaSoft = Color(0xFFF6E4EB);

  // Semánticos:
  static const ok = Color(0xFF1D7550);
  static const okSoft = Color(0xFFDFECE6);
  static const warn = Color(0xFF935700);
  static const warnSoft = Color(0xFFF0E7DB);
  static const crit = Color(0xFFB3304F);
  static const critSoft = Color(0xFFF4E2E6);
  static const info = Color(0xFF2E62C0);
  static const infoSoft = Color(0xFFE2E9F6);
  static const anulado = Color(0xFF5D5D78);
  static const anuladoSoft = Color(0xFFEFEFF2);

  // Color por módulo (fondos pastel de tarjetas e íconos):
  static const moduloVentas = Color(0xFFECEBFF);
  static const moduloInventario = Color(0xFFDDF5EE);
  static const moduloCaja = Color(0xFFE0F0FF);
  static const moduloClientes = Color(0xFFFFE4EE);
  static const moduloCompras = Color(0xFFFFF4C7);
  static const moduloReportes = Color(0xFFFFE9DC);

  // POS · detalle de producto y selector de fracciones (Etapa 5, HU-128):
  // estos dialogos son siempre oscuros, sin importar el tema de la app, igual
  // que el KDS de mas abajo.
  static const posStockCeroFondo = Color(0xFF451A1A);
  static const posStockCeroBorde = Color(0xFFDC2626);
  static const posStockCeroTexto = Color(0xFFFCA5A5);
  static const posStockBajoFondo = Color(0xFF452B14);
  static const posStockBajoBorde = Color(0xFFD97706);
  static const posStockBajoTexto = Color(0xFFFCD34D);
  static const posStockOkFondo = Color(0xFF133827);
  static const posStockOkBorde = Color(0xFF059669);
  static const posStockOkTexto = Color(0xFF6EE7B7);
  static const posModalFondo = Color(0xFF1E202C);
  static const posModalBorde = Color(0xFF334155);
  static const posModalAcento = Color(0xFF60A5FA);
  static const posModalTitulo = Color(0xFFF1F5F9);
  static const posModalTextoSecundario = Color(0xFFE2E8F0);
  static const posModalTextoTenue = Color(0xFFCBD5E1);
  static const posChipFondo = Color(0xFF2A2B3D);
  static const posChipBorde = Color(0xFF64748B);
  static const posChipBordeSeleccionado = Color(0xFF93C5FD);
  static const posAcentoFuerte = Color(0xFF2563EB);

  /// Ink de mayor contraste que [ink], para texto sobre el stepper de
  /// cantidad en modo claro (Etapa 5). El stepper es el unico control de
  /// Ventas que de verdad sigue el brillo del tema en vez de ser siempre
  /// claro u oscuro; su variante oscura reutiliza [kdsTexto].
  static const inkFuerte = Color(0xFF151522);
  static const stepperFondoOscuro = Color(0xFF28283A);
  static const stepperBordeOscuro = Color(0xFF4C4C66);
  static const stepperCampoOscuro = Color(0xFF0F172A);

  // KDS (siempre oscuro):
  static const kdsFondo = Color(0xFF15151E);
  static const kdsBarra = Color(0xFF1E1E2A);
  static const kdsBorde = Color(0xFF33334A);
  static const kdsPestanaInactiva = Color(0xFF282836);
  static const kdsPestanaInactivaTexto = Color(0xFFB5B5CC);
  static const kdsTexto = Color(0xFFF2F2FA);
  static const kdsNota = Color(0xFFFFA3C7);
  static const kdsVacioBorde = Color(0xFF42424E);
  static const kdsDivisor = Color(0x14FFFFFF);
  static const kdsBotonNeutral = Color(0x17FFFFFF);
}
