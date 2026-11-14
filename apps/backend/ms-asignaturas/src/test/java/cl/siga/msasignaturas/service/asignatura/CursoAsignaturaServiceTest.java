package cl.siga.msasignaturas.service.asignatura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.curso.ActualizarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.curso.RegistrarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.client.ClaseClient;
import cl.siga.msasignaturas.client.DocenteClient;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import cl.siga.msasignaturas.model.mapper.CursoAsignaturaMapper;
import cl.siga.msasignaturas.repository.InscripcionRepository;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;
import cl.siga.msasignaturas.repository.asignatura.MallaCurricularRepository;

@ExtendWith(MockitoExtension.class)
class CursoAsignaturaServiceTest {

    @Mock
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private MallaCurricularRepository mallaCurricularRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private CursoAsignaturaMapper cursoAsignaturaMapper;
    @Mock
    private ClaseClient claseClient;
    @Mock
    private DocenteClient docenteClient;

    private CursoAsignaturaService service;

    @BeforeEach
    void setUp() {
        service = new CursoAsignaturaService(
                cursoAsignaturaRepository,
                asignaturaRepository,
                mallaCurricularRepository,
                inscripcionRepository,
                cursoAsignaturaMapper,
                claseClient,
                docenteClient);
    }

    private Asignatura asignatura(AreaAcademica area) {
        return Asignatura.builder()
                .id(1L)
                .nombre("Matemática")
                .descripcion("Matemática básica")
                .area(area)
                .calificable(true)
                .active(true)
                .build();
    }

    private CursoAsignatura curso(boolean active, CaracterAsignatura caracter) {
        return CursoAsignatura.builder()
                .id(10L)
                .asignatura(asignatura(AreaAcademica.MATEMATICAS))
                .idClase(5L)
                .idDocente(2L)
                .semestre(Semestre.SEMESTRE_1)
                .caracter(caracter)
                .cupoMaximo(20)
                .active(active)
                .build();
    }

    private CursoAsignaturaResponseDTO response() {
        return new CursoAsignaturaResponseDTO(10L, 1L, "Matemática", "Matemática básica",
                AreaAcademica.MATEMATICAS, true, CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1,
                2L, 5L, null, null, 0, List.of());
    }

    private RegistrarCursoAsignaturaRequestDTO registrar(CaracterAsignatura caracter, Integer cupo) {
        return new RegistrarCursoAsignaturaRequestDTO(
                1L, 5L, 2L, Semestre.SEMESTRE_1, caracter, cupo,
                List.of(new HorarioRequestDTO(DiaSemana.LUNES,
                        LocalTime.of(8, 0), LocalTime.of(9, 30), "Sala 1")));
    }

    private ActualizarCursoAsignaturaRequestDTO actualizar(Long docente, CaracterAsignatura caracter,
            Integer cupo) {
        return new ActualizarCursoAsignaturaRequestDTO(docente, Semestre.SEMESTRE_1, caracter, cupo);
    }

