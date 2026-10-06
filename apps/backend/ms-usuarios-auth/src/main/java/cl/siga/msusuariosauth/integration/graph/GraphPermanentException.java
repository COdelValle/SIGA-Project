package cl.siga.msusuariosauth.integration.graph;

import cl.siga.coreshare.exception.BusinessException;

/**
 * Fallo no recuperable de Graph (4xx salvo 408/429): reintentar no sirve y el
 * proceso se marca FALLIDO.
 */
public class GraphPermanentException extends BusinessException {
    public GraphPermanentException(String message) {
        super(message);
    }

    public GraphPermanentException(String message, Throwable cause) {
        super(message, cause);
    }
}
