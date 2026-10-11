package cl.siga.msevaluaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msevaluaciones.client.AsignaturaClient;
import cl.siga.msevaluaciones.mensajeria.PublicadorEvaluacion;
import cl.siga.msevaluaciones.model.entity.Evaluacion;
import cl.siga.msevaluaciones.model.mapper.EvaluacionMapper;
import cl.siga.msevaluaciones.repository.EvaluacionRepository;

class EvaluacionServiceTest {

    private EvaluacionRepository repository;
    private EvaluacionMapper mapper;
    private AsignaturaClient asignaturaClient;
    private PublicadorEvaluacion publicador;
    private EvaluacionService service;

    @BeforeEach
    void setUp() {
        repository = mock(EvaluacionRepository.class);
        mapper = mock(EvaluacionMapper.class);
        asignaturaClient = mock(AsignaturaClient.class);
        publicador = mock(PublicadorEvaluacion.class);
        service = new EvaluacionService(repository, mapper, asignaturaClient, publicador);
    }

    private CursoAsignaturaResponseDTO dictacion(boolean calificable) {
        return new CursoAsignaturaResponseDTO(
                5L, 1L, "Matemática", "matemática", AreaAcademica.MATEMATICAS, calificable,
                CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1, 6L, 4L,
                null, null, 0, List.of());
    }

    @Test
    void rechazaEvaluacionDeDictacionInexistente() {
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(null);

        assertThrows(BusinessException.class, () -> service.saveEvaluacion(
                new RegistrarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0, 5L)));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaEvaluacionDeAsignaturaNoCalificable() {
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(dictacion(false));

        assertThrows(BusinessException.class, () -> service.saveEvaluacion(
                new RegistrarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0, 5L)));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaPonderacionQueSuperaElCienPorCiento() {
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(dictacion(true));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue("PRUEBA 2", 5L)).thenReturn(false);

        Evaluacion existente = new Evaluacion();
        existente.setId(1L);
        existente.setTipo(TipoEvaluacion.SUMATIVA);
        existente.setPonderacion(80.0);
        when(repository.findActiveByIdCursoAsignaturaForUpdate(5L)).thenReturn(List.of(existente));

        assertThrows(BusinessException.class, () -> service.saveEvaluacion(
                new RegistrarEvaluacionRequestDTO("PRUEBA 2", TipoEvaluacion.SUMATIVA, 30.0, 5L)));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rechazaSumativaConPonderacionCero() {
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(dictacion(true));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue("EXAMEN 2", 5L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.saveEvaluacion(
                new RegistrarEvaluacionRequestDTO("EXAMEN 2", TipoEvaluacion.SUMATIVA, 0.0, 5L)));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void guardarNoSumativaFuerzaPonderacionCero() {
        RegistrarEvaluacionRequestDTO request =
                new RegistrarEvaluacionRequestDTO("TAREA 1", TipoEvaluacion.FORMATIVA, 40.0, 5L);
        Evaluacion entity = new Evaluacion();
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(dictacion(true));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue("TAREA 1", 5L)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDto(entity)).thenReturn(
                new EvaluacionResponseDTO(40L, "TAREA 1", TipoEvaluacion.FORMATIVA, 0.0, 5L, true));

        service.saveEvaluacion(request);

        assertEquals(0.0, entity.getPonderacion());
        verify(repository, never()).findActiveByIdCursoAsignaturaForUpdate(5L);
    }

    @Test
    void ponderacionAcumuladaIgnoraEvaluacionesNoSumativas() {
        RegistrarEvaluacionRequestDTO request =
                new RegistrarEvaluacionRequestDTO("PRUEBA 2", TipoEvaluacion.SUMATIVA, 30.0, 5L);
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(dictacion(true));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue("PRUEBA 2", 5L)).thenReturn(false);

        Evaluacion formativa = new Evaluacion();
        formativa.setId(1L);
        formativa.setTipo(TipoEvaluacion.FORMATIVA);
        formativa.setPonderacion(80.0);
        when(repository.findActiveByIdCursoAsignaturaForUpdate(5L)).thenReturn(List.of(formativa));

        Evaluacion entity = new Evaluacion();
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDto(entity)).thenReturn(
                new EvaluacionResponseDTO(41L, "PRUEBA 2", TipoEvaluacion.SUMATIVA, 30.0, 5L, true));

        service.saveEvaluacion(request);

        assertEquals(30.0, entity.getPonderacion());
    }

    @Test
    void rechazaNombreDuplicadoAlActualizar() {
        Evaluacion actual = new Evaluacion();
        actual.setId(2L);
        actual.setIdCursoAsignatura(5L);
        actual.setPonderacion(30.0);
        when(repository.findByIdAndActiveTrue(2L)).thenReturn(Optional.of(actual));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrueAndIdNot("PRUEBA 1", 5L, 2L))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.updateEvaluacion(2L,
                new ActualizarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0)));
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void crearEvaluacionPublicaEventoSoloDespuesDeGuardar() {
        RegistrarEvaluacionRequestDTO request =
                new RegistrarEvaluacionRequestDTO("PRUEBA NUEVA", TipoEvaluacion.SUMATIVA, 20.0, 5L);
        Evaluacion entity = new Evaluacion();
        entity.setId(30L);
        entity.setIdCursoAsignatura(5L);
        entity.setPonderacion(20.0);
        EvaluacionResponseDTO response = new EvaluacionResponseDTO(
                30L, "PRUEBA NUEVA", TipoEvaluacion.SUMATIVA, 20.0, 5L, true);
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(dictacion(true));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue("PRUEBA NUEVA", 5L)).thenReturn(false);
        when(repository.findActiveByIdCursoAsignaturaForUpdate(5L)).thenReturn(List.of());
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDto(entity)).thenReturn(response);

        EvaluacionResponseDTO actual = service.saveEvaluacion(request);

        org.assertj.core.api.Assertions.assertThat(actual).isEqualTo(response);
        verify(publicador).publicarCreada(response);
    }

    @Test
    void actualizarEvaluacionPublicaEventoActualizada() {
        Evaluacion existente = new Evaluacion();
        existente.setId(31L);
        existente.setIdCursoAsignatura(5L);
        existente.setPonderacion(25.0);
        when(repository.findByIdAndActiveTrue(31L)).thenReturn(Optional.of(existente));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrueAndIdNot(
                "PRUEBA EDITADA", 5L, 31L)).thenReturn(false);
        when(repository.findActiveByIdCursoAsignaturaForUpdate(5L)).thenReturn(List.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        EvaluacionResponseDTO response = new EvaluacionResponseDTO(
                31L, "PRUEBA EDITADA", TipoEvaluacion.FORMATIVA, 25.0, 5L, true);
        when(mapper.toResponseDto(existente)).thenReturn(response);

        service.updateEvaluacion(31L,
                new ActualizarEvaluacionRequestDTO("PRUEBA EDITADA", TipoEvaluacion.FORMATIVA, 25.0));

        verify(publicador).publicarActualizada(response);
    }

    @Test
    void eliminarEvaluacionPublicaEventoEliminada() {
        Evaluacion existente = new Evaluacion();
        existente.setId(32L);
        existente.setIdCursoAsignatura(5L);
        existente.setPonderacion(25.0);
        when(repository.findById(32L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        EvaluacionResponseDTO response = new EvaluacionResponseDTO(
                32L, "PRUEBA", TipoEvaluacion.SUMATIVA, 25.0, 5L, false);
        when(mapper.toResponseDto(existente)).thenReturn(response);

        service.deleteEvaluacion(32L);

        verify(publicador).publicarEliminada(response);
    }
}
