package cl.siga.msestudiantes.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cl.siga.coreshare.dto.estudiante.ActualizarEstudianteRequestDTO;
import cl.siga.coreshare.dto.estudiante.EstudianteResponseDTO;
import cl.siga.coreshare.dto.estudiante.RegistrarEstudianteRequestDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msestudiantes.model.entity.Estudiante;
import cl.siga.msestudiantes.model.entity.State;
import cl.siga.msestudiantes.model.mapper.EstudianteMapper;
import cl.siga.msestudiantes.repository.EstudianteRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service
@Validated 
@RequiredArgsConstructor 
public class EstudianteService {
    private final EstudianteRepository repository;

    private final EstudianteMapper mapper;

    @Transactional (readOnly = true)
    public EstudianteResponseDTO getEstudianteById(Long id) {
        return mapper.toResponseDto(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante con ID " + id + " no encontrado.")));
    }

    @Transactional (readOnly = true)
    public EstudianteResponseDTO getEstudianteByIdUsuario(String idUsuario) {
        return mapper.toResponseDto(repository.findByIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Estudiante con ID de usuario " + idUsuario + " no encontrado.")));
    }

    @Transactional (readOnly = true)
    public List<EstudianteResponseDTO> searchEstudiantes(String rut, String firstName, String middleName, String lastName, LocalDate from, LocalDate to, State state) {
        Specification<Estudiante> spec = (root, query, cb) -> cb.conjunction();
        if (rut != null && !rut.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("rut"), rut));
        }
        if (firstName != null && !firstName.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase() + "%"));
        }
        if (middleName != null && !middleName.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("middleName")), "%" + middleName.toLowerCase() + "%"));
        }
        if (lastName != null && !lastName.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase() + "%"));
        }
        if (from != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("birthDate"), from));
        }
        if (to != null) {
            spec = spec.and((root, query, cb) -> cb.lessThanOrEqualTo(root.get("birthDate"), to));
        }
        if (state != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("state"), state));
        }
        return mapper.toResponseDtoList(repository.findAll(spec));
    }

    @Transactional 
    public EstudianteResponseDTO saveEstudiante(@Valid RegistrarEstudianteRequestDTO request) {
        if(repository.existsByIdUsuario(request.idUsuario())) {
            throw new BusinessException("El ID de usuario ya está registrado: " + request.idUsuario());
        }

        // Verificar si el RUT ya existe en la base de datos
        if (repository.existsByRut(request.rut())) {
            throw new BusinessException("El RUT ya está registrado: " + request.rut());
        }

        Estudiante estudiante = mapper.toEntity(request);
        estudiante.setState(State.REGISTRADO);

        return mapper.toResponseDto(repository.save(estudiante));
    }

    public EstudianteResponseDTO updateEstudiante(Long id, @Valid ActualizarEstudianteRequestDTO request) {
        // 1. Buscas la entidad actual en la BD
        Estudiante estudianteExistente = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Estudiante con ID " + id + " no encontrado."));

        // 2. MapStruct sobreescribe firstName, lastName, etc., pero el RUT queda INTACTO
        mapper.updateEntityFromDto(request, estudianteExistente);

        // 3. Guardas los cambios
        return mapper.toResponseDto(repository.save(estudianteExistente));
    }
}
