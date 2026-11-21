package de.seuhd.campuscoffee.api.mapper

import de.seuhd.campuscoffee.api.dtos.ConsumptionDto
import de.seuhd.campuscoffee.domain.tests.TestFixtures
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Unit test for [ConsumptionDtoMapper] with `ConsumptionDtoMapperImpl`, the implementation that MapStruct generates.
 * The DTO holds only the login name of the user and the coffee count. The consumption is a fixture from
 * [TestFixtures].
 */
class ConsumptionDtoMapperTest {
    private val mapper: ConsumptionDtoMapper = ConsumptionDtoMapperImpl()

    @Test
    fun `toDto copies the login name of the user and the coffee count`() {
        val consumption = TestFixtures.consumptionWithRecentCoffee(TestFixtures.regularUser())

        assertThat(mapper.toDto(consumption)).isEqualTo(ConsumptionDto(loginName = "maxmustermann", count = 1))
    }
}
