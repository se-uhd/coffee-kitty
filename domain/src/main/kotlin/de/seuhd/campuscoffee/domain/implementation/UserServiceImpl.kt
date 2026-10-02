package de.seuhd.campuscoffee.domain.implementation

import de.seuhd.campuscoffee.domain.exceptions.ValidationException
import de.seuhd.campuscoffee.domain.model.CoffeeConsumption
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.api.UserService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeConsumptionDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

/**
 * Domain implementation of [UserService]. Creating a user checks the login name and the email address against the
 * rules in [User]'s companion object, stores the user with the normalized email address, and then creates the
 * user's coffee consumption at zero, all in one transaction.
 */
@Service
class UserServiceImpl(
    private val userDataService: UserDataService,
    private val coffeeConsumptionDataService: CoffeeConsumptionDataService
) : UserService {
    override fun getAll(): List<User> = userDataService.getAll()

    override fun getById(id: UUID): User = userDataService.getById(id)

    @Transactional
    override fun create(user: User): User {
        // normalize the email so the case-sensitive UNIQUE constraint treats Jane@x and jane@x as the same
        // address: two accounts for one person would split their coffee count
        val emailAddress = user.emailAddress.trim().lowercase()
        requireValidLoginName(user.loginName)
        requireValidEmailAddress(emailAddress)
        // a fresh user: drop any client id, so that the data layer assigns one
        val created = userDataService.upsert(user.copy(id = null, emailAddress = emailAddress))
        // the consumption is created after the user so its user_id foreign key resolves
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = created, count = 0))
        return created
    }

    /**
     * Refuses a login name unless it has at most [User.MAX_LOGIN_NAME_LENGTH] characters that match
     * [User.LOGIN_NAME_PATTERN].
     *
     * @param loginName the requested login name
     * @throws ValidationException if the login name is malformed
     */
    private fun requireValidLoginName(loginName: String) {
        if (loginName.length > User.MAX_LOGIN_NAME_LENGTH || !LOGIN_NAME_REGEX.matches(loginName)) {
            throw ValidationException(
                "A login name must have 1 to ${User.MAX_LOGIN_NAME_LENGTH} characters, each a letter, a digit, " +
                    "or an underscore."
            )
        }
    }

    /**
     * Refuses an email address over [User.MAX_EMAIL_LENGTH] characters or without the form name@domain
     * ([User.EMAIL_ADDRESS_PATTERN]), which also rules out an address shorter than [User.MIN_EMAIL_LENGTH]. The
     * check covers the users that bypass the DTO (the fixtures) and keeps an empty address away from the database
     * check constraint. It differs from the API's `@Email`, which the requests through the API pass first (see
     * [User.EMAIL_ADDRESS_PATTERN]).
     *
     * @param emailAddress the normalized email address
     * @throws ValidationException if the address breaks the rule
     */
    private fun requireValidEmailAddress(emailAddress: String) {
        if (emailAddress.length > User.MAX_EMAIL_LENGTH || !EMAIL_ADDRESS_REGEX.matches(emailAddress)) {
            throw ValidationException(
                "An email address must have the form name@domain, with no spaces and one at sign, and at most " +
                    "${User.MAX_EMAIL_LENGTH} characters."
            )
        }
    }

    private companion object {
        private val LOGIN_NAME_REGEX = Regex(User.LOGIN_NAME_PATTERN)
        private val EMAIL_ADDRESS_REGEX = Regex(User.EMAIL_ADDRESS_PATTERN)
    }
}
