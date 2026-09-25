package cl.siga.coreshare.dto.docente;

import java.time.LocalDate;
import java.util.List;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.docente.enums.AreaAcademica;
import cl.siga.coreshare.validation.RUT;
import jakarta.validation.constraints.*;

public record RegistrarDocenteRequestDTO(
    @NotBlank(message = "El ID de usuario de Azure es obligatorio")
    @Size(min = 36, max = 36, message = "El ID de Azure debe tener exactamente 36 caracteres")
    String idUsuario,

    @NotBlank(message = "El nombre es requerido")
    @Size(min = 2, max = 50)
    String firstName,

    @Size(min = 0, max = 50)
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

    @NotNull (message = "El certificado es requerido")
    @Size (min = 1, message = "Debe tener al menos un certificado")
    List<CertificadoRequestDTO> certificados
) {}
