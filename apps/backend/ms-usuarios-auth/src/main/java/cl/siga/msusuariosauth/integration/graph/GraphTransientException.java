package cl.siga.msusuariosauth.integration.graph;

import cl.siga.coreshare.exception.ServiceUnavailableException;

/**
 * Fallo transitorio de Graph (5xx, 408, 429, red): el mensaje puede reintentarse.
 * Extiende {@link ServiceUnavailableException} para conservar el 503 hacia el
 * cliente y ser distinguible del fallo permanente.
 */
public class GraphTransientException extends ServiceUnavailableException {
    public GraphTransientException(String message) {
        super(message);
    }

    public GraphTransientException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
