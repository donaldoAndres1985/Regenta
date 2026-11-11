import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';

import '../datos/repositorio_de_ventas.dart';
import 'formato.dart';

/// Datos formateados para imprimir una línea en el ticket térmico.
class LineaTicket {
  const LineaTicket({
    required this.nombre,
    required this.cantidad,
    required this.precioUnitario,
    required this.total,
  });

  final String nombre;
  final num cantidad;
  final num precioUnitario;
  final num total;
}

/// Datos formateados de un pago registrado.
class PagoTicket {
  const PagoTicket({
    required this.metodo,
    required this.monto,
  });

  final String metodo;
  final num monto;
}

/// Generador de tirilla térmica de 80mm en PDF (HU-045).
class GeneradorTicketPos {
  static const PdfPageFormat formato80mm = PdfPageFormat(
    80 * PdfPageFormat.mm,
    double.infinity,
    marginLeft: 4 * PdfPageFormat.mm,
    marginRight: 4 * PdfPageFormat.mm,
    marginTop: 4 * PdfPageFormat.mm,
    marginBottom: 6 * PdfPageFormat.mm,
  );

  static Future<Uint8List> crearPdf({
    required String nombreNegocio,
    required String numeroVenta,
    required String fechaHora,
    String? nombreCliente,
    String? identificacionCliente,
    required List<LineaTicket> lineas,
    required num total,
    num subtotal = 0,
    num descuento = 0,
    num impuesto = 0,
    List<PagoTicket> pagos = const [],
    num cambio = 0,
    String? nota,
  }) async {
    final pdf = pw.Document();

    pdf.addPage(
      pw.Page(
        pageFormat: formato80mm,
        build: (pw.Context context) {
          return pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.start,
            children: [
              // Encabezado
              pw.Center(
                child: pw.Text(
                  nombreNegocio.toUpperCase(),
                  style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 13),
                  textAlign: pw.TextAlign.center,
                ),
              ),
              pw.SizedBox(height: 2),
              pw.Center(
                child: pw.Text(
                  'COMPROBANTE DE VENTA',
                  style: pw.TextStyle(fontSize: 9, fontWeight: pw.FontWeight.bold),
                ),
              ),
              pw.Center(
                child: pw.Text(
                  'Venta No. $numeroVenta',
                  style: pw.TextStyle(fontSize: 10, fontWeight: pw.FontWeight.bold),
                ),
              ),
              pw.Center(
                child: pw.Text(
                  fechaHora,
                  style: const pw.TextStyle(fontSize: 8),
                ),
              ),
              pw.Divider(thickness: 0.5),

              // Cliente
              pw.Text(
                'Cliente: ${nombreCliente ?? "Consumidor final"}',
                style: const pw.TextStyle(fontSize: 8.5),
              ),
              if (identificacionCliente != null && identificacionCliente.isNotEmpty)
                pw.Text(
                  'NIT/CC: $identificacionCliente',
                  style: const pw.TextStyle(fontSize: 8.5),
                ),
              pw.Divider(thickness: 0.5),

              // Tabla de productos
              pw.Row(
                mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                children: [
                  pw.Expanded(
                    flex: 3,
                    child: pw.Text('Cant · Producto',
                        style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold)),
                  ),
                  pw.Text('Total',
                      style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold)),
                ],
              ),
              pw.SizedBox(height: 3),

              for (final l in lineas) ...[
                pw.Row(
                  crossAxisAlignment: pw.CrossAxisAlignment.start,
                  mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                  children: [
                    pw.Expanded(
                      child: pw.Text(
                        '${l.cantidad} x ${l.nombre}',
                        style: const pw.TextStyle(fontSize: 8),
                      ),
                    ),
                    pw.Text(
                      formatearPesos(l.total),
                      style: const pw.TextStyle(fontSize: 8),
                    ),
                  ],
                ),
                pw.SizedBox(height: 2),
              ],

              pw.Divider(thickness: 0.5),

              // Totales
              if (subtotal > 0 && subtotal != total) ...[
                _filaTotalPdf('Subtotal:', formatearPesos(subtotal)),
              ],
              if (descuento > 0) ...[
                _filaTotalPdf('Descuento:', '- ${formatearPesos(descuento)}'),
              ],
              if (impuesto > 0) ...[
                _filaTotalPdf('IVA:', formatearPesos(impuesto)),
              ],
              pw.SizedBox(height: 2),
              pw.Row(
                mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
                children: [
                  pw.Text('TOTAL:',
                      style: pw.TextStyle(fontSize: 11, fontWeight: pw.FontWeight.bold)),
                  pw.Text(formatearPesos(total),
                      style: pw.TextStyle(fontSize: 11, fontWeight: pw.FontWeight.bold)),
                ],
              ),

              // Pagos
              if (pagos.isNotEmpty) ...[
                pw.Divider(thickness: 0.5),
                pw.Text('PAGOS REGISTRADOS:',
                    style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold)),
                for (final p in pagos) ...[
                  _filaTotalPdf(p.metodo, formatearPesos(p.monto)),
                ],
                if (cambio > 0) ...[
                  _filaTotalPdf('Cambio entregado:', formatearPesos(cambio)),
                ],
              ],

              if (nota != null && nota.trim().isNotEmpty) ...[
                pw.SizedBox(height: 4),
                pw.Text('Nota: $nota', style: const pw.TextStyle(fontSize: 7.5)),
              ],

              pw.SizedBox(height: 8),
              pw.Center(
                child: pw.Text(
                  '¡Gracias por su compra!',
                  style: pw.TextStyle(fontSize: 8.5, fontStyle: pw.FontStyle.italic),
                ),
              ),
            ],
          );
        },
      ),
    );

    return pdf.save();
  }

  static pw.Widget _filaTotalPdf(String etiqueta, String valor) {
    return pw.Padding(
      padding: const pw.EdgeInsets.symmetric(vertical: 0.5),
      child: pw.Row(
        mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
        children: [
          pw.Text(etiqueta, style: const pw.TextStyle(fontSize: 8)),
          pw.Text(valor, style: const pw.TextStyle(fontSize: 8)),
        ],
      ),
    );
  }

  /// Abre la vista de impresión del sistema o previsualización.
  static Future<void> imprimirOPrevisualizar(
    BuildContext context, {
    required String nombreNegocio,
    required VentaEnProceso venta,
    List<PagoTicket> pagos = const [],
    num cambio = 0,
  }) async {
    String? clienteNombre;
    String? clienteDoc;
    if (venta.clienteSnapshot != null && venta.clienteSnapshot!.isNotEmpty) {
      try {
        final mapa = jsonDecode(venta.clienteSnapshot!);
        if (mapa is Map) {
          clienteNombre = mapa['nombreCompleto'] ?? mapa['razonSocial'] ?? mapa['nombre'];
          clienteDoc = mapa['identificacion'] ?? mapa['documento'] ?? mapa['numeroDocumento'];
        }
      } catch (_) {}
    }

    final lineasTicket = venta.lineas
        .map((l) => LineaTicket(
              nombre: l.nombre,
              cantidad: l.cantidad,
              precioUnitario: l.precioUnitario,
              total: l.total,
            ))
        .toList();

    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => Scaffold(
          appBar: AppBar(
            title: Text('Ticket Venta #${venta.numero}'),
          ),
          body: PdfPreview(
            build: (format) => crearPdf(
              nombreNegocio: nombreNegocio,
              numeroVenta: venta.numero,
              fechaHora: DateTime.now().toString().split('.')[0],
              nombreCliente: clienteNombre,
              identificacionCliente: clienteDoc,
              lineas: lineasTicket,
              total: venta.total,
              nota: venta.notaCliente,
              pagos: pagos,
              cambio: cambio,
            ),
            canChangeOrientation: false,
            canChangePageFormat: false,
            initialPageFormat: formato80mm,
            actions: [
              IconButton(
                icon: const Icon(Icons.close),
                onPressed: () => Navigator.of(context).pop(),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
