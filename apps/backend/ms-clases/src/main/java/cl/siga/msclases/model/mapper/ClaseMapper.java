package cl.siga.msclases.model.mapper;

import cl.siga.coreshare.dto.clase.ActualizarDocenteJefeRequestDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.clase.RegistrarClaseRequestDTO;
import cl.siga.msclases.model.entity.Clase;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ClaseMapper {
  // Mapea un DTO de solicitud a una entidad de Clase.
  Clase toEntity(RegistrarClaseRequestDTO requestDto);

  // Mapea un DTO de modificación a una entidad de Clase.
  void updateEntityFromDto(ActualizarDocenteJefeRequestDTO dto, @MappingTarget Clase clase);

  // Mapea una entidad de Clase a un DTO de respuesta.
  ClaseResponseDTO toResponseDto(Clase clase);

  // Mapea una lista de entidades de Clase a una lista de DTOs de respuesta.
  List<ClaseResponseDTO> toResponseDtoList(List<Clase> clases);
}
