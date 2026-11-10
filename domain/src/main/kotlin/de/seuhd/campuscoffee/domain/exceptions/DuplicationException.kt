package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when storing a domain object would repeat a value that must be unique, such as a taken login name. The
 * exception handler maps it to the status 409 Conflict.
 *
 * @param domainClass the class of the domain object (e.g., `User::class.java`)
 * @param fieldName   the name of the unique field in words (e.g., "login name" or "email address")
 * @param fieldValue  the duplicate value
 */
class DuplicationException(
    domainClass: Class<*>,
    fieldName: String,
    fieldValue: String
) : RuntimeException("${domainClass.simpleName} with $fieldName '$fieldValue' already exists.") {
    /**
     * The message for the server log. It leaves out the duplicate value, which can be a login name or an email
     * address, because log lines identify users only by id.
     */
    val logMessage: String = "${domainClass.simpleName} with this $fieldName already exists."
}
