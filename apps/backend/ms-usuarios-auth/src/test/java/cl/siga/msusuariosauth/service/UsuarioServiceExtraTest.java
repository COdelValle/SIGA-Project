package cl.siga.msusuariosauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import cl.siga.coreshare.dto.usuario.ActualizarUsuarioRequestDTO;
import cl.siga.coreshare.dto.usuario.CandidatoUsuarioResponseDTO;
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
@MockitoSettings(strictness = Strictness.LENIENT)
class UsuarioServiceExtraTest {

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
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    private void autenticar(String oid, String email) {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("oid", oid)
                .claim("preferred_username", email)
                .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    private Usuario usuario(String email, Rol rol, StateUsuario state) {
        return Usuario.builder().id(OID).email(email).rol(rol).state(state).fullName("Usuario Test").build();
    }

    private UsuarioResponseDTO dto() {
        return new UsuarioResponseDTO(OID, "Usuario Test", EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO);
    }

    @Test
    void getCurrentUsuarioReutilizaLaFilaCuandoHayChoqueDeVinculacion() {
        autenticar(OID, EMAIL);
        Usuario existente = usuario(EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty(), Optional.of(existente));
        when(vinculacionService.vincular(OID, EMAIL))
                .thenThrow(new DataIntegrityViolationException("duplicado"));
        when(mapper.toResponseDto(existente)).thenReturn(dto());

        UsuarioResponseDTO resultado = service.getCurrentUsuario();

        assertThat(resultado.id()).isEqualTo(OID);
        verify(usuarioRepository, org.mockito.Mockito.times(2)).findById(OID);
    }

    @Test
    void getCurrentUsuarioFallaSiTrasElChoqueTampocoExisteLaFila() {
        autenticar(OID, EMAIL);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty());
        when(vinculacionService.vincular(OID, EMAIL))
                .thenThrow(new DataIntegrityViolationException("duplicado"));

        assertThatThrownBy(() -> service.getCurrentUsuario())
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCurrentUsuarioFallaSinOidEnElToken() {
        assertThatThrownBy(() -> service.getCurrentUsuario())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("autenticado");
    }

    @Test
    void lookupCandidatoDevuelveElCandidatoOFallaSiNoExiste() {
        CandidatoUsuarioResponseDTO candidato = new CandidatoUsuarioResponseDTO("oid-1", EMAIL, "Usuario");
        when(graphDirectory.findUserByEmail(EMAIL)).thenReturn(Optional.of(candidato));

        assertThat(service.lookupCandidato(EMAIL)).isEqualTo(candidato);

        when(graphDirectory.findUserByEmail("nadie@test.com")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.lookupCandidato("nadie@test.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void saveUsuarioResuelveElOidEnGraphYMarcaLaInvitacionVinculada() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO(null, EMAIL, Rol.DOCENTE);
        Usuario entidad = usuario(EMAIL, Rol.DOCENTE, null);
        InvitacionUsuario invitacion = InvitacionUsuario.builder()
                .email(EMAIL).rol(Rol.DOCENTE).state(StateInvitacion.INVITADO).build();
        when(graphDirectory.isEnabled()).thenReturn(true);
        when(graphDirectory.findUserByEmail(EMAIL))
                .thenReturn(Optional.of(new CandidatoUsuarioResponseDTO(OID, EMAIL, "Usuario")));
        when(usuarioRepository.existsById(OID)).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(mapper.toEntity(request)).thenReturn(entidad);
        when(usuarioRepository.save(entidad)).thenReturn(entidad);
        when(invitacionRepository.findByEmail(EMAIL)).thenReturn(Optional.of(invitacion));

        RegistrarUsuarioResponseDTO resultado = service.saveUsuario(request);

        assertThat(resultado.id()).isEqualTo(OID);
        assertThat(resultado.state()).isEqualTo(StateUsuario.ACTIVO);
        assertThat(invitacion.getState()).isEqualTo(StateInvitacion.VINCULADA);
        assertThat(invitacion.getBoundAt()).isNotNull();
        verify(invitacionRepository).save(invitacion);
        verify(graphDirectory).syncRole(OID, Rol.DOCENTE);
    }

    @Test
    void saveUsuarioRechazaOidYaRegistrado() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO(OID, EMAIL, Rol.DOCENTE);
        when(usuarioRepository.existsById(OID)).thenReturn(true);

        assertThatThrownBy(() -> service.saveUsuario(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está registrado");
    }

    @Test
    void saveUsuarioConIdEnBlancoResuelvePorGraphODeshabilitado() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO("   ", EMAIL, Rol.DOCENTE);
        when(graphDirectory.isEnabled()).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(false);
        when(invitacionRepository.existsByEmail(EMAIL)).thenReturn(false);

        RegistrarUsuarioResponseDTO resultado = service.saveUsuario(request);

        assertThat(resultado.state()).isEqualTo(StateUsuario.INVITADO);
        verify(invitacionRepository).save(any(InvitacionUsuario.class));
    }

    @Test
    void crearInvitacionRechazaCorreoYaRegistradoSinOid() {
        RegistrarUsuarioRequestDTO request = new RegistrarUsuarioRequestDTO(null, EMAIL, Rol.DOCENTE);
        when(graphDirectory.isEnabled()).thenReturn(false);
        when(usuarioRepository.existsByEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.saveUsuario(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está registrado");
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void syncRolesSeOmiteSiGraphSeDeshabilitaAntesDelCommit() {
        Usuario existente = usuario(EMAIL, Rol.ESTUDIANTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);
        when(graphDirectory.isEnabled()).thenReturn(true, false);
        doAnswer(inv -> {
            Usuario objetivo = inv.getArgument(1);
            objetivo.setRol(Rol.DOCENTE);
            return null;
        }).when(mapper).updateEntityFromDto(any(), eq(existente));
        when(mapper.toResponseDto(existente)).thenReturn(dto());

        TransactionSynchronizationManager.initSynchronization();
        service.updateUsuario(OID, new ActualizarUsuarioRequestDTO(EMAIL, StateUsuario.ACTIVO, Rol.DOCENTE));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(graphDirectory, never()).setAccountEnabled(anyString(), org.mockito.ArgumentMatchers.anyBoolean());
        verify(graphDirectory, never()).syncRole(anyString(), any());
    }

    @Test
    void getUsuarioByIdDevuelveElMapeo() {
        Usuario entidad = usuario(EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(entidad));
        when(mapper.toResponseDto(entidad)).thenReturn(dto());

        assertThat(service.getUsuarioById(OID)).isEqualTo(dto());
    }

    @Test
    void searchUsuariosCombinandoTodosLosFiltros() {
        Pageable pageable = PageRequest.of(0, 20);
        Page<Usuario> pagina = Page.empty();
        when(usuarioRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(pagina);

        assertThat(service.searchUsuarios(EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO, pageable).getTotalElements())
                .isZero();
        assertThat(service.searchUsuarios(null, null, null, pageable).getTotalElements()).isZero();

        verify(usuarioRepository, org.mockito.Mockito.times(2))
                .findAll(any(Specification.class), eq(pageable));
    }

    @Test
    void updateUsuarioFallaSiNoExiste() {
        when(usuarioRepository.findById(OID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateUsuario(OID,
                new ActualizarUsuarioRequestDTO(EMAIL, StateUsuario.ACTIVO, null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateUsuarioNoPermiteDesactivarLaPropiaCuenta() {
        autenticar(OID, EMAIL);
        Usuario existente = usuario(EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));

        assertThatThrownBy(() -> service.updateUsuario(OID,
                new ActualizarUsuarioRequestDTO(EMAIL, StateUsuario.INACTIVO, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desactivar tu propia cuenta");
    }

    @Test
    void updateUsuarioRechazaCorreoYaUsadoPorOtro() {
        Usuario existente = usuario(EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));
        when(usuarioRepository.existsByEmail("nuevo@test.com")).thenReturn(true);

        assertThatThrownBy(() -> service.updateUsuario(OID,
                new ActualizarUsuarioRequestDTO("Nuevo@Test.com", StateUsuario.ACTIVO, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está en uso");
        verify(mapper, never()).updateEntityFromDto(any(), any());
    }

    @Test
    void updateUsuarioSinCambiosNoSincronizaConGraph() {
        Usuario existente = usuario(EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);
        when(mapper.toResponseDto(existente)).thenReturn(dto());

        service.updateUsuario(OID, new ActualizarUsuarioRequestDTO(EMAIL, StateUsuario.ACTIVO, Rol.DOCENTE));

        verify(graphDirectory, never()).setAccountEnabled(anyString(), org.mockito.ArgumentMatchers.anyBoolean());
        verify(graphDirectory, never()).syncRole(anyString(), any());
    }

    @Test
    void updateUsuarioSincronizaRolDespuesDelCommit() {
        Usuario existente = usuario(EMAIL, Rol.ESTUDIANTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);
        when(graphDirectory.isEnabled()).thenReturn(true);
        doAnswer(inv -> {
            Usuario objetivo = inv.getArgument(1);
            objetivo.setRol(Rol.DOCENTE);
            return null;
        }).when(mapper).updateEntityFromDto(any(), eq(existente));
        when(mapper.toResponseDto(existente)).thenReturn(dto());

        TransactionSynchronizationManager.initSynchronization();
        service.updateUsuario(OID, new ActualizarUsuarioRequestDTO(EMAIL, StateUsuario.ACTIVO, Rol.DOCENTE));
        List<TransactionSynchronization> sincronizaciones =
                TransactionSynchronizationManager.getSynchronizations();
        assertThat(sincronizaciones).isNotEmpty();
        sincronizaciones.forEach(TransactionSynchronization::afterCommit);

        verify(graphDirectory).setAccountEnabled(OID, true);
        verify(graphDirectory).syncRole(OID, Rol.DOCENTE);
    }

    @Test
    void updateUsuarioNoPropagaUnFalloDeGraphDespuesDelCommit() {
        Usuario existente = usuario(EMAIL, Rol.ESTUDIANTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);
        when(graphDirectory.isEnabled()).thenReturn(true);
        doThrow(new IllegalStateException("Graph caido"))
                .when(graphDirectory).setAccountEnabled(OID, true);
        doAnswer(inv -> {
            Usuario objetivo = inv.getArgument(1);
            objetivo.setRol(Rol.DOCENTE);
            return null;
        }).when(mapper).updateEntityFromDto(any(), eq(existente));
        when(mapper.toResponseDto(existente)).thenReturn(dto());

        TransactionSynchronizationManager.initSynchronization();
        service.updateUsuario(OID, new ActualizarUsuarioRequestDTO(EMAIL, StateUsuario.ACTIVO, Rol.DOCENTE));
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);

        verify(graphDirectory).setAccountEnabled(OID, true);
        verify(graphDirectory, never()).syncRole(anyString(), any());
    }

    @Test
    void syncUserRolesValidaExistenciaYConfiguracion() {
        when(usuarioRepository.findById("sin")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.syncUserRoles("sin"))
                .isInstanceOf(ResourceNotFoundException.class);

        Usuario existente = usuario(EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));
        when(graphDirectory.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.syncUserRoles(OID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Microsoft Graph");

        when(graphDirectory.isEnabled()).thenReturn(true);
        service.syncUserRoles(OID);
        verify(graphDirectory).setAccountEnabled(OID, true);
        verify(graphDirectory).syncRole(OID, Rol.DOCENTE);
    }

    @Test
    void invitarLoteRechazaLotesEnormes() {
        List<InvitacionUsuarioRequestDTO> solicitudes = java.util.Collections.nCopies(
                2001, new InvitacionUsuarioRequestDTO("x@test.com", Rol.DOCENTE));

        assertThatThrownBy(() -> service.invitarLote(solicitudes))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2000");
    }

    @Test
    void invitarLoteReportaFilasNulasYCorreosEnBlanco() {
        InvitacionLoteResponseDTO resultado = service.invitarLote(Arrays.asList(
                null,
                new InvitacionUsuarioRequestDTO("   ", Rol.DOCENTE)));

        assertThat(resultado.creadas()).isZero();
        assertThat(resultado.invalidas()).hasSize(2);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void invitarLoteCsvRechazaArchivosGrandesYErroresDeLectura() throws Exception {
        byte[] grande = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile archivoGrande = new MockMultipartFile(
                "archivo", "grande.csv", "text/csv", grande);

        assertThatThrownBy(() -> service.invitarLoteCsv(archivoGrande))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("2 MB");

        MultipartFile archivoRoto = mock(MultipartFile.class);
        when(archivoRoto.isEmpty()).thenReturn(false);
        when(archivoRoto.getSize()).thenReturn(10L);
        when(archivoRoto.getInputStream()).thenThrow(new IOException("disco"));

        assertThatThrownBy(() -> service.invitarLoteCsv(archivoRoto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CSV");
    }

    @Test
    void invitarLoteCsvIgnoraBomEncabezadoYLineasVacias() {
        String contenido = "\uFEFFemail,rol\n\n   \n,,,\nvalido@test.com\n";
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "invitaciones.csv", "text/csv", contenido.getBytes());

        InvitacionLoteResponseDTO resultado = service.invitarLoteCsv(archivo);

        assertThat(resultado.creadas()).isZero();
        assertThat(resultado.invalidas()).hasSize(2);
        verify(invitacionRepository, never()).save(any());
    }

    @Test
    void invitarLoteCsvNuloEsRechazado() {
        assertThatThrownBy(() -> service.invitarLoteCsv(null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void deleteUsuarioSinGraphNoLlamaIntegraciones() {
        Usuario existente = usuario(EMAIL, Rol.DOCENTE, StateUsuario.ACTIVO);
        when(usuarioRepository.findById(OID)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(existente)).thenReturn(existente);
        when(graphDirectory.isEnabled()).thenReturn(false);

        service.deleteUsuario(OID);

        assertThat(existente.getState()).isEqualTo(StateUsuario.INACTIVO);
        verify(graphDirectory, never()).revokeSignInSessions(anyString());
    }
}
