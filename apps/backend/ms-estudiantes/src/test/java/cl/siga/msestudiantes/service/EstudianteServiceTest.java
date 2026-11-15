package cl.siga.msestudiantes.service;

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

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
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

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private RegistrarEstudianteRequestDTO request() {
        return new RegistrarEstudianteRequestDTO(
                OID, "Ana", null, "Perez", null, RUT,
                LocalDate.of(2012, 5, 1), null, 7L);
    }

    private Estudiante estudiante() {
        return Estudiante.builder()
                .id(1L)
                .idUsuario(OID)
                .rut(RUT)
                .firstName("Ana")
                .firstSurname("Perez")
                .birthDate(LocalDate.of(2012, 5, 1))
                .state(State.REGISTRADO)
                .build();
    }

    private EstudianteResponseDTO response() {
        return new EstudianteResponseDTO(1L, OID, RUT, "Ana", null, "Perez", null,
                LocalDate.of(2012, 5, 1), null, "REGISTRADO", 7L);
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

    @Test
    void getEstudianteByIdDevuelveElPerfil() {
        Estudiante estudiante = estudiante();
        when(repository.findByIdAndStateNot(1L, State.INACTIVO)).thenReturn(Optional.of(estudiante));
        when(mapper.toResponseDto(estudiante)).thenReturn(response());

        EstudianteResponseDTO resultado = service.getEstudianteById(1L);

        assertEquals(1L, resultado.id());
        assertEquals(RUT, resultado.rut());
    }

    @Test
    void getEstudianteByIdLanzaErrorSiNoExiste() {
        when(repository.findByIdAndStateNot(99L, State.INACTIVO)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEstudianteById(99L));
    }

    @Test
    void getEstudianteByIdUsuarioDevuelveElPerfil() {
        Estudiante estudiante = estudiante();
        when(repository.findByIdUsuarioAndStateNot(OID, State.INACTIVO)).thenReturn(Optional.of(estudiante));
        when(mapper.toResponseDto(estudiante)).thenReturn(response());

        assertEquals(OID, service.getEstudianteByIdUsuario(OID).idUsuario());
    }

    @Test
    void getEstudianteByIdUsuarioLanzaErrorSiNoExiste() {
        when(repository.findByIdUsuarioAndStateNot(OID, State.INACTIVO)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getEstudianteByIdUsuario(OID));
    }

    @Test
    void searchEstudiantesDevuelvePaginaMapeada() {
        Pageable pageable = PageRequest.of(0, 10);
        Estudiante estudiante = estudiante();
        Page<Estudiante> page = new PageImpl<>(List.of(estudiante), pageable, 1);
        when(repository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);
        when(mapper.toResponseDto(estudiante)).thenReturn(response());

        Page<EstudianteResponseDTO> resultado = service.searchEstudiantes(
                "ana", RUT, "Ana", null, "Perez", null, null, null, null, 7L, pageable);

        assertEquals(1, resultado.getTotalElements());
        assertEquals("Ana", resultado.getContent().get(0).firstName());
    }

    @Test
    void searchEstudiantesRespetaElEstadoExplicito() {
        Pageable pageable = PageRequest.of(0, 10);
        when(repository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<EstudianteResponseDTO> resultado = service.searchEstudiantes(
                null, null, null, null, null, null, null, null, State.INACTIVO, null, pageable);

        assertTrue(resultado.isEmpty());
    }

    @Test
    void guardaEstudianteNuevoConEstadoRegistrado() {
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.empty());
        when(repository.existsByRut(RUT)).thenReturn(false);
        Estudiante entidad = estudiante();
        when(mapper.toEntity(any(RegistrarEstudianteRequestDTO.class))).thenReturn(entidad);
        when(repository.save(entidad)).thenReturn(entidad);
        when(mapper.toResponseDto(entidad)).thenReturn(response());

        EstudianteResponseDTO resultado = service.saveEstudiante(request());

        assertEquals(State.REGISTRADO, entidad.getState());
        assertEquals(1L, resultado.id());
        verify(repository).save(entidad);
    }

    @Test
    void rechazaRutDuplicadoAlCrear() {
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.empty());
        when(repository.existsByRut(RUT)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveEstudiante(request()));
        verify(repository, never()).save(any());
    }

    @Test
    void rechazaReactivacionSiElRutNuevoYaExiste() {
        Estudiante existente = Estudiante.builder()
                .id(9L).idUsuario(OID).rut("11111111-1").state(State.INACTIVO).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.existsByRut(RUT)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveEstudiante(request()));
        verify(repository, never()).save(any());
    }

    @Test
    void reactivaPerfilSinRutNormalizado() {
        RegistrarEstudianteRequestDTO sinRut = new RegistrarEstudianteRequestDTO(
                OID, "Ana", null, "Perez", null, null,
                LocalDate.of(2012, 5, 1), null, null);
        Estudiante existente = Estudiante.builder()
                .id(9L).idUsuario(OID).rut(RUT).state(State.INACTIVO).build();
        when(repository.findByIdUsuario(OID)).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        service.saveEstudiante(sinRut);

        assertEquals(State.REGISTRADO, existente.getState());
        assertEquals(RUT, existente.getRut());
    }

    @Test
    void actualizarComoAdminActualizaElPerfil() {
        autenticar(OID, "ADMIN");
        Estudiante estudiante = estudiante();
        ActualizarEstudianteRequestDTO dto = new ActualizarEstudianteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2012, 5, 1), null, 8L);
        when(repository.findByIdAndStateNot(1L, State.INACTIVO)).thenReturn(Optional.of(estudiante));
        when(repository.save(estudiante)).thenReturn(estudiante);
        when(mapper.toResponseDto(estudiante)).thenReturn(response());

        service.updateEstudiante(1L, dto);

        verify(mapper).updateEntityFromDto(dto, estudiante);
        verify(repository).save(estudiante);
    }

    @Test
    void actualizarComoPropioEstudiantePermiteElCambio() {
        autenticar(OID, "ESTUDIANTE");
        Estudiante estudiante = estudiante();
        ActualizarEstudianteRequestDTO dto = new ActualizarEstudianteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2012, 5, 1), null, 8L);
        when(repository.findByIdAndStateNot(1L, State.INACTIVO)).thenReturn(Optional.of(estudiante));
        when(repository.save(estudiante)).thenReturn(estudiante);
        when(mapper.toResponseDto(estudiante)).thenReturn(response());

        service.updateEstudiante(1L, dto);

        verify(repository).save(estudiante);
    }

    @Test
    void actualizarComoApoderadoVinculadoPermiteElCambio() {
        autenticar("apoderado-oid", "APODERADO");
        Estudiante estudiante = estudiante();
        ActualizarEstudianteRequestDTO dto = new ActualizarEstudianteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2012, 5, 1), null, 8L);
        when(repository.findByIdAndStateNot(1L, State.INACTIVO)).thenReturn(Optional.of(estudiante));
        when(apoderadoClient.getApoderadoByIdUsuario("apoderado-oid")).thenReturn(
                new ApoderadoResponseDTO(3L, "apoderado-oid", "Luis", null, "Soto", null,
                        "11111111-1", List.of("+56912345678"),
                        List.of(new ParentescoEstudianteDTO(1L, Parentesco.MADRE_PADRE)), true));
        when(repository.save(estudiante)).thenReturn(estudiante);
        when(mapper.toResponseDto(estudiante)).thenReturn(response());

        service.updateEstudiante(1L, dto);

        verify(apoderadoClient).getApoderadoByIdUsuario("apoderado-oid");
        verify(repository).save(estudiante);
    }

    @Test
    void actualizarComoApoderadoNoVinculadoEsRechazado() {
        autenticar("apoderado-oid", "APODERADO");
        Estudiante estudiante = estudiante();
        ActualizarEstudianteRequestDTO dto = new ActualizarEstudianteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2012, 5, 1), null, 8L);
        when(repository.findByIdAndStateNot(1L, State.INACTIVO)).thenReturn(Optional.of(estudiante));
        when(apoderadoClient.getApoderadoByIdUsuario("apoderado-oid")).thenReturn(
                new ApoderadoResponseDTO(3L, "apoderado-oid", "Luis", null, "Soto", null,
                        "11111111-1", List.of("+56912345678"),
                        List.of(new ParentescoEstudianteDTO(99L, Parentesco.MADRE_PADRE)), true));

        assertThrows(AccessDeniedException.class, () -> service.updateEstudiante(1L, dto));
        verify(repository, never()).save(any());
    }

    @Test
    void actualizarSinUsuarioAutenticadoEsRechazado() {
        Estudiante estudiante = estudiante();
        ActualizarEstudianteRequestDTO dto = new ActualizarEstudianteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2012, 5, 1), null, 8L);
        when(repository.findByIdAndStateNot(1L, State.INACTIVO)).thenReturn(Optional.of(estudiante));

        assertThrows(AccessDeniedException.class, () -> service.updateEstudiante(1L, dto));
    }

    @Test
    void actualizarEstudianteInexistenteLanzaNotFound() {
        ActualizarEstudianteRequestDTO dto = new ActualizarEstudianteRequestDTO(
                "Ana Maria", null, "Perez", null, LocalDate.of(2012, 5, 1), null, 8L);
        when(repository.findByIdAndStateNot(99L, State.INACTIVO)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.updateEstudiante(99L, dto));
    }

    @Test
    void existePorIdDelegaEnElRepositorio() {
        when(repository.existsByIdAndStateNot(1L, State.INACTIVO)).thenReturn(true);
        when(repository.existsByIdAndStateNot(2L, State.INACTIVO)).thenReturn(false);

        assertTrue(service.existsEstudianteById(1L));
        assertFalse(service.existsEstudianteById(2L));
    }

    @Test
    void eliminarMarcaElPerfilComoInactivo() {
        Estudiante estudiante = estudiante();
        when(repository.findById(1L)).thenReturn(Optional.of(estudiante));
        when(repository.save(estudiante)).thenReturn(estudiante);

        service.deleteEstudiante(1L);

        assertEquals(State.INACTIVO, estudiante.getState());
        verify(repository).save(estudiante);
    }

    @Test
    void eliminarEstudianteInexistenteLanzaNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.deleteEstudiante(99L));
    }
}
