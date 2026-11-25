package de.seuhd.campuscoffee.domain.model

/**
 * The regular expressions of the Bean Validation constraints that several domain models share. A constraint with
 * `@Pattern` accepts a text only if the whole text matches the regular expression.
 */
object TextPatterns {
    /**
     * Matches a text that is not blank, that is, a text with at least one character that is not whitespace. The
     * whitespace characters are those of `isBlank` in Kotlin: the Unicode separators, which include the no-break
     * space, and the control characters for tabs, line breaks, and separators. The domain models use this pattern
     * instead of `@NotBlank`, which accepts a text of no-break spaces.
     */
    const val NOT_BLANK = "(?s).*[^\\p{Z}\\t\\n\\x0B\\f\\r\\x1C-\\x1F].*"

    /**
     * Matches a text without a control character (Unicode category Cc). The control characters include NUL, which
     * PostgreSQL cannot store in a `varchar` column, and the line breaks.
     */
    const val NO_CONTROL_CHARACTER = "[^\\p{Cc}]*"

    /** Matches a text without the NUL character, which PostgreSQL cannot store in a `varchar` column. */
    const val NO_NUL_CHARACTER = "[^\\u0000]*"
}
