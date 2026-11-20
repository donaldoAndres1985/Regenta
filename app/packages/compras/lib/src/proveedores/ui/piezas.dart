import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

/// Piezas de formulario con el estilo de Regenta (mismo trazo que la ficha de clientes).

InputDecoration decoracionDeCampo({String? hint, Widget? prefijo}) => InputDecoration(
      isDense: true,
      filled: true,
      fillColor: RegentaColors.surface,
      hintText: hint,
      hintStyle: const TextStyle(fontSize: 14, color: RegentaColors.faint),
      prefixIcon: prefijo,
      contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        borderSide: const BorderSide(color: RegentaColors.line2),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        borderSide: const BorderSide(color: RegentaColors.line2),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        borderSide: const BorderSide(color: RegentaColors.accent, width: 1.5),
      ),
    );

const TextStyle estiloDeCampo = TextStyle(
  fontSize: 14,
  fontWeight: FontWeight.w600,
  color: RegentaColors.inkFuerte,
);

class CampoConEtiqueta extends StatelessWidget {
  const CampoConEtiqueta({super.key, required this.etiqueta, required this.hijo, this.ayuda});

  final String etiqueta;
  final Widget hijo;
  final String? ayuda;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        Padding(
          padding: const EdgeInsets.only(bottom: 4),
          child: Text(etiqueta.toUpperCase(), style: RegentaType.etiqueta.copyWith(fontSize: 9.5)),
        ),
        hijo,
        if (ayuda != null) ...[
          const SizedBox(height: 3),
          Text(ayuda!, style: RegentaType.cuerpo.copyWith(fontSize: 11, color: RegentaColors.muted)),
        ],
      ]),
    );
  }
}

/// Franja roja con el motivo de un rechazo.
class AvisoDeError extends StatelessWidget {
  const AvisoDeError(this.texto, {super.key});

  final String texto;

  @override
  Widget build(BuildContext context) {
    return Container(
      margin: const EdgeInsets.only(bottom: 8),
      padding: const EdgeInsets.all(10),
      decoration: BoxDecoration(
        color: RegentaColors.critSoft,
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
      ),
      child: Text(texto, style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.crit)),
    );
  }
}

/// Encabezado de un diálogo: título y botón de cerrar.
class CabeceraDeDialogo extends StatelessWidget {
  const CabeceraDeDialogo({super.key, required this.titulo, this.subtitulo, this.onCerrar});

  final String titulo;
  final String? subtitulo;
  final VoidCallback? onCerrar;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.fromLTRB(18, 14, 8, 14),
      decoration: const BoxDecoration(border: Border(bottom: BorderSide(color: RegentaColors.line))),
      child: Row(children: [
        Expanded(
          child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
            Text(titulo,
                maxLines: 1,
                overflow: TextOverflow.ellipsis,
                style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
            if (subtitulo != null)
              Text(subtitulo!,
                  maxLines: 1,
                  overflow: TextOverflow.ellipsis,
                  style: RegentaType.cuerpo.copyWith(fontSize: 12, color: RegentaColors.muted)),
          ]),
        ),
        IconButton(
          tooltip: 'Cerrar',
          onPressed: onCerrar,
          icon: const Icon(Icons.close, size: 20, color: RegentaColors.ink2),
        ),
      ]),
    );
  }
}

/// Etiqueta de estado (INACTIVO, SUPERA CUPO, PREFERIDO…).
class ChipDeEstado extends StatelessWidget {
  const ChipDeEstado({super.key, required this.texto, required this.fondo, required this.tinta});

  final String texto;
  final Color fondo;
  final Color tinta;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
      decoration: BoxDecoration(color: fondo, borderRadius: BorderRadius.circular(3)),
      child: Text(texto.toUpperCase(), style: RegentaType.etiqueta.copyWith(fontSize: 9.5, color: tinta)),
    );
  }
}
