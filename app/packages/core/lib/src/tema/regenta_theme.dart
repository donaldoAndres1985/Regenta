import 'package:flutter/material.dart';

import 'pastel_theme.dart';
import 'patron_operativo.dart';

/// El tema Material de Regenta: Pastel Profesional.
///
/// El primario es el mismo en los tres patrones; el secundario es el color del
/// patrón.
ThemeData regentaTheme(PatronOperativo patron) => PastelTheme.light(patron: patron);
