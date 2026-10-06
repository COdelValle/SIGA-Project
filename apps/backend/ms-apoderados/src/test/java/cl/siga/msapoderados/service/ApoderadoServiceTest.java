package cl.siga.msapoderados.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import cl.siga.coreshare.dto.apoderado.RegistrarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msapoderados.client.EstudianteClient;
import cl.siga.msapoderados.model.entity.Apoderado;
import cl.siga.msapoderados.model.entity.ApoderadoEstudiante;
import cl.siga.msapoderados.model.mapper.ApoderadoMapper;
import cl.siga.msapoderados.repository.ApoderadoRepository;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApoderadoServiceTest {

    private static final String OID = "12345678-1234-1234-1234-123456789012";
    private static final String RUT = "21000003-8";

    @Mock
    private ApoderadoRepository repository;
    @Mock
    private ApoderadoMapper mapper;
    @Mock
    private EstudianteClient estudianteClient;

    private ApoderadoService service;

    @BeforeEach
    void setUp() {
        service = new ApoderadoService(repository, mapper, estudianteClient);
    }

    private RegistrarApoderadoRequestDTO request() {
        return new RegistrarApoderadoRequestDTO(
                OID, "Luis", null, "Soto", null, RUT,
                List.of("+56912345678"),
                List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE)));
    }

    @Test
    void reactivaPerfilInactivoConVinculosYTelefonos() {
        Apoderado existente = Apoderado.builder()
                .id(5L).idUsuario(OID).rut(RUT).activo(false).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toApoderadoEstudianteEntity(any())).thenReturn(
                ApoderadoEstudiante.builder().idEstudiante(1L).parentesco(Parentesco.MADRE_PADRE).build());

        // Flujo asíncrono: los pupilos ya se validaron al aceptar la solicitud.
        service.saveApoderadoDesdeEvento(request());

        assertEquals(Boolean.TRUE, existente.getActivo());
        assertEquals(1, existente.getTelefonos().size());
        assertEquals(1, existente.getEstudiantes().size());
        assertEquals(1L, existente.getEstudiantes().get(0).getIdEstudiante());
    }

    @Test
    void rechazaPerfilActivoDuplicado() {
        Apoderado existente = Apoderado.builder()
                .id(5L).idUsuario(OID).rut(RUT).activo(true).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> service.saveApoderadoDesdeEvento(request()));
        verify(repository, never()).save(any());
    }
}
