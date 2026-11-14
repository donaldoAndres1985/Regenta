import 'dart:convert';
import 'dart:typed_data';
import 'package:flutter/material.dart';
import 'package:pdf/pdf.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:printing/printing.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/repositorio_de_ventas.dart';
import 'formato.dart';

/// Datos formateados para imprimir una línea en el ticket térmico.
class LineaTicket {
  const LineaTicket({
    required this.nombre,
    required this.cantidad,
    required this.precioUnitario,
    required this.total,
    this.descuento = 0,
  });

  final String nombre;
  final num cantidad;
  final num precioUnitario;
  final num total;
  final num descuento;
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
/// Diseñado con la estructura y estética del tiquete POS de SISTEMA SARA (Factura_venta.jrxml).
class GeneradorTicketPos {
  static const PdfPageFormat formato80mm = PdfPageFormat(
    80 * PdfPageFormat.mm,
    double.infinity,
    marginLeft: 3.5 * PdfPageFormat.mm,
    marginRight: 3.5 * PdfPageFormat.mm,
    marginTop: 4 * PdfPageFormat.mm,
    marginBottom: 6 * PdfPageFormat.mm,
  );

  static Future<Uint8List> crearPdf({
    String? nombreNegocio,
    String? nit,
    String? direccion,
    String? telefono,
    String? celular,
    String? email,
    String? tipoComprobante,
    String? vendedor,
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
    String? mensajeAgradecimiento,
    String? mensajeGarantia,
    String? mensajeDevolucion,
    String? softwarePie,
  }) async {
    final pdf = pw.Document();

    final nombreFinal = (nombreNegocio != null && nombreNegocio.trim().isNotEmpty)
        ? nombreNegocio.trim()
        : ConfiguradorDeTicket.nombreNegocioOComercial;
    final nitFinal = (nit != null && nit.trim().isNotEmpty)
        ? nit.trim()
        : ConfiguradorDeTicket.nit.trim();
    final direccionFinal = (direccion != null && direccion.trim().isNotEmpty)
        ? direccion.trim()
        : ConfiguradorDeTicket.direccion.trim();
    final telefonoFinal = (telefono != null && telefono.trim().isNotEmpty)
        ? telefono.trim()
        : ConfiguradorDeTicket.telefono.trim();
    final celularFinal = (celular != null && celular.trim().isNotEmpty)
        ? celular.trim()
        : ConfiguradorDeTicket.celular.trim();
    final emailFinal = (email != null && email.trim().isNotEmpty)
        ? email.trim()
        : ConfiguradorDeTicket.email.trim();
    final tipoCompFinal = (tipoComprobante != null && tipoComprobante.trim().isNotEmpty)
        ? tipoComprobante.trim()
        : ConfiguradorDeTicket.tipoComprobante;
    final vendedorFinal = (vendedor != null && vendedor.trim().isNotEmpty)
        ? vendedor.trim()
        : ConfiguradorDeTicket.nombreVendedor;
    final agradecimientoFinal = mensajeAgradecimiento ?? ConfiguradorDeTicket.mensajeAgradecimiento;
    final garantiaFinal = mensajeGarantia ?? ConfiguradorDeTicket.mensajeGarantia;
    final devolucionFinal = mensajeDevolucion ?? ConfiguradorDeTicket.mensajeDevolucion;
    final softwareFinal = softwarePie ?? ConfiguradorDeTicket.softwarePie;

    // Desglosar fecha y hora si vienen juntas
    String fechaStr = fechaHora;
    String horaStr = '';
    if (fechaHora.contains(' ')) {
      final partes = fechaHora.split(' ');
      fechaStr = partes[0];
      horaStr = partes.sublist(1).join(' ');
    } else if (fechaHora.contains('T')) {
      final partes = fechaHora.split('T');
      fechaStr = partes[0];
      horaStr = partes.length > 1 ? partes[1].split('.')[0] : '';
    }

    final subtotalFinal = subtotal > 0 ? subtotal : (total + descuento - impuesto);

    pdf.addPage(
      pw.Page(
        pageFormat: formato80mm,
        build: (pw.Context context) {
          return pw.Column(
            crossAxisAlignment: pw.CrossAxisAlignment.stretch,
            children: [
              // ==========================================
              // 1. ENCABEZADO DEL NEGOCIO (Centrado)
              // ==========================================
              pw.Center(
                child: pw.Text(
                  nombreFinal.toUpperCase(),
                  style: pw.TextStyle(fontWeight: pw.FontWeight.bold, fontSize: 11),
                  textAlign: pw.TextAlign.center,
                ),
              ),
              if (nitFinal.isNotEmpty) ...[
                pw.SizedBox(height: 1),
                pw.Center(
                  child: pw.Text(
                    'NIT: $nitFinal',
                    style: const pw.TextStyle(fontSize: 8.5),
                    textAlign: pw.TextAlign.center,
                  ),
                ),
              ],
              if (direccionFinal.isNotEmpty) ...[
                pw.SizedBox(height: 1),
                pw.Center(
                  child: pw.Text(
                    direccionFinal.toUpperCase(),
                    style: const pw.TextStyle(fontSize: 8.5),
                    textAlign: pw.TextAlign.center,
                  ),
                ),
              ],
              if (telefonoFinal.isNotEmpty || celularFinal.isNotEmpty) ...[
                pw.SizedBox(height: 1),
                pw.Center(
                  child: pw.Text(
                    'Tel: $telefonoFinal${celularFinal.isNotEmpty ? " - Cel: $celularFinal" : ""}',
                    style: const pw.TextStyle(fontSize: 8.5),
                    textAlign: pw.TextAlign.center,
                  ),
                ),
              ],
              if (emailFinal.isNotEmpty) ...[
                pw.SizedBox(height: 1),
                pw.Center(
                  child: pw.Text(
                    'E-mail: $emailFinal',
                    style: const pw.TextStyle(fontSize: 8.5),
                    textAlign: pw.TextAlign.center,
                  ),
                ),
              ],
              pw.SizedBox(height: 2),
              _separador(),

              // ==========================================
              // 2. METADATOS DE LA VENTA (Alineado a la izquierda)
              // ==========================================
              _filaDato('FECHA     :', fechaStr),
              if (horaStr.isNotEmpty) _filaDato('HORA       :', horaStr),
              _filaDato(
                'CLIENTE :',
                (nombreCliente != null && nombreCliente.trim().isNotEmpty
                        ? nombreCliente.trim()
                        : 'CONSUMIDOR FINAL')
                    .toUpperCase(),
              ),
              _filaDato(
                'CC / NIT   :',
                (identificacionCliente != null && identificacionCliente.trim().isNotEmpty
                    ? identificacionCliente.trim()
                    : '222222222222'),
              ),
              pw.SizedBox(height: 2),
              _separador(),
              _filaDato('TIPO               :', tipoCompFinal.toUpperCase()),
              _filaDato('TIQUETE N° :', numeroVenta),
              _filaDato('VENDEDOR  :', vendedorFinal.toUpperCase()),
              _separador(),

              // ==========================================
              // 3. TABLA DE PRODUCTOS (Encabezado)
              // ==========================================
              pw.Row(
                children: [
                  pw.SizedBox(
                    width: 26,
                    child: pw.Text('CANT.', style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold)),
                  ),
                  pw.Expanded(
                    child: pw.Text('PRODUCTO', style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold)),
                  ),
                  pw.SizedBox(
                    width: 48,
                    child: pw.Text('V.UNIT.', textAlign: pw.TextAlign.right, style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold)),
                  ),
                  pw.SizedBox(
                    width: 52,
                    child: pw.Text('TOTAL', textAlign: pw.TextAlign.right, style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold)),
                  ),
                ],
              ),
              _separador(),

              // ==========================================
              // 4. DETALLE DE ÍTEMS
              // ==========================================
              for (final l in lineas) ...[
                pw.Padding(
                  padding: const pw.EdgeInsets.symmetric(vertical: 1.5),
                  child: pw.Row(
                    crossAxisAlignment: pw.CrossAxisAlignment.start,
                    children: [
                      pw.SizedBox(
                        width: 26,
                        child: pw.Text(
                          l.cantidad % 1 == 0 ? l.cantidad.toInt().toString() : l.cantidad.toString(),
                          style: const pw.TextStyle(fontSize: 8),
                        ),
                      ),
                      pw.Expanded(
                        child: pw.Text(
                          l.nombre.toUpperCase(),
                          style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold),
                        ),
                      ),
                      pw.SizedBox(
                        width: 48,
                        child: pw.Text(
                          formatearPesos(l.precioUnitario),
                          textAlign: pw.TextAlign.right,
                          style: const pw.TextStyle(fontSize: 8),
                        ),
                      ),
                      pw.SizedBox(
                        width: 52,
                        child: pw.Text(
                          formatearPesos(l.total),
                          textAlign: pw.TextAlign.right,
                          style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold),
                        ),
                      ),
                    ],
                  ),
                ),
              ],
              _separador(),

