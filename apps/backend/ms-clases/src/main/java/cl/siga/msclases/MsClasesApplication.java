package cl.siga.msclases;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsClasesApplication {

  public static void main(String[] args) {
    SpringApplication.run(MsClasesApplication.class, args);
  }

}
