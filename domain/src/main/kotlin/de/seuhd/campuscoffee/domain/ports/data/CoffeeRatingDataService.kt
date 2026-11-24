package de.seuhd.campuscoffee.domain.ports.data

import de.seuhd.campuscoffee.domain.model.CoffeeRating
import java.util.UUID

/**
 * Data port for the ratings, with lookups by user and by bean in addition to the operations of [CrudDataService].
 * `CoffeeRatingDataServiceImpl` in the data module implements it. [getAll] returns the ratings, ordered by creation
 * time.
 */
interface CoffeeRatingDataService : CrudDataService<CoffeeRating, UUID> {
    /**
     * Returns the ratings of the user with [userId], newest first. The list is empty for a user without ratings and for
     * an unknown user.
     *
     * @param userId the id of the user who gave the ratings
     */
    fun getAllByUserId(userId: UUID): List<CoffeeRating>

    /**
     * Returns the ratings of the bean with [beanId], oldest first. The list is empty for a bean without ratings and for
     * an unknown bean. It does not contain the ratings of a bean that an admin merged into this bean.
     *
     * @param beanId the id of the rated bean
     */
    fun getAllByBeanId(beanId: UUID): List<CoffeeRating>
}
