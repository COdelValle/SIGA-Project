package cl.siga.msnotas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class MsNotasApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsNotasApplication.class, args);
    }

}
