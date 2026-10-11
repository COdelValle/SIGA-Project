package cl.siga.msasignaturas.model.mapper;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.asignatura.malla.MallaCurricularResponseDTO;
import cl.siga.msasignaturas.model.entity.asignatura.MallaCurricular;

@Component
public class MallaCurricularMapper {

    public MallaCurricularResponseDTO toDto(MallaCurricular malla) {
        if (malla == null) {
            return null;
        }
        return new MallaCurricularResponseDTO(
            malla.getId(),
            malla.getNivel(),
            malla.getAsignatura().getId(),
            malla.getAsignatura().getNombre(),
            malla.getAsignatura().getArea(),
            malla.getCaracter(),
            malla.getPlan(),
            malla.getHorasSemanales(),
            malla.getAsignatura().isCalificable(),
            malla.isActive()
        );
    }
}
