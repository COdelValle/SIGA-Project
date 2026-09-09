package cl.siga.coreshare.dto.estudiante;

import java.time.LocalDate;
import java.util.List;

public record EstudianteResponseDTO(
    Long id,
    String rut,
    String firstName,
    String middleName,
    String lastName,
    LocalDate birthDate,
    List<String> allergies
) {
}
