package cl.siga.ms_kafka.config;

import java.util.Properties;

import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Adminclient {

  @Bean
  public AdminClient adminClient() {

    Properties props = new Properties();

    props.put(
      AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
      "kafka:29092"
    );

    return AdminClient.create(props);
  }
}
