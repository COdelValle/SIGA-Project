package cl.siga.bffweb.domain.admin;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import cl.siga.bffweb.domain.admin.dto.AsignaturaAdminDTO;
import cl.siga.bffweb.domain.admin.dto.UsuarioAdminDTO;
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
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.usuario.RegistrarUsuarioCompuestoRequestDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationCredentialResponseDTO;
import cl.siga.coreshare.dto.usuario.UserRegistrationStatusResponseDTO;
import cl.siga.coreshare.dto.usuario.UsuarioResponseDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.enums.StateUsuario;
import lombok.RequiredArgsConstructor;

/** Datos de gestion institucional (usuarios y asignaturas) para el portal admin. */
@Service
@RequiredArgsConstructor
public class AdminBffService {
    private static final int PAGE_SIZE = 200;

    private final UsuarioClient usuarioClient;
    private final AsignaturaClient asignaturaClient;
    private final ClaseClient claseClient;
    private final EstudianteClient estudianteClient;
    private final DocenteClient docenteClient;
    private final ApoderadoClient apoderadoClient;

    public List<UsuarioAdminDTO> getUsuarios(String email, Rol rol, StateUsuario state) {
        return contentOf(usuarioClient.searchUsuarios(email, rol, state, PAGE_SIZE)).stream()
            .map(usuario -> new UsuarioAdminDTO(
                usuario.id(),
                usuario.fullName() != null && !usuario.fullName().isBlank()
                    ? usuario.fullName()
                    : "Sin nombre",
                usuario.email(),
                usuario.rol() == null ? null : usuario.rol().name(),
                usuario.state() == null ? null : usuario.state().name()))
            .toList();
    }

    /** Detalle del usuario con un resumen del perfil de su rol. */
    public UsuarioDetalleDTO getUsuarioDetalle(String idUsuario) {
        UsuarioResponseDTO usuario = usuarioClient.getUsuarioById(idUsuario);
        String nombre = nombreDe(usuario);
        String rol = usuario.rol() == null ? null : usuario.rol().name();
        String estado = usuario.state() == null ? null : usuario.state().name();

        if (usuario.rol() == null) {
            return new UsuarioDetalleDTO(usuario.id(), nombre, usuario.email(), null, estado,
                null, null, "Sin rol asignado.", List.of());
        }
        try {
            return switch (usuario.rol()) {
                case ESTUDIANTE -> detalleEstudiante(usuario, nombre, rol, estado);
                case DOCENTE -> detalleDocente(usuario, nombre, rol, estado);
                case APODERADO -> detalleApoderado(usuario, nombre, rol, estado);
                case ADMIN -> new UsuarioDetalleDTO(usuario.id(), nombre, usuario.email(), rol, estado,
                    null, null, "Cuenta administradora.", List.of());
            };
        } catch (RuntimeException ex) {
            return new UsuarioDetalleDTO(usuario.id(), nombre, usuario.email(), rol, estado,
                null, null, "Sin perfil registrado o servicio no disponible.", List.of());
        }
    }

    private UsuarioDetalleDTO detalleEstudiante(UsuarioResponseDTO usuario, String nombre, String rol, String estado) {
        EstudianteResponseDTO estudiante = estudianteClient.getEstudianteByIdUsuario(usuario.id());
        String clase = "Sin clase asignada";
        if (estudiante.idClase() != null) {
            try {
                ClaseResponseDTO datos = claseClient.getClaseById(estudiante.idClase());
                clase = datos.nivel() == null
                    ? "Clase #" + estudiante.idClase()
                    : "Clase: " + datos.nivel().getDescripcion() + " " + datos.letra()
                        + " · " + datos.anioAcademico();
            } catch (RuntimeException ex) {
                clase = "Clase #" + estudiante.idClase();
            }
        }
        List<String> etiquetas = estudiante.allergies() == null ? List.of()
            : estudiante.allergies().stream().map(alergia -> "Alergia: " + alergia).toList();
        return new UsuarioDetalleDTO(usuario.id(), nombre, usuario.email(), rol, estado,
            estudiante.rut(),
            estudiante.birthDate() == null ? null : estudiante.birthDate().toString(),
            clase + " · Perfil: " + estudiante.state(),
            etiquetas);
    }

    private UsuarioDetalleDTO detalleDocente(UsuarioResponseDTO usuario, String nombre, String rol, String estado) {
        DocenteResponseDTO docente = docenteClient.getDocenteByIdUsuario(usuario.id());
        String area = docente.area() == null ? "-" : docente.area().getNombre();
        String detalle = "Área: " + area
            + " · Contratación: " + (docente.fechaContratacion() == null ? "-" : docente.fechaContratacion())
            + (Boolean.FALSE.equals(docente.activo()) ? " · PERFIL INACTIVO" : "");
        List<String> etiquetas = docente.certificados() == null ? List.of()
            : docente.certificados().stream()
                .map(certificado -> "Certificado: " + certificado.nombre()
                    + " (" + certificado.institucionRealizacion() + ", " + certificado.fechaTitulacion() + ")")
                .toList();
        return new UsuarioDetalleDTO(usuario.id(), nombre, usuario.email(), rol, estado,
            docente.rut(), null, detalle, etiquetas);
    }

