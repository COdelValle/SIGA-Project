package cl.siga.coreshare.dto.asignatura.enums;

/**
 * Plan en el que se imparte una asignatura dentro de la malla de un nivel.
 * La formación diferenciada de 3° y 4° Medio queda modelada para una fase
 * futura; en esta iteración solo se usa COMUN.
 */
public enum PlanFormacion {
    COMUN,
    DIFERENCIADA_HC,
    DIFERENCIADA_TP
}
