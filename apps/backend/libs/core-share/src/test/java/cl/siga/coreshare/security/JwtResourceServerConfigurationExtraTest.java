package cl.siga.coreshare.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class JwtResourceServerConfigurationExtraTest {

    private final JwtResourceServerConfiguration configuration = new JwtResourceServerConfiguration();

    @Test
    void construyeElDecoderConPropiedadesObligatoriasYValoresPorDefecto() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("AZURE_TENANT_ID", "tenant-1")
                .withProperty("AZURE_APP_ID_URI", "api://siga")
                .withProperty("AZURE_CLIENT_ID", "client-1");

        var decoder = configuration.sigaJwtDecoder(environment);

        assertThat(decoder).isNotNull();
    }

    @Test
    void usaIssuerJwksYAudiencesExplicitosCuandoVienenConfigurados() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("AZURE_TENANT_ID", "tenant-1")
                .withProperty("AZURE_APP_ID_URI", "api://siga")
                .withProperty("AZURE_CLIENT_ID", "client-1")
                .withProperty("AZURE_JWT_ISSUER", " https://issuer.test/ ")
                .withProperty("AZURE_JWKS_URI", "https://issuer.test/keys")
                .withProperty("AZURE_JWT_AUDIENCES", " api://siga , client-1 ,, api://otra ");

        assertThat(configuration.sigaJwtDecoder(environment)).isNotNull();
    }

    @Test
    void exigeLasPropiedadesObligatorias() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("AZURE_APP_ID_URI", "api://siga");

        assertThatThrownBy(() -> configuration.sigaJwtDecoder(environment))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("AZURE_TENANT_ID");
    }
}