    private UsuarioDetalleDTO detalleApoderado(UsuarioResponseDTO usuario, String nombre, String rol, String estado) {
        ApoderadoResponseDTO apoderado = apoderadoClient.getApoderadoByIdUsuario(usuario.id());
        List<String> telefonos = apoderado.telefonos() == null ? List.of() : apoderado.telefonos();
        String detalle = "Teléfonos: " + (telefonos.isEmpty() ? "-" : String.join(", ", telefonos))
            + (Boolean.FALSE.equals(apoderado.activo()) ? " · PERFIL INACTIVO" : "");
        List<String> etiquetas = apoderado.estudiantes() == null ? List.of()
            : apoderado.estudiantes().stream()
                .map(vinculo -> "Estudiante #" + vinculo.idEstudiante() + " · "
                    + (vinculo.parentesco() == null ? "-" : vinculo.parentesco().getTextoMostrado()))
                .toList();
        return new UsuarioDetalleDTO(usuario.id(), nombre, usuario.email(), rol, estado,
            apoderado.rut(), null, detalle, etiquetas);
    }

    private static String nombreDe(UsuarioResponseDTO usuario) {
        return usuario.fullName() != null && !usuario.fullName().isBlank()
            ? usuario.fullName()
            : usuario.email();
    }

    public List<AsignaturaAdminDTO> getAsignaturas() {
        List<AsignaturaResponseDTO> catalogo = contentOf(asignaturaClient.searchAsignaturas(PAGE_SIZE));
        Map<Long, List<MallaCurricularResponseDTO>> mallaPorAsignatura = asignaturaClient.getMalla(null).stream()
            .collect(Collectors.groupingBy(MallaCurricularResponseDTO::idAsignatura));

        return catalogo.stream()
            .map(asignatura -> new AsignaturaAdminDTO(
                asignatura.id(),
                asignatura.nombre(),
                asignatura.area(),
                asignatura.calificable(),
                nivelesDe(mallaPorAsignatura.getOrDefault(asignatura.id(), List.of())),
                asignatura.activa()))
            .toList();
    }

    /** Filas de la malla curricular, opcionalmente acotadas a un nivel. */
    public List<MallaCurricularResponseDTO> getMalla(Nivel nivel) {
        return asignaturaClient.getMalla(nivel);
    }

    // --- Registro asíncrono de usuarios ---

    public UserRegistrationStatusResponseDTO iniciarRegistroCompuesto(
            RegistrarUsuarioCompuestoRequestDTO request) {
        return usuarioClient.iniciarRegistroCompuesto(request);
    }

    public UserRegistrationStatusResponseDTO getRegistroCompuesto(String processId) {
        return usuarioClient.getRegistroCompuesto(processId);
    }

    public UserRegistrationCredentialResponseDTO getCredencialTemporal(String processId) {
        return usuarioClient.getCredencialTemporal(processId);
    }

    public UserRegistrationCredentialResponseDTO resetPassword(String idUsuario) {
        return usuarioClient.resetPassword(idUsuario);
    }

    public void eliminarUsuario(String idUsuario) {
        usuarioClient.deleteUsuario(idUsuario);
    }

    /** Clases activas del año indicado (para el selector de clase del estudiante). */
    public List<ClaseOpcionDTO> getClases(Integer anioAcademico) {
        return contentOf(claseClient.searchClases(anioAcademico, PAGE_SIZE)).stream()
            .map(clase -> new ClaseOpcionDTO(
                clase.id(),
                clase.nivel() == null ? null : clase.nivel().getDescripcion(),
                clase.letra(),
                clase.anioAcademico()))
            .toList();
    }

    /** Busca alumnos por RUT o nombre; requiere al menos 2 caracteres. */
    public List<EstudianteOpcionDTO> buscarEstudiantes(String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        return contentOf(estudianteClient.searchEstudiantes(q.trim(), 20)).stream()
            .map(estudiante -> new EstudianteOpcionDTO(
                estudiante.id(),
                estudiante.rut(),
                estudiante.firstName(),
                estudiante.firstSurname()))
            .toList();
    }

    private static List<String> nivelesDe(List<MallaCurricularResponseDTO> filas) {
        return filas.stream()
            .map(MallaCurricularResponseDTO::nivel)
            .distinct()
            .sorted(Comparator.comparingInt(Nivel::ordinal))
            .map(Nivel::getDescripcion)
            .toList();
    }

    private static <T> List<T> contentOf(PageResponseDTO<T> pagina) {
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }
}
