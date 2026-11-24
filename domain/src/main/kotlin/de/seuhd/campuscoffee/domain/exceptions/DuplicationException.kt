package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a domain object repeats a unique value of another object, such as a taken login name. The exception
 * handler maps the exception to the status 409 Conflict. The log message leaves out the duplicate value.
 *
 * @param domainClass the class of the domain object (e.g., `User::class.java`)
 * @param fieldName   the name of the unique field in words (e.g., "login name" or "email address")
 * @param fieldValue  the duplicate value
 */
class DuplicationException(
    domainClass: Class<*>,
    fieldName: String,
    fieldValue: String
) : DomainException(
        message = "${domainClass.simpleName} with $fieldName '$fieldValue' already exists.",
        logMessage = "${domainClass.simpleName} with this $fieldName already exists."
    )
