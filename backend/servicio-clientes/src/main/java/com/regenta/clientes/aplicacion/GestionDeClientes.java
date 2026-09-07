package com.regenta.clientes.aplicacion;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.clientes.domain.Cliente;
import com.regenta.clientes.domain.TipoDocumento;
import com.regenta.clientes.domain.TipoPersona;
import com.regenta.clientes.infra.ClienteRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Alta y consulta de clientes. HU-021.
 *
 * <p>La identidad es el par {@code (tipo_documento, numero_documento)}, unico por
 * negocio (criterio 1) salvo {@code SIN_IDENTIFICAR}, el «consumidor final», que
 * se repite (criterio 4). Una JURIDICA sin razon social se rechaza con 422
 * (criterio 2). La busqueda por nombre parcial se apoya en el indice trigram
 * (criterio 3).
 */
@Service
public class GestionDeClientes {

    private static final int LIMITE_POR_DEFECTO = 50;
    private static final int MINIMO_PARA_BUSCAR = 3;

    private final ClienteRepositorio clientes;
    private final RegistroDeEventos eventos;

    public GestionDeClientes(ClienteRepositorio clientes, RegistroDeEventos eventos) {
        this.clientes = clientes;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CLIENTES_CLIENTE_VER")
    public List<ClienteDelNegocio> listar() {
        return clientes
                .findByNegocioIdAndEliminadoEnIsNullOrderByNombreDisplayAsc(
                        ContextoDeNegocio.negocioActual())
                .stream().map(ClienteDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CLIENTES_CLIENTE_VER")
    public List<ClienteDelNegocio> buscar(String termino) {
        String q = termino == null ? "" : termino.trim();
        if (q.isBlank()) {
            return listar();
        }
        if (q.length() < MINIMO_PARA_BUSCAR) {
            throw new ReglaDeNegocioException("Escribe al menos tres caracteres para buscar");
        }
        return clientes.buscar("%" + q + "%", PageRequest.of(0, LIMITE_POR_DEFECTO))
                .stream().map(ClienteDelNegocio::de).toList();
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CLIENTES_CLIENTE_VER")
    public ClienteDelNegocio ver(UUID clienteId) {
        return ClienteDelNegocio.de(delNegocio(clienteId));
    }

    @Transactional
    @RequierePermiso("CLIENTES_CLIENTE_CREAR")
    public ClienteDelNegocio crear(SolicitudDeCliente solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuarioId = ContextoDeNegocio.usuarioActual();

        TipoPersona persona = valorDe(TipoPersona.class, solicitud.tipoPersona(),
                TipoPersona.NATURAL);
        TipoDocumento documento = valorDe(TipoDocumento.class, solicitud.tipoDocumento(),
                TipoDocumento.CC);

        String nombres = limpiar(solicitud.nombres());
        String apellidos = limpiar(solicitud.apellidos());
        String razonSocial = limpiar(solicitud.razonSocial());
        String numeroDocumento = limpiar(solicitud.numeroDocumento());
        String digitoVerificacion = limpiar(solicitud.digitoVerificacion());

        if (documento == TipoDocumento.SIN_IDENTIFICAR) {
            numeroDocumento = null;
            digitoVerificacion = null;
            if (nombres == null && razonSocial == null) {
                nombres = Cliente.CONSUMIDOR_FINAL;
            }
        }

        if (persona == TipoPersona.JURIDICA && razonSocial == null) {
            throw new ReglaDeNegocioException(
                    "La razon social es obligatoria para una persona juridica");
        }
        if (persona == TipoPersona.NATURAL && documento != TipoDocumento.SIN_IDENTIFICAR
                && nombres == null) {
            throw new ReglaDeNegocioException(
                    "Los nombres son obligatorios para una persona natural");
        }

        if (numeroDocumento != null && documento != TipoDocumento.SIN_IDENTIFICAR
                && clientes.existsByNegocioIdAndTipoDocumentoAndNumeroDocumento(
                        negocioId, documento, numeroDocumento)) {
            throw new RecursoDuplicadoException("Ya hay un cliente con el documento "
                    + documento + " " + numeroDocumento);
        }

        Cliente cliente = Cliente.crear(negocioId, usuarioId, persona, documento, numeroDocumento,
                digitoVerificacion, nombres, apellidos, razonSocial, limpiar(solicitud.email()),
                limpiar(solicitud.telefono()), limpiar(solicitud.telefonoAlterno()),
                limpiar(solicitud.segmento()), limpiar(solicitud.notas()));
        clientes.save(cliente);

        eventos.registrar(negocioId, "cliente", cliente.getId(), "cliente_creado",
                datos(negocioId, cliente));
        return ClienteDelNegocio.de(cliente);
    }

    @Transactional
    @RequierePermiso("CLIENTES_CLIENTE_EDITAR")
    public ClienteDelNegocio actualizar(UUID clienteId, SolicitudDeCliente solicitud) {
        Cliente cliente = delNegocio(clienteId);
        String razonSocial = limpiar(solicitud.razonSocial());
        String nombres = limpiar(solicitud.nombres());
        if (cliente.getTipoPersona() == TipoPersona.JURIDICA && razonSocial == null) {
            throw new ReglaDeNegocioException(
                    "La razon social es obligatoria para una persona juridica");
        }
        if (cliente.getTipoPersona() == TipoPersona.NATURAL
                && cliente.getTipoDocumento() != TipoDocumento.SIN_IDENTIFICAR && nombres == null) {
            throw new ReglaDeNegocioException(
                    "Los nombres son obligatorios para una persona natural");
        }
        cliente.editar(nombres, limpiar(solicitud.apellidos()), razonSocial,
                limpiar(solicitud.email()), limpiar(solicitud.telefono()),
                limpiar(solicitud.telefonoAlterno()), limpiar(solicitud.segmento()),
                limpiar(solicitud.notas()), ContextoDeNegocio.usuarioActual());
        clientes.save(cliente);
        return ClienteDelNegocio.de(cliente);
    }

    private Cliente delNegocio(UUID clienteId) {
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

    private static Map<String, Object> datos(UUID negocioId, Cliente cliente) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("cliente_id", cliente.getId().toString());
        payload.put("tipo_persona", cliente.getTipoPersona().name());
        payload.put("tipo_documento", cliente.getTipoDocumento().name());
        payload.put("numero_documento", cliente.getNumeroDocumento());
        payload.put("nombre_display", cliente.getNombreDisplay());
        return payload;
    }
}
