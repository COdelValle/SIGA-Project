package cl.siga.msestudiantes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsEstudiantesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsEstudiantesApplication.class, args);
    }

}
