package cl.siga.msusuariosauth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.docente.certificado.CertificadoRequestDTO;
import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroApoderadoDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroDocenteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroEstudianteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import cl.siga.coreshare.enums.AreaAcademica;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;

class EmailInstitucionalGeneratorRolesTest {

    private static final String DOMINIO = "platformsiga.onmicrosoft.com";

    private final RegistroAsyncProperties properties = new RegistroAsyncProperties();
    private final EmailInstitucionalGenerator generator = new EmailInstitucionalGenerator(properties);

    private DatosRegistroRolDTO docente(String nombre, String apellido, String segundoApellido) {
        return new DatosRegistroRolDTO(
                null,
                new DatosRegistroDocenteDTO(
                        nombre, null, apellido, segundoApellido, "11111111-1",
                        LocalDate.of(2020, 3, 1), AreaAcademica.MATEMATICAS,
                        List.of(new CertificadoRequestDTO("Docencia", "Universidad", LocalDate.of(2019, 1, 1)))),
                null);
    }

    private DatosRegistroRolDTO apoderado(String nombre, String apellido, String segundoApellido) {
        return new DatosRegistroRolDTO(
                null,
                null,
                new DatosRegistroApoderadoDTO(
                        nombre, null, apellido, segundoApellido, "22222222-2",
                        List.of("+56912345678"), List.of()));
    }

    @Test
    void generaCorreoDeDocenteYApoderado() {
        assertThat(generator.generarEmail(docente("Pedro", "Soto", "Rojas"),
                Rol.DOCENTE, correo -> false))
                .isEqualTo("pedro.soto@" + DOMINIO);
        assertThat(generator.generarEmail(apoderado("Lucía", "Muñoz", "Díaz"),
                Rol.APODERADO, correo -> false))
                .isEqualTo("lucia.munoz@" + DOMINIO);
    }

    @Test
    void nombreCompletoDeDocenteYApoderado() {
        assertThat(generator.nombreCompleto(docente("Pedro", "Soto", "Rojas"), Rol.DOCENTE))
                .isEqualTo("Pedro Soto Rojas");
        assertThat(generator.nombreCompleto(apoderado("Lucía", "Muñoz", null), Rol.APODERADO))
                .isEqualTo("Lucía Muñoz");
    }

    @Test
    void elRolAdminNoUsaElRegistroAsincrono() {
        assertThatThrownBy(() -> generator.nombreCompleto(docente("Pedro", "Soto", null), Rol.ADMIN))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ADMIN");
        assertThatThrownBy(() -> generator.generarEmail(docente("Pedro", "Soto", null),
                Rol.ADMIN, correo -> false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ADMIN");
    }

    @Test
    void nombreCompletoVacioEsRechazado() {
        DatosRegistroRolDTO datos = new DatosRegistroRolDTO(
                new DatosRegistroEstudianteDTO(null, " ", null, "", null, null, null, null),
                null, null);

        assertThatThrownBy(() -> generator.nombreCompleto(datos, Rol.ESTUDIANTE))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("nombre completo");
    }

    @Test
    void fallaCuandoSeAgotanTodosLosCandidatosDeCorreo() {
        Predicate<String> siempreOcupado = correo -> true;

        assertThatThrownBy(() -> generator.generarEmail(docente("Pedro", "Soto", "Rojas"),
                Rol.DOCENTE, siempreOcupado))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("agotaron los candidatos");
    }

    @Test
    void usaElSegundoApellidoCuandoElPrimeroEstaOcupado() {
        String base = "pedro.soto@" + DOMINIO;
        String conSegundo = "pedro.sotorojas@" + DOMINIO;

        assertThat(generator.generarEmail(docente("Pedro", "Soto", "Rojas"),
                Rol.DOCENTE, correo -> correo.equals(base) || correo.equals(conSegundo)))
                .isEqualTo("pedro.soto2@" + DOMINIO);
    }
}
