package com.regenta.alertas.api;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.regenta.alertas.aplicacion.ConsultaDeAlertas;
import com.regenta.alertas.aplicacion.ConsultaDeAlertas.AlertaDelUsuario;
import com.regenta.alertas.aplicacion.DispositivoDelUsuario;
import com.regenta.alertas.aplicacion.GestionDeDespacho;
import com.regenta.alertas.aplicacion.GestionDeDispositivos;
import com.regenta.alertas.aplicacion.GestionDePreferencias;
import com.regenta.alertas.aplicacion.PreferenciaDelUsuario;
import com.regenta.alertas.aplicacion.SolicitudDeDispositivo;
import com.regenta.alertas.aplicacion.SolicitudDePreferencia;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Entrega de alertas: dispositivos push, preferencias y centro de la app. HU-094. */
@RestController
@RequestMapping("/api/alertas")
@Tag(name = "Notificaciones", description = "Push, correo y dentro de la app")
public class NotificacionesControlador {

    private final GestionDeDispositivos dispositivos;
    private final GestionDePreferencias preferencias;
    private final ConsultaDeAlertas centro;
    private final GestionDeDespacho despacho;

    public NotificacionesControlador(GestionDeDispositivos dispositivos,
            GestionDePreferencias preferencias, ConsultaDeAlertas centro,
            GestionDeDespacho despacho) {
        this.dispositivos = dispositivos;
        this.preferencias = preferencias;
        this.centro = centro;
        this.despacho = despacho;
    }

    @PostMapping("/dispositivos")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Registra el token FCM del dispositivo del usuario")
    public DispositivoDelUsuario registrarDispositivo(
            @Valid @RequestBody SolicitudDeDispositivo solicitud) {
        return dispositivos.registrar(solicitud);
    }

    @GetMapping("/dispositivos")
    @Operation(summary = "Los dispositivos activos del usuario")
    public List<DispositivoDelUsuario> misDispositivos() {
        return dispositivos.mios();
    }

    @DeleteMapping("/dispositivos/{dispositivoId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Da de baja un dispositivo del usuario")
    public void eliminarDispositivo(@PathVariable UUID dispositivoId) {
        dispositivos.eliminar(dispositivoId);
    }

    @GetMapping("/preferencias")
    @Operation(summary = "Las preferencias de notificación del usuario")
    public List<PreferenciaDelUsuario> misPreferencias() {
        return preferencias.mias();
    }

    @PutMapping("/preferencias")
    @Operation(summary = "Fija canales y franja de no molestar para un tipo de alerta")
    public PreferenciaDelUsuario fijarPreferencia(
            @Valid @RequestBody SolicitudDePreferencia solicitud) {
        return preferencias.fijar(solicitud);
    }

    @GetMapping("/mias")
    @Operation(summary = "El centro de alertas del usuario, las nuevas primero y por severidad")
    public List<AlertaDelUsuario> misAlertas() {
        return centro.mias();
    }

    @PostMapping("/mias/vistas")
    @Operation(summary = "Al abrir el centro: marca vistas las alertas nuevas del usuario")
    public Map<String, Integer> marcarVistas() {
        return Map.of("marcadas", centro.marcarMiasVistas());
    }

    @PostMapping("/mias/{alertaId}/resolucion")
    @Operation(summary = "Marca una alerta resuelta: sale de las pendientes y queda quién la resolvió")
    public AlertaDelUsuario resolver(@PathVariable UUID alertaId) {
        return centro.resolver(alertaId);
    }

    @PostMapping("/entregas/reintento")
    @Operation(summary = "Reprocesa la cola de entregas pendientes y fallidas del negocio")
    public Map<String, Integer> reintentar() {
        return Map.of("entregasReprocesadas", despacho.reintentarPendientes());
    }
}
