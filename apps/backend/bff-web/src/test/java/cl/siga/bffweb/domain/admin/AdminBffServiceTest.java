package cl.siga.bffweb.domain.admin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import cl.siga.bffweb.domain.admin.dto.AsignaturaAdminDTO;
import cl.siga.bffweb.domain.admin.dto.api.ClaseOpcionDTO;
import cl.siga.bffweb.domain.admin.dto.api.EstudianteOpcionDTO;
import cl.siga.bffweb.domain.admin.dto.api.UsuarioDetalleDTO;
import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.bffweb.integration.usuarios.UsuarioClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.enums.Parentesco;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationCredentialResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.ResourceNotFoundException;

@ExtendWith(MockitoExtension.class)
class AdminBffServiceTest {

    private static final String OID_PRUEBA = "oid-1";
    private static final String CLAVE_PRUEBA = "clave-de-prueba";

    @Mock
    private UsuarioClient usuarioClient;
    @Mock
    private AsignaturaClient asignaturaClient;
    @Mock
    private ClaseClient claseClient;
    @Mock
    private EstudianteClient estudianteClient;
    @Mock
    private DocenteClient docenteClient;
    @Mock
    private ApoderadoClient apoderadoClient;

    private AdminBffService service;

    @BeforeEach
    void setUp() {
        service = new AdminBffService(usuarioClient, asignaturaClient, claseClient,
                estudianteClient, docenteClient, apoderadoClient);
    }

    private static <T> PageResponseDTO<T> pagina(List<T> content) {
        return new PageResponseDTO<>(content, content.size(), 1, content.size(), 0, true, true, content.isEmpty());
    }

    private static PageResponseDTO<UsuarioResponseDTO> paginaNula() {
        return new PageResponseDTO<>(null, 0, 1, 0, 0, true, true, true);
    }

    @Test
    void getClasesMapeaNivelYAnio() {
        when(claseClient.searchClases(2026, 200)).thenReturn(pagina(List.of(
                new ClaseResponseDTO(1L, Nivel.PRIMERO_BASICO, "A", 2026, null))));

        List<ClaseOpcionDTO> clases = service.getClases(2026);

        assertEquals(1, clases.size());
        assertEquals("1ro Básico", clases.get(0).nivel());
        assertEquals("A", clases.get(0).letra());
        assertEquals(2026, clases.get(0).anioAcademico());
    }

    @Test
    void getClasesConPaginaNulaDevuelveVacio() {
        when(claseClient.searchClases(2026, 200)).thenReturn(null);

        assertTrue(service.getClases(2026).isEmpty());
    }

    @Test
    void buscarEstudiantesExigeMinimoDosCaracteres() {
        assertTrue(service.buscarEstudiantes("c").isEmpty());
        assertTrue(service.buscarEstudiantes(null).isEmpty());
        assertTrue(service.buscarEstudiantes("   ").isEmpty());

        verifyNoInteractions(estudianteClient);
    }

    @Test
    void buscarEstudiantesMapeaOpcionYLimpiaElTexto() {
        when(estudianteClient.searchEstudiantes("22.126.386", 20)).thenReturn(pagina(List.of(
                new EstudianteResponseDTO(7L, "oid-1", "22126386-3", "Catalina", null,
                        "Ormeño", null, null, null, "REGISTRADO", 1L))));

        List<EstudianteOpcionDTO> resultado = service.buscarEstudiantes("  22.126.386 ");

        assertEquals(1, resultado.size());
        assertEquals("22126386-3", resultado.get(0).rut());
        assertEquals("Catalina", resultado.get(0).firstName());
        assertEquals("Ormeño", resultado.get(0).firstSurname());
    }

    @Test
    void buscarEstudiantesConContenidoNuloDevuelveVacio() {
        when(estudianteClient.searchEstudiantes("ana", 20))
                .thenReturn(new PageResponseDTO<>(null, 0, 1, 0, 0, true, true, true));

        assertTrue(service.buscarEstudiantes("ana").isEmpty());
    }

    @Test
    void eliminarUsuarioDelegaEnElCliente() {
        service.eliminarUsuario("oid-1");

        verify(usuarioClient).deleteUsuario("oid-1");
    }

