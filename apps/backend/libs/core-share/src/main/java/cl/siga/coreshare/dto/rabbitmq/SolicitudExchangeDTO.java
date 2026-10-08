package cl.siga.coreshare.dto.rabbitmq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Declaración de un exchange durable vía API de administración. */
public record SolicitudExchangeDTO(

    @NotBlank(message = "El nombre del exchange es obligatorio.")
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,254}$",
        message = "El nombre del exchange solo admite letras, números, punto, guion y guion bajo.")
    String nombre,

    @NotNull(message = "El tipo de exchange es obligatorio.")
    TipoExchange tipo) {
}
