package cl.siga.msnotas.service;

import java.util.List;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import cl.siga.coreshare.dto.notas.ActualizarNotaRequestDTO;
import cl.siga.coreshare.dto.notas.NotaResponseDTO;
import cl.siga.coreshare.dto.notas.RegistrarNotaRequestDTO;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msnotas.model.entity.Nota;
import cl.siga.msnotas.model.mapper.NotaMapper;
import cl.siga.msnotas.repository.NotaRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Service 
@Validated 
@RequiredArgsConstructor 
public class NotaService {
    private final NotaRepository repository;

    private final NotaMapper mapper;

    @Transactional (readOnly = true)
    public NotaResponseDTO getNotaById(Long id) {
        return mapper.toResponseDto(repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota con ID " + id + " no encontrada.")));
    }

    @Transactional (readOnly = true)
    public List<NotaResponseDTO> searchNotas(Long idEstudiante, Long idAsignatura, Double lessThatScore, Double greaterThanScore) {
        Specification<Nota> spec = (root, query, cb) -> cb.conjunction();
        if (idEstudiante != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("idEstudiante"), idEstudiante));
        }
        if (idAsignatura != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("idAsignatura"), idAsignatura));
        }
        if (lessThatScore != null) {
            spec = spec.and((root, query, cb) -> cb.lessThan(root.get("score"), lessThatScore));
        }
        if (greaterThanScore != null) {
            spec = spec.and((root, query, cb) -> cb.greaterThan(root.get("score"), greaterThanScore));
        }
        return mapper.toResponseDtoList(repository.findAll(spec));
    }

    @Transactional 
    public NotaResponseDTO saveNota(@Valid RegistrarNotaRequestDTO request) {
        Nota nota = mapper.toEntity(request);
        return mapper.toResponseDto(repository.save(nota));
    }

    @Transactional
    public NotaResponseDTO updateNota(Long id, @Valid ActualizarNotaRequestDTO request) {
        Nota existingNota = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota con ID " + id + " no encontrada."));

        mapper.updateEntityFromDto(request, existingNota);

        return mapper.toResponseDto(repository.save(existingNota));
    }

    @Transactional
    public void deleteNota(Long id) {
        Nota existingNota = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Nota con ID " + id + " no encontrada."));
        repository.delete(existingNota);
    }
}