    @Test
    void getUsuariosUsaElNombreYSinNombreComoFallback() {
        when(usuarioClient.searchUsuarios(null, null, null, 200)).thenReturn(pagina(List.of(
                new UsuarioResponseDTO("oid-1", "Catalina Ormeño", "catalina@x", Rol.ESTUDIANTE, StateUsuario.ACTIVO),
                new UsuarioResponseDTO("oid-2", null, "sin.nombre@x", Rol.DOCENTE, StateUsuario.ACTIVO),
                new UsuarioResponseDTO("oid-3", " ", "otro@x", null, null))));

        var usuarios = service.getUsuarios(null, null, null);

        assertEquals("Catalina Ormeño", usuarios.get(0).nombre());
        assertEquals("Sin nombre", usuarios.get(1).nombre());
        assertEquals("Sin nombre", usuarios.get(2).nombre());
        assertEquals(null, usuarios.get(2).rol());
        assertEquals(null, usuarios.get(2).estado());
    }

    @Test
    void getUsuariosConPaginaNulaDevuelveVacio() {
        when(usuarioClient.searchUsuarios(null, null, null, 200)).thenReturn(paginaNula());

        assertTrue(service.getUsuarios(null, null, null).isEmpty());
    }

    @Test
    void getUsuarioDetalleDeEstudianteIncluyeClaseYAlergias() {
        when(usuarioClient.getUsuarioById("oid-1")).thenReturn(
                new UsuarioResponseDTO("oid-1", "Catalina Ormeño", "catalina@x", Rol.ESTUDIANTE, StateUsuario.ACTIVO));
        when(estudianteClient.getEstudianteByIdUsuario("oid-1")).thenReturn(
                new EstudianteResponseDTO(7L, "oid-1", "22126386-3", "CATALINA", null, "ORMEÑO", null,
                        LocalDate.of(2012, 5, 1), List.of("Maní"), "REGISTRADO", 1L));
        when(claseClient.getClaseById(1L)).thenReturn(
                new ClaseResponseDTO(1L, Nivel.PRIMERO_BASICO, "A", 2026, null));

        var detalle = service.getUsuarioDetalle("oid-1");

        assertEquals("Catalina Ormeño", detalle.fullName());
        assertEquals("22126386-3", detalle.rut());
        assertEquals("2012-05-01", detalle.fechaNacimiento());
        assertTrue(detalle.detalle().contains("1ro Básico A"));
        assertEquals(1, detalle.etiquetas().size());
    }

    @Test
    void getUsuarioDetalleSinRolAsignado() {
        when(usuarioClient.getUsuarioById("oid-1")).thenReturn(
                new UsuarioResponseDTO("oid-1", null, "sin.rol@x", null, null));

        var detalle = service.getUsuarioDetalle("oid-1");

        assertEquals("sin.rol@x", detalle.fullName());
        assertEquals("Sin rol asignado.", detalle.detalle());
        assertEquals(null, detalle.rol());
        assertEquals(null, detalle.estado());
    }

    @Test
    void getUsuarioDetalleDeAdmin() {
        when(usuarioClient.getUsuarioById("oid-1")).thenReturn(
                new UsuarioResponseDTO("oid-1", "Admin", "admin@x", Rol.ADMIN, StateUsuario.ACTIVO));

        var detalle = service.getUsuarioDetalle("oid-1");

        assertEquals("Cuenta administradora.", detalle.detalle());
        assertTrue(detalle.etiquetas().isEmpty());
    }

    @Test
    void getUsuarioDetalleDeEstudianteSinClaseNiAlergias() {
        when(usuarioClient.getUsuarioById("oid-1")).thenReturn(
                new UsuarioResponseDTO("oid-1", "Alumno", "alumno@x", Rol.ESTUDIANTE, StateUsuario.ACTIVO));
        when(estudianteClient.getEstudianteByIdUsuario("oid-1")).thenReturn(
                new EstudianteResponseDTO(7L, "oid-1", "22126386-3", "Alumno", null, "Perez", null,
                        null, null, "REGISTRADO", null));

        var detalle = service.getUsuarioDetalle("oid-1");

        assertEquals("Sin clase asignada · Perfil: REGISTRADO", detalle.detalle());
        assertTrue(detalle.etiquetas().isEmpty());
        assertEquals(null, detalle.fechaNacimiento());
    }

    @Test
    void getUsuarioDetalleDeEstudianteConClaseSinNivel() {
        when(usuarioClient.getUsuarioById("oid-1")).thenReturn(
                new UsuarioResponseDTO("oid-1", "Alumno", "alumno@x", Rol.ESTUDIANTE, StateUsuario.ACTIVO));
        when(estudianteClient.getEstudianteByIdUsuario("oid-1")).thenReturn(
                new EstudianteResponseDTO(7L, "oid-1", "22126386-3", "Alumno", null, "Perez", null,
                        null, null, "REGISTRADO", 9L));
        when(claseClient.getClaseById(9L)).thenReturn(new ClaseResponseDTO(9L, null, "A", 2026, null));

        var detalle = service.getUsuarioDetalle("oid-1");

        assertTrue(detalle.detalle().contains("Clase #9"));
    }

