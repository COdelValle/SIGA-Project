package cl.siga.bffweb.domain.docentes.dto;

import java.util.List;

public record CursoDocenteDTO(
    Long id,
    String nombre,
    Integer nivel,
    String seccion,
    String asignatura,
    Long docenteId,
    String sala,
    List<String> diasClase,
    List<AlumnoDTO> alumnos
) {
}
