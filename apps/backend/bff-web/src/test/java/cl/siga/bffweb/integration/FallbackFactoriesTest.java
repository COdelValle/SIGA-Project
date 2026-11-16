package cl.siga.bffweb.integration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import cl.siga.bffweb.integration.apoderados.ApoderadoClientFallback;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClientFallback;
import cl.siga.bffweb.integration.asistencias.AsistenciaClientFallbackFactory;
import cl.siga.bffweb.integration.clases.ClaseClientFallback;
import cl.siga.bffweb.integration.docentes.DocenteClientFallback;
import cl.siga.bffweb.integration.estudiantes.EstudianteClientFallback;
import cl.siga.bffweb.integration.evaluaciones.EvaluacionClientFallbackFactory;
import cl.siga.bffweb.integration.notas.NotaClientFallbackFactory;
import cl.siga.bffweb.integration.notificaciones.NotificacionClientFallbackFactory;
import cl.siga.bffweb.integration.rabbitmq.RabbitMqAdminClient;
import cl.siga.bffweb.integration.rabbitmq.RabbitMqAdminClientFallbackFactory;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.bffweb.integration.usuarios.UsuarioClientFallbackFactory;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;

class FallbackFactoriesTest {

    private final FeignErrorTranslator translator = new FeignErrorTranslator(new ObjectMapper());

    private FeignException feignConStatus(int status) {
        FeignException causa = mock(FeignException.class);
        when(causa.status()).thenReturn(status);
        when(causa.contentUTF8()).thenReturn("");
        when(causa.getMessage()).thenReturn("fallo remoto");
        return causa;
    }

    private RuntimeException causaNoFeign() {
        return new RuntimeException("conexion rechazada");
    }

    @Test
    void usuarioFallbackConservaEl404DelUsuarioNoRegistrado() {
        UsuarioClient client = new UsuarioClientFallbackFactory(translator)
            .create(mock(FeignException.NotFound.class));

        assertThatThrownBy(client::getCurrentUsuario)
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("El usuario autenticado no esta registrado en SIGA.");
    }

