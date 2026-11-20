import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:regenta_core/regenta_core.dart';

import '../datos/proveedor_del_negocio.dart';
import 'piezas.dart';

/// Qué pasó en la ficha, para que el listado lo avise.
enum ResultadoDeFicha { nada, guardado, desactivado, reactivado }

/// Abre la ficha para crear un proveedor ([proveedor] nulo), editarlo, desactivarlo o, si está
/// desactivado, reactivarlo (HU-166 C3). Cada acción devuelve `null` si el servidor la aceptó, o
/// el motivo del rechazo, que la ficha muestra sin cerrarse.
Future<ResultadoDeFicha> abrirFormularioDeProveedor(
  BuildContext context, {
  ProveedorDelNegocio? proveedor,
  required Future<String?> Function(ProveedorDelNegocio) onGuardar,
  Future<String?> Function(String proveedorId)? onDesactivar,
  Future<String?> Function(String proveedorId)? onReactivar,
}) async {
  final resultado = await showDialog<ResultadoDeFicha>(
    context: context,
    barrierDismissible: false,
    builder: (_) => Dialog(
      clipBehavior: Clip.antiAlias,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 540, maxHeight: 760),
        child: FormularioDeProveedor(
          proveedor: proveedor,
          onGuardar: onGuardar,
          onDesactivar: onDesactivar,
          onReactivar: onReactivar,
        ),
      ),
    ),
  );
  return resultado ?? ResultadoDeFicha.nada;
}

/// La ficha del proveedor. El documento no se edita: es su identidad y el servidor no lo cambia.
class FormularioDeProveedor extends StatefulWidget {
  const FormularioDeProveedor({
    super.key,
    this.proveedor,
    required this.onGuardar,
    this.onDesactivar,
    this.onReactivar,
  });

  final ProveedorDelNegocio? proveedor;
  final Future<String?> Function(ProveedorDelNegocio) onGuardar;
  final Future<String?> Function(String proveedorId)? onDesactivar;
  final Future<String?> Function(String proveedorId)? onReactivar;

  @override
  State<FormularioDeProveedor> createState() => _FormularioDeProveedorState();
}

class _FormularioDeProveedorState extends State<FormularioDeProveedor> {
  static const _documentos = <String, String>{
    'NIT': 'NIT',
    'CC': 'Cédula',
    'CE': 'Cédula de extranjería',
    'PP': 'Pasaporte',
    'NIT_EXT': 'NIT extranjero',
    'SIN_IDENTIFICAR': 'Sin identificar',
  };

  late final TextEditingController _numero;
  late final TextEditingController _razonSocial;
  late final TextEditingController _nombreComercial;
  late final TextEditingController _contacto;
  late final TextEditingController _telefono;
  late final TextEditingController _email;
  late final TextEditingController _direccion;
  late final TextEditingController _ciudad;
  late final TextEditingController _diasCredito;
  late final TextEditingController _cupo;
  late final TextEditingController _notas;
  late String _tipoDocumento;
  String? _error;
  bool _ocupado = false;

  ProveedorDelNegocio get _original => widget.proveedor ?? const ProveedorDelNegocio();
  bool get _editando => widget.proveedor?.id != null;
  bool get _inactivo => _editando && !_original.activo;

  @override
  void initState() {
    super.initState();
    final p = _original;
    _tipoDocumento = _documentos.containsKey(p.tipoDocumento) ? p.tipoDocumento : 'NIT';
    _numero = TextEditingController(text: p.numeroDocumento);
    _razonSocial = TextEditingController(text: p.razonSocial);
    _nombreComercial = TextEditingController(text: p.nombreComercial ?? '');
    _contacto = TextEditingController(text: p.contactoNombre ?? '');
    _telefono = TextEditingController(text: p.telefono ?? '');
    _email = TextEditingController(text: p.email ?? '');
    _direccion = TextEditingController(text: p.direccion ?? '');
    _ciudad = TextEditingController(text: p.ciudad ?? '');
    _diasCredito = TextEditingController(text: '${p.diasCredito}');
    _cupo = TextEditingController(text: p.cupoCredito == 0 ? '' : '${p.cupoCredito.round()}');
    _notas = TextEditingController(text: p.notas ?? '');
  }

