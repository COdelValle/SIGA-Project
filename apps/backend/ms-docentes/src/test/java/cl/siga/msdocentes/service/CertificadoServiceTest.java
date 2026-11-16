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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msdocentes.model.entity.Certificado;
import cl.siga.msdocentes.model.entity.Docente;
import cl.siga.msdocentes.model.mapper.CertificadoMapper;
import cl.siga.msdocentes.repository.CertificadoRepository;
import cl.siga.msdocentes.repository.DocenteRepository;

@ExtendWith(MockitoExtension.class)
class CertificadoServiceTest {

    private static final String OID = "12345678-1234-1234-1234-123456789012";

    @Mock
    private CertificadoRepository certificadoRepository;
    @Mock
    private DocenteRepository docenteRepository;
    @Mock
    private CertificadoMapper mapper;

    private CertificadoService service;

    @BeforeEach
    void setUp() {
        service = new CertificadoService(certificadoRepository, docenteRepository, mapper);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Docente docente() {
        return Docente.builder()
                .id(9L)
                .idUsuario(OID)
                .rut("21000002-K")
                .firstName("Ana")
                .firstSurname("Perez")
                .fechaContratacion(LocalDate.of(2020, 3, 1))
                .area(AreaAcademica.MATEMATICAS)
                .activo(true)
                .build();
    }

    private Certificado certificado(Long id, Docente docente) {
        return Certificado.builder()
                .id(id)
                .nombre("Titulo")
                .institucionRealizacion("Instituto")
                .fechaTitulacion(LocalDate.of(2019, 1, 1))
                .docente(docente)
                .build();
    }

    private CertificadoResponseDTO response() {
        return new CertificadoResponseDTO(1L, "Titulo", "Instituto", LocalDate.of(2019, 1, 1));
    }

    private void autenticar(String oid, String... roles) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("oid", oid)
                .build();
        List<GrantedAuthority> authorities = java.util.Arrays.stream(roles)
                .map(rol -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + rol))
                .toList();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt, authorities));
    }

    @Test
    void listaCertificadosDeUnDocenteActivo() {
        when(docenteRepository.existsByIdAndActivoTrue(9L)).thenReturn(true);
        when(certificadoRepository.findByDocenteId(9L)).thenReturn(List.of(certificado(1L, docente())));
        when(mapper.toResponseDtoList(any())).thenReturn(List.of(response()));

        List<CertificadoResponseDTO> resultado = service.getCertificadosByDocenteId(9L);

        assertEquals(1, resultado.size());
        assertEquals("Titulo", resultado.get(0).nombre());
    }

    @Test
    void listaCertificadosDeDocenteInexistenteLanzaNotFound() {
        when(docenteRepository.existsByIdAndActivoTrue(99L)).thenReturn(false);

        assertThrows(ResourceNotFoundException.class, () -> service.getCertificadosByDocenteId(99L));
    }

    @Test
    void agregaCertificadoComoAdmin() {
        autenticar(OID, "ADMIN");
        Docente docente = docente();
        CertificadoRequestDTO dto = new CertificadoRequestDTO(
                "Magister", "Universidad", LocalDate.of(2021, 1, 1));
        Certificado certificado = certificado(null, null);
        when(docenteRepository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(docente));
        when(mapper.toEntity(dto)).thenReturn(certificado);
        when(certificadoRepository.save(certificado)).thenReturn(certificado);
        when(mapper.toResponseDto(certificado)).thenReturn(response());

        CertificadoResponseDTO resultado = service.addCertificado(9L, dto);

        assertEquals("Titulo", resultado.nombre());
        assertEquals(docente, certificado.getDocente());
    }

    @Test
    void agregaCertificadoComoPropietario() {
        autenticar(OID, "DOCENTE");
        Docente docente = docente();
        CertificadoRequestDTO dto = new CertificadoRequestDTO(
                "Magister", "Universidad", LocalDate.of(2021, 1, 1));
        Certificado certificado = certificado(null, null);
        when(docenteRepository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(docente));
        when(mapper.toEntity(dto)).thenReturn(certificado);
        when(certificadoRepository.save(certificado)).thenReturn(certificado);
        when(mapper.toResponseDto(certificado)).thenReturn(response());

        service.addCertificado(9L, dto);

        assertEquals(docente, certificado.getDocente());
    }

    @Test
    void agregaCertificadoAOtroDocenteEsRechazado() {
        autenticar("otro-oid", "DOCENTE");
        Docente docente = docente();
        when(docenteRepository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(docente));

        assertThrows(AccessDeniedException.class, () -> service.addCertificado(9L,
                new CertificadoRequestDTO("Magister", "Universidad", LocalDate.of(2021, 1, 1))));
        verify(certificadoRepository, never()).save(any());
    }

    @Test
    void agregaCertificadoADocenteInexistenteLanzaNotFound() {
        when(docenteRepository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.addCertificado(99L,
                new CertificadoRequestDTO("Magister", "Universidad", LocalDate.of(2021, 1, 1))));
    }

    @Test
    void eliminaCertificadoCuandoElDocenteMantieneOtros() {
        autenticar(OID, "ADMIN");
        Docente docente = docente();
        Certificado certificado = certificado(3L, docente);
        when(certificadoRepository.findById(3L)).thenReturn(Optional.of(certificado));
        when(certificadoRepository.countByDocenteId(9L)).thenReturn(2L);

        service.deleteCertificado(9L, 3L);

        verify(certificadoRepository).delete(certificado);
    }

    @Test
    void eliminarCertificadoInexistenteLanzaNotFound() {
        when(certificadoRepository.findById(3L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteCertificado(9L, 3L));
    }

    @Test
    void eliminarCertificadoDeOtroDocenteLanzaNotFound() {
        Certificado certificado = certificado(3L, docente());
        when(certificadoRepository.findById(3L)).thenReturn(Optional.of(certificado));

        assertThrows(ResourceNotFoundException.class, () -> service.deleteCertificado(99L, 3L));
    }

    @Test
    void eliminarCertificadoSinPermisoEsRechazado() {
        autenticar("otro-oid", "DOCENTE");
        Certificado certificado = certificado(3L, docente());
        when(certificadoRepository.findById(3L)).thenReturn(Optional.of(certificado));

        assertThrows(AccessDeniedException.class, () -> service.deleteCertificado(9L, 3L));
        verify(certificadoRepository, never()).delete(any());
    }

    @Test
    void noPermiteEliminarElUltimoCertificado() {
        autenticar(OID, "ADMIN");
        Certificado certificado = certificado(3L, docente());
        when(certificadoRepository.findById(3L)).thenReturn(Optional.of(certificado));
        when(certificadoRepository.countByDocenteId(9L)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.deleteCertificado(9L, 3L));
        verify(certificadoRepository, never()).delete(any());
    }
}
