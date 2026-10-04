package cl.siga.msasignaturas.model.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.curso.ActualizarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.curso.RegistrarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.Inscripcion;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;

@Component
public class CursoAsignaturaMapper {

    public CursoAsignaturaResponseDTO toDto(CursoAsignatura curso) {
        if (curso == null) {
            return null;
        }
        Asignatura asignatura = curso.getAsignatura();
        int totalInscritos = calcularTotalInscritos(curso);
        Integer cuposDisponibles = curso.getCupoMaximo() == null
            ? null
            : Math.max(0, curso.getCupoMaximo() - totalInscritos);

        return new CursoAsignaturaResponseDTO(
            curso.getId(),
            asignatura.getId(),
            asignatura.getNombre(),
            asignatura.getDescripcion(),
            asignatura.getArea(),
            asignatura.isCalificable(),
            curso.getCaracter(),
            curso.getSemestre(),
            curso.getIdDocente(),
            curso.getIdClase(),
            curso.getCupoMaximo(),
            cuposDisponibles,
            totalInscritos,
            horariosActivos(curso)
        );
    }

    public CursoAsignatura toEntity(RegistrarCursoAsignaturaRequestDTO dto, Asignatura asignatura) {
        CursoAsignatura curso = CursoAsignatura.builder()
            .asignatura(asignatura)
            .idClase(dto.idClase())
            .idDocente(dto.idDocente())
            .semestre(dto.semestre())
            .caracter(dto.caracter())
            .cupoMaximo(dto.cupoMaximo())
            .active(true)
            .build();
        if (dto.horarios() != null) {
            dto.horarios().forEach(horario -> curso.addHorario(Horario.builder()
                .dia(horario.dia())
                .horarioEntrada(horario.horarioEntrada())
                .horarioSalida(horario.horarioSalida())
                .ubicacion(horario.ubicacion())
                .active(true)
                .build()));
        }
        return curso;
    }

    public void updateEntity(ActualizarCursoAsignaturaRequestDTO dto, CursoAsignatura entity) {
        entity.setIdDocente(dto.idDocente());
        entity.setSemestre(dto.semestre());
        entity.setCaracter(dto.caracter());
        entity.setCupoMaximo(dto.cupoMaximo());
    }

    private List<HorarioResponseDTO> horariosActivos(CursoAsignatura curso) {
        if (curso.getHorarios() == null) {
            return List.of();
        }
        return curso.getHorarios().stream()
            .filter(Horario::isActive)
            .map(h -> new HorarioResponseDTO(
                h.getId(), h.getDia(), h.getHorarioEntrada(), h.getHorarioSalida(), h.getUbicacion()))
            .toList();
    }

    private int calcularTotalInscritos(CursoAsignatura curso) {
        if (curso.getInscripciones() == null) {
            return 0;
        }
        return (int) curso.getInscripciones().stream()
            .filter(inscripcion ->
                inscripcion.getEstado() == EstadoInscripcion.ACTIVO ||
                inscripcion.getEstado() == EstadoInscripcion.PRE_INSCRITO)
            .count();
    }
}
