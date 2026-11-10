package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

/**
 * Spring Data JPA repository for [UserEntity]. Spring Data implements this interface at runtime and derives the query
 * of each method declared here from the method name.
 */
interface UserRepository : JpaRepository<UserEntity, UUID> {
    /** Returns every user, ordered by login name. */
    fun findAllByOrderByLoginNameAsc(): List<UserEntity>

    /**
     * Returns the user with [loginName], or null.
     *
     * @param loginName the login name to look up
     */
    fun findByLoginName(loginName: String): UserEntity?
}
