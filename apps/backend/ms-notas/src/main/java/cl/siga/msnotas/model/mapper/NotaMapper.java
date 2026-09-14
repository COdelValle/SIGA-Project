package cl.siga.msnotas.model.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import cl.siga.msnotas.model.entity.Nota;

@Mapper (
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface NotaMapper {
    // Mapea un DTO de solicitud a una entidad de nota.
    Nota toEntity(RegistrarNotaRequestDTO request);

    // Mapea un DTO de solicitud a una entidad de nota existente.
    void updateEntityFromDto(ActualizarNotaRequestDTO request, @MappingTarget Nota nota);

    // Mapea una entidad de nota a un DTO de respuesta.
    NotaResponseDTO toResponseDto(Nota nota);

    // Mapea una lista de entidades de nota a una lista de DTOs de respuesta.
    List<NotaResponseDTO> toResponseDtoList(List<Nota> notas);
}
