package de.seuhd.campuscoffee.domain.ports.internal

/**
 * Internal port for the data reset, which deletes the data of every entity in an order that the foreign keys allow.
 * The fixture loader and the tests call it. An architecture test fails if a class in the api module uses this port,
 * because no endpoint may offer a data reset.
 */
interface DataResetService {
    /** Deletes the data of every entity. */
    fun clearAll()
}
