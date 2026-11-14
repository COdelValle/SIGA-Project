package cl.siga.msasignaturas.service.asignatura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.malla.ActualizarMallaCurricularRequestDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.asignatura.malla.RegistrarMallaCurricularRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.MallaCurricular;
import cl.siga.msasignaturas.model.mapper.MallaCurricularMapper;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import cl.siga.msasignaturas.repository.asignatura.MallaCurricularRepository;

@ExtendWith(MockitoExtension.class)
class MallaCurricularServiceTest {

    @Mock
    private MallaCurricularRepository mallaCurricularRepository;
    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private MallaCurricularMapper mallaCurricularMapper;

    private MallaCurricularService service;

    @BeforeEach
    void setUp() {
        service = new MallaCurricularService(mallaCurricularRepository, asignaturaRepository, mallaCurricularMapper);
    }

    private Asignatura asignatura() {
        return Asignatura.builder()
                .id(1L)
                .nombre("Matemática")
                .descripcion("Matemática básica")
                .area(AreaAcademica.MATEMATICAS)
                .calificable(true)
                .active(true)
                .build();
    }

    private MallaCurricular malla(boolean active, PlanFormacion plan) {
        return MallaCurricular.builder()
                .id(10L)
                .nivel(Nivel.OCTAVO_BASICO)
                .asignatura(asignatura())
                .caracter(CaracterAsignatura.OBLIGATORIA)
                .plan(plan)
                .horasSemanales(4)
                .active(active)
                .build();
    }

    private MallaCurricularResponseDTO response() {
        return new MallaCurricularResponseDTO(10L, Nivel.OCTAVO_BASICO, 1L, "Matemática",
                AreaAcademica.MATEMATICAS, CaracterAsignatura.OBLIGATORIA, PlanFormacion.COMUN,
                4, true, true);
    }

    private RegistrarMallaCurricularRequestDTO registrar() {
        return new RegistrarMallaCurricularRequestDTO(Nivel.OCTAVO_BASICO, 1L,
                CaracterAsignatura.OBLIGATORIA, PlanFormacion.COMUN, 4);
    }

    @Test
    void listarSinNivelNiPlanUsaTodasLasFilas() {
        MallaCurricular fila = malla(true, PlanFormacion.COMUN);
        when(mallaCurricularRepository.findByActiveTrueOrderByNivelAscAsignaturaNombreAsc())
                .thenReturn(List.of(fila));
        when(mallaCurricularMapper.toDto(fila)).thenReturn(response());

        assertEquals(1, service.listar(null, null).size());
    }

    @Test
    void listarSinNivelConPlanFiltraEnMemoria() {
        MallaCurricular comun = malla(true, PlanFormacion.COMUN);
        MallaCurricular diferenciada = malla(true, PlanFormacion.DIFERENCIADA_HC);
        when(mallaCurricularRepository.findByActiveTrueOrderByNivelAscAsignaturaNombreAsc())
                .thenReturn(List.of(comun, diferenciada));
        when(mallaCurricularMapper.toDto(comun)).thenReturn(response());

        assertEquals(1, service.listar(null, PlanFormacion.COMUN).size());
    }

    @Test
    void listarConNivelSinPlanUsaConsultaPorNivel() {
        MallaCurricular fila = malla(true, PlanFormacion.COMUN);
        when(mallaCurricularRepository.findByNivelAndActiveTrueOrderByAsignaturaNombreAsc(Nivel.OCTAVO_BASICO))
                .thenReturn(List.of(fila));
        when(mallaCurricularMapper.toDto(fila)).thenReturn(response());

        assertEquals(1, service.listar(Nivel.OCTAVO_BASICO, null).size());
    }

    @Test
    void listarConNivelYPlanUsaConsultaCombinada() {
        MallaCurricular fila = malla(true, PlanFormacion.COMUN);
        when(mallaCurricularRepository.findByNivelAndPlanAndActiveTrueOrderByAsignaturaNombreAsc(
                Nivel.OCTAVO_BASICO, PlanFormacion.COMUN)).thenReturn(List.of(fila));
        when(mallaCurricularMapper.toDto(fila)).thenReturn(response());

        assertEquals(1, service.listar(Nivel.OCTAVO_BASICO, PlanFormacion.COMUN).size());
    }

