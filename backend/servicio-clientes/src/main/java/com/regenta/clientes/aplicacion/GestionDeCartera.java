package com.regenta.clientes.aplicacion;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.clientes.domain.Cliente;
import com.regenta.clientes.domain.CuentaPorCobrar;
import com.regenta.clientes.domain.MetodoDeRecaudo;
import com.regenta.clientes.domain.OrigenDeCuenta;
import com.regenta.clientes.domain.Recaudo;
import com.regenta.clientes.infra.ClienteRepositorio;
import com.regenta.clientes.infra.CuentaPorCobrarRepositorio;
import com.regenta.clientes.infra.RecaudoRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Cupo de crédito y cartera del cliente. HU-022.
 *
 * <p>El administrador fija el cupo; una venta a plazo (evento
 * {@code venta_a_credito}) abre una cuenta por cobrar y sube el saldo del
 * cliente; los recaudos lo bajan. La API avisa antes de vender a crédito si la
 * operación excede el cupo (criterio 1).
 */
@Service
public class GestionDeCartera {

    private final ClienteRepositorio clientes;
    private final CuentaPorCobrarRepositorio cuentas;
    private final RecaudoRepositorio recaudos;

    public GestionDeCartera(ClienteRepositorio clientes, CuentaPorCobrarRepositorio cuentas,
            RecaudoRepositorio recaudos) {
        this.clientes = clientes;
        this.cuentas = cuentas;
        this.recaudos = recaudos;
    }