              // ==========================================
              // 5. TOTALES (Alineados a la derecha)
              // ==========================================
              _filaTotal('SUBTOTAL', formatearPesos(subtotalFinal)),
              if (descuento > 0)
                _filaTotal('DESCUENTO', '- ${formatearPesos(descuento)}'),
              if (impuesto > 0)
                _filaTotal('IVA', formatearPesos(impuesto)),
              pw.Padding(
                padding: const pw.EdgeInsets.symmetric(vertical: 1),
                child: _filaTotal(
                  'TOTAL A PAGAR',
                  formatearPesos(total),
                  destacado: true,
                ),
              ),
              _separador(),

              // ==========================================
              // 6. FORMAS DE PAGO (Si existen)
              // ==========================================
              if (pagos.isNotEmpty) ...[
                pw.Center(
                  child: pw.Text(
                    'FORMA DE PAGO',
                    style: pw.TextStyle(fontSize: 8.5, fontWeight: pw.FontWeight.bold),
                  ),
                ),
                pw.SizedBox(height: 2),
                pw.Row(
                  children: [
                    pw.Expanded(
                      flex: 4,
                      child: pw.Text('TIPO PAGO', style: pw.TextStyle(fontSize: 7.5, fontWeight: pw.FontWeight.bold)),
                    ),
                    pw.Expanded(
                      flex: 3,
                      child: pw.Text('CANCELO', textAlign: pw.TextAlign.right, style: pw.TextStyle(fontSize: 7.5, fontWeight: pw.FontWeight.bold)),
                    ),
                    pw.Expanded(
                      flex: 3,
                      child: pw.Text('CAMBIO', textAlign: pw.TextAlign.right, style: pw.TextStyle(fontSize: 7.5, fontWeight: pw.FontWeight.bold)),
                    ),
                  ],
                ),
                pw.Divider(thickness: 0.5),
                for (final p in pagos) ...[
                  pw.Row(
                    children: [
                      pw.Expanded(
                        flex: 4,
                        child: pw.Text(p.metodo.toUpperCase(), style: const pw.TextStyle(fontSize: 8)),
                      ),
                      pw.Expanded(
                        flex: 3,
                        child: pw.Text(formatearPesos(p.monto), textAlign: pw.TextAlign.right, style: const pw.TextStyle(fontSize: 8)),
                      ),
                      pw.Expanded(
                        flex: 3,
                        child: pw.Text(formatearPesos(cambio), textAlign: pw.TextAlign.right, style: const pw.TextStyle(fontSize: 8)),
                      ),
                    ],
                  ),
                ],
                _separador(),
              ],

