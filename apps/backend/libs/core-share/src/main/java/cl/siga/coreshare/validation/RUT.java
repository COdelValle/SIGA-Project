package cl.siga.coreshare.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = RUTValidator.class)
public @interface RUT {
    String message() default "RUT inválido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
