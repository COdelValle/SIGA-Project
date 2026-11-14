package cl.siga.msevaluaciones.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.enums.TipoEvaluacion;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
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

    @Test
    void getEvaluacionByIdRetornaLaEvaluacionActiva() {
        Evaluacion entity = new Evaluacion();
        entity.setId(10L);
        EvaluacionResponseDTO response = new EvaluacionResponseDTO(
                10L, "PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0, 5L, true);
        when(repository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(entity));
        when(mapper.toResponseDto(entity)).thenReturn(response);

        EvaluacionResponseDTO actual = service.getEvaluacionById(10L);

        assertEquals(response, actual);
        verify(mapper).toResponseDto(entity);
    }

    @Test
    void getEvaluacionByIdInexistenteLanzaNotFound() {
        when(repository.findByIdAndActiveTrue(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEvaluacionById(404L));
        verify(mapper, never()).toResponseDto(any());
    }

    @Test
    void searchEvaluacionesMapeaLaPaginaDelRepositorio() {
        Evaluacion entity = new Evaluacion();
        EvaluacionResponseDTO response = new EvaluacionResponseDTO(
                50L, "PRUEBA", TipoEvaluacion.SUMATIVA, 30.0, 5L, true);
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(entity), pageable, 1));
        when(mapper.toResponseDto(entity)).thenReturn(response);

        Page<EvaluacionResponseDTO> pagina = service.searchEvaluaciones(
                "PRUEBA", TipoEvaluacion.SUMATIVA, 5L, pageable);

        assertEquals(1, pagina.getTotalElements());
        assertEquals(response, pagina.getContent().get(0));
        verify(repository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void rechazaNombreDuplicadoAlCrear() {
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(dictacion(true));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue("PRUEBA 1", 5L))
                .thenReturn(true);

        BusinessException error = assertThrows(BusinessException.class, () -> service.saveEvaluacion(
                new RegistrarEvaluacionRequestDTO("PRUEBA 1", TipoEvaluacion.SUMATIVA, 30.0, 5L)));

        assertTrue(error.getMessage().contains("Ya existe una evaluación llamada 'PRUEBA 1'"));
        verify(repository, never()).save(any());
    }

    @Test
    void normalizaElNombreConEspaciosAntesDeBuscarDuplicados() {
        RegistrarEvaluacionRequestDTO request =
                new RegistrarEvaluacionRequestDTO("  PRUEBA 3  ", TipoEvaluacion.SUMATIVA, 30.0, 5L);
        Evaluacion entity = new Evaluacion();
        EvaluacionResponseDTO response = new EvaluacionResponseDTO(
                51L, "PRUEBA 3", TipoEvaluacion.SUMATIVA, 30.0, 5L, true);
        when(asignaturaClient.getCursoAsignaturaById(5L)).thenReturn(dictacion(true));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue("PRUEBA 3", 5L))
                .thenReturn(false);
        when(repository.findActiveByIdCursoAsignaturaForUpdate(5L)).thenReturn(List.of());
        when(mapper.toEntity(request)).thenReturn(entity);
        when(repository.save(entity)).thenReturn(entity);
        when(mapper.toResponseDto(entity)).thenReturn(response);

        service.saveEvaluacion(request);

        verify(repository).existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrue("PRUEBA 3", 5L);
        assertEquals(30.0, entity.getPonderacion());
    }

    @Test
    void updateEvaluacionInexistenteLanzaNotFound() {
        when(repository.findByIdAndActiveTrue(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateEvaluacion(404L,
                new ActualizarEvaluacionRequestDTO("PRUEBA", TipoEvaluacion.SUMATIVA, 30.0)));
        verify(repository, never()).save(any());
    }

    @Test
    void updateEvaluacionNoSumativaFuerzaPonderacionCero() {
        Evaluacion existente = new Evaluacion();
        existente.setId(33L);
        existente.setIdCursoAsignatura(5L);
        existente.setTipo(TipoEvaluacion.SUMATIVA);
        existente.setPonderacion(20.0);
        EvaluacionResponseDTO response = new EvaluacionResponseDTO(
                33L, "TAREA 2", TipoEvaluacion.FORMATIVA, 0.0, 5L, true);
        when(repository.findByIdAndActiveTrue(33L)).thenReturn(Optional.of(existente));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrueAndIdNot(
                "TAREA 2", 5L, 33L)).thenReturn(false);
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDto(existente)).thenReturn(response);

        EvaluacionResponseDTO actual = service.updateEvaluacion(33L,
                new ActualizarEvaluacionRequestDTO("TAREA 2", TipoEvaluacion.FORMATIVA, 40.0));

        assertEquals(0.0, existente.getPonderacion());
        assertEquals(response, actual);
        verify(repository, never()).findActiveByIdCursoAsignaturaForUpdate(5L);
    }

    @Test
    void updateEvaluacionSumativaConsideraSoloOtrasSumativas() {
        Evaluacion existente = new Evaluacion();
        existente.setId(34L);
        existente.setIdCursoAsignatura(5L);
        existente.setTipo(TipoEvaluacion.SUMATIVA);
        existente.setPonderacion(25.0);
        Evaluacion otraSumativa = new Evaluacion();
        otraSumativa.setId(35L);
        otraSumativa.setTipo(TipoEvaluacion.SUMATIVA);
        otraSumativa.setPonderacion(50.0);
        Evaluacion formativa = new Evaluacion();
        formativa.setId(36L);
        formativa.setTipo(TipoEvaluacion.FORMATIVA);
        formativa.setPonderacion(70.0);
        EvaluacionResponseDTO response = new EvaluacionResponseDTO(
                34L, "PRUEBA EDITADA", TipoEvaluacion.SUMATIVA, 20.0, 5L, true);
        when(repository.findByIdAndActiveTrue(34L)).thenReturn(Optional.of(existente));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrueAndIdNot(
                "PRUEBA EDITADA", 5L, 34L)).thenReturn(false);
        when(repository.findActiveByIdCursoAsignaturaForUpdate(5L))
                .thenReturn(List.of(existente, otraSumativa, formativa));
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDto(existente)).thenReturn(response);

        service.updateEvaluacion(34L,
                new ActualizarEvaluacionRequestDTO("PRUEBA EDITADA", TipoEvaluacion.SUMATIVA, 20.0));

        assertEquals(20.0, existente.getPonderacion());
        verify(publicador).publicarActualizada(response);
    }

    @Test
    void updateEvaluacionSumativaRechazaExcederElCienPorCiento() {
        Evaluacion existente = new Evaluacion();
        existente.setId(37L);
        existente.setIdCursoAsignatura(5L);
        existente.setTipo(TipoEvaluacion.SUMATIVA);
        existente.setPonderacion(10.0);
        Evaluacion otraSumativa = new Evaluacion();
        otraSumativa.setId(38L);
        otraSumativa.setTipo(TipoEvaluacion.SUMATIVA);
        otraSumativa.setPonderacion(90.0);
        when(repository.findByIdAndActiveTrue(37L)).thenReturn(Optional.of(existente));
        when(repository.existsByNombreIgnoreCaseAndIdCursoAsignaturaAndActiveTrueAndIdNot(
                "PRUEBA 3", 5L, 37L)).thenReturn(false);
        when(repository.findActiveByIdCursoAsignaturaForUpdate(5L))
                .thenReturn(List.of(existente, otraSumativa));

        assertThrows(BusinessException.class, () -> service.updateEvaluacion(37L,
                new ActualizarEvaluacionRequestDTO("PRUEBA 3", TipoEvaluacion.SUMATIVA, 20.0)));
        verify(repository, never()).save(any());
    }

    @Test
    void existsEvaluacionByIdRetornaElEstadoDelRepositorio() {
        when(repository.existsByIdAndActiveTrue(9L)).thenReturn(true);
        when(repository.existsByIdAndActiveTrue(10L)).thenReturn(false);

        assertTrue(service.existsEvaluacionById(9L));
        assertFalse(service.existsEvaluacionById(10L));
    }

    @Test
    void deleteEvaluacionInexistenteLanzaNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteEvaluacion(404L));
        verify(publicador, never()).publicarEliminada(any());
    }
}
