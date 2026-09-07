import 'package:flutter/material.dart';
import 'package:regenta_core/regenta_core.dart';

void main() {
  runApp(const RegentaApp());
}

/// Carcasa minima de la app. El tema, la navegacion y la sesion llegan en las
/// historias siguientes de E16; aqui solo se comprueba que un unico codigo
/// arranca en Android y en Web colgando del nucleo.
class RegentaApp extends StatelessWidget {
  const RegentaApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Regenta',
      debugShowCheckedModeBanner: false,
      home: Scaffold(
        appBar: AppBar(title: const Text('Regenta')),
        body: Center(
          child: Text('Nucleo $versionDelNucleo'),
        ),
      ),
    );
  }
}
