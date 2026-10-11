package cl.siga.msapoderados.model.mapper;

import cl.siga.coreshare.dto.apoderado.ActualizarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.apoderado.RegistrarApoderadoRequestDTO;
import cl.siga.coreshare.dto.apoderado.parentesco.ParentescoEstudianteDTO;
import cl.siga.msapoderados.model.entity.Apoderado;
import cl.siga.msapoderados.model.entity.ApoderadoEstudiante;
import org.mapstruct.*;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface ApoderadoMapper {
  // Mapea un DTO de creación a una entidad de Apoderado.
  Apoderado toEntity(RegistrarApoderadoRequestDTO requestDto);

  // Mapea un DTO de modificación a una entidad de Apoderado.
  void updateEntityFromDto(ActualizarApoderadoRequestDTO dto, @MappingTarget Apoderado apoderado);

  // Mapea una entidad de Apoderado a un DTO de respuesta.
  ApoderadoResponseDTO toResponseDto(Apoderado apoderado);

  // Mapea una lista de entidades de Apoderado a una lista de DTOs de respuesta.
  List<ApoderadoResponseDTO> toResponseDtoList(List<Apoderado> apoderados);

  // --- Mapeos Auxiliares para la clase Embebida ---

  // Mapea el DTO de parentesco al objeto embebible de la entidad
  ApoderadoEstudiante toApoderadoEstudianteEntity(ParentescoEstudianteDTO dto);

  // Mapea el objeto embebible de la entidad al DTO de parentesco para la respuesta
  ParentescoEstudianteDTO toParentescoEstudianteDto(ApoderadoEstudiante entity);
}
