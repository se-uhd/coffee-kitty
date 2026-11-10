package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown when a lookup finds no stored domain object. The exception handler maps it to the status 404 Not Found.
 */
class NotFoundException : RuntimeException {
    /**
     * The message for the server log. It leaves out a looked-up field value, such as a login name, because log lines
     * identify users only by id.
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
     * @param fieldName   the name of the field in words (e.g., "login name")
     * @param fieldValue  the value that no object has
     */
    constructor(domainClass: Class<*>, fieldName: String, fieldValue: String) :
        super("${domainClass.simpleName} with $fieldName '$fieldValue' does not exist.") {
        logMessage = "${domainClass.simpleName} with this $fieldName does not exist."
    }
}
