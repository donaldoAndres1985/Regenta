import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:regenta_core/regenta_core.dart';

import '../arranque/proveedores.dart';

/// La puerta. HU-119 criterio 1, reglas R1 a R6 de
/// `design/comportamiento/Login.md`.
///
/// El negocio no se escribe: sale del correo. Cuando el mismo correo trabaja en
/// más de uno, el API devuelve la lista y aquí se elige, sin volver a pedir la
/// contraseña.
class PantallaLogin extends ConsumerStatefulWidget {
  const PantallaLogin({super.key});

  @override
  ConsumerState<PantallaLogin> createState() => _PantallaLoginState();
}

class _PantallaLoginState extends ConsumerState<PantallaLogin> {
  final TextEditingController _correo = TextEditingController();
  final TextEditingController _clave = TextEditingController();

  bool _mantenerSesion = true;
  bool _entrando = false;
  String? _error;

  /// R2: cuando el correo trabaja en varios negocios, hay que elegir.
  List<NegocioParaElegir>? _negocios;

  @override
  void dispose() {
    _correo.dispose();
    _clave.dispose();
    super.dispose();
  }

  bool get _sePuedeEntrar =>
      !_entrando && _correo.text.trim().isNotEmpty && _clave.text.isNotEmpty;

  Future<void> _entrar({String? negocioId}) async {
    if (_entrando) return;
    if (negocioId == null && !_sePuedeEntrar) return;
    setState(() {
      _entrando = true;
      _error = null;
    });
    try {
      await ref.read(motorDeSesionProvider).entrar(Credenciales(
            email: _correo.text.trim(),
            password: _clave.text,
            negocioId: negocioId,
          ));
      // Entrar cambia el perfil y el enrutador se lleva la pantalla; no hay
      // nada más que hacer aquí.
    } on DebeElegirNegocio catch (elegir) {
      setState(() => _negocios = elegir.negocios);
    } on CuentaBloqueada {
      setState(() => _error =
          'La cuenta está bloqueada por intentos fallidos. Un administrador del negocio puede desbloquearla.');
    } on CredencialesInvalidas {
      // R3: el mismo mensaje si el correo no existe que si la clave está mal.
      setState(() => _error = 'Correo o contraseña incorrectos');
      _clave.clear();
    } catch (fallo) {
      setState(() => _error = 'No se pudo entrar: revisa tu conexión e intenta de nuevo.');
    } finally {
      if (mounted) setState(() => _entrando = false);
    }
  }

  void _explicarLaClaveOlvidada() {
    ScaffoldMessenger.of(context).showSnackBar(const SnackBar(
      content: Text('Pídele a un administrador de tu negocio que te reenvíe la invitación.'),
    ));
  }

  @override
  Widget build(BuildContext context) {
    final negocios = _negocios;
    return Scaffold(
      backgroundColor: RegentaColors.paper,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 380),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const _Marca(),
                  const SizedBox(height: 28),
                  if (negocios != null)
                    _ElegirNegocio(
                      negocios: negocios,
                      trabajando: _entrando,
                      onElegir: (n) => _entrar(negocioId: n.negocioId),
                    )
                  else
                    _Formulario(
                      correo: _correo,
                      clave: _clave,
                      entrando: _entrando,
                      mantenerSesion: _mantenerSesion,
                      onMantenerSesion: (v) => setState(() => _mantenerSesion = v),
                      onCambio: () => setState(() {}),
                      onEntrar: _sePuedeEntrar ? _entrar : null,
                      onClaveOlvidada: _explicarLaClaveOlvidada,
                    ),
                  if (_error != null) ...[
                    const SizedBox(height: 14),
                    Text(_error!,
                        textAlign: TextAlign.center,
                        style: RegentaType.cuerpo
                            .copyWith(fontSize: 13, color: RegentaColors.crit)),
                  ],
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _Marca extends StatelessWidget {
  const _Marca();

  @override
  Widget build(BuildContext context) {
    return Column(children: [
      Container(
        width: 52,
        height: 52,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: RegentaColors.accent,
          borderRadius: BorderRadius.circular(RegentaSpacing.radiusCard),
        ),
        child: Text('R',
            style: RegentaType.tituloPantalla
                .copyWith(fontSize: 26, color: RegentaColors.surface)),
      ),
      const SizedBox(height: 14),
      Text('Regenta',
          style: RegentaType.tituloPantalla.copyWith(fontSize: 22, color: RegentaColors.ink)),
      const SizedBox(height: 4),
      Text('Lleva el control de tu negocio\ndesde el celular y la web.',
          textAlign: TextAlign.center,
          style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink2)),
    ]);
  }
}

class _Formulario extends StatelessWidget {
  const _Formulario({
    required this.correo,
    required this.clave,
    required this.entrando,
    required this.mantenerSesion,
    required this.onMantenerSesion,
    required this.onCambio,
    required this.onEntrar,
    required this.onClaveOlvidada,
  });

