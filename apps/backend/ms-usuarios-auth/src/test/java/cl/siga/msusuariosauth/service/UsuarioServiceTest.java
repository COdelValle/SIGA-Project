package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;

@SpringBootTest
@RequiredArgsConstructor 
public class UsuarioServiceTest {

    private final UsuarioRepository usuarioRepository;

    @Test 
    void testCrearUsuarioLocal() {
        // Generas un UUID al vuelo que emula perfectamente el formato de Azure
        String idDePrueba = UUID.randomUUID().toString();

        // Usas el Builder de Lombok de forma limpia
        Usuario usuarioTest = Usuario.builder()
                .id(idDePrueba)
                .email("carlos@test.com")
                .build();

        usuarioRepository.save(usuarioTest);
        assertNotNull(usuarioRepository.findById(idDePrueba));
    }
}
