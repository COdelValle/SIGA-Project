package cl.siga.msapoderados;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsApoderadosApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsApoderadosApplication.class, args);
  }

}
