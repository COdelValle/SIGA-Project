package cl.siga.coreshare.mensajeria;

import java.time.LocalDateTime;

/**
 * Contrato común para eventos que deben poder rastrearse e ignorarse cuando
 * vuelven a entregarse.
 */
public interface EventoTrazable {

    String idEvento();

    LocalDateTime timestamp();

    String correlationId();
}
