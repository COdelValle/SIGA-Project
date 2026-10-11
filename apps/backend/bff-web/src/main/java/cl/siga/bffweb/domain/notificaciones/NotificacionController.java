package cl.siga.bffweb.domain.notificaciones;

import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.notificaciones.ContadorNotificacionesDTO;
import cl.siga.coreshare.dto.notificaciones.NotificacionResponseDTO;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bff/v1/notificaciones")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasAnyRole('ESTUDIANTE', 'APODERADO')")
public class NotificacionController {

    private final NotificacionBffService service;

    @GetMapping("/me")
    public ResponseEntity<PageResponseDTO<NotificacionResponseDTO>> obtenerMisNotificaciones(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ResponseEntity.ok(service.obtenerMisNotificaciones(page, size));
    }

    @GetMapping("/me/no-leidas/count")
    public ResponseEntity<ContadorNotificacionesDTO> contarNoLeidas() {
        return ResponseEntity.ok(service.contarNoLeidas());
    }

    @PatchMapping("/me/{id}/leida")
    public ResponseEntity<Void> marcarLeida(@PathVariable Long id) {
        service.marcarLeida(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/leidas")
    public ResponseEntity<Void> marcarTodasLeidas() {
        service.marcarTodasLeidas();
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/leidas")
    public ResponseEntity<Void> limpiarLeidas() {
        service.limpiarLeidas();
        return ResponseEntity.noContent().build();
    }
}
