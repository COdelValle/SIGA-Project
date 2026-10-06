package cl.siga.msnotificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * Consumidor: escucha la cola-notificaciones-evaluaciones
 * y registra en log a quién le llegaría (estudiantes + apoderados).
 */
@SpringBootApplication
@EnableFeignClients
public class MsNotificacionesApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsNotificacionesApplication.class, args);
  }

}
