import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../proveedores/ui/piezas.dart';
import '../ui/formato.dart';

/// HU-173 · Cómo compra el negocio: si sus órdenes necesitan aprobación (y desde qué monto) y cuánto
/// acepta recibir de más sobre lo pedido.
class ConfiguracionDeCompras {
  const ConfiguracionDeCompras({
    this.exigeAprobacion = true,
    this.montoMinimoAprobacion,
    this.toleranciaRecepcionPct = 5,
  });

  final bool exigeAprobacion;
  final num? montoMinimoAprobacion;
  final num toleranciaRecepcionPct;

  factory ConfiguracionDeCompras.desdeJson(Map<String, dynamic> json) => ConfiguracionDeCompras(
        exigeAprobacion: (json['exigeAprobacion'] as bool?) ?? true,
        montoMinimoAprobacion: json['montoMinimoAprobacion'] as num?,
        toleranciaRecepcionPct: (json['toleranciaRecepcionPct'] as num?) ?? 5,
      );

  Map<String, dynamic> aJson() => {
        'exigeAprobacion': exigeAprobacion,
        'montoMinimoAprobacion': exigeAprobacion ? montoMinimoAprobacion : null,
        'toleranciaRecepcionPct': toleranciaRecepcionPct,
      };
}

class RepositorioDeConfiguracionDeCompras {
  RepositorioDeConfiguracionDeCompras(this._http);

  static const _ruta = '/api/compras/configuracion';

  final ClienteHttp _http;

  Future<ConfiguracionDeCompras> ver() async =>
      ConfiguracionDeCompras.desdeJson(await _http.get<Map<String, dynamic>>(_ruta));

  Future<ConfiguracionDeCompras> guardar(ConfiguracionDeCompras c) async {
    final dynamic res = await _http.put<dynamic>(_ruta, datos: c.aJson());
    return ConfiguracionDeCompras.desdeJson((res as Map).cast<String, dynamic>());
  }
}

final repositorioDeConfiguracionDeComprasProvider = Provider<RepositorioDeConfiguracionDeCompras>((ref) {
  throw UnimplementedError(
    'Sobrescribe repositorioDeConfiguracionDeComprasProvider al montar el ProviderScope.',
  );
});

/// La pantalla de configuración de compras (HU-173).
class PantallaConfiguracionDeCompras extends ConsumerStatefulWidget {
  const PantallaConfiguracionDeCompras({super.key});

  @override
  ConsumerState<PantallaConfiguracionDeCompras> createState() => _PantallaConfiguracionDeComprasState();
}

class _PantallaConfiguracionDeComprasState extends ConsumerState<PantallaConfiguracionDeCompras> {
  final TextEditingController _minimo = TextEditingController();
  final TextEditingController _tolerancia = TextEditingController();
  bool _exige = true;
  bool _cargando = true;
  bool _guardando = false;
  String? _error;

  RepositorioDeConfiguracionDeCompras get _repo => ref.read(repositorioDeConfiguracionDeComprasProvider);

  @override
  void initState() {
    super.initState();
    unawaited(_cargar());
  }

  @override
  void dispose() {
    _minimo.dispose();
    _tolerancia.dispose();
    super.dispose();
  }

  static String _numero(num n) => n == n.roundToDouble() ? '${n.round()}' : '$n';

  void _mostrar(ConfiguracionDeCompras c) {
    _exige = c.exigeAprobacion;
    _minimo.text = c.montoMinimoAprobacion == null ? '' : _numero(c.montoMinimoAprobacion!);
    _tolerancia.text = _numero(c.toleranciaRecepcionPct);
  }

