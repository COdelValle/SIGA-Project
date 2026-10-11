package cl.siga.msdocentes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.siga.coreshare.dto.docente.RegistrarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msdocentes.model.entity.Certificado;
import cl.siga.msdocentes.model.entity.Docente;
import cl.siga.msdocentes.model.mapper.CertificadoMapper;
import cl.siga.msdocentes.model.mapper.DocenteMapper;
import cl.siga.msdocentes.repository.DocenteRepository;

@ExtendWith(MockitoExtension.class)
class DocenteServiceTest {

    private static final String OID = "12345678-1234-1234-1234-123456789012";
    private static final String RUT = "21000002-K";

    @Mock
    private DocenteRepository repository;
    @Mock
    private DocenteMapper mapper;
    @Mock
    private CertificadoMapper certificadoMapper;

    private DocenteService service;

    @BeforeEach
    void setUp() {
        service = new DocenteService(repository, mapper, certificadoMapper);
    }

    private RegistrarDocenteRequestDTO request() {
        return new RegistrarDocenteRequestDTO(
                OID, "Ana", null, "Perez", null, RUT,
                LocalDate.of(2020, 3, 1), AreaAcademica.MATEMATICAS,
                List.of(new CertificadoRequestDTO("Titulo", "Instituto", LocalDate.of(2019, 1, 1))));
    }

    @Test
    void reactivaPerfilInactivoYReemplazaCertificados() {
        Docente existente = Docente.builder()
                .id(9L).idUsuario(OID).rut(RUT).activo(false).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        when(certificadoMapper.toEntity(any())).thenReturn(new Certificado());

        service.saveDocente(request());

        assertEquals(Boolean.TRUE, existente.getActivo());
        assertEquals(AreaAcademica.MATEMATICAS, existente.getArea());
        assertEquals(1, existente.getCertificados().size());
        verify(repository, never()).existsByRut(any());
    }

    @Test
    void rechazaPerfilActivoDuplicado() {
        Docente existente = Docente.builder()
                .id(9L).idUsuario(OID).rut(RUT).activo(true).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> service.saveDocente(request()));
        verify(repository, never()).save(any());
    }
}
