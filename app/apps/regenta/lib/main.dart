import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

void main() {
  runApp(const RegentaApp());
}

/// Carcasa minima de la app. La navegacion y la sesion llegan en HU-107 y
/// HU-109; el patron operativo, que decide el color secundario, saldra del JWT.
/// Aqui se fija uno por defecto para comprobar que el tema y las fuentes del
/// nucleo cargan en Android y en Web.
class RegentaApp extends StatelessWidget {
  const RegentaApp({super.key, this.patron = PatronOperativo.ventaDirecta});

  final PatronOperativo patron;

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Regenta',
      debugShowCheckedModeBanner: false,
      theme: regentaTheme(patron),
      home: Scaffold(
        appBar: AppBar(title: Text('Regenta', style: RegentaType.seccion)),
        body: Center(
          child: Padding(
            padding: const EdgeInsets.all(RegentaSpacing.xl),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('Nucleo $versionDelNucleo', style: RegentaType.cuerpo),
                const SizedBox(height: RegentaSpacing.sm),
                Text(r'$ 0', style: RegentaType.dinero),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
