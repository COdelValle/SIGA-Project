package cl.siga.msrabbitmqadmin.controller;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

/**
 * Regresión de rutas: la API de administración RabbitMQ debe exponer la
 * declaración y el borrado de colas, exchanges y bindings. Se valida por
 * reflexión porque un 401 sin token no prueba que la ruta exista.
 */
class RabbitAdminControllerMappingsTest {

    @Test
    void declaraColasExchangesYBindings() {
        List<String> post = rutasPost();
        List<String> get = rutasGet();
        List<String> delete = rutasDelete();

        assertTrue(post.contains("/queues"), "Falta POST /queues");
        assertTrue(post.contains("/exchanges"), "Falta POST /exchanges");
        assertTrue(post.contains("/bindings"), "Falta POST /bindings");

        assertTrue(get.contains("/queues/{nombre}"), "Falta GET /queues/{nombre}");

        assertTrue(delete.contains("/queues/{nombre}"), "Falta DELETE /queues/{nombre}");
        assertTrue(delete.contains("/exchanges/{nombre}"), "Falta DELETE /exchanges/{nombre}");
        assertTrue(delete.contains("/bindings"), "Falta DELETE /bindings");
    }

    private static List<String> rutasPost() {
        return Arrays.stream(RabbitAdminController.class.getDeclaredMethods())
            .map(metodo -> metodo.getAnnotation(PostMapping.class))
            .filter(Objects::nonNull)
            .flatMap(anotacion -> Arrays.stream(anotacion.value()))
            .toList();
    }

    private static List<String> rutasGet() {
        return Arrays.stream(RabbitAdminController.class.getDeclaredMethods())
            .map(metodo -> metodo.getAnnotation(GetMapping.class))
            .filter(Objects::nonNull)
            .flatMap(anotacion -> Arrays.stream(anotacion.value()))
            .toList();
    }

    private static List<String> rutasDelete() {
        return Arrays.stream(RabbitAdminController.class.getDeclaredMethods())
            .map(metodo -> metodo.getAnnotation(DeleteMapping.class))
            .filter(Objects::nonNull)
            .flatMap(anotacion -> Arrays.stream(anotacion.value()))
            .toList();
    }
}
