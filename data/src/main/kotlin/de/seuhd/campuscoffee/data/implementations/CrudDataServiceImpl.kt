package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.EntityMapper
import de.seuhd.campuscoffee.data.persistence.ConstraintMapping
import de.seuhd.campuscoffee.data.persistence.entities.Entity
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.DomainModel
import de.seuhd.campuscoffee.domain.ports.data.CrudDataService
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Base of the relational data adapters. Each adapter reads, writes, and clears one table through its Spring Data
 * repository and maps between the domain model and the JPA entity with the type's MapStruct mapper.
 *
 * [upsert] and [delete] flush before they return, so that a violated constraint is raised inside the method rather
 * than at the commit. [upsert] reports a violated unique constraint of `ConstraintMapping.REGISTRY` as a
 * [DuplicationException] (a 409) and lets any other violation propagate.
 *
 * @param DOMAIN     the domain model type
 * @param ENTITY     the JPA entity type
 * @param REPOSITORY the repository type (a JpaRepository over the entity)
 * @param ID         the type of the unique identifier, which is [UUID] for every entity (see `PersistableEntity`)
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

    override fun getAll(): List<DOMAIN> = repository.findAll().map { mapper.fromEntity(it) }

    override fun getById(id: ID): DOMAIN = mapper.fromEntity(loadOrThrow(id))

    /**
     * Inserts an object without an id under a new random id, or updates the row of an object with an id. The
     * lifecycle callbacks of [Entity] set the timestamps. The result is mapped from the flushed entity without a
     * reload, because inside the transaction a reload would return the same managed instance.
     *
     * @throws NotFoundException if the object has an id that no row has
     * @throws DuplicationException if the object violates a unique constraint of `ConstraintMapping.REGISTRY`
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
     * Deletes the row with the given id. The database also deletes the rows of other tables that reference it with
     * `ON DELETE CASCADE`.
     *
     * @throws NotFoundException if no row has the id
     */
    @Transactional
    override fun delete(id: ID) {
        repository.delete(loadOrThrow(id))
        repository.flush()
    }

    /**
     * Queries by a unique field following the common pattern: query -> map -> orElseThrow. Reduces
     * duplication across data services that look up entities by a unique field other than the id.
     *
     * @throws NotFoundException if no entity matches the query
     */
    protected fun findByFieldOrThrow(
        queryFunction: () -> ENTITY?,
        fieldName: String,
        fieldValue: String
    ): DOMAIN =
        queryFunction()?.let { mapper.fromEntity(it) } ?: throw NotFoundException(domainClass, fieldName, fieldValue)

    /** Loads the managed entity with the given id, or throws a [NotFoundException] when no row has it. */
    private fun loadOrThrow(id: ID): ENTITY = repository.findByIdOrNull(id) ?: throw NotFoundException(domainClass, id)

    /**
     * The [DuplicationException] for a violated unique constraint, or null when the violation is not a known one.
     */
    private fun duplicationOrNull(
        exception: DataIntegrityViolationException,
        domain: DOMAIN
    ): DuplicationException? =
        ConstraintMapping
            .constraintNameOf(exception)
            ?.let { ConstraintMapping.forConstraint(it) }
            ?.duplicationFor(domain)
}
