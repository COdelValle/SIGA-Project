package cl.siga.bffweb.integration.rabbitmq;

import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudBindingDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudExchangeDTO;
import cl.siga.coreshare.exception.FeignFallbacks;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class RabbitMqAdminClientFallbackFactory implements FallbackFactory<RabbitMqAdminClient> {

    @Override
    public RabbitMqAdminClient create(Throwable causa) {
        return new RabbitMqAdminClient() {
            @Override
            public void declararCola(SolicitudColaDTO solicitud) {
                throw error("No se pudo declarar la cola.");
            }

            @Override
            public EstadoColaDTO estadoCola(String nombre) {
                throw error("No se pudo obtener el estado de la cola.");
            }

            @Override
            public void eliminarCola(String nombre) {
                throw error("No se pudo eliminar la cola.");
            }

            @Override
            public void declararExchange(SolicitudExchangeDTO solicitud) {
                throw error("No se pudo declarar el exchange.");
            }

            @Override
            public void eliminarExchange(String nombre) {
                throw error("No se pudo eliminar el exchange.");
            }

            @Override
            public void declararBinding(SolicitudBindingDTO solicitud) {
                throw error("No se pudo declarar el binding.");
            }

            @Override
            public void eliminarBinding(String cola, String exchange, String routingKey) {
                throw error("No se pudo eliminar el binding.");
            }

            private RuntimeException error(String mensaje) {
                log.error("Fallo al llamar a ms-rabbitmq-admin (HTTP {}): {}",
                    causa instanceof FeignException feign ? feign.status() : -1, causa.getMessage());
                return FeignFallbacks.noDisponible(causa, mensaje);
            }
        };
    }
}