    @Transactional
    @RequierePermiso("CLIENTES_CLIENTE_EDITAR")
    public CarteraDelCliente configurarCredito(UUID clienteId, SolicitudDeCredito solicitud) {
        Cliente cliente = exigirCliente(clienteId);
        cliente.configurarCredito(solicitud.habilitado(),
                solicitud.cupo() == null ? BigDecimal.ZERO : solicitud.cupo(),
                solicitud.diasCredito() == null ? 0 : solicitud.diasCredito());
        clientes.save(cliente);
        return cartera(cliente);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CLIENTES_CARTERA_VER")
    public CarteraDelCliente verCartera(UUID clienteId) {
        return cartera(exigirCliente(clienteId));
    }

    /** Criterio 1: ¿cabe una venta a crédito de {@code monto} en el cupo? */
    @Transactional(readOnly = true)
    @RequierePermiso("CLIENTES_CARTERA_VER")
    public ResultadoDeCupo validarCupo(UUID clienteId, BigDecimal monto) {
        if (monto == null || monto.signum() <= 0) {
            throw new ReglaDeNegocioException("El monto a validar debe ser mayor que cero");
        }
        Cliente cliente = exigirCliente(clienteId);
        BigDecimal cupo = cliente.getCupoCredito();
        BigDecimal saldo = cliente.getSaldoPendiente();
        BigDecimal disponible = cliente.getCupoDisponible();

        if (!cliente.isCreditoHabilitado()) {
            return new ResultadoDeCupo(false, cupo, saldo, BigDecimal.ZERO,
                    "El cliente no tiene credito habilitado");
        }
        if (monto.compareTo(disponible) > 0) {
            return new ResultadoDeCupo(false, cupo, saldo, disponible,
                    "La venta a credito de " + monto + " excede el cupo: disponible "
                            + disponible + " de " + cupo);
        }
        return new ResultadoDeCupo(true, cupo, saldo, disponible, null);
    }

    /**
     * Aplica un recaudo a una cuenta: baja su saldo (criterio 3), la deja
     * {@code PAGADA} si lo cubre (criterio 4), y ajusta el saldo del cliente.
     */
    @Transactional
    @RequierePermiso("CLIENTES_CLIENTE_EDITAR")
    public CuentaEnCartera registrarRecaudo(UUID cuentaId, SolicitudDeRecaudo solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        CuentaPorCobrar cuenta = cuentas.findByIdAndNegocioId(cuentaId, negocioId)
                .orElseThrow(() -> new NoEncontradoException("Esa cuenta por cobrar no existe"));

        cuenta.registrarRecaudo(solicitud.monto());
        cuentas.save(cuenta);
        recaudos.save(Recaudo.de(negocioId, cuentaId, solicitud.monto(),
                valorDe(MetodoDeRecaudo.class, solicitud.metodo(), MetodoDeRecaudo.EFECTIVO),
                limpiar(solicitud.referencia()), ContextoDeNegocio.usuarioActual()));

        Cliente cliente = exigirCliente(cuenta.getClienteId());
        cliente.ajustarSaldoPendiente(solicitud.monto().negate());
        clientes.save(cliente);

        return CuentaEnCartera.de(cuenta, LocalDate.now());
    }

    /**
     * Abre la cuenta por cobrar de una venta a crédito. Lo llama el consumidor
     * de {@code venta_a_credito}; sin {@code @RequierePermiso} porque lo dispara
     * un evento ya validado por Ventas. Idempotente por {@code (origen_tipo,
     * origen_id)} además del Inbox.
     */
    @Transactional
    public void abrirCuentaPorVentaACredito(EventoDeCredito evento) {
        UUID negocioId = evento.negocioId();
        if (cuentas.existsByNegocioIdAndOrigenTipoAndOrigenId(
                negocioId, OrigenDeCuenta.VENTA, evento.ventaId())) {
            return;
        }
        Cliente cliente = clientes.findByIdAndEliminadoEnIsNull(evento.clienteId()).orElse(null);
        if (cliente == null || !cliente.getNegocioId().equals(negocioId)) {
            return;   // cliente de otro negocio o inexistente (RLS)
        }
        cuentas.save(CuentaPorCobrar.crear(negocioId, evento.clienteId(), OrigenDeCuenta.VENTA,
                evento.ventaId(), evento.numero(), evento.monto(), LocalDate.now(),
                evento.fechaVencimiento()));
        cliente.ajustarSaldoPendiente(evento.monto());
        clientes.save(cliente);
    }

    private CarteraDelCliente cartera(Cliente cliente) {
        LocalDate hoy = LocalDate.now();
        List<CuentaEnCartera> lista = cuentas
                .findByNegocioIdAndClienteIdOrderByFechaVencimientoAsc(
                        cliente.getNegocioId(), cliente.getId())
                .stream().map(c -> CuentaEnCartera.de(c, hoy)).toList();
        return new CarteraDelCliente(cliente.getId(), cliente.isCreditoHabilitado(),
                cliente.getCupoCredito(), cliente.getSaldoPendiente(), cliente.getCupoDisponible(),
                cliente.getDiasCredito(), lista);
    }

    private Cliente exigirCliente(UUID clienteId) {
        Cliente cliente = clientes.findByIdAndEliminadoEnIsNull(clienteId)
                .orElseThrow(() -> new NoEncontradoException("Ese cliente no existe"));
        if (!cliente.getNegocioId().equals(ContextoDeNegocio.negocioActual())) {
            throw new NoEncontradoException("Ese cliente no existe");
        }
        return cliente;
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private static <E extends Enum<E>> E valorDe(Class<E> tipo, String texto, E porDefecto) {
        if (texto == null || texto.isBlank()) {
            return porDefecto;
        }
        try {
            return Enum.valueOf(tipo, texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Valor no valido: " + texto);
        }
    }

    /** Lo que la cartera necesita del evento {@code venta_a_credito}. */
    public record EventoDeCredito(UUID negocioId, UUID clienteId, UUID ventaId, String numero,
            BigDecimal monto, LocalDate fechaVencimiento) {

        public static EventoDeCredito desde(Map<String, Object> payload) {
            return new EventoDeCredito(
                    uuid(payload.get("negocio_id")),
                    uuid(payload.get("cliente_id")),
                    uuid(payload.get("venta_id")),
                    (String) payload.get("numero"),
                    new BigDecimal(String.valueOf(payload.get("monto"))),
                    LocalDate.parse(String.valueOf(payload.get("fecha_vencimiento"))));
        }

        private static UUID uuid(Object valor) {
            return valor == null ? null : UUID.fromString(valor.toString());
        }
    }
}
