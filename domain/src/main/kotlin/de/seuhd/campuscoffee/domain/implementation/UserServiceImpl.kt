package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.Role
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import de.seuhd.campuscoffee.domain.ports.internal.UserInternalService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * The domain implementation of [UserService] and [UserInternalService]. [create] checks every new user against the
 * constraints on [User] with the [DomainValidator]. The check includes the user fixtures and the bootstrap admin, which
 * no request creates. [create] stores the user and the
 * user's consumption in one transaction. If [create] cannot store the consumption, the rollback removes the user too.
 */
@Service
class UserServiceImpl(
    private val userDataService: UserDataService,
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService,
    private val domainValidator: DomainValidator
) : UserService,
    UserInternalService {
    override fun getAll(): List<User> = userDataService.getAll()

    override fun getById(id: UUID): User = userDataService.getById(id)

    override fun getByLoginName(loginName: String): User = userDataService.getByLoginName(loginName)

    @Transactional
    override fun create(
        user: User,
        actingUser: User
    ): User {
        requireAdmin(actingUser, "create a user")
        return create(user)
    }

    @Transactional
    override fun createInitialAdmin(admin: User): User? {
        val users = userDataService.getAll()
        if (users.any { it.role == Role.ADMIN }) {
            return null
        }
        val existing = users.firstOrNull { it.loginName == admin.loginName }
        return if (existing != null) {
            userDataService.upsert(existing.copy(role = Role.ADMIN))
        } else {
            create(admin.copy(role = Role.ADMIN))
        }
    }

    @Transactional
    override fun create(user: User): User {
        // store the email address trimmed and in lowercase, because the UNIQUE constraint is case-sensitive and would
        // otherwise accept both Jane@x and jane@x
        // drop any id, because upsert would update the stored user with that id, and drop the timestamps, which the
        // data module sets
        val newUser =
            user.copy(
                id = null,
                createdAt = null,
                updatedAt = null,
                emailAddress = user.emailAddress.trim().lowercase()
            )
        domainValidator.requireValid(newUser)
        val created = userDataService.upsert(newUser)
        // create the consumption after the user, because coffee_consumptions.user_id must reference a stored user
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = created, count = 0))
        return created
    }
}
