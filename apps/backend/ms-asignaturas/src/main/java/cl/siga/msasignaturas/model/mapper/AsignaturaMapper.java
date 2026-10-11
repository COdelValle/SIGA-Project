package cl.siga.msasignaturas.model.mapper;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.asignatura.ActualizarAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;

@Component
public class AsignaturaMapper {

    public AsignaturaResponseDTO toDto(Asignatura asignatura) {
        if (asignatura == null) {
            return null;
        }
        return new AsignaturaResponseDTO(
            asignatura.getId(),
            asignatura.getNombre(),
            asignatura.getNombreCorto(),
            asignatura.getDescripcion(),
            asignatura.getArea(),
            asignatura.isCalificable(),
            asignatura.isActive()
        );
    }

    public void updateEntity(ActualizarAsignaturaRequestDTO dto, Asignatura entity) {
        entity.setNombre(dto.nombre());
        entity.setNombreCorto(dto.nombreCorto());
        entity.setDescripcion(dto.descripcion());
        entity.setArea(dto.area());
        entity.setCalificable(dto.calificable());
    }
}
