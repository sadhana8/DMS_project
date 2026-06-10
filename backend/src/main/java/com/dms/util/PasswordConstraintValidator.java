package com.dms.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordConstraintValidator
        implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext ctx) {
        if (value == null) return true; // let @NotBlank handle null check
        var result = PasswordValidator.validate(value);
        if (!result.valid()) {
            ctx.disableDefaultConstraintViolation();
            ctx.buildConstraintViolationWithTemplate(
                    String.join(" ", result.errors())
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}
