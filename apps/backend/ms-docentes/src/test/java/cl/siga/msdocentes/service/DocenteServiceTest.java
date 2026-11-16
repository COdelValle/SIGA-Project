package cl.siga.msdocentes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import cl.siga.coreshare.dto.docente.ActualizarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.docente.RegistrarDocenteRequestDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RegistrarDocenteRequestDTO request() {
        return new RegistrarDocenteRequestDTO(
                OID, "Ana", null, "Perez", null, RUT,
                LocalDate.of(2020, 3, 1), AreaAcademica.MATEMATICAS,
                List.of(new CertificadoRequestDTO("Titulo", "Instituto", LocalDate.of(2019, 1, 1))));
    }

    private Docente docente() {
        return Docente.builder()
                .id(9L)
                .idUsuario(OID)
                .rut(RUT)
                .firstName("Ana")
                .firstSurname("Perez")
                .fechaContratacion(LocalDate.of(2020, 3, 1))
                .area(AreaAcademica.MATEMATICAS)
                .activo(true)
                .build();
    }

    private DocenteResponseDTO response() {
        return new DocenteResponseDTO(9L, OID, "Ana", null, "Perez", null, RUT,
                LocalDate.of(2020, 3, 1), true, AreaAcademica.MATEMATICAS, List.of());
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

    @Test
    void reactivaPerfilSinRutNiCertificados() {
        RegistrarDocenteRequestDTO sinDatos = new RegistrarDocenteRequestDTO(
                OID, "Ana", null, "Perez", null, null,
                LocalDate.of(2020, 3, 1), AreaAcademica.MATEMATICAS, null);
        Docente existente = Docente.builder()
                .id(9L).idUsuario(OID).rut(RUT).activo(false).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.saveDocente(sinDatos);

        assertEquals(RUT, existente.getRut());
        assertTrue(existente.getCertificados().isEmpty());
    }

    @Test
    void rechazaReactivacionSiElRutNuevoYaExiste() {
        Docente existente = Docente.builder()
                .id(9L).idUsuario(OID).rut("11111111-1").activo(false).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.existsByRut(RUT)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveDocente(request()));
        verify(repository, never()).save(any());
    }

    @Test
    void creaDocenteNuevoConEstadoActivo() {
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.empty());
        when(repository.existsByRut(RUT)).thenReturn(false);
        Docente docente = docente();
        when(mapper.toEntity(any(RegistrarDocenteRequestDTO.class))).thenReturn(docente);
        when(repository.save(docente)).thenReturn(docente);
        when(mapper.toResponseDto(docente)).thenReturn(response());

        DocenteResponseDTO resultado = service.saveDocente(request());

        assertEquals(9L, resultado.id());
        verify(repository).save(docente);
    }

    @Test
    void rechazaRutDuplicadoAlCrear() {
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.empty());
        when(repository.existsByRut(RUT)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveDocente(request()));
        verify(repository, never()).save(any());
    }

    @Test
    void getDocenteByIdDevuelveElPerfil() {
        Docente docente = docente();
        when(repository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(docente));
        when(mapper.toResponseDto(docente)).thenReturn(response());

        assertEquals(9L, service.getDocenteById(9L).id());
    }

    @Test
    void getDocenteByIdLanzaNotFound() {
        when(repository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDocenteById(99L));
    }

    @Test
    void getDocenteByIdUsuarioDevuelveElPerfil() {
        Docente docente = docente();
        when(repository.findByIdUsuarioAndActivoTrue(OID)).thenReturn(Optional.of(docente));
        when(mapper.toResponseDto(docente)).thenReturn(response());

        assertEquals(OID, service.getDocenteByIdUsuario(OID).idUsuario());
    }

    @Test
    void getDocenteByIdUsuarioLanzaNotFound() {
        when(repository.findByIdUsuarioAndActivoTrue(OID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getDocenteByIdUsuario(OID));
    }

    @Test
    void searchDocentesDevuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 10);
        Docente docente = docente();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(docente), pageable, 1));
        when(mapper.toResponseDto(docente)).thenReturn(response());

        Page<DocenteResponseDTO> resultado = service.searchDocentes(
                RUT, "Ana", "Perez", null, null, AreaAcademica.MATEMATICAS, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Ana", resultado.getContent().get(0).firstName());
    }

    @Test
    void actualizarComoAdminDelegaEnElMapper() {
        autenticar(OID, "ADMIN");
        Docente docente = docente();
        ActualizarDocenteRequestDTO dto = new ActualizarDocenteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2021, 3, 1), AreaAcademica.CIENCIAS);
        when(repository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(docente));
        when(repository.save(docente)).thenReturn(docente);
        when(mapper.toResponseDto(docente)).thenReturn(response());

        service.updateDocente(9L, dto);

        verify(mapper).updateEntityFromDto(dto, docente);
        verify(repository).save(docente);
    }

    @Test
    void actualizarComoPropietarioPermiteElCambio() {
        autenticar(OID, "DOCENTE");
        Docente docente = docente();
        ActualizarDocenteRequestDTO dto = new ActualizarDocenteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2021, 3, 1), AreaAcademica.CIENCIAS);
        when(repository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(docente));
        when(repository.save(docente)).thenReturn(docente);
        when(mapper.toResponseDto(docente)).thenReturn(response());

        service.updateDocente(9L, dto);

        verify(repository).save(docente);
    }

    @Test
    void actualizarComoOtroUsuarioEsRechazado() {
        autenticar("otro-oid", "DOCENTE");
        Docente docente = docente();
        ActualizarDocenteRequestDTO dto = new ActualizarDocenteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2021, 3, 1), AreaAcademica.CIENCIAS);
        when(repository.findByIdAndActivoTrue(9L)).thenReturn(Optional.of(docente));

        assertThrows(AccessDeniedException.class, () -> service.updateDocente(9L, dto));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizarDocenteInexistenteLanzaNotFound() {
        ActualizarDocenteRequestDTO dto = new ActualizarDocenteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2021, 3, 1), AreaAcademica.CIENCIAS);
        when(repository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateDocente(99L, dto));
    }

    @Test
    void existePorIdDelegaEnElRepositorio() {
        when(repository.existsByIdAndActivoTrue(9L)).thenReturn(true);
        when(repository.existsByIdAndActivoTrue(10L)).thenReturn(false);

        assertTrue(service.existsDocenteById(9L));
        assertFalse(service.existsDocenteById(10L));
    }

    @Test
    void eliminarMarcaElPerfilComoInactivo() {
        Docente docente = docente();
        when(repository.findById(9L)).thenReturn(Optional.of(docente));
        when(repository.save(docente)).thenReturn(docente);

        service.deleteDocente(9L);

        assertEquals(Boolean.FALSE, docente.getActivo());
        verify(repository).save(docente);
    }

    @Test
    void eliminarDocenteInexistenteLanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteDocente(99L));
    }
}
