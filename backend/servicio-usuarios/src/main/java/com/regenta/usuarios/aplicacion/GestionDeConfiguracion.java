package com.regenta.usuarios.aplicacion;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.errores.RecursoDuplicadoException;
import com.regenta.comun.errores.ReglaDeNegocioException;
import com.regenta.comun.eventos.RegistroDeEventos;
import com.regenta.comun.negocio.ContextoDeNegocio;
import com.regenta.comun.negocio.RequierePermiso;
import com.regenta.usuarios.domain.ConfiguracionNegocio;
import com.regenta.usuarios.domain.Impuesto;
import com.regenta.usuarios.domain.Negocio;
import com.regenta.usuarios.infra.ConfiguracionNegocioRepositorio;
import com.regenta.usuarios.infra.ImpuestoRepositorio;
import com.regenta.usuarios.infra.NegocioRepositorio;

/**
 * Datos fiscales, impuestos y preferencias de operacion. HU-018.
 *
 * <p>El porcentaje de un impuesto no se cambia nunca: una factura de marzo
 * tiene que seguir cuadrando en diciembre. Se crea otro impuesto y se deja el
 * viejo inactivo. Este servicio no sabe si el impuesto se uso en algun
 * documento —eso vive en facturacion— asi que aplica la regla siempre, que es
 * la unica forma de que sea cierta.
 */
@Service
public class GestionDeConfiguracion {

    public static final String ADVERTENCIA_IMPUESTO_INCLUIDO =
            "Cambiar si los precios incluyen impuesto afecta el calculo de TODAS las ventas"
                    + " nuevas. Las ya emitidas se quedan como estan.";

    private final ConfiguracionNegocioRepositorio configuraciones;
    private final NegocioRepositorio negocios;
    private final ImpuestoRepositorio impuestos;
    private final RegistroDeEventos eventos;

    public GestionDeConfiguracion(ConfiguracionNegocioRepositorio configuraciones,
            NegocioRepositorio negocios, ImpuestoRepositorio impuestos,
            RegistroDeEventos eventos) {
        this.configuraciones = configuraciones;
        this.negocios = negocios;
        this.impuestos = impuestos;
        this.eventos = eventos;
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CONFIGURACION_NEGOCIO_VER")
    public ConfiguracionVigente ver() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        return comoDto(negocio(negocioId), configuracion(negocioId), null);
    }

    @Transactional
    @RequierePermiso("CONFIGURACION_NEGOCIO_EDITAR")
    public ConfiguracionVigente actualizar(CambioDeConfiguracion cambio) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Negocio negocio = negocio(negocioId);
        ConfiguracionNegocio configuracion = configuracion(negocioId);

        negocio.renombrar(cambio.nombreComercial(), cambio.razonSocial());
        negocio.corregirDocumento(cambio.tipoDocumento(), cambio.numeroDocumento(),
                cambio.digitoVerificacion());
        negocios.save(negocio);

        boolean cambiaElCalculo = cambio.preciosIncluyenImpuesto() != null
                && cambio.preciosIncluyenImpuesto() != configuracion.isPreciosIncluyenImpuesto();

        configuracion.actualizarContacto(cambio.direccion(), cambio.ciudad(),
                cambio.departamento(), cambio.telefono(), cambio.email(), cambio.sitioWeb(),
                cambio.codigoPostal(), cambio.logoUrl());
        configuracion.actualizarFiscal(cambio.regimenFiscal(), cambio.responsabilidadesFiscales());
        configuracion.actualizarOperacion(
                cambio.decimalesMoneda() == null ? configuracion.getDecimalesMoneda()
                        : cambio.decimalesMoneda(),
                cambio.formatoFecha() == null ? configuracion.getFormatoFecha()
                        : cambio.formatoFecha(),
                cambio.preciosIncluyenImpuesto() == null
                        ? configuracion.isPreciosIncluyenImpuesto()
                        : cambio.preciosIncluyenImpuesto(),
                cambio.politicaStockNegativo() == null ? configuracion.isPoliticaStockNegativo()
                        : cambio.politicaStockNegativo());
        if (cambio.preferencias() != null) {
            configuracion.guardarPreferencias(cambio.preferencias());
        }
        configuraciones.save(configuracion);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("nombre_comercial", negocio.getNombreComercial());
        payload.put("regimen_fiscal", configuracion.getRegimenFiscal());
        payload.put("responsabilidades_fiscales", configuracion.getResponsabilidadesFiscales());
        payload.put("precios_incluyen_impuesto", configuracion.isPreciosIncluyenImpuesto());
        eventos.registrar(negocioId, "negocio", negocioId, "configuracion_negocio_actualizada",
                payload);

