package cl.siga.msusuariosauth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroEstudianteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;

class EmailInstitucionalGeneratorTest {

    private static final String DOMINIO = "platformsiga.onmicrosoft.com";

    private final RegistroAsyncProperties properties = new RegistroAsyncProperties();
    private final EmailInstitucionalGenerator generator = new EmailInstitucionalGenerator(properties);

    private DatosRegistroRolDTO estudiante(String nombre, String segundoNombre, String apellido, String segundoApellido) {
        return new DatosRegistroRolDTO(
                new DatosRegistroEstudianteDTO(
                        nombre, segundoNombre, apellido, segundoApellido, "21000001-1",
                        LocalDate.of(2012, 5, 1), null, null),
                null, null);
    }

    private Predicate<String> ocupados(String... locales) {
        Set<String> tomados = Set.of(locales);
        return tomados::contains;
    }

    @Test
    void generaNombrePuntoApellidoConDominio() {
        String email = generator.generarEmail(estudiante("Catalina", null, "Ormeño", "Soto"),
                Rol.ESTUDIANTE, ocupados());

        assertEquals("catalina.ormeno@" + DOMINIO, email);
    }

    @Test
    void nombresCompuestosUsanPrimeraPalabra() {
        String email = generator.generarEmail(estudiante("José Ángel", null, "Muñoz", "Peña"),
                Rol.ESTUDIANTE, ocupados());

        assertEquals("jose.munoz@" + DOMINIO, email);
    }

    @Test
    void quitaTildesEspaciosYGuiones() {
        String email = generator.generarEmail(estudiante("María José", null, "Pérez-Gómez", null),
                Rol.ESTUDIANTE, ocupados());

        assertEquals("maria.perezgomez@" + DOMINIO, email);
    }

    @Test
    void colisionUsaSegundoApellido() {
        String email = generator.generarEmail(estudiante("Catalina", null, "Ormeño", "Soto"),
                Rol.ESTUDIANTE, ocupados("catalina.ormeno@" + DOMINIO));

        assertEquals("catalina.ormenosoto@" + DOMINIO, email);
    }

    @Test
    void colisionSinSegundoApellidoUsaSufijoNumerico() {
        String email = generator.generarEmail(estudiante("Juan", null, "Pérez", null),
                Rol.ESTUDIANTE, ocupados("juan.perez@" + DOMINIO));

        assertEquals("juan.perez2@" + DOMINIO, email);
    }

    @Test
    void dobleColisionUsaSufijoNumerico() {
        String email = generator.generarEmail(estudiante("Catalina", null, "Ormeño", "Soto"),
                Rol.ESTUDIANTE, ocupados(
                        "catalina.ormeno@" + DOMINIO,
                        "catalina.ormenosoto@" + DOMINIO));

        assertEquals("catalina.ormeno2@" + DOMINIO, email);
    }

    @Test
    void truncaElLocalPartA64Caracteres() {
        String nombre = "Maximilianoalejandrofernandezdelacruz";
        String apellido = "Contrerasleteliergonzalezdelsolar";

        String email = generator.generarEmail(estudiante(nombre, null, apellido, null),
                Rol.ESTUDIANTE, ocupados());

        String local = email.substring(0, email.indexOf('@'));
        assertEquals(64, local.length());
        assertTrue(email.endsWith("@" + DOMINIO));
    }

    @Test
    void fallaCuandoElNombreNoTieneCaracteresUtilizables() {
        assertThrows(BusinessException.class, () -> generator.generarEmail(
                estudiante("Ω≈ç", null, "***", null), Rol.ESTUDIANTE, ocupados()));
    }

    @Test
    void nombreCompletoUneLosCamposPresentes() {
        String completo = generator.nombreCompleto(
                estudiante("Catalina", "Antonia", "Ormeño", "Soto"), Rol.ESTUDIANTE);

        assertEquals("Catalina Antonia Ormeño Soto", completo);
    }

    @Test
    void nombreCompletoNormalizaMayusculasYParticulas() {
        assertEquals("Catalina Antonia Ormeño Soto", generator.nombreCompleto(
                estudiante("cATALINA", "ANTONIA", "ORMEÑO", "SOTO"), Rol.ESTUDIANTE));
        assertEquals("María del Carmen Ormeño de la Cruz", generator.nombreCompleto(
                estudiante("María", "del Carmen", "Ormeño", "de la Cruz"), Rol.ESTUDIANTE));
    }
}
