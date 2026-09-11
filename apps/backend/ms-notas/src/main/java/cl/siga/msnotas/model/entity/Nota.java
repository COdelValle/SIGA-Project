package cl.siga.msnotas.model.entity;

import cl.siga.coreshare.validation.ChileanGrade;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Entity 
@Table (name = "notas")
@Getter 
@Setter 
@Builder 
@AllArgsConstructor 
@NoArgsConstructor 
public class Nota {
    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long  id;

    @NotNull (message = "El ID del estudiante es obligatorio")
    @Column (name = "id_estudiante", nullable = false)
    private Long idEstudiante;
    
    @NotNull (message = "El ID de la asignatura es obligatorio")
    @Column (name = "id_asignatura", nullable = false)
    private Long idAsignatura;
    
    @NotNull (message = "La nota es obligatoria")
    @ChileanGrade (message = "La nota debe estar entre 1.0 y 7.0")
    @Column (name = "score", nullable = false)
    private Double score;
}
