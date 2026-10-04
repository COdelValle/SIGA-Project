package cl.siga.bffweb.domain.docentes;

import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Contexto del docente autenticado: resuelve su ficha a partir del oid del token
 * y valida la pertenencia de una dictación. Reutilizado por los dominios de
 * docentes, notas, evaluaciones y asistencias.
 */
@Service
@RequiredArgsConstructor
public class DocenteContextService {
    private static final int PAGE_SIZE = 100;

    private final DocenteClient docenteClient;
    private final AsignaturaClient asignaturaClient;

    public DocenteResponseDTO docenteActual() {
        String oid = SecurityUtils.getCurrentUserOid()
            .orElseThrow(() -> new BusinessException("No se pudo determinar el usuario autenticado."));
        return docenteClient.getDocenteByIdUsuario(oid);
    }

    public List<CursoAsignaturaResponseDTO> cursosDelDocente(Long idDocente) {
        PageResponseDTO<CursoAsignaturaResponseDTO> pagina =
            asignaturaClient.searchCursoAsignaturasByDocente(idDocente, PAGE_SIZE);
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }

    /**
     * Valida que la dictación exista y pertenezca al docente autenticado.
     * Los administradores quedan exentos del chequeo de pertenencia.
     */
    public CursoAsignaturaResponseDTO validarCursoDelDocente(Long idCursoAsignatura) {
        CursoAsignaturaResponseDTO curso = asignaturaClient.getCursoAsignaturaById(idCursoAsignatura);
        if (curso == null) {
            throw new ResourceNotFoundException("Dictación con ID " + idCursoAsignatura + " no encontrada.");
        }
        if (SecurityUtils.isAdmin()) {
            return curso;
        }
        DocenteResponseDTO docente = docenteActual();
        if (!Objects.equals(curso.idDocente(), docente.id())) {
            throw new AccessDeniedException(
                "La dictación " + idCursoAsignatura + " no pertenece al docente autenticado.");
        }
        return curso;
    }
}
