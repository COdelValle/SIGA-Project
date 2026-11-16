package cl.siga.bffweb.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.security.access.prepost.PreAuthorize;

import cl.siga.bffweb.domain.asistencias.AsistenciaController;
import cl.siga.bffweb.domain.estudiantes.PerfilEstudianteController;

class BffPreAuthorizeExpressionsTest {

    private static final List<Class<?>> CONTROLLERS = List.of(
        PerfilEstudianteController.class,
        AsistenciaController.class);

    private final SpelExpressionParser parser = new SpelExpressionParser();

    @Test
    void todasLasExpresionesPreAuthorizeSonParseables() {
        for (Class<?> controller : CONTROLLERS) {
            validar(controller.getAnnotation(PreAuthorize.class), controller.getSimpleName());
            for (Method method : controller.getDeclaredMethods()) {
                validar(method.getAnnotation(PreAuthorize.class),
                    controller.getSimpleName() + "#" + method.getName());
            }
        }
    }

    @Test
    void endpointsPersonalesExigenRolEstudiante() throws NoSuchMethodException {
        PreAuthorize perfilMe = PerfilEstudianteController.class
            .getMethod("getPerfilMe")
            .getAnnotation(PreAuthorize.class);
        PreAuthorize asistenciasMe = AsistenciaController.class
            .getMethod("getAsistenciasMe")
            .getAnnotation(PreAuthorize.class);

        assertThat(perfilMe).isNotNull();
        assertThat(asistenciasMe).isNotNull();
        assertThat(perfilMe.value()).contains("hasRole('ESTUDIANTE')");
        assertThat(asistenciasMe.value()).contains("hasRole('ESTUDIANTE')");
    }

    private void validar(PreAuthorize anotacion, String ubicacion) {
        if (anotacion == null) {
            return;
        }
        assertThatCode(() -> parser.parseExpression(anotacion.value()))
            .as("Expresion @PreAuthorize invalida en %s", ubicacion)
            .doesNotThrowAnyException();
    }
}
