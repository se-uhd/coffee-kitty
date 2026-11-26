package de.seuhd.campuscoffee.data.implementation

import de.seuhd.campuscoffee.data.mapper.EntityMapper
import de.seuhd.campuscoffee.data.persistence.ConstraintMapping
import de.seuhd.campuscoffee.data.persistence.entities.Entity
import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.DomainModel
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import java.sql.SQLException
import java.util.UUID

/**
 * The base class of the data adapters. Each data adapter reads and writes one table through a Spring Data repository
 * and converts between domain objects and JPA entities with a MapStruct [EntityMapper].
 *
 * [upsert] and [delete] each run in a transaction and flush before they return. The database therefore reports a
 * constraint violation inside these methods and not at the commit. [upsert] throws a [DuplicationException] (409) for
 * the violation of a unique constraint that `ConstraintMapping.REGISTRY` lists. For any other violation, [upsert] and
 * [delete] throw a [DataConstraintException], so that no exception of Spring or Hibernate crosses the data port.
 *
 * @param DOMAIN     the domain model type
 * @param ENTITY     the JPA entity type
 * @param REPOSITORY the type of the Spring Data repository for [ENTITY]
 * @param ID         the type of the id, which is [UUID] for every subclass of [Entity] (see `PersistableEntity`)
 */
abstract class CrudDataServiceImpl<DOMAIN : DomainModel<ID>, ENTITY : Entity, REPOSITORY, ID : Any>(
    protected val repository: REPOSITORY,
    protected val mapper: EntityMapper<DOMAIN, ENTITY>,
    protected val domainClass: Class<DOMAIN>
) : CrudDataService<DOMAIN, ID>
    where REPOSITORY : JpaRepository<ENTITY, ID> {
    /**
     * Returns all objects ordered by creation time and then by id. An update does not change the position of an object
     * in the list, because it does not change the creation time.
     */
    override fun getAll(): List<DOMAIN> = repository.findAll(CREATION_ORDER).map { mapper.fromEntity(it) }

    override fun getById(id: ID): DOMAIN = mapper.fromEntity(loadOrThrow(id))

    /**
     * Inserts an object without an id under a new random id, or updates the row of an object with an id. The
     * lifecycle callbacks of [Entity] set the timestamps. The method maps the result from the flushed entity and does
     * not reload the row, because inside the transaction a reload would return the same managed instance.
     *
     * @throws NotFoundException if no row has the id of the object or the object references a row that does not exist
     * @throws DuplicationException if the object violates a unique constraint that `ConstraintMapping.REGISTRY` lists
     * @throws DataConstraintException if the object violates another constraint or a column cannot store a value
     */
    @Transactional
    override fun upsert(domain: DOMAIN): DOMAIN {
        val id = domain.id
        val entity =
            if (id == null) {
                mapper.toEntity(domain).also { it.id = UUID.randomUUID() }
            } else {
                loadOrThrow(id).also { mapper.updateEntity(domain, it) }
            }
        val saved =
            try {
                repository.saveAndFlush(entity)
            } catch (e: DataIntegrityViolationException) {
                throw duplicationOrNull(e, domain) ?: constraintException(e)
            }
        return mapper.fromEntity(saved)
    }

    /**
     * Deletes the row with [id]. With `ON DELETE CASCADE`, the database deletes the rows in other tables that reference
     * the deleted row, such as the consumption of a deleted user.
     *
     * @throws NotFoundException if no row has [id]
     * @throws DataConstraintException if a foreign key without `ON DELETE CASCADE` references the row
     */
    @Transactional
    override fun delete(id: ID) {
        val entity = loadOrThrow(id)
        try {
            repository.delete(entity)
            repository.flush()
        } catch (e: DataIntegrityViolationException) {
            throw constraintException(e)
        }
    }

    /**
     * Maps the entity that [queryFunction] returns to its domain object. A data adapter calls this method to look up a
     * row by a unique field other than the id, such as the login name.
     *
     * @throws NotFoundException with [fieldName] and [fieldValue] in the message if [queryFunction] returns null
     */
    protected fun findByFieldOrThrow(
        queryFunction: () -> ENTITY?,
        fieldName: String,
        fieldValue: String
    ): DOMAIN =
        queryFunction()?.let { mapper.fromEntity(it) } ?: throw NotFoundException(domainClass, fieldName, fieldValue)

    private fun loadOrThrow(id: ID): ENTITY = repository.findByIdOrNull(id) ?: throw NotFoundException(domainClass, id)

    private fun duplicationOrNull(
        exception: DataIntegrityViolationException,
        domain: DOMAIN
    ): DuplicationException? =
        ConstraintMapping
            .constraintNameOf(exception)
            ?.let { ConstraintMapping.forConstraint(it) }
            ?.duplicationFor(domain)

    // External: https://www.postgresql.org/docs/current/errcodes-appendix.html PostgreSQL reports a value that its
    // column cannot store, such as a text with the NUL character, as a data exception and not as a violated constraint.
    private fun constraintException(exception: DataIntegrityViolationException): DataConstraintException {
        val name = ConstraintMapping.constraintNameOf(exception)
        val state = generateSequence<Throwable>(exception) { it.cause }.filterIsInstance<SQLException>().firstOrNull()
        val sqlState = state?.sqlState ?: "unknown"
        val reason =
            when {
                name != null -> "it violates the constraint $name"
                sqlState.startsWith(DATA_EXCEPTION_CLASS) -> "a column cannot store one of its values"
                else -> "it violates a constraint, and the database did not report its name"
            }
        val suffix = if (name == null) " (SQL state $sqlState)" else ""
        return DataConstraintException(domainClass, reason + suffix, exception)
    }

    private companion object {
        private val CREATION_ORDER = Sort.by("createdAt", "id")

        /** The first two characters of the SQL state of a data exception, such as 22021 for the NUL character. */
        const val DATA_EXCEPTION_CLASS = "22"
    }
}
