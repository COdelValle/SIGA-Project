package cl.siga.coreshare.dto.asignatura.horario;

import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import jakarta.validation.constraints.*;

public record HorarioRequestDTO(
    @NotNull (message = "El día es obligatorio")
    DiaSemana dia,

    @NotNull(message = "La hora de entrada es obligatoria")
    @JsonFormat (pattern = "HH:mm")
    LocalTime horarioEntrada,

    @NotNull(message = "La hora de salida es obligatoria")
    @JsonFormat(pattern = "HH:mm")
    LocalTime horarioSalida,

    @NotBlank(message = "La ubicación es obligatoria")
    @Size(max = 50, message = "La ubicación no puede superar los 50 caracteres")
    String ubicacion
) {}
