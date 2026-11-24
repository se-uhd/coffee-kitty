package de.seuhd.campuscoffee.data.persistence.repositories

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import de.seuhd.campuscoffee.data.persistence.entities.CoffeeBeanEntity
import de.seuhd.campuscoffee.data.persistence.entities.ExpenseEntity
import de.seuhd.campuscoffee.data.persistence.entities.KittyBalanceEntity
import de.seuhd.campuscoffee.data.persistence.entities.UserBalanceEntity
import de.seuhd.campuscoffee.data.persistence.entities.UserEntity
import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.model.User
import de.seuhd.campuscoffee.domain.ports.data.CoffeeBeanDataService
import de.seuhd.campuscoffee.domain.ports.data.UserDataService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.assertj.core.api.SoftAssertions.assertSoftly
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.support.TransactionTemplate

/**
 * Integration tests of the tables `expenses`, `user_balance`, and `kitty_balance` against PostgreSQL. Each of these
 * tables has a JPA entity and a repository but no data port yet. The tests therefore write and read the rows through
 * the repositories.
 */
class ExpenseAndBalanceRepositoryIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var expenseRepository: ExpenseRepository

    @Autowired
    private lateinit var userBalanceRepository: UserBalanceRepository

    @Autowired
    private lateinit var kittyBalanceRepository: KittyBalanceRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var coffeeBeanRepository: CoffeeBeanRepository

    @Autowired
    private lateinit var userDataService: UserDataService

    @Autowired
    private lateinit var coffeeBeanDataService: CoffeeBeanDataService

    @Autowired
    private lateinit var transactionTemplate: TransactionTemplate

    private lateinit var buyer: UserEntity

    private lateinit var bean: CoffeeBeanEntity

    @BeforeEach
    fun storeBuyerAndBean() {
        val user =
            userDataService.upsert(
                User(loginName = "bert", emailAddress = "bert@se.de", firstName = "Bert", lastName = "Buyer")
            )
        buyer = requireNotNull(userRepository.findByIdOrNull(requireNotNull(user.id)))
        val storedBean = coffeeBeanDataService.upsert(CoffeeBean(name = "Ethiopia Sidamo"))
        bean = requireNotNull(coffeeBeanRepository.findByIdOrNull(requireNotNull(storedBean.id)))
    }

    /** Returns a bean purchase of [buyer] for 1299 cents, of which the kitty pays [kittyAmountCents]. */
    private fun beanPurchase(kittyAmountCents: Int = 0): ExpenseEntity =
        ExpenseEntity().apply {
            buyer = this@ExpenseAndBalanceRepositoryIntegrationTest.buyer
            expenseType = ExpenseEntity.BEANS_TYPE
            bean = this@ExpenseAndBalanceRepositoryIntegrationTest.bean
            weightGrams = 500
            amountCents = 1299
            privateAmountCents = 1299 - kittyAmountCents
            this.kittyAmountCents = kittyAmountCents
        }

    /** Returns an expense of [buyer] with the type `OTHER` for 350 cents with [note] and without a bean or a weight. */
    private fun otherExpense(note: String? = "Filters"): ExpenseEntity =
        ExpenseEntity().apply {
            buyer = this@ExpenseAndBalanceRepositoryIntegrationTest.buyer
            expenseType = ExpenseEntity.OTHER_TYPE
            amountCents = 350
            privateAmountCents = 350
            kittyAmountCents = 0
            this.note = note
        }

    @Test
    fun `findAllByBuyerIdOrderByCreatedAtDesc returns the expenses of the buyer, newest first`() {
        val purchase = expenseRepository.saveAndFlush(beanPurchase(kittyAmountCents = 299).apply { id = randomId() })
        val other = expenseRepository.saveAndFlush(otherExpense().apply { id = randomId() })

        val stored = expenseRepository.findAllByBuyerIdOrderByCreatedAtDesc(requireNotNull(buyer.id))

        assertThat(stored.map { it.id }).containsExactly(other.id, purchase.id)
        val storedPurchase = stored.last()
        assertThat(storedPurchase.buyer?.loginName).isEqualTo("bert")
        assertThat(storedPurchase.expenseType).isEqualTo("BEANS")
        assertThat(storedPurchase.bean?.name).isEqualTo("Ethiopia Sidamo")
        assertThat(storedPurchase.weightGrams).isEqualTo(500)
        assertThat(storedPurchase.amountCents).isEqualTo(1299)
        assertThat(storedPurchase.privateAmountCents).isEqualTo(1000)
        assertThat(storedPurchase.kittyAmountCents).isEqualTo(299)
        assertThat(storedPurchase.note).isNull()
        assertThat(stored.first().note).isEqualTo("Filters")
    }

    /**
     * Asserts that the database refuses to store each of [expenses], each under a new id. The function checks every
     * expense and reports each one that the database stored by its description.
     *
     * @param expenses the expenses, each with a description for the failure message
     */
    private fun assertRefused(vararg expenses: Pair<String, ExpenseEntity>) {
        assertSoftly { softly ->
            for ((description, expense) in expenses) {
                expense.id = randomId()
                softly
                    .assertThatThrownBy { expenseRepository.saveAndFlush(expense) }
                    .`as`(description)
                    .isInstanceOf(DataIntegrityViolationException::class.java)
            }
        }
        assertThat(expenseRepository.count()).isZero()
    }

    @Test
    fun `saving a bean purchase without a bean or without a weight above 0 throws DataIntegrityViolationException`() {
        assertRefused(
            "without a bean" to beanPurchase().apply { bean = null },
            "without a weight" to beanPurchase().apply { weightGrams = null },
            "with a weight of 0" to beanPurchase().apply { weightGrams = 0 }
        )
    }

    @Test
    fun `saving an expense of the type OTHER with a bean or a weight throws DataIntegrityViolationException`() {
        assertRefused(
            "with a bean" to otherExpense().also { it.bean = bean },
            "with a weight" to otherExpense().also { it.weightGrams = 250 }
        )
    }

    @Test
    fun `saving an expense with an amount of 0 or a negative share throws DataIntegrityViolationException`() {
        assertRefused(
            "with an amount of 0" to
                otherExpense().apply {
                    amountCents = 0
                    privateAmountCents = 0
                },
            "with a negative private share" to
                otherExpense().apply {
                    privateAmountCents = -1
                    kittyAmountCents = 351
                },
            "with a negative kitty share" to
                otherExpense().apply {
                    privateAmountCents = 351
                    kittyAmountCents = -1
                }
        )
    }

    @Test
    fun `saving an expense with shares that do not add up to its amount throws DataIntegrityViolationException`() {
        assertRefused(
            "with a kitty share of 1 and a private share of the full amount" to
                beanPurchase().apply { kittyAmountCents = 1 }
        )
    }

    @Test
    fun `saving an expense with an unknown type or an empty note throws DataIntegrityViolationException`() {
        assertRefused(
            "with the type MILK" to otherExpense().apply { expenseType = "MILK" },
            "with an empty note" to otherExpense(note = "")
        )
    }

    @Test
    fun `deleting the buyer of an expense throws DataConstraintException`() {
        expenseRepository.saveAndFlush(otherExpense().apply { id = randomId() })

        // the foreign key buyer_user_id has the default NO ACTION, because an expense must not disappear with its buyer
        assertThatThrownBy { userDataService.delete(requireNotNull(buyer.id)) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(expenseRepository.count()).isEqualTo(1)
    }

    @Test
    fun `deleting a purchased bean throws DataConstraintException`() {
        expenseRepository.saveAndFlush(beanPurchase().apply { id = randomId() })

        // the foreign key bean_id has the default NO ACTION, as buyer_user_id has
        assertThatThrownBy { coffeeBeanDataService.delete(requireNotNull(bean.id)) }
            .isInstanceOf(DataConstraintException::class.java)
        assertThat(expenseRepository.count()).isEqualTo(1)
    }

    @Test
    fun `deleting a user deletes the balance of the user`() {
        val userId = requireNotNull(buyer.id)
        userBalanceRepository.saveAndFlush(
            UserBalanceEntity().apply {
                this.userId = userId
                balanceCents = -450
            }
        )

        assertThat(userBalanceRepository.findByIdOrNull(userId)?.balanceCents).isEqualTo(-450)

        userDataService.delete(userId)

        assertThat(userBalanceRepository.count()).isZero()
    }

    @Test
    fun `findForUpdateByUserId returns the balance of the user`() {
        val userId = requireNotNull(buyer.id)
        userBalanceRepository.saveAndFlush(
            UserBalanceEntity().apply {
                this.userId = userId
                balanceCents = 550
            }
        )

        val balance = transactionTemplate.execute { userBalanceRepository.findForUpdateByUserId(userId)?.balanceCents }

        assertThat(balance).isEqualTo(550)
    }

    @Test
    fun `findForUpdateById returns the kitty balance`() {
        kittyBalanceRepository.saveAndFlush(KittyBalanceEntity().apply { balanceCents = 2000 })

        val balance =
            transactionTemplate.execute {
                kittyBalanceRepository.findForUpdateById(KittyBalanceEntity.SINGLETON_ID)?.balanceCents
            }

        assertThat(balance).isEqualTo(2000)
    }

    @Test
    fun `saving a kitty balance below 0 throws DataIntegrityViolationException`() {
        assertThatThrownBy { kittyBalanceRepository.saveAndFlush(KittyBalanceEntity().apply { balanceCents = -1 }) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
        assertThat(kittyBalanceRepository.count()).isZero()
    }

    @Test
    fun `saving a kitty balance with an id other than 1 throws DataIntegrityViolationException`() {
        assertThatThrownBy { kittyBalanceRepository.saveAndFlush(KittyBalanceEntity().apply { id = 2 }) }
            .isInstanceOf(DataIntegrityViolationException::class.java)
        assertThat(kittyBalanceRepository.count()).isZero()
    }

    private companion object {
        /** Returns a new random id. A test assigns it to an [ExpenseEntity], as `upsert` does in a data adapter. */
        fun randomId() = java.util.UUID.randomUUID()
    }
}
