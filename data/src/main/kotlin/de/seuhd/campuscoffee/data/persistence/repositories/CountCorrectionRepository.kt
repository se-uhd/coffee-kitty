package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.CountCorrectionEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * The Spring Data JPA repository for [CountCorrectionEntity]. Spring Data implements this interface at runtime and
 * derives the query of each method declared here from the method name.
 */
interface CountCorrectionRepository : JpaRepository<CountCorrectionEntity, UUID> {
    /**
     * Returns the corrections of the coffee count of the user with [userId], newest first. Spring Data reads `UserId`
     * in the method name as the property path `user.id`.
     *
     * @param userId the id of the user whose coffee count the admins corrected
     */
    fun findAllByUserIdOrderByCreatedAtDesc(userId: UUID): List<CountCorrectionEntity>
}
