package cl.siga.msasignaturas.model.mapper;

import java.util.List;

import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.basica.ActualizarAsignaturaBasicaRequestDTO;
import cl.siga.coreshare.dto.asignatura.basica.RegistrarAsignaturaBasicaRequestDTO;
import cl.siga.coreshare.dto.asignatura.electiva.ActualizarAsignaturaElectivaRequestDTO;
import cl.siga.coreshare.dto.asignatura.electiva.RegistrarAsignaturaElectivaRequestDTO;
import cl.siga.coreshare.dto.asignatura.inscripcion.enums.EstadoInscripcion;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaBasica;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;

@Mapper(
    componentModel = "spring",
    uses = {HorarioMapper.class, InscripcionMapper.class},
    nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface AsignaturaMapper {

    // ==========================================
    // 1. Mapeos de Request a Entidad (Creación)
    // ==========================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    AsignaturaBasica toEntity(RegistrarAsignaturaBasicaRequestDTO request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "inscripciones", ignore = true)
    AsignaturaElectiva toEntity(RegistrarAsignaturaElectivaRequestDTO request);

    // Enlaza la relación bidireccional (asigna la Asignatura a cada Horario) antes de persistir
    @AfterMapping 
    default void linkHorarios(@MappingTarget Asignatura asignatura) {
        if (asignatura.getHorarios() != null) {
            asignatura.getHorarios().forEach(horario -> horario.setAsignatura(asignatura));
        }
    }

    // ==========================================
    // 2. Mapeos de Actualización Parcial
    // ==========================================

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "semestre", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "horarios", ignore = true)
    @Mapping(target = "idClase", ignore = true)
    void updateBasicaFromDto(ActualizarAsignaturaBasicaRequestDTO dto, @MappingTarget AsignaturaBasica entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "semestre", ignore = true)
    @Mapping(target = "active", ignore = true)
    @Mapping(target = "horarios", ignore = true)
    @Mapping(target = "inscripciones", ignore = true)
    void updateElectivaFromDto(ActualizarAsignaturaElectivaRequestDTO dto, @MappingTarget AsignaturaElectiva entity);

    // ==========================================
    // 3. Mapeo Polimórfico a Response DTO
    // ==========================================

    default AsignaturaResponseDTO toResponseDto(Asignatura asignatura) {
        if (asignatura == null) {
            return null;
        }
        return switch (asignatura) {
            case AsignaturaBasica basica -> toBasicaResponseDto(basica);
            case AsignaturaElectiva electiva -> toElectivaResponseDto(electiva);
            default -> throw new BusinessException("Tipo de asignatura no soportado: " + asignatura.getClass());
        };
    }

    default List<AsignaturaResponseDTO> toResponseDtoList(List<Asignatura> asignaturas) {
        if (asignaturas == null) {
            return List.of();
        }
        return asignaturas.stream()
            .map(this::toResponseDto)
            .toList();
    }

    // Métodos auxiliares para la construcción específica del Response
    @Mapping(target = "tipo", constant = "BASICA")
    @Mapping(target = "cupoMaximo", ignore = true)
    @Mapping(target = "cuposDisponibles", ignore = true)
    @Mapping(target = "totalInscritos", ignore = true)
    @Mapping(target = "inscripciones", ignore = true)
    AsignaturaResponseDTO toBasicaResponseDto(AsignaturaBasica basica);

    @Mapping(target = "tipo", constant = "ELECTIVA")
    @Mapping(target = "idClase", ignore = true)
    @Mapping(target = "totalInscritos", expression = "java(calcularTotalInscritos(electiva))")
    @Mapping(target = "cuposDisponibles", expression = "java(calcularCuposDisponibles(electiva))")
    AsignaturaResponseDTO toElectivaResponseDto(AsignaturaElectiva electiva);

    // ==========================================
    // 4. Calculadores de cupos para electivos
    // ==========================================
    
    default Integer calcularTotalInscritos(AsignaturaElectiva electiva) {
        if (electiva.getInscripciones() == null) {
            return 0;
        }
        // Se cuentan solo los alumnos que realmente ocupan un cupo
        return (int) electiva.getInscripciones().stream()
                .filter(inscripcion -> 
                    inscripcion.getEstado() == EstadoInscripcion.ACTIVO || 
                    inscripcion.getEstado() == EstadoInscripcion.PRE_INSCRITO)
                .count();
    }

    default Integer calcularCuposDisponibles(AsignaturaElectiva electiva) {
        if (electiva.getCupoMaximo() == null) {
            return 0;
        }
        int inscritos = calcularTotalInscritos(electiva);
        return Math.max(0, electiva.getCupoMaximo() - inscritos);
    }
}