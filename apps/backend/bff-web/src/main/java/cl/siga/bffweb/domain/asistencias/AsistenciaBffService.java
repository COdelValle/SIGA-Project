package cl.siga.bffweb.domain.asistencias;

import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.asistencias.AsistenciaClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.common.PageResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Asistencias por estudiante: resuelve el oid del token, valida el vinculo
 * (estudiante o apoderado) y orquesta contra ms-asistencias.
 */
@Service
@RequiredArgsConstructor
public class AsistenciaBffService {
    private static final int PAGE_SIZE = 500;

    private final AsistenciaClient asistenciaClient;
    private final EstudianteClient estudianteClient;
    private final ApoderadoClient apoderadoClient;

    public List<AsistenciaResponseDTO> getAsistenciasMe() {
        EstudianteResponseDTO estudiante = estudianteClient.getEstudianteByIdUsuario(oid());
        return asistenciasDe(estudiante.id());
    }

    public List<AsistenciaResponseDTO> getAsistenciasEstudiante(Long idEstudiante) {
        EstudianteResponseDTO estudiante = estudianteClient.getEstudianteById(idEstudiante);
        validarAcceso(estudiante);
        return asistenciasDe(idEstudiante);
    }

    public AsistenciaResponseDTO registrarAsistencia(RegistrarAsistenciaRequestDTO request) {
        return asistenciaClient.saveAsistencia(request);
    }

    public AsistenciaResponseDTO actualizarAsistencia(Long id, ActualizarAsistenciaRequestDTO request) {
        return asistenciaClient.updateAsistencia(id, request);
    }

    private List<AsistenciaResponseDTO> asistenciasDe(Long idEstudiante) {
        PageResponseDTO<AsistenciaResponseDTO> pagina = asistenciaClient.searchAsistencias(idEstudiante, PAGE_SIZE);
        return pagina == null || pagina.content() == null ? List.of() : pagina.content();
    }

    private void validarAcceso(EstudianteResponseDTO estudiante) {
        if (SecurityUtils.isAdmin() || SecurityUtils.hasRole("DOCENTE")) {
            return;
        }
        String oid = oid();
        if (SecurityUtils.hasRole("ESTUDIANTE") && oid.equals(estudiante.idUsuario())) {
            return;
        }
        if (SecurityUtils.hasRole("APODERADO")) {
            ApoderadoResponseDTO apoderado = apoderadoClient.getApoderadoByIdUsuario(oid);
            boolean vinculado = apoderado.estudiantes() != null && apoderado.estudiantes().stream()
                .anyMatch(vinculo -> estudiante.id().equals(vinculo.idEstudiante()));
            if (vinculado) {
                return;
            }
        }
        throw new AccessDeniedException("No tienes permiso para ver las asistencias de este estudiante.");
    }

    private String oid() {
        return SecurityUtils.getCurrentUserOid()
            .orElseThrow(() -> new BusinessException("No se pudo determinar el usuario autenticado."));
    }
}
