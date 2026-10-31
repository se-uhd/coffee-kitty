package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository for [UserEntity]. Spring Data implements the interface at runtime and derives the query of
 * each declared method from its name.
 */
interface UserRepository : JpaRepository<UserEntity, UUID> {
    /** Returns all users ordered by login name ascending. */
    fun findAllByOrderByLoginNameAsc(): List<UserEntity>

    /**
     * Returns the user with the given login name, or null if none matches.
     *
     * @param loginName the login name to look up
     */
    fun findByLoginName(loginName: String): UserEntity?
}
