package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

/**
 * Base class of the JPA entities. On top of [PersistableEntity]'s assigned id, it adds the createdAt / updatedAt
 * timestamps and sets them through the JPA lifecycle callbacks when a row is inserted or updated. Each timestamp is
 * the current UTC time truncated to the microsecond, the precision of a PostgreSQL `timestamp` column, so a domain
 * object mapped from the stored entity carries the same timestamps as the row that a later query reads.
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

    /** The current UTC time, truncated to the microsecond. */
    private fun utcNowInMicros(): LocalDateTime = LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS)
}
