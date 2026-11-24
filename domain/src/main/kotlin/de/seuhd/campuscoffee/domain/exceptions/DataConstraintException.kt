package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown by a data port when the database refuses to store or to delete a domain object because of a constraint other
 * than a unique constraint. Examples are a CHECK constraint, such as the constraint that rejects a negative coffee
 * count, and a foreign key that prevents the deletion of a referenced row. A violated unique constraint causes a
 * [DuplicationException] instead.
 *
 * The exception is not a [DomainException], because it reports a missing check in the domain and not an error of the
 * client. A domain service has to check a rule before it calls the data port, and the constraint of the database is
 * only the last safeguard. The exception handler therefore maps the exception to the status 500 Internal Server Error
 * and logs it with its stack trace. With this exception, the contract of a data port names only types of the domain
 * module and no exception of the persistence framework.
 *
 * @param domainClass    the class of the domain object (e.g., `User::class.java`)
 * @param constraintName the name of the violated constraint, or null if the database did not report a name
 * @param cause          the exception of the persistence framework
 */
class DataConstraintException(
    domainClass: Class<*>,
    constraintName: String?,
    cause: Throwable
) : RuntimeException(
        "The database refused a ${domainClass.simpleName}, because it violates the constraint " +
            "${constraintName ?: "(unnamed)"}.",
        cause
    )
