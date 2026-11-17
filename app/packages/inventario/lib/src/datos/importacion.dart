import 'dart:typed_data';

import 'package:file_picker/file_picker.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

/// HU-164 · una fila del Excel tal como se leyó, con sus errores (vacío = se importa).
class FilaDeImportacion {
  const FilaDeImportacion({required this.numero, required this.datos, required this.errores});

  final int numero;

  /// Lo que vino en la fila, con las claves del backend (sku, nombre, categoria…).
  final Map<String, dynamic> datos;
  final List<String> errores;

  bool get valida => errores.isEmpty;
  String get sku => (datos['sku'] ?? '') as String;
  String get nombre => (datos['nombre'] ?? '') as String;

  factory FilaDeImportacion.desdeJson(Map<String, dynamic> j) => FilaDeImportacion(
        numero: ((j['numero'] ?? 0) as num).toInt(),
        datos: j,
        errores: [for (final e in (j['errores'] ?? const []) as List) e as String],
      );
}

/// HU-164 C2 · la vista previa: cada fila y cuántas son válidas.
class VistaPreviaDeImportacion {
  const VistaPreviaDeImportacion({required this.filas, required this.validas, required this.conError});

  final List<FilaDeImportacion> filas;
  final int validas;
  final int conError;

  factory VistaPreviaDeImportacion.desdeJson(Map<String, dynamic> j) => VistaPreviaDeImportacion(
        filas: [
          for (final f in ((j['filas'] ?? const []) as List).cast<Map<String, dynamic>>())
            FilaDeImportacion.desdeJson(f),
        ],
        validas: ((j['validas'] ?? 0) as num).toInt(),
        conError: ((j['conError'] ?? 0) as num).toInt(),
      );
}

/// HU-164 C3/C5 · cuántos se importaron y las filas omitidas.
class ResultadoDeImportacion {
  const ResultadoDeImportacion({required this.importados, required this.omitidas});

  final int importados;
  final List<FilaDeImportacion> omitidas;

  factory ResultadoDeImportacion.desdeJson(Map<String, dynamic> j) => ResultadoDeImportacion(
        importados: ((j['importados'] ?? 0) as num).toInt(),
        omitidas: [
          for (final f in ((j['omitidas'] ?? const []) as List).cast<Map<String, dynamic>>())
            FilaDeImportacion.desdeJson(f),
        ],
      );
}

/// Un archivo elegido por quien usa la app.
class ArchivoElegido {
  const ArchivoElegido({required this.nombre, required this.bytes});

  final String nombre;
  final Uint8List bytes;
}

/// HU-164 · elegir el .xlsx que se sube y guardar los .xlsx que se descargan.
abstract class ArchivosDeImportacion {
  Future<ArchivoElegido?> elegirExcel();

  Future<void> guardar(String nombre, List<int> bytes);
}

/// Con `file_picker`: en web descarga el archivo; en Android abre el diálogo de guardar.
class ArchivosConFilePicker implements ArchivosDeImportacion {
  const ArchivosConFilePicker();

  @override
  Future<ArchivoElegido?> elegirExcel() async {
    final r = await FilePicker.platform.pickFiles(
      type: FileType.custom,
      allowedExtensions: const ['xlsx'],
      withData: true,
    );
    final f = r?.files.singleOrNull;
    if (f == null || f.bytes == null) return null;
    return ArchivoElegido(nombre: f.name, bytes: f.bytes!);
  }

  @override
  Future<void> guardar(String nombre, List<int> bytes) async {
    await FilePicker.platform.saveFile(
      fileName: nombre,
      bytes: Uint8List.fromList(bytes),
      type: FileType.custom,
      allowedExtensions: const ['xlsx'],
    );
  }
}

final archivosDeImportacionProvider =
    Provider<ArchivosDeImportacion>((ref) => const ArchivosConFilePicker());

/// HU-164 · si quien usa la app importa productos (`INVENTARIO_IMPORTAR`).
final puedeImportarProvider = Provider<bool>((ref) => false);
