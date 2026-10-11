package cl.siga.coreshare.dto.usuario.payload;

import java.time.LocalDate;
import java.util.List;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.validation.RUT;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Datos de perfil del docente para el registro compuesto asíncrono. El oid se
 * inyecta en el consumidor de dominio.
 */
public record DatosRegistroDocenteDTO(
    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    String firstName,

    @Size(max = 50)
    String middleName,

    @NotBlank(message = "El primer apellido es requerido")
    @Size(min = 2, max = 50)
    String firstSurname,

    @Size(min = 2, max = 50)
    String secondSurname,

    @NotBlank(message = "El RUT es requerido")
    @RUT
    String rut,

    @NotNull(message = "Fecha de contratación requerida")
    @Past
    LocalDate fechaContratacion,

    @NotNull(message = "El área académica es requerida")
    AreaAcademica area,

    @NotNull(message = "El certificado es requerido")
    @Size(min = 1, message = "Debe tener al menos un certificado")
    List<@Valid CertificadoRequestDTO> certificados
) {
}
