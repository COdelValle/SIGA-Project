package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import cl.siga.coreshare.dto.usuario.ActualizarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.InvitacionLoteResponseDTO;
import cl.siga.coreshare.dto.usuario.InvitacionUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateInvitacion;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msusuariosauth.integration.graph.GraphUserDirectory;
import cl.siga.msusuariosauth.model.entity.InvitacionUsuario;
import cl.siga.msusuariosauth.model.entity.Usuario;
import cl.siga.msusuariosauth.model.mapper.UsuarioMapper;
import cl.siga.msusuariosauth.repository.InvitacionUsuarioRepository;
import cl.siga.msusuariosauth.repository.UsuarioRepository;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final String OID = "12345678-1234-1234-1234-123456789012";
    private static final String EMAIL = "user@test.com";

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private InvitacionUsuarioRepository invitacionRepository;

    @Mock
    private UsuarioMapper mapper;

    @Mock
    private GraphUserDirectory graphDirectory;

    @Mock
    private InvitacionVinculacionService vinculacionService;

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private UsuarioService service;

    @BeforeEach
    void setUp() {
        service = new UsuarioService(usuarioRepository, invitacionRepository, mapper,
                graphDirectory, vinculacionService, validator);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void autenticar(String oid, String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("oid", oid)
                .claim("preferred_username", email)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));
    }

    @Test
    void getCurrentUsuario_devuelveFilaExistente() {
        autenticar(OID, EMAIL);
        Usuario usuario = Usuario.builder().id(OID).email(EMAIL).rol(Rol.DOCENTE).state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(usuario));
        when(mapper.toResponseDto(usuario))
                .thenReturn(new UsuarioResponseDTO(OID, "Docente Test", EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO));

        UsuarioResponseDTO result = service.getCurrentUsuario();

        assertEquals(OID, result.id());
        verify(vinculacionService, never()).vincular(any(), any());
    }

    @Test
    void getCurrentUsuario_vinculaInvitacionEnPrimerLogin() {
        autenticar(OID, EMAIL);
        Usuario vinculado = Usuario.builder().id(OID).email(EMAIL).rol(Rol.ESTUDIANTE).state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty());
        when(vinculacionService.vincular(OID, EMAIL)).thenReturn(Optional.of(vinculado));
        when(mapper.toResponseDto(vinculado))
                .thenReturn(new UsuarioResponseDTO(OID, "Estudiante Test", EMAIL, Rol.ESTUDIANTE, StateUsuario.ACTIVO));

        UsuarioResponseDTO result = service.getCurrentUsuario();

        assertEquals(OID, result.id());
        verify(vinculacionService).vincular(OID, EMAIL);
    }

    @Test
    void getCurrentUsuario_sinInvitacionLanza404() {
        autenticar(OID, EMAIL);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty());
        when(vinculacionService.vincular(OID, EMAIL)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getCurrentUsuario());
    }

    @Test
    void saveUsuario_registraComoActivo() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO(OID, EMAIL, Rol.DOCENTE);
        Usuario entity = Usuario.builder().id(OID).email(EMAIL).rol(Rol.DOCENTE).build();

        when(usuarioRepository.existsById(OID)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entity);
        when(usuarioRepository.save(entity)).thenReturn(entity);

        RegistrarUsuarioResponseDTO result = service.saveUsuario(request);

        assertNotNull(result);
        assertEquals(StateUsuario.ACTIVO, entity.getState());
        assertEquals(OID, result.id());
        verify(usuarioRepository).save(entity);
    }

    @Test
    void saveUsuario_sinGraphCreaInvitacion() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO(null, EMAIL, Rol.DOCENTE);

        when(graphDirectory.isEnabled()).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(invitacionRepository.existsByEmail(EMAIL)).thenReturn(false);

        RegistrarUsuarioResponseDTO result = service.saveUsuario(request);

        assertEquals(StateUsuario.INVITADO, result.state());
        assertNull(result.id());
        verify(invitacionRepository).save(any(InvitacionUsuario.class));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void saveUsuario_rechazaEmailDuplicado() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO(OID, EMAIL, Rol.DOCENTE);

        when(usuarioRepository.existsById(OID)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveUsuario(request));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void saveUsuario_rechazaInvitacionDuplicada() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO(null, EMAIL, Rol.DOCENTE);

        when(graphDirectory.isEnabled()).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(invitacionRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThrows(BusinessException.class, () -> service.saveUsuario(request));
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void getUsuarioById_lanzaCuandoNoExiste() {
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.getUsuarioById(OID));
    }

    @Test
    void invitarLote_reportaCreadasDuplicadasEInvalidas() {
        InvitacionUsuarioRequestDTO valida = new InvitacionUsuarioRequestDTO("nuevo@test.com", Rol.DOCENTE);
        InvitacionUsuarioRequestDTO duplicada = new InvitacionUsuarioRequestDTO(EMAIL, Rol.ESTUDIANTE);
        InvitacionUsuarioRequestDTO invalida = new InvitacionUsuarioRequestDTO("sin-arroba", Rol.DOCENTE);
        InvitacionUsuarioRequestDTO repetida = new InvitacionUsuarioRequestDTO("nuevo@test.com", Rol.DOCENTE);

        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(false);
        when(invitacionRepository.existsByEmail("nuevo@test.com")).thenReturn(false);

        InvitacionLoteResponseDTO result =
                service.invitarLote(List.of(valida, duplicada, invalida, repetida));

        assertEquals(1, result.creadas());
        assertEquals(2, result.duplicadas());
        assertEquals(1, result.invalidas().size());
        assertTrue(result.invalidas().get(0).email().contains("sin-arroba"));
        verify(invitacionRepository).save(any(InvitacionUsuario.class));
    }

    @Test
    void invitarLote_rechazaLoteVacio() {
        assertThrows(BusinessException.class, () -> service.invitarLote(List.of()));
    }

    @Test
    void invitarLoteCsv_parseaCorreosYRoles() {
        String contenido = "email,rol\nnuevo@test.com,DOCENTE\nsegundo@test.com;ESTUDIANTE\n";
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "invitaciones.csv", "text/csv", contenido.getBytes());

        when(usuarioRepository.existsByEmail(any())).thenReturn(false);
        when(invitacionRepository.existsByEmail(any())).thenReturn(false);

        InvitacionLoteResponseDTO result = service.invitarLoteCsv(archivo);

        assertEquals(2, result.creadas());
        assertEquals(0, result.duplicadas());
        assertEquals(0, result.invalidas().size());
    }

    @Test
    void invitarLoteCsv_rechazaArchivoVacio() {
        MockMultipartFile archivo = new MockMultipartFile("archivo", "vacio.csv", "text/csv", new byte[0]);

        assertThrows(BusinessException.class, () -> service.invitarLoteCsv(archivo));
    }

    @Test
    void invitarLoteCsv_conRolInvalidoLoReporta() {
        String contenido = "nuevo@test.com,PROFESOR\n";
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "invitaciones.csv", "text/csv", contenido.getBytes());

        InvitacionLoteResponseDTO result = service.invitarLoteCsv(archivo);

        assertEquals(0, result.creadas());
        assertEquals(1, result.invalidas().size());
    }

    @Test
    void deleteUsuario_aplicaSoftDeleteEnEntra() {
        Usuario usuario = Usuario.builder().id(OID).email(EMAIL).rol(Rol.DOCENTE)
                .state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(graphDirectory.isEnabled()).thenReturn(true);

        service.deleteUsuario(OID);

        assertEquals(StateUsuario.INACTIVO, usuario.getState());
        verify(graphDirectory).revokeSignInSessions(OID);
        verify(graphDirectory).revokeRoles(OID);
        verify(graphDirectory).setAccountEnabled(OID, false);
    }

    @Test
    void deleteUsuario_propiaCuentaLanzaBusinessException() {
        autenticar(OID, EMAIL);
        Usuario usuario = Usuario.builder().id(OID).email(EMAIL).rol(Rol.ADMIN)
                .state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(usuario));

        assertThrows(BusinessException.class, () -> service.deleteUsuario(OID));

        assertEquals(StateUsuario.ACTIVO, usuario.getState());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deleteUsuario_ultimoAdminActivoLanzaBusinessException() {
        autenticar("otro-oid", "otro@test.com");
        Usuario usuario = Usuario.builder().id(OID).email(EMAIL).rol(Rol.ADMIN)
                .state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.countByRolAndState(Rol.ADMIN, StateUsuario.ACTIVO)).thenReturn(1L);

        assertThrows(BusinessException.class, () -> service.deleteUsuario(OID));

        assertEquals(StateUsuario.ACTIVO, usuario.getState());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deleteUsuario_conDosAdminsActivosPermiteBaja() {
        autenticar("otro-oid", "otro@test.com");
        Usuario usuario = Usuario.builder().id(OID).email(EMAIL).rol(Rol.ADMIN)
                .state(StateUsuario.ACTIVO).build();
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.countByRolAndState(Rol.ADMIN, StateUsuario.ACTIVO)).thenReturn(2L);
        when(usuarioRepository.save(usuario)).thenReturn(usuario);

        service.deleteUsuario(OID);

        assertEquals(StateUsuario.INACTIVO, usuario.getState());
    }

    @Test
    void updateUsuario_reactivadoHabilitaYAsignaRolEnEntra() {
        ActualizarUsuarioRequestDTO request =
                new ActualizarUsuarioRequestDTO(EMAIL, StateUsuario.ACTIVO, Rol.DOCENTE);
        Usuario usuario = Usuario.builder().id(OID).email(EMAIL).rol(Rol.DOCENTE)
                .state(StateUsuario.INACTIVO).build();
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(usuario));
        when(usuarioRepository.save(usuario)).thenReturn(usuario);
        when(graphDirectory.isEnabled()).thenReturn(true);
        doAnswer(invocation -> {
            Usuario objetivo = invocation.getArgument(1);
            objetivo.setState(StateUsuario.ACTIVO);
            return null;
        }).when(mapper).updateEntityFromDto(request, usuario);

        service.updateUsuario(OID, request);

        verify(graphDirectory).setAccountEnabled(OID, true);
        verify(graphDirectory).syncRole(OID, Rol.DOCENTE);
    }
}
