package cl.siga.msasignaturas.model.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.msasignaturas.model.entity.Horario;

@Mapper (
    componentModel = "spring",
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface HorarioMapper {

    @Mapping (target = "id", ignore = true)
    @Mapping(target = "asignatura", ignore = true)
    Horario toEntity(HorarioRequestDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "asignatura", ignore = true)
    void updateEntityFromDto(HorarioRequestDTO dto, @MappingTarget Horario entity);

    HorarioResponseDTO toDto(Horario entity);

    List<HorarioResponseDTO> toDtoList(List<Horario> entities);
}