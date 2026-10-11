package cl.siga.msasignaturas.model.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import cl.siga.coreshare.dto.asignatura.inscripcion.InscripcionResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.RegistrarInscripcionRequestDTO;
import cl.siga.msasignaturas.model.entity.Inscripcion;

@Mapper(componentModel = "spring")
public interface InscripcionMapper {

    @Mapping(target = "idCursoAsignatura", source = "cursoAsignatura.id")
    InscripcionResponseDTO toDto(Inscripcion inscripcion);

    List<InscripcionResponseDTO> toDtoList(List<Inscripcion> inscripciones);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "cursoAsignatura", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaInscripcion", ignore = true)
    Inscripcion toEntity(RegistrarInscripcionRequestDTO request);
}