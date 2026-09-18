import 'package:flutter/foundation.dart';
import 'package:regenta_core/regenta_core.dart';
import 'package:workmanager/workmanager.dart';

import 'dependencias.dart';

/// El trabajo de fondo de la cola de sincronización (HU-120).
///
/// HU-111 dejó la cola, el reintento con espera creciente y el registro de la
/// tarea periódica. Lo que faltaba es esto: el isolate que `workmanager`
/// levanta **arranca en frío** —no hereda el `Dio` autenticado, ni la sesión,
/// ni la base abierta del isolate principal— así que tiene que armárselo todo
/// otra vez. Solo la carcasa sabe con qué URL y con qué sesión guardada.

/// Lo que ejecuta `workmanager` cuando llega el momento.
///
/// Tiene que ser una función de nivel superior y anotada con
/// `vm:entry-point`: es el nombre por el que el motor de Dart la encuentra
/// desde el lado nativo, y sin la anotación el compilador de release la borra
/// por no ver quién la llama.
@pragma('vm:entry-point')
void despachadorDeTareas() {
  Workmanager().executeTask((tarea, datos) async {
    if (tarea != tareaDeSincronizacion) return true;
    final dependencias = DependenciasDeLaApp.crear(
      urlDelGateway: datos?['gateway'] as String? ?? '',
    );
    try {
      return await subirLoPendiente(dependencias);
    } finally {
      await dependencias.cerrar();
    }
  });
}

/// Una pasada de la cola con la sesión que haya guardada.
///
/// Devuelve si la tarea se puede dar por terminada. Devolver `false` hace que
/// el sistema la reintente, y eso solo tiene sentido cuando el fallo es de red:
/// que no haya sesión no es un fallo, y un token muerto no mejora por insistir.
Future<bool> subirLoPendiente(DependenciasDeLaApp dependencias) async {
  // Criterio 2: lee la sesión del almacén seguro y, si el acceso venció, la
  // refresca. Si el refresco no sirve, `iniciar` cierra y borra.
  await dependencias.motor.iniciar();

  // Criterio 5: sin sesión no se sube nada. Lo pendiente se queda para cuando
  // alguien entre; subirlo con un token muerto solo gastaría reintentos.
  if (dependencias.motor.sesion == null) return true;

  final trabajador = TrabajadorDeSincronizacion(
    dio: dependencias.dio,
    db: dependencias.base,
  );
  await trabajador.ejecutarUnaPasada();
  // Lo que falló por red ya quedó con su próximo intento anotado en la cola:
  // la siguiente pasada lo toma. No hace falta que el sistema reintente la
  // tarea entera.
  return true;
}

/// Dónde existe el trabajo en segundo plano de verdad.
///
/// En Web no hay: la cola se procesa al abrir la app (criterio 4). En
/// escritorio `workmanager` tampoco aplica.
bool hayTrabajoEnSegundoPlano({required bool esWeb, required String plataforma}) {
  if (esWeb) return false;
  return plataforma == 'android' || plataforma == 'ios';
}

/// Lo mismo, leyendo la plataforma real.
bool get hayTrabajoEnSegundoPlanoAqui => hayTrabajoEnSegundoPlano(
      esWeb: kIsWeb,
      plataforma: defaultTargetPlatform.name.toLowerCase(),
    );
