package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.DomainModel

/**
 * Generic data port with the create, read, update, and delete (CRUD) operations for the domain objects of one entity.
 * Every data port extends this interface, and every data adapter in the data module extends `CrudDataServiceImpl`.
 *
 * @param DOMAIN the domain model
 * @param ID     the type of the id, which is [java.util.UUID] for every domain model
 */
interface CrudDataService<DOMAIN : DomainModel<ID>, ID> {
    /** Deletes every stored object of this entity. */
    fun clear()

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
     * Inserts [domain] under a new id if it has no id. Otherwise, updates the stored object with the same id and keeps
     * the creation time of that object.
     *
     * @param domain the object to store
     * @return the stored object with its id and timestamps
     * @throws NotFoundException if no stored object has the id of [domain], or if [domain] references an object that
     *   is not stored
     * @throws DuplicationException if [domain] repeats a unique value of another object, such as a taken login name
     */
    fun upsert(domain: DOMAIN): DOMAIN

    /**
     * Deletes the object with [id].
     *
     * @param id the id of the object
     * @throws NotFoundException if no object has [id]
     */
    fun delete(id: ID)
}
