import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/configuracion_negocio_vista.dart';
import '../usuarios/proveedores.dart';

class PantallaConfiguracion extends ConsumerStatefulWidget {
  const PantallaConfiguracion({super.key});

  @override
  ConsumerState<PantallaConfiguracion> createState() => _PantallaConfiguracionState();
}

class _PantallaConfiguracionState extends ConsumerState<PantallaConfiguracion> {
  final _formKey = GlobalKey<FormState>();

  late TextEditingController _razonSocialCtrl;
  late TextEditingController _nombreComercialCtrl;
  late TextEditingController _documentoCtrl;
  late TextEditingController _direccionCtrl;
  late TextEditingController _ciudadCtrl;
  late TextEditingController _telefonoCtrl;
  late TextEditingController _emailCtrl;

  bool _preciosConIva = true;
  bool _stockNegativo = false;
  bool _inicializado = false;
  bool _guardando = false;

  @override
  void initState() {
    super.initState();
    _razonSocialCtrl = TextEditingController();
    _nombreComercialCtrl = TextEditingController();
    _documentoCtrl = TextEditingController();
    _direccionCtrl = TextEditingController();
    _ciudadCtrl = TextEditingController();
    _telefonoCtrl = TextEditingController();
    _emailCtrl = TextEditingController();
  }

  @override
  void dispose() {
    _razonSocialCtrl.dispose();
    _nombreComercialCtrl.dispose();
    _documentoCtrl.dispose();
    _direccionCtrl.dispose();
    _ciudadCtrl.dispose();
    _telefonoCtrl.dispose();
    _emailCtrl.dispose();
    super.dispose();
  }

  void _poblar(ConfiguracionNegocioVista config) {
    if (_inicializado) return;
    _inicializado = true;
    _razonSocialCtrl.text = config.razonSocial;
    _nombreComercialCtrl.text = config.nombreComercial;
    _documentoCtrl.text = config.numeroDocumento;
    _direccionCtrl.text = config.direccion;
    _ciudadCtrl.text = config.ciudad;
    _telefonoCtrl.text = config.telefono;
    _emailCtrl.text = config.email;
    _preciosConIva = config.preciosIncluyenImpuesto;
    _stockNegativo = config.politicaStockNegativo;
  }

