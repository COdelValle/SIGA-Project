package cl.siga.coreshare.dto.rabbitmq;

/** Tipos de exchange admitidos por la API de administración RabbitMQ. */
public enum TipoExchange {
    DIRECT,
    TOPIC,
    FANOUT,
    HEADERS
}
