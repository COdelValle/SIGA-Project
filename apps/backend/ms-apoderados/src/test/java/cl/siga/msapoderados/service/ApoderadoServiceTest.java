package cl.siga.msapoderados.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
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

import cl.siga.coreshare.dto.apoderado.ActualizarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.RegistrarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RegistrarApoderadoRequestDTO request() {
        return new RegistrarApoderadoRequestDTO(
                OID, "Luis", null, "Soto", null, RUT,
                List.of("+56912345678"),
                List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE)));
    }

    private Apoderado apoderado() {
        return Apoderado.builder()
                .id(5L)
                .idUsuario(OID)
                .rut(RUT)
                .firstName("Luis")
                .firstSurname("Soto")
                .telefonos(new ArrayList<>(List.of("+56912345678")))
                .estudiantes(new ArrayList<>(List.of(
                        ApoderadoEstudiante.builder().idEstudiante(1L).parentesco(Parentesco.MADRE_PADRE).build())))
                .activo(true)
                .build();
    }

    private ApoderadoResponseDTO response() {
        return new ApoderadoResponseDTO(5L, OID, "Luis", null, "Soto", null, RUT,
                List.of("+56912345678"),
                List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE)), true);
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
    void reactivaPerfilInactivoConVinculosYTelefonos() {
        Apoderado existente = Apoderado.builder()
                .id(5L).idUsuario(OID).rut(RUT).activo(false).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toApoderadoEstudianteEntity(any())).thenReturn(
                ApoderadoEstudiante.builder().idEstudiante(1L).parentesco(Parentesco.MADRE_PADRE).build());

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

    @Test
    void getApoderadoByIdDevuelveElPerfil() {
        Apoderado apoderado = apoderado();
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(apoderado));
        when(mapper.toResponseDto(apoderado)).thenReturn(response());

        assertEquals(5L, service.getApoderadoById(5L).id());
    }

    @Test
    void getApoderadoByIdLanzaNotFound() {
        when(repository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getApoderadoById(99L));
    }

    @Test
    void getApoderadoByIdUsuarioDevuelveElPerfil() {
        Apoderado apoderado = apoderado();
        when(repository.findByIdUsuarioAndActivoTrue(OID)).thenReturn(Optional.of(apoderado));
        when(mapper.toResponseDto(apoderado)).thenReturn(response());

        assertEquals(OID, service.getApoderadoByIdUsuario(OID).idUsuario());
    }

    @Test
    void getApoderadoByIdUsuarioLanzaNotFound() {
        when(repository.findByIdUsuarioAndActivoTrue(OID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getApoderadoByIdUsuario(OID));
    }

    @Test
    void searchApoderadosDevuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 10);
        Apoderado apoderado = apoderado();
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(apoderado), pageable, 1));
        when(mapper.toResponseDto(apoderado)).thenReturn(response());

        Page<ApoderadoResponseDTO> resultado = service.searchApoderados(
                RUT, "Luis", "Soto", 1L, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals(OID, resultado.getContent().get(0).idUsuario());
    }

    @Test
    void creaApoderadoNuevoValidandoElEstudiante() {
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.empty());
        when(repository.existsByRut(RUT)).thenReturn(false);
        when(estudianteClient.existsById(1L)).thenReturn(true);
        Apoderado apoderado = apoderado();
        when(mapper.toEntity(any(RegistrarApoderadoRequestDTO.class))).thenReturn(apoderado);
        when(repository.save(apoderado)).thenReturn(apoderado);
        when(mapper.toResponseDto(apoderado)).thenReturn(response());

        ApoderadoResponseDTO resultado = service.saveApoderado(request());

        assertEquals(5L, resultado.id());
        verify(estudianteClient).existsById(1L);
        verify(repository).save(apoderado);
    }

    @Test
    void creaApoderadoDesdeEventoSinValidarEstudiantes() {
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.empty());
        when(repository.existsByRut(RUT)).thenReturn(false);
        Apoderado apoderado = apoderado();
        when(mapper.toEntity(any(RegistrarApoderadoRequestDTO.class))).thenReturn(apoderado);
        when(repository.save(apoderado)).thenReturn(apoderado);
        when(mapper.toResponseDto(apoderado)).thenReturn(response());

        service.saveApoderadoDesdeEvento(request());

        verify(estudianteClient, never()).existsById(any());
    }

    @Test
    void rechazaRutDuplicadoAlCrear() {
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.empty());
        when(repository.existsByRut(RUT)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveApoderado(request()));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaEstudianteInexistenteAlCrear() {
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.empty());
        when(repository.existsByRut(RUT)).thenReturn(false);
        when(estudianteClient.existsById(1L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.saveApoderado(request()));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaReactivacionSiElRutNuevoYaExiste() {
        Apoderado existente = Apoderado.builder()
                .id(5L).idUsuario(OID).rut("11111111-1").activo(false).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.existsByRut(RUT)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveApoderadoDesdeEvento(request()));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizarComoAdminDelegaEnElMapper() {
        autenticar(OID, "ADMIN");
        Apoderado existente = apoderado();
        ActualizarApoderadoRequestDTO dto = new ActualizarApoderadoRequestDTO(
                "Luis Alberto", null, "Soto", null, List.of("+56987654321"));
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDto(existente)).thenReturn(response());

        service.updateApoderado(5L, dto);

        verify(mapper).updateEntityFromDto(dto, existente);
        verify(repository).save(existente);
    }

    @Test
    void actualizarComoPropietarioPermiteElCambio() {
        autenticar(OID, "APODERADO");
        Apoderado existente = apoderado();
        ActualizarApoderadoRequestDTO dto = new ActualizarApoderadoRequestDTO(
                "Luis Alberto", null, "Soto", null, List.of("+56987654321"));
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDto(existente)).thenReturn(response());

        service.updateApoderado(5L, dto);

        verify(repository).save(existente);
    }

    @Test
    void actualizarComoOtroUsuarioEsRechazado() {
        autenticar("otro-oid", "APODERADO");
        Apoderado existente = apoderado();
        ActualizarApoderadoRequestDTO dto = new ActualizarApoderadoRequestDTO(
                "Luis Alberto", null, "Soto", null, List.of("+56987654321"));
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));

        assertThrows(AccessDeniedException.class, () -> service.updateApoderado(5L, dto));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizarApoderadoInexistenteLanzaNotFound() {
        ActualizarApoderadoRequestDTO dto = new ActualizarApoderadoRequestDTO(
                "Luis Alberto", null, "Soto", null, List.of("+56987654321"));
        when(repository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateApoderado(99L, dto));
    }

    @Test
    void agregaEstudianteNoVinculado() {
        Apoderado existente = apoderado();
        ParentescoEstudianteDTO nuevo = new ParentescoEstudianteDTO(2L, Parentesco.TIA_TIO);
        ApoderadoEstudiante vinculo = ApoderadoEstudiante.builder()
                .idEstudiante(2L).parentesco(Parentesco.TIA_TIO).build();
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));
        when(estudianteClient.existsById(2L)).thenReturn(true);
        when(mapper.toApoderadoEstudianteEntity(nuevo)).thenReturn(vinculo);
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDto(existente)).thenReturn(response());

        service.addEstudiante(5L, nuevo);

        assertEquals(2, existente.getEstudiantes().size());
        assertTrue(existente.getEstudiantes().stream().anyMatch(e -> e.getIdEstudiante().equals(2L)));
    }

    @Test
    void rechazaEstudianteYaVinculado() {
        Apoderado existente = apoderado();
        ParentescoEstudianteDTO repetido = new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE);
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> service.addEstudiante(5L, repetido));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaAgregarEstudianteInexistente() {
        Apoderado existente = apoderado();
        ParentescoEstudianteDTO nuevo = new ParentescoEstudianteDTO(2L, Parentesco.TIA_TIO);
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));
        when(estudianteClient.existsById(2L)).thenReturn(false);

        assertThrows(BusinessException.class, () -> service.addEstudiante(5L, nuevo));
        verify(repository, never()).save(any());
    }

    @Test
    void agregarEstudianteAApoderadoInexistenteLanzaNotFound() {
        when(repository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> service.addEstudiante(99L, new ParentescoEstudianteDTO(2L, Parentesco.TIA_TIO)));
    }

    @Test
    void remueveEstudianteCuandoQuedanOtrosVinculos() {
        Apoderado existente = apoderado();
        existente.getEstudiantes().add(ApoderadoEstudiante.builder()
                .idEstudiante(2L).parentesco(Parentesco.TIA_TIO).build());
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDto(existente)).thenReturn(response());

        service.removeEstudiante(5L, 2L);

        assertEquals(1, existente.getEstudiantes().size());
        assertEquals(1L, existente.getEstudiantes().get(0).getIdEstudiante());
    }

    @Test
    void rechazaRemoverElUnicoEstudiante() {
        Apoderado existente = apoderado();
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));

        assertThrows(BusinessException.class, () -> service.removeEstudiante(5L, 1L));
        verify(repository, never()).save(any());
    }

    @Test
    void removerVinculoInexistenteLanzaNotFound() {
        Apoderado existente = apoderado();
        existente.getEstudiantes().add(ApoderadoEstudiante.builder()
                .idEstudiante(2L).parentesco(Parentesco.TIA_TIO).build());
        when(repository.findByIdAndActivoTrue(5L)).thenReturn(Optional.of(existente));

        assertThrows(ResourceNotFoundException.class, () -> service.removeEstudiante(5L, 99L));
    }

    @Test
    void removerEstudianteDeApoderadoInexistenteLanzaNotFound() {
        when(repository.findByIdAndActivoTrue(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.removeEstudiante(99L, 1L));
    }

    @Test
    void existePorIdDelegaEnElRepositorio() {
        when(repository.existsByIdAndActivoTrue(5L)).thenReturn(true);
        when(repository.existsByIdAndActivoTrue(6L)).thenReturn(false);

        assertTrue(service.existsApoderadoById(5L));
        assertFalse(service.existsApoderadoById(6L));
    }

    @Test
    void eliminarMarcaElPerfilComoInactivo() {
        Apoderado existente = apoderado();
        when(repository.findById(5L)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.deleteApoderado(5L);

        assertEquals(Boolean.FALSE, existente.getActivo());
        verify(repository).save(existente);
    }

    @Test
    void eliminarApoderadoInexistenteLanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteApoderado(99L));
    }
}
