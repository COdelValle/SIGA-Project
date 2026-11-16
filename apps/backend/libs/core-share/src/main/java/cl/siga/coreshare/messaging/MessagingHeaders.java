package cl.siga.coreshare.messaging;

/**
 * Cabeceras AMQP compartidas. El nombre del header de correlación no forma
 * parte de la topología configurable, por lo que se mantiene como constante
 * de compilación para las anotaciones {@code @Header}.
 */
public final class MessagingHeaders {

    public static final String CORRELATION_ID = "X-Correlation-Id";

    private MessagingHeaders() {
    }
}
