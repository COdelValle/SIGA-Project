package cl.siga.coreshare.dto.docente;

import java.time.LocalDate;
import java.util.List;

import cl.siga.coreshare.dto.docente.certificado.CertificadoResponseDTO;
import cl.siga.coreshare.dto.docente.enums.AreaAcademica;

public record DocenteResponseDTO(
    Long id,
    String idUsuario,
    String firstName,
    String middleName,
    String firstSurname,
    String secondSurname,
    String rut,
    LocalDate fechaContratacion,
    Boolean activo,
    AreaAcademica area,
    List<CertificadoResponseDTO> certificados
) {

}
