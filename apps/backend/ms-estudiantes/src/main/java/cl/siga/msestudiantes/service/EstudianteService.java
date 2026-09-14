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
    public List<EstudianteResponseDTO> searchEstudiantes(String rut, String firstName, String middleName, String firstSurname, String secondSurname, LocalDate from, LocalDate to, State state) {
        Specification<Estudiante> spec = EstudianteSpecifications.isActive()
                .and(EstudianteSpecifications.hasRut(rut))
                .and(EstudianteSpecifications.hasFirstName(firstName))
                .and(EstudianteSpecifications.hasMiddleName(middleName))
                .and(EstudianteSpecifications.hasFirstSurname(firstSurname))
                .and(EstudianteSpecifications.hasSecondSurname(secondSurname))
                .and(EstudianteSpecifications.hasBirthDateGreaterThanOrEqual(from))
                .and(EstudianteSpecifications.hasBirthDateLessThanOrEqual(to))
                .and(EstudianteSpecifications.hasState(state));
        return mapper.toResponseDtoList(repository.findAll(spec));
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

        // 2. MapStruct sobreescribe firstName, firstSurname, etc., pero el RUT queda INTACTO
        mapper.updateEntityFromDto(request, estudianteExistente);

        // 3. Guardas los cambios
        return mapper.toResponseDto(repository.save(estudianteExistente));
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
