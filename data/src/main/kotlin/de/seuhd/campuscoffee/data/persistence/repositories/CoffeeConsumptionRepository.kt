package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.CoffeeConsumptionEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID

/**
 * Spring Data JPA repository for [CoffeeConsumptionEntity]. Spring Data implements the interface at runtime and derives
 * the query of each declared method from its name.
 */
interface CoffeeConsumptionRepository : JpaRepository<CoffeeConsumptionEntity, UUID> {
    /**
     * Returns the consumption of the user with the given id, or null if none exists. Spring Data derives the query
     * from the nested `user.id` path.
     *
     * @param userId the id of the owning user
     */
    fun findByUserId(userId: UUID): CoffeeConsumptionEntity?

    /**
     * Returns the consumption of the user with the given id, or null if none exists, and locks its row until the
     * transaction ends (`PESSIMISTIC_WRITE`, which Hibernate sends to PostgreSQL as `SELECT ... FOR NO KEY UPDATE`). A
     * concurrent call for the same user waits for that transaction and then reads the count that it stored. Spring Data
     * ignores the words between `find` and `By`, so the `@Lock` annotation, not the method name, takes the lock.
     *
     * @param userId the id of the owning user
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateByUserId(userId: UUID): CoffeeConsumptionEntity?
}
