package cl.siga.msusuariosauth.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;

import org.springframework.stereotype.Component;

import cl.siga.coreshare.dto.usuario.enums.Rol;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroApoderadoDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroDocenteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroEstudianteDTO;
import cl.siga.coreshare.dto.usuario.payload.DatosRegistroRolDTO;
import cl.siga.coreshare.exception.BusinessException;
import cl.siga.coreshare.format.NombrePropio;
import cl.siga.msusuariosauth.config.RegistroAsyncProperties;
import lombok.RequiredArgsConstructor;

/**
 * Genera el correo institucional ({@code nombre.apellido@dominio}) y el nombre
 * completo a partir de los datos del rol.
 *
 * <p>Reglas del correo: se usa la primera palabra del primer nombre y del primer
 * apellido, sin tildes/ñ ni caracteres inválidos para un UPN de Entra ID. Ante
 * colisiones se intenta el segundo apellido ({@code nombre.apellido1apellido2})
 * y luego un sufijo numérico ({@code nombre.apellido2..99}). El local part
 * nunca supera los 64 caracteres.</p>
 */
@Component
@RequiredArgsConstructor
public class EmailInstitucionalGenerator {

    private static final int MAX_LOCAL_PART = 64;
    private static final int MAX_SUFIJO_NUMERICO = 99;

    private final RegistroAsyncProperties properties;

    public String nombreCompleto(DatosRegistroRolDTO roleData, Rol rol) {
        String[] campos = switch (rol) {
            case ESTUDIANTE -> {
                DatosRegistroEstudianteDTO datos = roleData.estudiante();
                yield new String[] {
                        datos.firstName(), datos.middleName(), datos.firstSurname(), datos.secondSurname() };
            }
            case DOCENTE -> {
                DatosRegistroDocenteDTO datos = roleData.docente();
                yield new String[] {
                        datos.firstName(), datos.middleName(), datos.firstSurname(), datos.secondSurname() };
            }
            case APODERADO -> {
                DatosRegistroApoderadoDTO datos = roleData.apoderado();
                yield new String[] {
                        datos.firstName(), datos.middleName(), datos.firstSurname(), datos.secondSurname() };
            }
            case ADMIN -> throw new BusinessException("El rol ADMIN no usa el registro asíncrono.");
        };
        // Se normaliza la cadena unida para que las particulas internas queden en
        // minuscula (p. ej. "Maria del Carmen Ormeno de la Cruz").
        String completo = NombrePropio.normalizar(unir(campos));
        if (completo == null || completo.isBlank()) {
            throw new BusinessException("El nombre completo no puede estar vacío.");
        }
        return completo;
    }

    /**
     * Genera un correo único usando {@code ocupado} para detectar candidatos
     * tomados por una cuenta activa (las deshabilitadas se reutilizan para
     * reactivación).
     */
    public String generarEmail(DatosRegistroRolDTO roleData, Rol rol, Predicate<String> ocupado) {
        String[] nombres = switch (rol) {
            case ESTUDIANTE -> new String[] {
                    roleData.estudiante().firstName(), roleData.estudiante().firstSurname(),
                    roleData.estudiante().secondSurname() };
            case DOCENTE -> new String[] {
                    roleData.docente().firstName(), roleData.docente().firstSurname(),
                    roleData.docente().secondSurname() };
            case APODERADO -> new String[] {
                    roleData.apoderado().firstName(), roleData.apoderado().firstSurname(),
                    roleData.apoderado().secondSurname() };
            case ADMIN -> throw new BusinessException("El rol ADMIN no usa el registro asíncrono.");
        };

        String nombre = primeraPalabra(nombres[0]);
        String apellido = primeraPalabra(nombres[1]);
        if (nombre.isEmpty() || apellido.isEmpty()) {
            throw new BusinessException(
                    "No se pudo generar el correo: el nombre y el primer apellido deben contener letras.");
        }

        String base = truncar(nombre + "." + apellido, MAX_LOCAL_PART);
        if (!ocupado.test(conDominio(base))) {
            return conDominio(base);
        }

        String segundoApellido = primeraPalabra(nombres[2]);
        if (!segundoApellido.isEmpty()) {
            String conSegundo = truncar(nombre + "." + apellido + segundoApellido, MAX_LOCAL_PART);
            if (!ocupado.test(conDominio(conSegundo))) {
                return conDominio(conSegundo);
            }
        }

        for (int numero = 2; numero <= MAX_SUFIJO_NUMERICO; numero++) {
            String sufijo = String.valueOf(numero);
            String candidato = truncar(base, MAX_LOCAL_PART - sufijo.length()) + sufijo;
            if (!ocupado.test(conDominio(candidato))) {
                return conDominio(candidato);
            }
        }
        throw new BusinessException(
                "No se pudo generar un correo único para el nombre indicado (se agotaron los candidatos).");
    }

    private String conDominio(String localPart) {
        return localPart + "@" + properties.getEmailDomain();
    }

    private static String unir(String[] partes) {
        StringBuilder resultado = new StringBuilder();
        for (String parte : partes) {
            if (parte == null || parte.isBlank()) {
                continue;
            }
            if (!resultado.isEmpty()) {
                resultado.append(' ');
            }
            resultado.append(parte.trim());
        }
        return resultado.toString();
    }

    private static String primeraPalabra(String texto) {
        if (texto == null || texto.isBlank()) {
            return "";
        }
        return normalizar(texto.trim().split("\\s+")[0]);
    }

    private static String normalizar(String texto) {
        String sinDiacriticos = Normalizer.normalize(texto, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return sinDiacriticos.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String truncar(String valor, int maximo) {
        return valor.length() <= maximo ? valor : valor.substring(0, maximo);
    }
}
