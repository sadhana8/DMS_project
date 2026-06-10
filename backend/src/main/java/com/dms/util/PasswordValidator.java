package com.dms.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Industry-standard password policy enforcer.
 *
 * <p>Rules (NIST SP 800-63B + OWASP ASVS Level 2):
 * <ul>
 *   <li>Minimum 10 characters</li>
 *   <li>Maximum 128 characters (prevent bcrypt DoS)</li>
 *   <li>At least one uppercase letter (A-Z)</li>
 *   <li>At least one lowercase letter (a-z)</li>
 *   <li>At least one digit (0-9)</li>
 *   <li>At least one special character (!@#$%^&amp;*…)</li>
 *   <li>No leading or trailing whitespace</li>
 *   <li>No repeating character sequences ≥ 4 (e.g. "aaaa", "1111")</li>
 *   <li>Not a known common/breached password (top-100 list)</li>
 * </ul>
 */
public final class PasswordValidator {

    private PasswordValidator() {}

    private static final int MIN_LEN = 10;
    private static final int MAX_LEN = 128;

    private static final Pattern HAS_UPPER   = Pattern.compile("[A-Z]");
    private static final Pattern HAS_LOWER   = Pattern.compile("[a-z]");
    private static final Pattern HAS_DIGIT   = Pattern.compile("[0-9]");
    private static final Pattern HAS_SPECIAL = Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?`~]");
    private static final Pattern REPEATING   = Pattern.compile("(.)\\1{3,}");

    // Top-100 most breached passwords (HIBP + SplashData 2023 list)
    private static final java.util.Set<String> COMMON_PASSWORDS = java.util.Set.of(
        "password", "password1", "password123", "password1234", "password12345",
        "123456789", "1234567890", "12345678", "123456", "1234567",
        "qwerty", "qwerty123", "qwertyuiop", "qwerty1",
        "abc123", "abcd1234", "admin", "admin123", "administrator",
        "letmein", "letmein1", "welcome", "welcome1", "welcome123",
        "iloveyou", "sunshine", "princess", "dragon", "monkey",
        "master", "master123", "superman", "batman", "football",
        "shadow", "michael", "jessica", "charlie", "donald",
        "baseball", "soccer", "hockey", "basketball",
        "pass@123", "pass@1234", "pass@12345",
        "india@123", "nepal@123", "admin@123", "user@123",
        "p@ssword", "p@ssw0rd", "passw0rd", "p@ssw0rd1",
        "test1234", "test@123", "demo@123", "root", "root123",
        "toor", "1q2w3e4r", "1q2w3e4r5t", "qazwsx", "zxcvbnm",
        "aaaaaa", "111111", "123123", "654321", "666666", "777777",
        "000000", "121212", "696969", "159753", "123321",
        "mustang", "access", "starwars", "trustno1", "matrix",
        "computer", "login", "hello", "hello123", "hello@123",
        "changeme", "changeme1", "temp1234", "temp@123"
    );

    public record ValidationResult(boolean valid, List<String> errors) {}

    /**
     * Validates a password and returns a result with all failing rules listed.
     */
    public static ValidationResult validate(String password) {
        List<String> errors = new ArrayList<>();

        if (password == null || password.isEmpty()) {
            errors.add("Password is required.");
            return new ValidationResult(false, errors);
        }

        if (password.length() < MIN_LEN)
            errors.add("Must be at least " + MIN_LEN + " characters long.");

        if (password.length() > MAX_LEN)
            errors.add("Must not exceed " + MAX_LEN + " characters.");

        if (!password.equals(password.trim()))
            errors.add("Must not have leading or trailing spaces.");

        if (!HAS_UPPER.matcher(password).find())
            errors.add("Must contain at least one uppercase letter (A-Z).");

        if (!HAS_LOWER.matcher(password).find())
            errors.add("Must contain at least one lowercase letter (a-z).");

        if (!HAS_DIGIT.matcher(password).find())
            errors.add("Must contain at least one digit (0-9).");

        if (!HAS_SPECIAL.matcher(password).find())
            errors.add("Must contain at least one special character (e.g. @, #, $, !).");

        if (REPEATING.matcher(password).find())
            errors.add("Must not contain 4 or more repeated characters in a row (e.g. 'aaaa').");

        if (COMMON_PASSWORDS.contains(password.toLowerCase()))
            errors.add("This password is too common and has been found in data breaches. Choose a unique passphrase.");

        return new ValidationResult(errors.isEmpty(), errors);
    }

    /** Throws {@link IllegalArgumentException} with all errors joined if invalid. */
    public static void validateOrThrow(String password) {
        ValidationResult r = validate(password);
        if (!r.valid()) {
            throw new IllegalArgumentException(String.join(" ", r.errors()));
        }
    }
}