    @Test
    void crearLanzaNotFoundSiLaAsignaturaNoExiste() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.crear(registrar()));
    }

    @Test
    void crearRechazaFilaDuplicada() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura()));
        when(mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.COMUN)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.crear(registrar()));
        verify(mallaCurricularRepository, never()).save(any());
    }

    @Test
    void crearReactivaFilaEliminadaLogicamente() {
        MallaCurricular existente = malla(false, PlanFormacion.COMUN);
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura()));
        when(mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.COMUN)).thenReturn(false);
        when(mallaCurricularRepository.findFirstByNivelAndAsignaturaIdAndPlan(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.COMUN)).thenReturn(Optional.of(existente));
        when(mallaCurricularRepository.save(existente)).thenReturn(existente);
        when(mallaCurricularMapper.toDto(existente)).thenReturn(response());

        service.crear(new RegistrarMallaCurricularRequestDTO(Nivel.OCTAVO_BASICO, 1L,
                CaracterAsignatura.ELECTIVA, PlanFormacion.COMUN, 6));

        assertTrue(existente.isActive());
        assertEquals(CaracterAsignatura.ELECTIVA, existente.getCaracter());
        assertEquals(6, existente.getHorasSemanales());
    }

    @Test
    void creaFilaNueva() {
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura()));
        when(mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.COMUN)).thenReturn(false);
        when(mallaCurricularRepository.findFirstByNivelAndAsignaturaIdAndPlan(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.COMUN)).thenReturn(Optional.empty());
        when(mallaCurricularRepository.save(any(MallaCurricular.class))).thenAnswer(inv -> inv.getArgument(0));
        when(mallaCurricularMapper.toDto(any(MallaCurricular.class))).thenReturn(response());

        service.crear(registrar());

        ArgumentCaptor<MallaCurricular> captor = ArgumentCaptor.forClass(MallaCurricular.class);
        verify(mallaCurricularRepository).save(captor.capture());
        assertTrue(captor.getValue().isActive());
        assertEquals(Nivel.OCTAVO_BASICO, captor.getValue().getNivel());
    }

    @Test
    void actualizarFilaInexistenteLanzaNotFound() {
        when(mallaCurricularRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.actualizar(99L, new ActualizarMallaCurricularRequestDTO(
                        CaracterAsignatura.OBLIGATORIA, PlanFormacion.COMUN, 4)));
    }

    @Test
    void actualizarRechazaDuplicado() {
        MallaCurricular existente = malla(true, PlanFormacion.COMUN);
        when(mallaCurricularRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(existente));
        when(mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrueAndIdNot(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.DIFERENCIADA_HC, 10L)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.actualizar(10L, new ActualizarMallaCurricularRequestDTO(
                        CaracterAsignatura.ELECTIVA, PlanFormacion.DIFERENCIADA_HC, 6)));
    }

    @Test
    void actualizarFilaGuardaCambios() {
        MallaCurricular existente = malla(true, PlanFormacion.COMUN);
        when(mallaCurricularRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(existente));
        when(mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrueAndIdNot(
                Nivel.OCTAVO_BASICO, 1L, PlanFormacion.DIFERENCIADA_HC, 10L)).thenReturn(false);
        when(mallaCurricularRepository.save(existente)).thenReturn(existente);
        when(mallaCurricularMapper.toDto(existente)).thenReturn(response());

        service.actualizar(10L, new ActualizarMallaCurricularRequestDTO(
                CaracterAsignatura.ELECTIVA, PlanFormacion.DIFERENCIADA_HC, 6));

        assertEquals(CaracterAsignatura.ELECTIVA, existente.getCaracter());
        assertEquals(PlanFormacion.DIFERENCIADA_HC, existente.getPlan());
        assertEquals(6, existente.getHorasSemanales());
    }

    @Test
    void eliminarFilaInexistenteLanzaNotFound() {
        when(mallaCurricularRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(99L));
    }

    @Test
    void eliminarFilaLaMarcaInactiva() {
        MallaCurricular existente = malla(true, PlanFormacion.COMUN);
        when(mallaCurricularRepository.findByIdAndActiveTrue(10L)).thenReturn(Optional.of(existente));
        when(mallaCurricularRepository.save(existente)).thenReturn(existente);

        service.eliminar(10L);

        assertFalse(existente.isActive());
        verify(mallaCurricularRepository).save(existente);
    }
}
