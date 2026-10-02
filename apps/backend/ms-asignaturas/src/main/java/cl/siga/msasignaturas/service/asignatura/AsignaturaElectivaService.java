package cl.siga.msasignaturas.service.asignatura;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import cl.siga.coreshare.dto.asignatura.AsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.electiva.ActualizarAsignaturaElectivaRequestDTO;
import cl.siga.coreshare.dto.asignatura.electiva.RegistrarAsignaturaElectivaRequestDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.client.DocenteClient;
import cl.siga.msasignaturas.model.InscripcionEstados;
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;
import cl.siga.msasignaturas.model.mapper.AsignaturaMapper;
import cl.siga.msasignaturas.repository.InscripcionRepository;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaElectivaRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AsignaturaElectivaService {

    private final AsignaturaElectivaRepository repository;
    private final AsignaturaMapper mapper;
    private final DocenteClient docenteClient;
    private final InscripcionRepository inscripcionRepository;

    // Agrupación oficial MINEDUC para el Plan Diferenciado HC
    private static final Set<AreaAcademica> AREA_A = Set.of(
            AreaAcademica.LENGUAJE, AreaAcademica.HISTORIA, AreaAcademica.IDIOMAS);
    private static final Set<AreaAcademica> AREA_B = Set.of(
            AreaAcademica.MATEMATICAS, AreaAcademica.CIENCIAS, AreaAcademica.TECNOLOGIA);
    private static final Set<AreaAcademica> AREA_C = Set.of(
            AreaAcademica.ARTES, AreaAcademica.EDUCACION_FISICA);

    @Transactional 
    public AsignaturaResponseDTO crearElectiva(RegistrarAsignaturaElectivaRequestDTO dto) {
        Optional<AsignaturaElectiva> existente = repository
                .findFirstByNameIgnoreCaseAndSemestre(dto.name(), dto.semestre());

        if (existente.isPresent() && existente.get().isActive()) {
            throw new BusinessException("Ya existe una asignatura electiva con ese nombre y semestre.");
        }

        // Validación de existencia de Docente
        if (dto.idDocente() != null && !docenteClient.existsById(dto.idDocente())) {
            throw new BusinessException("El docente con ID " + dto.idDocente() + " no existe o no está activo.");
        }

        // Si la asignatura fue eliminada logicamente, se reactiva en vez de chocar con el UNIQUE.
        if (existente.isPresent()) {
            AsignaturaElectiva entidad = existente.get();
            AsignaturaElectiva nueva = mapper.toEntity(dto);
            entidad.setName(nueva.getName());
            entidad.setDescription(nueva.getDescription());
            entidad.setArea(nueva.getArea());
            entidad.setIdDocente(nueva.getIdDocente());
            entidad.setCupoMaximo(nueva.getCupoMaximo());
            entidad.setActive(true);
            entidad.getHorarios().clear();
            nueva.getHorarios().forEach(horario -> {
                horario.setAsignatura(entidad);
                entidad.getHorarios().add(horario);
            });
            return mapper.toResponseDto(repository.save(entidad));
        }

        AsignaturaElectiva entidad = mapper.toEntity(dto);
        return mapper.toResponseDto(repository.save(entidad));
    }

    @Transactional
    public AsignaturaResponseDTO actualizarElectiva(Long id, ActualizarAsignaturaElectivaRequestDTO dto) {
        AsignaturaElectiva entidad = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura Electiva con ID " + id + " no encontrada."));

        if (repository.existsByNameIgnoreCaseAndSemestreAndActiveTrueAndIdNot(dto.name(), entidad.getSemestre(), id)) {
            throw new BusinessException("Ya existe una asignatura electiva con ese nombre y semestre.");
        }

        // Validación si se actualiza el Docente
        if (dto.idDocente() != null && !dto.idDocente().equals(entidad.getIdDocente())) {
            if (!docenteClient.existsById(dto.idDocente())) {
                throw new BusinessException("El docente con ID " + dto.idDocente() + " no existe o no está activo.");
            }
        }

        // Validar que no se reduzcan los cupos por debajo de las inscripciones que ocupan cupo
        if (dto.cupoMaximo() != null) {
            int inscritosActuales = inscripcionRepository.countByAsignaturaIdAndEstadoIn(
                    entidad.getId(), InscripcionEstados.OCUPAN_CUPO);
            if (dto.cupoMaximo() < inscritosActuales) {
                throw new BusinessException("El nuevo cupo máximo (" + dto.cupoMaximo() + 
                        ") no puede ser menor a la cantidad de alumnos ya inscritos (" + inscritosActuales + ").");
            }
        }

        mapper.updateElectivaFromDto(dto, entidad);
        return mapper.toResponseDto(repository.save(entidad));
    }

    /**
     * Valida si el colegio cumple con la normativa del MINEDUC para 3° y 4° Medio:
     * - Mínimo 6 electivos en total.
     * - Distribuidos en al menos 2 de las 3 áreas (A, B o C).
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validarCumplimientoMineduc() {
        List<AsignaturaElectiva> electivosActivos = repository.findAll().stream()
                .filter(AsignaturaElectiva::isActive)
                .toList();

        long countAreaA = electivosActivos.stream().filter(e -> AREA_A.contains(e.getArea())).count();
        long countAreaB = electivosActivos.stream().filter(e -> AREA_B.contains(e.getArea())).count();
        long countAreaC = electivosActivos.stream().filter(e -> AREA_C.contains(e.getArea())).count();

        int areasCubiertas = 0;
        if (countAreaA > 0) areasCubiertas++;
        if (countAreaB > 0) areasCubiertas++;
        if (countAreaC > 0) areasCubiertas++;

        boolean cumpleTotal = electivosActivos.size() >= 6;
        boolean cumpleAreas = areasCubiertas >= 2;

        return Map.of(
                "totalElectivosOfrecidos", electivosActivos.size(),
                "areasCubiertas", areasCubiertas,
                "cumpleNormativaMineduc", cumpleTotal && cumpleAreas,
                "detallePorArea", Map.of(
                        "Area_A_Humanidades", countAreaA,
                        "Area_B_Ciencias_Matematicas", countAreaB,
                        "Area_C_Artes_Tecnologia", countAreaC
                )
        );
    }
}