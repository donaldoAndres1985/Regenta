import 'dart:typed_data';

import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';
import 'package:regenta_core/regenta_core.dart';

import '../caja/datos/sesion_caja_modelos.dart';
import 'dialogo_de_cierre_por_metodo.dart';
import 'formato.dart';
import 'ticket_pos.dart';

/// HU-142 · la tirilla de cierre de caja, en el mismo formato térmico del tiquete del POS.
class GeneradorTirillaDeCierre {
  GeneradorTirillaDeCierre._();

  /// C4: el mismo papel que el tiquete del POS.
  static const PdfPageFormat formato = GeneradorTicketPos.formato80mm;

  static String _fecha(DateTime? f) {
    if (f == null) return '';
    final l = f.toLocal();
    String dos(int n) => n.toString().padLeft(2, '0');
    return '${l.year}-${dos(l.month)}-${dos(l.day)} ${dos(l.hour)}:${dos(l.minute)}';
  }

  /// El contenido de la tirilla, línea por línea. Lo usan el PDF y los tests.
  static List<String> lineas(
    TirillaDeCierreVista t, {
    required String razonSocial,
    required String nit,
    bool reimpresion = false,
  }) {
    return [
      if (reimpresion) '*** REIMPRESIÓN ***',
      razonSocial,
      if (nit.trim().isNotEmpty) 'NIT: $nit',
      'CIERRE DE CAJA',
      'Caja: ${t.caja}',
      'Turno: ${t.numero}',
      'Abrió: ${t.cajeroApertura}',
      'Cerró: ${t.cajeroCierre}',
      'Apertura: ${_fecha(t.abiertaEn)}',
      'Cierre: ${_fecha(t.cerradaEn)}',
      'Base: ${formatearPesos(t.base)}',
      '----------------------------------------',
      for (final l in t.lineas) ...[
        DialogoDeCierrePorMetodo.nombres[l.metodo] ?? l.metodo,
        if (l.cuadrable)
          '  Esperado ${formatearPesos(l.esperado)}  Contado ${formatearPesos(l.contado ?? 0)}  '
              'Dif ${formatearPesos((l.contado ?? 0) - l.esperado)}'
        else
          '  Esperado ${formatearPesos(l.esperado)}  NO SE CUADRA',
      ],
      '----------------------------------------',
      'Diferencia total: ${formatearPesos(t.diferenciaTotal)}',
      'Estado: ${t.estado}',
      if ((t.observaciones ?? '').trim().isNotEmpty) 'Obs: ${t.observaciones!.trim()}',
    ];
  }

  static Future<Uint8List> crearPdf(TirillaDeCierreVista t, {bool reimpresion = false}) {
    final doc = pw.Document();
    final texto = lineas(
      t,
      razonSocial: ConfiguradorDeTicket.razonSocial.isNotEmpty
          ? ConfiguradorDeTicket.razonSocial
          : ConfiguradorDeTicket.nombreNegocioOComercial,
      nit: ConfiguradorDeTicket.nit,
      reimpresion: reimpresion,
    );
    doc.addPage(pw.Page(
      pageFormat: formato,
      build: (_) => pw.Column(
        crossAxisAlignment: pw.CrossAxisAlignment.start,
        children: [
          for (final linea in texto)
            pw.Text(linea, style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold)),
        ],
      ),
    ));
    return doc.save();
  }

  /// Manda la tirilla a la impresora (diálogo del sistema), como el tiquete del POS.
  static Future<void> imprimir(TirillaDeCierreVista t, {bool reimpresion = false}) async {
    await Printing.layoutPdf(
      name: 'Cierre ${t.numero}',
      format: formato,
      onLayout: (_) => crearPdf(t, reimpresion: reimpresion),
    );
  }
}
