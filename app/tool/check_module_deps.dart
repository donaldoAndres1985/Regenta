// Criterio 3 de HU-002, como comprobacion ejecutable: un paquete de modulo
// depende de `regenta_core` y de ningun otro paquete de modulo.
//
// Sin dependencias externas a proposito: corre en el CI antes de `pub get`.
//
// Uso: dart run tool/check_module_deps.dart   (desde app/)

import 'dart:io';

/// El unico paquete del que un modulo puede depender.
const String nucleo = 'regenta_core';

void main() {
  final raizPaquetes = Directory('packages');
  if (!raizPaquetes.existsSync()) {
    stderr.writeln('No encuentro packages/. Corre esto desde app/.');
    exit(2);
  }

  final problemas = <String>[];

  for (final entrada in raizPaquetes.listSync().whereType<Directory>()) {
    final pubspec = File('${entrada.path}/pubspec.yaml');
    if (!pubspec.existsSync()) continue;

    final nombre = _valorDe('name', pubspec.readAsLinesSync());
    if (nombre == null || nombre == nucleo) continue;

    final dependenciasDeModulo = _dependenciasRegenta(pubspec.readAsLinesSync())
        .where((d) => d != nucleo)
        .toList();

    if (dependenciasDeModulo.isNotEmpty) {
      problemas.add('$nombre depende de ${dependenciasDeModulo.join(', ')} '
          '(solo puede depender de $nucleo)');
    }
  }

  if (problemas.isNotEmpty) {
    stderr.writeln('Dependencias entre modulos prohibidas:');
    for (final p in problemas) {
      stderr.writeln('  - $p');
    }
    exit(1);
  }

  stdout.writeln('OK: ningun modulo depende de otro modulo.');
}

/// Nombres `regenta_*` bajo el bloque `dependencies:` (no `dev_dependencies`).
List<String> _dependenciasRegenta(List<String> lineas) {
  final encontrados = <String>[];
  var enDependencies = false;

  for (final linea in lineas) {
    final sinComentario = linea.split('#').first;
    if (sinComentario.trimRight().isEmpty) continue;

    final sinSangria = sinComentario.trimLeft();
    final esClaveRaiz = sinComentario.length == sinSangria.length;

    if (esClaveRaiz) {
      enDependencies = sinSangria.startsWith('dependencies:');
      continue;
    }
    if (!enDependencies) continue;

    final match = RegExp(r'^([A-Za-z0-9_]+):').firstMatch(sinSangria);
    final clave = match?.group(1);
    if (clave != null && clave.startsWith('regenta_')) {
      encontrados.add(clave);
    }
  }
  return encontrados;
}

String? _valorDe(String clave, List<String> lineas) {
  for (final linea in lineas) {
    final m = RegExp('^$clave:\\s*(.+)\\s*\$').firstMatch(linea.split('#').first.trim());
    if (m != null) return m.group(1)!.trim();
  }
  return null;
}
