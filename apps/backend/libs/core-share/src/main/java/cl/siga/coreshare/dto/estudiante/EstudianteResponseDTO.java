package cl.siga.coreshare.dto.estudiante;

import java.time.LocalDate;
import java.util.List;

public record EstudianteResponseDTO(
    Long id,
    String idUsuario,
    String rut,
    String firstName,
    String middleName,
    String firstSurname,
    String secondSurname,
    LocalDate birthDate,
    List<String> allergies,
    String state
) {
}
