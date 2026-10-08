package cl.siga.msrabbitmqadmin.controller;

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
import cl.siga.msrabbitmqadmin.service.RabbitAdminService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API REST de administración del broker. Los parámetros se validan con Bean
 * Validation y las operaciones exigen rol ADMIN más un scope vigente de
 * usuarios (el scope dedicado de infraestructura se agregará cuando se exponga
 * en Entra ID).
 */
@RestController
@RequestMapping("/api/v1/rabbitmq")
@RequiredArgsConstructor
@Validated
@Tag(name = "Administración RabbitMQ", description = "Declaración y borrado de colas, exchanges y bindings")
public class RabbitAdminController {

    private final RabbitAdminService service;

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
        service.eliminarBinding(new SolicitudBindingDTO(cola, exchange, routingKey));
        return ResponseEntity.noContent().build();
    }
}
