package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Repository for persisting user entities.
 */
interface UserRepository : JpaRepository<UserEntity, UUID> {
    /**
     * Returns all users ordered by login name ascending. The order is stable and human-readable, so an update never
     * reshuffles the user list (PostgreSQL otherwise returns an updated row in a different physical position).
     */
    fun findAllByOrderByLoginNameAsc(): List<UserEntity>

    /**
     * Returns the user with the given login name, or null if none matches.
     *
     * @param loginName the login name to look up
     */
    fun findByLoginName(loginName: String): UserEntity?
}
