package cl.siga.bffweb.domain.apoderados;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import cl.siga.bffweb.domain.apoderados.dto.PupiloResumenDTO;
import cl.siga.bffweb.integration.apoderados.ApoderadoClient;
import cl.siga.bffweb.integration.clases.ClaseClient;
import cl.siga.bffweb.integration.estudiantes.EstudianteClient;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.security.SecurityUtils;
import lombok.RequiredArgsConstructor;

/**
 * Pupilos de un apoderado: lista el vinculo (ms-apoderados) y enriquece con el
 * curso del estudiante (ms-clases); tambien orquesta la actualizacion.
 */
@Service 
@RequiredArgsConstructor 
public class ApoderadoPupiloService {

    private final ApoderadoClient apoderadoClient;
    private final EstudianteClient estudianteClient;
    private final ClaseClient claseClient;

    public List<PupiloResumenDTO> getPupilos() {
        ApoderadoResponseDTO apoderado = apoderadoClient.getApoderadoByIdUsuario(oid());
        if (apoderado.estudiantes() == null) {
            return List.of();
        }
        return apoderado.estudiantes().stream()
            .map(vinculo -> {
                EstudianteResponseDTO estudiante = estudianteClient.getEstudianteById(vinculo.idEstudiante());
                ClaseResponseDTO clase = estudiante.idClase() == null
                    ? null
                    : claseClient.getClaseById(estudiante.idClase());
                return new PupiloResumenDTO(
                    estudiante.id(),
                    nombreCompleto(estudiante),
                    vinculo.parentesco() == null ? null : vinculo.parentesco().name(),
                    clase == null ? null : clase.nivel().getDescripcion() + " " + clase.letra());
            })
            .toList();
    }

    public EstudianteResponseDTO actualizarPupilo(Long idEstudiante, ActualizarEstudianteRequestDTO request) {
        ApoderadoResponseDTO apoderado = apoderadoClient.getApoderadoByIdUsuario(oid());
        boolean vinculado = apoderado.estudiantes() != null && apoderado.estudiantes().stream()
                .anyMatch(vinculo -> idEstudiante.equals(vinculo.idEstudiante()));
        if (!vinculado) {
            throw new AccessDeniedException("El estudiante no esta vinculado a tu cuenta de apoderado.");
        }

        return estudianteClient.updateEstudiante(idEstudiante, request);
    }

    private String oid() {
        return SecurityUtils.getCurrentUserOid()
            .orElseThrow(() -> new BusinessException("No se pudo determinar el usuario autenticado."));
    }

    private static String nombreCompleto(EstudianteResponseDTO estudiante) {
        return Stream.of(estudiante.firstName(), estudiante.middleName(),
                estudiante.firstSurname(), estudiante.secondSurname())
            .filter(parte -> parte != null && !parte.isBlank())
            .collect(Collectors.joining(" "));
    }
}
