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
  bool _busquedaDifusa = true;
  bool _creditoSinCupo = false;
  bool _impresionAutomaticaCotizacion = false;
  bool _impresionAutomaticaFactura = false;
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
    _busquedaDifusa = config.busquedaDifusa;
    _creditoSinCupo = config.creditoSinCupo;
    _impresionAutomaticaCotizacion = config.impresionAutomaticaCotizacion;
    _impresionAutomaticaFactura = config.impresionAutomaticaFactura;
    ConfiguradorDeBusqueda.busquedaDifusaHabilitada = _busquedaDifusa;
    ConfiguradorDeCredito.creditoSinCupoHabilitado = _creditoSinCupo;
    ConfiguradorDeStock.politicaStockNegativoHabilitada = _stockNegativo;
    ConfiguradorDeTicket.impresionAutomaticaCotizacion = _impresionAutomaticaCotizacion;
    ConfiguradorDeTicket.impresionAutomaticaFactura = _impresionAutomaticaFactura;
    ConfiguradorDeTicket.razonSocial = config.razonSocial;
    ConfiguradorDeTicket.nombreComercial = config.nombreComercial;
    ConfiguradorDeTicket.nit = config.numeroDocumento + (config.digitoVerificacion != null && config.digitoVerificacion!.isNotEmpty ? '-${config.digitoVerificacion}' : '');
    ConfiguradorDeTicket.direccion = config.direccion;
    ConfiguradorDeTicket.ciudad = config.ciudad;
    ConfiguradorDeTicket.telefono = config.telefono;
    ConfiguradorDeTicket.email = config.email;
    ConfiguradorDeTicket.regimenFiscal = config.regimenFiscal;
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
        busquedaDifusa: _busquedaDifusa,
        creditoSinCupo: _creditoSinCupo,
        impresionAutomaticaCotizacion: _impresionAutomaticaCotizacion,
        impresionAutomaticaFactura: _impresionAutomaticaFactura,
      );

      await repo.actualizarConfiguracion(actualizado);
      ConfiguradorDeBusqueda.busquedaDifusaHabilitada = _busquedaDifusa;
      ConfiguradorDeCredito.creditoSinCupoHabilitado = _creditoSinCupo;
      ConfiguradorDeStock.politicaStockNegativoHabilitada = _stockNegativo;
      ConfiguradorDeTicket.impresionAutomaticaCotizacion = _impresionAutomaticaCotizacion;
      ConfiguradorDeTicket.impresionAutomaticaFactura = _impresionAutomaticaFactura;
      ConfiguradorDeTicket.razonSocial = actualizado.razonSocial;
      ConfiguradorDeTicket.nombreComercial = actualizado.nombreComercial;
      ConfiguradorDeTicket.nit = actualizado.numeroDocumento;
      ConfiguradorDeTicket.direccion = actualizado.direccion;
      ConfiguradorDeTicket.ciudad = actualizado.ciudad;
      ConfiguradorDeTicket.telefono = actualizado.telefono;
      ConfiguradorDeTicket.email = actualizado.email;
      ConfiguradorDeTicket.regimenFiscal = actualizado.regimenFiscal;
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

  InputDecoration _decoracionCampo({
    required String labelText,
    String? hintText,
  }) {
    return InputDecoration(
      labelText: labelText,
      labelStyle: const TextStyle(color: RegentaColors.ink2, fontWeight: FontWeight.w500),
      hintText: hintText,
      hintStyle: const TextStyle(color: RegentaColors.muted, fontSize: 14),
      filled: true,
      fillColor: RegentaColors.surface,
      contentPadding: const EdgeInsets.symmetric(horizontal: 14, vertical: 14),
      border: OutlineInputBorder(
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        borderSide: const BorderSide(color: RegentaColors.line),
      ),
      enabledBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        borderSide: const BorderSide(color: RegentaColors.line),
      ),
      focusedBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        borderSide: const BorderSide(color: RegentaColors.accent, width: 2),
      ),
      errorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        borderSide: const BorderSide(color: RegentaColors.crit),
      ),
      focusedErrorBorder: OutlineInputBorder(
        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
        borderSide: const BorderSide(color: RegentaColors.crit, width: 2),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final configAsync = ref.watch(configuracionNegocioProvider);

    return Theme(
      data: PastelTheme.light(),
      child: Scaffold(
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
                                decoration: _decoracionCampo(labelText: 'Razón Social *'),
                                validator: (v) =>
                                    (v == null || v.trim().isEmpty) ? 'Requerido' : null,
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _nombreComercialCtrl,
                                style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                decoration: _decoracionCampo(labelText: 'Nombre Comercial'),
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _documentoCtrl,
                                style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                decoration: _decoracionCampo(labelText: 'NIT / Número de Documento *'),
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
                                decoration: _decoracionCampo(labelText: 'Dirección Comercial'),
                              ),
                              const SizedBox(height: 16),
                              Row(
                                children: [
                                  Expanded(
                                    child: TextFormField(
                                      controller: _ciudadCtrl,
                                      style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                      decoration: _decoracionCampo(labelText: 'Ciudad'),
                                    ),
                                  ),
                                  const SizedBox(width: 12),
                                  Expanded(
                                    child: TextFormField(
                                      controller: _telefonoCtrl,
                                      style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                      decoration: _decoracionCampo(labelText: 'Teléfono'),
                                    ),
                                  ),
                                ],
                              ),
                              const SizedBox(height: 16),
                              TextFormField(
                                controller: _emailCtrl,
                                style: const TextStyle(color: RegentaColors.ink, fontSize: 14),
                                decoration: _decoracionCampo(labelText: 'Correo Electrónico'),
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
                              const Divider(color: RegentaColors.line, height: 16),
                              SwitchListTile(
                                title: const Text('Búsqueda difusa multi-palabra', style: TextStyle(color: RegentaColors.ink, fontWeight: FontWeight.w600)),
                                subtitle: Text(
                                  'Si se activa, busca escribiendo palabras en cualquier orden (ej: "esmal negr"). Si se desactiva, usa la búsqueda directa estándar tradicional más rápida.',
                                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                                ),
                                value: _busquedaDifusa,
                                onChanged: (val) {
                                  setState(() => _busquedaDifusa = val);
                                  ConfiguradorDeBusqueda.busquedaDifusaHabilitada = val;
                                },
                              ),
                              const Divider(color: RegentaColors.line, height: 16),
                              SwitchListTile(
                                title: const Text('Permitir crédito directo sin cupo a todos los clientes', style: TextStyle(color: RegentaColors.ink, fontWeight: FontWeight.w600)),
                                subtitle: Text(
                                  'Si se activa, deja hacer crédito a cualquier cliente sin exigir cupo disponible asignado. Si se desactiva, valida y pide cupo.',
                                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                                ),
                                value: _creditoSinCupo,
                                onChanged: (val) {
                                  setState(() => _creditoSinCupo = val);
                                  ConfiguradorDeCredito.creditoSinCupoHabilitado = val;
                                },
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 20),

                      // Tarjeta Impresión y Facturación Térmica POS
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
                                'Impresión y Facturación Térmica POS',
                                style: RegentaType.seccion.copyWith(fontSize: 16),
                              ),
                              const Divider(color: RegentaColors.line, height: 24),
                              SwitchListTile(
                                title: const Text(
                                  'Imprimir cotización automáticamente',
                                  style: TextStyle(color: RegentaColors.ink, fontWeight: FontWeight.w600),
                                ),
                                subtitle: Text(
                                  'Si se activa, abre de inmediato la vista de impresión del tiquete al crear una cotización en el POS.',
                                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                                ),
                                value: _impresionAutomaticaCotizacion,
                                onChanged: (val) {
                                  setState(() => _impresionAutomaticaCotizacion = val);
                                  ConfiguradorDeTicket.impresionAutomaticaCotizacion = val;
                                },
                              ),
                              const Divider(color: RegentaColors.line, height: 16),
                              SwitchListTile(
                                title: const Text(
                                  'Imprimir factura / tirilla automáticamente',
                                  style: TextStyle(color: RegentaColors.ink, fontWeight: FontWeight.w600),
                                ),
                                subtitle: Text(
                                  'Si se activa, abre de inmediato la tirilla de venta para imprimir al cobrar en el POS.',
                                  style: RegentaType.cuerpo.copyWith(color: RegentaColors.ink2),
                                ),
                                value: _impresionAutomaticaFactura,
                                onChanged: (val) {
                                  setState(() => _impresionAutomaticaFactura = val);
                                  ConfiguradorDeTicket.impresionAutomaticaFactura = val;
                                },
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
    ),
  );
}
}
