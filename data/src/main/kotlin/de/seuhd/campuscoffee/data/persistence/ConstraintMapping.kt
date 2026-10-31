package de.seuhd.campuscoffee.data.persistence

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeConsumptionEntity
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.DomainModel
import de.seuhd.campuscoffee.domain.model.User
import org.hibernate.exception.ConstraintViolationException
import org.springframework.dao.DataIntegrityViolationException

/**
 * Maps a unique constraint of a table to the domain type and the field that it guards, and reads the offending value
 * from the domain object that violated it, so that `CrudDataServiceImpl.upsert` reports the violation as a
 * [DuplicationException] (a 409). [REGISTRY] is the only list of these mappings, and every unique constraint or
 * unique index of a table needs an entry there, which `ConstraintMappingRegistryIntegrationTest` checks. A violation
 * of a constraint without an entry propagates as a `DataIntegrityViolationException`, which `GlobalExceptionHandler`
 * answers with a generic 500.
 *
 * @param D the domain type with the unique field
 * @property constraintName the constraint or unique index name, as the Flyway DDL declares it
 * @property domainClass the class of [D]
 * @property fieldName the field in words, as the exception message names it
 * @param valueOf reads the value that the exception message shows from the domain object
 */
class ConstraintMapping<D : DomainModel<*>>(
    val constraintName: String,
    val domainClass: Class<D>,
    val fieldName: String,
    private val valueOf: (D) -> String
) {
    /**
     * The exception for a violation of this constraint by the given domain object.
     *
     * @param domain the domain object that violated the constraint on insert or update, an instance of [domainClass]
     */
    fun duplicationFor(domain: DomainModel<*>): DuplicationException =
        DuplicationException(domainClass, fieldName, valueOf(domainClass.cast(domain)))

    /** The registry of the tables' unique constraints, and the lookup of a violated constraint by its name. */
    companion object {
        /** Every unique constraint and unique index of the tables. */
        val REGISTRY: List<ConstraintMapping<*>> =
            listOf(
                ConstraintMapping(UserEntity.LOGIN_NAME_UNIQUE_CONSTRAINT, User::class.java, "login name") {
                    it.loginName
                },
                ConstraintMapping(UserEntity.EMAIL_ADDRESS_UNIQUE_CONSTRAINT, User::class.java, "email address") {
                    it.emailAddress
                },
                ConstraintMapping(
                    CoffeeConsumptionEntity.USER_UNIQUE_CONSTRAINT,
                    CoffeeConsumption::class.java,
                    "user ID"
                ) { it.user.id.toString() }
            )

        private val BY_NAME = REGISTRY.associateBy { it.constraintName.lowercase() }

        /**
         * The mapping of a violated constraint, or null for a constraint without one.
         *
         * @param constraintName the name that the database reported
         */
        fun forConstraint(constraintName: String): ConstraintMapping<*>? = BY_NAME[constraintName.lowercase()]

        /**
         * Returns the name of the violated constraint that Hibernate extracted from the database error, or null when
         * the cause chain contains no Hibernate [ConstraintViolationException] or Hibernate extracted no name.
         * Hibernate's PostgreSQL dialect reads the name from the English server message, so a server that reports its
         * messages in another language yields null, and the violation becomes a 500.
         *
         * @param exception the data integrity violation whose cause chain is inspected
         */
        fun constraintNameOf(exception: DataIntegrityViolationException): String? {
            var cause: Throwable? = exception
            while (cause != null) {
                if (cause is ConstraintViolationException) {
                    return cause.constraintName
                }
                cause = cause.cause
            }
            return null
        }
    }
}