  Future<void> _cargar() async {
    try {
      final c = await _repo.ver();
      if (!mounted) return;
      setState(() {
        _mostrar(c);
        _cargando = false;
      });
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _mostrar(const ConfiguracionDeCompras());
        _cargando = false;
        _error = fallo is ErrorDeRed ? 'No hay conexión: se muestran los valores por omisión.' : fallo.mensaje;
      });
    }
  }

  Future<void> _guardar() async {
    final minimoTexto = _minimo.text.replaceAll(RegExp(r'[^0-9.]'), '');
    final minimo = minimoTexto.isEmpty ? null : num.tryParse(minimoTexto);
    final tolerancia = num.tryParse(_tolerancia.text.replaceAll(',', '.').trim());
    if (tolerancia == null || tolerancia < 0 || tolerancia > 100) {
      setState(() => _error = 'La tolerancia va de 0 a 100 %.');
      return;
    }
    if (_exige && minimo != null && minimo <= 0) {
      setState(() => _error = 'El monto mínimo debe ser mayor que cero, o dejarse vacío.');
      return;
    }
    setState(() {
      _guardando = true;
      _error = null;
    });
    try {
      final guardada = await _repo.guardar(ConfiguracionDeCompras(
        exigeAprobacion: _exige,
        montoMinimoAprobacion: minimo,
        toleranciaRecepcionPct: tolerancia,
      ));
      if (!mounted) return;
      setState(() {
        _mostrar(guardada);
        _guardando = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Configuración de compras guardada.')));
    } on ErrorDeApi catch (fallo) {
      if (!mounted) return;
      setState(() {
        _guardando = false;
        _error = switch (fallo) {
          SinPermiso() => 'No tienes permiso para cambiar la configuración de compras.',
          ErrorDeRed() => 'No hay conexión: no se guardó. Intenta de nuevo.',
          _ => fallo.mensaje,
        };
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(builder: (context, afuera) {
      final escritorio = afuera.maxWidth >= kBreakpointEscritorio;
      final minimo = num.tryParse(_minimo.text.replaceAll(RegExp(r'[^0-9.]'), ''));
      final resumen = !_exige
          ? 'Toda orden confirmada queda aprobada sola.'
          : (minimo == null || minimo <= 0)
              ? 'Toda orden necesita que alguien con permiso la apruebe.'
              : 'Las órdenes desde ${formatearPesos(minimo)} necesitan aprobación; las menores se aprueban solas.';
      return Scaffold(
        backgroundColor: RegentaColors.paper,
        appBar: escritorio
            ? const EncabezadoDeSeccion(
                subtitulo: 'Aprobación de órdenes y tolerancia al recibir',
                titulo: 'Configuración de compras',
              )
            : AppBar(
                backgroundColor: RegentaColors.surface,
                surfaceTintColor: RegentaColors.surface,
                elevation: 0,
                shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
                title: Text('Configuración de compras',
                    style: RegentaType.seccion.copyWith(fontSize: 16, color: RegentaColors.ink)),
              ),
        body: _cargando
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
                padding: const EdgeInsets.all(14),
                child: Center(
                  child: ConstrainedBox(
                    constraints: const BoxConstraints(maxWidth: 640),
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: RegentaColors.surface,
                        border: Border.all(color: RegentaColors.line),
                        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                      ),
                      child: Material(
                        type: MaterialType.transparency,
                        child: Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
                          SwitchListTile(
                            key: const Key('config-exige-aprobacion'),
                            contentPadding: EdgeInsets.zero,
                            value: _exige,
                            onChanged: (v) => setState(() => _exige = v),
                            title: const Text('Las órdenes de compra necesitan aprobación'),
                            subtitle: Text(resumen,
                                key: const Key('config-resumen'),
                                style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.ink2)),
                          ),
                          const SizedBox(height: 8),
                          CampoConEtiqueta(
                            etiqueta: 'Solo desde este monto',
                            ayuda: 'Vacío: todas las órdenes necesitan aprobación',
                            hijo: TextField(
                              key: const Key('config-monto-minimo'),
                              controller: _minimo,
                              enabled: _exige,
                              keyboardType: TextInputType.number,
                              onChanged: (_) => setState(() {}),
                              style: estiloDeCampo,
                              decoration: decoracionDeCampo(hint: 'Sin monto mínimo'),
                            ),
                          ),
                          CampoConEtiqueta(
                            etiqueta: 'Tolerancia al recibir (%)',
                            ayuda: 'Cuánto de más se acepta sobre lo pedido. 0 = nada de más',
                            hijo: TextField(
                              key: const Key('config-tolerancia'),
                              controller: _tolerancia,
                              keyboardType: const TextInputType.numberWithOptions(decimal: true),
                              style: estiloDeCampo,
                              decoration: decoracionDeCampo(hint: '5'),
                            ),
                          ),
                          if (_error != null) AvisoDeError(_error!, key: const Key('config-error')),
                          const SizedBox(height: 4),
                          SizedBox(
                            height: RegentaSpacing.hitTarget,
                            child: FilledButton(
                              key: const Key('config-guardar'),
                              onPressed: _guardando ? null : () => unawaited(_guardar()),
                              child: Text(_guardando ? 'Guardando…' : 'Guardar'),
                            ),
                          ),
                        ]),
                      ),
                    ),
                  ),
                ),
              ),
      );
    });
  }
}
