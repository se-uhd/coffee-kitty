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
 * The base class of the JPA entities with a [UUID] id that the application assigns. [isNew] returns true until JPA
 * loads or inserts the row, so that `save()` inserts a new entity without a SELECT for the row (ADR-021). The id is in
 * the private field `entityId`, because a Kotlin property named `id` would generate a second `getId()` next to the
 * override of [Persistable.getId]. Kotlin code still uses `entity.id`, because Kotlin treats `getId()` and `setId()`
 * as the property `id`.
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
     * Sets the id. In Kotlin code, the assignment `entity.id = value` calls this function.
     *
     * @param value the new id
     */
    fun setId(value: UUID?) {
        entityId = value
    }

    override fun isNew(): Boolean = !persisted

    @PostLoad
    @PostPersist
    private fun markPersisted() {
        persisted = true
    }
}
