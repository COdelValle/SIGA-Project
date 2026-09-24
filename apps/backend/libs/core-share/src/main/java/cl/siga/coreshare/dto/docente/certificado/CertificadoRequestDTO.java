package cl.siga.coreshare.dto.docente.certificado;

import java.time.LocalDate;

import jakarta.validation.constraints.*;

public record CertificadoRequestDTO(
    
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 100)
    String nombre,

    @NotBlank(message = "El instituto de realización es requerido")
    @Size(min = 2, max = 50)
    String institucionRealizacion,

    @NotNull(message = "Fecha de titulación requerida")
    @Past
    LocalDate fechaTitulacion
) {

}
