package healthcare;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import static org.junit.jupiter.api.Assertions.*;

import healthcare.utils.ValidationUtils;

/**
 * JUnit tests for ValidationUtils class.
 */
public class ValidationUtilsTest {

    @Test
    @DisplayName("Test email validation")
    void testEmailValidation() {
        // Valid emails
        assertTrue(ValidationUtils.isValidEmail("test@example.com"));
        assertTrue(ValidationUtils.isValidEmail("user.name@domain.co.uk"));
        assertTrue(ValidationUtils.isValidEmail("123@test.org"));

        // Invalid emails
        assertFalse(ValidationUtils.isValidEmail("invalid-email"));
        assertFalse(ValidationUtils.isValidEmail("@domain.com"));
        assertFalse(ValidationUtils.isValidEmail("test@"));
        assertFalse(ValidationUtils.isValidEmail(null));
        assertFalse(ValidationUtils.isValidEmail(""));
    }

    @Test
    @DisplayName("Test phone number validation")
    void testPhoneValidation() {
        // Valid phone numbers
        assertTrue(ValidationUtils.isValidPhone("1234567890"));
        assertTrue(ValidationUtils.isValidPhone("+61123456789"));
        assertTrue(ValidationUtils.isValidPhone("0123456789"));
        assertTrue(ValidationUtils.isValidPhone("12345678901234"));

        // Invalid phone numbers
        assertFalse(ValidationUtils.isValidPhone("123"));
        assertFalse(ValidationUtils.isValidPhone("abc123456789"));
        assertFalse(ValidationUtils.isValidPhone(null));
        assertFalse(ValidationUtils.isValidPhone(""));
        assertFalse(ValidationUtils.isValidPhone("123456789012345678")); // too long
    }

    @Test
    @DisplayName("Test ID validation")
    void testIdValidation() {
        // Valid IDs
        assertTrue(ValidationUtils.isValidId("ABC123"));
        assertTrue(ValidationUtils.isValidId("MGR001"));
        assertTrue(ValidationUtils.isValidId("DOC001"));
        assertTrue(ValidationUtils.isValidId("123"));
        assertTrue(ValidationUtils.isValidId("ABCDEF1234"));

        // Invalid IDs
        assertFalse(ValidationUtils.isValidId("AB")); // too short
        assertFalse(ValidationUtils.isValidId("ABCDEFGHIJK")); // too long
        assertFalse(ValidationUtils.isValidId("ABC-123")); // contains hyphen
        assertFalse(ValidationUtils.isValidId(null));
        assertFalse(ValidationUtils.isValidId(""));
    }

    @Test
    @DisplayName("Test gender validation")
    void testGenderValidation() {
        // Valid genders
        assertTrue(ValidationUtils.isValidGender("M"));
        assertTrue(ValidationUtils.isValidGender("F"));
        assertTrue(ValidationUtils.isValidGender("m"));
        assertTrue(ValidationUtils.isValidGender("f"));

        // Invalid genders
        assertFalse(ValidationUtils.isValidGender("Male"));
        assertFalse(ValidationUtils.isValidGender("Female"));
        assertFalse(ValidationUtils.isValidGender("X"));
        assertFalse(ValidationUtils.isValidGender(null));
        assertFalse(ValidationUtils.isValidGender(""));
    }

    @Test
    @DisplayName("Test dosage validation")
    void testDosageValidation() {
        // Valid dosages
        assertTrue(ValidationUtils.isValidDosage("100mg"));
        assertTrue(ValidationUtils.isValidDosage("2.5 mg"));
        assertTrue(ValidationUtils.isValidDosage("1 tablet"));
        assertTrue(ValidationUtils.isValidDosage("3 tablets"));
        assertTrue(ValidationUtils.isValidDosage("50ml"));
        assertTrue(ValidationUtils.isValidDosage("1.5g"));
        assertTrue(ValidationUtils.isValidDosage("2 pills"));

        // Invalid dosages
        assertFalse(ValidationUtils.isValidDosage("100"));
        assertFalse(ValidationUtils.isValidDosage("mg"));
        assertFalse(ValidationUtils.isValidDosage("abc mg"));
        assertFalse(ValidationUtils.isValidDosage(null));
        assertFalse(ValidationUtils.isValidDosage(""));
    }

    @Test
    @DisplayName("Test string emptiness validation")
    void testIsNotEmpty() {
        // Valid strings
        assertTrue(ValidationUtils.isNotEmpty("test"));
        assertTrue(ValidationUtils.isNotEmpty("  test  ")); // trimmed internally
        assertTrue(ValidationUtils.isNotEmpty("123"));

        // Invalid strings
        assertFalse(ValidationUtils.isNotEmpty(null));
        assertFalse(ValidationUtils.isNotEmpty(""));
        assertFalse(ValidationUtils.isNotEmpty("   ")); // only spaces
        assertFalse(ValidationUtils.isNotEmpty("\t\n")); // only whitespace
    }

    @Test
    @DisplayName("Test name formatting")
    void testNameFormatting() {
        assertEquals("John Smith", ValidationUtils.formatName("john smith"));
        assertEquals("Mary Jane Watson", ValidationUtils.formatName("MARY JANE WATSON"));
        assertEquals("Dr. Brown", ValidationUtils.formatName("dr. brown"));
        assertEquals("", ValidationUtils.formatName(null));
        assertEquals("", ValidationUtils.formatName(""));
        assertEquals("", ValidationUtils.formatName("   "));
    }

    @Test
    @DisplayName("Test string cleaning")
    void testStringCleaning() {
        assertEquals("test", ValidationUtils.cleanString("  test  "));
        assertEquals("hello world", ValidationUtils.cleanString("hello world"));
        assertEquals("", ValidationUtils.cleanString(null));
        assertEquals("", ValidationUtils.cleanString(""));
        assertEquals("", ValidationUtils.cleanString("   "));
    }
}
