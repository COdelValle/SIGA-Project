
package cl.siga.ms_kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicProvisioningConfig {

  private static final int PARTICIONES = 3;
  private static final int FACTOR_REPLICACION = 3;

  // Retención propuesta: 365 días.
  // Pendiente de validación institucional.
  private static final String RETENCION_MS = "31536000000";

  @Bean
  public NewTopic topicRendimiento() {
    return TopicBuilder
      .name("siga.academico.rendimiento")
      .partitions(PARTICIONES)
      .replicas(FACTOR_REPLICACION)
      .config(TopicConfig.RETENTION_MS_CONFIG, RETENCION_MS)
      .config(
        TopicConfig.CLEANUP_POLICY_CONFIG,
        TopicConfig.CLEANUP_POLICY_DELETE
      )
      .build();
  }

  @Bean
  public NewTopic topicAsistencias() {
    return TopicBuilder
      .name("siga.academico.asistencias")
      .partitions(PARTICIONES)
      .replicas(FACTOR_REPLICACION)
      .config(TopicConfig.RETENTION_MS_CONFIG, RETENCION_MS)
      .config(
        TopicConfig.CLEANUP_POLICY_CONFIG,
        TopicConfig.CLEANUP_POLICY_DELETE
      )
      .build();
  }
}
