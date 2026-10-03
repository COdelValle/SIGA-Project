package cl.siga.msestudiantes.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cl.siga.coreshare.dto.apoderado.ApoderadoResponseDTO;
import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.coreshare.security.SecurityUtils;
import cl.siga.msestudiantes.client.ApoderadoClient;
import cl.siga.msestudiantes.model.entity.Estudiante;
import cl.siga.coreshare.dto.estudiante.enums.State;
import cl.siga.msestudiantes.model.mapper.EstudianteMapper;
import cl.siga.msestudiantes.model.specifications.EstudianteSpecifications;
import cl.siga.msestudiantes.repository.EstudianteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated 
@RequiredArgsConstructor 
public class EstudianteService {
    private final EstudianteRepository repository;

    private final EstudianteMapper mapper;

    private final ApoderadoClient apoderadoClient;

    @Transactional (readOnly = true)
    public EstudianteResponseDTO getEstudianteById(Long id) {
        return mapper.toResponseDto(repository.findByIdAndStateNot(id, State.INACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante con ID " + id + " no encontrado.")));
    }

    @Transactional (readOnly = true)
    public EstudianteResponseDTO getEstudianteByIdUsuario(String idUsuario) {
        return mapper.toResponseDto(repository.findByIdUsuarioAndStateNot(idUsuario, State.INACTIVO)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante con ID de usuario " + idUsuario + " no encontrado.")));
    }

    @Transactional (readOnly = true)
    public Page<EstudianteResponseDTO> searchEstudiantes(String rut, String firstName, String middleName, String firstSurname, String secondSurname, LocalDate from, LocalDate to, State state, Long idClase, Pageable pageable) {
        // Si se pide un estado explicito se respeta (incluye INACTIVO); si no,
        // se excluyen los inactivos por defecto.
        Specification<Estudiante> spec = state != null
                ? EstudianteSpecifications.hasState(state)
                : EstudianteSpecifications.isActive();
        spec = spec
                .and(EstudianteSpecifications.hasRut(rut))
                .and(EstudianteSpecifications.hasFirstName(firstName))
                .and(EstudianteSpecifications.hasMiddleName(middleName))
                .and(EstudianteSpecifications.hasFirstSurname(firstSurname))
                .and(EstudianteSpecifications.hasSecondSurname(secondSurname))
                .and(EstudianteSpecifications.hasBirthDateGreaterThanOrEqual(from))
                .and(EstudianteSpecifications.hasBirthDateLessThanOrEqual(to))
                .and(EstudianteSpecifications.hasIdClase(idClase));
        return repository.findAll(spec, pageable).map(mapper::toResponseDto);
    }

    @Transactional 
    public EstudianteResponseDTO saveEstudiante(@Valid RegistrarEstudianteRequestDTO request) {
        if(repository.existsByIdUsuario(request.idUsuario())) {
            throw new BusinessException("El ID de usuario ya está registrado: " + request.idUsuario());
        }

        // Verificar si el RUT ya existe en la base de datos
        String normalizedRut = request.rut() == null ? null : request.rut().trim().toUpperCase();
        if (repository.existsByRut(normalizedRut)) {
            throw new BusinessException("El RUT ya está registrado: " + request.rut());
        }

        Estudiante estudiante = mapper.toEntity(request);
        estudiante.setState(State.REGISTRADO);

        return mapper.toResponseDto(repository.save(estudiante));
    }

    @Transactional 
    public EstudianteResponseDTO updateEstudiante(Long id, @Valid ActualizarEstudianteRequestDTO request) {
        // 1. Buscas la entidad actual en la BD
        Estudiante estudianteExistente = repository.findByIdAndStateNot(id, State.INACTIVO)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante con ID " + id + " no encontrado."));

        // 2. ADMIN, el propio estudiante o un apoderado vinculado
        validarPermisoActualizacion(estudianteExistente);

        // 3. MapStruct sobreescribe firstName, firstSurname, etc., pero el RUT queda INTACTO
        mapper.updateEntityFromDto(request, estudianteExistente);

        // 4. Guardas los cambios
        return mapper.toResponseDto(repository.save(estudianteExistente));
    }

    private void validarPermisoActualizacion(Estudiante estudiante) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        String oid = SecurityUtils.getCurrentUserOid()
                .orElseThrow(() -> new AccessDeniedException("No se pudo identificar al usuario autenticado."));

        if (oid.equals(estudiante.getIdUsuario())) {
            return;
        }

        if (SecurityUtils.hasRole("APODERADO")) {
            ApoderadoResponseDTO apoderado = apoderadoClient.getApoderadoByIdUsuario(oid);
            boolean vinculado = apoderado.estudiantes() != null && apoderado.estudiantes().stream()
                    .anyMatch(vinculo -> estudiante.getId().equals(vinculo.idEstudiante()));
            if (vinculado) {
                return;
            }
        }

        throw new AccessDeniedException("No tienes permiso para modificar este estudiante.");
    }

    @Transactional (readOnly = true)
    public boolean existsEstudianteById(Long id) {
        return repository.existsByIdAndStateNot(id, State.INACTIVO);
    }

    @Transactional
    public void deleteEstudiante(Long id) {
        Estudiante existing = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante con ID " + id + " no encontrado."));
        existing.setState(State.INACTIVO);
        repository.save(existing);
    }
}
