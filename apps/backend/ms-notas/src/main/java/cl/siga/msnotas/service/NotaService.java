package cl.siga.msnotas.service;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msnotas.client.AsignaturaClient;
import cl.siga.msnotas.client.EstudianteClient;
import cl.siga.msnotas.model.entity.Nota;
import cl.siga.msnotas.model.mapper.NotaMapper;
import cl.siga.msnotas.model.specifications.NotaSpecifications;
import cl.siga.msnotas.repository.NotaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service 
@Validated 
@RequiredArgsConstructor 
public class NotaService {
    private final NotaRepository repository;

    private final NotaMapper mapper;

    private final EstudianteClient estudianteClient;

    private final AsignaturaClient asignaturaClient;

    @Transactional (readOnly = true)
    public NotaResponseDTO getNotaById(Long id) {
        return mapper.toResponseDto(repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota con ID " + id + " no encontrada.")));
    }

    @Transactional (readOnly = true)
    public List<NotaResponseDTO> searchNotas(Long idEstudiante, Long idAsignatura, Double lessThanScore, Double greaterThanScore) {
        Specification<Nota> spec = NotaSpecifications.isActive()
                .and(NotaSpecifications.hasIdEstudiante(idEstudiante))
                .and(NotaSpecifications.hasIdAsignatura(idAsignatura))
                .and(NotaSpecifications.hasScoreGreaterThanOrEqual(greaterThanScore))
                .and(NotaSpecifications.hasScoreLessThanOrEqual(lessThanScore));
        return mapper.toResponseDtoList(repository.findAll(spec));
    }

    @Transactional 
    public NotaResponseDTO saveNota(@Valid RegistrarNotaRequestDTO request) {
        if (!estudianteClient.existsById(request.idEstudiante())) {
            throw new BusinessException("El estudiante con ID " + request.idEstudiante() + " no existe.");
        }
        if (!asignaturaClient.existsById(request.idAsignatura())) {
            throw new BusinessException("La asignatura con ID " + request.idAsignatura() + " no existe.");
        }

        Nota nota = mapper.toEntity(request);
        return mapper.toResponseDto(repository.save(nota));
    }

    @Transactional
    public NotaResponseDTO updateNota(Long id, @Valid ActualizarNotaRequestDTO request) {
        Nota existingNota = repository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota con ID " + id + " no encontrada."));

        mapper.updateEntityFromDto(request, existingNota);

        return mapper.toResponseDto(repository.save(existingNota));
    }

    @Transactional
    public void deleteNota(Long id) {
        Nota existingNota = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota con ID " + id + " no encontrada."));
        existingNota.setActive(false);
        repository.save(existingNota);
    }
}
