package cl.siga.msestudiantes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msestudiantes.client.ApoderadoClient;
import cl.siga.msestudiantes.model.entity.Estudiante;
import cl.siga.msestudiantes.model.mapper.EstudianteMapper;
import cl.siga.msestudiantes.repository.EstudianteRepository;

@ExtendWith(MockitoExtension.class)
class EstudianteServiceTest {

    private static final String OID = "12345678-1234-1234-1234-123456789012";
    private static final String RUT = "21000001-1";

    @Mock
    private EstudianteRepository repository;
    @Mock
    private EstudianteMapper mapper;
    @Mock
    private ApoderadoClient apoderadoClient;

    private EstudianteService service;

    @BeforeEach
    void setUp() {
        service = new EstudianteService(repository, mapper, apoderadoClient);
    }

    private RegistrarEstudianteRequestDTO request() {
        return new RegistrarEstudianteRequestDTO(
                OID, "Ana", null, "Perez", null, RUT,
                LocalDate.of(2012, 5, 1), null, 7L);
    }

    @Test
    void reactivaPerfilInactivoConLosDatosRecibidos() {
        Estudiante existente = Estudiante.builder()
                .id(9L).idUsuario(OID).rut(RUT).state(State.INACTIVO).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.saveEstudiante(request());

        assertEquals(State.REGISTRADO, existente.getState());
        assertEquals("Ana", existente.getFirstName());
        assertEquals(7L, existente.getIdClase());
        verify(repository, never()).existsByRut(any());
    }

    @Test
    void rechazaPerfilActivoDuplicado() {
        Estudiante existente = Estudiante.builder()
                .id(9L).idUsuario(OID).rut(RUT).state(State.REGISTRADO).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> service.saveEstudiante(request()));
        verify(repository, never()).save(any());
    }
}
