package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.UserEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.data.persistence.repositories.UserRepository
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * The data layer's adapter for the [UserDataService] port over the `users` table. The business rules are in the
 * domain services. [upsert] reports a violation of the unique constraints on the login name and the email address
 * as a `DuplicationException` through `ConstraintMapping.REGISTRY`.
 */
@Service
class UserDataServiceImpl(
    repository: UserRepository,
    entityMapper: UserEntityMapper
) : CrudDataServiceImpl<User, UserEntity, UserRepository, UUID>(
        repository,
        entityMapper,
        User::class.java
    ),
    UserDataService {
    /**
     * Returns all users in a stable order (by login name ascending). Overrides the base, which uses the
     * repository's default (physically ordered) `findAll`, so that an update never reshuffles the user list.
     */
    override fun getAll(): List<User> = repository.findAllByOrderByLoginNameAsc().map { mapper.fromEntity(it) }

    /**
     * Retrieves a user by their unique login name.
     *
     * @throws NotFoundException if no user exists with the given login name
     */
    override fun getByLoginName(loginName: String): User =
        findByFieldOrThrow({ repository.findByLoginName(loginName) }, UserEntity.LOGIN_NAME_COLUMN, loginName)
}
