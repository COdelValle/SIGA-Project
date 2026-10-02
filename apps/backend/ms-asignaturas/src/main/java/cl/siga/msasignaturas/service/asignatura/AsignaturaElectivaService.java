package cl.siga.msasignaturas.service.asignatura;

import java.util.List;
import java.util.Map;
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
import cl.siga.msasignaturas.model.entity.asignatura.AsignaturaElectiva;
import cl.siga.msasignaturas.model.mapper.AsignaturaMapper;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaElectivaRepository;
import lombok.RequiredArgsConstructor;

@Service 
@RequiredArgsConstructor 
public class AsignaturaElectivaService {

    private final AsignaturaElectivaRepository repository;
    private final AsignaturaMapper mapper;
    private final DocenteClient docenteClient;

    // Agrupación oficial MINEDUC para el Plan Diferenciado HC
    private static final Set<AreaAcademica> AREA_A = Set.of(
            AreaAcademica.LENGUAJE, AreaAcademica.HISTORIA, AreaAcademica.IDIOMAS);
    private static final Set<AreaAcademica> AREA_B = Set.of(
            AreaAcademica.MATEMATICAS, AreaAcademica.CIENCIAS, AreaAcademica.TECNOLOGIA);
    private static final Set<AreaAcademica> AREA_C = Set.of(
            AreaAcademica.ARTES, AreaAcademica.EDUCACION_FISICA);

    @Transactional 
    public AsignaturaResponseDTO crearElectiva(RegistrarAsignaturaElectivaRequestDTO dto) {
        // Validación de existencia de Docente
        if (dto.idDocente() != null && !docenteClient.existsById(dto.idDocente())) {
            throw new BusinessException("El docente con ID " + dto.idDocente() + " no existe o no está activo.");
        }

        AsignaturaElectiva entidad = mapper.toEntity(dto);
        return mapper.toResponseDto(repository.save(entidad));
    }

    @Transactional
    public AsignaturaResponseDTO actualizarElectiva(Long id, ActualizarAsignaturaElectivaRequestDTO dto) {
        AsignaturaElectiva entidad = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura Electiva con ID " + id + " no encontrada."));

        // Validación si se actualiza el Docente
        if (dto.idDocente() != null && !dto.idDocente().equals(entidad.getIdDocente())) {
            if (!docenteClient.existsById(dto.idDocente())) {
                throw new BusinessException("El docente con ID " + dto.idDocente() + " no existe o no está activo.");
            }
        }

        // Validar que no se reduzcan los cupos por debajo de las inscripciones actuales
        if (dto.cupoMaximo() != null) {
            int inscritosActuales = entidad.getInscripciones() != null ? entidad.getInscripciones().size() : 0;
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