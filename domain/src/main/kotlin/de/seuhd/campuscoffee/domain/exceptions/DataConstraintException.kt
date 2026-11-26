package de.seuhd.campuscoffee.domain.exceptions

/**
 * Thrown by a data port when the database refuses to store or to delete a domain object because of a constraint other
 * than a unique constraint, such as a CHECK constraint or a foreign key, or because a column cannot store a value, such
 * as a text with the NUL character. A violated unique constraint causes a [DuplicationException] instead. The exception
 * is not a [DomainException], because it reports a check that the domain is missing, not an error of the client. The
 * exception handler maps it to 500 Internal Server Error and logs its stack trace. With this exception, the contract
 * of a data port names no exception of the persistence framework.
 *
 * @param domainClass the class of the domain object (e.g., `User::class.java`)
 * @param reason why the database refused the object, without its values, because the exception handler logs it
 * @param cause the exception of the persistence framework
 */
class DataConstraintException(
    domainClass: Class<*>,
    reason: String,
    cause: Throwable
) : RuntimeException("The database refused a ${domainClass.simpleName}, because $reason.", cause)
