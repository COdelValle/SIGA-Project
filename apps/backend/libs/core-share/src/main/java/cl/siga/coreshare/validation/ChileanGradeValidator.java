package cl.siga.coreshare.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class ChileanGradeValidator implements ConstraintValidator<ChileanGrade, Double> {
    @Override
    public boolean isValid(Double value, ConstraintValidatorContext context) {
        if (value == null) return false;
        return value >= 1.0 && value <= 7.0;
    }
}
