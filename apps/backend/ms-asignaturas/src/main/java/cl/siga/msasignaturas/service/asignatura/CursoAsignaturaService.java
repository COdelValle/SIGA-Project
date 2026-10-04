package cl.siga.msasignaturas.service.asignatura;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.CursoAsignaturaResponseDTO;
import cl.siga.coreshare.dto.asignatura.curso.ActualizarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.curso.RegistrarCursoAsignaturaRequestDTO;
import cl.siga.coreshare.dto.asignatura.enums.CaracterAsignatura;
import cl.siga.coreshare.dto.asignatura.enums.PlanFormacion;
import cl.siga.coreshare.dto.asignatura.enums.Semestre;
import cl.siga.coreshare.dto.clase.ClaseResponseDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.client.ClaseClient;
import cl.siga.msasignaturas.client.DocenteClient;
import cl.siga.msasignaturas.model.InscripcionEstados;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import cl.siga.msasignaturas.model.mapper.CursoAsignaturaMapper;
import cl.siga.msasignaturas.model.specifications.CursoAsignaturaSpecifications;
import cl.siga.msasignaturas.repository.InscripcionRepository;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;
import cl.siga.msasignaturas.repository.asignatura.MallaCurricularRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CursoAsignaturaService {

    // Agrupación MINEDUC para el plan diferenciada (humanidades, ciencias, artes).
    private static final Set<AreaAcademica> AREA_A = Set.of(
            AreaAcademica.LENGUAJE, AreaAcademica.HISTORIA, AreaAcademica.IDIOMAS,
            AreaAcademica.CIUDADANIA, AreaAcademica.FILOSOFIA);
    private static final Set<AreaAcademica> AREA_B = Set.of(
            AreaAcademica.MATEMATICAS, AreaAcademica.CIENCIAS, AreaAcademica.CIENCIAS_CIUDADANIA,
            AreaAcademica.TECNOLOGIA, AreaAcademica.ECONOMIA);
    private static final Set<AreaAcademica> AREA_C = Set.of(
            AreaAcademica.ARTES, AreaAcademica.EDUCACION_FISICA);

    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final MallaCurricularRepository mallaCurricularRepository;
    private final InscripcionRepository inscripcionRepository;
    private final CursoAsignaturaMapper cursoAsignaturaMapper;
    private final ClaseClient claseClient;
    private final DocenteClient docenteClient;

    @Transactional(readOnly = true)
    public Page<CursoAsignaturaResponseDTO> searchCursoAsignaturas(
            Long idClase, Long idDocente, Long idAsignatura, CaracterAsignatura caracter,
            Semestre semestre, AreaAcademica area, String nombre, Boolean conCupoDisponible,
            Pageable pageable) {

        Specification<CursoAsignatura> spec = Specification.where(CursoAsignaturaSpecifications.isActive())
                .and(CursoAsignaturaSpecifications.hasIdClase(idClase))
                .and(CursoAsignaturaSpecifications.hasIdDocente(idDocente))
                .and(CursoAsignaturaSpecifications.hasIdAsignatura(idAsignatura))
                .and(CursoAsignaturaSpecifications.hasCaracter(caracter))
                .and(CursoAsignaturaSpecifications.hasSemestre(semestre))
                .and(CursoAsignaturaSpecifications.hasArea(area))
                .and(CursoAsignaturaSpecifications.hasNombre(nombre))
                .and(CursoAsignaturaSpecifications.hasCuposDisponibles(conCupoDisponible));

        return cursoAsignaturaRepository.findAll(spec, pageable).map(cursoAsignaturaMapper::toDto);
    }

    @Transactional(readOnly = true)
    public CursoAsignaturaResponseDTO obtenerPorId(Long id) {
        return cursoAsignaturaMapper.toDto(cursoAsignaturaRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dictación con ID " + id + " no encontrada o inactiva.")));
    }

    @Transactional(readOnly = true)
    public boolean existCursoAsignatura(Long id) {
        return cursoAsignaturaRepository.existsByIdAndActiveTrue(id);
    }

    @Transactional
    public CursoAsignaturaResponseDTO crear(RegistrarCursoAsignaturaRequestDTO dto) {
        Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrue(dto.idAsignatura())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura no encontrada o inactiva."));

        ClaseResponseDTO clase = claseClient.getClaseById(dto.idClase());
        if (!mallaCurricularRepository.existsByNivelAndAsignaturaIdAndPlanAndActiveTrue(
                clase.nivel(), dto.idAsignatura(), PlanFormacion.COMUN)) {
            throw new BusinessException(
                "La asignatura no está en la malla curricular del nivel " + clase.nivel().getDescripcion() + ".");
        }

        if (!docenteClient.existsById(dto.idDocente())) {
            throw new BusinessException("El docente con ID " + dto.idDocente() + " no existe o no está activo.");
        }

        Integer cupo = normalizarCupo(dto.caracter(), dto.cupoMaximo());

        return cursoAsignaturaRepository
                .findFirstByAsignaturaIdAndIdClaseAndSemestre(dto.idAsignatura(), dto.idClase(), dto.semestre())
                .map(existente -> {
                    if (existente.isActive()) {
                        throw new BusinessException("Ya existe una dictación de esa asignatura para el curso y semestre.");
                    }
                    existente.setIdDocente(dto.idDocente());
                    existente.setCaracter(dto.caracter());
                    existente.setCupoMaximo(cupo);
                    existente.setActive(true);
                    existente.getHorarios().clear();
                    CursoAsignatura nueva = cursoAsignaturaMapper.toEntity(dto, asignatura);
                    nueva.getHorarios().forEach(horario -> existente.addHorario(horario));
                    return cursoAsignaturaMapper.toDto(cursoAsignaturaRepository.save(existente));
                })
                .orElseGet(() -> cursoAsignaturaMapper.toDto(
                    cursoAsignaturaRepository.save(cursoAsignaturaMapper.toEntity(dto, asignatura))));
    }

    @Transactional
    public CursoAsignaturaResponseDTO actualizar(Long id, ActualizarCursoAsignaturaRequestDTO dto) {
        CursoAsignatura curso = cursoAsignaturaRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dictación con ID " + id + " no encontrada."));

        if (!dto.idDocente().equals(curso.getIdDocente()) && !docenteClient.existsById(dto.idDocente())) {
            throw new BusinessException("El docente con ID " + dto.idDocente() + " no existe o no está activo.");
        }

        Integer cupo = normalizarCupo(dto.caracter(), dto.cupoMaximo());
        if (cupo != null) {
            int inscritos = inscripcionRepository.countByCursoAsignaturaIdAndEstadoIn(
                    id, InscripcionEstados.OCUPAN_CUPO);
            if (cupo < inscritos) {
                throw new BusinessException("El nuevo cupo máximo (" + cupo +
                        ") no puede ser menor a la cantidad de alumnos ya inscritos (" + inscritos + ").");
            }
        }

        cursoAsignaturaMapper.updateEntity(dto, curso);
        curso.setCupoMaximo(cupo);
        return cursoAsignaturaMapper.toDto(cursoAsignaturaRepository.save(curso));
    }

    @Transactional
    public void eliminar(Long id) {
        CursoAsignatura curso = cursoAsignaturaRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dictación con ID " + id + " no encontrada."));

        curso.setActive(false);
        curso.getHorarios().forEach(horario -> horario.setActive(false));
        cursoAsignaturaRepository.save(curso);
    }

    /**
     * Valida si el colegio cumple la normativa MINEDUC para 3° y 4° Medio:
     * mínimo 6 electivos y presencia en al menos 2 de las 3 áreas.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> validarCumplimientoMineduc() {
        List<CursoAsignatura> electivos = cursoAsignaturaRepository.findAll(
                Specification.where(CursoAsignaturaSpecifications.isActive())).stream()
            .filter(curso -> curso.getCaracter() != CaracterAsignatura.OBLIGATORIA)
            .toList();

        long countAreaA = electivos.stream().filter(c -> AREA_A.contains(c.getAsignatura().getArea())).count();
        long countAreaB = electivos.stream().filter(c -> AREA_B.contains(c.getAsignatura().getArea())).count();
        long countAreaC = electivos.stream().filter(c -> AREA_C.contains(c.getAsignatura().getArea())).count();

        int areasCubiertas = 0;
        if (countAreaA > 0) areasCubiertas++;
        if (countAreaB > 0) areasCubiertas++;
        if (countAreaC > 0) areasCubiertas++;

        boolean cumpleTotal = electivos.size() >= 6;
        boolean cumpleAreas = areasCubiertas >= 2;

        return Map.of(
                "totalElectivosOfrecidos", electivos.size(),
                "areasCubiertas", areasCubiertas,
                "cumpleNormativaMineduc", cumpleTotal && cumpleAreas,
                "detallePorArea", Map.of(
                        "Area_A_Humanidades", countAreaA,
                        "Area_B_Ciencias_Matematicas", countAreaB,
                        "Area_C_Artes_EducacionFisica", countAreaC
                )
        );
    }

    private Integer normalizarCupo(CaracterAsignatura caracter, Integer cupo) {
        if (caracter == CaracterAsignatura.OBLIGATORIA) {
            return null;
        }
        if (cupo == null) {
            throw new BusinessException("Las asignaturas optativas y electivas requieren cupo máximo.");
        }
        return cupo;
    }
}
