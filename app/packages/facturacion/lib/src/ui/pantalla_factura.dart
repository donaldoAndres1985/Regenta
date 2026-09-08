import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/factura_vista.dart';
import '../factura/estado_de_factura.dart';
import '../factura/proveedores.dart';
import 'formato.dart';

/// La pantalla de una factura: verla, copiar el CUFE y enviarla al cliente.
/// HU-058. Un solo widget que se adapta con `LayoutBuilder` en
/// [kBreakpointEscritorio].
class PantallaFactura extends ConsumerWidget {
  const PantallaFactura({
    super.key,
    required this.facturaId,
    this.onVolver,
    this.onImprimir,
  });

  final String facturaId;
  final VoidCallback? onVolver;
  final VoidCallback? onImprimir;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final estado = ref.watch(controladorDeFacturaProvider(facturaId));
    final ctrl = ref.read(controladorDeFacturaProvider(facturaId).notifier);

    ref.listen<EstadoDeFactura>(controladorDeFacturaProvider(facturaId), (antes, ahora) {
      if (ahora.avisoEnvio != null && ahora.avisoEnvio != antes?.avisoEnvio) {
        ScaffoldMessenger.of(context)
            .showSnackBar(SnackBar(content: Text(ahora.avisoEnvio!)));
      }
    });

    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        leading: onVolver == null
            ? null
            : IconButton(icon: const Icon(Icons.arrow_back), onPressed: onVolver),
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [
            Text('Factura',
                style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
            if (estado.factura != null)
              Text(estado.factura!.numeroCompleto,
                  style: RegentaType.codigo.copyWith(fontSize: 10, color: RegentaColors.muted)),
          ],
        ),
      ),
      body: LayoutBuilder(
        builder: (context, restricciones) {
          if (estado.cargando) {
            return const Center(child: CircularProgressIndicator());
          }
          if (estado.error != null || estado.factura == null) {
            return Center(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Text(estado.error ?? 'No se pudo cargar la factura',
                    textAlign: TextAlign.center,
                    style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted)),
              ),
            );
          }
          final f = estado.factura!;
          final esEscritorio = restricciones.maxWidth >= kBreakpointEscritorio;
          final cuerpo = _Cuerpo(
            factura: f,
            enviando: estado.enviando,
            puedeEnviar: estado.puedeEnviar,
            onEnviar: () => ctrl.enviar(),
            onImprimir: onImprimir,
          );
          if (esEscritorio) {
            return Align(
              key: const Key('factura-escritorio'),
              alignment: Alignment.topCenter,
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 720),
                child: cuerpo,
              ),
            );
          }
          return KeyedSubtree(key: const Key('factura-movil'), child: cuerpo);
        },
      ),
    );
  }
}

class _Cuerpo extends StatelessWidget {
  const _Cuerpo({
    required this.factura,
    required this.enviando,
    required this.puedeEnviar,
    required this.onEnviar,
    this.onImprimir,
  });

  final FacturaVista factura;
  final bool enviando;
  final bool puedeEnviar;
  final VoidCallback onEnviar;
  final VoidCallback? onImprimir;

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(14),
      children: [
        Container(
          decoration: BoxDecoration(
            color: RegentaColors.surface,
            border: Border.all(color: RegentaColors.line),
            borderRadius: BorderRadius.circular(RegentaSpacing.radiusCard),
          ),
          padding: const EdgeInsets.all(15),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(factura.numeroCompleto,
                            style: RegentaType.codigo.copyWith(
                                fontSize: 19,
                                fontWeight: FontWeight.w600,
                                color: RegentaColors.ink)),
                        const SizedBox(height: 4),
                        Text(factura.fechaEmision,
                            style: RegentaType.codigo
                                .copyWith(fontSize: 11, color: RegentaColors.muted)),
                      ],
                    ),
                  ),
                  const SizedBox(width: 10),
                  _ChipEstado(estado: factura.estado),
                ],
              ),
              if (factura.rechazada) ...[
                const SizedBox(height: 12),
                _MotivoRechazo(codigo: factura.codigoRechazo, mensaje: factura.mensajeRechazo),
              ],
              const Divider(height: 26, color: RegentaColors.line),
              _Etiqueta('Emisor'),
              const SizedBox(height: 5),
              Text(factura.nombreEmisor,
                  style: RegentaType.item.copyWith(color: RegentaColors.ink)),
              const SizedBox(height: 12),
              _Etiqueta('Adquiriente'),
              const SizedBox(height: 5),
              Text(factura.nombreAdquiriente,
                  style: RegentaType.item
                      .copyWith(fontWeight: FontWeight.w600, color: RegentaColors.ink)),
              if (factura.documentoAdquiriente != null) ...[
                const SizedBox(height: 3),
                Text(factura.documentoAdquiriente!,
                    style: RegentaType.codigo
                        .copyWith(fontSize: 11, color: RegentaColors.muted)),
              ],
              const SizedBox(height: 16),
              Text('Detalle',
                  style: RegentaType.item.copyWith(color: RegentaColors.ink)),
              const SizedBox(height: 4),
              for (final l in factura.lineas) _LineaFactura(linea: l),
              const SizedBox(height: 12),
              _Totales(factura: factura),
            ],
          ),
        ),
        if (factura.cufe != null) ...[
          const SizedBox(height: 16),
          _Cufe(cufe: factura.cufe!),
        ],
        if (factura.urlDelQr != null) ...[
          const SizedBox(height: 12),
          _Qr(url: factura.urlDelQr!),
        ],
        if (factura.trazabilidad.isNotEmpty) ...[
          const SizedBox(height: 16),
          _Trazabilidad(pasos: factura.trazabilidad),
        ],
        const SizedBox(height: 16),
        _Acciones(
          enviando: enviando,
          puedeEnviar: puedeEnviar,
          onEnviar: onEnviar,
          onImprimir: onImprimir,
        ),
        const SizedBox(height: 8),
      ],
    );
  }
}

