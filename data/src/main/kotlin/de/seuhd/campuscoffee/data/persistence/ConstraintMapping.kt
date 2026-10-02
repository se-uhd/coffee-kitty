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
 * Maps a unique constraint of a table to the domain type and column that it guards, and reads the offending value
 * from the domain object that violated it, so that `CrudDataServiceImpl.upsert` reports the violation as a
 * [DuplicationException] (a 409). [REGISTRY] is the only list of these mappings, and every unique constraint or
 * unique index of a table needs an entry there. A violation of a constraint without an entry reaches the client as
 * a raw `DataIntegrityViolationException` (a 500).
 *
 * @param D the domain type with the unique field
 * @property constraintName the constraint or unique index name, as the Flyway DDL declares it
 * @property domainClass the class of [D]
 * @property columnName the guarded column, as the Flyway DDL declares it
 * @property fieldName the field as the exception message names it: in words for the login name and the email
 *   address, which a request can duplicate, and the column name otherwise
 * @param valueOf reads the value that the exception message shows from the domain object
 */
class ConstraintMapping<D : DomainModel<*>>(
    val constraintName: String,
    val domainClass: Class<D>,
    val columnName: String,
    val fieldName: String = columnName,
    private val valueOf: (D) -> String
) {
    /**
     * The exception for a violation of this constraint by the given domain object.
     *
     * @param domain the domain object that violated the constraint when it was stored, an instance of [domainClass]
     */
    fun duplicationFor(domain: DomainModel<*>): DuplicationException =
        DuplicationException(domainClass, fieldName, valueOf(domainClass.cast(domain)))

    /** The registry of the tables' unique constraints, and the lookup of a violated constraint by its name. */
    companion object {
        /** Every unique constraint and unique index of the tables. */
        val REGISTRY: List<ConstraintMapping<*>> =
            listOf(
                ConstraintMapping(
                    UserEntity.LOGIN_NAME_UNIQUE_CONSTRAINT,
                    User::class.java,
                    UserEntity.LOGIN_NAME_COLUMN,
                    fieldName = "login name"
                ) { it.loginName },
                ConstraintMapping(
                    UserEntity.EMAIL_ADDRESS_UNIQUE_CONSTRAINT,
                    User::class.java,
                    UserEntity.EMAIL_ADDRESS_COLUMN,
                    fieldName = "email address"
                ) { it.emailAddress },
                ConstraintMapping(
                    CoffeeConsumptionEntity.USER_UNIQUE_CONSTRAINT,
                    CoffeeConsumption::class.java,
                    "user_id"
                ) { "user ${it.user.id}" }
            )

        private val BY_NAME = REGISTRY.associateBy { it.constraintName.lowercase() }

        /**
         * The mapping of a violated constraint, or null for a constraint without one.
         *
         * @param constraintName the name that the database reported
         */
        fun forConstraint(constraintName: String): ConstraintMapping<*>? = BY_NAME[constraintName.lowercase()]

        /**
         * Returns the name of the database constraint reported by a data integrity violation, or null
         * when the cause chain contains no Hibernate [ConstraintViolationException]. Reading the name
         * the driver reported avoids matching on database-specific error-message text.
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
