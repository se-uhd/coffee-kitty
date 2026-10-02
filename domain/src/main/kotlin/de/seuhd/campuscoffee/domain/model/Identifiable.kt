package de.seuhd.campuscoffee.domain.model

/**
 * Domain objects and DTOs that carry an identifier. Generic code, such as `CrudDataServiceImpl.upsert` in the data
 * layer and [persistedId], reads the id of any of these types through this interface.
 */
interface Identifiable<T> {
    /** The unique identifier, or null if the resource has not been created yet. */
    val id: T?
}

/**
 * This object's non-null [Identifiable.id]. Use it only when the object is known to be saved; on an unsaved
 * object (whose [Identifiable.id] is still null) it throws a `NullPointerException`.
 */
val <ID : Any> Identifiable<ID>.persistedId: ID get() = id!!
