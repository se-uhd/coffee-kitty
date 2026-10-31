package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when no stored domain object matches a lookup (a 404). Its two constructors cover a failed lookup by id and
 * a failed lookup by a field value.
 */
class NotFoundException : RuntimeException {
    /**
     * The message for the server log, without a looked-up field value, because the value can be a login name and a log
     * line names a user only by id.
     */
    val logMessage: String

    /**
     * Creates an exception for a failed lookup by id.
     *
     * @param domainClass the class of the domain object (e.g., `User::class.java`)
     * @param id          the id that no object has
     */
    constructor(domainClass: Class<*>, id: Any) :
        super("${domainClass.simpleName} with ID $id does not exist.") {
        logMessage = "${domainClass.simpleName} with ID $id does not exist."
    }

    /**
     * Creates an exception for a failed lookup by a field value.
     *
     * @param domainClass the class of the domain object (e.g., `User::class.java`)
     * @param fieldName   the field in words (e.g., "login name")
     * @param fieldValue  the value that no object has
     */
    constructor(domainClass: Class<*>, fieldName: String, fieldValue: String) :
        super("${domainClass.simpleName} with $fieldName '$fieldValue' does not exist.") {
        logMessage = "${domainClass.simpleName} with this $fieldName does not exist."
    }
}
