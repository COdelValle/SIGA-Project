package cl.siga.msevaluaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsEvaluacionesApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsEvaluacionesApplication.class, args);
  }

}
