package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Resolves a `@ManyToOne` association to its referenced entity by id, for use by the entity mappers (via their
 * `uses`). Mapping an association this way sets only the foreign key and never deep-copies or mutates the referenced
 * parent row. Without it, MapStruct would map a consumption's nested `User` through the user mapper and, when it
 * updates a consumption, write the fields of that copy over the referenced user row.
 *
 * It uses [EntityManager.find] rather than `getReference`, so the association holds a loaded entity rather than an
 * uninitialized proxy, and code that maps the entity back to the domain reads the association's fields without a
 * lazy load. When the referenced row is already in the persistence context (such as a user created earlier in the
 * same transaction), `find` is a cache hit and issues no extra query.
 *
 * @param entityManager the JPA entity manager used to load the referenced entities
 */
@Component
class EntityReferenceResolver(
    private val entityManager: EntityManager
) {
    /**
     * The [UserEntity] for the given id, or null when the id is null.
     *
     * @param userId the user id, or null for an absent association
     */
    fun userReference(userId: UUID?): UserEntity? = userId?.let { entityManager.find(UserEntity::class.java, it) }
}
