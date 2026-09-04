package com.regenta.comun.eventos;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Lo configurable del Outbox. Los valores por defecto sirven para todos los servicios. */
@ConfigurationProperties(prefix = "regenta.eventos")
public class PropiedadesEventos {

    /** Exchange al que salen los eventos de negocio. */
    private String exchange = "regenta.eventos";

    /** Exchange y cola donde terminan los que agotaron sus reintentos. */
    private String exchangeMuertos = "regenta.eventos.muertos";
    private String colaMuertos = "regenta.eventos.muertos";

    /** Cuantos eventos toma el publicador en cada pasada. */
    private int lote = 50;

    /** Cada cuanto revisa si hay pendientes. */
    private Duration intervalo = Duration.ofSeconds(2);

    /** Intentos antes de darlo por fallido y mandarlo a la cola muerta. */
    private int maximoIntentos = 10;

    /** Permite apagar el publicador en tests o en instancias que solo consumen. */
    private boolean publicadorActivo = true;

    public String getExchange() {
        return exchange;
    }

    public void setExchange(String exchange) {
        this.exchange = exchange;
    }

    public String getExchangeMuertos() {
        return exchangeMuertos;
    }

    public void setExchangeMuertos(String exchangeMuertos) {
        this.exchangeMuertos = exchangeMuertos;
    }

    public String getColaMuertos() {
        return colaMuertos;
    }

    public void setColaMuertos(String colaMuertos) {
        this.colaMuertos = colaMuertos;
    }

    public int getLote() {
        return lote;
    }

    public void setLote(int lote) {
        this.lote = lote;
    }

    public Duration getIntervalo() {
        return intervalo;
    }

    public void setIntervalo(Duration intervalo) {
        this.intervalo = intervalo;
    }

    public int getMaximoIntentos() {
        return maximoIntentos;
    }

    public void setMaximoIntentos(int maximoIntentos) {
        this.maximoIntentos = maximoIntentos;
    }

    public boolean isPublicadorActivo() {
        return publicadorActivo;
    }

    public void setPublicadorActivo(boolean publicadorActivo) {
        this.publicadorActivo = publicadorActivo;
    }
}
