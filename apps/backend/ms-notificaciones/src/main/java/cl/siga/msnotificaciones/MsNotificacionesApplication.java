package cl.siga.msnotificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Consume eventos académicos y mantiene la bandeja in-app por usuario OID.
 */
@SpringBootApplication
@EnableFeignClients
@EnableScheduling
public class MsNotificacionesApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsNotificacionesApplication.class, args);
  }

}
