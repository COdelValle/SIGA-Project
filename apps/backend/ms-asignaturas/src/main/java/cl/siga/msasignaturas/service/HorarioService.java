package cl.siga.msasignaturas.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import cl.siga.coreshare.dto.asignatura.horario.HorarioRequestDTO;
import cl.siga.coreshare.dto.asignatura.horario.HorarioResponseDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.exception.ResourceNotFoundException;
import cl.siga.msasignaturas.model.entity.Horario;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import cl.siga.msasignaturas.model.mapper.HorarioMapper;
import cl.siga.msasignaturas.repository.HorarioRepository;
import cl.siga.msasignaturas.repository.asignatura.AsignaturaRepository;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HorarioService {

    private final HorarioRepository horarioRepository;
    private final AsignaturaRepository asignaturaRepository;
    private final HorarioMapper horarioMapper;

    @Transactional
    public HorarioResponseDTO crearHorario(Long asignaturaId, HorarioRequestDTO request) {
        validarRangoHorario(request);

        Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrue(asignaturaId)
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura activa no encontrada con ID: " + asignaturaId));

        Horario horario = horarioMapper.toEntity(request);
        horario.setAsignatura(asignatura);

        return horarioMapper.toDto(horarioRepository.save(horario));
    }

    @Transactional
    public HorarioResponseDTO actualizarHorario(Long id, HorarioRequestDTO request) {
        validarRangoHorario(request);

        Horario horario = horarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Horario no encontrado con ID: " + id));

        Asignatura asignatura = asignaturaRepository.findByIdAndActiveTrue(horario.getAsignatura().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Asignatura activa no encontrada con ID: " + horario.getAsignatura().getId()));

        horarioMapper.updateEntityFromDto(request, horario);
        horario.setAsignatura(asignatura);

        return horarioMapper.toDto(horarioRepository.save(horario));
    }

    @Transactional
    public void eliminarHorario(Long id) {
        if (!horarioRepository.existsById(id)) {
            throw new ResourceNotFoundException("Horario no encontrado con ID: " + id);
        }
        horarioRepository.deleteById(id);
    }

    private void validarRangoHorario(HorarioRequestDTO request) {
        if (!request.horarioEntrada().isBefore(request.horarioSalida())) {
            throw new BusinessException("La hora de entrada debe ser anterior a la hora de salida");
        }
    }
}