package cl.siga.coreshare.exception;

/**
 * Excepción para conflictos de estado (p. ej. recursos duplicados).
 */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
