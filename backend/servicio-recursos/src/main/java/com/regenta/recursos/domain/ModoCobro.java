package com.regenta.recursos.domain;

import java.util.Locale;

import com.regenta.comun.errores.ReglaDeNegocioException;

/**
 * Cómo se multiplica el precio de un servicio adicional (HU-068). Un desayuno se
 * cobra por persona y por noche; un late check-out, una vez por estancia; el
 * alquiler de una raqueta, por unidad.
 */
public enum ModoCobro {

    /** Una vez, sin importar personas ni noches. */
    POR_ESTANCIA {
        @Override
        public int unidades(int personas, int noches, int cantidad) {
            return 1;
        }
    },
    /** Una por cada noche de la estancia. */
    POR_NOCHE {
        @Override
        public int unidades(int personas, int noches, int cantidad) {
            return Math.max(1, noches);
        }
    },
    /** Una por cada persona. */
    POR_PERSONA {
        @Override
        public int unidades(int personas, int noches, int cantidad) {
            return Math.max(1, personas);
        }
    },
    /** Una por cada persona y cada noche: 2 personas × 3 noches = 6. */
    POR_PERSONA_NOCHE {
        @Override
        public int unidades(int personas, int noches, int cantidad) {
            return Math.max(1, personas) * Math.max(1, noches);
        }
    },
    /** Las que se pidan explícitamente. */
    POR_UNIDAD {
        @Override
        public int unidades(int personas, int noches, int cantidad) {
            return Math.max(1, cantidad);
        }
    };

    public abstract int unidades(int personas, int noches, int cantidad);

    public static ModoCobro desde(String texto) {
        if (texto == null || texto.isBlank()) {
            return POR_ESTANCIA;
        }
        try {
            return valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException noExiste) {
            throw new ReglaDeNegocioException("Modo de cobro no válido: " + texto);
        }
    }
}
