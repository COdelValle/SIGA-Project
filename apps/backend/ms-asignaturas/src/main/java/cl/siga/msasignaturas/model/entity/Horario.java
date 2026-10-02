package cl.siga.msasignaturas.model.entity;

import java.time.LocalTime;

import cl.siga.coreshare.dto.asignatura.horario.enums.DiaSemana;
import cl.siga.msasignaturas.model.entity.asignatura.Asignatura;
import jakarta.persistence.*;
import lombok.*;

@Entity 
@Table (name = "horarios")
@Getter 
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Horario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DiaSemana dia;

    @Column(name = "horario_entrada", nullable = false)
    private LocalTime horarioEntrada;

    @Column(name = "horario_salida", nullable = false)
    private LocalTime horarioSalida;

    @Column(length = 50, nullable = false)
    private String ubicacion;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;
}