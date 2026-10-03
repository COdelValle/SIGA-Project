package cl.siga.msasistencias.model.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import cl.siga.coreshare.dto.asistencia.ActualizarAsistenciaRequestDTO;
import cl.siga.coreshare.dto.asistencia.AsistenciaResponseDTO;
import cl.siga.coreshare.dto.asistencia.RegistrarAsistenciaRequestDTO;
import cl.siga.msasistencias.model.entity.Asistencia;

@Mapper(
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AsistenciaMapper {
    Asistencia toEntity(RegistrarAsistenciaRequestDTO requestDto);

    void updateEntityFromDto(ActualizarAsistenciaRequestDTO dto, @MappingTarget Asistencia asistencia);

    AsistenciaResponseDTO toResponseDto(Asistencia asistencia);

    List<AsistenciaResponseDTO> toResponseDtoList(List<Asistencia> asistencias);
}
