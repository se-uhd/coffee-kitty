package de.seuhd.campuscoffee.data.implementation

import de.seuhd.campuscoffee.data.integration.AbstractDataIntegrationTest
import de.seuhd.campuscoffee.domain.exceptions.DataConstraintException
import de.seuhd.campuscoffee.domain.exceptions.DuplicationException
import de.seuhd.campuscoffee.domain.model.CoffeeBean
import de.seuhd.campuscoffee.domain.ports.data.CoffeeBeanDataService
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

/**
 * The integration tests of [CoffeeBeanDataServiceImpl] against PostgreSQL. The partial unique index on the bean names
 * ignores case and leaves out the merged beans.
 */
class CoffeeBeanDataServiceIntegrationTest : AbstractDataIntegrationTest() {
    @Autowired
    private lateinit var coffeeBeanDataService: CoffeeBeanDataService

    private fun storeMergedBean(name: String): CoffeeBean {
        val target = coffeeBeanDataService.upsert(CoffeeBean(name = "$name Blend"))
        val merged = coffeeBeanDataService.upsert(CoffeeBean(name = name))
        return coffeeBeanDataService.upsert(merged.copy(active = false, mergedIntoId = target.id))
    }

    @Test
    fun `upsert stores a new bean as a live bean with an id`() {
        val created = coffeeBeanDataService.upsert(CoffeeBean(name = "Ethiopia Sidamo"))

        assertThat(created.id).isNotNull()
        assertThat(created.active).isTrue()
        assertThat(created.mergedIntoId).isNull()
        assertThat(coffeeBeanDataService.getById(requireNotNull(created.id))).isEqualTo(created)
    }

    @Test
    fun `upsert stores the new name of a bean`() {
        val created = coffeeBeanDataService.upsert(CoffeeBean(name = "Ethiopia Sidamo"))

        val updated =
            coffeeBeanDataService.upsert(created.copy(name = "Ethiopia Guji", createdAt = null, updatedAt = null))

        assertThat(updated.id).isEqualTo(created.id)
        assertThat(updated.createdAt).isEqualTo(created.createdAt)
        assertThat(updated.updatedAt).isAfter(created.updatedAt)
        assertThat(coffeeBeanDataService.getById(requireNotNull(created.id)).name).isEqualTo("Ethiopia Guji")
    }

    @Test
    fun `upsert throws DataConstraintException for an active bean that is merged into another bean`() {
        val target = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenya AA"))
        val other = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenia AA"))

        assertThatThrownBy { coffeeBeanDataService.upsert(other.copy(mergedIntoId = target.id)) }
            .isInstanceOf(DataConstraintException::class.java)
    }

    @Test
    fun `upsert throws DataConstraintException for an inactive bean that is not merged`() {
        val bean = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenia AA"))

        assertThatThrownBy { coffeeBeanDataService.upsert(bean.copy(active = false)) }
            .isInstanceOf(DataConstraintException::class.java)
    }

    @Test
    fun `upsert throws DataConstraintException for a bean merged into itself`() {
        val bean = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenya AA"))

        assertThatThrownBy { coffeeBeanDataService.upsert(bean.copy(active = false, mergedIntoId = bean.id)) }
            .isInstanceOf(DataConstraintException::class.java)
    }

    @Test
    fun `upsert stores a bean name of the maximum length`() {
        val name = "a".repeat(CoffeeBean.MAX_NAME_LENGTH)

        val created = coffeeBeanDataService.upsert(CoffeeBean(name = name))

        assertThat(coffeeBeanDataService.getById(requireNotNull(created.id)).name).isEqualTo(name)
    }

    @Test
    fun `upsert throws DuplicationException for the name of another live bean in other letter case`() {
        coffeeBeanDataService.upsert(CoffeeBean(name = "Ethiopia Sidamo"))

        assertThatThrownBy { coffeeBeanDataService.upsert(CoffeeBean(name = "ethiopia sidamo")) }
            .isInstanceOf(DuplicationException::class.java)
    }

    @Test
    fun `upsert stores a live bean with the name of a merged bean`() {
        val merged = storeMergedBean("Kenya AA")

        val live = coffeeBeanDataService.upsert(CoffeeBean(name = "Kenya AA"))

        assertThat(coffeeBeanDataService.findActiveByName("Kenya AA")).isEqualTo(live)
        assertThat(coffeeBeanDataService.getById(requireNotNull(merged.id)).active).isFalse()
    }

    @Test
    fun `findActiveByName ignores the letter case and returns null for a merged or unknown bean`() {
        val live = coffeeBeanDataService.upsert(CoffeeBean(name = "Brazil Santos"))
        storeMergedBean("Colombia Supremo")

        assertThat(coffeeBeanDataService.findActiveByName("BRAZIL santos")).isEqualTo(live)
        assertThat(coffeeBeanDataService.findActiveByName("Colombia Supremo")).isNull()
        assertThat(coffeeBeanDataService.findActiveByName("Unknown")).isNull()
    }

    @Test
    fun `upsert throws DataConstraintException for an empty bean name`() {
        assertThatThrownBy { coffeeBeanDataService.upsert(CoffeeBean(name = "")) }
            .isInstanceOf(DataConstraintException::class.java)
    }

    @Test
    fun `deleting a bean with a merged bean throws DataConstraintException`() {
        val merged = storeMergedBean("Guatemala")

        assertThatThrownBy { coffeeBeanDataService.delete(requireNotNull(merged.mergedIntoId)) }
            .isInstanceOf(DataConstraintException::class.java)
    }
}