  final TextEditingController correo;
  final TextEditingController clave;
  final bool entrando;
  final bool mantenerSesion;
  final ValueChanged<bool> onMantenerSesion;
  final VoidCallback onCambio;
  final VoidCallback? onEntrar;
  final VoidCallback onClaveOlvidada;

  @override
  Widget build(BuildContext context) {
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      _Etiqueta('Correo'),
      TextField(
        key: const Key('login-correo'),
        controller: correo,
        autofocus: true,
        keyboardType: TextInputType.emailAddress,
        textInputAction: TextInputAction.next,
        onChanged: (_) => onCambio(),
        decoration: _decoracion('donaldo@eltornillo.co'),
      ),
      const SizedBox(height: 14),
      _Etiqueta('Contraseña'),
      TextField(
        key: const Key('login-clave'),
        controller: clave,
        obscureText: true,
        textInputAction: TextInputAction.done,
        onChanged: (_) => onCambio(),
        onSubmitted: (_) => onEntrar?.call(),
        decoration: _decoracion('••••••••••'),
      ),
      const SizedBox(height: 12),
      Row(children: [
        // R5: dónde vive el token de refresco.
        Semantics(
          label: 'Mantener sesión',
          child: Checkbox(
            value: mantenerSesion,
            activeColor: RegentaColors.accent,
            onChanged: (v) => onMantenerSesion(v ?? false),
          ),
        ),
        Expanded(
          child: Text('Mantener sesión',
              style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink2)),
        ),
        TextButton(
          onPressed: onClaveOlvidada,
          child: Text('¿Olvidaste tu clave?',
              style: RegentaType.cuerpo.copyWith(fontSize: 12.5, color: RegentaColors.accent)),
        ),
      ]),
      const SizedBox(height: 14),
      SizedBox(
        height: 52,
        child: FilledButton(
          key: const Key('login-entrar'),
          onPressed: onEntrar,
          style: FilledButton.styleFrom(
            backgroundColor: RegentaColors.accent,
            foregroundColor: RegentaColors.surface,
            disabledBackgroundColor: RegentaColors.line2,
            shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(RegentaSpacing.radius)),
          ),
          child: entrando
              ? const SizedBox(
                  width: 20,
                  height: 20,
                  child: CircularProgressIndicator(
                      strokeWidth: 2, color: RegentaColors.surface))
              : Text('Entrar',
                  style: RegentaType.item.copyWith(fontSize: 15, color: RegentaColors.surface)),
        ),
      ),
    ]);
  }

  static InputDecoration _decoracion(String pista) => InputDecoration(
        isDense: true,
        hintText: pista,
        hintStyle: RegentaType.cuerpo.copyWith(fontSize: 14, color: RegentaColors.faint),
        contentPadding: const EdgeInsets.symmetric(horizontal: 12, vertical: 14),
        border: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RegentaSpacing.radius),
          borderSide: const BorderSide(color: RegentaColors.line2),
        ),
        enabledBorder: OutlineInputBorder(
          borderRadius: BorderRadius.circular(RegentaSpacing.radius),
          borderSide: const BorderSide(color: RegentaColors.line2),
        ),
      );
}

/// R2. No se vuelve a pedir la contraseña: ya se escribió.
class _ElegirNegocio extends StatelessWidget {
  const _ElegirNegocio({
    required this.negocios,
    required this.trabajando,
    required this.onElegir,
  });

  final List<NegocioParaElegir> negocios;
  final bool trabajando;
  final void Function(NegocioParaElegir) onElegir;

  @override
  Widget build(BuildContext context) {
    return Column(crossAxisAlignment: CrossAxisAlignment.stretch, children: [
      Text('Tu correo trabaja en más de un negocio',
          style: RegentaType.seccion.copyWith(fontSize: 15, color: RegentaColors.ink)),
      const SizedBox(height: 4),
      Text('Elige en cuál quieres entrar.',
          style: RegentaType.cuerpo.copyWith(fontSize: 13, color: RegentaColors.ink2)),
      const SizedBox(height: 14),
      for (final negocio in negocios)
        Padding(
          padding: const EdgeInsets.only(bottom: 8),
          child: InkWell(
            onTap: trabajando ? null : () => onElegir(negocio),
            child: Container(
              constraints: const BoxConstraints(minHeight: RegentaSpacing.hitTarget),
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 12),
              decoration: BoxDecoration(
                color: RegentaColors.surface,
                border: Border.all(color: RegentaColors.line2),
                borderRadius: BorderRadius.circular(RegentaSpacing.radius),
              ),
              child: Row(children: [
                Expanded(
                  child: Text(negocio.nombreComercial,
                      style: RegentaType.item
                          .copyWith(fontSize: 14, color: RegentaColors.ink)),
                ),
                const Icon(Icons.chevron_right, size: 18, color: RegentaColors.faint),
              ]),
            ),
          ),
        ),
    ]);
  }
}

class _Etiqueta extends StatelessWidget {
  const _Etiqueta(this.texto);

  final String texto;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 5),
      child: Text(texto.toUpperCase(), style: RegentaType.etiqueta.copyWith(fontSize: 9.5)),
    );
  }
}
