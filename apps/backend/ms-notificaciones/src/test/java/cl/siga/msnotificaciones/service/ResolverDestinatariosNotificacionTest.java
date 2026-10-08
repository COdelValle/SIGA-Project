package cl.siga.msnotificaciones.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.msnotificaciones.cliente.ClienteApoderado;
import cl.siga.msnotificaciones.cliente.ClienteAsignatura;
import cl.siga.msnotificaciones.cliente.ClienteEstudiante;
import cl.siga.msnotificaciones.cliente.ClienteInscripcion;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

class ResolverDestinatariosNotificacionTest {

    private final ClienteEstudiante estudianteClient = mock(ClienteEstudiante.class);
    private final ClienteApoderado apoderadoClient = mock(ClienteApoderado.class);
    private final ClienteAsignatura asignaturaClient = mock(ClienteAsignatura.class);
    private final ClienteInscripcion inscripcionClient = mock(ClienteInscripcion.class);
    private final ResolverDestinatariosNotificacion resolver = new ResolverDestinatariosNotificacion(
        estudianteClient, apoderadoClient, asignaturaClient, inscripcionClient);

    @AfterEach
    void limpiarSecurityContext() {
        SecurityContextHolder.clearContext();
        resolver.limpiarCache();
    }

    @Test
    void estudianteObtieneSoloSusDictacionesYElectivasVigentes() {
        autenticar("oid-estudiante", "ESTUDIANTE");
        when(estudianteClient.obtenerPorIdUsuario("oid-estudiante")).thenReturn(estudiante(41L, "oid-estudiante"));
        when(asignaturaClient.buscarPorClase(4L, 100)).thenReturn(pagina(List.of(
            dictacion(81L, CaracterAsignatura.OBLIGATORIA),
            dictacion(82L, CaracterAsignatura.ELECTIVA),
            dictacion(83L, CaracterAsignatura.ELECTIVA))));
        when(inscripcionClient.buscarPorEstudiante(41L, 100)).thenReturn(pagina(List.of(
            inscripcion(41L, 82L, EstadoInscripcion.ACTIVO),
            inscripcion(41L, 83L, EstadoInscripcion.CANCELADO))));

        DestinatariosNotificacion resultado = resolver.resolverActual();

        assertThat(resultado.idUsuario()).isEqualTo("oid-estudiante");
        assertThat(resultado.idEstudiantes()).containsExactly(41L);
        assertThat(resultado.idDictaciones()).containsExactlyInAnyOrder(81L, 82L);
    }

    @Test
    void apoderadoSoloRecibeDestinosDeSusPupilosVinculados() {
        autenticar("oid-apoderado", "APODERADO");
        when(apoderadoClient.obtenerPorIdUsuario("oid-apoderado")).thenReturn(new ApoderadoResponseDTO(
            3L, "oid-apoderado", "ANA", null, "PEREZ", null, "11111111-1", List.of(),
            List.of(new ParentescoEstudianteDTO(41L, null)), true));
        when(estudianteClient.obtenerPorId(41L)).thenReturn(estudiante(41L, "oid-estudiante"));
        when(asignaturaClient.buscarPorClase(4L, 100)).thenReturn(pagina(List.of(
            dictacion(81L, CaracterAsignatura.OBLIGATORIA))));
        when(inscripcionClient.buscarPorEstudiante(41L, 100)).thenReturn(pagina(List.of()));

        DestinatariosNotificacion resultado = resolver.resolverActual();

        assertThat(resultado.idUsuario()).isEqualTo("oid-apoderado");
        assertThat(resultado.idEstudiantes()).containsExactly(41L);
        assertThat(resultado.idDictaciones()).containsExactly(81L);
    }

    @Test
    void cacheaLaResolucionYNoVuelveALLamarFeignHastaLimpiar() {
        autenticar("oid-cache", "ESTUDIANTE");
        when(estudianteClient.obtenerPorIdUsuario("oid-cache")).thenReturn(estudiante(50L, "oid-cache"));
        when(asignaturaClient.buscarPorClase(4L, 100)).thenReturn(pagina(List.of()));
        when(inscripcionClient.buscarPorEstudiante(50L, 100)).thenReturn(pagina(List.of()));

        resolver.resolverActual();
        resolver.resolverActual();

        verify(estudianteClient, times(1)).obtenerPorIdUsuario("oid-cache");

        resolver.limpiarCache();
        resolver.resolverActual();

        verify(estudianteClient, times(2)).obtenerPorIdUsuario("oid-cache");
    }

    private static EstudianteResponseDTO estudiante(Long id, String oid) {
        return new EstudianteResponseDTO(id, oid, "11111111-1", "ESTUDIANTE", null, "PRUEBA", null,
            LocalDate.of(2012, 1, 1), List.of(), "REGISTRADO", 4L);
    }

    private static CursoAsignaturaResponseDTO dictacion(Long id, CaracterAsignatura caracter) {
        return new CursoAsignaturaResponseDTO(id, 1L, "Matemática", "Matemática",
            AreaAcademica.MATEMATICAS, true, caracter, Semestre.SEMESTRE_1,
            4L, 7L, null, null, 0, List.of());
    }

    private static InscripcionResponseDTO inscripcion(Long estudiante, Long curso, EstadoInscripcion estado) {
        return new InscripcionResponseDTO(1L, estudiante, curso, estado, LocalDateTime.now());
    }

    private static <T> PageResponseDTO<T> pagina(List<T> content) {
        return new PageResponseDTO<>(content, content.size(), 1, 100, 0, true, true, content.isEmpty());
    }

    private static void autenticar(String oid, String rol) {
        Jwt jwt = Jwt.withTokenValue("token")
            .header("alg", "none")
            .claim("oid", oid)
            .build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(
            jwt, List.of(new SimpleGrantedAuthority("ROLE_" + rol))));
    }
}
