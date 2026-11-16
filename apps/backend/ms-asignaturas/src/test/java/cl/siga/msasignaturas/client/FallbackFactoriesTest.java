package cl.siga.msasignaturas.client;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.exception.ServiceUnavailableException;

class FallbackFactoriesTest {

    private final RuntimeException causa = new RuntimeException("conexion rechazada");

    @Test
    void claseClientFallbackDevuelve503EnTodasSusOperaciones() {
        ClaseClient client = new ClaseClientFallbackFactory().create(causa);

        ServiceUnavailableException existe = assertThrows(ServiceUnavailableException.class,
            () -> client.existsById(1L));
        ServiceUnavailableException obtiene = assertThrows(ServiceUnavailableException.class,
            () -> client.getClaseById(2L));

        assertTrue(existe.getMessage().contains("No se pudo verificar la existencia de la clase 1"));
        assertTrue(obtiene.getMessage().contains("No se pudo obtener la clase 2"));
        assertTrue(existe.getMessage().contains("HTTP -1"));
    }

    @Test
    void estudianteClientFallbackDevuelve503EnTodasSusOperaciones() {
        EstudianteClient client = new EstudianteClientFallbackFactory().create(causa);

        ServiceUnavailableException porUsuario = assertThrows(ServiceUnavailableException.class,
            () -> client.getEstudianteByIdUsuario("oid-1"));
        ServiceUnavailableException existe = assertThrows(ServiceUnavailableException.class,
            () -> client.existsById(7L));

        assertTrue(porUsuario.getMessage().contains("No se pudo verificar el estudiante con ID de usuario oid-1"));
        assertTrue(existe.getMessage().contains("No se pudo verificar la existencia del estudiante 7"));
    }

    @Test
    void docenteClientFallbackDevuelve503() {
        DocenteClient client = new DocenteClientFallbackFactory().create(causa);

        ServiceUnavailableException existe = assertThrows(ServiceUnavailableException.class,
            () -> client.existsById(3L));

        assertTrue(existe.getMessage().contains("No se pudo verificar la existencia del docente 3"));
    }
}
