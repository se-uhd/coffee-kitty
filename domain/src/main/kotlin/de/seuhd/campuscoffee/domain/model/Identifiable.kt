package de.seuhd.campuscoffee.domain.model

/**
 * Type of the domain objects and DTOs that have an id. Generic code reads the id through this interface, for example
 * `CrudDataServiceImpl.upsert` in the data module and the extension property [persistedId].
 */
interface Identifiable<T> {
    /** The unique identifier, or null if the object is not stored yet. */
    val id: T?
}

/**
 * The [Identifiable.id] of a stored object as a non-null value. Throws an `IllegalStateException` for an object that
 * is not stored yet and has no id.
 */
val <ID : Any> Identifiable<ID>.persistedId: ID
    get() = checkNotNull(id) { "${this::class.simpleName} has no id, because it is not stored yet." }
