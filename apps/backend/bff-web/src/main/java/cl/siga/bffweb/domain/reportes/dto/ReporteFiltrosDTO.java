package cl.siga.bffweb.domain.reportes.dto;

import java.util.List;

/** Catálogo de cursos disponibles para el dashboard de reportes. */
public record ReporteFiltrosDTO(
    List<CursoReporteDTO> cursos
) {
}
