package cl.siga.msusuariosauth.service;

import java.util.UUID;

import org.junit.jupiter.api.Test;

public class UsuarioServiceTest {
    @Test 
    void testCrearUsuarioLocal() {
        // Generas un UUID al vuelo que emula perfectamente el formato de Azure
        String idDePrueba = UUID.randomUUID().toString();

        // Usas el Builder de Lombok de forma limpia
        Usuario usuarioTest = Usuario.builder()
                .id(idDePrueba)
                .nombre("Carlos Test")
                .email("carlos@test.com")
                .build();

        usuarioRepository.save(usuarioTest);
        assertNotNull(usuarioRepository.findById(idDePrueba));
    }
}
