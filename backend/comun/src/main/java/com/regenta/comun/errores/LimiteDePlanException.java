package com.regenta.comun.errores;

import org.springframework.http.HttpStatus;

/** El plan no da para mas: usuarios, sucursales, dispositivos. 402. */
public class LimiteDePlanException extends ErrorDeAplicacion {

    private static final long serialVersionUID = 1L;

    private final String limite;

    public LimiteDePlanException(String limite, String detalle) {
        super(HttpStatus.PAYMENT_REQUIRED, "Limite del plan", detalle);
        this.limite = limite;
    }

    public String getLimite() {
        return limite;
    }
}
