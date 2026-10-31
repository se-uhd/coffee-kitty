package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.UserEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.data.persistence.repositories.UserRepository
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * The data adapter for the [UserDataService] port over the `users` table. The business rules are in the
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
    /** Returns all users ordered by login name, the order of the user list, instead of the base's creation order. */
    override fun getAll(): List<User> = repository.findAllByOrderByLoginNameAsc().map { mapper.fromEntity(it) }

    override fun getByLoginName(loginName: String): User =
        findByFieldOrThrow({ repository.findByLoginName(loginName) }, "login name", loginName)
}
