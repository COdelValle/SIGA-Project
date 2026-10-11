package cl.siga.msclases.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.clase.ActualizarDocenteJefeRequestDTO;
import cl.siga.coreshare.dto.clase.RegistrarClaseRequestDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msclases.client.DocenteClient;
import cl.siga.msclases.model.entity.Clase;
import cl.siga.msclases.model.mapper.ClaseMapper;
import cl.siga.msclases.repository.ClaseRepository;

class ClaseServiceTest {

    private ClaseRepository repository;
    private ClaseMapper mapper;
    private DocenteClient docenteClient;
    private ClaseService service;

    @BeforeEach
    void setUp() {
        repository = mock(ClaseRepository.class);
        mapper = mock(ClaseMapper.class);
        docenteClient = mock(DocenteClient.class);
        service = new ClaseService(repository, mapper, docenteClient);
    }

    @Test
    void permiteReasignarElMismoDocenteJefe() {
        Clase clase = new Clase();
        clase.setId(1L);
        clase.setActive(true);
        clase.setIdDocenteJefe(2L);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clase));
        when(docenteClient.existsById(2L)).thenReturn(true);
        when(repository.existsByIdDocenteJefeAndActiveTrueAndIdNot(2L, 1L)).thenReturn(false);

        service.updateDocenteJefe(1L, new ActualizarDocenteJefeRequestDTO(2L));

        assertEquals(2L, clase.getIdDocenteJefe());
        verify(repository).save(clase);
    }

    @Test
    void rechazaDocenteJefeDeOtraClaseActiva() {
        Clase clase = new Clase();
        clase.setId(1L);
        clase.setActive(true);
        when(repository.findByIdAndActiveTrue(1L)).thenReturn(Optional.of(clase));
        when(docenteClient.existsById(3L)).thenReturn(true);
        when(repository.existsByIdDocenteJefeAndActiveTrueAndIdNot(3L, 1L)).thenReturn(true);

        assertThrows(BusinessException.class,
                () -> service.updateDocenteJefe(1L, new ActualizarDocenteJefeRequestDTO(3L)));
        verify(repository, never()).save(any());
    }

    @Test
    void reactivaClaseEliminadaLogicamente() {
        Clase eliminada = new Clase();
        eliminada.setId(7L);
        eliminada.setActive(false);
        when(repository.findByNivelAndLetraAndAnioAcademico(Nivel.OCTAVO_BASICO, "A", 2026))
                .thenReturn(Optional.of(eliminada));
        when(repository.save(eliminada)).thenReturn(eliminada);

        service.saveClase(new RegistrarClaseRequestDTO(Nivel.OCTAVO_BASICO, "a", 2026, null));

        assertTrue(eliminada.isActive());
        verify(repository).save(eliminada);
    }
}
