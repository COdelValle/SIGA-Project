package cl.siga.coreshare.dto.usuario.payload;

import java.time.LocalDate;
import java.util.List;

import cl.siga.coreshare.validation.RUT;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

/**
 * Datos de perfil del estudiante para el registro compuesto asíncrono. No
 * incluye {@code idUsuario}: el oid se resuelve en la etapa de Entra ID y el
 * consumidor de dominio lo inyecta al construir el DTO local.
 */
public record DatosRegistroEstudianteDTO(
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

    @NotNull(message = "Fecha de nacimiento requerida")
    @Past
    LocalDate birthDate,

    List<String> allergies,

    Long idClase
) {
}
