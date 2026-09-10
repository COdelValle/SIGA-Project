package cl.siga.msestudiantes.model.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.msestudiantes.model.entity.Estudiante;

// MapStruct hace el mapeo de forma automática sin configuraciones extra.
@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EstudianteMapper {
    // Mapea un DTO de solicitud a una entidad de estudiante.
    Estudiante toEntity(RegistrarEstudianteRequestDTO requestDto);

    // Mapea un DTO de modificación a una entidad de estudiante.
    void updateEntityFromDto(ActualizarEstudianteRequestDTO dto, @MappingTarget Estudiante estudiante);

    // Mapea una entidad de estudiante a un DTO de respuesta.
    EstudianteResponseDTO toResponseDto(Estudiante estudiante);

    // Mapea una lista de entidades de estudiante a una lista de DTOs de respuesta.
    List<EstudianteResponseDTO> toResponseDtoList(List<Estudiante> estudiantes);
}