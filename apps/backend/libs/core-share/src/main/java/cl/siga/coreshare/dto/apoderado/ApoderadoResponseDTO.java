package cl.siga.coreshare.dto.apoderado;

import java.util.List;

import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;

public record ApoderadoResponseDTO(
    Long id,
    String idUsuario,
    String firstName,
    String middleName,
    String firstSurname,
    String secondSurname,
    String rut,
    List<String> telefonos,
    List<ParentescoEstudianteDTO> estudiantes,
    Boolean activo
) {

}
