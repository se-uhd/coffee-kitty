package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.DomainModel

/**
 * Generic data port for the create, read, update, and delete (CRUD) operations on the domain objects of one entity.
 * Each data port extends it, and each data adapter in the data module extends `CrudDataServiceImpl`.
 *
 * @param DOMAIN the domain model type
 * @param ID     the type of the unique identifier, which is [java.util.UUID] for every domain model
 */
interface CrudDataService<DOMAIN : DomainModel<ID>, ID> {
    /** Deletes every stored object of this entity. The deletion cannot be undone. */
    fun clear()

    /**
     * Retrieves every stored object of this entity.
     *
     * @return the objects, in an order that each data port defines, or an empty list
     */
    fun getAll(): List<DOMAIN>

    /**
     * Retrieves the object with the given id.
     *
     * @param id the id of the object to retrieve
     * @return the object with [id]
     * @throws NotFoundException if no object has [id]
     */
    fun getById(id: ID): DOMAIN

    /**
     * Stores a new object or updates a stored one. An object without an id is stored as a new object, and an object
     * with an id updates the stored object with that id, which keeps its creation time.
     *
     * @param domain the object to store
     * @return the stored object, with its id and timestamps
     * @throws NotFoundException if [domain] has an id that no stored object has, or references an object that does not
     *   exist
     * @throws DuplicationException if [domain] repeats a unique value of another object, such as a taken login name
     */
    fun upsert(domain: DOMAIN): DOMAIN

    /**
     * Deletes the object with the given id.
     *
     * @param id the id of the object to delete
     * @throws NotFoundException if no object has [id]
     */
    fun delete(id: ID)
}
