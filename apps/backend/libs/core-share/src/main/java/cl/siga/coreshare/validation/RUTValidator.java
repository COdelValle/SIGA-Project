package cl.siga.coreshare.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class RUTValidator implements ConstraintValidator<RUT, String> {

    @Override
    public void initialize(RUT constraintAnnotation) {
        // no initialization needed
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) return false;

        String rut = value.replace(".", "").replace("-", "").toUpperCase().trim();
        if (rut.length() < 8) return false;

        String numberPart = rut.substring(0, rut.length() - 1);
        char dvChar = rut.charAt(rut.length() - 1);

        if (!numberPart.chars().allMatch(Character::isDigit)) return false;

        try {
            int rutNum = Integer.parseInt(numberPart);
            char expectedDv = calculateDV(rutNum);
            return dvChar == expectedDv;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private char calculateDV(int rut) {
        int m = 0, s = 1;
        while (rut != 0) {
            s = (s + rut % 10 * (9 - m++ % 6)) % 11;
            rut /= 10;
        }
        if (s == 0) return 'K';
        return (char) ('0' + s - 1);
    }
}
