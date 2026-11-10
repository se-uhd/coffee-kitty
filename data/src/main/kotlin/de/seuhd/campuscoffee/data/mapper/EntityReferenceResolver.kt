package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.User
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * For a `@ManyToOne` association, loads the referenced JPA entity by its id. [CoffeeConsumptionEntityMapper] lists this
 * class in `uses` and sets the user of a consumption to the stored [UserEntity]. Saving a consumption then writes only
 * the foreign key `user_id` and leaves the user row unchanged. Without the resolver, MapStruct maps the `User` in a
 * consumption with [UserEntityMapper], and an update of a consumption writes the fields of that `User` over the stored
 * user row.
 *
 * The resolver calls [EntityManager.find], which returns a loaded entity. `getReference` would return an
 * uninitialized proxy, and mapping a consumption back to its domain object would then load the user lazily. When the
 * persistence context already holds the user, for example because the same transaction created the user, `find`
 * returns that instance without a query.
 *
 * @param entityManager the JPA entity manager that loads the referenced entities
 */
@Component
class EntityReferenceResolver(
    private val entityManager: EntityManager
) {
    /**
     * Returns the stored [UserEntity] with [userId], or null if [userId] is null.
     *
     * @param userId the id of the user, or null if the association is empty
     * @throws NotFoundException if no user has [userId]
     */
    fun userReference(userId: UUID?): UserEntity? =
        userId?.let { entityManager.find(UserEntity::class.java, it) ?: throw NotFoundException(User::class.java, it) }
}
