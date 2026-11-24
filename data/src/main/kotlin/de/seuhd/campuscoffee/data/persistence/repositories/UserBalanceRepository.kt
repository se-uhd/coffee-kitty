package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.UserBalanceEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import java.util.UUID

/**
 * Spring Data JPA repository for [UserBalanceEntity], whose id is the id of the user. The inherited `findById` reads
 * the balance of a user without a lock. No data adapter uses the repository yet.
 */
interface UserBalanceRepository : JpaRepository<UserBalanceEntity, UUID> {
    /**
     * Returns the balance of the user with [userId], or null, and locks its row until the transaction ends, as
     * `CoffeeConsumptionRepository.findForUpdateByUserId` locks a consumption. A concurrent call for the same user
     * waits until the transaction ends and then reads the stored balance. A call outside a transaction throws an
     * exception, because a lock needs a transaction. The lock does not cover a missing row. Two transactions can both
     * find no row and both insert one, and the second insert then violates the primary key.
     *
     * @param userId the id of the user
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateByUserId(userId: UUID): UserBalanceEntity?
}
