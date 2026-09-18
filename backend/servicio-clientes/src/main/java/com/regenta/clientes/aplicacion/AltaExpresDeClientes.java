package com.regenta.clientes.aplicacion;

import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.clientes.domain.Cliente;
import com.regenta.clientes.domain.DigitoDeVerificacion;
import com.regenta.clientes.domain.TipoDocumento;
import com.regenta.clientes.domain.TipoPersona;
import com.regenta.clientes.infra.ClienteRepositorio;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;

/**
 * Crear el cliente en el mostrador, con lo mínimo para facturar (HU-114).
 *
 * <p>Es un caso de uso aparte de {@link GestionDeClientes#crear} a propósito:
 * el alta completa admite un consumidor final sin documento y sin correo,
 * porque sirve para cargar la base de a poco. Esta no: se está creando para
 * facturarle a alguien que está esperando en el mostrador, y una factura sin
 * documento o sin correo no sirve. Lo que sobra —teléfono, segmento, notas— se
 * completa después en la ficha.
 */
@Service
public class AltaExpresDeClientes {

    private final ClienteRepositorio clientes;
    private final RegistroDeEventos eventos;

    public AltaExpresDeClientes(ClienteRepositorio clientes, RegistroDeEventos eventos) {
        this.clientes = clientes;
        this.eventos = eventos;
    }

    @Transactional
    @RequierePermiso("CLIENTES_CLIENTE_CREAR")
    public ClienteDelNegocio crear(SolicitudExpres solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID usuarioId = ContextoDeNegocio.usuarioActual();

        TipoDocumento documento = documentoDe(solicitud.tipoDocumento());
        TipoPersona persona = personaDe(solicitud.tipoPersona());
        String numeroDocumento = limpiar(solicitud.numeroDocumento());
        String razonSocial = limpiar(solicitud.razonSocial());
        String nombres = limpiar(solicitud.nombres());
        String apellidos = limpiar(solicitud.apellidos());
        String email = limpiar(solicitud.email());

        exigirLoMinimoParaFacturar(documento, persona, numeroDocumento, nombres, razonSocial, email);

        // Criterio 5: la cola de sincronización reintenta. Si ya subió este
        // mismo cliente, el segundo intento devuelve el que hay, no un 409.
        if (solicitud.id() != null) {
            Optional<Cliente> yaSubido = clientes.findByIdAndEliminadoEnIsNull(solicitud.id());
            if (yaSubido.isPresent()) {
                return ClienteDelNegocio.de(yaSubido.get());
            }
        }
        exigirDocumentoLibre(negocioId, documento, numeroDocumento);

        Cliente cliente = Cliente.crear(negocioId, usuarioId, persona, documento, numeroDocumento,
                digitoDe(documento, numeroDocumento, limpiar(solicitud.digitoVerificacion())),
                nombres, apellidos, razonSocial, email, null, null, null, null);
        cliente.conElIdDelDispositivo(solicitud.id());
        clientes.save(cliente);

        eventos.registrar(negocioId, "cliente", cliente.getId(), "cliente_creado",
                GestionDeClientes.datos(negocioId, cliente));
        return ClienteDelNegocio.de(cliente);
    }

    /**
     * Criterio 2: el 409 lleva el id del que ya existe. Sin eso, la pantalla
     * solo puede decir "ya existe" y dejar a la persona buscándolo a mano con
     * el cliente enfrente.
     */
    private void exigirDocumentoLibre(UUID negocioId, TipoDocumento documento,
            String numeroDocumento) {
        clientes.findByNegocioIdAndTipoDocumentoAndNumeroDocumento(negocioId, documento,
                numeroDocumento).ifPresent(yaExiste -> {
                    throw new RecursoDuplicadoException("Ya hay un cliente con el documento "
                            + documento + " " + numeroDocumento + ": " + yaExiste.getNombreDisplay())
                            .conDato("cliente_id", yaExiste.getId().toString());
                });
    }

    private static void exigirLoMinimoParaFacturar(TipoDocumento documento, TipoPersona persona,
            String numeroDocumento, String nombres, String razonSocial, String email) {
        if (documento == TipoDocumento.SIN_IDENTIFICAR) {
            throw new ReglaDeNegocioException("Para facturar hace falta el documento; "
                    + "si no lo tiene, la venta va a consumidor final sin crear cliente");
        }
        if (numeroDocumento == null) {
            throw new ReglaDeNegocioException("Falta el numero de documento");
        }
        if (persona == TipoPersona.JURIDICA && razonSocial == null) {
            throw new ReglaDeNegocioException("Falta la razon social");
        }
        if (persona == TipoPersona.NATURAL && nombres == null) {
            throw new ReglaDeNegocioException("Faltan los nombres");
        }
        if (email == null) {
            throw new ReglaDeNegocioException(
                    "Falta el correo: es donde llega la factura electronica");
        }
    }

    /** Criterio 3: se calcula solo, y lo escrito a mano manda sobre la fórmula. */
    private static String digitoDe(TipoDocumento documento, String numeroDocumento,
            String escritoAMano) {
        if (documento != TipoDocumento.NIT) {
            return null;
        }
        return escritoAMano != null ? escritoAMano : DigitoDeVerificacion.de(numeroDocumento);
    }

    private static TipoDocumento documentoDe(String texto) {
        try {
            return TipoDocumento.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException | NullPointerException noExiste) {
            throw new ReglaDeNegocioException("Tipo de documento no valido: " + texto);
        }
    }

    private static TipoPersona personaDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return TipoPersona.NATURAL;
        }
        try {
            return TipoPersona.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Tipo de persona no valido: " + texto);
        }
    }

    private static String limpiar(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }
}
