package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeRatingEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * The Spring Data JPA repository for [CoffeeRatingEntity]. Spring Data implements this interface at runtime and derives
 * the query of each method declared here from the method name.
 */
interface CoffeeRatingRepository : JpaRepository<CoffeeRatingEntity, UUID> {
    /**
     * Returns the ratings of the user with [userId], newest first. Spring Data reads `UserId` in the method name as the
     * property path `user.id`.
     *
     * @param userId the id of the user who gave the ratings
     */
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<CoffeeRatingEntity>

    /**
     * Returns the ratings of the bean with [beanId], oldest first. Spring Data reads `BeanId` in the method name as the
     * property path `bean.id`.
     *
     * @param beanId the id of the rated bean
     */
    fun findAllByBeanIdOrderByCreatedAtAsc(beanId: UUID): List<CoffeeRatingEntity>
}
