package cl.siga.bffweb.domain.apoderados;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Actualizacion de un pupilo por parte de su apoderado: el BFF valida el vinculo
 * (ms-apoderados) y orquesta la actualizacion contra ms-estudiantes.
 */
@Service 
@RequiredArgsConstructor 
public class ApoderadoPupiloService {

    private final ApoderadoClient apoderadoClient;
    private final EstudianteClient estudianteClient;

    public EstudianteResponseDTO actualizarPupilo(Long idEstudiante, ActualizarEstudianteRequestDTO request) {
        String oid = SecurityUtils.getCurrentUserOid()
                .orElseThrow(() -> new BusinessException("No se pudo determinar el usuario autenticado."));

        ApoderadoResponseDTO apoderado = apoderadoClient.getApoderadoByIdUsuario(oid);
        boolean vinculado = apoderado.estudiantes() != null && apoderado.estudiantes().stream()
                .anyMatch(vinculo -> idEstudiante.equals(vinculo.idEstudiante()));
        if (!vinculado) {
            throw new AccessDeniedException("El estudiante no esta vinculado a tu cuenta de apoderado.");
        }

        return estudianteClient.updateEstudiante(idEstudiante, request);
    }
}
