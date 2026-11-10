package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.EntityMapper
import de.seuhd.campuscoffee.data.persistence.ConstraintMapping
import de.seuhd.campuscoffee.data.persistence.entities.Entity
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.DomainModel
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.Sort
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Base class of the data adapters. Each data adapter reads, writes, and clears one table through a Spring Data
 * repository and converts between domain objects and JPA entities with a MapStruct [EntityMapper].
 *
 * [upsert] and [delete] each run in a transaction and flush before they return. The database therefore reports a
 * constraint violation inside these methods and not at the commit. [upsert] converts the violation of a unique
 * constraint that `ConstraintMapping.REGISTRY` lists into a [DuplicationException], which the API reports as a 409.
 * Any other violation reaches the caller unchanged.
 *
 * @param DOMAIN     the domain model type
 * @param ENTITY     the JPA entity type
 * @param REPOSITORY the type of the Spring Data repository for [ENTITY]
 * @param ID         the type of the id, which is [UUID] for every entity (see `PersistableEntity`)
 */
abstract class CrudDataServiceImpl<DOMAIN : DomainModel<ID>, ENTITY : Entity, REPOSITORY, ID : Any>(
    protected val repository: REPOSITORY,
    protected val mapper: EntityMapper<DOMAIN, ENTITY>,
    protected val domainClass: Class<DOMAIN>
) : CrudDataService<DOMAIN, ID>
    where REPOSITORY : JpaRepository<ENTITY, ID> {
    override fun clear() {
        repository.deleteAllInBatch()
        repository.flush()
    }

    /**
     * Returns all objects ordered by creation time and then by id. An update keeps the position of an object in the
     * list, because the update does not change the creation time.
     */
    override fun getAll(): List<DOMAIN> = repository.findAll(CREATION_ORDER).map { mapper.fromEntity(it) }

    override fun getById(id: ID): DOMAIN = mapper.fromEntity(loadOrThrow(id))

    /**
     * Inserts an object without an id under a new random id, or updates the row of an object with an id. The
     * lifecycle callbacks of [Entity] set the timestamps. The method maps the result from the flushed entity and does
     * not reload the row, because inside the transaction a reload would return the same managed instance.
     *
     * @throws NotFoundException if no row has the id of the object, or if the object references a row that does not
     *   exist (see `EntityReferenceResolver`)
     * @throws DuplicationException if the object violates a unique constraint that `ConstraintMapping.REGISTRY` lists
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
                throw duplicationOrNull(e, domain) ?: e
            }
        return mapper.fromEntity(saved)
    }

    /**
     * Deletes the row with [id]. With `ON DELETE CASCADE`, the database deletes the rows in other tables that reference
     * the deleted row, such as the consumption of a deleted user.
     *
     * @throws NotFoundException if no row has [id]
     */
    @Transactional
    override fun delete(id: ID) {
        repository.delete(loadOrThrow(id))
        repository.flush()
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

    /** Loads the managed entity with [id], or throws a [NotFoundException] if no row has that id. */
    private fun loadOrThrow(id: ID): ENTITY = repository.findByIdOrNull(id) ?: throw NotFoundException(domainClass, id)

    /**
     * Returns the [DuplicationException] for the unique constraint that [exception] reports. Returns null if Hibernate
     * did not extract a constraint name or if `ConstraintMapping.REGISTRY` does not list the constraint.
     */
    private fun duplicationOrNull(
        exception: DataIntegrityViolationException,
        domain: DOMAIN
    ): DuplicationException? =
        ConstraintMapping
            .constraintNameOf(exception)
            ?.let { ConstraintMapping.forConstraint(it) }
            ?.duplicationFor(domain)

    private companion object {
        /** Sort order of [getAll] by creation time and then by id. */
        private val CREATION_ORDER = Sort.by("createdAt", "id")
    }
}
