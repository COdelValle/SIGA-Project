package cl.siga.msevaluaciones.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msevaluaciones.client.AsignaturaClient;
import cl.siga.msevaluaciones.model.entity.Evaluacion;
import cl.siga.msevaluaciones.model.mapper.EvaluacionMapper;
import cl.siga.msevaluaciones.repository.EvaluacionRepository;

class EvaluacionServiceTest {

    private EvaluacionRepository repository;
    private EvaluacionMapper mapper;
    private AsignaturaClient asignaturaClient;
    private EvaluacionService service;

    @BeforeEach
    void setUp() {
        repository = mock(EvaluacionRepository.class);
        mapper = mock(EvaluacionMapper.class);
        asignaturaClient = mock(AsignaturaClient.class);
        service = new EvaluacionService(repository, mapper, asignaturaClient);
    }

    @Test
    void rechazaEvaluacionDeAsignaturaInexistente() {
        when(asignaturaClient.existsById(5L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.saveEvaluacion(
                new RegistrarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0, 5L)));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaPonderacionQueSuperaElCienPorCiento() {
        when(asignaturaClient.existsById(5L)).thenReturn(true);
        when(repository.existsByNombreIgnoreCaseAndIdAsignaturaAndActiveTrue("PRUEBA 2", 5L)).thenReturn(false);

        Evaluacion existente = new Evaluacion();
        existente.setId(1L);
        existente.setPonderacion(80.0);
        when(repository.findActiveByIdAsignaturaForUpdate(5L)).thenReturn(List.of(existente));

        assertThrows(BusinessException.class, () -> service.saveEvaluacion(
                new RegistrarEvaluacionRequestDTO("PRUEBA 2", TipoEvaluacion.SUMATIVA, 30.0, 5L)));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaNombreDuplicadoAlActualizar() {
        Evaluacion actual = new Evaluacion();
        actual.setId(2L);
        actual.setIdAsignatura(5L);
        actual.setPonderacion(30.0);
        when(repository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(actual));
        when(repository.existsByNombreIgnoreCaseAndIdAsignaturaAndActiveTrueAndIdNot("PRUEBA 1", 5L, 2L))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.updateEvaluacion(2L,
                new ActualizarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0)));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
