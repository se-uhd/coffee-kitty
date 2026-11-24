package de.seuhd.campuscoffee.data.implementations

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.exceptions.NotFoundException
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.model.CoffeeRating
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeBeanDataService
import de.seuhd.campuscoffee.domain.ports.data.CoffeeRatingDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID

/**
 * Integration tests of [CoffeeRatingDataServiceImpl] against PostgreSQL. Every rating needs a stored user and a stored
 * bean. [storeUserAndBean] stores both before each test.
 */
class CoffeeRatingDataServiceIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var coffeeRatingDataService: CoffeeRatingDataService

    @Autowired
    private lateinit var coffeeBeanDataService: CoffeeBeanDataService

    @Autowired
    private lateinit var userDataService: UserDataService

    private lateinit var user: User

    private lateinit var bean: CoffeeBean

    /** Returns a user with [loginName] that is not stored yet. */
    private fun user(loginName: String): User =
        User(loginName = loginName, emailAddress = "$loginName@se.de", firstName = "First", lastName = "Last")

    @BeforeEach
    fun storeUserAndBean() {
        user = userDataService.upsert(user("rita"))
        bean = coffeeBeanDataService.upsert(CoffeeBean(name = "Ethiopia Sidamo"))
    }

    @Test
    fun `upsert stores a new rating with an id, timestamps, the user, and the bean`() {
        val created = coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = bean, value = 4))

        assertThat(created.id).isNotNull()
        assertThat(created.createdAt).isNotNull()
        assertThat(created.user).isEqualTo(user)
        assertThat(created.bean).isEqualTo(bean)
        assertThat(coffeeRatingDataService.getById(requireNotNull(created.id))).isEqualTo(created)
    }

    @Test
    fun `getAllByUserId returns the newest rating first, and getAllByBeanId returns the oldest rating first`() {
        val otherUser = userDataService.upsert(user("sven"))
        val otherBean = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenya AA"))
        val first = coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = bean, value = 3))
        val other = coffeeRatingDataService.upsert(CoffeeRating(user = otherUser, bean = bean, value = 5))
        val second = coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = otherBean, value = 2))

        assertThat(coffeeRatingDataService.getAllByUserId(requireNotNull(user.id))).containsExactly(second, first)
        assertThat(coffeeRatingDataService.getAllByBeanId(requireNotNull(bean.id))).containsExactly(first, other)
    }

    @Test
    fun `upsert of a stored rating changes neither createdAt nor the user nor the bean`() {
        val created = coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = bean, value = 4))

        val updated =
            coffeeRatingDataService.upsert(
                created.copy(
                    user = user.copy(firstName = "Changed"),
                    bean = bean.copy(name = "Renamed"),
                    value = 5,
                    createdAt = null,
                    updatedAt = null
                )
            )

        assertThat(updated.value).isEqualTo(5)
        assertThat(updated.createdAt).isEqualTo(created.createdAt)
        assertThat(userDataService.getById(requireNotNull(user.id))).isEqualTo(user)
        assertThat(coffeeBeanDataService.getById(requireNotNull(bean.id))).isEqualTo(bean)
    }

    @ParameterizedTest
    @ValueSource(ints = [CoffeeRating.MIN_VALUE, CoffeeRating.MAX_VALUE])
    fun `upsert stores a score of 1 or 5`(value: Int) {
        val created = coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = bean, value = value))

        assertThat(coffeeRatingDataService.getById(requireNotNull(created.id)).value).isEqualTo(value)
    }

    @ParameterizedTest
    @ValueSource(ints = [0, 6])
    fun `upsert throws DataConstraintException for a score of 0 or 6`(value: Int) {
        assertThatThrownBy { coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = bean, value = value)) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(coffeeRatingDataService.getAll()).isEmpty()
    }

    @Test
    fun `upsert throws NotFoundException for a rating of an unknown bean`() {
        val unknownBean = bean.copy(id = UUID(0L, 10L))

        assertThatThrownBy { coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = unknownBean, value = 4)) }
            .isInstanceOf(NotFoundException::class.java)
    }

    @Test
    fun `deleting a user deletes the ratings of the user`() {
        coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = bean, value = 4))

        userDataService.delete(requireNotNull(user.id))

        assertThat(coffeeRatingDataService.getAll()).isEmpty()
    }

    @Test
    fun `deleting a rated bean throws DataConstraintException`() {
        coffeeRatingDataService.upsert(CoffeeRating(user = user, bean = bean, value = 4))

        // the foreign key bean_id has the default NO ACTION
        assertThatThrownBy { coffeeBeanDataService.delete(requireNotNull(bean.id)) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(coffeeRatingDataService.getAll()).hasSize(1)
    }
}
