package de.seuhd.campuscoffee.data.persistence.entities

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PostLoad
import jakarta.persistence.PostPersist
import jakarta.persistence.Transient
import org.springframework.data.domain.Persistable
import java.util.UUID

/**
 * Base class of the JPA entities with a [UUID] id that the application assigns, not the database. By default, Spring
 * Data treats an entity with an id as stored, and `repository.save()` calls `merge`, which first runs a SELECT for the
 * row. This class implements [Persistable], and [isNew] returns true until JPA loads or inserts the row. `save()`
 * therefore inserts a new entity without the SELECT.
 *
 * The private field `entityId` holds the id, and the functions `getId()` and `setId()` read and write it. A Kotlin
 * property named `id` would generate a second `getId()` next to the override of [Persistable.getId], and the two would
 * clash. Kotlin code still writes `entity.id`. Because `getId()` overrides a getter of the Java interface
 * [Persistable], Kotlin treats `getId()` and `setId()` as the property `id`.
 *
 * [Entity] extends this class with timestamps, and every JPA entity extends [Entity].
 */
@MappedSuperclass
abstract class PersistableEntity : Persistable<UUID> {
    @field:Id
    @field:Column(name = "id")
    private var entityId: UUID? = null

    @field:Transient
    private var persisted = false

    override fun getId(): UUID? = entityId

    /**
     * Sets the id. Kotlin code writes `entity.id = value` to call this function.
     *
     * @param value the new id
     */
    fun setId(value: UUID?) {
        entityId = value
    }

    override fun isNew(): Boolean = !persisted

    /** Sets `persisted` after JPA loads or inserts the row. [isNew] returns false from then on. */
    @PostLoad
    @PostPersist
    private fun markPersisted() {
        persisted = true
    }
}
