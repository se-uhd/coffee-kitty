package de.seuhd.campuscoffee.domain.exceptions

/**
 * Generic exception thrown when an entity is not found in the database.
 * Supports finding by ID or by a specific field name and value.
 */
class NotFoundException : RuntimeException {
    /**
     * The message without a looked-up field value, for server logs, which name a user only by id: the value can
     * be a login name or an email address.
     */
    val logMessage: String

    /**
     * Creates an exception for an entity not found by ID.
     *
     * @param domainClass class of domain object (e.g., "User", "CoffeeConsumption")
     * @param id          the ID that was not found
     */
    constructor(domainClass: Class<*>, id: Any?) :
        super("${domainClass.simpleName} with ID $id does not exist.") {
        logMessage = "${domainClass.simpleName} with ID $id does not exist."
    }

    /**
     * Creates an exception for an entity not found by a specific field.
     *
     * @param domainClass class of domain object (e.g., "User", "CoffeeConsumption")
     * @param fieldName   the field name (e.g., "name", "login name")
     * @param fieldValue  the field value that was not found
     */
    constructor(domainClass: Class<*>, fieldName: String, fieldValue: String) :
        super("${domainClass.simpleName} with $fieldName '$fieldValue' does not exist.") {
        logMessage = "${domainClass.simpleName} with this $fieldName does not exist."
    }
}
