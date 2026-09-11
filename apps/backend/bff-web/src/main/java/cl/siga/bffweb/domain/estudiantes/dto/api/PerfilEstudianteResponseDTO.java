package cl.siga.bffweb.domain.estudiantes.dto.api;

import java.time.LocalDate;
import java.util.List;

import cl.siga.bffweb.domain.estudiantes.dto.internal.AsignaturaDetalleDTO;

public record PerfilEstudianteResponseDTO(
    Long id,
    String idUsuario,
    String rut,
    String firstName,
    String middleName,
    String firstSurname,
    String secondSurname,
    LocalDate birthDate,
    List<String> allergies,
    String state,
    List<AsignaturaDetalleDTO> asignaturas
) {

}