        return comoDto(negocio, configuracion,
                cambiaElCalculo ? ADVERTENCIA_IMPUESTO_INCLUIDO : null);
    }

    @Transactional(readOnly = true)
    @RequierePermiso("CONFIGURACION_IMPUESTO_VER")
    public List<ImpuestoDelNegocio> impuestos() {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        UUID porDefecto = configuracion(negocioId).getImpuestoDefaultId();
        return impuestos.findByNegocioIdOrderByCodigo(negocioId).stream()
                .map(impuesto -> comoDto(impuesto, porDefecto))
                .toList();
    }

    @Transactional
    @RequierePermiso("CONFIGURACION_IMPUESTO_CREAR")
    public ImpuestoDelNegocio crearImpuesto(SolicitudDeImpuesto solicitud) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        if (impuestos.existsByNegocioIdAndCodigoAndPorcentaje(negocioId, solicitud.codigo(),
                solicitud.porcentaje())) {
            throw new RecursoDuplicadoException("Ya existe el impuesto " + solicitud.codigo()
                    + " al " + solicitud.porcentaje() + "%");
        }
        Impuesto impuesto = impuestos.save(Impuesto.nuevo(negocioId, solicitud.codigo(),
                solicitud.nombre(), solicitud.tipo(), solicitud.porcentaje(),
                solicitud.aplicaSobre()));
        return comoDto(impuesto, configuracion(negocioId).getImpuestoDefaultId());
    }

    /** Criterio 3: el nombre se corrige; el porcentaje, jamas. */
    @Transactional
    @RequierePermiso("CONFIGURACION_IMPUESTO_EDITAR")
    public ImpuestoDelNegocio renombrarImpuesto(UUID impuestoId, String nombre,
            BigDecimal porcentajePedido) {
        Impuesto impuesto = impuesto(impuestoId);
        if (porcentajePedido != null
                && porcentajePedido.compareTo(impuesto.getPorcentaje()) != 0) {
            throw new ReglaDeNegocioException("El porcentaje de un impuesto no se cambia: los"
                    + " documentos ya emitidos con el dejarian de cuadrar. Crea uno nuevo y deja"
                    + " este inactivo");
        }
        impuesto.renombrar(nombre);
        impuestos.save(impuesto);
        return comoDto(impuesto,
                configuracion(ContextoDeNegocio.negocioActual()).getImpuestoDefaultId());
    }

    @Transactional
    @RequierePermiso("CONFIGURACION_IMPUESTO_EDITAR")
    public void desactivarImpuesto(UUID impuestoId) {
        Impuesto impuesto = impuesto(impuestoId);
        impuesto.desactivar();
        impuestos.save(impuesto);
    }

    /** Criterio 2: el que quede por defecto es el que tomaran los productos nuevos. */
    @Transactional
    @RequierePermiso("CONFIGURACION_IMPUESTO_EDITAR")
    public ImpuestoDelNegocio marcarPorDefecto(UUID impuestoId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        Impuesto impuesto = impuesto(impuestoId);
        if (!impuesto.isActivo()) {
            throw new ReglaDeNegocioException("Un impuesto inactivo no puede ser el de por defecto");
        }
        ConfiguracionNegocio configuracion = configuracion(negocioId);
        configuracion.fijarImpuestoPorDefecto(impuestoId);
        configuraciones.save(configuracion);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("negocio_id", negocioId.toString());
        payload.put("impuesto_id", impuestoId.toString());
        payload.put("porcentaje", impuesto.getPorcentaje());
        eventos.registrar(negocioId, "negocio", negocioId, "impuesto_por_defecto_cambiado",
                payload);
        return comoDto(impuesto, impuestoId);
    }

    private Negocio negocio(UUID negocioId) {
        return negocios.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("El negocio no existe"));
    }

    private ConfiguracionNegocio configuracion(UUID negocioId) {
        return configuraciones.findById(negocioId)
                .orElseThrow(() -> new NoEncontradoException("El negocio no tiene configuracion"));
    }

    private Impuesto impuesto(UUID impuestoId) {
        return impuestos.findById(impuestoId)
                .orElseThrow(() -> new NoEncontradoException("Ese impuesto no existe"));
    }

    private static ImpuestoDelNegocio comoDto(Impuesto impuesto, UUID porDefecto) {
        return new ImpuestoDelNegocio(impuesto.getId(), impuesto.getCodigo(), impuesto.getNombre(),
                impuesto.getTipo(), impuesto.getPorcentaje(), impuesto.getAplicaSobre(),
                impuesto.isActivo(), impuesto.getId().equals(porDefecto));
    }

    private static ConfiguracionVigente comoDto(Negocio negocio, ConfiguracionNegocio conf,
            String advertencia) {
        return new ConfiguracionVigente(negocio.getId(), negocio.getNombreComercial(),
                negocio.getRazonSocial(), negocio.getTipoDocumento(), negocio.getNumeroDocumento(),
                negocio.getDigitoVerificacion(), conf.getDireccion(), conf.getCiudad(),
                conf.getDepartamento(), conf.getTelefono(), conf.getEmail(), conf.getSitioWeb(),
                conf.getCodigoPostal(), conf.getLogoUrl(), conf.getRegimenFiscal(),
                conf.getResponsabilidadesFiscales(), conf.getDecimalesMoneda(),
                conf.getFormatoFecha(), conf.isPreciosIncluyenImpuesto(),
                conf.isPoliticaStockNegativo(), conf.getImpuestoDefaultId(),
                conf.getPreferencias(), advertencia);
    }
}
