/// Configuración fiscal y de operación del negocio (HU-018).
class ConfiguracionNegocioVista {
  const ConfiguracionNegocioVista({
    required this.razonSocial,
    required this.nombreComercial,
    required this.tipoDocumento,
    required this.numeroDocumento,
    this.digitoVerificacion,
    this.direccion = '',
    this.ciudad = '',
    this.departamento = '',
    this.telefono = '',
    this.email = '',
    this.regimenFiscal = 'RESPONSABLE_DE_IVA',
    this.preciosIncluyenImpuesto = true,
    this.politicaStockNegativo = false,
    this.busquedaDifusa = true,
    this.creditoSinCupo = false,
    this.impresionAutomaticaCotizacion = false,
    this.impresionAutomaticaFactura = false,
  });

  final String razonSocial;
  final String nombreComercial;
  final String tipoDocumento;
  final String numeroDocumento;
  final String? digitoVerificacion;
  final String direccion;
  final String ciudad;
  final String departamento;
  final String telefono;
  final String email;
  final String regimenFiscal;
  final bool preciosIncluyenImpuesto;
  final bool politicaStockNegativo;
  final bool busquedaDifusa;
  final bool creditoSinCupo;
  final bool impresionAutomaticaCotizacion;
  final bool impresionAutomaticaFactura;

  factory ConfiguracionNegocioVista.desdeJson(Map<String, dynamic> j) =>
      ConfiguracionNegocioVista(
        razonSocial: (j['razonSocial'] as String?) ?? '',
        nombreComercial: (j['nombreComercial'] as String?) ?? '',
        tipoDocumento: (j['tipoDocumento'] as String?) ?? 'NIT',
        numeroDocumento: (j['numeroDocumento'] as String?) ?? '',
        digitoVerificacion: j['digitoVerificacion'] as String?,
        direccion: (j['direccion'] as String?) ?? '',
        ciudad: (j['ciudad'] as String?) ?? '',
        departamento: (j['departamento'] as String?) ?? '',
        telefono: (j['telefono'] as String?) ?? '',
        email: (j['email'] as String?) ?? '',
        regimenFiscal: (j['regimenFiscal'] as String?) ?? 'RESPONSABLE_DE_IVA',
        preciosIncluyenImpuesto: (j['preciosIncluyenImpuesto'] as bool?) ?? true,
        politicaStockNegativo: (j['politicaStockNegativo'] as bool?) ?? false,
        busquedaDifusa: (j['busquedaDifusa'] as bool?) ?? true,
        creditoSinCupo: (j['creditoSinCupo'] as bool?) ?? false,
        impresionAutomaticaCotizacion: (j['impresionAutomaticaCotizacion'] as bool?) ??
            ((j['preferencias'] as Map<String, dynamic>?)?['impresionAutomaticaCotizacion'] as bool?) ??
            false,
        impresionAutomaticaFactura: (j['impresionAutomaticaFactura'] as bool?) ??
            ((j['preferencias'] as Map<String, dynamic>?)?['impresionAutomaticaFactura'] as bool?) ??
            false,
      );

  Map<String, dynamic> aJson() => {
        'razonSocial': razonSocial,
        'nombreComercial': nombreComercial,
        'tipoDocumento': tipoDocumento,
        'numeroDocumento': numeroDocumento,
        if (digitoVerificacion != null) 'digitoVerificacion': digitoVerificacion,
        'direccion': direccion,
        'ciudad': ciudad,
        'departamento': departamento,
        'telefono': telefono,
        'email': email,
        'regimenFiscal': regimenFiscal,
        'preciosIncluyenImpuesto': preciosIncluyenImpuesto,
        'politicaStockNegativo': politicaStockNegativo,
        'busquedaDifusa': busquedaDifusa,
        'creditoSinCupo': creditoSinCupo,
        'impresionAutomaticaCotizacion': impresionAutomaticaCotizacion,
        'impresionAutomaticaFactura': impresionAutomaticaFactura,
        'preferencias': {
          'busquedaDifusa': busquedaDifusa,
          'creditoSinCupo': creditoSinCupo,
          'impresionAutomaticaCotizacion': impresionAutomaticaCotizacion,
          'impresionAutomaticaFactura': impresionAutomaticaFactura,
        },
      };
}
