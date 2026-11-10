package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.mapper.UserEntityMapper
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.data.persistence.repositories.UserRepository
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.springframework.stereotype.Service
import java.util.UUID

/**
 * Data adapter that implements the [UserDataService] port on the `users` table. The business rules are in the domain
 * services. [upsert] throws a `DuplicationException` for a login name or an email address that another user has (see
 * [CrudDataServiceImpl]).
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
     * Returns every user, ordered by login name. The API returns the user list in this order, whereas
     * [CrudDataServiceImpl.getAll] orders by creation time.
     */
    override fun getAll(): List<User> = repository.findAllByOrderByLoginNameAsc().map { mapper.fromEntity(it) }

    override fun getByLoginName(loginName: String): User =
        findByFieldOrThrow({ repository.findByLoginName(loginName) }, "login name", loginName)
}
