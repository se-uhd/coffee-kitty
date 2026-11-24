package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.KittyBalanceEntity
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock

/**
 * The Spring Data JPA repository for [KittyBalanceEntity]. The only row has the id [KittyBalanceEntity.SINGLETON_ID].
 * The inherited `findById` reads the kitty balance without a lock. No data adapter uses the repository yet.
 */
interface KittyBalanceRepository : JpaRepository<KittyBalanceEntity, Int> {
    /**
     * Returns the row with [id], or null, and locks the row until the transaction ends (see
     * [UserBalanceRepository.findForUpdateByUserId]). The lock does not cover a missing row. Pass
     * [KittyBalanceEntity.SINGLETON_ID] as [id].
     *
     * @param id the id of the row
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    fun findForUpdateById(id: Int): KittyBalanceEntity?
}
