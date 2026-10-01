package cl.siga.msevaluaciones.model.mapper;

import cl.siga.coreshare.dto.evaluaciones.ActualizarEvaluacionRequestDTO;
import cl.siga.coreshare.dto.evaluaciones.EvaluacionResponseDTO;
import cl.siga.coreshare.dto.evaluaciones.RegistrarEvaluacionRequestDTO;
import cl.siga.msevaluaciones.model.entity.Evaluacion;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import java.util.List;

@Mapper(
  componentModel = "spring",
  nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface EvaluacionMapper {

  // Mapea un DTO de creación a una entidad Evaluacion.
  Evaluacion toEntity(RegistrarEvaluacionRequestDTO requestDto);

  // Mapea un DTO de modificación a una entidad Evaluacion existente.
  void updateEntityFromDto(ActualizarEvaluacionRequestDTO dto, @MappingTarget Evaluacion evaluacion);

  // Mapea una entidad Evaluacion a un DTO de respuesta.
  EvaluacionResponseDTO toResponseDto(Evaluacion evaluacion);

  // Mapea una lista de entidades Evaluacion a una lista de DTOs de respuesta.
  List<EvaluacionResponseDTO> toResponseDtoList(List<Evaluacion> evaluaciones);
}
