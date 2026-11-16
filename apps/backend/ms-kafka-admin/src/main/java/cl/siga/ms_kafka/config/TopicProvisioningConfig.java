
package cl.siga.ms_kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.config.TopicConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class TopicProvisioningConfig {

  private static final int PARTICIONES = 3;
  private static final int FACTOR_REPLICACION = 3;

  // Retención para la demo en AWS: 7 días y 128 MB por partición. Se puede
  // sobrescribir por entorno sin recompilar.
  @Value("${siga.kafka.retention-ms:604800000}")
  private long retencionMs;

  @Value("${siga.kafka.retention-bytes:134217728}")
  private long retencionBytes;

  @Bean
  public NewTopic topicRendimiento() {
    return TopicBuilder
      .name("siga.academico.rendimiento")
      .partitions(PARTICIONES)
      .replicas(FACTOR_REPLICACION)
      .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retencionMs))
      .config(TopicConfig.RETENTION_BYTES_CONFIG, String.valueOf(retencionBytes))
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
      .config(TopicConfig.RETENTION_MS_CONFIG, String.valueOf(retencionMs))
      .config(TopicConfig.RETENTION_BYTES_CONFIG, String.valueOf(retencionBytes))
      .config(
        TopicConfig.CLEANUP_POLICY_CONFIG,
        TopicConfig.CLEANUP_POLICY_DELETE
      )
      .build();
  }
}
