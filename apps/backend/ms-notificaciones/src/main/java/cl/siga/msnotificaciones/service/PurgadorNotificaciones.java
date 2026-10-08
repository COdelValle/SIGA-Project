package cl.siga.msnotificaciones.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retención global de la bandeja: elimina notificaciones más antiguas que
 * {@code siga.notificaciones.retencion-dias} (90 por defecto). Las marcas de
 * lectura caen por la FK ON DELETE CASCADE.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PurgadorNotificaciones {

    private final NotificacionService notificacionService;

    @Value("${siga.notificaciones.retencion-dias:90}")
    private int retencionDias;

    @Scheduled(cron = "${siga.notificaciones.purga.cron:0 30 3 * * *}")
    public void purgarAntiguas() {
        try {
            notificacionService.purgarAntiguas(retencionDias);
        } catch (Exception error) {
            log.warn("No se pudo ejecutar la purga de notificaciones: {}", error.getMessage());
        }
    }
}