    @Test
    void usuarioFallbackTraduceTodosLosEndpoints() {
        UsuarioClient client = new UsuarioClientFallbackFactory(translator)
            .create(feignConStatus(404));

        assertThatThrownBy(client::getCurrentUsuario)
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("HTTP 404");
        assertThatThrownBy(() -> client.getUsuarioById("oid-1"))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No se pudo obtener el usuario oid-1.");
        assertThatThrownBy(() -> client.searchUsuarios(null, null, null, 10))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudieron obtener los usuarios.")
            .hasMessageContaining("HTTP 404");
        assertThatThrownBy(() -> client.iniciarRegistroCompuesto(null))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.getRegistroCompuesto("p-1"))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.getCredencialTemporal("p-1"))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.resetPassword("oid-1"))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.deleteUsuario("oid-1"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void usuarioFallbackConCausaNoFeignDevuelve503ConStatusMenosUno() {
        UsuarioClient client = new UsuarioClientFallbackFactory(translator).create(causaNoFeign());

        assertThatThrownBy(client::getCurrentUsuario)
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessage("No se pudo obtener el usuario autenticado. (HTTP -1)");
    }

    @Test
    void notificacionFallbackCubreTodasLasOperaciones() {
        var client = new NotificacionClientFallbackFactory().create(causaNoFeign());

        assertThatThrownBy(() -> client.obtenerMisNotificaciones(0, 10))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudieron obtener las notificaciones.");
        assertThatThrownBy(client::contarNoLeidas)
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudo obtener el contador de notificaciones.");
        assertThatThrownBy(() -> client.marcarLeida(1L))
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudo actualizar la notificaci");
        assertThatThrownBy(client::marcarTodasLeidas)
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudieron marcar las notificaciones como le");
        assertThatThrownBy(client::limpiarLeidas)
            .isInstanceOf(ServiceUnavailableException.class)
            .hasMessageContaining("No se pudieron limpiar las notificaciones le");
    }

    @Test
    void notaFallbackCubreTodasLasOperaciones() {
        var client = new NotaClientFallbackFactory(translator).create(feignConStatus(404));

        assertThatThrownBy(() -> client.getNotaById(1L))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.searchNotas(1L, 10))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.searchNotasByEvaluacion(2L, 10))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.saveNota(null))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.updateNota(1L, null))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.deleteNota(1L))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void asistenciaFallbackCubreTodasLasOperaciones() {
        var client = new AsistenciaClientFallbackFactory(translator).create(feignConStatus(400));

        assertThatThrownBy(() -> client.searchAsistencias(1L, 10))
            .isInstanceOf(cl.siga.coreshare.exception.BusinessException.class);
        assertThatThrownBy(() -> client.searchAsistenciasByAsignatura(2L, null, null, 10))
            .isInstanceOf(cl.siga.coreshare.exception.BusinessException.class);
        assertThatThrownBy(() -> client.getAsistenciaById(1L))
            .isInstanceOf(cl.siga.coreshare.exception.BusinessException.class);
        assertThatThrownBy(() -> client.saveAsistencia(null))
            .isInstanceOf(cl.siga.coreshare.exception.BusinessException.class);
        assertThatThrownBy(() -> client.updateAsistencia(1L, null))
            .isInstanceOf(cl.siga.coreshare.exception.BusinessException.class);
    }

    @Test
    void evaluacionFallbackCubreTodasLasOperaciones() {
        var client = new EvaluacionClientFallbackFactory(translator).create(feignConStatus(409));

        assertThatThrownBy(() -> client.getEvaluacionById(1L))
            .isInstanceOf(cl.siga.coreshare.exception.ConflictException.class);
        assertThatThrownBy(() -> client.searchEvaluacionesByAsignatura(2L, 10))
            .isInstanceOf(cl.siga.coreshare.exception.ConflictException.class);
        assertThatThrownBy(() -> client.saveEvaluacion(null))
            .isInstanceOf(cl.siga.coreshare.exception.ConflictException.class);
        assertThatThrownBy(() -> client.updateEvaluacion(1L, null))
            .isInstanceOf(cl.siga.coreshare.exception.ConflictException.class);
        assertThatThrownBy(() -> client.deleteEvaluacion(1L))
            .isInstanceOf(cl.siga.coreshare.exception.ConflictException.class);
    }

    @Test
    void rabbitMqFallbackCubreTodasLasOperaciones() {
        RabbitMqAdminClient client =
            new RabbitMqAdminClientFallbackFactory(translator).create(feignConStatus(404));

        assertThatThrownBy(() -> client.declararCola(null))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessage("No se pudo declarar la cola.");
        assertThatThrownBy(() -> client.estadoCola("cola"))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.eliminarCola("cola"))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.declararExchange(null))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.eliminarExchange("exchange"))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.declararBinding(null))
            .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> client.eliminarBinding("cola", "exchange", "rk"))
            .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void fallbacksDeLecturaDirectaDevuelvenSiempre503() {
        AsignaturaClientFallback asignaturas = new AsignaturaClientFallback();
        EstudianteClientFallback estudiantes = new EstudianteClientFallback();
        ClaseClientFallback clases = new ClaseClientFallback();
        DocenteClientFallback docentes = new DocenteClientFallback();
        ApoderadoClientFallback apoderados = new ApoderadoClientFallback();

        assertThatThrownBy(() -> asignaturas.searchAsignaturas(10))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> asignaturas.getMalla(null))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> asignaturas.getCursoAsignaturaById(1L))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> asignaturas.searchCursoAsignaturasByClase(1L, 10))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> asignaturas.searchCursoAsignaturasByDocente(1L, 10))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> asignaturas.searchInscripcionesByAlumno(1L, 10))
            .isInstanceOf(ServiceUnavailableException.class);

        assertThatThrownBy(() -> estudiantes.getEstudianteById(1L))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> estudiantes.getEstudianteByIdUsuario("oid-1"))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> estudiantes.searchEstudiantesByClase(1L, 10))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> estudiantes.searchEstudiantes("ana", 10))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> estudiantes.updateEstudiante(1L, null))
            .isInstanceOf(ServiceUnavailableException.class);

        assertThatThrownBy(() -> clases.getClaseById(1L))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> clases.searchClases(2026, 10))
            .isInstanceOf(ServiceUnavailableException.class);

        assertThatThrownBy(() -> docentes.getDocenteById(1L))
            .isInstanceOf(ServiceUnavailableException.class);
        assertThatThrownBy(() -> docentes.getDocenteByIdUsuario("oid-1"))
            .isInstanceOf(ServiceUnavailableException.class);

        assertThatThrownBy(() -> apoderados.getApoderadoByIdUsuario("oid-1"))
            .isInstanceOf(ServiceUnavailableException.class);
    }
}
