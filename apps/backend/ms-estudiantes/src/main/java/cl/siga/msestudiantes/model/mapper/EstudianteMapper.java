package cl.siga.msestudiantes.model.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.msestudiantes.model.entity.Estudiante;

// MapStruct hace el mapeo de forma automática sin configuraciones extra.
@Mapper(componentModel = "spring")
public interface EstudianteMapper {
    // Mapea un DTO de solicitud a una entidad de estudiante.
    @Mapping (target = "id", ignore = true)
    Estudiante toEntity(RegistrarEstudianteRequestDTO requestDto);

    // Mapea un DTO de modificación a una entidad de estudiante.
    void updateEntityFromDto(ActualizarEstudianteRequestDTO dto, @MappingTarget Estudiante estudiante);

    // Mapea una entidad de estudiante a un DTO de respuesta.
    EstudianteResponseDTO toResponseDto(Estudiante estudiante);
}
