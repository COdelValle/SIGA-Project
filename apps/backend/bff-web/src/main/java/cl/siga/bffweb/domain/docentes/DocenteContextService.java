package cl.siga.bffweb.domain.docentes;

import java.util.List;
import java.util.Objects;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import cl.siga.bffweb.integration.asignaturas.AsignaturaClient;
import cl.siga.bffweb.integration.docentes.DocenteClient;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.docente.DocenteResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Contexto del docente autenticado: resuelve su ficha a partir del oid del token
 * y valida la pertenencia de una asignatura. Reutilizado por los dominios de
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

    public List<AsignaturaResponseDTO> asignaturasDelDocente(Long idDocente) {
        PageResponseDTO<AsignaturaResponseDTO> pagina =
            asignaturaClient.searchAsignaturasByDocente(idDocente, PAGE_SIZE);
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }

    /**
     * Valida que la asignatura exista y pertenezca al docente autenticado.
     * Los administradores quedan exentos del chequeo de pertenencia.
     */
    public AsignaturaResponseDTO validarAsignaturaDelDocente(Long idAsignatura) {
        AsignaturaResponseDTO asignatura = asignaturaClient.getAsignaturaById(idAsignatura);
        if (asignatura == null) {
            throw new ResourceNotFoundException("Asignatura con ID " + idAsignatura + " no encontrada.");
        }
        if (SecurityUtils.isAdmin()) {
            return asignatura;
        }
        DocenteResponseDTO docente = docenteActual();
        if (!Objects.equals(asignatura.idDocente(), docente.id())) {
            throw new AccessDeniedException(
                "La asignatura " + idAsignatura + " no pertenece al docente autenticado.");
        }
        return asignatura;
    }
}
