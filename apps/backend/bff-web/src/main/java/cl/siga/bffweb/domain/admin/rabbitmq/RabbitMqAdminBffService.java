package cl.siga.bffweb.domain.admin.rabbitmq;

import org.springframework.stereotype.Service;

import cl.siga.bffweb.integration.rabbitmq.RabbitMqAdminClient;
import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudBindingDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudExchangeDTO;
import lombok.RequiredArgsConstructor;

/**
 * Orquesta la administración RabbitMQ del portal admin: el BFF valida el token
 * y el rol, y delega en ms-rabbitmq-admin (el único que opera el broker).
 */
@Service
@RequiredArgsConstructor
public class RabbitMqAdminBffService {

    private final RabbitMqAdminClient client;

    public void declararCola(SolicitudColaDTO solicitud) {
        client.declararCola(solicitud);
    }

    public EstadoColaDTO estadoCola(String nombre) {
        return client.estadoCola(nombre);
    }

    public void eliminarCola(String nombre) {
        client.eliminarCola(nombre);
    }

    public void declararExchange(SolicitudExchangeDTO solicitud) {
        client.declararExchange(solicitud);
    }

    public void eliminarExchange(String nombre) {
        client.eliminarExchange(nombre);
    }

    public void declararBinding(SolicitudBindingDTO solicitud) {
        client.declararBinding(solicitud);
    }

    public void eliminarBinding(String cola, String exchange, String routingKey) {
        client.eliminarBinding(cola, exchange, routingKey);
    }
}
