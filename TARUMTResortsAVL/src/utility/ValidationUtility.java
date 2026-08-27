package utility;

/*
 * Shared validation methods used across different modules.
 *
 * @author All
 */
public class ValidationUtility {

    private ValidationUtility() {
    }

    // Checks whether a value is null or empty.
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    // Checks whether the value contains exactly 8 digits.
    public static boolean isEightDigitNumber(String value) {
        return value != null && value.trim().matches("\\d{8}");
    }
 // Checks whether the value contains digits only.
    public static boolean isDigitsOnly(String value) {
        return value != null && value.trim().matches("\\d+");
    }
   /*
     * Allows letters, spaces and common name punctuation.
     * Digits and other symbols are rejected.
     */
    public static boolean isValidName(String value) {

        if (isBlank(value)) {
            return false;
        }

        String trimmed = value.trim();
        for (int i = 0; i < trimmed.length(); i++) {
            char c = trimmed.charAt(i);
            boolean allowed = Character.isLetter(c)
                    || c == ' ' || c == '\'' || c == '-' || c == '.' || c == '/';
            if (!allowed) {
                return false;
            }
        }
        return true;
    }
    // Compares two IDs without considering uppercase or lowercase.
    public static boolean idsMatch(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

   /*
     * Accepts dates in yyyy-MM-dd format.
     * Returns null if the format or date is invalid.
     */
    public static String normalizeDate(String value) {

        if (isBlank(value)) {
            return null;
        }

        String trimmed = value.trim();

        if (!trimmed.matches("\\d{4}-\\d{2}-\\d{2}")) {
            return null;
        }

        try {
           // Confirms that the date actually exists.
            java.time.LocalDate.parse(trimmed);
            return trimmed;
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

      // Returns true only if the date is valid.
    public static boolean isValidDate(String value) {
        return normalizeDate(value) != null;
    }
}
