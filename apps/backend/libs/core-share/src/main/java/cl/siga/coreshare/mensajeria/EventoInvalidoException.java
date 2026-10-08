package cl.siga.coreshare.mensajeria;

/**
 * Mensaje consumido con datos inválidos o incompletos. Es un error permanente:
 * no se reintenta y el consumidor lo rechaza (NACK sin requeue) hacia la DLQ.
 */
public class EventoInvalidoException extends RuntimeException {

    public EventoInvalidoException(String mensaje) {
        super(mensaje);
    }
}
