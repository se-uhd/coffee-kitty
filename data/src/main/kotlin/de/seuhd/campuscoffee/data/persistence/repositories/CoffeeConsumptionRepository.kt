package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeConsumptionEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID

/**
 * Spring Data JPA repository for [CoffeeConsumptionEntity]. Spring Data implements this interface at runtime and
 * derives the query of each method declared here from the method name.
 */
interface CoffeeConsumptionRepository : JpaRepository<CoffeeConsumptionEntity, UUID> {
    /**
     * Returns the consumption of the user with [userId], or null. Spring Data reads `UserId` in the method name as the
     * property path `user.id`.
     *
     * @param userId the id of the user
     */
    fun findByUserId(userId: UUID): CoffeeConsumptionEntity?

    /**
     * Returns the consumption of the user with [userId], or null, and locks its row until the transaction ends.
     * `@Lock(PESSIMISTIC_WRITE)` sets the lock, and Hibernate sends it to PostgreSQL as `SELECT ... FOR NO KEY UPDATE`.
     * A concurrent call for the same user waits until the transaction ends and then reads the stored count. The word
     * `ForUpdate` in the method name does not change the query, because Spring Data ignores the words between `find`
     * and `By`.
     *
     * @param userId the id of the user
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateByUserId(userId: UUID): CoffeeConsumptionEntity?
}