  Future<void> _guardar() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _guardando = true);

    try {
      final repo = ref.read(repositorioDeUsuariosProvider);
      final actualizado = ConfiguracionNegocioVista(
        razonSocial: _razonSocialCtrl.text.trim(),
        nombreComercial: _nombreComercialCtrl.text.trim(),
        tipoDocumento: 'NIT',
        numeroDocumento: _documentoCtrl.text.trim(),
        direccion: _direccionCtrl.text.trim(),
        ciudad: _ciudadCtrl.text.trim(),
        telefono: _telefonoCtrl.text.trim(),
        email: _emailCtrl.text.trim(),
        preciosIncluyenImpuesto: _preciosConIva,
        politicaStockNegativo: _stockNegativo,
      );

      await repo.actualizarConfiguracion(actualizado);
      ref.invalidate(configuracionNegocioProvider);

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Configuración guardada correctamente'),
            backgroundColor: RegentaColors.ok,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Error al guardar: $e'),
            backgroundColor: RegentaColors.crit,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _guardando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final configAsync = ref.watch(configuracionNegocioProvider);

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
            onPressed: () {
              _inicializado = false;
              ref.invalidate(configuracionNegocioProvider);
            },
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: configAsync.when(
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (err, stack) => Center(
          child: Padding(
            padding: const EdgeInsets.all(24),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                const Icon(Icons.error_outline, size: 48, color: RegentaColors.crit),
                const SizedBox(height: 16),
                Text(
                  'No se pudo cargar la configuración: $err',
                  textAlign: TextAlign.center,
                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                ),
                const SizedBox(height: 16),
                ElevatedButton(
                  onPressed: () {
                    _inicializado = false;
                    ref.invalidate(configuracionNegocioProvider);
                  },
                  child: const Text('Reintentar'),
                ),
              ],
            ),
          ),
        ),
        data: (config) {
          _poblar(config);

          return SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: Center(
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 800),
                child: Form(
                  key: _formKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      // Tarjeta Datos Fiscales
                      Card(
                        elevation: 0,
                        color: RegentaColors.surface,
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.all(Radius.circular(10)),
                          side: BorderSide(color: RegentaColors.line),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Datos de Identificación y Fiscales',
                                style: RegentaType.seccion.copyWith(fontSize: 16),
                              ),
                              const Divider(color: RegentaColors.line, height: 24),
                              TextFormField(
                                controller: _razonSocialCtrl,
                                style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                decoration: const InputDecoration(
                                  labelText: 'Razón Social *',
                                  labelStyle: TextStyle(color: RegentaColors.ink2, fontWeight: FontWeight.w500),
                                  border: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.line)),
                                  focusedBorder: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.accent, width: 2)),
                                ),
                                validator: (v) =>
                                    (v == null || v.trim().isEmpty) ? 'Requerido' : null,
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _nombreComercialCtrl,
                                style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                decoration: const InputDecoration(
                                  labelText: 'Nombre Comercial',
                                  labelStyle: TextStyle(color: RegentaColors.ink2, fontWeight: FontWeight.w500),
                                  border: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.line)),
                                  focusedBorder: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.accent, width: 2)),
                                ),
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _documentoCtrl,
                                style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                decoration: const InputDecoration(
                                  labelText: 'NIT / Número de Documento *',
                                  labelStyle: TextStyle(color: RegentaColors.ink2, fontWeight: FontWeight.w500),
                                  border: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.line)),
                                  focusedBorder: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.accent, width: 2)),
                                ),
                                validator: (v) =>
                                    (v == null || v.trim().isEmpty) ? 'Requerido' : null,
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),

                      // Tarjeta Contacto
                      Card(
                        elevation: 0,
                        color: RegentaColors.surface,
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.all(Radius.circular(10)),
                          side: BorderSide(color: RegentaColors.line),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Contacto y Ubicación',
                                style: RegentaType.seccion.copyWith(fontSize: 16),
                              ),
                              const Divider(color: RegentaColors.line, height: 24),
                              TextFormField(
                                controller: _direccionCtrl,
                                style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                decoration: const InputDecoration(
                                  labelText: 'Dirección Comercial',
                                  labelStyle: TextStyle(color: RegentaColors.ink2, fontWeight: FontWeight.w500),
                                  border: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.line)),
                                  focusedBorder: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.accent, width: 2)),
                                ),
                              ),
                              const SizedBox(height: 16),
                              Row(
                                children: [
                                  Expanded(
                                    child: TextFormField(
                                      controller: _ciudadCtrl,
                                      style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                      decoration: const InputDecoration(
                                        labelText: 'Ciudad',
                                        labelStyle: TextStyle(color: RegentaColors.ink2, fontWeight: FontWeight.w500),
                                        border: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.line)),
                                        focusedBorder: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.accent, width: 2)),
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: TextFormField(
                                      controller: _telefonoCtrl,
                                      style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                      decoration: const InputDecoration(
                                        labelText: 'Teléfono',
                                        labelStyle: TextStyle(color: RegentaColors.ink2, fontWeight: FontWeight.w500),
                                        border: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.line)),
                                        focusedBorder: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.accent, width: 2)),
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _emailCtrl,
                                style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                decoration: const InputDecoration(
                                  labelText: 'Correo Electrónico',
                                  labelStyle: TextStyle(color: RegentaColors.ink2, fontWeight: FontWeight.w500),
                                  border: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.line)),
                                  focusedBorder: OutlineInputBorder(borderSide: BorderSide(color: RegentaColors.accent, width: 2)),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),

                      // Tarjeta Políticas
                      Card(
                        elevation: 0,
                        color: RegentaColors.surface,
                        shape: const RoundedRectangleBorder(
                          borderRadius: BorderRadius.all(Radius.circular(10)),
                          side: BorderSide(color: RegentaColors.line),
                        ),
                        child: Padding(
                          padding: const EdgeInsets.all(20),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Políticas de Operación y Precios',
                                style: RegentaType.seccion.copyWith(fontSize: 16),
                              ),
                              const Divider(color: RegentaColors.line, height: 24),
                              SwitchListTile(
                                title: const Text('Los precios de venta incluyen impuestos', style: TextStyle(color: RegentaColors.ink, fontWeight: FontWeight.w600)),
                                subtitle: Text(
                                  'Si se desactiva, el IVA se sumará sobre el precio al cobrar.',
                                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                                ),
                                value: _preciosConIva,
                                onChanged: (val) => setState(() => _preciosConIva = val),
                              ),
                              const Divider(color: RegentaColors.line, height: 16),
                              SwitchListTile(
                                title: const Text('Permitir venta con inventario en negativo', style: TextStyle(color: RegentaColors.ink, fontWeight: FontWeight.w600)),
                                subtitle: Text(
                                  'Permite completar ventas aunque el stock registrado sea 0.',
                                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                                ),
                                value: _stockNegativo,
                                onChanged: (val) => setState(() => _stockNegativo = val),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 24),

                      // Botón Guardar
                      FilledButton.icon(
                        style: FilledButton.styleFrom(
                          backgroundColor: RegentaColors.accent,
                          padding: const EdgeInsets.symmetric(vertical: 16),
                        ),
                        onPressed: _guardando ? null : _guardar,
                        icon: _guardando
                            ? const SizedBox(
                                width: 20,
                                height: 20,
                                child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                              )
                            : const Icon(Icons.save),
                        label: Text(
                          _guardando ? 'Guardando...' : 'Guardar Configuración',
                          style: const TextStyle(fontSize: 16),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }
}
