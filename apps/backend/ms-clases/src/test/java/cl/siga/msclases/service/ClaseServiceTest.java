package cl.siga.msclases.service;

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

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import cl.siga.coreshare.dto.clase.ActualizarDocenteJefeRequestDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.RegistrarClaseRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msclases.client.DocenteClient;
import cl.siga.msclases.model.entity.Clase;
import cl.siga.msclases.model.mapper.ClaseMapper;
import cl.siga.msclases.repository.ClaseRepository;

@ExtendWith(MockitoExtension.class)
class ClaseServiceTest {

    @Mock
    private ClaseRepository repository;
    @Mock
    private ClaseMapper mapper;
    @Mock
    private DocenteClient docenteClient;

    private ClaseService service;

    @BeforeEach
    void setUp() {
        service = new ClaseService(repository, mapper, docenteClient);
    }

    private Clase clase(Long id, boolean active, Long idDocenteJefe) {
        Clase clase = new Clase();
        clase.setId(id);
        clase.setNivel(Nivel.OCTAVO_BASICO);
        clase.setLetra("A");
        clase.setAnioAcademico(2026);
        clase.setActive(active);
        clase.setIdDocenteJefe(idDocenteJefe);
        return clase;
    }

    private ClaseResponseDTO response() {
        return new ClaseResponseDTO(1L, Nivel.OCTAVO_BASICO, "A", 2026, 2L);
    }

