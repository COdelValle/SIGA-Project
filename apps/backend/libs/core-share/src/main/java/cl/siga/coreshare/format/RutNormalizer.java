package cl.siga.coreshare.format;

import java.util.Locale;

/**
 * Normaliza RUT al formato canonico de almacenamiento: sin puntos, con guion
 * antes del digito verificador y {@code K} en mayuscula (p. ej. {@code 13789943-2}).
 *
 * <p>No valida: la validacion del digito verificador la hace {@code @RUT}.</p>
 */
public final class RutNormalizer {

    private RutNormalizer() {
    }

    public static String normalizar(String rut) {
        if (rut == null) {
            return null;
        }
        String limpio = rut.replace(".", "").replace(" ", "").trim().toUpperCase(Locale.ROOT);
        if (limpio.isEmpty()) {
            return limpio;
        }
        if (!limpio.contains("-") && limpio.length() >= 2) {
            limpio = limpio.substring(0, limpio.length() - 1) + "-" + limpio.charAt(limpio.length() - 1);
        }
        return limpio;
    }
}
