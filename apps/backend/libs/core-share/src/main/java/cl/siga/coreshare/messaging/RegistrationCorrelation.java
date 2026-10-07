package cl.siga.coreshare.messaging;

import org.slf4j.MDC;

import cl.siga.coreshare.config.CorrelationIdConfig;

/**
 * Ejecuta un consumidor AMQP publicando el correlation id en el MDC para que
 * los logs del listener sean trazables extremo a extremo.
 */
public final class RegistrationCorrelation {

    private RegistrationCorrelation() {
    }

    public static void ejecutar(String correlationId, Runnable action) {
        boolean enMdc = correlationId != null && !correlationId.isBlank();
        if (enMdc) {
            MDC.put(CorrelationIdConfig.MDC_KEY, correlationId);
        }
        try {
            action.run();
        } finally {
            MDC.remove(CorrelationIdConfig.MDC_KEY);
        }
    }
}
