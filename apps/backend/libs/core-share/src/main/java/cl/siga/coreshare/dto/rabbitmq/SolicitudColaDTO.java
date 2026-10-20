package cl.siga.coreshare.dto.rabbitmq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.AssertTrue;

/**
 * Declaración de una cola durable vía API de administración. Opcionalmente
 * permite configurar su dead-letter exchange y routing key.
 */
public record SolicitudColaDTO(

    @NotBlank(message = "El nombre de la cola es obligatorio.")
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,254}$",
        message = "El nombre de la cola solo admite letras, números, punto, guion y guion bajo.")
    String nombre,

    @Pattern(
        regexp = "^$|^[A-Za-z0-9][A-Za-z0-9._-]{0,254}$",
        message = "El exchange de dead-letter solo admite letras, números, punto, guion y guion bajo.")
    String deadLetterExchange,

    @Pattern(
        regexp = "^$|^[A-Za-z0-9._*#-]{0,255}$",
        message = "La routing key de dead-letter solo admite letras, números, punto, guion, guion bajo, asterisco y numeral.")
    String deadLetterRoutingKey) {

    @AssertTrue(message = "El dead-letter exchange y su routing key deben enviarse juntos.")
    public boolean isConfiguracionDeadLetterValida() {
        boolean tieneExchange = deadLetterExchange != null && !deadLetterExchange.isBlank();
        boolean tieneRoutingKey = deadLetterRoutingKey != null && !deadLetterRoutingKey.isBlank();
        return tieneExchange == tieneRoutingKey;
    }
}
