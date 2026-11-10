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
 * Domain implementation of [UserService]. [create] checks every new user, including the fixture users, which do not
 * pass through a DTO or Bean Validation. [create] stores the user and the user's consumption in one transaction. If
 * storing the consumption fails, the rollback removes the user too.
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
        // store the email address trimmed and in lowercase, because the UNIQUE constraint is case-sensitive and
        // would otherwise allow a user with Jane@x next to a user with jane@x
        val emailAddress = user.emailAddress.trim().lowercase()
        requireValidLoginName(user.loginName)
        requireValidEmailAddress(emailAddress)
        requireValidName("first name", user.firstName)
        requireValidName("last name", user.lastName)
        // drop any id from the client, because upsert would update the stored user with that id, and drop the
        // timestamps, which the data module sets
        val created =
            userDataService.upsert(
                user.copy(id = null, createdAt = null, updatedAt = null, emailAddress = emailAddress)
            )
        // create the consumption after the user, because coffee_consumptions.user_id must reference a stored user
        coffeeConsumptionDataService.upsert(CoffeeConsumption(user = created, count = 0))
        return created
    }

    /**
     * Checks that [loginName] has at most [User.MAX_LOGIN_NAME_LENGTH] characters and matches
     * [User.LOGIN_NAME_PATTERN].
     *
     * @param loginName the login name of the new user
     * @throws ValidationException if the login name fails the check
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
     * Checks that [emailAddress] has at most [User.MAX_EMAIL_LENGTH] characters and matches
     * [User.EMAIL_ADDRESS_PATTERN]. The pattern requires at least [User.MIN_EMAIL_LENGTH] characters.
     *
     * @param emailAddress the email address of the new user, trimmed and in lowercase
     * @throws ValidationException if the email address fails the check
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
     * Checks that [name] is not blank, has at most [User.MAX_NAME_LENGTH] characters, and contains no control
     * character. The control characters include NUL, which PostgreSQL cannot store in a `varchar` column.
     *
     * @param field the name of the field in words for the error message, such as "first name"
     * @param name the first or the last name of the new user
     * @throws ValidationException if the name fails the check
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
