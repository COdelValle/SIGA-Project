package cl.siga.msasignaturas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.asignatura.CursoAsignatura;
import cl.siga.msasignaturas.model.mapper.HorarioMapper;
import cl.siga.msasignaturas.repository.HorarioRepository;
import cl.siga.msasignaturas.repository.asignatura.CursoAsignaturaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final CursoAsignaturaRepository cursoAsignaturaRepository;
    private final HorarioMapper horarioMapper;

    @Transactional
    public HorarioResponseDTO crearHorario(Long cursoAsignaturaId, HorarioRequestDTO request) {
        validarRangoHorario(request);

        CursoAsignatura curso = cursoAsignaturaRepository.findByIdAndActiveTrue(cursoAsignaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Dictación activa no encontrada con ID: " + cursoAsignaturaId));

        validarSolapamiento(cursoAsignaturaId, request, null);

        Horario horario = horarioMapper.toEntity(request);
        horario.setCursoAsignatura(curso);
        horario.setActive(true);

        return horarioMapper.toDto(horarioRepository.save(horario));
    }

    @Transactional
    public HorarioResponseDTO actualizarHorario(Long id, HorarioRequestDTO request) {
        validarRangoHorario(request);

        Horario horario = horarioRepository.findById(id)
                .filter(Horario::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado con ID: " + id));

        Long cursoId = horario.getCursoAsignatura().getId();
        CursoAsignatura curso = cursoAsignaturaRepository.findByIdAndActiveTrue(cursoId)
                .orElseThrow(() -> new ResourceNotFoundException("Dictación activa no encontrada con ID: " + cursoId));

        validarSolapamiento(curso.getId(), request, id);

        horarioMapper.updateEntityFromDto(request, horario);
        horario.setCursoAsignatura(curso);

        return horarioMapper.toDto(horarioRepository.save(horario));
    }

    @Transactional
    public void eliminarHorario(Long id) {
        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado con ID: " + id));

        if (!horario.isActive()) {
            return;
        }

        if (horarioRepository.countByCursoAsignaturaIdAndActiveTrue(horario.getCursoAsignatura().getId()) <= 1) {
            throw new BusinessException("La dictación debe mantener al menos un horario.");
        }

        horario.setActive(false);
        horarioRepository.save(horario);
    }

    private void validarRangoHorario(HorarioRequestDTO request) {
        if (!request.horarioEntrada().isBefore(request.horarioSalida())) {
            throw new BusinessException("La hora de entrada debe ser anterior a la hora de salida");
        }
    }

    private void validarSolapamiento(Long cursoAsignaturaId, HorarioRequestDTO request, Long idExcluir) {
        boolean solapaCurso = horarioRepository.findByCursoAsignaturaIdAndActiveTrue(cursoAsignaturaId).stream()
                .filter(h -> idExcluir == null || !h.getId().equals(idExcluir))
                .anyMatch(h -> h.getDia() == request.dia() && solapan(h, request));
        if (solapaCurso) {
            throw new BusinessException("La dictación ya tiene un horario que se solapa con el indicado.");
        }

        boolean solapaUbicacion = horarioRepository
                .findByUbicacionIgnoreCaseAndDiaAndActiveTrue(request.ubicacion(), request.dia()).stream()
                .filter(h -> idExcluir == null || !h.getId().equals(idExcluir))
                .anyMatch(h -> solapan(h, request));
        if (solapaUbicacion) {
            throw new BusinessException("La ubicación ya está ocupada en ese horario.");
        }
    }

    private boolean solapan(Horario existente, HorarioRequestDTO request) {
        return request.horarioEntrada().isBefore(existente.getHorarioSalida())
                && existente.getHorarioEntrada().isBefore(request.horarioSalida());
    }
}
