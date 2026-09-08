package com.regenta.alertas.aplicacion;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.regenta.alertas.domain.Alerta;
import com.regenta.alertas.domain.CanalDeAlerta;
import com.regenta.alertas.domain.Entrega;
import com.regenta.alertas.domain.Huella;
import com.regenta.alertas.domain.PlantillaDeAlerta;
import com.regenta.alertas.domain.ReglaAlerta;
import com.regenta.alertas.domain.Severidad;
import com.regenta.alertas.domain.TipoAlerta;
import com.regenta.alertas.infra.AlertaRepositorio;
import com.regenta.alertas.infra.EntregaRepositorio;
import com.regenta.alertas.infra.ReglaAlertaRepositorio;
import com.regenta.alertas.infra.TipoAlertaRepositorio;
import com.regenta.comun.errores.NoEncontradoException;
import com.regenta.comun.negocio.ContextoDeNegocio;

/**
 * El motor de reglas (HU-092). Dado un hecho de un tipo, evalúa las reglas
 * activas de ese tipo en el negocio y, por cada una que coincide, genera la
 * alerta —una sola por huella (criterio 3)— y deja una entrega {@code PENDIENTE}
 * por cada destinatario y canal (criterio 5). Una regla desactivada no genera
 * nada (criterio 4).
 *
 * <p>Sin {@code @RequierePermiso}: lo llaman los consumidores de eventos
 * (HU-093) y la evaluación manual, que sí valida permiso en su borde.
 */
@Service
public class MotorDeAlertas {

    private final ReglaAlertaRepositorio reglas;
    private final TipoAlertaRepositorio tipos;
    private final AlertaRepositorio alertas;
    private final EntregaRepositorio entregas;
    private final DirectorioDeUsuarios directorio;

    public MotorDeAlertas(ReglaAlertaRepositorio reglas, TipoAlertaRepositorio tipos,
            AlertaRepositorio alertas, EntregaRepositorio entregas,
            DirectorioDeUsuarios directorio) {
        this.reglas = reglas;
        this.tipos = tipos;
        this.alertas = alertas;
        this.entregas = entregas;
        this.directorio = directorio;
    }

    /**
     * Resuelve solas las alertas abiertas de una entidad y un tipo (HU-093
     * criterio 4: el stock volvió por encima del mínimo). Devuelve cuántas.
     */
    @Transactional
    public int resolverPorEntidad(String tipoCodigo, String entidadTipo, UUID entidadId) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        List<com.regenta.alertas.domain.Alerta> abiertas =
                alertas.activasDeEntidad(negocioId, tipoCodigo, entidadTipo, entidadId);
        for (com.regenta.alertas.domain.Alerta a : abiertas) {
            a.resolver(null);
            alertas.save(a);
        }
        return abiertas.size();
    }

    @Transactional
    public List<UUID> evaluar(HechoDeAlerta hecho) {
        UUID negocioId = ContextoDeNegocio.negocioActual();
        TipoAlerta tipo = tipos.findById(hecho.tipoCodigo())
                .orElseThrow(() -> new NoEncontradoException(
                        "Tipo de alerta desconocido: " + hecho.tipoCodigo()));

        List<UUID> generadas = new ArrayList<>();
        for (ReglaAlerta regla : reglas.findByNegocioIdAndTipoCodigoAndActivaTrue(
                negocioId, hecho.tipoCodigo())) {
            if (!regla.disparaCon(hecho.campos())) {
                continue;
            }
            UUID id = generarODejar(negocioId, regla, tipo, hecho);
            if (id != null) {
                generadas.add(id);
            }
        }
        return generadas;
    }

    private UUID generarODejar(UUID negocioId, ReglaAlerta regla, TipoAlerta tipo,
            HechoDeAlerta hecho) {
        String huella = Huella.de(regla, hecho.entidadTipo(), hecho.entidadId());
        String titulo = PlantillaDeAlerta.render(tipo.getPlantillaTitulo(), hecho.campos());
        String mensaje = PlantillaDeAlerta.render(tipo.getPlantillaMensaje(), hecho.campos());
        Severidad severidad = regla.getSeveridad();

        Alerta existente = alertas.tomarPorHuella(negocioId, huella).orElse(null);
        if (existente == null) {
            Alerta nueva = Alerta.generar(negocioId, regla, hecho.tipoCodigo(), severidad,
                    titulo, mensaje, datos(hecho), hecho.entidadTipo(), hecho.entidadId(),
                    hecho.rutaApp(), huella);
            alertas.save(nueva);
            crearEntregas(negocioId, regla, nueva.getId());
            return nueva.getId();
        }
        if (existente.estaActiva()
                || existente.dentroDeVentanaDeSilencio(regla.getSilenciarHoras(),
                        OffsetDateTime.now())) {
            return null; // criterio 3
        }
        existente.reabrir(titulo, mensaje, datos(hecho));
        alertas.save(existente);
        crearEntregas(negocioId, regla, existente.getId());
        return existente.getId();
    }

    private void crearEntregas(UUID negocioId, ReglaAlerta regla, UUID alertaId) {
        Set<UUID> destinatarios = regla.destinatarios(
                rol -> directorio.usuariosConRol(negocioId, rol));
        for (UUID usuario : destinatarios) {
            for (String canal : regla.getCanales()) {
                entregas.save(Entrega.pendiente(negocioId, alertaId, usuario,
                        CanalDeAlerta.valueOf(canal)));
            }
        }
    }

    private static Map<String, Object> datos(HechoDeAlerta hecho) {
        Map<String, Object> datos = new LinkedHashMap<>(hecho.campos());
        if (hecho.entidadId() != null) {
            datos.putIfAbsent("entidad_id", hecho.entidadId().toString());
        }
        return datos;
    }
}
