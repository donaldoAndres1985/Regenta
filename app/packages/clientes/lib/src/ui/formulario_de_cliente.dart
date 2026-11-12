import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/ficha_de_cliente.dart';

/// Abre la ficha para crear un cliente ([ficha] nula) o editarlo.
///
/// [onGuardar] devuelve `null` si el servidor lo aceptó, o el motivo del
/// rechazo: el diálogo se queda abierto y lo muestra, para corregir sin volver
/// a escribir todo. Devuelve `true` si quedó guardado.
Future<bool> abrirFormularioDeCliente(
  BuildContext context, {
  FichaDeCliente? ficha,
  required Future<String?> Function(FichaDeCliente) onGuardar,
}) async {
  final guardado = await showDialog<bool>(
    context: context,
    barrierDismissible: false,
    builder: (_) => Dialog(
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 540, maxHeight: 720),
        child: FormularioDeCliente(ficha: ficha, onGuardar: onGuardar),
      ),
    ),
  );
  return guardado ?? false;
}

/// La ficha del cliente: lo que pide `POST /api/clientes` y deja cambiar
/// `PUT /api/clientes/{id}`. El tipo de persona y el documento no se editan:
/// son la identidad del cliente y el servidor no los cambia.
class FormularioDeCliente extends StatefulWidget {
  const FormularioDeCliente({super.key, this.ficha, required this.onGuardar});

  final FichaDeCliente? ficha;
  final Future<String?> Function(FichaDeCliente) onGuardar;

  @override
  State<FormularioDeCliente> createState() => _FormularioDeClienteState();
}

class _FormularioDeClienteState extends State<FormularioDeCliente> {
  static const _documentos = <String, String>{
    'CC': 'Cédula',
    'NIT': 'NIT',
    'CE': 'Cédula de extranjería',
    'TI': 'Tarjeta de identidad',
    'PP': 'Pasaporte',
    'NIT_EXT': 'NIT extranjero',
    'SIN_IDENTIFICAR': 'Sin identificar',
  };

  static const _segmentos = <String>['MINORISTA', 'MAYORISTA', 'VIP'];

  late final TextEditingController _numero;
  late final TextEditingController _dv;
  late final TextEditingController _nombres;
  late final TextEditingController _apellidos;
  late final TextEditingController _razonSocial;
  late final TextEditingController _email;
  late final TextEditingController _telefono;
  late final TextEditingController _telefonoAlterno;
  late final TextEditingController _notas;

  late String _tipoPersona;
  late String _tipoDocumento;
  String? _segmento;
  String? _regimen;
  String? _error;
  bool _guardando = false;

  bool get _editando => widget.ficha?.id != null;
  bool get _juridica => _tipoPersona == 'JURIDICA';
  bool get _sinIdentificar => _tipoDocumento == 'SIN_IDENTIFICAR';

  @override
  void initState() {
    super.initState();
    final f = widget.ficha ?? const FichaDeCliente();
    _tipoPersona = f.tipoPersona;
    _tipoDocumento = _documentos.containsKey(f.tipoDocumento) ? f.tipoDocumento : 'CC';
    _segmento = (f.segmento == null || f.segmento!.trim().isEmpty)
        ? null
        : f.segmento!.trim().toUpperCase();
    _regimen = f.regimenFiscal;
    _numero = TextEditingController(text: f.numeroDocumento ?? '');
    _dv = TextEditingController(text: f.digitoVerificacion ?? '');
    _nombres = TextEditingController(text: f.nombres ?? '');
    _apellidos = TextEditingController(text: f.apellidos ?? '');
    _razonSocial = TextEditingController(text: f.razonSocial ?? '');
    _email = TextEditingController(text: f.email ?? '');
    _telefono = TextEditingController(text: f.telefono ?? '');
    _telefonoAlterno = TextEditingController(text: f.telefonoAlterno ?? '');
    _notas = TextEditingController(text: f.notas ?? '');
  }

