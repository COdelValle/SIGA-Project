package cl.siga.ms_kafka.config;

import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Adminclient {

  @Value("${spring.kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public AdminClient adminClient() {
    Properties properties = new Properties();

    properties.put(
      AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
      bootstrapServers
    );

    return AdminClient.create(properties);
  }
}
