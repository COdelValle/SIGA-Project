package cl.siga.msusuariosauth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableFeignClients(basePackages = "cl.siga.msusuariosauth.integration")
@EnableScheduling
public class MsUsuariosAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsUsuariosAuthApplication.class, args);
    }

}
