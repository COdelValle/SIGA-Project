package cl.siga.msasignaturas.model.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import cl.siga.coreshare.dto.asignatura.AsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.msasignaturas.model.entity.Asignatura;

@Mapper (
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AsignaturaMapper {
    // Mapea un DTO de solicitud a una entidad de asignatura.
    Asignatura toEntity(AsignaturaRequestDTO requestDto);

    // Mapea un DTO de modificación a una entidad de asignatura.
    void updateEntityFromDto(AsignaturaRequestDTO dto, @MappingTarget Asignatura asignatura);

    // Mapea una entidad de asignatura a un DTO de respuesta.
    AsignaturaResponseDTO toResponseDto(Asignatura asignatura);

    // Mapea una lista de entidades de asignatura a una lista de DTOs de respuesta.
    List<AsignaturaResponseDTO> toResponseDtoList(List<Asignatura> asignaturas);
}
