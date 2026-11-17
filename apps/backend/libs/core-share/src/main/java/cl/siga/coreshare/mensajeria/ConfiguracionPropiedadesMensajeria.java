package cl.siga.coreshare.mensajeria;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Registra las propiedades de mensajería de core-share. Los valores concretos
 * se cargan desde la configuración de cada servicio.
 */
@AutoConfiguration
@EnableConfigurationProperties({
    RegistroMensajeriaProperties.class,
    NotificacionesMensajeriaProperties.class
})
public class ConfiguracionPropiedadesMensajeria {
}
