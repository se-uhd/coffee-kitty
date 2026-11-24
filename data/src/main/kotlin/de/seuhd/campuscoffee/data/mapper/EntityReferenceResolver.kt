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
 * Loads the JPA entity of a `@ManyToOne` association by its id. [CoffeeConsumptionEntityMapper] lists this class in
 * `uses` and sets the user of a consumption to the stored [UserEntity]. Saving a consumption then writes only the
 * foreign key `user_id` and leaves the user row unchanged. [CountCorrectionEntityMapper] sets the user and the admin of
 * a correction in the same way. [PaymentEntityMapper] sets the user of a payment, and [CoffeeRatingEntityMapper] sets
 * the user and the bean of a rating. Without the resolver, MapStruct would map the `User` in a consumption with
 * [UserEntityMapper]. An update of the consumption would then overwrite the stored user row with the fields of that
 * `User`.
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
