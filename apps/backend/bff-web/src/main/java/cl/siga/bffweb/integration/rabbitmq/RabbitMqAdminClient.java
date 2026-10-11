package cl.siga.bffweb.integration.rabbitmq;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudBindingDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudExchangeDTO;

/**
 * Fachada Feign hacia ms-rabbitmq-admin. El BFF reexpone las operaciones bajo
 * /api/bff/v1/admin/rabbitmq para que el navegador nunca hable con el broker.
 */
@FeignClient(
    name = "ms-rabbitmq-admin",
    url = "${services.rabbitmq-admin.url}",
    fallbackFactory = RabbitMqAdminClientFallbackFactory.class
)
public interface RabbitMqAdminClient {

    @PostMapping("/api/v1/rabbitmq/queues")
    void declararCola(@RequestBody SolicitudColaDTO solicitud);

    @GetMapping("/api/v1/rabbitmq/queues/{nombre}")
    EstadoColaDTO estadoCola(@PathVariable("nombre") String nombre);

    @DeleteMapping("/api/v1/rabbitmq/queues/{nombre}")
    void eliminarCola(@PathVariable("nombre") String nombre);

    @PostMapping("/api/v1/rabbitmq/exchanges")
    void declararExchange(@RequestBody SolicitudExchangeDTO solicitud);

    @DeleteMapping("/api/v1/rabbitmq/exchanges/{nombre}")
    void eliminarExchange(@PathVariable("nombre") String nombre);

    @PostMapping("/api/v1/rabbitmq/bindings")
    void declararBinding(@RequestBody SolicitudBindingDTO solicitud);

    @DeleteMapping("/api/v1/rabbitmq/bindings")
    void eliminarBinding(
        @RequestParam("cola") String cola,
        @RequestParam("exchange") String exchange,
        @RequestParam(name = "routingKey", required = false) String routingKey);
}
