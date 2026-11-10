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
 * Links a unique constraint to the domain model and the field that must be unique. `CrudDataServiceImpl.upsert` uses
 * the mapping to convert a violation of the constraint into a [DuplicationException] (409) with the field and the
 * duplicate value. Every unique constraint and unique index of a table needs an entry in [REGISTRY].
 * `ConstraintMappingRegistryIntegrationTest` checks the entries against the schema. A violation of a constraint
 * without an entry reaches `GlobalExceptionHandler` as a `DataIntegrityViolationException` and becomes a 500.
 *
 * @param D the domain model with the unique field
 * @property constraintName the name of the constraint or unique index, as the migration declares it
 * @property domainClass the class of [D]
 * @property fieldName the name of the field in the exception message, such as "login name"
 * @param valueOf reads the value of the unique field from a domain object for the exception message
 */
class ConstraintMapping<D : DomainModel<*>>(
    val constraintName: String,
    val domainClass: Class<D>,
    val fieldName: String,
    private val valueOf: (D) -> String
) {
    /**
     * Returns the [DuplicationException] for [domain] with the value of the unique field.
     *
     * @param domain the domain object that violated the constraint, which must be an instance of [domainClass]
     */
    fun duplicationFor(domain: DomainModel<*>): DuplicationException =
        DuplicationException(domainClass, fieldName, valueOf(domainClass.cast(domain)))

    /** Holds [REGISTRY] and finds the mapping of a violated constraint by name. */
    companion object {
        /** The mappings of all unique constraints and unique indexes of the tables. */
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
         * Returns the mapping of the constraint named [constraintName], or null if [REGISTRY] does not contain that
         * name. The comparison ignores case.
         *
         * @param constraintName the name that the database reported
         */
        fun forConstraint(constraintName: String): ConstraintMapping<*>? = BY_NAME[constraintName.lowercase()]

        /**
         * Returns the name of the violated constraint from the first Hibernate [ConstraintViolationException] in the
         * cause chain of [exception]. Returns null if the chain does not contain a [ConstraintViolationException] or
         * if Hibernate did not extract a name. The PostgreSQL dialect of Hibernate extracts the name from the English
         * text of the error message. If the server writes its messages in another language, this method returns null,
         * and the violation becomes a 500.
         *
         * @param exception the data integrity violation to inspect
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
