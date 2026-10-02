package cl.siga.bffweb.domain.estudiantes.mapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import cl.siga.bffweb.domain.estudiantes.dto.api.PerfilEstudianteResponseDTO;
import cl.siga.bffweb.domain.estudiantes.dto.internal.AsignaturaDetalleDTO;
import cl.siga.bffweb.domain.estudiantes.dto.internal.NotaDetalleDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;

@Mapper (componentModel = "spring")
public interface PerfilEstudianteMapper {
    // 1. El método principal ahora recibe 4 parámetros
    @Mapping(target = "asignaturas", expression = "java(mezclarAsignaturasYNotas(asignaturas, notas, evaluacionAAsignatura))")
    PerfilEstudianteResponseDTO toResponse(
            EstudianteResponseDTO estudiante, 
            List<AsignaturaResponseDTO> asignaturas, 
            List<NotaResponseDTO> notas,
            Map<Long, Long> evaluacionAAsignatura
    );

    // 2. El método auxiliar que cruza la información de ambas listas
    default List<AsignaturaDetalleDTO> mezclarAsignaturasYNotas(
            List<AsignaturaResponseDTO> asignaturas, 
            List<NotaResponseDTO> notas,
            Map<Long, Long> evaluacionAAsignatura) {
        
        if (asignaturas == null || asignaturas.isEmpty()) {
            return Collections.emptyList();
        }
        if (notas == null) {
            notas = Collections.emptyList();
        }
        if (evaluacionAAsignatura == null) {
            evaluacionAAsignatura = Collections.emptyMap();
        }

        // A. Agrupamos las notas por la asignatura de su evaluación
        final Map<Long, Long> evaluacionAAsignaturaFinal = evaluacionAAsignatura;
        Map<Long, List<NotaDetalleDTO>> mapaNotasPorAsignatura = notas.stream()
            .filter(nota -> evaluacionAAsignaturaFinal.containsKey(nota.idEvaluacion()))
            .collect(Collectors.groupingBy(
                nota -> evaluacionAAsignaturaFinal.get(nota.idEvaluacion()),
                Collectors.mapping(nota -> new NotaDetalleDTO(nota.id(), nota.score()), Collectors.toList())
            ));

        // B. Iteramos las asignaturas y les inyectamos las notas que acabamos de agrupar
        return asignaturas.stream()
            .map(asig -> new AsignaturaDetalleDTO(
                asig.id(),
                asig.name(),
                asig.description(),
                // Buscamos si hay notas para este ID, si no, pasamos lista vacía
                mapaNotasPorAsignatura.getOrDefault(asig.id(), Collections.emptyList()) 
            ))
            .toList();
    }
}