class _Etiqueta extends StatelessWidget {
  const _Etiqueta(this.texto);
  final String texto;

  @override
  Widget build(BuildContext context) {
    return Text(texto.toUpperCase(),
        style: RegentaType.etiqueta.copyWith(color: RegentaColors.muted));
  }
}

class _ChipEstado extends StatelessWidget {
  const _ChipEstado({required this.estado});
  final EstadoFacturaVista estado;

  @override
  Widget build(BuildContext context) {
    final (texto, fondo, tinta) = switch (estado) {
      EstadoFacturaVista.aceptada =>
        ('Aceptada por la DIAN', RegentaColors.okSoft, RegentaColors.ok),
      EstadoFacturaVista.rechazada =>
        ('Rechazada por la DIAN', RegentaColors.critSoft, RegentaColors.crit),
      EstadoFacturaVista.contingencia =>
        ('En contingencia', RegentaColors.warnSoft, RegentaColors.warn),
      EstadoFacturaVista.enviada => ('Enviada', RegentaColors.sunken, RegentaColors.ink2),
      _ => ('Generada', RegentaColors.sunken, RegentaColors.muted),
    };
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 3),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(3)),
      child: Text(texto.toUpperCase(),
          style: RegentaType.etiqueta.copyWith(fontSize: 9.5, color: tinta)),
    );
  }
}

class _MotivoRechazo extends StatelessWidget {
  const _MotivoRechazo({this.codigo, this.mensaje});
  final String? codigo;
  final String? mensaje;

  @override
  Widget build(BuildContext context) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(11),
      decoration: BoxDecoration(
        color: RegentaColors.critSoft,
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text('Rechazo ${codigo ?? ''}'.trim(),
              style: RegentaType.codigo.copyWith(
                  fontSize: 11, fontWeight: FontWeight.w600, color: RegentaColors.crit)),
          if (mensaje != null && mensaje!.isNotEmpty) ...[
            const SizedBox(height: 3),
            Text(mensaje!,
                style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.crit)),
          ],
        ],
      ),
    );
  }
}

class _LineaFactura extends StatelessWidget {
  const _LineaFactura({required this.linea});
  final LineaVista linea;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(vertical: 11),
      decoration:
          const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(linea.descripcion,
                    style: RegentaType.cuerpo.copyWith(
                        fontSize: 13.5, fontWeight: FontWeight.w500, color: RegentaColors.ink)),
              ),
              const SizedBox(width: 10),
              Text(formatearPesos(linea.total),
                  style: RegentaType.codigo.copyWith(
                      fontSize: 13, fontWeight: FontWeight.w600, color: RegentaColors.ink)),
            ],
          ),
          const SizedBox(height: 4),
          Row(
            children: [
              if (linea.codigo != null)
                Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: Text(linea.codigo!,
                      style: RegentaType.codigo
                          .copyWith(fontSize: 10.5, color: RegentaColors.muted)),
                ),
              Text('${linea.cantidad} × ${formatearPesos(linea.precioUnitario)}',
                  style:
                      RegentaType.codigo.copyWith(fontSize: 10.5, color: RegentaColors.faint)),
            ],
          ),
        ],
      ),
    );
  }
}

class _Totales extends StatelessWidget {
  const _Totales({required this.factura});
  final FacturaVista factura;

  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        _Fila('Subtotal', formatearPesos(factura.subtotal)),
        if (factura.descuentoTotal > 0)
          _Fila('Descuento', '-${formatearPesos(factura.descuentoTotal)}'),
        _Fila('Base gravable', formatearPesos(factura.baseGravable)),
        for (final i in factura.impuestos)
          _Fila('${i.nombre} ${i.porcentaje}%'.trim(), formatearPesos(i.valor)),
        Padding(
          padding: const EdgeInsets.only(top: 8),
          child: Container(
            decoration: const BoxDecoration(
                border: Border(top: BorderSide(color: RegentaColors.line2))),
            padding: const EdgeInsets.only(top: 10),
            child: Row(
              children: [
                Expanded(
                  child: Text('Total',
                      style: RegentaType.seccion
                          .copyWith(fontSize: 15, color: RegentaColors.ink)),
                ),
                Text(formatearPesos(factura.total),
                    style: RegentaType.codigo.copyWith(
                        fontSize: 17, fontWeight: FontWeight.w700, color: RegentaColors.ink)),
              ],
            ),
          ),
        ),
      ],
    );
  }
}

