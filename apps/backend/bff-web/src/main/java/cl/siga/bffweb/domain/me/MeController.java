package cl.siga.bffweb.domain.me;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import cl.siga.bffweb.domain.me.dto.api.MeResponseDTO;
import lombok.RequiredArgsConstructor;

@RestController 
@RequestMapping ("/api/me")
@RequiredArgsConstructor 
public class MeController {
    private final MeService service;

    @GetMapping
    public ResponseEntity<MeResponseDTO> me() {
        return ResponseEntity.ok(service.getMe());
    }
}
