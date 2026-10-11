package cl.siga.msnotificaciones.service;

import cl.siga.coreshare.dto.notificaciones.AccionEvaluacion;
import cl.siga.coreshare.dto.notificaciones.AccionNota;
import cl.siga.coreshare.dto.notificaciones.ContadorNotificacionesDTO;
import cl.siga.coreshare.dto.notificaciones.EventoAsistencia;
import cl.siga.coreshare.dto.notificaciones.EventoEvaluacion;
import cl.siga.coreshare.dto.notificaciones.EventoNota;
import cl.siga.coreshare.dto.notificaciones.NotificacionResponseDTO;
import cl.siga.coreshare.dto.notificaciones.TipoDestinoNotificacion;
import cl.siga.coreshare.dto.notificaciones.TipoNotificacion;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.mensajeria.EventoInvalidoException;
import cl.siga.msnotificaciones.model.entity.Notificacion;
import cl.siga.msnotificaciones.model.entity.NotificacionLectura;
import cl.siga.msnotificaciones.model.entity.NotificacionLecturaId;
import cl.siga.msnotificaciones.repository.NotificacionLecturaRepository;
import cl.siga.msnotificaciones.repository.NotificacionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificacionService {

    private static final List<Long> SIN_DESTINOS = List.of(-1L);
    private static final int TAMANO_MAXIMO = 50;
    private static final DateTimeFormatter FECHA_CORTA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final NotificacionRepository notificacionRepository;
    private final NotificacionLecturaRepository lecturaRepository;
    private final ResolverDestinatariosNotificacion resolverDestinatarios;

    @Transactional
    public void registrarEvento(EventoEvaluacion evento) {
        if (evento.idCursoAsignatura() == null) {
            throw new EventoInvalidoException(
                "Evento de evaluación sin idCursoAsignatura (idEvento=" + evento.idEvento() + ").");
        }
        AccionEvaluacion accion = evento.accion();
        String nombre = textoSeguro(evento.nombre(), "evaluación");
        String asignatura = textoOpcional(evento.nombreAsignatura());
        String titulo = switch (accion) {
            case CREADA -> conAsignatura("Nueva evaluación", asignatura);
            case ACTUALIZADA -> conAsignatura("Evaluación actualizada", asignatura);
            case ELIMINADA -> conAsignatura("Evaluación cancelada", asignatura);
        };
        String enAsignatura = asignatura == null ? " en una de tus asignaturas" : " en " + asignatura;
        String resumen = switch (accion) {
            case CREADA -> "Se agregó «" + nombre + "»" + enAsignatura + ".";
            case ACTUALIZADA -> "Se actualizaron los datos de «" + nombre + "»" + enAsignatura + ".";
            case ELIMINADA -> "Se canceló «" + nombre + "»" + enAsignatura + ".";
        };
        insertar(
            evento.idEvento(), "EVALUACION|" + evento.idEvaluacion() + "|" + accion + "|" + evento.fechaHora(),
            TipoNotificacion.EVALUACION, accion.name(), TipoDestinoNotificacion.CURSO_ASIGNATURA,
            evento.idCursoAsignatura(), titulo, resumen, evento.fechaHora());
    }

    @Transactional
    public void registrarEvento(EventoNota evento) {
        if (evento.idEstudiante() == null) {
            throw new EventoInvalidoException(
                "Evento de nota sin idEstudiante (idEvento=" + evento.idEvento() + ").");
        }
        AccionNota accion = evento.accion();
        String evaluacion = textoOpcional(evento.nombreEvaluacion());
        String asignatura = textoOpcional(evento.nombreAsignatura());
        String titulo = accion == AccionNota.CREADA
            ? conAsignatura("Calificación agregada", asignatura)
            : conAsignatura("Calificación actualizada", asignatura);
        String resumen = resumenNota(accion, evaluacion, asignatura);
        insertar(
            evento.idEvento(), "NOTA|" + evento.idNota() + "|" + accion + "|" + evento.fechaHora(),
            TipoNotificacion.NOTA, accion.name(), TipoDestinoNotificacion.ESTUDIANTE,
            evento.idEstudiante(), titulo, resumen, evento.fechaHora());
    }

    @Transactional
    public void registrarEvento(EventoAsistencia evento) {
        if (evento.idEstudiante() == null) {
            throw new EventoInvalidoException(
                "Evento de asistencia sin idEstudiante (idEvento=" + evento.idEvento() + ").");
        }
        String estado = evento.estado() == null ? "asistencia" : evento.estado().name().toLowerCase();
        String asignatura = textoOpcional(evento.nombreAsignatura());
        String titulo = evento.estado() == null
            ? "Actualización de asistencia"
            : conAsignatura("Registro de " + estado, asignatura);
        StringBuilder resumen = new StringBuilder("Se registró un estado ")
            .append(estado);
        if (asignatura != null) {
            resumen.append(" en ").append(asignatura);
        }
        if (evento.fecha() != null) {
            resumen.append(" el ").append(FECHA_CORTA.format(evento.fecha()));
        }
        resumen.append('.');
        if (evento.superaUmbralInasistencia()) {
            resumen.append(" Inasistencia mensual: ")
                .append(Math.round(evento.porcentajeInasistencia())).append("%.");
        }
        insertar(
            evento.idEvento(), "ASISTENCIA|" + evento.idAsistencia() + "|" + evento.accion() + "|" + evento.fechaHora(),
            TipoNotificacion.ASISTENCIA, evento.accion().name(), TipoDestinoNotificacion.ESTUDIANTE,
            evento.idEstudiante(), titulo, resumen.toString(), evento.fechaHora());
    }

    @Transactional(readOnly = true)
    public Page<NotificacionResponseDTO> obtenerMisNotificaciones(int pagina, int tamano) {
        DestinatariosNotificacion destinatarios = resolverDestinatarios.resolverActual();
        List<Long> estudiantes = conSentinela(destinatarios.idEstudiantes());
        List<Long> dictaciones = conSentinela(destinatarios.idDictaciones());
        Page<Notificacion> resultados = notificacionRepository.buscarVisibles(
            destinatarios.incluyeEstudiantes(), TipoDestinoNotificacion.ESTUDIANTE, estudiantes,
            destinatarios.incluyeDictaciones(), TipoDestinoNotificacion.CURSO_ASIGNATURA, dictaciones,
            destinatarios.idUsuario(),
            PageRequest.of(pagina, Math.min(tamano, TAMANO_MAXIMO),
                Sort.by(Sort.Order.desc("fechaHora"), Sort.Order.desc("id"))));

        List<Long> ids = resultados.getContent().stream().map(Notificacion::getId).toList();
        Set<Long> leidas = ids.isEmpty()
            ? Set.of()
            : Set.copyOf(lecturaRepository.buscarIdsLeidos(destinatarios.idUsuario(), ids));
        return resultados.map(notificacion -> toResponse(notificacion, leidas.contains(notificacion.getId())));
    }

    @Transactional(readOnly = true)
    public ContadorNotificacionesDTO contarNoLeidas() {
        DestinatariosNotificacion destinatarios = resolverDestinatarios.resolverActual();
        long cuenta = notificacionRepository.contarNoLeidas(
            destinatarios.incluyeEstudiantes(), TipoDestinoNotificacion.ESTUDIANTE,
            conSentinela(destinatarios.idEstudiantes()),
            destinatarios.incluyeDictaciones(), TipoDestinoNotificacion.CURSO_ASIGNATURA,
            conSentinela(destinatarios.idDictaciones()), destinatarios.idUsuario());
        return new ContadorNotificacionesDTO(cuenta);
    }

    @Transactional
    public void marcarLeida(Long id) {
        Notificacion notificacion = notificacionRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada."));
        DestinatariosNotificacion destinatarios = resolverDestinatarios.resolverActual();
        validarVisible(notificacion, destinatarios);
        lecturaRepository.marcarLeidaSiNoExiste(id, destinatarios.idUsuario(), LocalDateTime.now());
    }

    /** "Marcar todas como leídas": inserta la marca de las visibles que falten. */
    @Transactional
    public int marcarTodasLeidas() {
        DestinatariosNotificacion destinatarios = resolverDestinatarios.resolverActual();
        List<Long> pendientes = notificacionRepository.idsVisiblesNoLeidas(
            destinatarios.incluyeEstudiantes(), TipoDestinoNotificacion.ESTUDIANTE,
            conSentinela(destinatarios.idEstudiantes()),
            destinatarios.incluyeDictaciones(), TipoDestinoNotificacion.CURSO_ASIGNATURA,
            conSentinela(destinatarios.idDictaciones()), destinatarios.idUsuario());
        if (pendientes.isEmpty()) {
            return 0;
        }
        LocalDateTime ahora = LocalDateTime.now();
        List<NotificacionLectura> lecturas = pendientes.stream()
            .map(id -> new NotificacionLectura(
                new NotificacionLecturaId(id, destinatarios.idUsuario()), ahora, null))
            .toList();
        lecturaRepository.saveAll(lecturas);
        log.info("Marcadas como leídas {} notificaciones del usuario", lecturas.size());
        return lecturas.size();
    }

    /** "Limpiar mis leídas": oculta de mi bandeja las notificaciones ya leídas. */
    @Transactional
    public int limpiarLeidas() {
        DestinatariosNotificacion destinatarios = resolverDestinatarios.resolverActual();
        int ocultadas = lecturaRepository.ocultarLeidas(destinatarios.idUsuario(), LocalDateTime.now());
        log.info("Ocultadas {} notificaciones leídas del usuario", ocultadas);
        return ocultadas;
    }

    /** Purga global de notificaciones antiguas (las lecturas caen por FK en cascada). */
    @Transactional
    public int purgarAntiguas(int diasRetencion) {
        if (diasRetencion <= 0) {
            return 0;
        }
        LocalDateTime limite = LocalDateTime.now().minusDays(diasRetencion);
        int eliminadas = notificacionRepository.eliminarAnterioresA(limite);
        if (eliminadas > 0) {
            log.info("Purga de notificaciones: {} filas anteriores a {}", eliminadas, limite);
        }
        return eliminadas;
    }

    private void validarVisible(Notificacion notificacion, DestinatariosNotificacion destinatarios) {
        boolean visible = (notificacion.getTipoDestino() == TipoDestinoNotificacion.ESTUDIANTE
                && destinatarios.idEstudiantes().contains(notificacion.getIdDestino()))
            || (notificacion.getTipoDestino() == TipoDestinoNotificacion.CURSO_ASIGNATURA
                && destinatarios.idDictaciones().contains(notificacion.getIdDestino()));
        if (!visible) {
            // 404 evita revelar si existe una notificación ajena.
            throw new ResourceNotFoundException("Notificación no encontrada.");
        }
    }

    private static String resumenNota(AccionNota accion, String evaluacion, String asignatura) {
        String referencia = evaluacion == null
            ? (asignatura == null ? null : " de " + asignatura)
            : " en «" + evaluacion + "»" + (asignatura == null ? "" : " de " + asignatura);
        if (referencia == null) {
            return accion == AccionNota.CREADA
                ? "Hay una nueva calificación disponible en SIGA."
                : "Se actualizó una calificación disponible en SIGA.";
        }
        return (accion == AccionNota.CREADA
            ? "Se agregó una calificación"
            : "Se actualizó una calificación") + referencia + ".";
    }

    private static String conAsignatura(String base, String asignatura) {
        return asignatura == null ? base : base + " en " + asignatura;
    }

    private void insertar(
            String idEvento,
            String claveFallback,
            TipoNotificacion tipo,
            String accion,
            TipoDestinoNotificacion tipoDestino,
            Long idDestino,
            String titulo,
            String resumen,
            LocalDateTime fechaHora) {
        String clave = idEvento == null || idEvento.isBlank() ? claveFallback : idEvento;
        String idEstable = UUID.nameUUIDFromBytes(clave.getBytes(StandardCharsets.UTF_8)).toString();
        int insertados = notificacionRepository.insertarSiNuevo(
            idEstable, tipo.name(), accion, tipoDestino.name(), idDestino,
            textoSeguro(titulo, "Notificación"), textoSeguro(resumen, "Hay una actualización académica en SIGA."),
            fechaHora == null ? LocalDateTime.now() : fechaHora);
        if (insertados == 1) {
            log.info("Notificación persistida: tipo={} destino={} idDestino={} idEvento={}",
                tipo, tipoDestino, idDestino, idEstable);
        }
    }

    private static NotificacionResponseDTO toResponse(Notificacion notificacion, boolean leida) {
        return new NotificacionResponseDTO(
            notificacion.getId(), notificacion.getTipo(), notificacion.getAccion(),
            notificacion.getTitulo(), notificacion.getResumen(), notificacion.getFechaHora(), leida);
    }

    private static List<Long> conSentinela(Set<Long> ids) {
        return ids.isEmpty() ? SIN_DESTINOS : List.copyOf(ids);
    }

    private static String textoOpcional(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static String textoSeguro(String texto, String alternativo) {
        String resultado = texto == null || texto.isBlank() ? alternativo : texto.trim();
        return resultado.length() > 500 ? resultado.substring(0, 500) : resultado;
    }
}
