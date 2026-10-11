package cl.siga.bffweb.integration.asignaturas;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;

@FeignClient (
    name = "ms-asignaturas",
    url = "${services.asignaturas.url}",
    fallback = AsignaturaClientFallback.class
)
public interface AsignaturaClient {
    /** Catálogo general de asignaturas (independiente de cursos). */
    @GetMapping ("/api/v1/asignaturas/search")
    PageResponseDTO<AsignaturaResponseDTO> searchAsignaturas(@RequestParam ("size") int size);

    /** Malla curricular, opcionalmente filtrada por nivel. */
    @GetMapping ("/api/v1/malla")
    List<MallaCurricularResponseDTO> getMalla(@RequestParam (value = "nivel", required = false) Nivel nivel);

    /** Dictación concreta de una asignatura en un curso. */
    @GetMapping ("/api/v1/curso-asignaturas/{id}")
    CursoAsignaturaResponseDTO getCursoAsignaturaById(@PathVariable ("id") Long id);

    @GetMapping ("/api/v1/curso-asignaturas/search")
    PageResponseDTO<CursoAsignaturaResponseDTO> searchCursoAsignaturasByClase(
        @RequestParam ("idClase") Long idClase,
        @RequestParam ("size") int size);

    @GetMapping ("/api/v1/curso-asignaturas/search")
    PageResponseDTO<CursoAsignaturaResponseDTO> searchCursoAsignaturasByDocente(
        @RequestParam ("idDocente") Long idDocente,
        @RequestParam ("size") int size);

    /** Inscripciones del alumno a dictaciones optativas/electivas. */
    @GetMapping ("/api/v1/inscripciones/search")
    PageResponseDTO<InscripcionResponseDTO> searchInscripcionesByAlumno(
        @RequestParam ("idAlumno") Long idAlumno,
        @RequestParam ("size") int size);
}
