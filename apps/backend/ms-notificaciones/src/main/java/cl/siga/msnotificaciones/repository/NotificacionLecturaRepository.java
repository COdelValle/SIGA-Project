package cl.siga.msnotificaciones.repository;

import cl.siga.msnotificaciones.model.entity.NotificacionLectura;
import cl.siga.msnotificaciones.model.entity.NotificacionLecturaId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificacionLecturaRepository extends JpaRepository<NotificacionLectura, NotificacionLecturaId> {

    @Query("""
        select l.id.idNotificacion from NotificacionLectura l
        where l.id.idUsuario = :idUsuario and l.id.idNotificacion in :idsNotificacion
          and l.ocultadaEn is null
        """)
    List<Long> buscarIdsLeidos(
        @Param("idUsuario") String idUsuario,
        @Param("idsNotificacion") List<Long> idsNotificacion);

    /** "Limpiar mis leídas": oculta las leídas propias sin borrar filas compartidas. */
    @Modifying
    @Query("""
        update NotificacionLectura l set l.ocultadaEn = :ocultadaEn
        where l.id.idUsuario = :idUsuario and l.ocultadaEn is null
        """)
    int ocultarLeidas(
        @Param("idUsuario") String idUsuario,
        @Param("ocultadaEn") LocalDateTime ocultadaEn);

    @Modifying
    @Query(value = """
        INSERT IGNORE INTO notificacion_lecturas (id_notificacion, id_usuario, leida_en)
        VALUES (:idNotificacion, :idUsuario, :leidaEn)
        """, nativeQuery = true)
    int marcarLeidaSiNoExiste(
        @Param("idNotificacion") Long idNotificacion,
        @Param("idUsuario") String idUsuario,
        @Param("leidaEn") LocalDateTime leidaEn);
}