  @override
  void dispose() {
    for (final c in [
      _numero,
      _dv,
      _nombres,
      _apellidos,
      _razonSocial,
      _email,
      _telefono,
      _telefonoAlterno,
      _notas,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  String? _validar() {
    if (!_sinIdentificar && _numero.text.trim().isEmpty) {
      return 'Falta el número de documento del cliente.';
    }
    if (_juridica && _razonSocial.text.trim().isEmpty) {
      return 'Falta la razón social.';
    }
    if (!_juridica && !_sinIdentificar && _nombres.text.trim().isEmpty) {
      return 'Falta el nombre del cliente.';
    }
    final email = _email.text.trim();
    if (email.isNotEmpty && !RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email)) {
      return 'El correo no parece válido.';
    }
    return null;
  }

  Future<void> _guardar() async {
    final problema = _validar();
    if (problema != null) {
      setState(() => _error = problema);
      return;
    }
    setState(() {
      _error = null;
      _guardando = true;
    });
    final rechazo = await widget.onGuardar(FichaDeCliente(
      id: widget.ficha?.id,
      tipoPersona: _tipoPersona,
      tipoDocumento: _tipoDocumento,
      numeroDocumento: _sinIdentificar ? null : _numero.text,
      digitoVerificacion: _tipoDocumento == 'NIT' ? _dv.text : null,
      nombres: _juridica ? null : _nombres.text,
      apellidos: _juridica ? null : _apellidos.text,
      razonSocial: _juridica ? _razonSocial.text : null,
      email: _email.text,
      telefono: _telefono.text,
      telefonoAlterno: _telefonoAlterno.text,
      segmento: _segmento,
      notas: _notas.text,
      regimenFiscal: _regimen,
    ));
    if (!mounted) return;
    if (rechazo == null) {
      Navigator.of(context).pop(true);
    } else {
      setState(() {
        _error = rechazo;
        _guardando = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return CallbackShortcuts(
      bindings: {
        const SingleActivator(LogicalKeyboardKey.escape): () {
          if (!_guardando) Navigator.of(context).pop(false);
        },
      },
      child: Focus(
        autofocus: true,
        child: Material(
          color: RegentaColors.surface,
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.fromLTRB(18, 14, 8, 14),
              decoration: const BoxDecoration(
                border: Border(bottom: BorderSide(color: RegentaColors.line)),
              ),
              child: Row(children: [
                Expanded(
                  child: Text(_editando ? 'Editar cliente' : 'Nuevo cliente',
                      style: RegentaType.seccion
                          .copyWith(fontSize: 16, color: RegentaColors.ink)),
                ),
                IconButton(
                  tooltip: 'Cerrar',
                  onPressed: _guardando ? null : () => Navigator.of(context).pop(false),
                  icon: const Icon(Icons.close, size: 20, color: RegentaColors.ink2),
                ),
              ]),
            ),
            Flexible(
              child: SingleChildScrollView(
                padding: const EdgeInsets.fromLTRB(18, 16, 18, 8),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    _Etiqueta('Tipo de persona'),
                    SegmentedButton<String>(
                      key: const Key('cliente-tipo-persona'),
                      style: ButtonStyle(
                        backgroundColor: WidgetStateProperty.resolveWith((states) {
                          if (states.contains(WidgetState.selected)) {
                            return RegentaColors.accent;
                          }
                          return RegentaColors.sunken;
                        }),
                        foregroundColor: WidgetStateProperty.resolveWith((states) {
                          if (states.contains(WidgetState.selected)) {
                            return Colors.white;
                          }
                          return RegentaColors.inkFuerte;
                        }),
                        textStyle: WidgetStateProperty.all(
                          const TextStyle(
                              fontSize: 13,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 0.5),
                        ),
                        side: WidgetStateProperty.resolveWith((states) {
                          if (states.contains(WidgetState.selected)) {
                            return const BorderSide(
                                color: RegentaColors.accent, width: 1.5);
                          }
                          return const BorderSide(color: RegentaColors.line2);
                        }),
                        shape: WidgetStateProperty.all(
                          RoundedRectangleBorder(
                              borderRadius:
                                  BorderRadius.circular(RegentaSpacing.radius)),
                        ),
                      ),
                      segments: [
                        ButtonSegment(
                          value: 'NATURAL',
                          label: Text(
                            'NATURAL',
                            style: TextStyle(
                              fontWeight: FontWeight.w800,
                              color: _tipoPersona == 'NATURAL'
                                  ? Colors.white
                                  : RegentaColors.inkFuerte,
                            ),
                          ),
                          icon: Icon(
                            Icons.person_outline,
                            size: 18,
                            color: _tipoPersona == 'NATURAL'
                                ? Colors.white
                                : RegentaColors.inkFuerte,
                          ),
                        ),
                        ButtonSegment(
                          value: 'JURIDICA',
                          label: Text(
                            'JURÍDICA',
                            style: TextStyle(
                              fontWeight: FontWeight.w800,
                              color: _tipoPersona == 'JURIDICA'
                                  ? Colors.white
                                  : RegentaColors.inkFuerte,
                            ),
                          ),
                          icon: Icon(
                            Icons.business_outlined,
                            size: 18,
                            color: _tipoPersona == 'JURIDICA'
                                ? Colors.white
                                : RegentaColors.inkFuerte,
                          ),
                        ),
                      ],
                      selected: {_tipoPersona},
                      showSelectedIcon: false,
                      onSelectionChanged: _editando
                          ? null
                          : (s) => setState(() {
                                _tipoPersona = s.first;
                                if (_juridica && _tipoDocumento == 'CC') {
                                  _tipoDocumento = 'NIT';
                                } else if (!_juridica && _tipoDocumento == 'NIT') {
                                  _tipoDocumento = 'CC';
                                }
                              }),
                    ),
                  const SizedBox(height: 12),
                  Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Expanded(
                      flex: 4,
                      child: _Campo(
                        etiqueta: 'Documento',
                        hijo: DropdownButtonFormField<String>(
                          key: const Key('cliente-tipo-documento'),
                          initialValue: _tipoDocumento,
                          isExpanded: true,
                          decoration: _decoracion(),
                          items: [
                            for (final d in _documentos.entries)
                              DropdownMenuItem(
                                value: d.key,
                                child: Text(d.value, overflow: TextOverflow.ellipsis),
                              ),
                          ],
                          onChanged: _editando
                              ? null
                              : (v) => setState(() => _tipoDocumento = v ?? 'CC'),
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      flex: 5,
                      child: _Campo(
                        etiqueta: _sinIdentificar ? 'Número' : 'Número *',
                        hijo: TextField(
                          key: const Key('cliente-numero'),
                          controller: _numero,
                          enabled: !_editando && !_sinIdentificar,
                          keyboardType: TextInputType.number,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: RegentaColors.inkFuerte,
                          ),
                          decoration: _decoracion(),
                        ),
                      ),
                    ),
                    if (_tipoDocumento == 'NIT') ...[
                      const SizedBox(width: 10),
                      SizedBox(
                        width: 58,
                        child: _Campo(
                          etiqueta: 'DV',
                          hijo: TextField(
                            key: const Key('cliente-dv'),
                            controller: _dv,
                            enabled: !_editando,
                            maxLength: 1,
                            keyboardType: TextInputType.number,
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              color: RegentaColors.inkFuerte,
                            ),
                            decoration: _decoracion().copyWith(counterText: ''),
                          ),
                        ),
                      ),
                    ],
                  ]),
                  if (_juridica)
                    _Campo(
                      etiqueta: 'Razón social *',
                      hijo: TextField(
                        key: const Key('cliente-razon-social'),
                        controller: _razonSocial,
                        textCapitalization: TextCapitalization.words,
                        style: const TextStyle(
                          fontSize: 14,
                          fontWeight: FontWeight.w600,
                          color: RegentaColors.inkFuerte,
                        ),
                        decoration: _decoracion(),
                      ),
                    )
                  else
                    Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                      Expanded(
                        child: _Campo(
                          etiqueta: _sinIdentificar ? 'Nombres' : 'Nombres *',
                          hijo: TextField(
                            key: const Key('cliente-nombres'),
                            controller: _nombres,
                            textCapitalization: TextCapitalization.words,
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              color: RegentaColors.inkFuerte,
                            ),
                            decoration: _decoracion(),
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: _Campo(
                          etiqueta: 'Apellidos',
                          hijo: TextField(
                            key: const Key('cliente-apellidos'),
                            controller: _apellidos,
                            textCapitalization: TextCapitalization.words,
                            style: const TextStyle(
                              fontSize: 14,
                              fontWeight: FontWeight.w600,
                              color: RegentaColors.inkFuerte,
                            ),
                            decoration: _decoracion(),
                          ),
                        ),
                      ),
                    ]),
                  Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Expanded(
                      child: _Campo(
                        etiqueta: 'Celular',
                        hijo: TextField(
                          key: const Key('cliente-telefono'),
                          controller: _telefono,
                          keyboardType: TextInputType.phone,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: RegentaColors.inkFuerte,
                          ),
                          decoration: _decoracion(),
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _Campo(
                        etiqueta: 'Otro teléfono',
                        hijo: TextField(
                          key: const Key('cliente-telefono-alterno'),
                          controller: _telefonoAlterno,
                          keyboardType: TextInputType.phone,
                          style: const TextStyle(
                            fontSize: 14,
                            fontWeight: FontWeight.w600,
                            color: RegentaColors.inkFuerte,
                          ),
                          decoration: _decoracion(),
                        ),
                      ),
                    ),
                  ]),
                  _Campo(
                    etiqueta: 'Correo',
                    ayuda: 'Ahí llega la factura electrónica',
                    hijo: TextField(
                      key: const Key('cliente-correo'),
                      controller: _email,
                      keyboardType: TextInputType.emailAddress,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.inkFuerte,
                      ),
                      decoration: _decoracion(),
                    ),
                  ),
                  Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                    Expanded(
                      child: _Campo(
                        etiqueta: 'Lista de precios',
                        hijo: DropdownButtonFormField<String?>(
                          key: const Key('cliente-segmento'),
                          initialValue: _segmento,
                          isExpanded: true,
                          decoration: _decoracion(),
                          items: [
                            const DropdownMenuItem<String?>(
                                value: null, child: Text('Sin asignar')),
                            for (final s in {..._segmentos, ?_segmento})
                              DropdownMenuItem<String?>(
                                value: s,
                                child: Text(s[0] + s.substring(1).toLowerCase(),
                                    overflow: TextOverflow.ellipsis),
                              ),
                          ],
                          onChanged: (v) => setState(() => _segmento = v),
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: _Campo(
                        etiqueta: 'Régimen fiscal',
                        hijo: DropdownButtonFormField<String?>(
                          key: const Key('cliente-regimen'),
                          initialValue: _regimen,
                          isExpanded: true,
                          decoration: _decoracion(),
                          items: [
                            const DropdownMenuItem<String?>(
                                value: null, child: Text('Sin indicar')),
                            const DropdownMenuItem<String?>(
                                value: 'RESPONSABLE_IVA',
                                child: Text('Responsable de IVA',
                                    overflow: TextOverflow.ellipsis)),
                            const DropdownMenuItem<String?>(
                                value: 'NO_RESPONSABLE',
                                child: Text('No responsable de IVA',
                                    overflow: TextOverflow.ellipsis)),
                            if (_regimen != null &&
                                _regimen != 'RESPONSABLE_IVA' &&
                                _regimen != 'NO_RESPONSABLE')
                              DropdownMenuItem<String?>(
                                  value: _regimen,
                                  child: Text(_regimen!, overflow: TextOverflow.ellipsis)),
                          ],
                          onChanged: (v) => setState(() => _regimen = v),
                        ),
                      ),
                    ),
                  ]),
                  _Campo(
                    etiqueta: 'Comentario',
                    hijo: TextField(
                      key: const Key('cliente-notas'),
                      controller: _notas,
                      minLines: 2,
                      maxLines: 4,
                      style: const TextStyle(
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: RegentaColors.inkFuerte,
                      ),
                      decoration: _decoracion(),
                    ),
                  ),
                  if (_error != null)
                    Container(
                      key: const Key('cliente-error'),
                      margin: const EdgeInsets.only(bottom: 8),
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: RegentaColors.critSoft,
                        borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                      ),
                      child: Text(_error!,
                          style: RegentaType.cuerpo
                              .copyWith(fontSize: 12.5, color: RegentaColors.crit)),
                    ),
                ],
              ),
            ),
          ),
          Container(
            padding: const EdgeInsets.fromLTRB(18, 12, 18, 14),
            decoration: const BoxDecoration(
              border: Border(top: BorderSide(color: RegentaColors.line)),
            ),
            child: Row(children: [
              Expanded(
                child: SizedBox(
                  height: RegentaSpacing.hitTarget,
                  child: OutlinedButton(
                    onPressed: _guardando ? null : () => Navigator.of(context).pop(false),
                    child: const Text('Cancelar'),
                  ),
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: SizedBox(
                  height: RegentaSpacing.hitTarget,
                  child: FilledButton(
                    key: const Key('cliente-guardar'),
                    onPressed: _guardando ? null : _guardar,
                    style: FilledButton.styleFrom(
                      backgroundColor: RegentaColors.accent,
                      foregroundColor: RegentaColors.surface,
                      shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
                    ),
                    child: Text(_guardando
                        ? 'Guardando…'
                        : (_editando ? 'Guardar cambios' : 'Guardar cliente')),
                  ),
                ),
              ),
            ]),
          ),
        ],
      ),
    ),
    ),
  );
}

  InputDecoration _decoracion() => InputDecoration(
        isDense: true,
        filled: true,
        fillColor: RegentaColors.surface,
        contentPadding: const EdgeInsets.symmetric(horizontal: 10, vertical: 12),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RegentaSpacing.radius),
          borderSide: const BorderSide(color: RegentaColors.line2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RegentaSpacing.radius),
          borderSide: const BorderSide(color: RegentaColors.line2),
        ),
        focusedBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RegentaSpacing.radius),
          borderSide: const BorderSide(color: RegentaColors.accent, width: 1.5),
        ),
      );
}

class _Etiqueta extends StatelessWidget {
  const _Etiqueta(this.texto);

  final String texto;

  @override
  Widget build(BuildContext context) => Padding(
        padding: const EdgeInsets.only(bottom: 4),
        child: Text(texto.toUpperCase(), style: RegentaType.etiqueta.copyWith(fontSize: 9.5)),
      );
}

class _Campo extends StatelessWidget {
  const _Campo({required this.etiqueta, required this.hijo, this.ayuda});

  final String etiqueta;
  final Widget hijo;
  final String? ayuda;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Column(crossAxisAlignment: CrossAxisAlignment.start, children: [
        _Etiqueta(etiqueta),
        hijo,
        if (ayuda != null) ...[
          const SizedBox(height: 3),
          Text(ayuda!,
              style: RegentaType.cuerpo.copyWith(fontSize: 11, color: RegentaColors.muted)),
        ],
      ]),
    );
  }
}
