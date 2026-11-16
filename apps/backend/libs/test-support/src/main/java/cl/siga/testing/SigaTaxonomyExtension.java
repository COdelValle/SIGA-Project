package cl.siga.testing;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Label;
import io.qameta.allure.model.TestResult;

/**
 * Completa la taxonomia Allure de los tests SIGA sin obligar a anotar cada clase:
 * layer, epic, feature, story, owner, severity y un nombre legible.
 *
 * Las anotaciones explicitas de Allure (@Epic, @Feature, @Story, @Owner,
 * @Severity, @DisplayName, @Layer) tienen prioridad y no se sobrescriben.
 */
public class SigaTaxonomyExtension implements BeforeTestExecutionCallback {

    private static final Map<String, String> MODULE_FEATURES = new LinkedHashMap<>();
    private static final Map<String, String> CONTRACT_FEATURES = new LinkedHashMap<>();

    static {
        MODULE_FEATURES.put("msnotas", "Notas");
        MODULE_FEATURES.put("msevaluaciones", "Evaluaciones");
        MODULE_FEATURES.put("msasistencias", "Asistencias");
        MODULE_FEATURES.put("msestudiantes", "Estudiantes");
        MODULE_FEATURES.put("msapoderados", "Apoderados");
        MODULE_FEATURES.put("msdocentes", "Docentes");
        MODULE_FEATURES.put("msasignaturas", "Asignaturas");
        MODULE_FEATURES.put("msclases", "Clases");
        MODULE_FEATURES.put("msnotificaciones", "Notificaciones");
        MODULE_FEATURES.put("msrabbitmqadmin", "RabbitMQ Admin");
        MODULE_FEATURES.put("msusuariosauth", "Usuarios y autenticacion");

        CONTRACT_FEATURES.put("NotaClient", "ms-notas");
        CONTRACT_FEATURES.put("EvaluacionClient", "ms-evaluaciones");
        CONTRACT_FEATURES.put("AsignaturaClient", "ms-asignaturas");
        CONTRACT_FEATURES.put("RabbitMqAdminClient", "ms-rabbitmq-admin");
        CONTRACT_FEATURES.put("ClaseClient", "ms-clases");
        CONTRACT_FEATURES.put("UsuarioClient", "ms-usuarios-auth");
        CONTRACT_FEATURES.put("ApoderadoClient", "ms-apoderados");
        CONTRACT_FEATURES.put("DocenteClient", "ms-docentes");
        CONTRACT_FEATURES.put("AsistenciaClient", "ms-asistencias");
        CONTRACT_FEATURES.put("NotificacionClient", "ms-notificaciones");
        CONTRACT_FEATURES.put("EstudianteClient", "ms-estudiantes");
    }

    @Override
    public void beforeTestExecution(ExtensionContext context) {
        Class<?> testClass = context.getRequiredTestClass();
        String method = context.getRequiredTestMethod().getName();
        Taxonomy taxonomy = taxonomyOf(testClass);

        Allure.getLifecycle().updateTest(result -> {
            if (needsReadableName(result, method)) {
                result.setName(humanize(method));
            }
            addLabel(result, "layer", explicitLayer(testClass, method, taxonomy.layer()));
            addLabel(result, "epic", taxonomy.epic());
            addLabel(result, "feature", taxonomy.feature());
            addLabel(result, "story", taxonomy.story());
            addLabel(result, "owner", taxonomy.owner());
            addLabel(result, "severity", "normal");
            addLabel(result, "tag", taxonomy.layer());
        });
    }

    private static boolean needsReadableName(TestResult result, String method) {
        String name = result.getName();
        return name == null
            || name.isBlank()
            || name.equals(method)
            || name.endsWith("()");
    }

    private static void addLabel(TestResult result, String name, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        Label existing = result.getLabels().stream()
            .filter(label -> name.equals(label.getName()))
            .findFirst()
            .orElse(null);
        if (existing == null) {
            result.getLabels().add(new Label().setName(name).setValue(value));
        } else if (existing.getValue() == null || existing.getValue().isBlank()) {
            existing.setValue(value);
        }
    }

    private static String explicitLayer(Class<?> testClass, String method, String fallback) {
        try {
            Layer layer = testClass.getDeclaredMethod(method).getAnnotation(Layer.class);
            if (layer != null) {
                return layer.value();
            }
        } catch (NoSuchMethodException ignored) {
            // El metodo puede estar heredado; se usa la anotacion de clase.
        }
        Layer layer = testClass.getAnnotation(Layer.class);
        return layer != null ? layer.value() : fallback;
    }

    private static Taxonomy taxonomyOf(Class<?> testClass) {
        String packageName = testClass.getPackageName();
        String className = testClass.getSimpleName();

        String layer = inferLayer(className);
        String epic;
        String feature;
        String owner = "backend";

        if (packageName.contains("bffweb.contract")) {
            epic = "Contratos BFF";
            feature = contractFeature(className, packageName);
            owner = "qa";
        } else if (packageName.contains("bffweb")) {
            epic = "BFF";
            feature = bffFeature(packageName, className);
        } else if (packageName.contains("coreshare")) {
            epic = "Plataforma";
            feature = coreFeature(packageName, className);
        } else {
            epic = moduleEpic(packageName);
            feature = moduleFeature(packageName, className);
        }

        if (className.contains("OpenApiSpecificationTest")) {
            feature = packageName.contains("bffweb") ? "OpenAPI BFF" : "OpenAPI " + feature;
            layer = "contract";
        }

        return new Taxonomy(layer, epic, feature, humanize(storyName(className)), owner);
    }

