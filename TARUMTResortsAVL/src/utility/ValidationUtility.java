package utility;

/**
 * ValidationUtility.java
 * Shared input format validation. Whatever module a given kind of input
 * (member ID, confirmation number, phone number) shows up in, this class is
 * the single set of rules deciding whether it's valid, so modules don't
 * each write their own inconsistent checks.
 */
public class ValidationUtility {

    private ValidationUtility() {
    }

    /**
     * Checks whether a string is blank (null, or empty after trimming).
     */
    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * Checks whether a string is exactly 8 digits — the valid format for a
     * confirmation number.
     */
    public static boolean isEightDigitNumber(String value) {
        return value != null && value.trim().matches("\\d{8}");
    }

    /**
     * Checks whether a string is made up of digits only (at least one
     * digit) — phone numbers only need this level of validity; no stricter
     * checks like area code or length are done.
     */
    public static boolean isDigitsOnly(String value) {
        return value != null && value.trim().matches("\\d+");
    }
    /**
     * Checks whether a string is a valid person's name — only letters,
     * spaces, and the punctuation common in names (apostrophe as in
     * O'Brien, hyphen as in Anne-Marie, period as in Jr., slash as in A/L,
     * A/P) are allowed. Digits and any other symbol are rejected.
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
    /**
     * Compares two identifiers (e.g. member IDs) case-insensitively to see
     * if they count as the same one — when a user types an ID to look
     * something up, case shouldn't affect whether it's found. Null-safe:
     * either side being null counts as not matching.
     */
    public static boolean idsMatch(String a, String b) {
        return a != null && b != null && a.equalsIgnoreCase(b);
    }

    /**
     * Validates a date input, only accepting the "yyyy-MM-dd" format — the
     * data files and report screens both use this format, so input follows
     * the same convention with no other formats supported.
     *
     * This actually checks whether the day exists, not just its shape:
     * 2026-02-30 looks like a date, but February has no 30th, so it's still
     * rejected — a plain regex \\d{4}-\\d{2}-\\d{2} check can't catch that.
     *
     * @param value the string entered by the user
     * @return the trimmed date string if valid; null if the format is wrong
     *         or the day doesn't exist
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
            // Only a successful parse confirms the day really exists
            // (this rejects things like 2026-02-30 or 2026-13-01).
            java.time.LocalDate.parse(trimmed);
            return trimmed;
        } catch (java.time.format.DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Checks whether a user-entered date is valid (correct format and the
     * day actually exists). Use this when only a true/false answer is
     * needed, not the date string itself.
     */
    public static boolean isValidDate(String value) {
        return normalizeDate(value) != null;
    }
}
