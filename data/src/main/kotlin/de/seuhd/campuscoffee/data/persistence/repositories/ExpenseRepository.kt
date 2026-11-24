package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.ExpenseEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository for [ExpenseEntity]. Spring Data implements this interface at runtime and derives the
 * query of each method declared here from the method name. No data adapter uses the repository yet.
 */
interface ExpenseRepository : JpaRepository<ExpenseEntity, UUID> {
    /**
     * Returns the expenses of the buyer with [buyerId], newest first. Spring Data reads `BuyerId` in the method name as
     * the property path `buyer.id`.
     *
     * @param buyerId the id of the user who spent the money
     */
    fun findAllByBuyerIdOrderByCreatedAtDesc(buyerId: UUID): List<ExpenseEntity>
}
