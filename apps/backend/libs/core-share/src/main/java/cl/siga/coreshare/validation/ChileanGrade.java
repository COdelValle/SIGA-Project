package cl.siga.coreshare.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ChileanGradeValidator.class)
public @interface ChileanGrade {
    String message() default "La nota debe estar en el rango de 1.0 a 7.0";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
