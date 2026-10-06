package cl.siga.msusuariosauth.integration.graph;

import java.security.SecureRandom;

/**
 * Genera contraseñas temporales conformes a la política de Entra ID
 * (mayúscula, minúscula, dígito y símbolo; sin caracteres ambiguos).
 */
public final class PasswordGenerator {

    private static final String MAYUSCULAS = "ABCDEFGHJKLMNPQRSTUVWXYZ";
    private static final String MINUSCULAS = "abcdefghijkmnpqrstuvwxyz";
    private static final String DIGITOS = "23456789";
    private static final String SIMBOLOS = "!@#$%*?-_";
    private static final int LARGO = 16;

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {
    }

    public static String generar() {
        StringBuilder password = new StringBuilder(LARGO);
        password.append(aleatorio(MAYUSCULAS));
        password.append(aleatorio(MINUSCULAS));
        password.append(aleatorio(DIGITOS));
        password.append(aleatorio(SIMBOLOS));
        String todos = MAYUSCULAS + MINUSCULAS + DIGITOS + SIMBOLOS;
        while (password.length() < LARGO) {
            password.append(aleatorio(todos));
        }
        return mezclar(password);
    }

    private static char aleatorio(String universo) {
        return universo.charAt(RANDOM.nextInt(universo.length()));
    }

    private static String mezclar(StringBuilder valor) {
        char[] caracteres = valor.toString().toCharArray();
        for (int i = caracteres.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temporal = caracteres[i];
            caracteres[i] = caracteres[j];
            caracteres[j] = temporal;
        }
        return new String(caracteres);
    }
}