  @override
  void dispose() {
    for (final c in [
      _numero, _razonSocial, _nombreComercial, _contacto, _telefono, _email,
      _direccion, _ciudad, _diasCredito, _cupo, _notas,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  String? _validar() {
    if (_numero.text.trim().isEmpty) return 'Falta el número de documento del proveedor.';
    if (_razonSocial.text.trim().isEmpty) return 'Falta la razón social.';
    final email = _email.text.trim();
    if (email.isNotEmpty && !RegExp(r'^[^@\s]+@[^@\s]+\.[^@\s]+$').hasMatch(email)) {
      return 'El correo no parece válido.';
    }
    if (int.tryParse(_diasCredito.text.trim().isEmpty ? '0' : _diasCredito.text.trim()) == null) {
      return 'Los días de crédito deben ser un número entero.';
    }
    if (_cupo.text.trim().isNotEmpty && num.tryParse(_soloDigitos(_cupo.text)) == null) {
      return 'El cupo debe ser un número.';
    }
    return null;
  }

  static String _soloDigitos(String texto) => texto.replaceAll(RegExp(r'[^0-9]'), '');

  Future<void> _guardar() async {
    final problema = _validar();
    if (problema != null) {
      setState(() => _error = problema);
      return;
    }
    final cupoTexto = _soloDigitos(_cupo.text);
    await _ejecutar(
      () => widget.onGuardar(ProveedorDelNegocio(
        id: widget.proveedor?.id,
        tipoDocumento: _tipoDocumento,
        numeroDocumento: _numero.text,
        razonSocial: _razonSocial.text,
        nombreComercial: _nombreComercial.text,
        contactoNombre: _contacto.text,
        telefono: _telefono.text,
        email: _email.text,
        direccion: _direccion.text,
        ciudad: _ciudad.text,
        diasCredito: int.tryParse(_diasCredito.text.trim()) ?? 0,
        cupoCredito: cupoTexto.isEmpty ? 0 : num.parse(cupoTexto),
        notas: _notas.text,
      )),
      ResultadoDeFicha.guardado,
    );
  }

  /// HU-166 C3: desactivar pide confirmación y explica qué pasa.
  Future<void> _desactivar() async {
    final confirmado = await showDialog<bool>(
      context: context,
      builder: (dialogo) => AlertDialog(
        title: Text('¿Desactivar a ${_original.nombre}?'),
        content: const Text(
          'Dejará de aparecer al hacer órdenes y compras. Su historial, sus cuentas y sus pagos '
          'se conservan, y lo puedes reactivar cuando quieras.',
        ),
        actions: [
          TextButton(onPressed: () => Navigator.of(dialogo).pop(false), child: const Text('Cancelar')),
          FilledButton(
            key: const Key('proveedor-confirmar-desactivar'),
            style: FilledButton.styleFrom(backgroundColor: RegentaColors.crit),
            onPressed: () => Navigator.of(dialogo).pop(true),
            child: const Text('Desactivar'),
          ),
        ],
      ),
    );
    if (confirmado != true || !mounted) return;
    await _ejecutar(() => widget.onDesactivar!(_original.id!), ResultadoDeFicha.desactivado);
  }

  Future<void> _reactivar() =>
      _ejecutar(() => widget.onReactivar!(_original.id!), ResultadoDeFicha.reactivado);

  Future<void> _ejecutar(Future<String?> Function() accion, ResultadoDeFicha siSale) async {
    setState(() {
      _error = null;
      _ocupado = true;
    });
    final rechazo = await accion();
    if (!mounted) return;
    if (rechazo == null) {
      Navigator.of(context).pop(siSale);
    } else {
      setState(() {
        _error = rechazo;
        _ocupado = false;
      });
    }
  }

  Widget _texto(String clave, TextEditingController c,
      {TextInputType? teclado, int lineas = 1, bool habilitado = true, TextCapitalization? mayus}) {
    return TextField(
      key: Key('proveedor-$clave'),
      controller: c,
      enabled: habilitado && !_inactivo,
      keyboardType: teclado,
      minLines: lineas,
      maxLines: lineas == 1 ? 1 : lineas + 2,
      textCapitalization: mayus ?? TextCapitalization.none,
      style: estiloDeCampo,
      decoration: decoracionDeCampo(),
    );
  }

  @override
  Widget build(BuildContext context) {
    final titulo = !_editando ? 'Nuevo proveedor' : (_inactivo ? 'Proveedor desactivado' : 'Editar proveedor');
    return CallbackShortcuts(
      bindings: {
        const SingleActivator(LogicalKeyboardKey.escape): () {
          if (!_ocupado) Navigator.of(context).pop(ResultadoDeFicha.nada);
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
              CabeceraDeDialogo(
                titulo: titulo,
                subtitulo: _editando ? _original.nombre : null,
                onCerrar: _ocupado ? null : () => Navigator.of(context).pop(ResultadoDeFicha.nada),
              ),
              Flexible(
                child: SingleChildScrollView(
                  padding: const EdgeInsets.fromLTRB(18, 16, 18, 8),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      if (_inactivo)
                        Container(
                          key: const Key('proveedor-aviso-inactivo'),
                          margin: const EdgeInsets.only(bottom: 12),
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: RegentaColors.warnSoft,
                            borderRadius: BorderRadius.circular(RegentaSpacing.radius),
                          ),
                          child: Text(
                            'Este proveedor está desactivado: no aparece al comprar. '
                            'Reactívalo para volver a comprarle o editarlo.',
                            style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.warn),
                          ),
                        ),
                      Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Expanded(
                          flex: 4,
                          child: CampoConEtiqueta(
                            etiqueta: 'Documento',
                            hijo: DropdownButtonFormField<String>(
                              key: const Key('proveedor-tipo-documento'),
                              initialValue: _tipoDocumento,
                              isExpanded: true,
                              decoration: decoracionDeCampo(),
                              items: [
                                for (final d in _documentos.entries)
                                  DropdownMenuItem(
                                    value: d.key,
                                    child: Text(d.value, overflow: TextOverflow.ellipsis),
                                  ),
                              ],
                              onChanged: _editando ? null : (v) => setState(() => _tipoDocumento = v ?? 'NIT'),
                            ),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          flex: 5,
                          child: CampoConEtiqueta(
                            etiqueta: 'Número *',
                            hijo: _texto('numero', _numero,
                                teclado: TextInputType.number, habilitado: !_editando),
                          ),
                        ),
                      ]),
                      CampoConEtiqueta(
                        etiqueta: 'Razón social *',
                        hijo: _texto('razon-social', _razonSocial, mayus: TextCapitalization.words),
                      ),
                      Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Expanded(
                          child: CampoConEtiqueta(
                            etiqueta: 'Nombre comercial',
                            hijo: _texto('nombre-comercial', _nombreComercial,
                                mayus: TextCapitalization.words),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: CampoConEtiqueta(
                            etiqueta: 'Contacto',
                            hijo: _texto('contacto', _contacto, mayus: TextCapitalization.words),
                          ),
                        ),
                      ]),
                      Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Expanded(
                          child: CampoConEtiqueta(
                            etiqueta: 'Teléfono',
                            hijo: _texto('telefono', _telefono, teclado: TextInputType.phone),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: CampoConEtiqueta(
                            etiqueta: 'Correo',
                            hijo: _texto('correo', _email, teclado: TextInputType.emailAddress),
                          ),
                        ),
                      ]),
                      Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Expanded(
                          flex: 3,
                          child: CampoConEtiqueta(etiqueta: 'Dirección', hijo: _texto('direccion', _direccion)),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          flex: 2,
                          child: CampoConEtiqueta(
                            etiqueta: 'Ciudad',
                            hijo: _texto('ciudad', _ciudad, mayus: TextCapitalization.words),
                          ),
                        ),
                      ]),
                      Row(crossAxisAlignment: CrossAxisAlignment.start, children: [
                        Expanded(
                          child: CampoConEtiqueta(
                            etiqueta: 'Días de crédito',
                            ayuda: '0 = de contado',
                            hijo: _texto('dias-credito', _diasCredito, teclado: TextInputType.number),
                          ),
                        ),
                        const SizedBox(width: 10),
                        Expanded(
                          child: CampoConEtiqueta(
                            etiqueta: 'Cupo de crédito',
                            ayuda: 'Solo informa: avisa si la deuda lo pasa',
                            hijo: _texto('cupo', _cupo, teclado: TextInputType.number),
                          ),
                        ),
                      ]),
                      CampoConEtiqueta(
                        etiqueta: 'Notas',
                        hijo: _texto('notas', _notas, lineas: 2),
                      ),
                      if (_error != null) AvisoDeError(_error!, key: const Key('proveedor-error')),
                    ],
                  ),
                ),
              ),
              _Pie(
                editando: _editando,
                inactivo: _inactivo,
                ocupado: _ocupado,
                onCancelar: () => Navigator.of(context).pop(ResultadoDeFicha.nada),
                onGuardar: _guardar,
                onDesactivar: widget.onDesactivar == null ? null : _desactivar,
                onReactivar: widget.onReactivar == null ? null : _reactivar,
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _Pie extends StatelessWidget {
  const _Pie({
    required this.editando,
    required this.inactivo,
    required this.ocupado,
    required this.onCancelar,
    required this.onGuardar,
    this.onDesactivar,
    this.onReactivar,
  });

  final bool editando;
  final bool inactivo;
  final bool ocupado;
  final VoidCallback onCancelar;
  final VoidCallback onGuardar;
  final VoidCallback? onDesactivar;
  final VoidCallback? onReactivar;

  @override
  Widget build(BuildContext context) {
    final principal = inactivo
        ? FilledButton.icon(
            key: const Key('proveedor-reactivar'),
            onPressed: ocupado || onReactivar == null ? null : onReactivar,
            style: _estiloPrincipal,
            icon: const Icon(Icons.restore, size: 18),
            label: Text(ocupado ? 'Reactivando…' : 'Reactivar'),
          )
        : FilledButton(
            key: const Key('proveedor-guardar'),
            onPressed: ocupado ? null : onGuardar,
            style: _estiloPrincipal,
            child: Text(ocupado ? 'Guardando…' : (editando ? 'Guardar cambios' : 'Guardar proveedor')),
          );
    final desactivar = editando && !inactivo && onDesactivar != null
        ? TextButton.icon(
            key: const Key('proveedor-desactivar'),
            onPressed: ocupado ? null : onDesactivar,
            style: TextButton.styleFrom(foregroundColor: RegentaColors.crit),
            icon: const Icon(Icons.block, size: 18),
            label: const Text('Desactivar'),
          )
        : null;
    final botones = Row(children: [
      Expanded(
        child: SizedBox(
          height: RegentaSpacing.hitTarget,
          child: OutlinedButton(onPressed: ocupado ? null : onCancelar, child: const Text('Cancelar')),
        ),
      ),
      const SizedBox(width: 10),
      Expanded(child: SizedBox(height: RegentaSpacing.hitTarget, child: principal)),
    ]);
    return Container(
      padding: const EdgeInsets.fromLTRB(18, 12, 18, 14),
      decoration: const BoxDecoration(border: Border(top: BorderSide(color: RegentaColors.line))),
      // Si no hay espacio para los tres en una fila, «Desactivar» va arriba, solo.
      child: LayoutBuilder(builder: (context, c) {
        if (desactivar == null) return botones;
        if (c.maxWidth >= 600) {
          return Row(children: [desactivar, const SizedBox(width: 12), Expanded(child: botones)]);
        }
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          mainAxisSize: MainAxisSize.min,
          children: [desactivar, const SizedBox(height: 6), botones],
        );
      }),
    );
  }

  static final ButtonStyle _estiloPrincipal = FilledButton.styleFrom(
    backgroundColor: RegentaColors.accent,
    foregroundColor: RegentaColors.surface,
    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
  );
}