              if (nota != null && nota.trim().isNotEmpty) ...[
                pw.Text('Nota: $nota', style: const pw.TextStyle(fontSize: 7.5)),
                pw.SizedBox(height: 3),
              ],

              // ==========================================
              // 7. PIE DE PÁGINA (Idéntico a SISTEMA SARA)
              // ==========================================
              pw.SizedBox(height: 4),
              pw.Center(
                child: pw.Text(
                  agradecimientoFinal,
                  style: pw.TextStyle(fontSize: 9.5, fontWeight: pw.FontWeight.bold),
                ),
              ),
              if (garantiaFinal.isNotEmpty) ...[
                pw.SizedBox(height: 2),
                pw.Center(
                  child: pw.Text(
                    garantiaFinal,
                    style: pw.TextStyle(fontSize: 7.5, fontWeight: pw.FontWeight.bold),
                  ),
                ),
              ],
              if (devolucionFinal.isNotEmpty) ...[
                pw.SizedBox(height: 1),
                pw.Center(
                  child: pw.Text(
                    devolucionFinal,
                    style: pw.TextStyle(fontSize: 7.5, fontWeight: pw.FontWeight.bold),
                  ),
                ),
              ],
              if (softwareFinal.isNotEmpty) ...[
                pw.SizedBox(height: 6),
                pw.Center(
                  child: pw.Text(
                    softwareFinal,
                    style: pw.TextStyle(fontSize: 7, fontStyle: pw.FontStyle.italic),
                  ),
                ),
              ],
            ],
          );
        },
      ),
    );

    return pdf.save();
  }

  static pw.Widget _separador() {
    return pw.Padding(
      padding: const pw.EdgeInsets.symmetric(vertical: 1.5),
      child: pw.Text(
        '======================================================',
        maxLines: 1,
        overflow: pw.TextOverflow.clip,
        style: pw.TextStyle(fontSize: 7.5, fontWeight: pw.FontWeight.bold),
        textAlign: pw.TextAlign.center,
      ),
    );
  }

  static pw.Widget _filaDato(String etiqueta, String valor) {
    return pw.Padding(
      padding: const pw.EdgeInsets.symmetric(vertical: 0.5),
      child: pw.Row(
        children: [
          pw.SizedBox(
            width: 72,
            child: pw.Text(
              etiqueta,
              style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold),
            ),
          ),
          pw.Expanded(
            child: pw.Text(
              valor,
              style: pw.TextStyle(fontSize: 8, fontWeight: pw.FontWeight.bold),
            ),
          ),
        ],
      ),
    );
  }

  static pw.Widget _filaTotal(String etiqueta, String valor, {bool destacado = false}) {
    return pw.Padding(
      padding: const pw.EdgeInsets.symmetric(vertical: 0.8),
      child: pw.Row(
        mainAxisAlignment: pw.MainAxisAlignment.spaceBetween,
        children: [
          pw.Text(
            etiqueta,
            style: pw.TextStyle(
              fontSize: destacado ? 10 : 8,
              fontWeight: pw.FontWeight.bold,
            ),
          ),
          pw.Text(
            valor,
            style: pw.TextStyle(
              fontSize: destacado ? 10.5 : 8.5,
              fontWeight: pw.FontWeight.bold,
            ),
          ),
        ],
      ),
    );
  }

  /// Abre la vista de impresión del sistema o previsualización.
  static Future<void> imprimirOPrevisualizar(
    BuildContext context, {
    String? nombreNegocio,
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

  /// Abre la vista de impresión o previsualización para una venta cobrada en formato térmico de 80mm.
  static Future<void> imprimirOPrevisualizarTicketCobrado(
    BuildContext context, {
    String? nombreNegocio,
    required String numeroVenta,
    String? fechaHora,
    String? clienteNombre,
    String? clienteDoc,
    required List<LineaTicket> lineas,
    required num total,
    List<PagoTicket> pagos = const [],
    num cambio = 0,
  }) async {
    final fechaFinal = fechaHora ?? DateTime.now().toString().split('.')[0];
    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => Scaffold(
          appBar: AppBar(title: Text('Ticket Venta #$numeroVenta')),
          body: PdfPreview(
            build: (format) => crearPdf(
              nombreNegocio: nombreNegocio ?? ConfiguradorDeTicket.nombreNegocioOComercial,
              numeroVenta: numeroVenta,
              fechaHora: fechaFinal,
              nombreCliente: clienteNombre,
              identificacionCliente: clienteDoc,
              lineas: lineas,
              total: total,
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

  /// Abre la vista de impresión o previsualización para una cotización en formato térmico de 80mm
  /// con los datos de configuración del negocio (estilo Cotizacion.jrxml de SISTEMA SARA).
  static Future<void> imprimirOPrevisualizarCotizacion(
    BuildContext context, {
    String? nombreNegocio,
    required String numeroCotizacion,
    String? fechaHora,
    String? clienteNombre,
    String? clienteDoc,
    required List<LineaTicket> lineas,
    required num total,
    num subtotal = 0,
    num descuento = 0,
    num impuesto = 0,
    String? validaHasta,
  }) async {
    final fechaFinal = fechaHora ?? DateTime.now().toString().split('.')[0];
    final subtotalFinal = subtotal > 0 ? subtotal : (total + descuento - impuesto);

    await Navigator.of(context).push(
      MaterialPageRoute(
        builder: (_) => Scaffold(
          appBar: AppBar(
            title: Text('Cotización #$numeroCotizacion'),
          ),
          body: PdfPreview(
            build: (format) => crearPdf(
              nombreNegocio: nombreNegocio,
              tipoComprobante: 'COTIZACION',
              numeroVenta: numeroCotizacion,
              fechaHora: fechaFinal,
              nombreCliente: clienteNombre,
              identificacionCliente: clienteDoc,
              lineas: lineas,
              total: total,
              subtotal: subtotalFinal,
              descuento: descuento,
              impuesto: impuesto,
              nota: (validaHasta != null && validaHasta.trim().isNotEmpty)
                  ? 'Válida hasta: ${validaHasta.trim()}'
                  : null,
              mensajeAgradecimiento: 'GRACIAS POR PREFERIRNOS',
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
