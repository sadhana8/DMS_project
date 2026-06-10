package com.dms.util;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * JSR-303 annotation that wires {@link PasswordConstraintValidator} into
 * Bean Validation so {@code @Valid} on controller request bodies
 * automatically enforces the password policy.
 *
 * <p>Usage:
 * <pre>{@code
 * @ValidPassword
 * private String password;
 * }</pre>
 */
@Documented
@Constraint(validatedBy = PasswordConstraintValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default "Password does not meet security requirements.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
