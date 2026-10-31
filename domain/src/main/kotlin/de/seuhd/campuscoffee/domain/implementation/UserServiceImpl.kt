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
 * Domain implementation of [UserService]. Creating a user normalizes the email address, checks the login name, the
 * normalized address, and the names (see [create]), stores the user, and then creates the user's consumption at zero,
 * all in one transaction.
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
        // normalize the email address, so that the case-sensitive UNIQUE constraint treats Jane@x and jane@x as
        // the same address, because two users for one person would split their coffee count
        val emailAddress = user.emailAddress.trim().lowercase()
        requireValidLoginName(user.loginName)
        requireValidEmailAddress(emailAddress)
        requireValidName("first name", user.firstName)
        requireValidName("last name", user.lastName)
        // a new user: drop any id and timestamps from the client, so that the data layer assigns them
        val created =
            userDataService.upsert(
                user.copy(id = null, createdAt = null, updatedAt = null, emailAddress = emailAddress)
            )
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
                "A login name must have 1 to ${User.MAX_LOGIN_NAME_LENGTH} characters, each an ASCII letter, a " +
                    "digit, or an underscore."
            )
        }
    }

    /**
     * Refuses an email address over [User.MAX_EMAIL_LENGTH] characters or not matching [User.EMAIL_ADDRESS_PATTERN],
     * which also refuses an address shorter than [User.MIN_EMAIL_LENGTH]. The check also covers the fixture users,
     * which bypass the DTO.
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

    /**
     * Refuses a first or a last name that is blank, longer than [User.MAX_NAME_LENGTH] characters, or holds a control
     * character (e.g., NUL, which PostgreSQL cannot store in a text column). The check also covers the fixture users,
     * which bypass the DTO.
     *
     * @param field the name of the field in words, such as "first name"
     * @param name the requested name
     * @throws ValidationException if the name breaks the rule
     */
    private fun requireValidName(
        field: String,
        name: String
    ) {
        if (name.isBlank() || name.length > User.MAX_NAME_LENGTH || name.any { it.isISOControl() }) {
            throw ValidationException(
                "A $field must have 1 to ${User.MAX_NAME_LENGTH} characters, not only spaces, and no control " +
                    "characters."
            )
        }
    }

    private companion object {
        private val LOGIN_NAME_REGEX = Regex(User.LOGIN_NAME_PATTERN)
        private val EMAIL_ADDRESS_REGEX = Regex(User.EMAIL_ADDRESS_PATTERN)
    }
}
