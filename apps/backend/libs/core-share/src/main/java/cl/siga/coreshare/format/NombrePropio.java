package cl.siga.coreshare.format;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Capitalizacion inteligente de nombres propios:
 * <ul>
 *   <li>Primera letra de cada palabra en mayuscula y el resto en minuscula
 *       ({@code CAtAlina} -> {@code Catalina}).</li>
 *   <li>Particulas conectoras siempre en minuscula (incluido el inicio del
 *       campo): {@code JUAN DE LA ROSA} -> {@code Juan de la Rosa} y
 *       {@code DEL VALLE} -> {@code del Valle}.</li>
 *   <li>Prefijos {@code Mc}/{@code Mac} y apostrofes:
 *       {@code mcdonald} -> {@code McDonald}, {@code o'hara} -> {@code O'Hara}.</li>
 *   <li>Guiones: cada segmento capitalizado ({@code perez-gomez} -> {@code Perez-Gomez}).</li>
 * </ul>
 * Espejo en el frontend (preview y edicion en vivo).
 */
public final class NombrePropio {

    private static final Set<String> PARTICULAS = Set.of("de", "del", "la", "las", "los", "y", "e");
    private static final String APOSTROFES = "'\u2019";

    private NombrePropio() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim().replaceAll("\\s+", " ");
        if (limpio.isEmpty()) {
            return limpio;
        }
        String[] palabras = limpio.toLowerCase(Locale.ROOT).split(" ");
        StringBuilder resultado = new StringBuilder();
        for (int i = 0; i < palabras.length; i++) {
            if (i > 0) {
                resultado.append(' ');
            }
            resultado.append(normalizarPalabra(palabras[i]));
        }
        return resultado.toString();
    }

    private static String normalizarPalabra(String palabra) {
        if (palabra.isEmpty()) {
            return palabra;
        }
        if (PARTICULAS.contains(palabra)) {
            return palabra;
        }
        StringBuilder sb = new StringBuilder(palabra.length());
        boolean inicioSegmento = true;
        for (int i = 0; i < palabra.length(); i++) {
            char c = palabra.charAt(i);
            if (c == '-' || APOSTROFES.indexOf(c) >= 0) {
                sb.append(c);
                inicioSegmento = true;
                continue;
            }
            sb.append(inicioSegmento ? Character.toUpperCase(c) : c);
            inicioSegmento = false;
        }
        return aplicarPrefijos(sb.toString());
    }

    private static String aplicarPrefijos(String palabra) {
        String lower = palabra.toLowerCase(Locale.ROOT);
        for (String prefijo : List.of("mc", "mac")) {
            if (!lower.startsWith(prefijo) || palabra.length() <= prefijo.length()) {
                continue;
            }
            char siguiente = palabra.charAt(prefijo.length());
            boolean macConVocal = "mac".equals(prefijo) && esVocal(lower.charAt(prefijo.length()));
            if (macConVocal) {
                continue;
            }
            return palabra.substring(0, prefijo.length()) + Character.toUpperCase(siguiente)
                    + palabra.substring(prefijo.length() + 1);
        }
        return palabra;
    }

    private static boolean esVocal(char c) {
        return "aeiou".indexOf(c) >= 0;
    }
}
