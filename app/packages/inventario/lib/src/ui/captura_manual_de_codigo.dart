import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

/// El ingreso manual del código. En la web es el camino principal (no una
/// opción escondida) y en Android es la alternativa cuando la cámara falla
/// (HU-035, criterio 3). Devuelve el código escrito, o `null` si se cancela.
Future<String?> pedirCodigoAMano(BuildContext context) {
  final controlador = TextEditingController();
  return showDialog<String>(
    context: context,
    builder: (context) => AlertDialog(
      backgroundColor: RegentaColors.surface,
      title: Text('Ingresar código', style: RegentaType.seccion),
      content: TextField(
        controller: controlador,
        autofocus: true,
        style: RegentaType.codigo.copyWith(fontSize: 14),
        decoration: const InputDecoration(
          hintText: 'Código de barras',
          border: OutlineInputBorder(),
        ),
        onSubmitted: (valor) => Navigator.of(context).pop(valor.trim()),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          onPressed: () => Navigator.of(context).pop(controlador.text.trim()),
          child: const Text('Buscar'),
        ),
      ],
    ),
  );
}
