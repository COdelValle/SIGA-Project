package cl.siga.coreshare.dto.asignatura.horario;

import java.time.LocalTime;

import com.fasterxml.jackson.annotation.JsonFormat;

import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;

public record HorarioResponseDTO(
    Long id,
    DiaSemana dia,
    
    @JsonFormat (pattern = "HH:mm")
    LocalTime horarioEntrada,
    
    @JsonFormat (pattern = "HH:mm")
    LocalTime horarioSalida,
    
    String ubicacion
) {}