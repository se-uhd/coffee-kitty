package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown by a data port when the database refuses to store or to delete a domain object because of a constraint other
 * than a unique constraint, or because a column cannot store one of the values of the object. Examples are a CHECK
 * constraint, such as the constraint that rejects a negative coffee count, a foreign key that prevents the deletion of
 * a referenced row, and a text with the NUL character, which PostgreSQL cannot store. A violated unique constraint
 * causes a [DuplicationException] instead.
 *
 * The exception is not a [DomainException], because it reports a missing check in the domain and not an error of the
 * client. A domain service has to check a rule before it calls the data port, and the constraint of the database is
 * only the last safeguard. The exception handler therefore maps the exception to the status 500 Internal Server Error
 * and logs it with its stack trace. With this exception, the contract of a data port names only types of the domain
 * module and no exception of the persistence framework.
 *
 * @param domainClass the class of the domain object (e.g., `User::class.java`)
 * @param reason      why the database refused the object, without its values, because the exception handler logs the
 *                    message (e.g., "it violates the constraint users_email_address_check")
 * @param cause       the exception of the persistence framework
 */
class DataConstraintException(
    domainClass: Class<*>,
    reason: String,
    cause: Throwable
) : RuntimeException("The database refused a ${domainClass.simpleName}, because $reason.", cause)
