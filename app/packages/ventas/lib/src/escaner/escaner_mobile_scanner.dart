import 'package:flutter/foundation.dart'
    show defaultTargetPlatform, kIsWeb, TargetPlatform;
import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import 'package:regenta_core/regenta_core.dart';

import 'escaner_de_codigos.dart';

/// El escáner real, sobre `mobile_scanner`. Solo tiene sentido en Android/iOS:
/// en la web `disponible` es `false` y el ingreso manual es el único camino
/// (HU-035, criterio 3).
class EscanerMobileScanner implements EscanerDeCodigos {
  const EscanerMobileScanner();

  @override
  Future<bool> get disponible async {
    if (kIsWeb) return false;
    return defaultTargetPlatform == TargetPlatform.android ||
        defaultTargetPlatform == TargetPlatform.iOS;
  }

  @override
  Future<String?> escanearUnCodigo(BuildContext context) async {
    if (!await disponible) throw const EscaneoNoDisponible();
    if (!context.mounted) return null;
    return Navigator.of(context).push<String>(
      MaterialPageRoute(builder: (_) => const _VistaDeEscaner(), fullscreenDialog: true),
    );
  }
}

class _VistaDeEscaner extends StatefulWidget {
  const _VistaDeEscaner();

  @override
  State<_VistaDeEscaner> createState() => _VistaDeEscanerState();
}

class _VistaDeEscanerState extends State<_VistaDeEscaner> {
  final MobileScannerController _controlador = MobileScannerController();
  bool _yaLeido = false;

  @override
  void dispose() {
    _controlador.dispose();
    super.dispose();
  }

  void _alDetectar(BarcodeCapture captura) {
    if (_yaLeido) return;
    final codigo = captura.barcodes
        .map((b) => b.rawValue)
        .firstWhere((v) => v != null && v.isNotEmpty, orElse: () => null);
    if (codigo == null) return;
    _yaLeido = true;
    Navigator.of(context).pop(codigo);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: RegentaColors.ink,
      appBar: AppBar(
        backgroundColor: RegentaColors.ink,
        foregroundColor: RegentaColors.surface,
        title: const Text('Escanear código'),
      ),
      body: MobileScanner(controller: _controlador, onDetect: _alDetectar),
    );
  }
}
