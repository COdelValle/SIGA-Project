package cl.siga.bffweb.domain.admin.rabbitmq;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.coreshare.dto.rabbitmq.EstadoColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudBindingDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudColaDTO;
import cl.siga.coreshare.dto.rabbitmq.SolicitudExchangeDTO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * Fachada del portal admin para administrar RabbitMQ. Reexpone la API de
 * ms-rabbitmq-admin bajo el BFF: el navegador nunca se conecta al broker.
 */
@RestController
@RequestMapping("/api/bff/v1/admin/rabbitmq")
@RequiredArgsConstructor
@Validated
public class RabbitMqAdminController {

    private final RabbitMqAdminBffService service;

    @PostMapping("/queues")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<Void> declararCola(@RequestBody @Valid SolicitudColaDTO solicitud) {
        service.declararCola(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/queues/{nombre}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<EstadoColaDTO> estadoCola(@PathVariable String nombre) {
        return ResponseEntity.ok(service.estadoCola(nombre));
    }

    @DeleteMapping("/queues/{nombre}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:delete')")
    public ResponseEntity<Void> eliminarCola(@PathVariable String nombre) {
        service.eliminarCola(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/exchanges")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<Void> declararExchange(@RequestBody @Valid SolicitudExchangeDTO solicitud) {
        service.declararExchange(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/exchanges/{nombre}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:delete')")
    public ResponseEntity<Void> eliminarExchange(@PathVariable String nombre) {
        service.eliminarExchange(nombre);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/bindings")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<Void> declararBinding(@RequestBody @Valid SolicitudBindingDTO solicitud) {
        service.declararBinding(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/bindings")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:delete')")
    public ResponseEntity<Void> eliminarBinding(
            @RequestParam String cola,
            @RequestParam String exchange,
            @RequestParam(required = false) String routingKey) {
        service.eliminarBinding(cola, exchange, routingKey);
        return ResponseEntity.noContent().build();
    }
}
