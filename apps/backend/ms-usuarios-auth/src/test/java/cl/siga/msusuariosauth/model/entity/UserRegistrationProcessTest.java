package cl.siga.msusuariosauth.model.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.usuario.enums.RegistrationProcessState;
import cl.siga.coreshare.dto.usuario.enums.RegistrationStepState;

class UserRegistrationProcessTest {

    @Test
    void onCreateInicializaFechasYEstadosPorDefecto() {
        UserRegistrationProcess process = UserRegistrationProcess.builder()
                .processId("proc-1")
                .eventId("evt-1")
                .correlationId("corr-1")
                .email("ana@test.com")
                .fullName("Ana")
                .build();

        process.onCreate();

        assertThat(process.getCreatedAt()).isNotNull();
        assertThat(process.getUpdatedAt()).isNotNull();
        assertThat(process.getState()).isEqualTo(RegistrationProcessState.PENDIENTE);
        assertThat(process.getAzureState()).isEqualTo(RegistrationStepState.PENDIENTE);
        assertThat(process.getDomainState()).isEqualTo(RegistrationStepState.PENDIENTE);
    }

    @Test
    void onCreateRespetaValoresExistentes() {
        OffsetDateTime creado = OffsetDateTime.now().minusDays(1);
        UserRegistrationProcess process = UserRegistrationProcess.builder()
                .createdAt(creado)
                .state(RegistrationProcessState.COMPLETADO)
                .azureState(RegistrationStepState.COMPLETADO)
                .domainState(RegistrationStepState.FALLIDO)
                .build();

        process.onCreate();

        assertThat(process.getCreatedAt()).isEqualTo(creado);
        assertThat(process.getState()).isEqualTo(RegistrationProcessState.COMPLETADO);
        assertThat(process.getAzureState()).isEqualTo(RegistrationStepState.COMPLETADO);
        assertThat(process.getDomainState()).isEqualTo(RegistrationStepState.FALLIDO);
    }

    @Test
    void onUpdateRefrescaLaFechaDeActualizacion() {
        UserRegistrationProcess process = UserRegistrationProcess.builder()
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .build();
        OffsetDateTime anterior = process.getUpdatedAt();

        process.onUpdate();

        assertThat(process.getUpdatedAt()).isAfter(anterior);
    }
}
