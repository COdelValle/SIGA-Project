package cl.siga.coreshare.dto.docente.certificado;

import java.time.LocalDate;

public record CertificadoResponseDTO(
    Long id,
    String nombre,
    String institucionRealizacion,
    LocalDate fechaTitulacion
) {

}
