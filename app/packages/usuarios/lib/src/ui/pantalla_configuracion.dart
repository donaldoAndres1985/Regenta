import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

class PantallaConfiguracion extends ConsumerStatefulWidget {
  const PantallaConfiguracion({super.key});

  @override
  ConsumerState<PantallaConfiguracion> createState() => _PantallaConfiguracionState();
}

class _PantallaConfiguracionState extends ConsumerState<PantallaConfiguracion> {
  bool _cargando = true;
  String? _error;
  Map<String, dynamic>? _config;

  final _razonSocialCtrl = TextEditingController();
  final _nitCtrl = TextEditingController();
  final _telefonoCtrl = TextEditingController();
  final _direccionCtrl = TextEditingController();
  final _correoCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    _cargarConfig();
  }

  @override
  void dispose() {
    _razonSocialCtrl.dispose();
    _nitCtrl.dispose();
    _telefonoCtrl.dispose();
    _direccionCtrl.dispose();
    _correoCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarConfig() async {
    setState(() {
      _cargando = true;
      _error = null;
    });

    try {
      final http = ref.read(clienteHttpProvider);
      final res = await http.get<Map<String, dynamic>>('/api/usuarios/configuracion');
      _config = res;
      _razonSocialCtrl.text = (res['razonSocial'] ?? '') as String;
      _nitCtrl.text = (res['nit'] ?? '') as String;
      _telefonoCtrl.text = (res['telefono'] ?? '') as String;
      _direccionCtrl.text = (res['direccion'] ?? '') as String;
      _correoCtrl.text = (res['correo'] ?? '') as String;
      setState(() => _cargando = false);
    } catch (e) {
      setState(() {
        _cargando = false;
        _error = 'No se pudo cargar la configuración: $e';
      });
    }
  }

  Future<void> _guardarConfig() async {
    setState(() => _cargando = true);
    try {
      final http = ref.read(clienteHttpProvider);
      await http.put<Map<String, dynamic>>(
        '/api/usuarios/configuracion',
        datos: {
          'razonSocial': _razonSocialCtrl.text.trim(),
          'nit': _nitCtrl.text.trim(),
          'telefono': _telefonoCtrl.text.trim(),
          'direccion': _direccionCtrl.text.trim(),
          'correo': _correoCtrl.text.trim(),
        },
      );
      if (mounted) {
        setState(() => _cargando = false);
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Configuración actualizada correctamente'),
            backgroundColor: RegentaColors.ok,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _cargando = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error al guardar: $e'),
            backgroundColor: RegentaColors.crit,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      appBar: AppBar(
        backgroundColor: RegentaColors.surface,
        surfaceTintColor: RegentaColors.surface,
        elevation: 0,
        shape: const Border(bottom: BorderSide(color: RegentaColors.line)),
        title: Row(
          children: [
            const Icon(Icons.settings_outlined, color: RegentaColors.ink, size: 22),
            const SizedBox(width: 8),
            Text(
              'Configuración del Negocio',
              style: RegentaType.seccion.copyWith(
                fontSize: 18,
                color: RegentaColors.ink,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            tooltip: 'Refrescar',
            icon: const Icon(Icons.refresh, color: RegentaColors.ink),
            onPressed: _cargarConfig,
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: _cargando
          ? const Center(child: CircularProgressIndicator())
          : _error != null
              ? Center(
                  child: Padding(
                    padding: const EdgeInsets.all(24),
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Icon(Icons.error_outline, size: 48, color: RegentaColors.crit),
                        const SizedBox(height: 16),
                        Text(_error!, style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2)),
                        const SizedBox(height: 16),
                        ElevatedButton(onPressed: _cargarConfig, child: const Text('Reintentar')),
                      ],
                    ),
                  ),
                )
              : SingleChildScrollView(
                  padding: const EdgeInsets.all(24),
                  child: Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 680),
                      child: Card(
                        elevation: 0,
                        color: RegentaColors.surface,
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(12),
                          side: const BorderSide(color: RegentaColors.line),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(24),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.stretch,
                            children: [
                              Text(
                                'Datos Fiscales y de Operación',
                                style: RegentaType.seccion.copyWith(fontSize: 16),
                              ),
                              const SizedBox(height: 8),
                              Text(
                                'Estos datos aparecerán en los encabezados de tus facturas y tirillas de venta.',
                                style: RegentaType.cuerpo.copyWith(color: RegentaColors.muted),
                              ),
                              const Divider(height: 32, color: RegentaColors.line),
                              TextField(
                                controller: _razonSocialCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'Razón Social / Nombre Comercial',
                                  border: OutlineInputBorder(),
                                ),
                              ),
                              const SizedBox(height: 16),
                              TextField(
                                controller: _nitCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'NIT / Identificación Fiscal',
                                  border: OutlineInputBorder(),
                                ),
                              ),
                              const SizedBox(height: 16),
                              TextField(
                                controller: _direccionCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'Dirección Comercial',
                                  border: OutlineInputBorder(),
                                ),
                              ),
                              const SizedBox(height: 16),
                              Row(
                                children: [
                                  Expanded(
                                    child: TextField(
                                      controller: _telefonoCtrl,
                                      decoration: const InputDecoration(
                                        labelText: 'Teléfono de Contacto',
                                        border: OutlineInputBorder(),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 16),
                                  Expanded(
                                    child: TextField(
                                      controller: _correoCtrl,
                                      decoration: const InputDecoration(
                                        labelText: 'Correo Electrónico',
                                        border: OutlineInputBorder(),
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 32),
                              FilledButton.icon(
                                style: FilledButton.styleFrom(
                                  backgroundColor: RegentaColors.accent,
                                  padding: const EdgeInsets.symmetric(vertical: 14),
                                ),
                                icon: const Icon(Icons.save),
                                label: const Text('Guardar Configuración'),
                                onPressed: _guardarConfig,
                              ),
                            ],
                          ),
                        ),
                      ),
                    ),
                  ),
                ),
    );
  }
}
