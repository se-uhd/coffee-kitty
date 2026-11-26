package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.DomainModel

/**
 * The generic data port with the create, read, update, and delete (CRUD) operations for the domain objects of one
 * entity. The data port of an entity that the domain may change and delete extends this interface. The data port of an
 * entity that never changes after it is stored, such as [CountCorrectionDataService], declares only the operations that
 * the domain needs. Every data adapter in the data module extends `CrudDataServiceImpl`.
 *
 * @param DOMAIN the domain model
 * @param ID     the type of the id, which is [java.util.UUID] for every domain model
 */
interface CrudDataService<DOMAIN : DomainModel<ID>, ID> {
    /** Returns every stored object of this entity. The KDoc of each data port states the order. */
    fun getAll(): List<DOMAIN>

    /**
     * Returns the object with [id].
     *
     * @param id the id of the object
     * @throws NotFoundException if no object has [id]
     */
    fun getById(id: ID): DOMAIN

    /**
     * Inserts [domain] under a new id if it has no id. Otherwise, updates the stored object with the same id, except
     * for its creation time.
     *
     * @param domain the object to store
     * @return the stored object with its id and timestamps
     * @throws NotFoundException if the id of [domain] or an object that [domain] references is not stored
     * @throws DuplicationException if [domain] repeats a unique value of another object, such as a taken login name
     * @throws DataConstraintException if [domain] violates another constraint or a column cannot store a value
     */
    fun upsert(domain: DOMAIN): DOMAIN

    /**
     * Deletes the object with [id].
     *
     * @param id the id of the object
     * @throws NotFoundException if no object has [id]
     * @throws DataConstraintException if a foreign key without `ON DELETE CASCADE` references the object
     */
    fun delete(id: ID)
}
