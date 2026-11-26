package de.seuhd.campuscoffee.data.mapper

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeBeanEntity
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.model.User
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * Loads the JPA entity of a `@ManyToOne` association by its id. The entity mappers, such as
 * [CoffeeConsumptionEntityMapper], list this class in `uses`, so that saving a consumption writes only the foreign key
 * `user_id`. Without the resolver, MapStruct would map the `User` with [UserEntityMapper], and an update of the
 * consumption would overwrite the stored user row. The resolver calls [EntityManager.find], because `getReference`
 * returns an uninitialized proxy, and mapping the consumption back to its domain object would load the user lazily.
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

    /**
     * Returns the stored [CoffeeBeanEntity] with [beanId], or null if [beanId] is null.
     *
     * @param beanId the id of the bean, or null if the association is empty
     * @throws NotFoundException if no bean has [beanId]
     */
    fun beanReference(beanId: UUID?): CoffeeBeanEntity? =
        beanId?.let {
            entityManager.find(CoffeeBeanEntity::class.java, it) ?: throw NotFoundException(CoffeeBean::class.java, it)
        }
}