    @Test
    void getClaseByIdDevuelveLaClase() {
        Clase clase = clase(1L, true, 2L);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clase));
        when(mapper.toResponseDto(clase)).thenReturn(response());

        assertEquals(1L, service.getClaseById(1L).id());
    }

    @Test
    void getClaseByIdLanzaNotFound() {
        when(repository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getClaseById(99L));
    }

    @Test
    void searchClasesDevuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 10);
        Clase clase = clase(1L, true, 2L);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(clase), pageable, 1));
        when(mapper.toResponseDto(clase)).thenReturn(response());

        Page<ClaseResponseDTO> resultado = service.searchClases(
                Nivel.OCTAVO_BASICO, "a", 2026, 2L, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("A", resultado.getContent().get(0).letra());
    }

    @Test
    void rechazaClaseActivaDuplicada() {
        when(repository.findByNivelAndLetraAndAnioAcademico(Nivel.OCTAVO_BASICO, "A", 2026))
                .thenReturn(Optional.of(clase(1L, true, null)));

        assertThrows(BusinessException.class, () -> service.saveClase(
                new RegistrarClaseRequestDTO(Nivel.OCTAVO_BASICO, " a ", 2026, null)));
        verify(repository, never()).save(any());
    }

    @Test
    void creaClaseSinDocenteJefe() {
        when(repository.findByNivelAndLetraAndAnioAcademico(Nivel.OCTAVO_BASICO, "A", 2026))
                .thenReturn(Optional.empty());
        Clase nueva = clase(null, true, null);
        when(mapper.toEntity(any(RegistrarClaseRequestDTO.class))).thenReturn(nueva);
        when(repository.save(nueva)).thenReturn(nueva);
        when(mapper.toResponseDto(nueva)).thenReturn(response());

        service.saveClase(new RegistrarClaseRequestDTO(Nivel.OCTAVO_BASICO, "a", 2026, null));

        verify(docenteClient, never()).existsById(any());
        verify(repository).save(nueva);
    }

    @Test
    void creaClaseConDocenteJefeValido() {
        when(repository.findByNivelAndLetraAndAnioAcademico(Nivel.OCTAVO_BASICO, "A", 2026))
                .thenReturn(Optional.empty());
        when(docenteClient.existsById(2L)).thenReturn(true);
        when(repository.existsByIdDocenteJefeAndActiveTrue(2L)).thenReturn(false);
        Clase nueva = clase(null, true, 2L);
        when(mapper.toEntity(any(RegistrarClaseRequestDTO.class))).thenReturn(nueva);
        when(repository.save(nueva)).thenReturn(nueva);
        when(mapper.toResponseDto(nueva)).thenReturn(response());

        service.saveClase(new RegistrarClaseRequestDTO(Nivel.OCTAVO_BASICO, "a", 2026, 2L));

        verify(repository).save(nueva);
    }

    @Test
    void rechazaDocenteJefeInexistenteAlCrear() {
        when(repository.findByNivelAndLetraAndAnioAcademico(Nivel.OCTAVO_BASICO, "A", 2026))
                .thenReturn(Optional.empty());
        when(docenteClient.existsById(2L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.saveClase(
                new RegistrarClaseRequestDTO(Nivel.OCTAVO_BASICO, "a", 2026, 2L)));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaDocenteJefeDeOtraClaseAlCrear() {
        when(repository.findByNivelAndLetraAndAnioAcademico(Nivel.OCTAVO_BASICO, "A", 2026))
                .thenReturn(Optional.empty());
        when(docenteClient.existsById(2L)).thenReturn(true);
        when(repository.existsByIdDocenteJefeAndActiveTrue(2L)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveClase(
                new RegistrarClaseRequestDTO(Nivel.OCTAVO_BASICO, "a", 2026, 2L)));
        verify(repository, never()).save(any());
    }

    @Test
    void reactivaClaseEliminadaLogicamente() {
        Clase eliminada = clase(7L, false, null);
        when(repository.findByNivelAndLetraAndAnioAcademico(Nivel.OCTAVO_BASICO, "A", 2026))
                .thenReturn(Optional.of(eliminada));
        when(repository.save(eliminada)).thenReturn(eliminada);

        service.saveClase(new RegistrarClaseRequestDTO(Nivel.OCTAVO_BASICO, "a", 2026, null));

        assertTrue(eliminada.isActive());
        verify(repository).save(eliminada);
    }

    @Test
    void permiteReasignarElMismoDocenteJefe() {
        Clase clase = clase(1L, true, 2L);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clase));
        when(docenteClient.existsById(2L)).thenReturn(true);
        when(repository.existsByIdDocenteJefeAndActiveTrueAndIdNot(2L, 1L)).thenReturn(false);

        service.updateDocenteJefe(1L, new ActualizarDocenteJefeRequestDTO(2L));

        assertEquals(2L, clase.getIdDocenteJefe());
        verify(repository).save(clase);
    }

    @Test
    void rechazaDocenteJefeDeOtraClaseActiva() {
        Clase clase = clase(1L, true, null);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clase));
        when(docenteClient.existsById(3L)).thenReturn(true);
        when(repository.existsByIdDocenteJefeAndActiveTrueAndIdNot(3L, 1L)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.updateDocenteJefe(1L, new ActualizarDocenteJefeRequestDTO(3L)));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaDocenteJefeInexistenteAlActualizar() {
        Clase clase = clase(1L, true, null);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clase));
        when(docenteClient.existsById(3L)).thenReturn(false);

        assertThrows(BusinessException.class,
                () -> service.updateDocenteJefe(1L, new ActualizarDocenteJefeRequestDTO(3L)));
        verify(repository, never()).save(any());
    }

    @Test
    void limpiarDocenteJefeNoConsultaAlCliente() {
        Clase clase = clase(1L, true, 2L);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clase));
        when(repository.save(clase)).thenReturn(clase);

        service.updateDocenteJefe(1L, new ActualizarDocenteJefeRequestDTO(null));

        assertNull(clase.getIdDocenteJefe());
        verify(docenteClient, never()).existsById(any());
    }

    @Test
    void actualizarClaseInexistenteLanzaNotFound() {
        when(repository.findByIdAndActiveTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.updateDocenteJefe(99L, new ActualizarDocenteJefeRequestDTO(2L)));
    }

    @Test
    void existePorIdDelegaEnElRepositorio() {
        when(repository.existsByIdAndActiveTrue(1L)).thenReturn(true);
        when(repository.existsByIdAndActiveTrue(2L)).thenReturn(false);

        assertTrue(service.existsClaseById(1L));
        assertFalse(service.existsClaseById(2L));
    }

    @Test
    void eliminarMarcaLaClaseComoInactiva() {
        Clase clase = clase(1L, true, 2L);
        when(repository.findById(1L)).thenReturn(Optional.of(clase));
        when(repository.save(clase)).thenReturn(clase);

        service.deleteClase(1L);

        assertFalse(clase.isActive());
        verify(repository).save(clase);
    }

    @Test
    void eliminarClaseInexistenteLanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteClase(99L));
    }
}
