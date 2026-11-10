package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Base class of the JPA entities. It extends [PersistableEntity] with the timestamps [createdAt] and [updatedAt],
 * which the lifecycle callbacks [onCreate] and [onUpdate] set before JPA inserts or updates a row. Each timestamp is
 * the current UTC time truncated to microseconds, because a PostgreSQL `timestamp` column stores microseconds. A
 * domain object mapped from a saved entity therefore has the same timestamps as the row that a later query reads.
 *
 * Annotate each subclass with the fully qualified `@jakarta.persistence.Entity`. In a file that imports
 * `jakarta.persistence.Entity`, the name `Entity` refers to the annotation and no longer to this class.
 */
@MappedSuperclass
abstract class Entity : PersistableEntity() {
    @field:Column(name = "created_at")
    var createdAt: LocalDateTime? = null

    @field:Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

    /** Sets [createdAt] and [updatedAt] to the current UTC time before an insert. */
    @PrePersist
    protected fun onCreate() {
        val now = utcNowInMicros()
        createdAt = now
        updatedAt = now
    }

    /** Sets [updatedAt] to the current UTC time before an update. */
    @PreUpdate
    protected fun onUpdate() {
        updatedAt = utcNowInMicros()
    }

    /** Returns the current UTC time, truncated to microseconds. */
    private fun utcNowInMicros(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
}
