package de.seuhd.campuscoffee.domain.model

/**
 * Domain objects and DTOs that carry an identifier. Generic code, such as `CrudDataServiceImpl.upsert` in the data
 * layer and [persistedId], reads the id of any of these types through this interface.
 */
interface Identifiable<T> {
    /** The unique identifier, or null if the object is not stored yet. */
    val id: T?
}

/**
 * The non-null [Identifiable.id] of a stored object. On an object that is not stored yet, whose id is still null, it
 * throws an `IllegalStateException`.
 */
val <ID : Any> Identifiable<ID>.persistedId: ID
    get() = checkNotNull(id) { "${this::class.simpleName} has no id, because it is not stored yet." }
