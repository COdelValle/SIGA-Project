package cl.siga.coreshare.dto.usuario.payload;

import java.util.List;

import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.validation.Phone;
import cl.siga.coreshare.validation.RUT;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Datos de perfil del apoderado para el registro compuesto asíncrono. El oid
 * se inyecta en el consumidor de dominio.
 */
public record DatosRegistroApoderadoDTO(
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

    @NotEmpty(message = "Debe tener al menos un teléfono")
    List<@Phone(message = "El teléfono debe tener un formato válido") String> telefonos,

    @NotNull(message = "Debe tener al menos un estudiante")
    @Size(min = 1, message = "Debe tener al menos un estudiante")
    List<@Valid ParentescoEstudianteDTO> estudiantes
) {
}
