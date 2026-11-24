package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.PaymentEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * The Spring Data JPA repository for [PaymentEntity]. Spring Data implements this interface at runtime and derives the
 * query of each method declared here from the method name.
 */
interface PaymentRepository : JpaRepository<PaymentEntity, UUID> {
    /**
     * Returns the deposits of the user with [userId], newest first. Spring Data reads `UserId` in the method name as
     * the property path `user.id`, so the result never contains a kitty adjustment, which has no user.
     *
     * @param userId the id of the user who paid the deposits
     */
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<PaymentEntity>
}
