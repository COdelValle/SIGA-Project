package cl.siga.coreshare.dto.asignatura.enums;

/**
 * Carácter de una asignatura dentro de la malla de un nivel. Es sensible al
 * nivel: una misma asignatura puede ser obligatoria en un ciclo y optativa en
 * otro (p. ej. Inglés en 1°-4° Básico vs. 5° Básico en adelante).
 */
public enum CaracterAsignatura {
    OBLIGATORIA,
    OPTATIVA,
    ELECTIVA
}