    private static String inferLayer(String className) {
        if (className.contains("ContractTest") || className.contains("OpenApiSpecificationTest")) {
            return "contract";
        }
        if (className.contains("ControllerTest")
            || className.contains("Migraciones")
            || className.contains("Rabbit")
            || className.contains("Outbox")
            || className.contains("ConfirmadorMensajesRabbit")) {
            return "integration";
        }
        if (className.contains("PreAuthorize")
            || className.contains("Security")
            || className.contains("Jwt")
            || className.contains("CredentialCipher")) {
            return "security";
        }
        return "unit";
    }

    private static String moduleEpic(String packageName) {
        if (packageName.contains("msusuariosauth")) {
            return "Identidad y Acceso";
        }
        if (packageName.contains("msnotificaciones")) {
            return "Notificaciones";
        }
        if (packageName.contains("msrabbitmqadmin")) {
            return "Plataforma";
        }
        return "Gestion Academica";
    }

    private static String moduleFeature(String packageName, String className) {
        for (Map.Entry<String, String> entry : MODULE_FEATURES.entrySet()) {
            if (packageName.contains(entry.getKey())) {
                if (entry.getKey().equals("msusuariosauth")) {
                    return userFeature(className);
                }
                return entry.getValue();
            }
        }
        return humanize(storyName(className));
    }

    private static String userFeature(String className) {
        if (className.contains("AsyncUserRegistration") || className.contains("MigracionesRegistro")) {
            return "Registro asincrono";
        }
        if (className.contains("Invitacion")) {
            return "Invitaciones";
        }
        if (className.contains("Credential")) {
            return "Credenciales";
        }
        if (className.contains("EmailInstitucional")) {
            return "Correo institucional";
        }
        if (className.contains("Migraciones")) {
            return "Migraciones";
        }
        return "Usuarios";
    }

    private static String bffFeature(String packageName, String className) {
        int domain = packageName.indexOf(".domain.");
        if (domain >= 0) {
            String rest = packageName.substring(domain + ".domain.".length());
            String[] parts = rest.split("\\.");
            StringBuilder feature = new StringBuilder();
            for (String part : parts) {
                if (part.equals("dto") || part.equals("api") || part.equals("internal")) {
                    break;
                }
                feature.append(humanize(part)).append(' ');
            }
            return feature.toString().trim();
        }
        if (packageName.contains(".integration.")) {
            String rest = packageName.substring(packageName.indexOf(".integration.") + ".integration.".length());
            return humanize(rest.replace('.', ' '));
        }
        return humanize(storyName(className));
    }

    private static String coreFeature(String packageName, String className) {
        if (packageName.contains(".security.")) {
            return "Seguridad";
        }
        if (packageName.contains(".mensajeria.") || packageName.contains(".messaging.")) {
            return "Mensajeria";
        }
        if (packageName.contains(".validation.")) {
            return "Validacion";
        }
        if (packageName.contains(".format.")) {
            return "Formato";
        }
        if (packageName.contains(".exception.")) {
            return "Errores";
        }
        if (packageName.contains(".config.")) {
            return "Configuracion";
        }
        if (packageName.contains(".dto.")) {
            return "Contratos";
        }
        return humanize(storyName(className));
    }

    private static String contractFeature(String className, String packageName) {
        for (Map.Entry<String, String> entry : CONTRACT_FEATURES.entrySet()) {
            if (className.startsWith(entry.getKey())) {
                return entry.getValue();
            }
        }
        return humanize(storyName(className));
    }

    private static String storyName(String className) {
        return className
            .replace("OpenApiSpecificationTest", "")
            .replace("ContractTest", "")
            .replace("SpecificationTest", "")
            .replace("ApplicationTests", "")
            .replace("Test", "");
    }

    private static String humanize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String spaced = value
            .replace("()", "")
            .replace('_', ' ')
            .replaceAll("([a-z0-9])([A-Z])", "$1 $2")
            .replaceAll("([A-Z]+)([A-Z][a-z])", "$1 $2")
            .replaceAll("\\s+", " ")
            .trim();

        String[] words = spaced.split(" ");
        StringBuilder result = new StringBuilder();
        for (int index = 0; index < words.length; index++) {
            String word = words[index];
            if (index == 0) {
                result.append(capitalize(word));
            } else if (word.length() > 1 && word.equals(word.toUpperCase(Locale.ROOT))) {
                result.append(word);
            } else {
                result.append(word.toLowerCase(Locale.ROOT));
            }
            if (index < words.length - 1) {
                result.append(' ');
            }
        }
        return result.toString();
    }

    private static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        return value.substring(0, 1).toUpperCase(Locale.ROOT) + value.substring(1);
    }

    private record Taxonomy(String layer, String epic, String feature, String story, String owner) {
    }
}
