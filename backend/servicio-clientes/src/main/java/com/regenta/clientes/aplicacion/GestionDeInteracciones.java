package com.regenta.clientes.aplicacion;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.clientes.domain.Cliente;
import com.regenta.clientes.domain.Interaccion;
import com.regenta.clientes.domain.TipoInteraccion;
import com.regenta.clientes.infra.ClienteRepositorio;
import com.regenta.clientes.infra.InteraccionRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Historial de interacciones del cliente. HU-024.
 *
 * <p>Registrar deja tipo, fecha, autor y detalle (criterio 1); la ficha las
 * lista de la mas reciente a la mas antigua (criterio 2); y el barrido de
 * seguimientos publica un recordatorio al responsable cuando llega la fecha
 * (criterio 3).
 */
@Service
public class GestionDeInteracciones {

    private static final int LOTE_DEL_BARRIDO = 200;

    private final InteraccionRepositorio interacciones;
    private final ClienteRepositorio clientes;
    private final RegistroDeEventos eventos;

    public GestionDeInteracciones(InteraccionRepositorio interacciones, ClienteRepositorio clientes,
            RegistroDeEventos eventos) {
        this.interacciones = interacciones;
        this.clientes = clientes;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CLIENTES_CLIENTE_VER")
    public List<InteraccionDelCliente> listar(UUID clienteId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        exigirCliente(clienteId, negocioId);
        return interacciones
                .findByNegocioIdAndClienteIdOrderByOcurridoEnDesc(negocioId, clienteId)
                .stream().map(InteraccionDelCliente::de).toList();
    }

    @Transactional
    @RequierePermiso("CLIENTES_CLIENTE_EDITAR")
    public InteraccionDelCliente registrar(UUID clienteId, SolicitudDeInteraccion solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        exigirCliente(clienteId, negocioId);

        Interaccion interaccion = Interaccion.registrar(negocioId, clienteId,
                ContextoDeNegocio.usuarioActual(), tipoDe(solicitud.tipo()),
                limpiar(solicitud.asunto()), limpiar(solicitud.detalle()), solicitud.ocurridoEn(),
                solicitud.seguimientoEn());
        interacciones.save(interaccion);
        return InteraccionDelCliente.de(interaccion);
    }

    /**
     * Criterio 3: recorre los seguimientos cuya fecha ya llego y no avisaron
     * todavia, publica un recordatorio para el responsable y los marca. Corre
     * dentro del negocio en contexto; el barrido multi-tenant programado se
     * conecta con el rol privilegiado (mismo diferido que el publicador de
     * outbox).
     *
     * @return cuantos recordatorios se publicaron
     */
    @Transactional
    public int procesarSeguimientosPendientes(LocalDate hoy) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<Interaccion> vencidos = interacciones.tomarSeguimientosVencidos(
                hoy == null ? LocalDate.now() : hoy, PageRequest.of(0, LOTE_DEL_BARRIDO));
        for (Interaccion interaccion : vencidos) {
            eventos.registrar(negocioId, "interaccion", interaccion.getId(),
                    "recordatorio_de_seguimiento", recordatorio(negocioId, interaccion));
            interaccion.marcarSeguimientoNotificado();
        }
        return vencidos.size();
    }

    private void exigirCliente(UUID clienteId, UUID negocioId) {
        Cliente cliente = clientes.findByIdAndEliminadoEnIsNull(clienteId)
                .orElseThrow(() -> new NoEncontradoException("Ese cliente no existe"));
        if (!cliente.getNegocioId().equals(negocioId)) {
            throw new NoEncontradoException("Ese cliente no existe");
        }
    }

    private static TipoInteraccion tipoDe(String texto) {
        try {
            return TipoInteraccion.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException noExiste) {
            throw new ReglaDeNegocioException("Tipo de interaccion no valido: " + texto);
        }
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String recortado = valor.trim();
        return recortado.isEmpty() ? null : recortado;
    }

    private static Map<String, Object> recordatorio(UUID negocioId, Interaccion i) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("cliente_id", i.getClienteId().toString());
        payload.put("interaccion_id", i.getId().toString());
        payload.put("responsable_id", i.getUsuarioId() == null ? null : i.getUsuarioId().toString());
        payload.put("tipo", i.getTipo().name());
        payload.put("asunto", i.getAsunto());
        payload.put("seguimiento_en", i.getSeguimientoEn().toString());
        return payload;
    }
}