    @Test
    void getUsuarioDetalleDeEstudianteConClaseNoDisponible() {
        when(usuarioClient.getUsuarioById("oid-1")).thenReturn(
                new UsuarioResponseDTO("oid-1", "Alumno", "alumno@x", Rol.ESTUDIANTE, StateUsuario.ACTIVO));
        when(estudianteClient.getEstudianteByIdUsuario("oid-1")).thenReturn(
                new EstudianteResponseDTO(7L, "oid-1", "22126386-3", "Alumno", null, "Perez", null,
                        null, null, "REGISTRADO", 9L));
        when(claseClient.getClaseById(9L)).thenThrow(new ResourceNotFoundException("sin clase"));

        var detalle = service.getUsuarioDetalle("oid-1");

        assertTrue(detalle.detalle().contains("Clase #9"));
    }

    @Test
    void getUsuarioDetalleDeDocenteMapeaCertificadosYPerfilInactivo() {
        when(usuarioClient.getUsuarioById("oid-2")).thenReturn(
                new UsuarioResponseDTO("oid-2", "Docente", "docente@x", Rol.DOCENTE, StateUsuario.INACTIVO));
        when(docenteClient.getDocenteByIdUsuario("oid-2")).thenReturn(new DocenteResponseDTO(
                2L, "oid-2", "Ana", null, "Perez", null, "21000002-K",
                null, false, null, List.of(new CertificadoResponseDTO(1L, "Titulo", "Instituto",
                        LocalDate.of(2019, 1, 1)))));

        var detalle = service.getUsuarioDetalle("oid-2");

        assertEquals("21000002-K", detalle.rut());
        assertTrue(detalle.detalle().contains("Área: -"));
        assertTrue(detalle.detalle().contains("PERFIL INACTIVO"));
        assertEquals(List.of("Certificado: Titulo (Instituto, 2019-01-01)"), detalle.etiquetas());
    }

    @Test
    void getUsuarioDetalleDeDocenteSinCertificados() {
        when(usuarioClient.getUsuarioById("oid-2")).thenReturn(
                new UsuarioResponseDTO("oid-2", "Docente", "docente@x", Rol.DOCENTE, StateUsuario.ACTIVO));
        when(docenteClient.getDocenteByIdUsuario("oid-2")).thenReturn(new DocenteResponseDTO(
                2L, "oid-2", "Ana", null, "Perez", null, "21000002-K",
                LocalDate.of(2020, 3, 1), true, AreaAcademica.MATEMATICAS, null));

        var detalle = service.getUsuarioDetalle("oid-2");

        assertEquals("Área: Matemáticas · Contratación: 2020-03-01", detalle.detalle());
        assertTrue(detalle.etiquetas().isEmpty());
    }

    @Test
    void getUsuarioDetalleDeApoderadoMapeaTelefonosYVinculos() {
        when(usuarioClient.getUsuarioById("oid-3")).thenReturn(
                new UsuarioResponseDTO("oid-3", "Apoderado", "apoderado@x", Rol.APODERADO, StateUsuario.ACTIVO));
        when(apoderadoClient.getApoderadoByIdUsuario("oid-3")).thenReturn(new ApoderadoResponseDTO(
                3L, "oid-3", "Luis", null, "Soto", null, "21000003-8",
                List.of("+56912345678", "+56987654321"),
                List.of(new ParentescoEstudianteDTO(7L, Parentesco.MADRE_PADRE),
                        new ParentescoEstudianteDTO(8L, null)),
                false));

        var detalle = service.getUsuarioDetalle("oid-3");

        assertEquals("21000003-8", detalle.rut());
        assertTrue(detalle.detalle().contains("+56912345678, +56987654321"));
        assertTrue(detalle.detalle().contains("PERFIL INACTIVO"));
        assertEquals(2, detalle.etiquetas().size());
        assertTrue(detalle.etiquetas().get(1).contains("· -"));
    }

    @Test
    void getUsuarioDetalleDeApoderadoSinTelefonosNiVinculos() {
        when(usuarioClient.getUsuarioById("oid-3")).thenReturn(
                new UsuarioResponseDTO("oid-3", "Apoderado", "apoderado@x", Rol.APODERADO, StateUsuario.ACTIVO));
        when(apoderadoClient.getApoderadoByIdUsuario("oid-3")).thenReturn(new ApoderadoResponseDTO(
                3L, "oid-3", "Luis", null, "Soto", null, "21000003-8",
                null, null, true));

        var detalle = service.getUsuarioDetalle("oid-3");

        assertEquals("Teléfonos: -", detalle.detalle());
        assertTrue(detalle.etiquetas().isEmpty());
    }

