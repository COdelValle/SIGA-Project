package cl.siga.msasignaturas.service.asignatura;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.asignatura.ActualizarAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.RegistrarAsignaturaRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.mapper.AsignaturaMapper;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;

@ExtendWith(MockitoExtension.class)
class AsignaturaServiceTest {

    @Mock
    private AsignaturaRepository asignaturaRepository;
    @Mock
    private CursoAsignaturaRepository cursoAsignaturaRepository;
    @Mock
    private AsignaturaMapper asignaturaMapper;

    private AsignaturaService service;

    @BeforeEach
    void setUp() {
        service = new AsignaturaService(asignaturaRepository, cursoAsignaturaRepository, asignaturaMapper);
    }

    private Asignatura asignatura(boolean active) {
        return Asignatura.builder()
                .id(1L)
                .nombre("Matemática")
                .nombreCorto("MAT")
                .descripcion("Matemática básica")
                .area(AreaAcademica.MATEMATICAS)
                .calificable(true)
                .active(active)
                .build();
    }

    private AsignaturaResponseDTO response() {
        return new AsignaturaResponseDTO(1L, "Matemática", "MAT", "Matemática básica",
                AreaAcademica.MATEMATICAS, true, true);
    }

    private RegistrarAsignaturaRequestDTO registrar() {
        return new RegistrarAsignaturaRequestDTO("Matemática", "MAT", "Matemática básica",
                AreaAcademica.MATEMATICAS, true);
    }

    private ActualizarAsignaturaRequestDTO actualizar() {
        return new ActualizarAsignaturaRequestDTO("Matemática II", "MAT2", "Matemática avanzada",
                AreaAcademica.MATEMATICAS, true);
    }

    @Test
    void searchAsignaturasDevuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 10);
        Asignatura asignatura = asignatura(true);
        when(asignaturaRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(asignatura), pageable, 1));
        when(asignaturaMapper.toDto(asignatura)).thenReturn(response());

        Page<AsignaturaResponseDTO> resultado = service.searchAsignaturas(
                "mat", AreaAcademica.MATEMATICAS, true, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Matemática", resultado.getContent().get(0).nombre());
    }

    @Test
    void obtenerPorIdDevuelveLaAsignatura() {
        Asignatura asignatura = asignatura(true);
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(asignatura));
        when(asignaturaMapper.toDto(asignatura)).thenReturn(response());

        assertEquals(1L, service.obtenerPorId(1L).id());
    }

    @Test
    void obtenerPorIdLanzaNotFound() {
        when(asignaturaRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.obtenerPorId(99L));
    }

    @Test
    void existAsignaturaDelegaEnElRepositorio() {
        when(asignaturaRepository.existsByIdAndActiveTrue(1L)).thenReturn(true);
        when(asignaturaRepository.existsByIdAndActiveTrue(2L)).thenReturn(false);

        assertTrue(service.existAsignatura(1L));
        assertFalse(service.existAsignatura(2L));
    }

    @Test
    void rechazaNombreDuplicadoAlCrear() {
        when(asignaturaRepository.existsByNombreIgnoreCaseAndActiveTrue("Matemática")).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.crear(registrar()));
        verify(asignaturaRepository, never()).save(any());
    }

    @Test
    void reactivaAsignaturaEliminadaLogicamente() {
        Asignatura existente = asignatura(false);
        when(asignaturaRepository.existsByNombreIgnoreCaseAndActiveTrue("Matemática II")).thenReturn(false);
        when(asignaturaRepository.findFirstByNombreIgnoreCase("Matemática II")).thenReturn(Optional.of(existente));
        when(asignaturaRepository.save(existente)).thenReturn(existente);
        when(asignaturaMapper.toDto(existente)).thenReturn(response());

        service.crear(new RegistrarAsignaturaRequestDTO("Matemática II", "MAT2", "Avanzada",
                AreaAcademica.CIENCIAS, false));

        assertTrue(existente.isActive());
        assertEquals("Matemática II", existente.getNombre());
        assertEquals(AreaAcademica.CIENCIAS, existente.getArea());
        assertFalse(existente.isCalificable());
    }

    @Test
    void creaAsignaturaNueva() {
        when(asignaturaRepository.existsByNombreIgnoreCaseAndActiveTrue("Matemática")).thenReturn(false);
        when(asignaturaRepository.findFirstByNombreIgnoreCase("Matemática")).thenReturn(Optional.empty());
        when(asignaturaRepository.save(any(Asignatura.class))).thenAnswer(inv -> inv.getArgument(0));
        when(asignaturaMapper.toDto(any(Asignatura.class))).thenReturn(response());

        service.crear(registrar());

        ArgumentCaptor<Asignatura> captor = ArgumentCaptor.forClass(Asignatura.class);
        verify(asignaturaRepository).save(captor.capture());
        assertTrue(captor.getValue().isActive());
        assertEquals("Matemática", captor.getValue().getNombre());
    }

    @Test
    void actualizarAsignaturaInexistenteLanzaNotFound() {
        when(asignaturaRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.actualizar(99L, actualizar()));
    }

    @Test
    void rechazaNombreDuplicadoAlActualizar() {
        Asignatura existente = asignatura(true);
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existente));
        when(asignaturaRepository.existsByNombreIgnoreCaseAndActiveTrueAndIdNot("Matemática II", 1L))
                .thenReturn(true);

        assertThrows(BusinessException.class, () -> service.actualizar(1L, actualizar()));
        verify(asignaturaRepository, never()).save(any());
    }

    @Test
    void actualizarAsignaturaDelegaEnElMapper() {
        Asignatura existente = asignatura(true);
        ActualizarAsignaturaRequestDTO dto = actualizar();
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existente));
        when(asignaturaRepository.existsByNombreIgnoreCaseAndActiveTrueAndIdNot("Matemática II", 1L))
                .thenReturn(false);
        when(asignaturaRepository.save(existente)).thenReturn(existente);
        when(asignaturaMapper.toDto(existente)).thenReturn(response());

        service.actualizar(1L, dto);

        verify(asignaturaMapper).updateEntity(dto, existente);
        verify(asignaturaRepository).save(existente);
    }

    @Test
    void eliminarAsignaturaInexistenteLanzaNotFound() {
        when(asignaturaRepository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.eliminar(99L));
    }

    @Test
    void rechazaEliminarAsignaturaConDictacionesActivas() {
        Asignatura existente = asignatura(true);
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existente));
        when(cursoAsignaturaRepository.existsByAsignaturaIdAndActiveTrue(1L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.eliminar(1L));
        verify(asignaturaRepository, never()).save(any());
    }

    @Test
    void eliminarAsignaturaLaMarcaInactiva() {
        Asignatura existente = asignatura(true);
        when(asignaturaRepository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(existente));
        when(cursoAsignaturaRepository.existsByAsignaturaIdAndActiveTrue(1L)).thenReturn(false);
        when(asignaturaRepository.save(existente)).thenReturn(existente);

        service.eliminar(1L);

        assertFalse(existente.isActive());
        verify(asignaturaRepository).save(existente);
    }
}
