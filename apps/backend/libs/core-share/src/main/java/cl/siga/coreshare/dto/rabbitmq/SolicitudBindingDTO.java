package cl.siga.coreshare.dto.rabbitmq;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Enlace (binding) entre una cola y un exchange vía API de administración. */
public record SolicitudBindingDTO(

    @NotBlank(message = "La cola del binding es obligatoria.")
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,254}$",
        message = "El nombre de la cola solo admite letras, números, punto, guion y guion bajo.")
    String cola,

    @NotBlank(message = "El exchange del binding es obligatorio.")
    @Pattern(
        regexp = "^[A-Za-z0-9][A-Za-z0-9._-]{0,254}$",
        message = "El nombre del exchange solo admite letras, números, punto, guion y guion bajo.")
    String exchange,

    @Pattern(
        regexp = "^$|^[A-Za-z0-9._*#-]{0,255}$",
        message = "La routing key solo admite letras, números, punto, guion, guion bajo, asterisco y numeral.")
    String routingKey) {
}
