/// Nucleo compartido de Regenta. Cada modulo depende de este paquete y de
/// ningun otro modulo.
library;

export 'src/local/base_local.dart';
export 'src/local/politica_de_purga.dart';

/// Version del contrato del nucleo. Sube cuando cambia algo que los modulos
/// consumen.
const String versionDelNucleo = '0.1.0';
