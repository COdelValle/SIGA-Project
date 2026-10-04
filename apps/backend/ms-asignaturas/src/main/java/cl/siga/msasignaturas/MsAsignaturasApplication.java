package cl.siga.msasignaturas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsAsignaturasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAsignaturasApplication.class, args);
    }

}
