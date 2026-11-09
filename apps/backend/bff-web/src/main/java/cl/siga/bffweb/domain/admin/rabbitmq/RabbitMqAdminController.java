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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
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

    private static final String REGEX_NOMBRE = "^[a-zA-Z0-9._-]+$";
    private static final String MSG_ERROR = "El nombre no puede estar vacío, ni contener espacios o caracteres especiales";

    private final RabbitMqAdminBffService service;

    @PostMapping("/queues")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:write')")
    public ResponseEntity<Void> declararCola(@RequestBody @Valid SolicitudColaDTO solicitud) {
        service.declararCola(solicitud);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/queues/{nombre}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:read')")
    public ResponseEntity<EstadoColaDTO> estadoCola(@PathVariable @NotBlank (message = MSG_ERROR) @Pattern(regexp = REGEX_NOMBRE, message = MSG_ERROR) String nombre) {
        return ResponseEntity.ok(service.estadoCola(nombre));
    }

    @DeleteMapping("/queues/{nombre}")
    @PreAuthorize("hasRole('ADMIN') and hasAuthority('SCOPE_usuarios:delete')")
    public ResponseEntity<Void> eliminarCola(@PathVariable @NotBlank(message = MSG_ERROR) @Pattern(regexp = REGEX_NOMBRE, message = MSG_ERROR) String nombre) {
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
    public ResponseEntity<Void> eliminarExchange(@PathVariable @NotBlank(message = MSG_ERROR) @Pattern(regexp = REGEX_NOMBRE, message = MSG_ERROR) String nombre) {
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
            @RequestParam @NotBlank(message = MSG_ERROR) @Pattern(regexp = REGEX_NOMBRE, message = MSG_ERROR) String cola,
            @RequestParam @NotBlank(message = MSG_ERROR) @Pattern(regexp = REGEX_NOMBRE, message = MSG_ERROR) String exchange,
            @RequestParam(required = false) String routingKey) {
        service.eliminarBinding(cola, exchange, routingKey);
        return ResponseEntity.noContent().build();
    }
}
