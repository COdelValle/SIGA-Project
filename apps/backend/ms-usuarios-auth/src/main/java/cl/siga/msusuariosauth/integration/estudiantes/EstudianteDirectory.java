package cl.siga.msusuariosauth.integration.estudiantes;

import java.util.List;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ServiceUnavailableException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Verifica contra ms-estudiantes que los pupilos referenciados existan antes de
 * aceptar el alta asíncrona de un apoderado. Si el estudiante no existe se
 * rechaza la solicitud de inmediato (400); si el servicio no responde se
 * responde 503 y no se crea el proceso.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EstudianteDirectory {

    private final EstudianteClient estudianteClient;

    public void validarVinculos(List<ParentescoEstudianteDTO> vinculos) {
        if (vinculos == null) {
            return;
        }
        for (ParentescoEstudianteDTO vinculo : vinculos) {
            Long idEstudiante = vinculo == null ? null : vinculo.idEstudiante();
            if (idEstudiante == null) {
                throw new BusinessException("Cada vínculo debe incluir un estudiante válido.");
            }
            boolean existe;
            try {
                existe = Boolean.TRUE.equals(estudianteClient.existsById(idEstudiante));
            } catch (FeignException.NotFound ex) {
                throw new BusinessException("El estudiante con ID " + idEstudiante + " no existe.");
            } catch (FeignException ex) {
                log.error("ms-estudiantes no disponible validando el id {}: HTTP {}", idEstudiante, ex.status());
                throw new ServiceUnavailableException(
                        "No se pudo validar el estudiante " + idEstudiante + " contra ms-estudiantes.");
            }
            if (!existe) {
                throw new BusinessException("El estudiante con ID " + idEstudiante + " no existe.");
            }
        }
    }
}
