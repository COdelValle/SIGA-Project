package cl.siga.bffweb.domain.estudiantes.mapper;

import java.util.List;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import cl.siga.bffweb.domain.estudiantes.dto.api.PerfilEstudianteResponseDTO;
import cl.siga.bffweb.domain.estudiantes.dto.internal.AsignaturaDetalleDTO;
import cl.siga.bffweb.domain.estudiantes.dto.internal.ClaseDetalleDTO;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;

@Mapper (componentModel = "spring")
public interface PerfilEstudianteMapper {
    @Mapping (target = "id", source = "estudiante.id")
    @Mapping (target = "idUsuario", source = "estudiante.idUsuario")
    @Mapping (target = "rut", source = "estudiante.rut")
    @Mapping (target = "firstName", source = "estudiante.firstName")
    @Mapping (target = "middleName", source = "estudiante.middleName")
    @Mapping (target = "firstSurname", source = "estudiante.firstSurname")
    @Mapping (target = "secondSurname", source = "estudiante.secondSurname")
    @Mapping (target = "birthDate", source = "estudiante.birthDate")
    @Mapping (target = "allergies", source = "estudiante.allergies")
    @Mapping (target = "state", source = "estudiante.state")
    @Mapping (target = "idClase", source = "estudiante.idClase")
    @Mapping (target = "clase", expression = "java(toClaseDetalle(clase))")
    @Mapping (target = "asignaturas", source = "asignaturas")
    PerfilEstudianteResponseDTO toResponse(
            EstudianteResponseDTO estudiante,
            ClaseResponseDTO clase,
            List<AsignaturaDetalleDTO> asignaturas);

    default ClaseDetalleDTO toClaseDetalle(ClaseResponseDTO clase) {
        if (clase == null) {
            return null;
        }
        return new ClaseDetalleDTO(
            clase.id(),
            clase.nivel() == null ? null : clase.nivel().getDescripcion(),
            clase.letra(),
            clase.anioAcademico());
    }
}
