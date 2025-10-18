package healthcare.utils;

import java.util.regex.Pattern;

/**
 * ValidationUtils class provides utility methods for input validation.
 */
public class ValidationUtils {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^[\\+]?[0-9]{10,15}$");

    private static final Pattern ID_PATTERN =
            Pattern.compile("^[A-Z0-9]{3,10}$");

    /**
     * Validate email address format
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Validate phone number format
     */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone.replaceAll("\\s", "")).matches();
    }

    /**
     * Validate ID format (alphanumeric, 3-10 characters)
     */
    public static boolean isValidId(String id) {
        return id != null && ID_PATTERN.matcher(id.toUpperCase()).matches();
    }

    /**
     * Validate that string is not null or empty
     */
    public static boolean isNotEmpty(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Validate gender (M or F)
     */
    public static boolean isValidGender(String gender) {
        return gender != null && (gender.equalsIgnoreCase("M") || gender.equalsIgnoreCase("F"));
    }

    /**
     * Validate dosage format (number + unit)
     */
    public static boolean isValidDosage(String dosage) {
        return dosage != null && Pattern.matches("^\\d+(\\.\\d+)?\\s*(mg|g|ml|tablets?|pills?)$", dosage.toLowerCase());
    }

    /**
     * Clean and format input strings
     */
    public static String cleanString(String input) {
        if (input == null) return "";
        return input.trim();
    }

    /**
     * Format name (capitalize first letter of each word)
     */
    public static String formatName(String name) {
        if (name == null || name.trim().isEmpty()) return "";

        String[] words = name.trim().toLowerCase().split("\\s+");
        StringBuilder formatted = new StringBuilder();

        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(Character.toUpperCase(word.charAt(0)))
                        .append(word.substring(1))
                        .append(" ");
            }
        }

        return formatted.toString().trim();
    }
}
