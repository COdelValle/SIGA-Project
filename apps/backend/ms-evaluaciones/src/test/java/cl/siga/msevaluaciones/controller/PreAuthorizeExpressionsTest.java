package cl.siga.msevaluaciones.controller;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Valida que todas las expresiones @PreAuthorize de los controllers sean
 * parseables por SpEL. Un parentesis desbalanceado solo explota en runtime
 * (SpelParseException -> 500), por lo que se cubre aqui como regresion.
 */
class PreAuthorizeExpressionsTest {

    private static final List<Class<?>> CONTROLLERS = List.of(EvaluacionController.class);

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Test
    void todasLasExpresionesPreAuthorizeSonParseables() {
        for (Class<?> controller : CONTROLLERS) {
            validar(controller.getAnnotation(PreAuthorize.class),
                controller.getSimpleName() + " (clase)");
            for (Method method : controller.getDeclaredMethods()) {
                validar(method.getAnnotation(PreAuthorize.class),
                    controller.getSimpleName() + "#" + method.getName());
            }
        }
    }

    private void validar(PreAuthorize anotacion, String ubicacion) {
        if (anotacion == null) {
            return;
        }
        assertDoesNotThrow(() -> parser.parseExpression(anotacion.value()),
            "Expresion @PreAuthorize invalida en " + ubicacion);
    }
}
