package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.User
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Resolves a `@ManyToOne` association to its referenced entity by id, for use by the entity mappers (via their
 * `uses`). Mapping an association this way sets only the foreign key and never deep-copies or mutates the referenced
 * parent row. Without the resolver, MapStruct maps the `User` nested in a consumption through the user mapper, and an
 * update of the consumption writes the fields of that nested `User` over the referenced user row.
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
     * The [UserEntity] with the given id, or null when the id is null.
     *
     * @param userId the user id, or null for an absent association
     * @throws NotFoundException if no stored user has the id
     */
    fun userReference(userId: UUID?): UserEntity? =
        userId?.let { entityManager.find(UserEntity::class.java, it) ?: throw NotFoundException(User::class.java, it) }
}
