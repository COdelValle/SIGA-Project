package cl.siga.msnotificaciones.service;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.security.SecurityUtils;
import cl.siga.msnotificaciones.cliente.ClienteApoderado;
import cl.siga.msnotificaciones.cliente.ClienteAsignatura;
import cl.siga.msnotificaciones.cliente.ClienteEstudiante;
import cl.siga.msnotificaciones.cliente.ClienteInscripcion;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Resuelve los perfiles y dictaciones autorizados en la consulta autenticada.
 * El consumidor RabbitMQ persiste solamente IDs académicos y no depende del JWT
 * del docente que originó el evento.
 *
 * <p>El resultado se cachea por OID durante 3 minutos porque la campana consulta
 * el contador cada 10 segundos y sin caché cada consulta dispararía varias
 * llamadas Feign. Cambios de vínculos/inscripciones pueden tardar hasta el TTL
 * en reflejarse.</p>
 */
@Component
@RequiredArgsConstructor
public class ResolverDestinatariosNotificacion {

    private static final int PAGE_SIZE = 100;
    private static final Duration TTL = Duration.ofMinutes(3);

    private final ClienteEstudiante estudianteClient;
    private final ClienteApoderado apoderadoClient;
    private final ClienteAsignatura asignaturaClient;
    private final ClienteInscripcion inscripcionClient;

    private final Map<String, EntradaCache> cache = new ConcurrentHashMap<>();

    public DestinatariosNotificacion resolverActual() {
        String idUsuario = SecurityUtils.getCurrentUserOid()
            .orElseThrow(() -> new AccessDeniedException("No se pudo identificar al usuario autenticado."));

        EntradaCache cacheada = cache.get(idUsuario);
        if (cacheada != null && !cacheada.expirada()) {
            return cacheada.destinatarios();
        }

        DestinatariosNotificacion resuelto = resolver(idUsuario);
        cache.put(idUsuario, new EntradaCache(resuelto, Instant.now().plus(TTL)));
        return resuelto;
    }

    /** Solo para pruebas: fuerza la próxima resolución real. */
    void limpiarCache() {
        cache.clear();
    }

    private DestinatariosNotificacion resolver(String idUsuario) {
        Map<Long, EstudianteResponseDTO> estudiantes = new LinkedHashMap<>();
        if (SecurityUtils.hasRole("ESTUDIANTE")) {
            agregar(estudiantes, estudianteClient.obtenerPorIdUsuario(idUsuario));
        }

        if (SecurityUtils.hasRole("APODERADO")) {
            ApoderadoResponseDTO apoderado = apoderadoClient.obtenerPorIdUsuario(idUsuario);
            if (Boolean.TRUE.equals(apoderado.activo()) && apoderado.estudiantes() != null) {
                apoderado.estudiantes().stream()
                    .map(vinculo -> vinculo.idEstudiante())
                    .filter(id -> id != null)
                    .distinct()
                    .map(estudianteClient::obtenerPorId)
                    .forEach(estudiante -> agregar(estudiantes, estudiante));
            }
        }

        Set<Long> idEstudiantes = new LinkedHashSet<>(estudiantes.keySet());
        Set<Long> idDictaciones = new LinkedHashSet<>();
        for (EstudianteResponseDTO estudiante : estudiantes.values()) {
            if (estudiante.idClase() == null) {
                continue;
            }

            Set<Long> inscripcionesVigentes = contentOf(
                    inscripcionClient.buscarPorEstudiante(estudiante.id(), PAGE_SIZE)).stream()
                .filter(inscripcion -> inscripcion.estado() != EstadoInscripcion.CANCELADO)
                .map(InscripcionResponseDTO::idCursoAsignatura)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

            contentOf(asignaturaClient.buscarPorClase(estudiante.idClase(), PAGE_SIZE)).stream()
                .filter(dictacion -> dictacion.caracter() != CaracterAsignatura.ELECTIVA
                    || inscripcionesVigentes.contains(dictacion.id()))
                .map(CursoAsignaturaResponseDTO::id)
                .filter(id -> id != null)
                .forEach(idDictaciones::add);
        }

        return new DestinatariosNotificacion(idUsuario, Set.copyOf(idEstudiantes), Set.copyOf(idDictaciones));
    }

    private static void agregar(Map<Long, EstudianteResponseDTO> estudiantes, EstudianteResponseDTO estudiante) {
        if (estudiante != null && estudiante.id() != null) {
            estudiantes.putIfAbsent(estudiante.id(), estudiante);
        }
    }

    private static <T> List<T> contentOf(PageResponseDTO<T> pagina) {
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }

    private record EntradaCache(DestinatariosNotificacion destinatarios, Instant expiraEn) {
        boolean expirada() {
            return Instant.now().isAfter(expiraEn);
        }
    }
}