class _Fila extends StatelessWidget {
  const _Fila(this.etiqueta, this.valor);
  final String etiqueta;
  final String valor;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        children: [
          Expanded(
            child: Text(etiqueta,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2)),
          ),
          const SizedBox(width: 8),
          Text(valor,
              style: RegentaType.codigo.copyWith(fontSize: 12.5, color: RegentaColors.ink2)),
        ],
      ),
    );
  }
}

class _Cufe extends StatelessWidget {
  const _Cufe({required this.cufe});
  final String cufe;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: RegentaColors.sunken,
        borderRadius: BorderRadius.circular(RegentaSpacing.radiusCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(child: _Etiqueta('CUFE')),
              Semantics(
                button: true,
                label: 'Copiar CUFE',
                child: InkWell(
                  onTap: () async {
                    await Clipboard.setData(ClipboardData(text: cufe));
                    if (context.mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('CUFE copiado')));
                    }
                  },
                  child: const SizedBox(
                    width: RegentaSpacing.hitTarget,
                    height: RegentaSpacing.hitTarget,
                    child: Icon(Icons.copy, size: 17, color: RegentaColors.ink2),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          SelectableText(cufe,
              style: RegentaType.codigo.copyWith(
                  fontSize: 10.5, height: 1.55, color: RegentaColors.ink)),
        ],
      ),
    );
  }
}

class _Qr extends StatelessWidget {
  const _Qr({required this.url});
  final String url;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(13),
      decoration: BoxDecoration(
        color: RegentaColors.surface,
        border: Border.all(color: RegentaColors.line),
        borderRadius: BorderRadius.circular(RegentaSpacing.radiusCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _Etiqueta('QR'),
          const SizedBox(height: 4),
          SelectableText(url,
              style: RegentaType.codigo
                  .copyWith(fontSize: 10.5, height: 1.5, color: RegentaColors.ink2)),
        ],
      ),
    );
  }
}

class _Trazabilidad extends StatelessWidget {
  const _Trazabilidad({required this.pasos});
  final List<PasoDeTrazabilidad> pasos;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text('Trazabilidad', style: RegentaType.item.copyWith(color: RegentaColors.ink)),
        const SizedBox(height: 6),
        for (final p in pasos)
          Padding(
            padding: const EdgeInsets.symmetric(vertical: 5),
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Padding(
                  padding: EdgeInsets.only(top: 4, right: 8),
                  child: Icon(Icons.circle, size: 7, color: RegentaColors.faint),
                ),
                Expanded(
                  child: Text(
                    [
                      p.evento,
                      if (p.codigoError != null) p.codigoError!,
                      if (p.mensaje != null) p.mensaje!,
                      if (p.ocurridoEn != null) p.ocurridoEn!,
                    ].join(' · '),
                    style: RegentaType.codigo
                        .copyWith(fontSize: 10.5, color: RegentaColors.muted),
                  ),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _Acciones extends StatelessWidget {
  const _Acciones({
    required this.enviando,
    required this.puedeEnviar,
    required this.onEnviar,
    this.onImprimir,
  });

  final bool enviando;
  final bool puedeEnviar;
  final VoidCallback onEnviar;
  final VoidCallback? onImprimir;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Expanded(
          child: SizedBox(
            height: RegentaSpacing.hitTarget,
            child: FilledButton(
              onPressed: puedeEnviar ? onEnviar : null,
              style: FilledButton.styleFrom(
                backgroundColor: RegentaColors.accent,
                padding: const EdgeInsets.symmetric(horizontal: 10),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.send_outlined, size: 17),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(enviando ? 'Enviando…' : 'Enviar',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: RegentaType.item
                            .copyWith(fontSize: 14, color: RegentaColors.surface)),
                  ),
                ],
              ),
            ),
          ),
        ),
        const SizedBox(width: 9),
        Expanded(
          child: SizedBox(
            height: RegentaSpacing.hitTarget,
            child: OutlinedButton(
              onPressed: onImprimir,
              style: OutlinedButton.styleFrom(
                foregroundColor: RegentaColors.ink,
                padding: const EdgeInsets.symmetric(horizontal: 10),
                side: const BorderSide(color: RegentaColors.line2),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(4)),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(Icons.print_outlined, size: 17),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text('Imprimir',
                        maxLines: 1,
                        overflow: TextOverflow.ellipsis,
                        style: RegentaType.item
                            .copyWith(fontSize: 14, color: RegentaColors.ink)),
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }
}