    @Test
    void getUsuarioDetalleSinPerfilNoFalla() {
        when(usuarioClient.getUsuarioById("oid-2")).thenReturn(
                new UsuarioResponseDTO("oid-2", null, "sin.perfil@x", Rol.DOCENTE, StateUsuario.INACTIVO));
        when(docenteClient.getDocenteByIdUsuario("oid-2"))
                .thenThrow(new ResourceNotFoundException("sin perfil"));

        var detalle = service.getUsuarioDetalle("oid-2");

        assertEquals("sin.perfil@x", detalle.fullName());
        assertTrue(detalle.detalle().toLowerCase().contains("sin perfil"));
    }

    @Test
    void getAsignaturasAgrupaLaMallaPorAsignaturaYOrdenaLosNiveles() {
        AsignaturaResponseDTO primera = new AsignaturaResponseDTO(1L, "Matemática", "MAT",
                "Matemática básica", AreaAcademica.MATEMATICAS, true, true);
        AsignaturaResponseDTO segunda = new AsignaturaResponseDTO(2L, "Arte", "ART",
                "Artes visuales", AreaAcademica.ARTES, false, true);
        when(asignaturaClient.searchAsignaturas(200)).thenReturn(pagina(List.of(primera, segunda)));
        when(asignaturaClient.getMalla(null)).thenReturn(List.of(
                malla(10L, Nivel.OCTAVO_BASICO, 1L, "Matemática"),
                malla(11L, Nivel.PRIMERO_BASICO, 1L, "Matemática"),
                malla(12L, Nivel.OCTAVO_BASICO, 1L, "Matemática")));

        List<AsignaturaAdminDTO> resultado = service.getAsignaturas();

        assertEquals(2, resultado.size());
        assertEquals(List.of("1ro Básico", "8vo Básico"), resultado.get(0).niveles());
        assertTrue(resultado.get(1).niveles().isEmpty());
    }

    @Test
    void getMallaDelegaEnElCliente() {
        List<MallaCurricularResponseDTO> filas = List.of(malla(10L, Nivel.OCTAVO_BASICO, 1L, "Matemática"));
        when(asignaturaClient.getMalla(Nivel.OCTAVO_BASICO)).thenReturn(filas);

        assertEquals(filas, service.getMalla(Nivel.OCTAVO_BASICO));
    }

    @Test
    void operacionesDeRegistroDeleganEnElCliente() {
        RegistrarUsuarioCompuestoRequestDTO request = new RegistrarUsuarioCompuestoRequestDTO(
                "nuevo@test.com", "Nuevo", Rol.ESTUDIANTE, null, null,
                new DatosRegistroRolDTO(null, null, null));
        UserRegistrationStatusResponseDTO estado = new UserRegistrationStatusResponseDTO(
                "process-1", RegistrationProcessState.EN_PROCESO, RegistrationStepState.PENDIENTE,
                RegistrationStepState.PENDIENTE, Rol.ESTUDIANTE, "nuevo@test.com", null, null,
                OffsetDateTime.now(), OffsetDateTime.now());
        UserRegistrationCredentialResponseDTO credencial = new UserRegistrationCredentialResponseDTO(
                "process-1", "nuevo@test.com", OID_PRUEBA, CLAVE_PRUEBA, OffsetDateTime.now());
        when(usuarioClient.iniciarRegistroCompuesto(request)).thenReturn(estado);
        when(usuarioClient.getRegistroCompuesto("process-1")).thenReturn(estado);
        when(usuarioClient.getCredencialTemporal("process-1")).thenReturn(credencial);
        when(usuarioClient.resetPassword(OID_PRUEBA)).thenReturn(credencial);

        assertEquals(estado, service.iniciarRegistroCompuesto(request));
        assertEquals(estado, service.getRegistroCompuesto("process-1"));
        assertEquals(credencial, service.getCredencialTemporal("process-1"));
        assertEquals(credencial, service.resetPassword(OID_PRUEBA));
    }

    private MallaCurricularResponseDTO malla(Long id, Nivel nivel, Long idAsignatura, String nombre) {
        return new MallaCurricularResponseDTO(id, nivel, idAsignatura, nombre,
                AreaAcademica.MATEMATICAS, CaracterAsignatura.OBLIGATORIA, PlanFormacion.COMUN,
                4, true, true);
    }
}
