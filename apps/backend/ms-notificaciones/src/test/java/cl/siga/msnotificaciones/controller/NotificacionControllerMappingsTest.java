package cl.siga.msnotificaciones.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;

/**
 * Regresión del 404 reportado: el BFF llamaba PATCH/DELETE /me/leidas y el
 * microservicio no declaraba esas rutas. Un 401 sin token no prueba que la ruta
 * exista (Spring autentica antes de resolver el handler), por eso se valida por
 * reflexión.
 */
class NotificacionControllerMappingsTest {

    @Test
    void declaraMarcarTodasYOcultarLeidas() {
        List<String> patch = rutasPatch();
        List<String> delete = rutasDelete();

        assertTrue(patch.contains("/me/leidas"), "Falta PATCH /me/leidas");
        assertTrue(delete.contains("/me/leidas"), "Falta DELETE /me/leidas");
    }

    private static List<String> rutasPatch() {
        return Arrays.stream(NotificacionController.class.getDeclaredMethods())
            .map(metodo -> metodo.getAnnotation(PatchMapping.class))
            .filter(Objects::nonNull)
            .flatMap(anotacion -> Arrays.stream(anotacion.value()))
            .toList();
    }

    private static List<String> rutasDelete() {
        return Arrays.stream(NotificacionController.class.getDeclaredMethods())
            .map(metodo -> metodo.getAnnotation(DeleteMapping.class))
            .filter(Objects::nonNull)
            .flatMap(anotacion -> Arrays.stream(anotacion.value()))
            .toList();
    }
}
