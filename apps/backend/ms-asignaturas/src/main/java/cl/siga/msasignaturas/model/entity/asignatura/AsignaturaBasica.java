package cl.siga.msasignaturas.model.entity.asignatura;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity 
@DiscriminatorValue("BASICA")
@Getter 
@Setter
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
public class AsignaturaBasica extends Asignatura {

    @Column(name = "id_clase")
    private Long idClase;

    @Override
    @PrePersist
    @PreUpdate
    protected void formatFieldsAndValidate() {
        super.formatFieldsAndValidate();
        if (idClase == null) {
            throw new IllegalStateException("Una asignatura básica requiere obligatoriamente un ID de Clase.");
        }
    }
}