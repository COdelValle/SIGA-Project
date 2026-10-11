package cl.siga.coreshare.dto.rabbitmq;

/** Estado operativo de una cola: mensajes en espera y consumidores activos. */
public record EstadoColaDTO(String nombre, Integer mensajes, Integer consumidores) {
}
