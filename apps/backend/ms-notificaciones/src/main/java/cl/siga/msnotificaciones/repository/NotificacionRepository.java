package cl.siga.msnotificaciones.repository;

import cl.siga.coreshare.dto.notificaciones.TipoDestinoNotificacion;
import cl.siga.msnotificaciones.model.entity.Notificacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO notificaciones
            (id_evento, tipo, accion, tipo_destino, id_destino, titulo, resumen, fecha_hora)
        VALUES
            (:idEvento, :tipo, :accion, :tipoDestino, :idDestino, :titulo, :resumen, :fechaHora)
        """, nativeQuery = true)
    int insertarSiNuevo(
        @Param("idEvento") String idEvento,
        @Param("tipo") String tipo,
        @Param("accion") String accion,
        @Param("tipoDestino") String tipoDestino,
        @Param("idDestino") Long idDestino,
        @Param("titulo") String titulo,
        @Param("resumen") String resumen,
        @Param("fechaHora") LocalDateTime fechaHora);

    @Query("""
        select n from Notificacion n
        where ((:incluirEstudiantes = true
                    and n.tipoDestino = :destinoEstudiante
                    and n.idDestino in :idEstudiantes)
            or (:incluirDictaciones = true
                    and n.tipoDestino = :destinoDictacion
                    and n.idDestino in :idDictaciones))
          and not exists (
              select l.id from NotificacionLectura l
              where l.id.idNotificacion = n.id
                and l.id.idUsuario = :idUsuario
                and l.ocultadaEn is not null)
        """)
    Page<Notificacion> buscarVisibles(
        @Param("incluirEstudiantes") boolean incluirEstudiantes,
        @Param("destinoEstudiante") TipoDestinoNotificacion destinoEstudiante,
        @Param("idEstudiantes") List<Long> idEstudiantes,
        @Param("incluirDictaciones") boolean incluirDictaciones,
        @Param("destinoDictacion") TipoDestinoNotificacion destinoDictacion,
        @Param("idDictaciones") List<Long> idDictaciones,
        @Param("idUsuario") String idUsuario,
        Pageable pageable);

    @Query("""
        select count(n) from Notificacion n
        where ((:incluirEstudiantes = true
                    and n.tipoDestino = :destinoEstudiante
                    and n.idDestino in :idEstudiantes)
            or (:incluirDictaciones = true
                    and n.tipoDestino = :destinoDictacion
                    and n.idDestino in :idDictaciones))
          and not exists (
              select l.id from NotificacionLectura l
              where l.id.idNotificacion = n.id
                and l.id.idUsuario = :idUsuario)
        """)
    long contarNoLeidas(
        @Param("incluirEstudiantes") boolean incluirEstudiantes,
        @Param("destinoEstudiante") TipoDestinoNotificacion destinoEstudiante,
        @Param("idEstudiantes") List<Long> idEstudiantes,
        @Param("incluirDictaciones") boolean incluirDictaciones,
        @Param("destinoDictacion") TipoDestinoNotificacion destinoDictacion,
        @Param("idDictaciones") List<Long> idDictaciones,
        @Param("idUsuario") String idUsuario);

    /** Ids visibles que aun no tienen marca de lectura del usuario (para marcar todas). */
    @Query("""
        select n.id from Notificacion n
        where ((:incluirEstudiantes = true
                    and n.tipoDestino = :destinoEstudiante
                    and n.idDestino in :idEstudiantes)
            or (:incluirDictaciones = true
                    and n.tipoDestino = :destinoDictacion
                    and n.idDestino in :idDictaciones))
          and not exists (
              select l.id from NotificacionLectura l
              where l.id.idNotificacion = n.id
                and l.id.idUsuario = :idUsuario)
        """)
    List<Long> idsVisiblesNoLeidas(
        @Param("incluirEstudiantes") boolean incluirEstudiantes,
        @Param("destinoEstudiante") TipoDestinoNotificacion destinoEstudiante,
        @Param("idEstudiantes") List<Long> idEstudiantes,
        @Param("incluirDictaciones") boolean incluirDictaciones,
        @Param("destinoDictacion") TipoDestinoNotificacion destinoDictacion,
        @Param("idDictaciones") List<Long> idDictaciones,
        @Param("idUsuario") String idUsuario);

    /** Purga global: las filas hijas caen por ON DELETE CASCADE. */
    @Modifying
    @Query("delete from Notificacion n where n.fechaHora < :limite")
    int eliminarAnterioresA(@Param("limite") LocalDateTime limite);
}
