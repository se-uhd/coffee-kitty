package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when storing a domain object would repeat a value that must be unique, such as a taken login name (a 409).
 *
 * @param domainClass the class of the domain object (e.g., `User::class.java`)
 * @param fieldName   the unique field in words (e.g., "login name" or "email address")
 * @param fieldValue  the duplicate value
 */
class DuplicationException(
    domainClass: Class<*>,
    fieldName: String,
    fieldValue: String
) : RuntimeException("${domainClass.simpleName} with $fieldName '$fieldValue' already exists.") {
    /**
     * The message for the server log, without the duplicate value, because the value can be a login name or an email
     * address and a log line names a user only by id.
     */
    val logMessage: String = "${domainClass.simpleName} with this $fieldName already exists."
}
