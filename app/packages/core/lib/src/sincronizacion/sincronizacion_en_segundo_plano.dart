import 'package:workmanager/workmanager.dart';

// ignore: unused_import
import '../local/base_local.dart';
// ignore: unused_import
import 'cola_de_salida_local.dart';
// ignore: unused_import
import 'trabajador_de_sincronizacion.dart';

/// El identificador de la tarea periodica ante Android/`workmanager`.
const String tareaDeSincronizacion = 'regenta.sincronizacion';

/// HU-111 criterio 1: registra la tarea para que `workmanager` dispare la
/// subida sola en cuanto detecte red, sin que nadie tenga que abrir la app.
/// Criterio 5: por eso mismo sigue funcionando con la app cerrada en Android
/// —es justo lo que resuelve `workmanager`, a diferencia de un timer en Dart
/// que muere con el proceso.
///
/// Solo registra la tarea; no decide qué hace. Eso lo define el
/// `callbackDispatcher` que se le pasa a `Workmanager().initialize(...)`
/// —una función top-level de la propia app, anotada con
/// `@pragma('vm:entry-point')`—, porque un isolate de fondo arranca desde
/// cero: no hereda nada de lo que este paquete construyó en el isolate
/// principal (el `Dio` autenticado, la sesión, la base local abierta). Este
/// paquete pone las piezas —[TrabajadorDeSincronizacion], [ColaDeSalidaLocal],
/// [BaseLocal]— para que ese `callbackDispatcher` las arme; no puede armarlas
/// él mismo porque no sabe con qué URL de backend ni con qué sesión guardada
/// tiene que hacerlo.
///
/// `workmanager` es Android/iOS —no existe segundo plano real en Web—: por
/// eso el criterio 5 dice explícitamente "en Android".
Future<void> registrarSincronizacionEnSegundoPlano({
  Duration frecuencia = const Duration(minutes: 15),
}) {
  return Workmanager().registerPeriodicTask(
    tareaDeSincronizacion,
    tareaDeSincronizacion,
    frequency: frecuencia,
    constraints: Constraints(networkType: NetworkType.connected),
    existingWorkPolicy: ExistingWorkPolicy.keep,
    backoffPolicy: BackoffPolicy.exponential,
    backoffPolicyDelay: const Duration(seconds: 30),
  );
}

/// Para cuando la sesión se cierra: no tiene sentido seguir subiendo la cola
/// de un usuario que ya no está.
Future<void> cancelarSincronizacionEnSegundoPlano() {
  return Workmanager().cancelByUniqueName(tareaDeSincronizacion);
}
