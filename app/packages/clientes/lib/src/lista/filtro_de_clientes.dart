import '../datos/cliente_en_lista.dart';

/// Los filtros del listado (HU-025 criterio 3). Se aplican en memoria sobre lo
/// que ya está cargado: la lista responde al instante, sin pedir nada.
enum FiltroDeClientes {
  todos('Todos'),
  conSaldo('Con saldo'),
  vencidos('Vencidos'),
  mayoristas('Mayoristas');

  const FiltroDeClientes(this.etiqueta);

  final String etiqueta;

  bool admite(ClienteEnLista cliente) => switch (this) {
        FiltroDeClientes.todos => true,
        FiltroDeClientes.conSaldo => cliente.tieneSaldo,
        FiltroDeClientes.vencidos => cliente.vencido,
        FiltroDeClientes.mayoristas => cliente.esMayorista,
      };
}
