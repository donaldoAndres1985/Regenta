import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../busqueda/proveedores.dart';
import '../datos/importacion.dart';

/// HU-164 · importar productos desde Excel: descargar la plantilla, subir el archivo, ver la
/// vista previa con los errores de cada fila, importar las válidas (las que tienen error se
/// omiten) y descargar las omitidas para corregirlas.
class PantallaImportacion extends ConsumerStatefulWidget {
  const PantallaImportacion({super.key});

  @override
  ConsumerState<PantallaImportacion> createState() => _PantallaImportacionState();
}

class _PantallaImportacionState extends ConsumerState<PantallaImportacion> {
  ArchivoElegido? _archivo;
  VistaPreviaDeImportacion? _vista;
  ResultadoDeImportacion? _resultado;
  String? _error;
  bool _trabajando = false;

  Future<void> _hacer(Future<void> Function() accion) async {
    setState(() {
      _trabajando = true;
      _error = null;
    });
    try {
      await accion();
    } on ErrorDeApi catch (e) {
      if (mounted) setState(() => _error = e.mensaje);
    } finally {
      if (mounted) setState(() => _trabajando = false);
    }
  }

  Future<void> _plantilla() => _hacer(() async {
        final bytes = await ref.read(repositorioDeInventarioProvider).plantillaDeImportacion();
        await ref.read(archivosDeImportacionProvider).guardar('plantilla-productos.xlsx', bytes);
      });

  Future<void> _elegir() => _hacer(() async {
        final archivo = await ref.read(archivosDeImportacionProvider).elegirExcel();
        if (archivo == null) return;
        final vista = await ref.read(repositorioDeInventarioProvider).vistaPreviaDeImportacion(archivo);
        if (mounted) {
          setState(() {
            _archivo = archivo;
            _vista = vista;
            _resultado = null;
          });
        }
      });

  Future<void> _importar() => _hacer(() async {
        final r = await ref.read(repositorioDeInventarioProvider).importarProductos(_archivo!);
        if (mounted) setState(() => _resultado = r);
      });

  Future<void> _omitidas() => _hacer(() async {
        final bytes = await ref.read(repositorioDeInventarioProvider).omitidasComoExcel(_resultado!.omitidas);
        await ref.read(archivosDeImportacionProvider).guardar('filas-con-error.xlsx', bytes);
      });

  @override
  Widget build(BuildContext context) {
    final vista = _vista;
    final resultado = _resultado;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(title: const Text('Importar productos')),
      body: ListView(padding: const EdgeInsets.all(16), children: [
        Wrap(spacing: 8, runSpacing: 8, children: [
          OutlinedButton.icon(
            key: const Key('importacion-plantilla'),
            icon: const Icon(Icons.download),
            label: const Text('Descargar plantilla'),
            onPressed: _trabajando ? null : _plantilla,
          ),
          FilledButton.icon(
            key: const Key('importacion-elegir'),
            icon: const Icon(Icons.upload_file),
            label: const Text('Elegir archivo .xlsx'),
            onPressed: _trabajando ? null : _elegir,
          ),
        ]),
        if (_error != null)
          Padding(
            padding: const EdgeInsets.only(top: 12),
            child: Text(_error!, style: const TextStyle(color: RegentaColors.crit)),
          ),
        if (resultado != null) ...[
          const SizedBox(height: 16),
          Text(
            'Se importaron ${resultado.importados} productos.'
            '${resultado.omitidas.isEmpty ? '' : ' Se omitieron ${resultado.omitidas.length} filas con error.'}',
            style: RegentaType.seccion.copyWith(color: RegentaColors.inkFuerte),
          ),
          if (resultado.omitidas.isNotEmpty)
            Align(
              alignment: Alignment.centerLeft,
              child: TextButton.icon(
                key: const Key('importacion-omitidas'),
                icon: const Icon(Icons.download),
                label: const Text('Descargar las filas con error'),
                onPressed: _trabajando ? null : _omitidas,
              ),
            ),
        ] else if (vista != null) ...[
          const SizedBox(height: 16),
          Text('${_archivo?.nombre}: ${vista.validas} filas listas, ${vista.conError} con error',
              style: RegentaType.seccion.copyWith(color: RegentaColors.inkFuerte)),
          const SizedBox(height: 8),
          FilledButton(
            key: const Key('importacion-confirmar'),
            onPressed: _trabajando || vista.validas == 0 ? null : _importar,
            child: Text('Importar ${vista.validas} productos (se omiten ${vista.conError} con error)'),
          ),
          const SizedBox(height: 12),
          for (final f in vista.filas)
            ListTile(
              dense: true,
              leading: Icon(f.valida ? Icons.check_circle_outline : Icons.error_outline,
                  color: f.valida ? RegentaColors.ok : RegentaColors.crit),
              title: Text(f.sku, style: const TextStyle(color: RegentaColors.inkFuerte)),
              subtitle: Text(
                f.valida ? 'Fila ${f.numero} · ${f.nombre}' : 'Fila ${f.numero} · ${f.errores.join('; ')}',
                style: TextStyle(color: f.valida ? RegentaColors.ink2 : RegentaColors.crit),
              ),
            ),
        ],
      ]),
    );
  }
}
