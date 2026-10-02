package cl.siga.msasignaturas.model.entity.asignatura;

import java.util.List;

import cl.siga.msasignaturas.model.entity.Inscripcion;

import java.util.ArrayList;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity 
@DiscriminatorValue("ELECTIVA")
@Getter 
@Setter
@SuperBuilder 
@AllArgsConstructor
@NoArgsConstructor
public class AsignaturaElectiva extends Asignatura {

    @Column(name = "cupo_maximo")
    private Integer cupoMaximo;

    // Relación con la tabla intermedia
    @Builder.Default
    @OneToMany(mappedBy = "asignatura", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Inscripcion> inscripciones = new ArrayList<>();
}
