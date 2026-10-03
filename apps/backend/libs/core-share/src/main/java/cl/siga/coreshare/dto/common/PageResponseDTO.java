package cl.siga.coreshare.dto.common;

import java.util.List;

/**
 * Contrato minimo de una pagina de Spring Data para clientes Feign.
 * Evita deserializar la interfaz Page<T> (no construible por Jackson).
 */
public record PageResponseDTO<T>(
    List<T> content,
    long totalElements,
    int totalPages,
    int size,
    int number,
    boolean first,
    boolean last,
    boolean empty
) {
}