    private void stubClaseYDocenteValidos() {
        when(claseClient.getClaseById(5L)).thenReturn(
                new ClaseResponseDTO(5L, Nivel.OCTAVO_BASICO, "A", 2026, null));
        when(mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.COMUN)).thenReturn(true);
        when(docenteClient.existsById(2L)).thenReturn(true);
    }

    @Test
    void searchCursoAsignaturasDevuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 10);
        CursoAsignatura curso = curso(true, CaracterAsignatura.OBLIGATORIA);
        when(cursoAsignaturaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(curso), pageable, 1));
        when(cursoAsignaturaMapper.toDto(curso)).thenReturn(response());

        Page<CursoAsignaturaResponseDTO> resultado = service.searchCursoAsignaturas(
                5L, 2L, 1L, CaracterAsignatura.OBLIGATORIA, Semestre.SEMESTRE_1,
                AreaAcademica.MATEMATICAS, "mat", true, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Matemática", resultado.getContent().get(0).nombre());
    }

    @Test
    void obtenerPorIdDevuelveLaDictacion() {
        CursoAsignatura curso = curso(true, CaracterAsignatura.OBLIGATORIA);
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(curso));
        when(cursoAsignaturaMapper.toDto(curso)).thenReturn(response());

        assertEquals(10L, service.obtenerPorId(10L).id());
    }

    @Test
    void obtenerPorIdLanzaNotFound() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorId(99L));
    }

    @Test
    void existCursoAsignaturaDelegaEnElRepositorio() {
        when(cursoAsignaturaRepository.existsByIdAndActiveTrue(10L)).thenReturn(true);
        when(cursoAsignaturaRepository.existsByIdAndActiveTrue(11L)).thenReturn(false);

        assertTrue(service.existCursoAsignatura(10L));
        assertFalse(service.existCursoAsignatura(11L));
    }

    @Test
    void crearLanzaNotFoundSiLaAsignaturaNoExiste() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.crear(registrar(CaracterAsignatura.OBLIGATORIA, null)));
    }

    @Test
    void crearRechazaAsignaturaFueraDeLaMalla() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura(AreaAcademica.MATEMATICAS)));
        when(claseClient.getClaseById(5L)).thenReturn(
                new ClaseResponseDTO(5L, Nivel.OCTAVO_BASICO, "A", 2026, null));
        when(mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.COMUN)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.crear(registrar(CaracterAsignatura.OBLIGATORIA, null)));
    }

    @Test
    void crearRechazaDocenteInexistente() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura(AreaAcademica.MATEMATICAS)));
        when(claseClient.getClaseById(5L)).thenReturn(
                new ClaseResponseDTO(5L, Nivel.OCTAVO_BASICO, "A", 2026, null));
        when(mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.COMUN)).thenReturn(true);
        when(docenteClient.existsById(2L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.crear(registrar(CaracterAsignatura.OBLIGATORIA, null)));
    }

    @Test
    void crearRechazaOptativaSinCupo() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura(AreaAcademica.MATEMATICAS)));
        stubClaseYDocenteValidos();

        assertThrows(BusinessException.class,
                () -> service.crear(registrar(CaracterAsignatura.OPTATIVA, null)));
    }

    @Test
    void crearRechazaDictacionActivaDuplicada() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura(AreaAcademica.MATEMATICAS)));
        stubClaseYDocenteValidos();
        when(cursoAsignaturaRepository.findFirstByAsignaturaIdAndIdClaseAndSemestre(
                1L, 5L, Semestre.SEMESTRE_1)).thenReturn(Optional.of(curso(true, CaracterAsignatura.OBLIGATORIA)));

        assertThrows(BusinessException.class,
                () -> service.crear(registrar(CaracterAsignatura.OBLIGATORIA, null)));
        verify(cursoAsignaturaRepository, never()).save(any());
    }

    @Test
    void crearReactivaDictacionInactivaYReemplazaHorarios() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura(AreaAcademica.MATEMATICAS)));
        stubClaseYDocenteValidos();
        CursoAsignatura existente = curso(false, CaracterAsignatura.OBLIGATORIA);
        existente.getHorarios().add(Horario.builder().id(7L).active(true).build());
        when(cursoAsignaturaRepository.findFirstByAsignaturaIdAndIdClaseAndSemestre(
                1L, 5L, Semestre.SEMESTRE_1)).thenReturn(Optional.of(existente));
        CursoAsignatura nueva = curso(true, CaracterAsignatura.OPTATIVA);
        nueva.addHorario(Horario.builder().dia(DiaSemana.LUNES).active(true).build());
        when(cursoAsignaturaMapper.toEntity(any(RegistrarCursoAsignaturaRequestDTO.class), any(Asignatura.class)))
                .thenReturn(nueva);
        when(cursoAsignaturaRepository.save(existente)).thenReturn(existente);
        when(cursoAsignaturaMapper.toDto(existente)).thenReturn(response());

        service.crear(registrar(CaracterAsignatura.OPTATIVA, 15));

        assertTrue(existente.isActive());
        assertEquals(2L, existente.getIdDocente());
        assertEquals(CaracterAsignatura.OPTATIVA, existente.getCaracter());
        assertEquals(15, existente.getCupoMaximo());
        assertEquals(1, existente.getHorarios().size());
    }

    @Test
    void crearDictacionNuevaNormalizaCupoObligatorio() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura(AreaAcademica.MATEMATICAS)));
        stubClaseYDocenteValidos();
        when(cursoAsignaturaRepository.findFirstByAsignaturaIdAndIdClaseAndSemestre(
                1L, 5L, Semestre.SEMESTRE_1)).thenReturn(Optional.empty());
        CursoAsignatura nueva = curso(true, CaracterAsignatura.OBLIGATORIA);
        when(cursoAsignaturaMapper.toEntity(any(RegistrarCursoAsignaturaRequestDTO.class), any(Asignatura.class)))
                .thenReturn(nueva);
        when(cursoAsignaturaRepository.save(nueva)).thenReturn(nueva);
        when(cursoAsignaturaMapper.toDto(nueva)).thenReturn(response());

        service.crear(registrar(CaracterAsignatura.OBLIGATORIA, 30));

        verify(cursoAsignaturaRepository).save(nueva);
    }

    @Test
    void actualizarDictacionInexistenteLanzaNotFound() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizar(99L, actualizar(2L, CaracterAsignatura.OBLIGATORIA, null)));
    }

    @Test
    void actualizarConNuevoDocenteInexistenteLanzaBusiness() {
        CursoAsignatura curso = curso(true, CaracterAsignatura.OBLIGATORIA);
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(curso));
        when(docenteClient.existsById(3L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.actualizar(10L, actualizar(3L, CaracterAsignatura.OBLIGATORIA, null)));
    }

    @Test
    void actualizarRechazaCupoMenorALosInscritos() {
        CursoAsignatura curso = curso(true, CaracterAsignatura.OBLIGATORIA);
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(curso));
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(10L), any())).thenReturn(5);

        assertThrows(BusinessException.class,
                () -> service.actualizar(10L, actualizar(2L, CaracterAsignatura.OPTATIVA, 4)));
        verify(cursoAsignaturaRepository, never()).save(any());
    }

    @Test
    void actualizarRechazaOptativaSinCupo() {
        CursoAsignatura curso = curso(true, CaracterAsignatura.OBLIGATORIA);
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(curso));

        assertThrows(BusinessException.class,
                () -> service.actualizar(10L, actualizar(2L, CaracterAsignatura.OPTATIVA, null)));
    }

    @Test
    void actualizarObligatoriaNoValidaCupos() {
        CursoAsignatura curso = curso(true, CaracterAsignatura.OPTATIVA);
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(curso));
        when(cursoAsignaturaRepository.save(curso)).thenReturn(curso);
        when(cursoAsignaturaMapper.toDto(curso)).thenReturn(response());

        service.actualizar(10L, actualizar(2L, CaracterAsignatura.OBLIGATORIA, 30));

        assertNull(curso.getCupoMaximo());
        verify(inscripcionRepository, never()).countByCursoAsignaturaIdAndEstadoIn(any(), any());
        verify(cursoAsignaturaMapper).updateEntity(any(ActualizarCursoAsignaturaRequestDTO.class), eq(curso));
    }

    @Test
    void actualizarConCupoSuficienteGuardaLosCambios() {
        CursoAsignatura curso = curso(true, CaracterAsignatura.OBLIGATORIA);
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(curso));
        when(docenteClient.existsById(3L)).thenReturn(true);
        when(inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(eq(10L), any())).thenReturn(5);
        when(cursoAsignaturaRepository.save(curso)).thenReturn(curso);
        when(cursoAsignaturaMapper.toDto(curso)).thenReturn(response());

        service.actualizar(10L, actualizar(3L, CaracterAsignatura.OPTATIVA, 10));

        assertEquals(10, curso.getCupoMaximo());
        verify(cursoAsignaturaRepository).save(curso);
    }

    @Test
    void eliminarDictacionInexistenteLanzaNotFound() {
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(99L));
    }

    @Test
    void eliminarDesactivaLaDictacionYSusHorarios() {
        CursoAsignatura curso = curso(true, CaracterAsignatura.OBLIGATORIA);
        Horario horario = Horario.builder().id(7L).active(true).build();
        curso.addHorario(horario);
        when(cursoAsignaturaRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(curso));
        when(cursoAsignaturaRepository.save(curso)).thenReturn(curso);

        service.eliminar(10L);

        assertFalse(curso.isActive());
        assertFalse(horario.isActive());
        verify(cursoAsignaturaRepository).save(curso);
    }

    @Test
    void validarCumplimientoMineducApruebaSeisElectivosEnDosAreas() {
        List<CursoAsignatura> dictaciones = List.of(
                dictacion(AreaAcademica.LENGUAJE, CaracterAsignatura.ELECTIVA),
                dictacion(AreaAcademica.HISTORIA, CaracterAsignatura.ELECTIVA),
                dictacion(AreaAcademica.MATEMATICAS, CaracterAsignatura.ELECTIVA),
                dictacion(AreaAcademica.CIENCIAS, CaracterAsignatura.ELECTIVA),
                dictacion(AreaAcademica.ARTES, CaracterAsignatura.ELECTIVA),
                dictacion(AreaAcademica.EDUCACION_FISICA, CaracterAsignatura.ELECTIVA),
                dictacion(AreaAcademica.MATEMATICAS, CaracterAsignatura.OBLIGATORIA));
        when(cursoAsignaturaRepository.findAll(any(Specification.class))).thenReturn(dictaciones);

        Map<String, Object> resultado = service.validarCumplimientoMineduc();

        assertEquals(6, resultado.get("totalElectivosOfrecidos"));
        assertEquals(3, resultado.get("areasCubiertas"));
        assertEquals(Boolean.TRUE, resultado.get("cumpleNormativaMineduc"));
    }

    @Test
    void validarCumplimientoMineducRechazaPocosElectivosYMismaArea() {
        List<CursoAsignatura> dictaciones = List.of(
                dictacion(AreaAcademica.LENGUAJE, CaracterAsignatura.ELECTIVA),
                dictacion(AreaAcademica.LENGUAJE, CaracterAsignatura.ELECTIVA),
                dictacion(AreaAcademica.LENGUAJE, CaracterAsignatura.ELECTIVA));
        when(cursoAsignaturaRepository.findAll(any(Specification.class))).thenReturn(dictaciones);

        Map<String, Object> resultado = service.validarCumplimientoMineduc();

        assertEquals(3, resultado.get("totalElectivosOfrecidos"));
        assertEquals(1, resultado.get("areasCubiertas"));
        assertEquals(Boolean.FALSE, resultado.get("cumpleNormativaMineduc"));
    }

    private CursoAsignatura dictacion(AreaAcademica area, CaracterAsignatura caracter) {
        return CursoAsignatura.builder()
                .id(1L)
                .asignatura(asignatura(area))
                .idClase(5L)
                .idDocente(2L)
                .semestre(Semestre.SEMESTRE_1)
                .caracter(caracter)
                .active(true)
                .build();
    }
}
