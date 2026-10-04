package cl.siga.bffweb.integration.asignaturas;

import java.util.List;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.coreshare.dto.clase.enums.Nivel;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.exception.ServiceUnavailableException;

@Component
public class AsignaturaClientFallback implements AsignaturaClient {
    @Override
    public PageResponseDTO<AsignaturaResponseDTO> searchAsignaturas(int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las asignaturas");
    }

    @Override
    public List<MallaCurricularResponseDTO> getMalla(Nivel nivel) {
        throw new ServiceUnavailableException("No se pudo obtener la malla curricular");
    }

    @Override
    public CursoAsignaturaResponseDTO getCursoAsignaturaById(Long id) {
        throw new ServiceUnavailableException("No se pudo obtener la dictación " + id);
    }

    @Override
    public PageResponseDTO<CursoAsignaturaResponseDTO> searchCursoAsignaturasByClase(Long idClase, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las asignaturas de la clase " + idClase);
    }

    @Override
    public PageResponseDTO<CursoAsignaturaResponseDTO> searchCursoAsignaturasByDocente(Long idDocente, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las asignaturas del docente " + idDocente);
    }

    @Override
    public PageResponseDTO<InscripcionResponseDTO> searchInscripcionesByAlumno(Long idAlumno, int size) {
        throw new ServiceUnavailableException("No se pudieron obtener las inscripciones del alumno " + idAlumno);
    }
}
